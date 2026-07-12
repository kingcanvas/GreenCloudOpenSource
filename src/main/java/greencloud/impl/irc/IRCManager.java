package greencloud.impl.irc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import greencloud.GreenCloud;
import greencloud.impl.managers.notification.NotificationManager;
import greencloud.impl.websocket.ServerHandshake;
import greencloud.impl.websocket.WebSocketClient;


import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IRCManager {
    private WebSocketClient wsClient;
    private boolean connected = false;
    private boolean authenticated = false;
    private String username;
    private String authToken;
    private String clientUuid;
    private String clientId;
    private String sessionToken;
    private final Gson gson = new Gson();

    private final List<IRCMessage> messages = new CopyOnWriteArrayList<>();
    private final List<ConnectedUser> onlineUsers = new CopyOnWriteArrayList<>();
    private long lastHeartbeat = 0;
    private long serverLatency = 0;
    private ScheduledExecutorService heartbeatExecutor;

    public void connectAsGuest() {
        this.username = "Guest_" + (System.currentTimeMillis() % 10000);
        this.authToken = null;
        connectToServer();
    }

    public void connectWithAuth(String token, String user) {
        this.authToken = token;
        this.username = user;
        connectToServer();
    }

    private void connectToServer() {
        try {
            String wsUrl = "wss://api.cattoclient.com/ws";

            wsClient = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    connected = true;
                    GreenCloud.logger.info("[IRC] Connected to server");
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    connected = false;
                    authenticated = false;
                    GreenCloud.logger.info("[IRC] Disconnected: " + reason);
                    NotificationManager.getInstance().addNotification(
                            "IRC Disconnected",
                            "Connection closed",
                            NotificationManager.NotificationType.WARNING,
                            3000
                    );
                }

                @Override
                public void onError(Exception ex) {
                    GreenCloud.logger.error("[IRC] Error: " + ex.getMessage());
                }
            };

            wsClient.connect();
        } catch (Exception e) {
            GreenCloud.logger.error("[IRC] Failed to connect: " + e.getMessage());
            NotificationManager.getInstance().addNotification(
                    "IRC Error",
                    "Failed to connect to server",
                    NotificationManager.NotificationType.ERROR,
                    3000
            );
        }
    }

    private void handleMessage(String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String type = json.get("type").getAsString();

            switch (type) {
                case "connection_init":
                    clientUuid = json.get("uuid").getAsString();
                    clientId = json.get("clientId").getAsString();
                    sessionToken = json.get("sessionToken").getAsString();
                    GreenCloud.logger.info("[IRC] Connection initialized: " + clientId);
                    authenticate();
                    break;

                case "auth_success":
                    authenticated = true;
                    clientUuid = json.get("uuid").getAsString();
                    boolean spotifyConnected = json.has("spotifyConnected") && json.get("spotifyConnected").getAsBoolean();

                    GreenCloud.logger.info("[IRC] Authenticated successfully as " + username);


                    if (json.has("connectedClients")) {
                        onlineUsers.clear();
                        json.getAsJsonArray("connectedClients").forEach(element -> {
                            JsonObject user = element.getAsJsonObject();
                            onlineUsers.add(new ConnectedUser(
                                    user.get("username").getAsString(),
                                    user.get("uuid").getAsString(),
                                    user.get("clientId").getAsString(),
                                    user.has("isGuest") && user.get("isGuest").getAsBoolean(),
                                    user.has("spotifyEnabled") && user.get("spotifyEnabled").getAsBoolean()
                            ));
                        });
                    }

                    addLocalMessage("Connected to Catto IRC");
                    addLocalMessage("Spotify: " + (spotifyConnected ? "Connected" : "Not connected"));

                    NotificationManager.getInstance().addNotification(
                            "IRC Connected",
                            "Successfully connected as " + username,
                            NotificationManager.NotificationType.SUCCESS,
                            3000
                    );

                    startHeartbeat();
                    break;

                case "guest_auth_success":
                    authenticated = true;
                    clientUuid = json.get("uuid").getAsString();

                    GreenCloud.logger.info("[IRC] Guest authenticated as " + username);


                    if (json.has("connectedClients")) {
                        onlineUsers.clear();
                        json.getAsJsonArray("connectedClients").forEach(element -> {
                            JsonObject user = element.getAsJsonObject();
                            onlineUsers.add(new ConnectedUser(
                                    user.get("username").getAsString(),
                                    user.get("uuid").getAsString(),
                                    user.get("clientId").getAsString(),
                                    user.has("isGuest") && user.get("isGuest").getAsBoolean(),
                                    user.has("spotifyEnabled") && user.get("spotifyEnabled").getAsBoolean()
                            ));
                        });
                    }

                    addLocalMessage("Connected as guest");

                    NotificationManager.getInstance().addNotification(
                            "IRC Connected",
                            "Connected as guest: " + username,
                            NotificationManager.NotificationType.SUCCESS,
                            3000
                    );

                    startHeartbeat();
                    break;

                case "auth_failed":
                    String reason = json.get("reason").getAsString();
                    String details = json.has("details") ? json.get("details").getAsString() : "";
                    GreenCloud.logger.error("[IRC] Auth failed: " + reason + " - " + details);
                    addLocalMessage("Authentication failed: " + reason);
                    NotificationManager.getInstance().addNotification(
                            "IRC Error",
                            "Authentication failed: " + reason,
                            NotificationManager.NotificationType.ERROR,
                            5000
                    );
                    break;

                case "chat_message":
                    String msgUsername = json.get("username").getAsString();
                    String msgContent = json.get("content").getAsString();
                    long msgTimestamp = json.get("timestamp").getAsLong();
                    boolean msgIsGuest = json.has("isGuest") && json.get("isGuest").getAsBoolean();
                    String msgUuid = json.get("uuid").getAsString();

                    IRCMessage ircMsg = new IRCMessage(
                            msgUsername,
                            msgContent,
                            msgTimestamp,
                            msgIsGuest,
                            msgUuid
                    );
                    messages.add(ircMsg);

                    // Keep only last 200 messages
                    if (messages.size() > 200) {
                        messages.remove(0);
                    }

                    if (!msgUsername.equals(username)) {
                        String preview = msgContent.length() > 40 ? msgContent.substring(0, 37) + "..." : msgContent;
                        NotificationManager.getInstance().addNotification(
                                msgUsername + (msgIsGuest ? " (Guest)" : ""),
                                preview,
                                NotificationManager.NotificationType.INFO,
                                4000
                        );
                    }
                    break;

                case "client_joined":
                    String joinedUser = json.get("username").getAsString();
                    String joinedUuid = json.get("uuid").getAsString();
                    String joinedClientId = json.get("clientId").getAsString();
                    boolean joinedIsGuest = json.has("isGuest") && json.get("isGuest").getAsBoolean();
                    boolean joinedSpotify = json.has("spotifyEnabled") && json.get("spotifyEnabled").getAsBoolean();

                    onlineUsers.add(new ConnectedUser(
                            joinedUser,
                            joinedUuid,
                            joinedClientId,
                            joinedIsGuest,
                            joinedSpotify
                    ));

                    addLocalMessage(joinedUser + " joined the server" + (joinedIsGuest ? " (Guest)" : ""));

                    if (!joinedUser.equals(username)) {
                        NotificationManager.getInstance().addNotification(
                                "User Joined",
                                joinedUser + " joined" + (joinedIsGuest ? " as guest" : ""),
                                NotificationManager.NotificationType.SUCCESS,
                                3000
                        );
                    }
                    break;

                case "client_left":
                    String leftUser = json.get("username").getAsString();
                    String leftUuid = json.get("uuid").getAsString();

                    onlineUsers.removeIf(u -> u.getUuid().equals(leftUuid));
                    addLocalMessage(leftUser + " left the server");

                    if (!leftUser.equals(username)) {
                        NotificationManager.getInstance().addNotification(
                                "User Left",
                                leftUser + " disconnected",
                                NotificationManager.NotificationType.WARNING,
                                3000
                        );
                    }
                    break;

                case "clients_list":
                    onlineUsers.clear();
                    json.getAsJsonArray("clients").forEach(element -> {
                        JsonObject user = element.getAsJsonObject();
                        onlineUsers.add(new ConnectedUser(
                                user.get("username").getAsString(),
                                user.get("uuid").getAsString(),
                                user.get("clientId").getAsString(),
                                user.has("isGuest") && user.get("isGuest").getAsBoolean(),
                                user.has("spotifyEnabled") && user.get("spotifyEnabled").getAsBoolean()
                        ));
                    });
                    GreenCloud.logger.info("[IRC] Updated online users: " + onlineUsers.size());
                    break;

                case "spotify_connection_update":
                    String spotifyUser = json.get("username").getAsString();
                    boolean spotifyStatus = json.get("connected").getAsBoolean();


                    onlineUsers.stream()
                            .filter(u -> u.getUsername().equals(spotifyUser))
                            .findFirst()
                            .ifPresent(u -> u.setSpotifyEnabled(spotifyStatus));

                    addLocalMessage(spotifyUser + " " + (spotifyStatus ? "connected" : "disconnected") + " Spotify");
                    break;

                case "spotify_playback_update":
                    String playbackUser = json.get("username").getAsString();
                    if (json.has("playback")) {
                        JsonObject playback = json.getAsJsonObject("playback");
                        if (playback != null && playback.has("trackName")) {
                            String track = playback.get("trackName").getAsString();
                            String artist = playback.has("artists") ? playback.get("artists").getAsString() : "Unknown";
                        }
                    }
                    break;

                case "heartbeat_ack":
                    lastHeartbeat = System.currentTimeMillis();
                    if (json.has("serverTime")) {
                        long serverTime = json.get("serverTime").getAsLong();
                        serverLatency = System.currentTimeMillis() - serverTime;
                    }
                    break;

                case "pong":
                    lastHeartbeat = System.currentTimeMillis();
                    if (json.has("latency") && !json.get("latency").isJsonNull()) {
                        serverLatency = json.get("latency").getAsLong();
                    }
                    break;

                case "error":
                    String errorMsg = json.has("error") ? json.get("error").getAsString() : "Unknown error";
                    String errorDetails = json.has("details") ? json.get("details").getAsString() : "";

                    addLocalMessage("Error: " + errorMsg);
                    GreenCloud.logger.error("[IRC] Server error: " + errorMsg + " - " + errorDetails);

                    NotificationManager.getInstance().addNotification(
                            "IRC Error",
                            errorMsg,
                            NotificationManager.NotificationType.ERROR,
                            5000
                    );
                    break;

                default:
                    GreenCloud.logger.debug("[IRC] Unknown message type: " + type);
                    break;
            }
        } catch (Exception e) {
            GreenCloud.logger.error("[IRC] Failed to parse message: " + e.getMessage());
        }
    }

    private void authenticate() {
        if (authToken != null) {
            JsonObject auth = new JsonObject();
            auth.addProperty("type", "auth");
            auth.addProperty("username", username);
            auth.addProperty("token", authToken);
            auth.addProperty("hwid", getHWID());
            sendMessage(auth.toString());
        } else {
            JsonObject guestAuth = new JsonObject();
            guestAuth.addProperty("type", "guest_auth");
            guestAuth.addProperty("username", username);
            guestAuth.addProperty("hwid", getHWID());
            sendMessage(guestAuth.toString());
        }
    }

    private void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "IRC Heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 0, 30, TimeUnit.SECONDS);
    }

    private void sendHeartbeat() {
        JsonObject heartbeat = new JsonObject();
        heartbeat.addProperty("type", "heartbeat");
        sendMessage(heartbeat.toString());
    }

    public void sendPing() {
        JsonObject ping = new JsonObject();
        ping.addProperty("type", "ping");
        ping.addProperty("clientTime", System.currentTimeMillis());
        sendMessage(ping.toString());
    }

    public void sendChatMessage(String content) {
        if (!authenticated) {
            addLocalMessage("Error: Not connected to IRC");
            return;
        }

        if (content == null || content.trim().isEmpty()) {
            return;
        }

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "chat_message");
        msg.addProperty("content", content.trim());

        sendMessage(msg.toString());


        IRCMessage ownMsg = new IRCMessage(
                username,
                content.trim(),
                System.currentTimeMillis(),
                authToken == null,
                clientUuid
        );
        messages.add(ownMsg);

        if (messages.size() > 200) {
            messages.remove(0);
        }
    }

    public void requestClientsList() {
        if (!authenticated) {
            return;
        }

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "request_clients");
        sendMessage(msg.toString());
    }

    private void sendMessage(String message) {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                wsClient.send(message);
            } catch (Exception e) {
                GreenCloud.logger.error("[IRC] Failed to send message: " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        if (wsClient != null) {
            try {
                wsClient.close();
            } catch (Exception e) {
                GreenCloud.logger.error("[IRC] Error during disconnect: " + e.getMessage());
            }
        }
        connected = false;
        authenticated = false;
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }
        addLocalMessage("Disconnected from IRC");
    }

    public void addLocalMessage(String content) {
        messages.add(new IRCMessage(
                "System",
                content,
                System.currentTimeMillis(),
                false,
                "system"
        ));
    }

    private String getHWID() {
        try {
            String os = System.getProperty("os.name", "unknown");
            String user = System.getProperty("user.name", "unknown");
            return user + "_" + os;
        } catch (Exception e) {
            return "unknown_hwid";
        }
    }

    public boolean isConnected() {
        return connected && authenticated;
    }

    public String getUsername() {
        return username;
    }

    public boolean isGuest() {
        return authToken == null;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public String getClientId() {
        return clientId;
    }

    public long getServerLatency() {
        return serverLatency;
    }

    public List<IRCMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    public List<ConnectedUser> getOnlineUsers() {
        return new ArrayList<>(onlineUsers);
    }

    public int getOnlineUserCount() {
        return onlineUsers.size();
    }

    public void clearMessages() {
        messages.clear();
    }


    public static class ConnectedUser {
        private final String username;
        private final String uuid;
        private final String clientId;
        private final boolean isGuest;
        private boolean spotifyEnabled;

        public ConnectedUser(String username, String uuid, String clientId, boolean isGuest, boolean spotifyEnabled) {
            this.username = username;
            this.uuid = uuid;
            this.clientId = clientId;
            this.isGuest = isGuest;
            this.spotifyEnabled = spotifyEnabled;
        }

        public String getUsername() {
            return username;
        }

        public String getUuid() {
            return uuid;
        }

        public String getClientId() {
            return clientId;
        }

        public boolean isGuest() {
            return isGuest;
        }

        public boolean isSpotifyEnabled() {
            return spotifyEnabled;
        }

        public void setSpotifyEnabled(boolean spotifyEnabled) {
            this.spotifyEnabled = spotifyEnabled;
        }

        @Override
        public String toString() {
            return username + (isGuest ? " (Guest)" : "") + (spotifyEnabled ? " 🎵" : "");
        }
    }
}
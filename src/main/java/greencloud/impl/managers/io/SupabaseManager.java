package greencloud.impl.managers.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import greencloud.GreenCloud;
import greencloud.impl.utils.HWIDUtil;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class SupabaseManager {

    private static final String URL = "https://zcbyeprsurmxhltwamdb.supabase.co/rest/v1";
    private static final String AUTH_URL = "https://zcbyeprsurmxhltwamdb.supabase.co/auth/v1/token?grant_type=password";
    private static final String KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpjYnllcHJzdXJteGhsdHdhbWRiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzcxNjU2MzAsImV4cCI6MjA5Mjc0MTYzMH0.s4I1mWY-Jw1swYQY8pcO5JX9IA6uVxdx4E5v5Qe_LY0";

    public static String currentUser = null;
    public static String currentUserRole = null;
    public static boolean isLoggedIn = false;
    public static boolean isBanned = false;

    private static final File sessionFile = new File(GreenCloud.mainDir, "session.gc");

    static {
        try {
            Field methodsField = HttpURLConnection.class.getDeclaredField("methods");
            methodsField.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);
            String[] oldMethods = (String[]) methodsField.get(null);
            Set<String> methodSet = new LinkedHashSet<>(Arrays.asList(oldMethods));
            methodSet.addAll(Arrays.asList("PATCH", "DELETE"));
            String[] newMethods = methodSet.toArray(new String[0]);
            methodsField.set(null, newMethods);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loginAsGuest() {
        currentUser = "Guest";
        currentUserRole = "Guest";
        isLoggedIn = true;
        isBanned = false;
    }

    public static void login(String email, String password, Runnable onSuccess, Runnable onFail) {
        new Thread(() -> {
            try {
                JsonObject authBody = new JsonObject();
                authBody.addProperty("email", email);
                authBody.addProperty("password", password);

                HttpURLConnection authConn = (HttpURLConnection) new java.net.URL(AUTH_URL).openConnection();
                authConn.setRequestMethod("POST");
                authConn.setRequestProperty("apikey", KEY);
                authConn.setRequestProperty("Content-Type", "application/json");
                authConn.setDoOutput(true);

                try (OutputStream os = authConn.getOutputStream()) {
                    os.write(authBody.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = authConn.getResponseCode();
                if (code == 200) {
                    String authResponse = IOUtils.toString(authConn.getInputStream(), StandardCharsets.UTF_8);
                    JsonObject authJson = new JsonParser().parse(authResponse).getAsJsonObject();
                    String accessToken = authJson.get("access_token").getAsString();
                    String userId = authJson.get("user").getAsJsonObject().get("id").getAsString();
                    fetchProfile(userId, accessToken, email, password, onSuccess, onFail);
                } else {
                    onFail.run();
                }
            } catch (Exception e) {
                e.printStackTrace();
                onFail.run();
            }
        }).start();
    }

    private static void fetchProfile(String userId, String token, String email, String password, Runnable onSuccess, Runnable onFail) {
        try {
            String query = "/profiles?id=eq." + userId + "&select=*";
            HttpURLConnection conn = (HttpURLConnection) new java.net.URL(URL + query).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token);

            if (conn.getResponseCode() == 200) {
                String response = IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8);
                JsonArray array = new JsonParser().parse(response).getAsJsonArray();

                if (array.size() > 0) {
                    JsonObject profile = array.get(0).getAsJsonObject();
                    isBanned = profile.has("is_banned") && profile.get("is_banned").getAsBoolean();
                    if (isBanned) {
                        GreenCloud.commandManager.sendMessage(EnumChatFormatting.RED + "You are banned!");
                        onFail.run();
                        return;
                    }
                    currentUser = profile.has("username") ? profile.get("username").getAsString() : email;
                    currentUserRole = profile.has("role") && !profile.get("role").isJsonNull() ? profile.get("role").getAsString() : "Member";
                    isLoggedIn = true;
                    saveSession(email, password);
                    onSuccess.run();
                } else {
                    currentUser = email; currentUserRole = "Member"; isLoggedIn = true;
                    saveSession(email, password); onSuccess.run();
                }
            } else { onFail.run(); }
        } catch (Exception e) { e.printStackTrace(); onFail.run(); }
    }

    public static void logout() {
        currentUser = null; isLoggedIn = false; isBanned = false;
        if (sessionFile.exists()) sessionFile.delete();
    }

    public static void uploadConfig(String configName, String description, String data) {
        if (currentUser == null || currentUser.equals("Guest")) return;
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("config_name", configName);
                json.addProperty("description", description);
                json.addProperty("config_data", data);
                json.addProperty("author_name", currentUser);
                sendRequest("POST", "/configs", json.toString());
                GreenCloud.commandManager.sendMessage(EnumChatFormatting.GREEN + "Config uploaded successfully!");
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    public static void updateConfig(long id, String configName, String description, String data) {
        if (currentUser == null || currentUser.equals("Guest")) return;
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("config_name", configName);
                json.addProperty("description", description);
                json.addProperty("config_data", data);
                sendRequest("PATCH", "/configs?id=eq." + id, json.toString());
                GreenCloud.commandManager.sendMessage(EnumChatFormatting.GREEN + "Config updated successfully!");
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    public static void deleteConfig(long id) {
        if (currentUser == null || currentUser.equals("Guest")) return;
        new Thread(() -> {
            try {
                sendRequest("DELETE", "/configs?id=eq." + id, null);
                GreenCloud.commandManager.sendMessage(EnumChatFormatting.GREEN + "Config deleted.");
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    public static List<CloudConfig> getConfigs() {
        List<CloudConfig> list = new ArrayList<>();
        try {
            String response = sendRequest("GET", "/configs?select=*&order=created_at.desc", null);
            JsonArray array = new JsonParser().parse(response).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                list.add(new CloudConfig(
                        obj.get("id").getAsLong(),
                        obj.get("config_name").getAsString(),
                        obj.get("author_name").getAsString(),
                        obj.get("config_data").getAsString(),
                        obj.get("created_at").getAsString(),
                        obj.has("updated_at") && !obj.get("updated_at").isJsonNull() ? obj.get("updated_at").getAsString() : obj.get("created_at").getAsString(),
                        obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : ""
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static List<CloudReview> getReviews(long configId) {
        List<CloudReview> list = new ArrayList<>();
        try {
            String response = sendRequest("GET", "/reviews?config_id=eq." + configId + "&order=created_at.desc", null);
            JsonArray array = new JsonParser().parse(response).getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                list.add(new CloudReview(
                        obj.get("author_name").getAsString(),
                        obj.get("comment").isJsonNull() ? "" : obj.get("comment").getAsString(),
                        obj.get("created_at").getAsString()
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static void postReview(long configId, String comment) {
        if (currentUser == null || currentUser.equals("Guest")) return;
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("config_id", configId);
                json.addProperty("author_name", currentUser);
                json.addProperty("stars", 5);
                json.addProperty("comment", comment);
                sendRequest("POST", "/reviews", json.toString());
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    public static void loadSession() {
        if (!sessionFile.exists()) return;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(sessionFile));
            String line = reader.readLine(); reader.close();
            if (line != null && !line.isEmpty()) {
                String decoded = new String(Base64.getDecoder().decode(line));
                String[] parts = decoded.split(":", 2);
                if (parts.length == 2) login(parts[0], parts[1], () -> {}, () -> sessionFile.delete());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void saveSession(String user, String pass) {
        try {
            String encoded = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
            PrintWriter writer = new PrintWriter(sessionFile);
            writer.println(encoded); writer.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static String sendRequest(String method, String endpoint, String data) throws Exception {
        java.net.URL url = new java.net.URL(URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("apikey", KEY);
        conn.setRequestProperty("Authorization", "Bearer " + KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        if (data != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) { os.write(data.getBytes(StandardCharsets.UTF_8)); }
        }
        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) return IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8);
        String error = "Unknown Error";
        if (conn.getErrorStream() != null) { error = IOUtils.toString(conn.getErrorStream(), StandardCharsets.UTF_8); }
        throw new IOException("HTTP " + code + ": " + error);
    }

    public static class CloudConfig {
        public long id; public String name, author, data, created, updated, description;
        public CloudConfig(long id, String n, String a, String d, String c, String u, String desc) {
            this.id = id; name = n; author = a; data = d; created = c; updated = u; description = desc;
        }
        public String getFormattedDate() {
            try { return new SimpleDateFormat("MMM dd, yyyy").format(Date.from(Instant.parse(updated))); } catch (Exception e) { return "Unknown"; }
        }
    }

    public static class CloudReview {
        public String author, comment, date;
        public CloudReview(String a, String c, String d) { author = a; comment = c; date = d; }
        public String getFormattedDate() {
            try { return new SimpleDateFormat("MMM dd, yyyy").format(Date.from(Instant.parse(date))); } catch (Exception e) { return "Unknown"; }
        }
    }
}

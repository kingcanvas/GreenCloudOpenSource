package greencloud.impl.websocket;

import greencloud.impl.utils.websocket.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

public abstract class WebSocketClient {
    private static final Logger logger = Logger.getLogger(WebSocketClient.class);
    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final URI uri;
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Thread readerThread;
    private volatile boolean open = false;
    private volatile boolean closing = false;

    public WebSocketClient(URI serverUri) {
        this.uri = serverUri;
    }

    public abstract void onOpen(ServerHandshake handshake);
    public abstract void onMessage(String message);
    public abstract void onClose(int code, String reason, boolean remote);
    public abstract void onError(Exception ex);

    public void connect() {
        new Thread(() -> {
            try {
                connectBlocking();
            } catch (Exception e) {
                logger.error("Connection failed", e);
                onError(e);
            }
        }, "WebSocket-Connect").start();
    }

    public boolean connectBlocking() throws Exception {
        String host = uri.getHost();
        int port = uri.getPort();
        boolean isSecure = uri.getScheme().equalsIgnoreCase("wss");

        if (port == -1) {
            port = isSecure ? 443 : 80;
        }

        logger.info("Connecting to " + uri);

        if (isSecure) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, new SecureRandom());
            SSLSocketFactory factory = sslContext.getSocketFactory();
            socket = factory.createSocket(host, port);
            ((SSLSocket) socket).startHandshake();
        } else {
            socket = new Socket(host, port);
        }

        socket.setTcpNoDelay(true);
        socket.setSoTimeout(0);

        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();

        ServerHandshake handshake = performHandshake();

        open = true;

        readerThread = new Thread(this::readLoop, "WebSocket-Reader");
        readerThread.setDaemon(true);
        readerThread.start();

        onOpen(handshake);

        return true;
    }

    private ServerHandshake performHandshake() throws Exception {
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (uri.getQuery() != null) {
            path += "?" + uri.getQuery();
        }

        byte[] keyBytes = new byte[16];
        new SecureRandom().nextBytes(keyBytes);
        String key = Base64.getEncoder().encodeToString(keyBytes);

        StringBuilder request = new StringBuilder();
        request.append("GET ").append(path).append(" HTTP/1.1\r\n");
        request.append("Host: ").append(uri.getHost());
        if (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443) {
            request.append(":").append(uri.getPort());
        }
        request.append("\r\n");
        request.append("Upgrade: websocket\r\n");
        request.append("Connection: Upgrade\r\n");
        request.append("Sec-WebSocket-Key: ").append(key).append("\r\n");
        request.append("Sec-WebSocket-Version: 13\r\n");
        request.append("Origin: http://").append(uri.getHost()).append("\r\n");
        request.append("\r\n");

        outputStream.write(request.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String statusLine = reader.readLine();

        if (statusLine == null || !statusLine.contains("101")) {
            throw new IOException("WebSocket handshake failed: " + statusLine);
        }

        ServerHandshake handshake = new ServerHandshake();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String headerName = line.substring(0, colonIndex).trim();
                String headerValue = line.substring(colonIndex + 1).trim();
                handshake.put(headerName, headerValue);
            }
        }

        logger.info("WebSocket handshake successful");
        return handshake;
    }

    private void readLoop() {
        try {
            while (open && !closing && !Thread.currentThread().isInterrupted()) {
                readFrame();
            }
        } catch (Exception e) {
            if (open && !closing) {
                logger.error("Read error", e);
                onError(e);
            }
        } finally {
            closeConnection(1006, "Abnormal closure", true);
        }
    }

    private void readFrame() throws IOException {
        int b1 = inputStream.read();
        if (b1 == -1) {
            throw new IOException("Connection closed by remote");
        }

        boolean fin = (b1 & 0x80) != 0;
        int opcode = b1 & 0x0F;

        int b2 = inputStream.read();
        if (b2 == -1) {
            throw new IOException("Connection closed by remote");
        }

        boolean masked = (b2 & 0x80) != 0;
        long payloadLength = b2 & 0x7F;

        if (payloadLength == 126) {
            payloadLength = ((inputStream.read() & 0xFF) << 8) | (inputStream.read() & 0xFF);
        } else if (payloadLength == 127) {
            payloadLength = 0;
            for (int i = 0; i < 8; i++) {
                payloadLength = (payloadLength << 8) | (inputStream.read() & 0xFF);
            }
        }

        byte[] maskKey = null;
        if (masked) {
            maskKey = new byte[4];
            for (int i = 0; i < 4; i++) {
                maskKey[i] = (byte) inputStream.read();
            }
        }

        byte[] payload = new byte[(int) payloadLength];
        int totalRead = 0;
        while (totalRead < payloadLength) {
            int read = inputStream.read(payload, totalRead, (int) (payloadLength - totalRead));
            if (read == -1) {
                throw new IOException("Connection closed while reading payload");
            }
            totalRead += read;
        }

        if (masked && maskKey != null) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskKey[i % 4];
            }
        }

        switch (opcode) {
            case 0x0:
                break;
            case 0x1:
                String message = new String(payload, StandardCharsets.UTF_8);
                onMessage(message);
                break;
            case 0x2:
                break;
            case 0x8:
                int closeCode = 1000;
                String closeReason = "";
                if (payload.length >= 2) {
                    closeCode = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
                    if (payload.length > 2) {
                        closeReason = new String(payload, 2, payload.length - 2, StandardCharsets.UTF_8);
                    }
                }
                closeConnection(closeCode, closeReason, true);
                break;
            case 0x9:
                sendPong(payload);
                break;
            case 0xA:
                break;
        }
    }

    public void send(String text) {
        if (!open) {
            logger.warn("Cannot send - not connected");
            return;
        }

        try {
            byte[] payload = text.getBytes(StandardCharsets.UTF_8);
            sendFrame(0x81, payload);
        } catch (IOException e) {
            logger.error("Send failed", e);
            onError(e);
        }
    }

    private void sendFrame(int firstByte, byte[] payload) throws IOException {
        synchronized (outputStream) {
            ByteArrayOutputStream frame = new ByteArrayOutputStream();

            frame.write(firstByte);

            int length = payload.length;
            if (length < 126) {
                frame.write(0x80 | length);
            } else if (length < 65536) {
                frame.write(0x80 | 126);
                frame.write((length >> 8) & 0xFF);
                frame.write(length & 0xFF);
            } else {
                frame.write(0x80 | 127);
                frame.write(0);
                frame.write(0);
                frame.write(0);
                frame.write(0);
                frame.write((length >> 24) & 0xFF);
                frame.write((length >> 16) & 0xFF);
                frame.write((length >> 8) & 0xFF);
                frame.write(length & 0xFF);
            }

            byte[] maskKey = new byte[4];
            new Random().nextBytes(maskKey);
            frame.write(maskKey);

            for (int i = 0; i < payload.length; i++) {
                frame.write(payload[i] ^ maskKey[i % 4]);
            }

            outputStream.write(frame.toByteArray());
            outputStream.flush();
        }
    }

    private void sendPong(byte[] payload) {
        try {
            sendFrame(0x8A, payload);
        } catch (IOException e) {
            logger.error("Failed to send pong", e);
        }
    }

    public void close() {
        close(1000, "Normal closure");
    }

    public void close(int code, String reason) {
        if (closing || !open) {
            return;
        }

        closing = true;

        try {
            byte[] reasonBytes = reason.getBytes(StandardCharsets.UTF_8);
            byte[] payload = new byte[2 + reasonBytes.length];
            payload[0] = (byte) ((code >> 8) & 0xFF);
            payload[1] = (byte) (code & 0xFF);
            System.arraycopy(reasonBytes, 0, payload, 2, reasonBytes.length);

            sendFrame(0x88, payload);
        } catch (IOException e) {
            logger.error("Error sending close frame", e);
        }

        closeConnection(code, reason, false);
    }

    private void closeConnection(int code, String reason, boolean remote) {
        if (!open) {
            return;
        }

        open = false;
        closing = true;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing socket", e);
        }

        if (readerThread != null) {
            readerThread.interrupt();
        }

        onClose(code, reason, remote);
    }

    public boolean isOpen() {
        return open && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public boolean isClosed() {
        return !open;
    }

    public URI getURI() {
        return uri;
    }
}
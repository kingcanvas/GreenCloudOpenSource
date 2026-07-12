package greencloud.impl.websocket;

import java.util.HashMap;
import java.util.Map;

public class ServerHandshake {
    private final Map<String, String> headers;

    public ServerHandshake() {
        this.headers = new HashMap<>();
    }

    public void put(String key, String value) {
        headers.put(key.toLowerCase(), value);
    }

    public String getFieldValue(String key) {
        return headers.get(key.toLowerCase());
    }

    public boolean hasFieldValue(String key) {
        return headers.containsKey(key.toLowerCase());
    }

    public short getHttpStatus() {
        return 101;
    }

    public String getHttpStatusMessage() {
        return "Switching Protocols";
    }
}
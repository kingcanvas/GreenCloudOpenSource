package greencloud.impl.irc;

public class IRCMessage {
    private final String username;
    private final String content;
    private final long timestamp;
    private final boolean isGuest;
    private final String uuid;

    public IRCMessage(String username, String content, long timestamp, boolean isGuest, String uuid) {
        this.username = username;
        this.content = content;
        this.timestamp = timestamp;
        this.isGuest = isGuest;
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isGuest() {
        return isGuest;
    }

    public String getUuid() {
        return uuid;
    }

    public String getFormattedTime() {
        long seconds = (System.currentTimeMillis() - timestamp) / 1000;
        if (seconds < 60) return seconds + "s ago";
        if (seconds < 3600) return (seconds / 60) + "m ago";
        if (seconds < 86400) return (seconds / 3600) + "h ago";
        return (seconds / 86400) + "d ago";
    }
}
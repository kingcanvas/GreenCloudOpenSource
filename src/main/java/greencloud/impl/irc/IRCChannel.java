package greencloud.impl.irc;

public class IRCChannel {
    private final String id;
    private final String name;
    private final String description;
    private final String topic;

    public IRCChannel(String id, String name, String description, String topic) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.topic = topic;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getTopic() {
        return topic;
    }
}
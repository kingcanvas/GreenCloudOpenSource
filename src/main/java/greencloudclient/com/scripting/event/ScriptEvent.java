package greencloudclient.com.scripting.event;

public abstract class ScriptEvent {

    private final String name;

    protected ScriptEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

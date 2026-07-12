package greencloud.impl.scripting.engine;

import greencloud.impl.scripting.script.ScriptContext;
import greencloud.impl.scripting.script.ScriptState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScriptPool {

    private final Map<String, ScriptContext> contexts = new LinkedHashMap<>();

    public void add(ScriptContext ctx) {
        contexts.put(ctx.getScript().getName(), ctx);
    }

    public void remove(String name) {
        ScriptContext ctx = contexts.remove(name);
        if (ctx != null) {
            ctx.callHookForced("onUnload");
            ctx.setState(ScriptState.DISABLED);
        }
    }

    public ScriptContext get(String name) {
        return contexts.get(name);
    }

    public Collection<ScriptContext> getRunning() {
        List<ScriptContext> running = new ArrayList<>();
        for (ScriptContext ctx : contexts.values()) {
            if (ctx.getState() == ScriptState.RUNNING) {
                running.add(ctx);
            }
        }
        return running;
    }

    public Collection<ScriptContext> getAll() {
        return Collections.unmodifiableCollection(contexts.values());
    }

    public void clear() {
        for (ScriptContext ctx : contexts.values()) {
            ctx.callHookForced("onUnload");
            ctx.setState(ScriptState.DISABLED);
        }
        contexts.clear();
    }

    public int size() {
        return contexts.size();
    }
}

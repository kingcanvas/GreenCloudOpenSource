package greencloud.impl.scripting.script;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

public final class ScriptContext {

    private final Script script;
    private Globals globals;
    private ScriptState state;
    private String errorMessage;

    public ScriptContext(Script script, Globals globals) {
        this.script = script;
        this.globals = globals;
        this.state = ScriptState.LOADED;
    }

    public Script getScript() { return script; }
    public Globals getGlobals() { return globals; }
    public ScriptState getState() { return state; }
    public String getErrorMessage() { return errorMessage; }

    public void setState(ScriptState state) { this.state = state; }

    public void markErrored(String message) {
        this.state = ScriptState.ERRORED;
        this.errorMessage = message;
    }

    public void callHook(String name, LuaValue... args) {
        if (state != ScriptState.RUNNING) return;
        invokeHook(name, args);
    }

    public void callHookForced(String name, LuaValue... args) {
        invokeHook(name, args);
    }

    private void invokeHook(String name, LuaValue[] args) {
        LuaValue fn = globals.get(name);
        if (fn.isnil() || !fn.isfunction()) return;
        try {
            fn.invoke(LuaValue.varargsOf(args));
        } catch (LuaError e) {
            markErrored("Hook '" + name + "' error: " + e.getMessage());
        }
    }
}

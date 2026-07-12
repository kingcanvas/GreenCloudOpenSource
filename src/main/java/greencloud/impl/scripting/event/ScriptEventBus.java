package greencloud.impl.scripting.event;

import greencloud.impl.scripting.script.ScriptContext;
import org.luaj.vm2.LuaValue;

import java.util.Collection;

public final class ScriptEventBus {

    private ScriptEventBus() {}

    public static void dispatch(Collection<ScriptContext> contexts, String hook, LuaValue... args) {
        for (ScriptContext ctx : contexts) {
            ctx.callHook(hook, args);
        }
    }
}

package greencloud.impl.scripting.engine;

import greencloud.impl.logger.Log;
import greencloud.impl.logger.Logger;
import greencloud.impl.scripting.api.module.ModuleAPI;
import greencloud.impl.scripting.api.player.PlayerAPI;
import greencloud.impl.scripting.api.render.RenderAPI;
import greencloud.impl.scripting.api.world.WorldAPI;
import greencloud.impl.scripting.sandbox.Sandbox;
import greencloud.impl.scripting.script.Script;
import greencloud.impl.scripting.script.ScriptContext;
import greencloud.impl.scripting.script.ScriptState;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import java.io.FileInputStream;
import java.io.IOException;

public final class ScriptEngine {

    private static final Logger log = Log.get(ScriptEngine.class);

    private ScriptEngine() {}

    public static ScriptContext load(Script script) {
        Globals globals = Sandbox.createGlobals();
        injectAPIs(globals, script);

        ScriptContext ctx = new ScriptContext(script, globals);

        try (FileInputStream fis = new FileInputStream(script.getFile())) {
            LuaValue chunk = globals.load(
                    new java.io.InputStreamReader(fis, "UTF-8"),
                    script.getName()
            );
            chunk.call();
            ctx.setState(ScriptState.RUNNING);
            log.info("[Scripting] Loaded: " + script);
            ctx.callHook("onLoad");
        } catch (LuaError e) {
            ctx.markErrored(e.getMessage());
            log.error("[Scripting] Lua error in " + script.getName() + ": " + e.getMessage());
        } catch (IOException e) {
            ctx.markErrored(e.getMessage());
            log.error("[Scripting] Failed to read " + script.getFile().getName() + ": " + e.getMessage());
        }

        return ctx;
    }

    private static void injectAPIs(Globals globals, Script script) {
        globals.set("player", PlayerAPI.create());
        globals.set("modules", ModuleAPI.create());
        globals.set("world", WorldAPI.create());
        globals.set("render", RenderAPI.create());

        globals.set("print", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                log.info("[Script:" + script.getName() + "] " + arg.tojstring());
                return LuaValue.NIL;
            }
        });

        globals.set("SCRIPT_NAME", LuaValue.valueOf(script.getName()));
        globals.set("SCRIPT_AUTHOR", LuaValue.valueOf(script.getAuthor()));
        globals.set("SCRIPT_VERSION", LuaValue.valueOf(script.getVersion()));
    }
}

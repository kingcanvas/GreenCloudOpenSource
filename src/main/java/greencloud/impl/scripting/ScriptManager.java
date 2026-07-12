package greencloud.impl.scripting;

import greencloud.impl.logger.Log;
import greencloud.impl.logger.Logger;
import greencloud.impl.scripting.engine.ScriptEngine;
import greencloud.impl.scripting.engine.ScriptLoader;
import greencloud.impl.scripting.engine.ScriptPool;
import greencloud.impl.scripting.event.ScriptEventBus;
import greencloud.impl.scripting.script.Script;
import greencloud.impl.scripting.script.ScriptContext;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.util.Collection;
import java.util.List;

public final class ScriptManager {

    private static final Logger log = Log.get(ScriptManager.class);

    private final ScriptPool pool = new ScriptPool();
    private final File scriptsDir;

    public ScriptManager(File mainDir) {
        this.scriptsDir = new File(mainDir, "scripts");
    }

    public void init() {
        log.info("[Scripting] Initializing ScriptManager");
        MinecraftForge.EVENT_BUS.register(this);
        reload();
    }

    public void reload() {
        log.info("[Scripting] Reloading scripts");
        pool.clear();

        List<Script> discovered = ScriptLoader.discover(scriptsDir);
        for (Script script : discovered) {
            ScriptContext ctx = ScriptEngine.load(script);
            pool.add(ctx);
        }

        log.info("[Scripting] " + pool.size() + " script(s) loaded");
    }

    public void loadScript(File file) {
        List<Script> discovered = ScriptLoader.discover(file.getParentFile());
        for (Script s : discovered) {
            if (s.getFile().equals(file)) {
                ScriptContext ctx = ScriptEngine.load(s);
                pool.add(ctx);
                log.info("[Scripting] Loaded: " + s);
                return;
            }
        }
        log.warn("[Scripting] Could not load script from: " + file.getAbsolutePath());
    }

    public void unloadScript(String name) {
        pool.remove(name);
        log.info("[Scripting] Unloaded script: " + name);
    }

    public Collection<ScriptContext> getAll() {
        return pool.getAll();
    }

    public ScriptContext get(String name) {
        return pool.get(name);
    }

    public File getScriptsDir() {
        return scriptsDir;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        ScriptEventBus.dispatch(pool.getRunning(), "onTick");
    }

    @SubscribeEvent
    public void onRender2D(RenderGameOverlayEvent.Text event) {
        ScriptEventBus.dispatch(pool.getRunning(), "onRender2D",
                LuaValue.valueOf(event.resolution.getScaledWidth()),
                LuaValue.valueOf(event.resolution.getScaledHeight())
        );
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        ScriptEventBus.dispatch(pool.getRunning(), "onChat",
                LuaValue.valueOf(event.message.getUnformattedText())
        );
    }
}

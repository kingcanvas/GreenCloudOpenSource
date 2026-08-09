package greencloudclient.com.scripting.event.handler;

import greencloudclient.com.scripting.event.ScriptEventBus;
import greencloudclient.com.scripting.script.ScriptContext;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.luaj.vm2.LuaValue;

import java.util.Collection;

public final class RenderHandler {

    private final Collection<ScriptContext> contexts;

    public RenderHandler(Collection<ScriptContext> contexts) {
        this.contexts = contexts;
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Text event) {
        ScriptEventBus.dispatch(contexts, "onRender2D",
                LuaValue.valueOf(event.resolution.getScaledWidth()),
                LuaValue.valueOf(event.resolution.getScaledHeight())
        );
    }
}

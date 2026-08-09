package greencloudclient.com.scripting.event.handler;

import greencloudclient.com.scripting.event.ScriptEventBus;
import greencloudclient.com.scripting.script.ScriptContext;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collection;

public final class TickHandler {

    private final Collection<ScriptContext> contexts;

    public TickHandler(Collection<ScriptContext> contexts) {
        this.contexts = contexts;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        ScriptEventBus.dispatch(contexts, "onTick");
    }
}

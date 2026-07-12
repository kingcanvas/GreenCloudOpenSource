package greencloud.impl.scripting.event.handler;

import greencloud.impl.scripting.event.ScriptEventBus;
import greencloud.impl.scripting.script.ScriptContext;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.luaj.vm2.LuaValue;

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

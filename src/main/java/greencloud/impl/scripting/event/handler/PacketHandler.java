package greencloud.impl.scripting.event.handler;

import greencloud.impl.scripting.event.ScriptEventBus;
import greencloud.impl.scripting.script.ScriptContext;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.luaj.vm2.LuaValue;

import java.util.Collection;

public final class PacketHandler {

    private final Collection<ScriptContext> contexts;

    public PacketHandler(Collection<ScriptContext> contexts) {
        this.contexts = contexts;
    }

    @SubscribeEvent
    public void onPacketSend(net.minecraftforge.client.event.ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText();
        ScriptEventBus.dispatch(contexts, "onChat",
                LuaValue.valueOf(message)
        );
    }
}

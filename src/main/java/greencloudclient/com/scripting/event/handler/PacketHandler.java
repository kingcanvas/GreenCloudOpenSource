package greencloudclient.com.scripting.event.handler;

import greencloudclient.com.scripting.event.ScriptEventBus;
import greencloudclient.com.scripting.script.ScriptContext;
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

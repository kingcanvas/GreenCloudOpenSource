package greencloud.impl.events;

import greencloud.GreenCloud;
import greencloud.event.PacketEvent;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatListener {

    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        if (event.getDirection() != PacketEvent.Direction.OUTGOING) return;

        Object packet = event.getPacket();
        if (!(packet instanceof C01PacketChatMessage)) return;

        String message = ((C01PacketChatMessage) packet).getMessage();
        greencloud.impl.command.CommandManager cm = GreenCloud.commandManager;
        if (message.startsWith(cm.getPrefix())) {
            cm.handleCommand(message);
            event.setCanceled(true);
        }
    }
}
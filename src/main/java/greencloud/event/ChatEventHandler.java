package greencloud.event;

import greencloud.GreenCloud;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatEventHandler {

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText();

        if (message.startsWith(GreenCloud.commandManager.getPrefix())) {
            boolean handled = GreenCloud.commandManager.handleCommand(message);
            if (handled) {
                event.setCanceled(true);
            }
        }
    }
}
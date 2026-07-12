package greencloud.impl.managers.player;

import greencloud.event.InputUpdateEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class InputManager {
    private final Minecraft mc = Minecraft.getMinecraft();

    public InputManager() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.entity == mc.thePlayer && mc.thePlayer.movementInput != null) {
            InputUpdateEvent inputEvent = new InputUpdateEvent(mc.thePlayer, mc.thePlayer.movementInput);
            MinecraftForge.EVENT_BUS.post(inputEvent);
        }
    }
}
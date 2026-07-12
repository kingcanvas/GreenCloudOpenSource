package greencloud.impl.modules.utility;

import greencloud.GreenCloud;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class NoJumpDelay extends Module {

    public NoJumpDelay() {
        super("NoJumpDelay", "Removes jump delay", Category.UTILITY);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.player == mc.thePlayer) {
            event.player.setJumping(false);
            try {
                net.minecraft.entity.EntityLivingBase.class.getField("jumpTicks").set(event.player, 0);
            } catch (Exception ignored) {}
        }
    }
}
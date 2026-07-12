package greencloud.impl.modules.movement;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class Sprint extends Module {

    public Sprint() {
        super("Sprint", "Auto-Sprint", Category.MOVEMENT);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.player != mc.thePlayer) return;

        if (shouldSprint()) {
            mc.thePlayer.setSprinting(true);
        }
    }

    private boolean shouldSprint() {
        return mc.thePlayer.moveForward > 0
                && !mc.thePlayer.isSneaking()
                && !mc.thePlayer.isUsingItem()
                && !mc.thePlayer.isCollidedHorizontally
                && mc.thePlayer.getFoodStats().getFoodLevel() > 6
                && mc.currentScreen == null;
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.setSprinting(false);
        }
        super.onDisable();
    }
}
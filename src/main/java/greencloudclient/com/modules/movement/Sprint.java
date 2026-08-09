package greencloudclient.com.modules.movement;

import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.com.settings.ModeSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class Sprint extends Module {
    
    public ModeSetting mode = new ModeSetting("Mode", this, "Legit", "Legit", "Omni");
    
    public Sprint() {
        super("Sprint", Category.MOVEMENT);
    }
    
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.player != mc.thePlayer) return;
        
        if (mode.is("Legit") && shouldSprintLegit()) {
            mc.thePlayer.setSprinting(true);
        } else if (mode.is("Omni") && shouldSprintOmni()) {
            mc.thePlayer.setSprinting(true);
        }
    }
    
    private boolean shouldSprintLegit() {
        return mc.thePlayer.moveForward > 0
                && !mc.thePlayer.isSneaking()
                && !mc.thePlayer.isUsingItem()
                && !mc.thePlayer.isCollidedHorizontally
                && mc.thePlayer.getFoodStats().getFoodLevel() > 6
                && mc.currentScreen == null;
    }
    
    private boolean shouldSprintOmni() {
        return (mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0)
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
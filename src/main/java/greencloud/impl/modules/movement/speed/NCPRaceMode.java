package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;

public class NCPRaceMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    public NCPRaceMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "NCPRace";
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {}

    @Override
    public void onUpdate() {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump();
            module.setTimerSpeed(1.2f);
            mc.thePlayer.motionX *= 1.0708;
            mc.thePlayer.motionZ *= 1.0708;
            mc.thePlayer.moveStrafing *= 2.0f;
            return;
        }
        module.setTimerSpeed(0.98f);
        mc.thePlayer.jumpMovementFactor = 0.0265f;
    }

    @Override
    public void onDisable() {}
}

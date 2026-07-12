package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;

public class NCPMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    public NCPMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "NCP";
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {
        module.setTimerSpeed(1.0865f);
    }

    @Override
    public void onUpdate() {
        module.setTimerSpeed(1.0865f);
        if (module.isMoving()) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                mc.thePlayer.motionX *= 1.01;
                mc.thePlayer.motionZ *= 1.01;
                module.setSpeedInAir(0.022f);
            }
            mc.thePlayer.motionY -= 0.00099;
            module.setMoveSpeed(module.getCurrentSpeed());
        } else {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }
    }

    @Override
    public void onDisable() {}
}

package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;

public class IntaveFastMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    public IntaveFastMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "IntaveFast";
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {
        module.setTimerSpeed(1.0f);
        module.setSpeedInAir(0.02f);
    }

    @Override
    public void onUpdate() {
        if (!module.isMoving()) {
            module.setTimerSpeed(1.0f);
            module.setSpeedInAir(0.02f);
            return;
        }

        if (mc.thePlayer.onGround) {
            module.setTimerSpeed(4.0f);
            module.setSpeedInAir(0.02f);
            mc.thePlayer.jump();
        } else {
            module.setTimerSpeed(0.4f);
            module.setSpeedInAir(0.029f);
        }
    }

    @Override
    public void onDisable() {
        module.setTimerSpeed(1.0f);
        module.setSpeedInAir(0.02f);
    }
}

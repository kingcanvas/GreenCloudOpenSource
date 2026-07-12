package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;

public class TestMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    public TestMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {}

    @Override
    public void onUpdate() {
        if (module.isMoving()) {
            if (mc.thePlayer.isUsingItem()) {
                if (mc.thePlayer.isBlocking()) {
                    module.setTimerSpeed(1.23f);
                } else if (module.getTimerSpeed() > 1.0f) {
                    module.setTimerSpeed(1.0f);
                }
            } else {
                module.setTimerSpeed(1.23f);
            }
        } else if (module.getTimerSpeed() > 1.0f) {
            module.setTimerSpeed(1.0f);
        }

        if (mc.thePlayer.hurtTime > 8 && !mc.thePlayer.isBurning() && mc.thePlayer.fallDistance < 2.0f) {
            module.setMoveSpeed(module.getCurrentSpeed() + 0.4);
        }
    }

    @Override
    public void onDisable() {}
}

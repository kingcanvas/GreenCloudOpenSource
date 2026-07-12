package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;

import java.util.Collections;
import java.util.List;

public class IntaveStrafeMode implements ISubSetting {

    private final Speed module;

    public IntaveStrafeMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "IntaveStrafe";
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {}

    @Override
    public void onUpdate() {
        if (module.offGround >= 10 && module.offGround % 5 == 0) {
            module.setMoveSpeed(module.getCurrentSpeed());
        }
    }

    @Override
    public void onDisable() {}
}

package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.settings.Setting;

import java.util.Arrays;
import java.util.List;

public class VanillaMode implements ISubSetting {

    private final Speed module;
    public final NumberSetting speed;

    public VanillaMode(Speed module) {
        this.module = module;
        this.speed = new NumberSetting("Speed", module, 1.0, 0.0, 10.0, 0.1);
    }

    @Override
    public String getName() {
        return "Vanilla";
    }

    @Override
    public List<Setting> getSettings() {
        return Arrays.asList(speed);
    }

    @Override
    public void onEnable() {}

    @Override
    public void onUpdate() {
        if (module.isMoving()) {
            module.setMoveSpeed(0.1 * speed.value);
        }
    }

    @Override
    public void onDisable() {}
}

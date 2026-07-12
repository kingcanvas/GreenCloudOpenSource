package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.List;

public class VanillaHopMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    public final NumberSetting speed;
    public final NumberSetting height;
    public final BooleanSetting strafe;

    public VanillaHopMode(Speed module) {
        this.module = module;
        this.speed = new NumberSetting("Speed", module, 1.0, 0.0, 10.0, 0.1);
        this.height = new NumberSetting("Height", module, 0.2, 0.01, 0.42, 0.01);
        this.strafe = new BooleanSetting("Strafe", module, false);
    }

    @Override
    public String getName() {
        return "VanillaHop";
    }

    @Override
    public List<Setting> getSettings() {
        return Arrays.asList(speed, height, strafe);
    }

    @Override
    public void onEnable() {}

    @Override
    public void onUpdate() {
        if (module.canJump()) {
            mc.thePlayer.motionY = height.value;
            if (strafe.enabled) {
                if (mc.thePlayer.onGround) mc.thePlayer.jump();
                module.setMoveSpeed(module.getCurrentSpeed());
            }
        } else if (module.isMoving()) {
            module.setMoveSpeed(0.1 * speed.value);
        }
    }

    @Override
    public void onDisable() {}
}

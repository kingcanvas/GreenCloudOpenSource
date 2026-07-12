package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;

public class GroundStrafeMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    public GroundStrafeMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "GroundStrafe";
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
            module.setMoveSpeed(module.getCurrentSpeed());
        }
    }

    @Override
    public void onDisable() {}
}

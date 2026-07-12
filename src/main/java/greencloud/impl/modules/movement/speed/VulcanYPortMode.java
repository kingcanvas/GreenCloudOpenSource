package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;

public class VulcanYPortMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    public VulcanYPortMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "VulcanYPort";
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {}

    @Override
    public void onUpdate() {
        if (!module.isMoving()) return;

        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump();
            mc.thePlayer.motionY = 0.42;
            module.setMoveSpeed(module.getBaseMoveSpeed() * 1.9);
            return;
        }

        if (mc.thePlayer.motionY > 0) mc.thePlayer.motionY -= 0.08;

        if (mc.thePlayer.motionY < 0) {
            mc.thePlayer.motionY = -0.15;
            module.setMoveSpeed(module.getBaseMoveSpeed() * 1.75);
        }
    }

    @Override
    public void onDisable() {}
}

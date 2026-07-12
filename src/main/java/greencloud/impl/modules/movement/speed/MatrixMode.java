package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;

public class MatrixMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    public MatrixMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "Matrix";
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {}

    @Override
    public void onUpdate() {
        if (mc.thePlayer.motionX == 0.0 && mc.thePlayer.motionZ == 0.0) return;
        if (!module.isMoving()) return;

        mc.thePlayer.motionY -= 0.009999;
        if (mc.thePlayer.onGround) {
            module.setMoveSpeed(module.getBaseMoveSpeed() * 1.6);
        } else if (mc.thePlayer.movementInput.moveForward > 0.0F && mc.thePlayer.movementInput.moveStrafe != 0.0F) {
            module.setSpeedInAir(0.02f);
        } else {
            module.setSpeedInAir(0.0208f);
        }
    }

    @Override
    public void onDisable() {}
}

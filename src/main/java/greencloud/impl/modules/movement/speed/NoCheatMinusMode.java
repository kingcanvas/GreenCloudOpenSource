package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;

import java.util.Collections;
import java.util.List;

public class NoCheatMinusMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    private double speed;
    private int stage;
    private double lastDist;

    public NoCheatMinusMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "NoCheatMinus";
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {
        speed = module.getBaseMoveSpeed();
        stage = 2;
        lastDist = 0.0;
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer.moveForward == 0.0f && mc.thePlayer.moveStrafing == 0.0f) {
            speed = module.getBaseMoveSpeed();
        }

        if (stage == 1 && mc.thePlayer.isCollidedVertically && (mc.thePlayer.moveForward != 0.0f || mc.thePlayer.moveStrafing != 0.0f)) {
            speed = 0.25 + module.getBaseMoveSpeed() - 0.01;
        } else if (stage == 2 && mc.thePlayer.isCollidedVertically && module.isOnGround(0.001) && (mc.thePlayer.moveForward != 0.0f || mc.thePlayer.moveStrafing != 0.0f)) {
            mc.thePlayer.motionY = 0.4;
            mc.thePlayer.jump();
            speed *= 2.149;
        } else if (stage == 3) {
            double diff = 0.66 * (lastDist - module.getBaseMoveSpeed());
            speed = lastDist - diff;
        } else {
            List<AxisAlignedBB> collidingList = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, mc.thePlayer.motionY, 0.0));
            if ((collidingList.size() > 0 || mc.thePlayer.isCollidedVertically) && stage > 0) {
                if (1.35 * module.getBaseMoveSpeed() - 0.01 > speed) {
                    stage = 0;
                } else {
                    stage = (mc.thePlayer.moveForward != 0.0f || mc.thePlayer.moveStrafing != 0.0f) ? 1 : 0;
                }
            }
            speed = lastDist - lastDist / 159.0;
        }

        speed = Math.max(speed, module.getBaseMoveSpeed());
        if (stage > 0) module.setMoveSpeed(speed);
        if (mc.thePlayer.moveForward != 0.0f || mc.thePlayer.moveStrafing != 0.0f) stage++;
    }

    @Override
    public void onDisable() {}
}

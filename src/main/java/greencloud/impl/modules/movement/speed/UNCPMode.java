package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;

import java.util.Collections;
import java.util.List;

public class UNCPMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    private float uncpSpeed;
    private int uncpTick;

    public UNCPMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "UNCP";
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {
        uncpSpeed = 0.0f;
        uncpTick = 0;
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer.isInWater() || module.getIsInWeb() || mc.thePlayer.isOnLadder()) return;

        if (module.isMoving()) {
            if (mc.thePlayer.onGround) {
                boolean speedActive = mc.thePlayer.isPotionActive(Potion.moveSpeed);
                int amplifier = speedActive ? mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() : -1;
                uncpSpeed = (speedActive && amplifier >= 1) ? 0.4563f : 0.3385f;
                mc.thePlayer.jump();
            } else {
                uncpSpeed *= 0.98f;
            }

            if (mc.thePlayer.fallDistance > 2) {
                module.setTimerSpeed(1.0f);
                return;
            }

            module.setMoveSpeed(uncpSpeed);

            if (!mc.thePlayer.onGround) {
                uncpTick++;
                if (uncpTick % 3 == 0) {
                    module.setTimerSpeed(1.0815f);
                    uncpTick = 0;
                } else {
                    module.setTimerSpeed(0.9598f);
                }
            }
        } else {
            module.setTimerSpeed(1.0f);
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
        }
    }

    @Override
    public void onDisable() {}
}

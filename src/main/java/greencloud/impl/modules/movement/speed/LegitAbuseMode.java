package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;

public class LegitAbuseMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    public LegitAbuseMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "LegitAbuse";
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
            if (mc.thePlayer.onGround) mc.thePlayer.jump();

            int down = 0;
            if (mc.gameSettings.keyBindForward.isKeyDown()) down++;
            if (mc.gameSettings.keyBindBack.isKeyDown()) down++;
            if (mc.gameSettings.keyBindLeft.isKeyDown()) down++;
            if (mc.gameSettings.keyBindRight.isKeyDown()) down++;

            if (down != 1) return;

            double increase = mc.thePlayer.onGround ? 0.0026 : 0.00052;
            double yaw = module.getDirection();
            mc.thePlayer.motionX += -Math.sin(yaw) * increase;
            mc.thePlayer.motionZ += Math.cos(yaw) * increase;
        }
    }

    @Override
    public void onDisable() {}
}

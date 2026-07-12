package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;

public class NCPLowMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    private int motionDelay;

    public NCPLowMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "NCPLow";
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {
        motionDelay = 0;
    }

    @Override
    public void onUpdate() {
        if (module.isMoving()) {
            if (mc.gameSettings.keyBindJump.isPressed()) return;

            if (mc.thePlayer.onGround) {
                motionDelay++;
                motionDelay %= 3;
                if (motionDelay == 0) {
                    mc.thePlayer.motionY += 0.18;
                    mc.thePlayer.motionX *= 1.2;
                    mc.thePlayer.motionZ *= 1.2;
                }
            }

            if (!mc.thePlayer.onGround) {
                mc.thePlayer.motionX *= 1.05;
                mc.thePlayer.motionZ *= 1.05;
            }
        }
    }

    @Override
    public void onDisable() {}
}

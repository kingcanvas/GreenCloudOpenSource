package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;

import java.util.Collections;
import java.util.List;

public class OldBlocksMCMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    private int obmcState;
    private int obmcFlagDelay;

    public OldBlocksMCMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "OldBlocksMC";
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {
        obmcState = 0;
        obmcFlagDelay = 0;
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer.onGround) {
            obmcState = 1;
        }

        double speed = 0.06;
        speed += mc.thePlayer.onGround ? 0.12 : 0.21;
        speed += mc.thePlayer.motionY / 20.0;

        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            if (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() == 0) {
                speed += 0.1;
            }
        }

        if (obmcFlagDelay > 0) {
            obmcFlagDelay--;
            for (int i = 0; i < obmcFlagDelay; i++) {
                speed -= 0.007;
            }
        }

        if (obmcState == 1 && module.airTicks == 4) {
            mc.thePlayer.motionY = -0.098;
        }

        if (module.isMoving() && mc.thePlayer.onGround && obmcState != 0) {
            mc.thePlayer.jump();
        }

        if (!mc.thePlayer.onGround && obmcState != 0 && module.isMoving()) {
            module.setMoveSpeed(speed);
        }
    }

    @Override
    public void onDisable() {}
}

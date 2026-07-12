package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.List;

public class VerusMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanSetting damageBoost;
    public final NumberSetting dmgSpeed;

    public VerusMode(Speed module) {
        this.module = module;
        this.damageBoost = new BooleanSetting("Damage Boost", module, false);
        this.dmgSpeed = new NumberSetting("DMG Speed", module, 1.0, 0.0, 2.0, 0.1);
    }

    @Override
    public String getName() {
        return "Verus";
    }

    @Override
    public List<Setting> getSettings() {
        return Arrays.asList(damageBoost, dmgSpeed);
    }

    @Override
    public void onEnable() {}

    @Override
    public void onUpdate() {
        if (damageBoost.enabled && mc.thePlayer.hurtTime != 0 && mc.thePlayer.fallDistance < 3.0f) {
            module.setMoveSpeed(dmgSpeed.value);
        } else {
            module.setMoveSpeed(0.292);
        }

        if (module.canJump()) {
            mc.thePlayer.jump();
        } else {
            mc.thePlayer.jumpMovementFactor = 0.1f;
        }
    }

    @Override
    public void onDisable() {}
}

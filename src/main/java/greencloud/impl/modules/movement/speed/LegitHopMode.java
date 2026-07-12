package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;

public class LegitHopMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    public LegitHopMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "LegitHop";
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {}

    @Override
    public void onUpdate() {
        if (module.isMoving() && mc.thePlayer.onGround) {
            mc.thePlayer.jump();
        }
    }

    @Override
    public void onDisable() {}
}

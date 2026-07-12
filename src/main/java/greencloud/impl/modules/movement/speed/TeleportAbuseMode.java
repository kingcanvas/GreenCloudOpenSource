package greencloud.impl.modules.movement.speed;

import greencloud.impl.modules.movement.Speed;
import greencloud.impl.modules.subsetting.ISubSetting;
import greencloud.impl.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;

public class TeleportAbuseMode implements ISubSetting {

    private final Speed module;
    private final Minecraft mc = Minecraft.getMinecraft();

    private boolean first;

    public TeleportAbuseMode(Speed module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "TeleportAbuse";
    }

    @Override
    public List<Setting> getSettings() {
        return Collections.emptyList();
    }

    @Override
    public void onEnable() {
        first = true;
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump();
            module.setTimerSpeed(first ? 5f : 30f);
            first = false;
        } else {
            module.setTimerSpeed(0.3f);
        }
    }

    @Override
    public void onDisable() {}
}

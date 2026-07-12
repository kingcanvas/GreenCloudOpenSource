package greencloud.impl.modules.render;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.ModeSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Fullbright extends Module {

    public ModeSetting mode = new ModeSetting("Mode", this, "Gamma", "Gamma");
    private float oldGamma;

    public Fullbright() {
        super("Fullbright", "Makes it brighter", Category.RENDER);
        addSetting(mode);
    }

    @Override
    public void onEnable() {
        oldGamma = mc.gameSettings.gammaSetting;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        mc.gameSettings.gammaSetting = oldGamma;
        super.onDisable();
    }
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null) return;
        if (mode.currentMode.equals("Gamma")) {
            mc.gameSettings.gammaSetting = 100.0F;
        }
    }
}
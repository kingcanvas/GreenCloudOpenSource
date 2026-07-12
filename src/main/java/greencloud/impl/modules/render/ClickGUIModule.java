package greencloud.impl.modules.render;

import greencloud.GreenCloud;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ColorSetting;
import greencloud.impl.settings.ModeSetting;
import org.lwjgl.input.Keyboard;

import java.awt.Color;

public class ClickGUIModule extends Module {

    public final ModeSetting guiMode = new ModeSetting("Mode", this, "Modern", "Modern", "KingCanvas");
    public final BooleanSetting glow = new BooleanSetting("Glow", this, true);
    public final ColorSetting glowColor = new ColorSetting("Glow Color", this, new Color(46, 204, 113));
    public final BooleanSetting blur = new BooleanSetting("Blur", this, true);
    public final greencloud.impl.settings.NumberSetting blurStrength = new greencloud.impl.settings.NumberSetting("Blur Strength", this, 10, 1, 30, 1, () -> blur.enabled);

    public ClickGUIModule() {
        super("ClickGUI", "Opens the menu", Category.RENDER);
        this.setKeyCode(Keyboard.KEY_RSHIFT);

        this.addSetting(guiMode);
        this.addSetting(glow);
        this.addSetting(glowColor);
        this.addSetting(blur);
        this.addSetting(blurStrength);
    }

    @Override
    public void onEnable() {
        if (mc.currentScreen == null) {
            if (guiMode.is("KingCanvas")) {
                mc.displayGuiScreen(GreenCloud.kingCanvasGUI);
            } else {
                mc.displayGuiScreen(GreenCloud.modernGUI);
            }
        }
        this.setToggled(false);
    }
}
package greencloudclient.com.modules.render;

import greencloudclient.com.GreenCloud;
import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.com.settings.BooleanSetting;
import greencloudclient.com.settings.ColorSetting;
import greencloudclient.com.settings.ModeSetting;
import org.lwjgl.input.Keyboard;

import java.awt.Color;

public class ClickGUIModule extends Module {

    public final ModeSetting guiMode = new ModeSetting("Mode", this, "Modern", "Modern", "KingCanvas");
    public final BooleanSetting glow = new BooleanSetting("Glow", this, true);
    public final ColorSetting glowColor = new ColorSetting("Glow Color", this, new Color(46, 204, 113));
    public final BooleanSetting blur = new BooleanSetting("Blur", this, true);
    public final greencloudclient.com.settings.NumberSetting blurStrength = new greencloudclient.com.settings.NumberSetting("Blur Strength", this, 10, 1, 30, 1, () -> blur.enabled);

    public ClickGUIModule() {
        super("ClickGUI", Category.RENDER);
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
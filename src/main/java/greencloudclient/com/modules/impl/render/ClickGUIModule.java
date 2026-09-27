package greencloudclient.com.modules.impl.render;

import greencloudclient.com.GreenCloud;
import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.com.settings.BooleanSetting;
import greencloudclient.com.settings.ColorSetting;
import greencloudclient.com.settings.ModeSetting;
import greencloudclient.com.settings.NumberSetting;
import org.lwjgl.input.Keyboard;

import java.awt.Color;

public class ClickGUIModule extends Module {

    private static final int DEFAULT_ACCENT = 0xFF6599EF;
    private static final int DEFAULT_SECONDARY = 0xFF9965EF;
    private static final int DEFAULT_BACKGROUND = 0xFF141416;

    public final ModeSetting guiMode = new ModeSetting("Mode", this, "Modern", "Modern", "KingCanvas");
    public final ColorSetting accent = new ColorSetting("Accent", this, new Color(DEFAULT_ACCENT));
    public final ColorSetting secondary = new ColorSetting("Secondary", this, new Color(DEFAULT_SECONDARY));
    public final ColorSetting background = new ColorSetting("Background", this, new Color(DEFAULT_BACKGROUND));
    public final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", this, 225, 0, 255, 5);
    public final BooleanSetting notifications = new BooleanSetting("Notifications", this, true);
    public final BooleanSetting blur = new BooleanSetting("Blur", this, false);
    public final NumberSetting blurStrength = new NumberSetting("Blur Strength", this, 10, 1, 30, 1, () -> blur.enabled);

    public ClickGUIModule() {
        super("ClickGUI", Category.RENDER);
        this.setKeyCode(Keyboard.KEY_RSHIFT);

        addSettings(guiMode, accent, secondary, background, backgroundAlpha, notifications, blur, blurStrength);
    }

    private static ClickGUIModule get() {
        return GreenCloud.moduleManager != null ? GreenCloud.moduleManager.getModule(ClickGUIModule.class) : null;
    }

    public static int getColor() {
        ClickGUIModule m = get();
        return m != null ? m.accent.getColor() : DEFAULT_ACCENT;
    }

    public static int getSecondaryColor() {
        ClickGUIModule m = get();
        return m != null ? m.secondary.getColor() : DEFAULT_SECONDARY;
    }

    public static int getBackgroundColor() {
        ClickGUIModule m = get();
        if (m == null) return 0xE1141416;
        int alpha = Math.max(0, Math.min(255, (int) m.backgroundAlpha.value));
        return alpha << 24 | (m.background.getColor() & 0xFFFFFF);
    }

    public static boolean notificationsEnabled() {
        ClickGUIModule m = get();
        return m == null || m.notifications.enabled;
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

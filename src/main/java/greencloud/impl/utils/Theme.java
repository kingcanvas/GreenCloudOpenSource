package greencloud.impl.utils;

import java.awt.Color;

public enum Theme {
    GREENCLOUD("GreenCloud", new Color(21, 21, 21), new Color(40, 40, 40), new Color(50, 200, 100), new Color(0, 175, 255)),
    VIBRANT_PURPLE("Vibrant Purple", new Color(28, 22, 28), new Color(46, 38, 46), new Color(150, 90, 255), new Color(220, 120, 255)),
    OCEAN_BLUE("Ocean Blue", new Color(22, 26, 33), new Color(31, 36, 46), new Color(10, 131, 224), new Color(10, 191, 255)),
    SUNSET_RED("Sunset Red", new Color(29, 24, 24), new Color(48, 40, 40), new Color(215, 84, 61), new Color(229, 142, 69));

    public final String name;
    public final Color panelColor;
    public final Color moduleIdleColor;
    public final Color accentColor;
    public final Color settingAccentColor;
    public final Color textColor;

    Theme(String name, Color panel, Color module, Color accent, Color settingAccent) {
        this.name = name;
        this.panelColor = panel;
        this.moduleIdleColor = module;
        this.accentColor = accent;
        this.settingAccentColor = settingAccent;
        this.textColor = Color.WHITE;
    }


    public Theme next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}
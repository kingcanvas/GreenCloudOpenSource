package greencloud.impl.gui.clickgui.components;

import greencloud.impl.gui.clickgui.buttons.ModuleButton;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.utils.render.AnimationUtil;
import greencloud.impl.utils.render.GreenRender;
import greencloud.impl.utils.font.FontUtil;
import java.awt.Color;

public class BooleanComponent extends Component {
    private final BooleanSetting bs;
    private float toggleAnim = 0.0f;

    public BooleanComponent(BooleanSetting setting, ModuleButton parent) {
        super(setting, parent); this.bs = setting;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        FontUtil.getSafeSmall().drawString(bs.name, x + 10, y + 4, -1);
        toggleAnim = AnimationUtil.moveUD(toggleAnim, bs.enabled ? 1.0f : 0.0f, 0.2f, 0.05f);

        float sw = 18, sh = 9, sx = x + width - sw - 10, sy = y + 4.5f;
        int colorOff = new Color(55, 55, 60).getRGB(), colorOn = HUD.getColor();
        int trackColor = GreenRender.lerpARGB(colorOff, colorOn, toggleAnim);

        GreenRender.fillRR(sx, sy, sw, sh, sh / 2f, new Color(trackColor, true));
        float knobSize = sh - 3, knobX = sx + 1.5f + ((sw - knobSize - 3) * toggleAnim);
        GreenRender.fillCircle(knobX + knobSize/2f, sy + sh/2f, knobSize/2f, Color.WHITE);
    }

    @Override public void mouseClicked(int mx, int my, int mb) { if (mx >= x && mx <= x + width && my >= y && my <= y + 16 && mb == 0) bs.toggle(); }
}
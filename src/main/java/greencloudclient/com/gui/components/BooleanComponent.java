package greencloudclient.com.gui.components;

import greencloudclient.com.gui.buttons.ModuleButton;
import greencloudclient.com.settings.BooleanSetting;
import greencloudclient.com.modules.impl.render.ClickGUIModule;
import greencloudclient.com.utils.render.AnimationUtil;
import greencloudclient.com.utils.render.GreenRender;
import greencloudclient.com.utils.font.FontUtil;

public class BooleanComponent extends Component {
    private static final float SWITCH_W = 22f, SWITCH_H = 12f, KNOB_INSET = 2f;
    private static final int TRACK_OFF = 0xFF303036;

    private final BooleanSetting bs;
    private float toggleAnim;
    private float hoverAnim;

    public BooleanComponent(BooleanSetting setting, ModuleButton parent) {
        super(setting, parent);
        this.bs = setting;
        this.height = 18f;
        this.toggleAnim = bs.enabled ? 1f : 0f;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        boolean hovered = isHovered(mx, my);
        hoverAnim = GreenRender.smooth(hoverAnim, hovered ? 1f : 0f, 0.25f);
        toggleAnim = AnimationUtil.moveUD(toggleAnim, bs.enabled ? 1f : 0f, 0.2f, 0.05f);

        if (hoverAnim > 0.01f) {
            GreenRender.fillRR(x + 4, y, width - 8, height, 4, GreenRender.withAlphaARGB(0xFFFFFFFF, hoverAnim * 0.05f));
        }

        float fontH = FontUtil.getSafeSmall().getHeight();
        int labelColor = GreenRender.lerpARGB(0xFFC8C8CC, 0xFFFFFFFF, Math.max(toggleAnim, hoverAnim));
        float sx = x + width - SWITCH_W - 10, sy = y + (height - SWITCH_H) / 2f;
        FontUtil.getSafeSmall().drawString(fitLabel(bs.name, sx - x - 16f), x + 10, y + (height - fontH) / 2f, labelColor);
        int accent = ClickGUIModule.getColor();
        GreenRender.fillRR(sx, sy, SWITCH_W, SWITCH_H, SWITCH_H / 2f, GreenRender.lerpARGB(TRACK_OFF, accent, toggleAnim));

        float knobR = (SWITCH_H - KNOB_INSET * 2) / 2f + hoverAnim * 0.5f;
        float travel = SWITCH_W - SWITCH_H;
        float kx = sx + SWITCH_H / 2f + travel * toggleAnim, ky = sy + SWITCH_H / 2f;
        GreenRender.glowRR(kx - knobR, ky - knobR + 0.5f, knobR * 2, knobR * 2, knobR, 2f, 0x60000000);
        GreenRender.fillCircle(kx, ky, knobR, 0xFFFFFFFF);
    }

    private boolean isHovered(int mx, int my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    @Override
    public void mouseClicked(int mx, int my, int mb) {
        if (mb == 0 && isHovered(mx, my)) bs.toggle();
    }
}

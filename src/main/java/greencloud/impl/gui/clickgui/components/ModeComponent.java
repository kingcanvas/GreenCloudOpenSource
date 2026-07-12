package greencloud.impl.gui.clickgui.components;

import greencloud.impl.gui.clickgui.buttons.ModuleButton;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.utils.render.GreenRender;
import greencloud.impl.utils.font.FontUtil;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class ModeComponent extends Component {
    private final ModeSetting ms;
    private final Map<String, Float> modeAnims = new HashMap<>();

    private static final float HEADER_H = 22f;
    private static final float BTN_H = 18f;
    private static final float BTN_GAP = 4f;
    private static final float PAD_X = 10f;
    private static final float PAD_TOP = 6f;
    private static final float ROW_SPACE = BTN_H + BTN_GAP;

    public ModeComponent(ModeSetting setting, ModuleButton parent) {
        super(setting, parent);
        this.ms = (ModeSetting) setting;
        for (String mode : ms.modes) modeAnims.put(mode, 0f);
    }

    private Color accent() {
        return new Color(HUD.getColor(), true);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        float fh = FontUtil.getSafeSmall().getHeight();
        float midY = y + (HEADER_H - fh) / 2f;

        FontUtil.getSafeSmall().drawString(ms.name, x + PAD_X, midY, -1);

        String cur = ms.currentMode;
        float tw = FontUtil.getSafeSmall().getStringWidth(cur);
        float bw = tw + 12f;
        float bx = x + width - bw - PAD_X;
        float by = y + (HEADER_H / 2f) - 9f;

        Color ac = accent();
        GreenRender.fillRR(bx - 1, by - 1, bw + 2, 19f, 4, new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 180));
        GreenRender.fillRR(bx, by, bw, 17f, 3, new Color(35, 37, 42));
        FontUtil.getSafeSmall().drawString(cur, bx + 6f, by + (17f - fh) / 2f, -1);

        if (!expanded) return;

        GreenRender.fillRect(x + PAD_X, y + HEADER_H - 1, width - PAD_X * 2, 1, new Color(50, 52, 58));

        float curX = x + PAD_X;
        float curY = y + HEADER_H + PAD_TOP;

        for (String mode : ms.modes) {
            float mtw = FontUtil.getSafeSmall().getStringWidth(mode) + 14;
            if (curX + mtw > x + width - PAD_X) { curX = x + PAD_X; curY += ROW_SPACE; }

            boolean sel = mode.equals(ms.currentMode);
            float cur2 = modeAnims.getOrDefault(mode, 0f);
            float target = sel ? 1f : 0f;
            float anim = GreenRender.smooth(cur2, target, 0.18f);
            modeAnims.put(mode, anim);

            int r = (int)(35  + (ac.getRed()   - 35)  * anim);
            int g = (int)(37  + (ac.getGreen() - 37)  * anim);
            int b = (int)(42  + (ac.getBlue()  - 42)  * anim);
            GreenRender.fillRR(curX, curY, mtw, BTN_H, 3, new Color(r, g, b));

            int textCol = sel ? -1 : new Color(150, 150, 155).getRGB();
            FontUtil.getSafeSmall().drawString(mode,
                    curX + 7,
                    curY + (BTN_H - FontUtil.getSafeSmall().getHeight()) / 2f,
                    textCol);

            curX += mtw + BTN_GAP;
        }
    }

    @Override
    public void mouseClicked(int mx, int my, int mb) {
        boolean header = mx >= x && mx <= x + width && my >= y && my <= y + HEADER_H;
        if (header && mb == 1) { expanded = !expanded; return; }
        if (!expanded || mb != 0) return;

        float curX = x + PAD_X;
        float curY = y + HEADER_H + PAD_TOP;

        for (String mode : ms.modes) {
            float mtw = FontUtil.getSafeSmall().getStringWidth(mode) + 14;
            if (curX + mtw > x + width - PAD_X) { curX = x + PAD_X; curY += ROW_SPACE; }
            if (mx >= curX && mx <= curX + mtw && my >= curY && my <= curY + BTN_H) { ms.currentMode = mode; return; }
            curX += mtw + BTN_GAP;
        }
    }

    @Override
    public float getHeight() {
        if (!expanded) return HEADER_H;
        float curX = x + PAD_X; int rows = 1;
        for (String mode : ms.modes) {
            float mtw = FontUtil.getSafeSmall().getStringWidth(mode) + 14;
            if (curX + mtw > x + width - PAD_X) { curX = x + PAD_X; rows++; }
            curX += mtw + BTN_GAP;
        }
        return HEADER_H + PAD_TOP + (rows * ROW_SPACE) + PAD_TOP;
    }
}
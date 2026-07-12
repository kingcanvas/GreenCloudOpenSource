package greencloud.impl.gui.clickgui.components;

import greencloud.impl.gui.clickgui.buttons.ModuleButton;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.utils.render.GreenRender;
import greencloud.impl.utils.font.FontUtil;
import org.lwjgl.input.Mouse;
import java.awt.Color;

public class NumberComponent extends Component {
    private final NumberSetting ns;
    private boolean dragging;
    private boolean draggingMin, draggingMax;
    private float visualPerc = 0f;
    private float visualMin = 0f, visualMax = 0f;

    public NumberComponent(NumberSetting setting, ModuleButton parent) {
        super(setting, parent);
        this.ns = setting;
        this.height = 22f;
        if (ns.isRange) {
            this.visualMin = (float) ((ns.value    - ns.min) / (ns.max - ns.min));
            this.visualMax = (float) ((ns.maxValue - ns.min) / (ns.max - ns.min));
        }
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        float sw = width - 20, sx = x + 10, sy = y + 13;

        if (ns.isRange) {
            if (!Mouse.isButtonDown(0)) { draggingMin = false; draggingMax = false; }

            if (draggingMin) {
                float p = Math.min(1f, Math.max(0f, (mx - sx) / sw));
                ns.setValue(ns.min + (ns.max - ns.min) * p);
            } else if (draggingMax) {
                float p = Math.min(1f, Math.max(0f, (mx - sx) / sw));
                ns.setMaxValue(ns.min + (ns.max - ns.min) * p);
            }

            float tMin = (float) ((ns.value    - ns.min) / (ns.max - ns.min));
            float tMax = (float) ((ns.maxValue - ns.min) / (ns.max - ns.min));
            visualMin = GreenRender.smooth(visualMin, tMin, 0.2f);
            visualMax = GreenRender.smooth(visualMax, tMax, 0.2f);

            FontUtil.getSafeSmall().drawString(ns.name, x + 10, y + 2, -1);
            String val = ns.getRoundedValue() + " - " + ns.getRoundedMaxValue();
            FontUtil.getSafeSmall().drawString(val,
                    x + width - FontUtil.getSafeSmall().getStringWidth(val) - 10,
                    y + 2, new Color(140, 140, 140).getRGB());

            GreenRender.fillRR(sx, sy, sw, 6, 3, new Color(30, 30, 35));
            GreenRender.fillRR(sx + sw * visualMin, sy, sw * (visualMax - visualMin), 6, 3, new Color(HUD.getColor(), true));
        } else {
            if (dragging && !Mouse.isButtonDown(0)) dragging = false;

            float targetPerc = (float) ((ns.value - ns.min) / (ns.max - ns.min));
            visualPerc = GreenRender.smooth(visualPerc, targetPerc, 0.2f);

            FontUtil.getSafeSmall().drawString(ns.name, x + 10, y + 2, -1);
            String val = String.valueOf(ns.getRoundedValue());
            FontUtil.getSafeSmall().drawString(val,
                    x + width - FontUtil.getSafeSmall().getStringWidth(val) - 10,
                    y + 2, new Color(140, 140, 140).getRGB());

            GreenRender.fillRR(sx, sy, sw, 6, 3, new Color(30, 30, 35));
            GreenRender.fillRR(sx, sy, sw * visualPerc, 6, 3, new Color(HUD.getColor(), true));

            if (dragging) {
                float p = Math.min(1, Math.max(0, (mx - sx) / sw));
                ns.setValue(ns.min + (ns.max - ns.min) * p);
            }
        }
    }

    @Override
    public void mouseClicked(int mx, int my, int mb) {
        if (mb != 0 || my < y + 10 || my > y + 18) return;
        if (ns.isRange) {
            float sw = width - 20, sx = x + 10;
            float minX = sx + sw * (float) ((ns.value    - ns.min) / (ns.max - ns.min));
            float maxX = sx + sw * (float) ((ns.maxValue - ns.min) / (ns.max - ns.min));
            if (Math.abs(mx - minX) <= Math.abs(mx - maxX)) draggingMin = true;
            else draggingMax = true;
        } else {
            if (mx >= x + 10 && mx <= x + width - 10) dragging = true;
        }
    }

    @Override
    public void mouseReleased(int mx, int my, int st) {
        dragging = false;
        draggingMin = false;
        draggingMax = false;
    }
}

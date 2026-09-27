package greencloudclient.com.gui.components;

import greencloudclient.com.gui.buttons.ModuleButton;
import greencloudclient.com.modules.impl.render.ClickGUIModule;
import greencloudclient.com.settings.NumberSetting;
import greencloudclient.com.utils.render.GreenRender;
import greencloudclient.com.utils.font.FontUtil;
import org.lwjgl.input.Mouse;

public class NumberComponent extends Component {
    private static final float TRACK_H = 4f, TRACK_Y = 18f, KNOB_R = 4.5f, PAD = 10f;
    private static final int TRACK_BG = 0xFF26262C;

    private final NumberSetting ns;
    private boolean dragging;
    private boolean draggingMin, draggingMax;
    private float visualPerc, visualMin, visualMax;
    private float hoverAnim, dragAnim;

    public NumberComponent(NumberSetting setting, ModuleButton parent) {
        super(setting, parent);
        this.ns = setting;
        this.height = 26f;
        this.visualPerc = percent(ns.value);
        this.visualMin = percent(ns.value);
        this.visualMax = percent(ns.maxValue);
    }

    private float percent(double v) {
        double range = ns.max - ns.min;
        return range <= 0 ? 0f : (float) Math.max(0, Math.min(1, (v - ns.min) / range));
    }

    private double valueAt(float mx) {
        float p = Math.max(0f, Math.min(1f, (mx - trackX()) / trackW()));
        return ns.min + (ns.max - ns.min) * p;
    }

    private float trackX() { return x + PAD; }
    private float trackW() { return width - PAD * 2; }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        if (!Mouse.isButtonDown(0)) { dragging = false; draggingMin = false; draggingMax = false; }
        boolean anyDrag = dragging || draggingMin || draggingMax;

        if (dragging) ns.setValue(valueAt(mx));
        else if (draggingMin) ns.setValue(Math.min(valueAt(mx), ns.maxValue));
        else if (draggingMax) ns.setMaxValue(Math.max(valueAt(mx), ns.value));

        boolean hovered = mx >= x && mx <= x + width && my >= y && my <= y + height;
        hoverAnim = GreenRender.smooth(hoverAnim, hovered || anyDrag ? 1f : 0f, 0.25f);
        dragAnim = GreenRender.smooth(dragAnim, anyDrag ? 1f : 0f, 0.3f);

        int accent = ClickGUIModule.getColor();
        float sx = trackX(), sw = trackW(), sy = y + TRACK_Y;

        String val = ns.isRange ? format(ns.value) + " - " + format(ns.maxValue) : format(ns.value);
        float valW = FontUtil.getSafeSmall().getWidth(val);
        FontUtil.getSafeSmall().drawString(fitLabel(ns.name, sw - valW - 6f), sx, y + 4, 0xFFE6E6EA);
        FontUtil.getSafeSmall().drawString(val, x + width - valW - PAD, y + 4,
                GreenRender.lerpARGB(0xFF8C8C94, accent, dragAnim));

        GreenRender.fillRR(sx, sy, sw, TRACK_H, TRACK_H / 2f, TRACK_BG);

        float knobR = KNOB_R + hoverAnim * 0.75f;
        if (ns.isRange) {
            visualMin = GreenRender.smooth(visualMin, percent(ns.value), 0.25f);
            visualMax = GreenRender.smooth(visualMax, percent(ns.maxValue), 0.25f);
            float a = sx + sw * visualMin, b = sx + sw * visualMax;
            GreenRender.fillRRGradientH(a, sy, Math.max(TRACK_H, b - a), TRACK_H, TRACK_H / 2f,
                    accent, ClickGUIModule.getSecondaryColor());
            drawKnob(a, sy + TRACK_H / 2f, draggingMin ? knobR + 0.5f : knobR);
            drawKnob(b, sy + TRACK_H / 2f, draggingMax ? knobR + 0.5f : knobR);
        } else {
            visualPerc = GreenRender.smooth(visualPerc, percent(ns.value), 0.25f);
            float end = sx + sw * visualPerc;
            if (end - sx > 0.5f) {
                GreenRender.fillRRGradientH(sx, sy, Math.max(TRACK_H, end - sx), TRACK_H, TRACK_H / 2f,
                        accent, GreenRender.lerpARGB(accent, ClickGUIModule.getSecondaryColor(), visualPerc));
            }
            drawKnob(end, sy + TRACK_H / 2f, knobR);
        }
    }

    private static String format(double v) {
        double r = Math.round(v * 100.0) / 100.0;
        if (r == Math.rint(r)) return String.valueOf((long) r);
        String s = String.valueOf(r);
        return s.endsWith("0") && s.contains(".") ? s.substring(0, s.length() - 1) : s;
    }

    private void drawKnob(float cx, float cy, float r) {
        GreenRender.glowRR(cx - r, cy - r + 0.5f, r * 2, r * 2, r, 2.5f, 0x70000000);
        GreenRender.fillCircle(cx, cy, r, 0xFFFFFFFF);
    }

    @Override
    public void mouseClicked(int mx, int my, int mb) {
        if (mb != 0 || mx < x || mx > x + width || my < y + 12 || my > y + height) return;
        if (ns.isRange) {
            float minX = trackX() + trackW() * percent(ns.value);
            float maxX = trackX() + trackW() * percent(ns.maxValue);
            if (Math.abs(mx - minX) < Math.abs(mx - maxX) || (minX == maxX && mx < minX)) draggingMin = true;
            else draggingMax = true;
        } else {
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(int mx, int my, int st) {
        dragging = false;
        draggingMin = false;
        draggingMax = false;
    }
}

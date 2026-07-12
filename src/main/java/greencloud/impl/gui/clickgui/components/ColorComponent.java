package greencloud.impl.gui.clickgui.components;

import greencloud.impl.gui.clickgui.buttons.ModuleButton;
import greencloud.impl.settings.ColorSetting;
import greencloud.impl.settings.Setting;
import greencloud.impl.utils.animation.animations.EasingAnimation;
import greencloud.impl.utils.animation.animations.EasingAnimation.Easing;
import greencloud.impl.utils.render.GreenRender;
import greencloud.impl.utils.render.RenderUtil;
import greencloud.impl.utils.font.FontUtil;
import java.awt.Color;

public class ColorComponent extends Component {
    private final ColorSetting colorSetting;
    private boolean expanded = false;
    private boolean draggingHue = false;
    private boolean draggingSB = false;

    private static final float HEADER_H = 16f;
    private static final float PICKER_H = 90f;

    private static final float TOP_PAD = 6f;
    private static final float HUE_H = 8f;
    private static final float GAP = 10f;
    private static final float SB_H = 50f;

    private final EasingAnimation pickerAnim = new EasingAnimation(0f);

    public ColorComponent(Setting setting, ModuleButton parent) {
        super(setting, parent);
        this.colorSetting = (ColorSetting) setting;
    }

    @Override
    public float getHeight() {
        return HEADER_H + pickerAnim.getValue();
    }

    @Override
    public float getFinalHeight() {
        return HEADER_H + (expanded ? PICKER_H : 0f);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        pickerAnim.animateTo(expanded ? PICKER_H : 0f, expanded ? 250 : 200, expanded ? Easing.EASE_OUT_QUART : Easing.EASE_OUT_CUBIC);
        float anim = pickerAnim.update();

        FontUtil.getSafeSmall().drawString(colorSetting.name, x + 12, y + 4, -1);

        float previewSize = 10f;
        GreenRender.fillRR(x + width - 20f, y + 3f, previewSize, previewSize, 2f, colorSetting.getColor());

        if (anim > 1f) {
            float pickerY = y + HEADER_H;
            GreenRender.pushScissor(x, pickerY, width, anim);

            float contentX = x + 12f;
            float contentW = width - 24f;

            float hueY = pickerY + TOP_PAD;
            for (int i = 0; i < (int) contentW; i++) {
                float h = (float) i / contentW;
                RenderUtil.drawRect(contentX + i, hueY, 1, HUE_H, Color.getHSBColor(h, 1f, 1f).getRGB());
            }

            RenderUtil.drawRect(contentX + (colorSetting.hue * contentW) - 1f, hueY - 1f, 2f, HUE_H + 2f, -1);

            float sbY = hueY + HUE_H + GAP;
            int baseHue = Color.getHSBColor(colorSetting.hue, 1f, 1f).getRGB();
            RenderUtil.drawRect(contentX, sbY, contentW, SB_H, baseHue);
            RenderUtil.drawGradientRectSideways(contentX, sbY, contentX + contentW, sbY + SB_H, 0xFFFFFFFF, 0x00FFFFFF);
            RenderUtil.drawGradientRect(contentX, sbY, contentX + contentW, sbY + SB_H, 0x00000000, 0xFF000000);

            float cx = contentX + (colorSetting.saturation * contentW);
            float cy = sbY + (SB_H * (1f - colorSetting.brightness));
            GreenRender.fillCircle(cx, cy, 3f, Color.WHITE);
            GreenRender.outlineRR(cx - 3f, cy - 3f, 6f, 6f, 3f, 1f, Color.BLACK);

            if (draggingHue) {
                float val = Math.min(1f, Math.max(0f, (mouseX - contentX) / contentW));
                colorSetting.setHue(val);
            } else if (draggingSB) {
                float sat = Math.min(1f, Math.max(0f, (mouseX - contentX) / contentW));
                float bri = Math.min(1f, Math.max(0f, 1f - ((mouseY - sbY) / SB_H)));
                colorSetting.setSaturation(sat);
                colorSetting.setBrightness(bri);
            }

            GreenRender.popScissor();
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 1 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_H) {
            expanded = !expanded;
            return;
        }
        if (expanded && mouseButton == 0) {
            float hueY = y + HEADER_H + TOP_PAD;
            if (mouseX >= x + 12 && mouseX <= x + width - 12 && mouseY >= hueY && mouseY <= hueY + HUE_H)
                draggingHue = true;
            float sbY = hueY + HUE_H + GAP;
            if (mouseX >= x + 12 && mouseX <= x + width - 12 && mouseY >= sbY && mouseY <= sbY + SB_H)
                draggingSB = true;
        }
    }

    @Override
    public void mouseReleased(int mx, int my, int state) { draggingHue = draggingSB = false; }
}

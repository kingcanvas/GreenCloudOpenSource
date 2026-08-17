package greencloudclient.com.gui.components;

import greencloudclient.com.gui.buttons.ModuleButton;
import greencloudclient.com.modules.impl.render.HUD;
import greencloudclient.com.settings.MultiModeSetting;
import greencloudclient.com.utils.font.FontUtil;
import greencloudclient.com.utils.render.GreenRender;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiModeComponent extends Component {
    private static final float HEADER_H = 22f;
    private static final float BTN_H = 18f;
    private static final float BTN_GAP = 4f;
    private static final float PAD_X = 10f;
    private static final float PAD_TOP = 6f;
    private static final float ROW_SPACE = BTN_H + BTN_GAP;

    private final MultiModeSetting multiMode;
    private final Map<String, Float> modeAnimations = new HashMap<>();

    public MultiModeComponent(MultiModeSetting setting, ModuleButton parent) {
        super(setting, parent);
        this.multiMode = setting;
        for (String mode : setting.modes) modeAnimations.put(mode, setting.isSelected(mode) ? 1f : 0f);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float fontHeight = FontUtil.getSafeSmall().getHeight();
        float middleY = y + (HEADER_H - fontHeight) / 2f;
        FontUtil.getSafeSmall().drawString(multiMode.name, x + PAD_X, middleY, -1);

        List<String> selectedModes = multiMode.getSelectedModes();
        String value = selectedModes.size() > 1 ? String.valueOf(selectedModes.size()) : multiMode.getDisplayValue();
        float valueWidth = FontUtil.getSafeSmall().getStringWidth(value) + 12f;
        float valueX = x + width - valueWidth - PAD_X;
        float valueY = y + HEADER_H / 2f - 9f;
        Color accent = new Color(HUD.getColor(), true);
        GreenRender.fillRR(valueX - 1f, valueY - 1f, valueWidth + 2f, 19f, 4f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 180));
        GreenRender.fillRR(valueX, valueY, valueWidth, 17f, 3f, new Color(35, 37, 42));
        FontUtil.getSafeSmall().drawString(value, valueX + 6f, valueY + (17f - fontHeight) / 2f, -1);

        if (!expanded) return;
        GreenRender.fillRect(x + PAD_X, y + HEADER_H - 1f, width - PAD_X * 2f, 1f, new Color(50, 52, 58));

        float currentX = x + PAD_X;
        float currentY = y + HEADER_H + PAD_TOP;
        for (String mode : multiMode.modes) {
            float buttonWidth = FontUtil.getSafeSmall().getStringWidth(mode) + 14f;
            if (currentX + buttonWidth > x + width - PAD_X) {
                currentX = x + PAD_X;
                currentY += ROW_SPACE;
            }
            boolean selected = multiMode.isSelected(mode);
            float animation = GreenRender.smooth(modeAnimations.getOrDefault(mode, 0f), selected ? 1f : 0f, 0.18f);
            modeAnimations.put(mode, animation);
            int red = (int) (35 + (accent.getRed() - 35) * animation);
            int green = (int) (37 + (accent.getGreen() - 37) * animation);
            int blue = (int) (42 + (accent.getBlue() - 42) * animation);
            GreenRender.fillRR(currentX, currentY, buttonWidth, BTN_H, 3f, new Color(red, green, blue));
            FontUtil.getSafeSmall().drawString(mode, currentX + 7f,
                    currentY + (BTN_H - fontHeight) / 2f,
                    selected ? -1 : new Color(150, 150, 155).getRGB());
            currentX += buttonWidth + BTN_GAP;
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean header = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_H;
        if (header && (mouseButton == 0 || mouseButton == 1)) {
            expanded = !expanded;
            return;
        }
        if (!expanded || mouseButton != 0) return;

        float currentX = x + PAD_X;
        float currentY = y + HEADER_H + PAD_TOP;
        for (String mode : multiMode.modes) {
            float buttonWidth = FontUtil.getSafeSmall().getStringWidth(mode) + 14f;
            if (currentX + buttonWidth > x + width - PAD_X) {
                currentX = x + PAD_X;
                currentY += ROW_SPACE;
            }
            if (mouseX >= currentX && mouseX <= currentX + buttonWidth
                    && mouseY >= currentY && mouseY <= currentY + BTN_H) {
                multiMode.toggle(mode);
                return;
            }
            currentX += buttonWidth + BTN_GAP;
        }
    }

    @Override
    public float getHeight() {
        if (!expanded) return HEADER_H;
        float currentX = x + PAD_X;
        int rows = 1;
        for (String mode : multiMode.modes) {
            float buttonWidth = FontUtil.getSafeSmall().getStringWidth(mode) + 14f;
            if (currentX + buttonWidth > x + width - PAD_X) {
                currentX = x + PAD_X;
                rows++;
            }
            currentX += buttonWidth + BTN_GAP;
        }
        return HEADER_H + PAD_TOP + rows * ROW_SPACE + PAD_TOP;
    }
}

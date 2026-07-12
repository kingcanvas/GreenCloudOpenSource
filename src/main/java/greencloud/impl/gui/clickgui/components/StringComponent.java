package greencloud.impl.gui.clickgui.components;

import greencloud.impl.gui.clickgui.buttons.ModuleButton;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.settings.StringSetting;
import greencloud.impl.settings.Setting;
import greencloud.impl.utils.font.FontUtil;
import greencloud.impl.utils.render.GreenRender;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import java.awt.Color;

public class StringComponent extends Component {
    private final StringSetting stringSetting;
    private boolean focused;

    public StringComponent(Setting setting, ModuleButton parent) {
        super(setting, parent);
        this.stringSetting = (StringSetting) setting;
        this.height = 20f;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GreenRender.fillRect(x, y, width, height, new Color(22, 24, 27));

        float bx = x + 8, by = y + 4, bw = width - 16, bh = 12;
        GreenRender.fillRR(bx, by, bw, bh, 3, new Color(32, 34, 37));

        if (focused) {
            GreenRender.outlineRR(bx, by, bw, bh, 3, 1f, new Color(HUD.getColor(), true));
        }

        String text = stringSetting.getValue();
        if (text.isEmpty() && !focused) text = stringSetting.name;

        String renderText = text + (focused && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        FontUtil.getSafeSmall().drawString(renderText, bx + 5, by + 3, focused ? -1 : new Color(130, 130, 135).getRGB());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            focused = (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!focused) return;

        if (keyCode == Keyboard.KEY_BACK) {
            String val = stringSetting.getValue();
            if (!val.isEmpty()) stringSetting.setValue(val.substring(0, val.length() - 1));
        } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
            focused = false;
        } else if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
            stringSetting.setValue(stringSetting.getValue() + typedChar);
        }
    }
}
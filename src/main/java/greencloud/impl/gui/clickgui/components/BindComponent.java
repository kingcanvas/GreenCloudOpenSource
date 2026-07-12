package greencloud.impl.gui.clickgui.components;

import greencloud.impl.gui.clickgui.buttons.ModuleButton;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.utils.font.FontUtil;
import greencloud.impl.utils.render.GreenRender;
import org.lwjgl.input.Keyboard;
import java.awt.Color;

public class BindComponent extends Component {
    private boolean binding;

    public BindComponent(ModuleButton parent) {
        super(null, parent);
        this.height = 22f;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        float midY = y + (height / 2f) - (FontUtil.getSafeSmall().getHeight() / 2f);

        FontUtil.getSafeSmall().drawString("Keybind", x + 10, midY, -1);

        String key = Keyboard.getKeyName(parent.module.getKeyCode());
        String text = binding ? "..." : (key == null || key.equals("NONE") ? "None" : key);
        float tw = FontUtil.getSafeSmall().getStringWidth(text);
        float bw = tw + 10;
        float bx = x + width - bw - 8;
        float by = y + (height / 2f) - 7f;

        GreenRender.fillRR(bx, by, bw, 14, 3, new Color(40, 42, 48));
        FontUtil.getSafeSmall().drawString(text, bx + 5, by + 3, binding ? HUD.getColor() : -1);
    }

    @Override
    public void mouseClicked(int mx, int my, int mb) {
        if (mx >= x && mx <= x + width && my >= y && my <= y + height) {
            if (mb == 0) binding = true;
            else if (mb == 1) parent.module.setKeyCode(0);
        }
    }

    @Override
    public void keyTyped(char ch, int key) {
        if (binding) {
            if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_DELETE) parent.module.setKeyCode(0);
            else parent.module.setKeyCode(key);
            binding = false;
        }
    }
}
package greencloudclient.com.gui.components;

import greencloudclient.com.gui.buttons.ModuleButton;
import greencloudclient.com.settings.Setting;
import greencloudclient.com.utils.font.FontUtil;
import net.minecraft.client.Minecraft;

public abstract class Component {
    public final Setting setting;
    public final ModuleButton parent;
    public float x, y, width, height;
    public boolean expanded = false;
    protected final Minecraft mc = Minecraft.getMinecraft();

    public Component(Setting setting, ModuleButton parent) {
        this.setting = setting; this.parent = parent; this.width = parent.width; this.height = 16f;
    }

    protected static String fitLabel(String text, float maxWidth) {
        FontUtil.SafeFont font = FontUtil.getSafeSmall();
        if (font.getWidth(text) <= maxWidth) return text;
        String ellipsis = "..";
        int end = text.length();
        while (end > 1 && font.getWidth(text.substring(0, end).trim() + ellipsis) > maxWidth) end--;
        return text.substring(0, end).trim() + ellipsis;
    }

    public abstract void drawScreen(int mouseX, int mouseY, float partialTicks);
    public abstract void mouseClicked(int mouseX, int mouseY, int mouseButton);
    public void mouseReleased(int mouseX, int mouseY, int state) {}
    public void keyTyped(char typedChar, int keyCode) {}
    public float getHeight() { return height; }
    public float getFinalHeight() { return getHeight(); }
}
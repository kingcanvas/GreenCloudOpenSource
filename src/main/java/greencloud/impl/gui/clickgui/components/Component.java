package greencloud.impl.gui.clickgui.components;

import greencloud.impl.gui.clickgui.buttons.ModuleButton;
import greencloud.impl.settings.Setting;
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

    public abstract void drawScreen(int mouseX, int mouseY, float partialTicks);
    public abstract void mouseClicked(int mouseX, int mouseY, int mouseButton);
    public void mouseReleased(int mouseX, int mouseY, int state) {}
    public void keyTyped(char typedChar, int keyCode) {}
    public float getHeight() { return height; }
    public float getFinalHeight() { return getHeight(); }
}
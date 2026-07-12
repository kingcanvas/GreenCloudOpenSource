package greencloud.impl.gui.clickgui.buttons;

import greencloud.impl.gui.clickgui.Clickguis.Panel;
import net.minecraft.client.Minecraft;


public abstract class Button {
    protected final Minecraft mc = Minecraft.getMinecraft();
    public int x, y, width, height;
    public final Panel parent;

    public Button(Panel parent, int x, int y, int width, int height) {
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }


    public abstract void drawScreen(int mouseX, int mouseY, float partialTicks);

    public abstract void mouseClicked(int mouseX, int mouseY, int mouseButton);
    public void mouseReleased(int mouseX, int mouseY, int state) {}
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {}
    public void keyTyped(char typedChar, int keyCode) {}
    protected boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
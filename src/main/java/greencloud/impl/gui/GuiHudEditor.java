package greencloud.impl.gui;

import greencloud.GreenCloud;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.modules.render.ArmorHUD;
import net.minecraft.client.gui.GuiScreen;
import java.awt.Color;
import java.io.IOException;

public class GuiHudEditor extends GuiScreen {

    private Object draggingModule = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, new Color(0, 0, 0, 200).getRGB());
        mc.fontRendererObj.drawStringWithShadow("Position Editor", 10, 10, -1);

        Color accent = new Color(HUD.getColor(), true);

        ArmorHUD armor = (ArmorHUD) GreenCloud.moduleManager.getModule(ArmorHUD.class);
        if (armor != null && armor.isToggled()) {
            handleDrag(armor, mouseX, mouseY);
            drawOutline(armor.getX(), armor.getY(), armor.getWidth(), armor.getHeight(), mouseX, mouseY, accent);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void handleDrag(Object module, int mouseX, int mouseY) {
        if (draggingModule == module) {
            if (module instanceof ArmorHUD) {
                ((ArmorHUD) module).setPosition(mouseX - dragOffsetX, mouseY - dragOffsetY);
            }
        }
    }

    private void drawOutline(int x, int y, int w, int h, int mx, int my, Color accent) {
        boolean hovering = mx >= x && mx <= x + w && my >= y && my <= y + h;
        Color col = hovering ? accent : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 100);
        drawHollowRect(x - 2, y - 2, x + w + 2, y + h + 2, col.getRGB());
    }

    private void drawHollowRect(int left, int top, int right, int bottom, int color) {
        drawRect(left, top, right, top + 1, color);
        drawRect(left, bottom - 1, right, bottom, color);
        drawRect(left, top, left + 1, bottom, color);
        drawRect(right - 1, top, right, bottom, color);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            ArmorHUD armor = (ArmorHUD) GreenCloud.moduleManager.getModule(ArmorHUD.class);
            if (armor != null && armor.isToggled() && isHovered(armor.getX(), armor.getY(), armor.getWidth(), armor.getHeight(), mouseX, mouseY)) {
                draggingModule = armor;
                dragOffsetX = mouseX - armor.getX();
                dragOffsetY = mouseY - armor.getY();
            }
        }
    }

    private boolean isHovered(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingModule = null;
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}

package greencloud.impl.managers.player;

import greencloud.GreenCloud;
import greencloud.impl.modules.render.HUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PositionManager extends GuiScreen {

    private static PositionManager instance;
    public final List<DraggableElement> elements = new ArrayList<>();
    private DraggableElement dragging = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public static PositionManager getInstance() {
        if (instance == null) {
            instance = new PositionManager();
        }
        return instance;
    }

    public static void addElement(String name, int x, int y, int width, int height, Runnable renderCallback) {
        DraggableElement existing = null;
        for (DraggableElement el : getInstance().elements) {
            if (el.name.equals(name)) {
                existing = el;
                break;
            }
        }

        if (existing != null) {
            existing.x = x;
            existing.y = y;
            existing.width = width;
            existing.height = height;
            existing.renderCallback = renderCallback;
        } else {
            getInstance().elements.add(new DraggableElement(name, x, y, width, height, renderCallback));
        }
    }

    public static void removeElement(String name) {
        getInstance().elements.removeIf(e -> e.name.equals(name));
    }

    public static void updateElement(String name, int x, int y, int width, int height) {
        for (DraggableElement element : getInstance().elements) {
            if (element.name.equals(name)) {
                element.x = x;
                element.y = y;
                element.width = width;
                element.height = height;
                break;
            }
        }
    }

    public static void open() {
        Minecraft.getMinecraft().displayGuiScreen(getInstance());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Removed `drawRect(0, 0, width, height, new Color(0, 0, 0, 200).getRGB());` to fix the black flicker issue
        Color accent = new Color(HUD.getColor(), true);

        greencloud.impl.utils.font.FontUtil.getSafeNormal().drawString("Position Editor - Drag elements to reposition", 10, 10, accent.getRGB());
        greencloud.impl.utils.font.FontUtil.getSafeNormal().drawString("Press ESC to save and exit", 10, 22, new Color(150, 150, 150).getRGB());

        for (DraggableElement element : elements) {

            if (dragging == element) {
                int newX = mouseX - dragOffsetX;
                int newY = mouseY - dragOffsetY;
                element.x = newX;
                element.y = newY;


                if (element.positionUpdateCallback != null) {
                    element.positionUpdateCallback.accept(newX, newY);
                }
            }


            if (element.renderCallback != null) {
                element.renderCallback.run();
            }

            boolean hovering = mouseX >= element.x && mouseX <= element.x + element.width
                    && mouseY >= element.y && mouseY <= element.y + element.height;


            Color outlineColor = hovering || dragging == element
                    ? accent
                    : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 100);

            drawHollowRect(element.x - 2, element.y - 2, element.x + element.width + 2, element.y + element.height + 2, outlineColor.getRGB());


            int labelY = element.y - 12;
            if (labelY < 0) labelY = element.y + element.height + 4;
            greencloud.impl.utils.font.FontUtil.getSafeNormal().drawString(element.name, element.x, labelY, accent.getRGB());
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawHollowRect(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, top + 1, color);
        Gui.drawRect(left, bottom - 1, right, bottom, color);
        Gui.drawRect(left, top, left + 1, bottom, color);
        Gui.drawRect(right - 1, top, right, bottom, color);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) {
            for (DraggableElement element : elements) {
                if (mouseX >= element.x && mouseX <= element.x + element.width
                        && mouseY >= element.y && mouseY <= element.y + element.height) {
                    dragging = element;
                    dragOffsetX = mouseX - element.x;
                    dragOffsetY = mouseY - element.y;
                    return;
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);

        // Final position update when dragging ends
        if (dragging != null && dragging.positionUpdateCallback != null) {
            dragging.positionUpdateCallback.accept(dragging.x, dragging.y);
        }

        dragging = null;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static class DraggableElement {
        public String name;
        public int x, y, width, height;
        public Runnable renderCallback;
        public PositionUpdateCallback positionUpdateCallback;

        public DraggableElement(String name, int x, int y, int width, int height, Runnable renderCallback) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.renderCallback = renderCallback;
        }

        public void setPositionUpdateCallback(PositionUpdateCallback callback) {
            this.positionUpdateCallback = callback;
        }
    }

    @FunctionalInterface
    public interface PositionUpdateCallback {
        void accept(int x, int y);
    }
}
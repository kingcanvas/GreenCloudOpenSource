package greencloud.impl.gui.clickgui.Clickguis;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.utils.animation.animations.EasingAnimation;
import greencloud.impl.utils.animation.animations.EasingAnimation.Easing;
import greencloud.impl.utils.render.GreenRender;
import greencloud.impl.utils.font.FontUtil;
import greencloud.impl.utils.render.shaders.BlurUtil;
import greencloud.GreenCloud;
import greencloud.impl.modules.render.ClickGUIModule;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModernGUI extends GuiScreen {
    private final List<Panel> panels = new ArrayList<>();

    private float targetGlobalScroll = 0;
    private float lastGlobalScroll = 0;
    private final EasingAnimation scrollAnim = new EasingAnimation(0f);

    public static String searchQuery = "";
    private boolean searchFocused = false;

    private final EasingAnimation searchWidthAnim = new EasingAnimation(0f);
    private int cursorTick = 0;

    @Override
    public void initGui() {
        if (panels.isEmpty()) {
            int startX = 20;
            for (Category category : Category.values()) {
                if (category == Category.MISC) continue;
                panels.add(new Panel(category, startX, 20));
                startX += 135;
            }
        }
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ClickGUIModule clickGui = GreenCloud.moduleManager.getModule(ClickGUIModule.class);
        if (clickGui != null && clickGui.blur.enabled && !BlurUtil.isFastRenderActive()) {
            BlurUtil.snapshot();
            GreenRender.fillRect(0, 0, width, height, new Color(0, 0, 0, 60));
        } else {
            GreenRender.fillRect(0, 0, width, height, new Color(0, 0, 0, 100));
        }

        scrollAnim.animateTo(targetGlobalScroll, 400, Easing.EASE_OUT_QUART);
        float globalScroll = scrollAnim.update();
        float deltaScroll = globalScroll - lastGlobalScroll;
        lastGlobalScroll = globalScroll;

        for (Panel panel : panels) {
            panel.y += deltaScroll;
            panel.drawScreen(mouseX, mouseY, partialTicks, (int) globalScroll);
        }

        drawSearchBar(mouseX, mouseY);
    }

    private void drawSearchBar(int mouseX, int mouseY) {

        searchWidthAnim.animateTo(
            searchFocused ? 1f : 0f,
            searchFocused ? 300 : 200,
            searchFocused ? Easing.EASE_SPRING : Easing.EASE_OUT_CUBIC
        );
        float t = searchWidthAnim.update();

        float searchW = 120 + (t * 80);
        float searchX = width / 2f - (searchW / 2f);
        float searchY = height - 40;
        float searchH = 22;

        boolean hovered = mouseX >= searchX && mouseX <= searchX + searchW
                       && mouseY >= searchY && mouseY <= searchY + searchH;

        ClickGUIModule clickGui = GreenCloud.moduleManager.getModule(ClickGUIModule.class);
        if (clickGui != null && clickGui.blur.enabled && !BlurUtil.isFastRenderActive()) {
            BlurUtil.blurRegion(searchX, searchY, searchW, searchH, (float) clickGui.blurStrength.value);
            GreenRender.fillRR(searchX, searchY, searchW, searchH, 6, new Color(20, 22, 25, 120).getRGB());
        } else {
            GreenRender.fillRR(searchX, searchY, searchW, searchH, 6, new Color(20, 22, 25, 160).getRGB());
        }

        if (searchFocused || hovered) {
            float alpha = searchFocused ? 1.0f : 0.4f;
            int outlineColor = GreenRender.withAlphaARGB(HUD.getColor(), alpha);
            GreenRender.outlineRR(searchX, searchY, searchW, searchH, 6, 1.5f, outlineColor);
        }

        String text     = searchQuery.isEmpty() && !searchFocused ? "Search modules..." : searchQuery;
        int    textColor = searchQuery.isEmpty() && !searchFocused ? new Color(150, 150, 150).getRGB() : -1;

        FontUtil.getSafeNormal().drawString(text, searchX + 10, searchY + 7, textColor);

        if (searchFocused) {
            float tw = FontUtil.getSafeNormal().getStringWidth(searchQuery);
            GreenRender.fillRect(searchX + 11 + tw, searchY + 7, 1, 9, -1);
        }

        float btnX = width - 140, btnY = height - 40, btnW = 120, btnH = 22;
        boolean btnHov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        GreenRender.fillRR(btnX, btnY, btnW, btnH, 6, new Color(20, 22, 25, 120).getRGB());
        if (btnHov) GreenRender.outlineRR(btnX, btnY, btnW, btnH, 6, 1.5f, new Color(80, 200, 120).getRGB());
        FontUtil.getSafeNormal().drawCenteredString("Edit HUD", btnX + btnW / 2f, btnY + 7, -1);

    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        float t       = searchWidthAnim.getValue();
        float searchW = 120 + (t * 80);
        float searchX = width / 2f - (searchW / 2f);
        float searchY = height - 40;

        if (mouseX >= width - 140 && mouseX <= width - 20 && mouseY >= height - 40 && mouseY <= height - 18) {
            greencloud.impl.managers.player.PositionManager.open();
            return;
        }


        if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + 22) {
            searchFocused = true;
            return;
        } else {
            searchFocused = false;
        }

        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseClicked(mouseX, mouseY, mouseButton)) break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchFocused) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (!searchQuery.isEmpty()) searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            } else if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                searchFocused = false;
            } else if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                searchQuery += typedChar;
            }
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(null); return; }
        for (Panel panel : panels) panel.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            targetGlobalScroll += (wheel > 0 ? 60 : -60);
            if (targetGlobalScroll > 0) targetGlobalScroll = 0;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for (Panel panel : panels) panel.mouseReleased(mouseX, mouseY, state);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}

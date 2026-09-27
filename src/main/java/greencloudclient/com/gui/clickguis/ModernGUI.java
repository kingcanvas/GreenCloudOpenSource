package greencloudclient.com.gui.clickguis;

import greencloudclient.com.modules.Category;
import greencloudclient.com.utils.AndroidUtil;
import greencloudclient.com.utils.animation.animations.EasingAnimation;
import greencloudclient.com.utils.animation.animations.EasingAnimation.Easing;
import greencloudclient.com.utils.render.GreenRender;
import greencloudclient.com.utils.font.FontUtil;
import greencloudclient.com.utils.render.shaders.BlurUtil;
import greencloudclient.com.GreenCloud;
import greencloudclient.com.modules.impl.render.ClickGUIModule;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
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
    
    private final float clickGuiScale = 1.0f;
    private int baseWidth;
    private int baseHeight;
    
    @Override
    public void initGui() {
        if (panels.isEmpty()) {
            int startX = 20;
            for (Category category : Category.values()) {
                panels.add(new Panel(category, startX, 20));
                startX += 130;
            }
        }
        Keyboard.enableRepeatEvents(true);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        baseWidth = sr.getScaledWidth();
        baseHeight = sr.getScaledHeight();
        
        int virtualMouseX = (int) (mouseX / clickGuiScale);
        int virtualMouseY = (int) (mouseY / clickGuiScale);
        
        ClickGUIModule clickGui = GreenCloud.moduleManager.getModule(ClickGUIModule.class);
        if (!AndroidUtil.isAndroid() && clickGui != null && clickGui.blur.enabled && !BlurUtil.isFastRenderActive()) {
            try {
                BlurUtil.snapshot();
                GreenRender.fillRect(0, 0, baseWidth, baseHeight, new Color(0, 0, 0, 60));
            } catch (Exception e) {
                GreenRender.fillRect(0, 0, baseWidth, baseHeight, new Color(0, 0, 0, 100));
            }
        } else {
            GreenRender.fillRect(0, 0, baseWidth, baseHeight, new Color(0, 0, 0, 100));
        }
        
        scrollAnim.animateTo(targetGlobalScroll, 400, Easing.EASE_OUT_QUART);
        float globalScroll = scrollAnim.update();
        float deltaScroll = globalScroll - lastGlobalScroll;
        lastGlobalScroll = globalScroll;
        
        for (Panel panel : panels) {
            panel.y += deltaScroll;
            panel.drawScreen(virtualMouseX, virtualMouseY, partialTicks, (int) globalScroll);
        }
        
        drawSearchBar(virtualMouseX, virtualMouseY);
    }
    
    private static final float SEARCH_H = 22, BUTTON_W = 90;

    private float searchW() { return 140 + searchWidthAnim.getValue() * 60; }
    private float searchX() { return 20; }
    private float barY()    { return baseHeight - SEARCH_H - 20; }
    private float buttonX() { return baseWidth - BUTTON_W - 20; }

    private void drawSearchBar(int mouseX, int mouseY) {
        searchWidthAnim.animateTo(
                searchFocused ? 1f : 0f,
                searchFocused ? 300 : 200,
                searchFocused ? Easing.EASE_SPRING : Easing.EASE_OUT_CUBIC
        );
        searchWidthAnim.update();

        float searchX = searchX(), searchY = barY(), searchW = searchW(), searchH = SEARCH_H;
        boolean hovered = isOver(mouseX, mouseY, searchX, searchY, searchW, searchH);

        ClickGUIModule clickGui = GreenCloud.moduleManager.getModule(ClickGUIModule.class);
        if (!AndroidUtil.isAndroid() && clickGui != null && clickGui.blur.enabled && !BlurUtil.isFastRenderActive()) {
            BlurUtil.blurRegion(searchX, searchY, searchW, searchH, (float) clickGui.blurStrength.value);
        }
        GreenRender.fillRR(searchX, searchY, searchW, searchH, 2, ClickGUIModule.getBackgroundColor());
        if (searchFocused || hovered) {
            int outlineColor = GreenRender.withAlphaARGB(ClickGUIModule.getColor(), searchFocused ? 1.0f : 0.5f);
            GreenRender.outlineRR(searchX, searchY, searchW, searchH, 2, 1.5f, outlineColor);
        }

        float ix = searchX + 11, iy = searchY + searchH / 2f - 1;
        GreenRender.outlineRR(ix - 3.5f, iy - 3.5f, 7, 7, 3.5f, 1.2f, 0xFFB4B4BA);
        GreenRender.drawLine(ix + 2.4f, iy + 2.4f, ix + 5f, iy + 5f, 1.3f, 0xFFB4B4BA);

        FontUtil.SafeFont font = FontUtil.getSafeNormal();
        float textX = searchX + 24, textY = searchY + (searchH - font.getHeight()) / 2f;
        boolean placeholder = searchQuery.isEmpty() && !searchFocused;
        font.drawString(placeholder ? "Search..." : searchQuery, textX, textY, placeholder ? 0xFF96969C : -1);

        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            GreenRender.fillRect(textX + font.getWidth(searchQuery) + 1, textY, 1, font.getHeight(), -1);
        }

        float btnX = buttonX(), btnY = barY();
        boolean btnHov = isOver(mouseX, mouseY, btnX, btnY, BUTTON_W, SEARCH_H);
        GreenRender.fillRR(btnX, btnY, BUTTON_W, SEARCH_H, 2, ClickGUIModule.getBackgroundColor());
        if (btnHov) GreenRender.outlineRR(btnX, btnY, BUTTON_W, SEARCH_H, 2, 1.5f, ClickGUIModule.getColor());
        font.drawCenteredString("Edit HUD", btnX + BUTTON_W / 2f, btnY + (SEARCH_H - font.getHeight()) / 2f, -1);
    }

    private static boolean isOver(int mx, int my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int virtualMouseX = (int) (mouseX / clickGuiScale);
        int virtualMouseY = (int) (mouseY / clickGuiScale);

        if (isOver(virtualMouseX, virtualMouseY, buttonX(), barY(), BUTTON_W, SEARCH_H)) {
            greencloudclient.com.managers.player.PositionManager.open();
            return;
        }

        searchFocused = isOver(virtualMouseX, virtualMouseY, searchX(), barY(), searchW(), SEARCH_H);
        if (searchFocused) return;

        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseClicked(virtualMouseX, virtualMouseY, mouseButton)) break;
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
        int virtualMouseX = (int) (mouseX / clickGuiScale);
        int virtualMouseY = (int) (mouseY / clickGuiScale);
        for (Panel panel : panels) panel.mouseReleased(virtualMouseX, virtualMouseY, state);
    }
    
    @Override public boolean doesGuiPauseGame() { return false; }
}

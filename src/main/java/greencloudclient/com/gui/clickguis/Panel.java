package greencloudclient.com.gui.clickguis;

import greencloudclient.com.GreenCloud;
import greencloudclient.com.gui.buttons.ModuleButton;
import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.com.utils.animation.animations.EasingAnimation;
import greencloudclient.com.utils.animation.animations.EasingAnimation.Easing;
import greencloudclient.com.utils.render.GreenRender;
import greencloudclient.com.utils.font.FontUtil;
import greencloudclient.com.utils.render.shaders.BlurUtil;
import greencloudclient.com.modules.impl.render.ClickGUIModule;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Panel {
    public static final float BORDER = 1.5f;
    private static final float CORNER = 2f;
    private static final float ICON_SIZE = 10f;

    public final Category category;
    public float x, y, width, height;
    public boolean dragging, expanded = true;
    public float dragX, dragY;

    private final EasingAnimation expandAnim = new EasingAnimation(1.0f);
    private final List<ModuleButton> buttons = new ArrayList<>();

    public Panel(Category category, int x, int y) {
        this.category = category; this.x = x; this.y = y; this.width = 120; this.height = 18;
        List<Module> modules = GreenCloud.instance.moduleManager.getModulesInCategory(category);
        modules.sort(Comparator.comparing(Module::getName));
        for (Module m : modules) if (!m.isHidden()) buttons.add(new ModuleButton(m, this));
    }

    public void drawScreen(int mouseX, int mouseY, float pt, int scrollOffset) {
        if (dragging) { x = mouseX - dragX; y = mouseY - dragY; }

        expandAnim.animateTo(expanded ? 1.0f : 0.0f, expanded ? 300 : 220, expanded ? Easing.EASE_OUT_QUART : Easing.EASE_OUT_CUBIC);
        float anim = expandAnim.update();

        List<ModuleButton> visibleButtons = new ArrayList<>();
        for (ModuleButton b : buttons) {
            if (ModernGUI.searchQuery.isEmpty() || b.module.getName().toLowerCase().contains(ModernGUI.searchQuery.toLowerCase())) {
                visibleButtons.add(b);
            }
        }

        float fullListH = 0;
        for (ModuleButton b : visibleButtons) {
            fullListH += expanded ? b.getTotalHeight() : b.height;
        }
        if (fullListH > 0) fullListH += BORDER;

        float currentListH = fullListH * anim;
        float totalH = height + currentListH;
        int accent = ClickGUIModule.getColor();

        ClickGUIModule clickGui = GreenCloud.moduleManager.getModule(ClickGUIModule.class);
        if (clickGui != null && clickGui.blur.enabled && !BlurUtil.isFastRenderActive()) {
            BlurUtil.blurRegionRounded(x, y, width, totalH, (float) clickGui.blurStrength.value, (int) CORNER);
        }
        GreenRender.glowRR(x, y, width, totalH, CORNER, 8f, 0x60000000);
        GreenRender.strokeRR(x, y, width, totalH, CORNER, BORDER, ClickGUIModule.getBackgroundColor(), accent);
        GreenRender.fillRRCorners(x, y, width, height, CORNER, CORNER, currentListH > 0.5f ? 0 : CORNER, currentListH > 0.5f ? 0 : CORNER, accent);

        drawHeader();

        if (anim > 0.01f && !visibleButtons.isEmpty()) {
            GreenRender.pushScissor(x, y + height, width, currentListH - BORDER);

            float curY = y + height;
            for (int i = 0; i < visibleButtons.size(); i++) {
                ModuleButton b = visibleButtons.get(i);
                b.x = x; b.y = curY; b.width = width;
                b.isLast = (i == visibleButtons.size() - 1);
                b.drawScreen(mouseX, mouseY, pt);
                curY += b.getTotalHeight();
            }

            GreenRender.popScissor();
        }
    }

    private void drawHeader() {
        String title = category.getDisplayName();
        FontUtil.SafeFont font = FontUtil.getSafeLarge();
        float gap = 4f;
        float contentW = ICON_SIZE + gap + font.getWidth(title);
        float left = x + (width - contentW) / 2f;
        float cy = y + height / 2f;

        drawIcon(category, left + ICON_SIZE / 2f, cy, 0xFFFFFFFF);
        font.drawString(title, left + ICON_SIZE + gap, cy - font.getHeight() / 2f, 0xFFFFFFFF);
    }

    private static void drawIcon(Category category, float cx, float cy, int color) {
        float t = 1.3f;
        switch (category) {
            case COMBAT:
                GreenRender.drawLine(cx - 1.5f, cy + 1.5f, cx + 4.5f, cy - 4.5f, t, color);
                GreenRender.drawLine(cx - 3.5f, cy - 0.5f, cx + 0.5f, cy + 3.5f, t, color);
                GreenRender.drawLine(cx - 1.5f, cy + 1.5f, cx - 4.5f, cy + 4.5f, t, color);
                break;
            case RENDER:
                GreenRender.outlineRR(cx - 5f, cy - 3.2f, 10f, 6.4f, 3.2f, t, color);
                GreenRender.fillCircle(cx, cy, 1.7f, color);
                break;
            case MOVEMENT:
                GreenRender.fillCircle(cx + 1.8f, cy - 4f, 1.4f, color);
                GreenRender.drawLine(cx + 0.8f, cy - 1.8f, cx - 0.4f, cy + 1.4f, t, color);
                GreenRender.drawLine(cx - 2.8f, cy - 0.6f, cx + 0.8f, cy - 1.8f, t, color);
                GreenRender.drawLine(cx + 0.8f, cy - 1.8f, cx + 3f, cy + 0.2f, t, color);
                GreenRender.drawLine(cx - 0.4f, cy + 1.4f, cx + 2.2f, cy + 2.8f, t, color);
                GreenRender.drawLine(cx + 2.2f, cy + 2.8f, cx + 2.2f, cy + 5f, t, color);
                GreenRender.drawLine(cx - 0.4f, cy + 1.4f, cx - 3.2f, cy + 4.6f, t, color);
                break;
            case UTILITY:
                GreenRender.outlineRR(cx - 2.3f, cy - 5f, 4.6f, 4.6f, 2.3f, t, color);
                GreenRender.outlineRR(cx - 4.5f, cy + 0.8f, 9f, 5f, 2.5f, t, color);
                break;
            default:
                GreenRender.fillCircle(cx, cy, 2f, color);
        }
    }

    public boolean mouseClicked(int mx, int my, int mb) {
        if (mx >= x && mx <= x + width && my >= y && my <= y + height) {
            if (mb == 0) { dragging = true; dragX = mx - x; dragY = my - y; }
            else if (mb == 1) {
                expanded = !expanded;
                if (!expanded) for (ModuleButton b : buttons) b.expanded = false;
            }
            return true;
        }
        if (expanded && expandAnim.getValue() > 0.8f) {
            for (ModuleButton b : buttons) {
                if (mx >= b.x && mx <= b.x + b.width && my >= b.y && my <= b.y + b.getTotalHeight()) {
                    b.mouseClicked(mx, my, mb);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasExpandedModule() { for (ModuleButton b : buttons) if (b.expanded) return true; return false; }
    public void mouseReleased(int mx, int my, int st) { dragging = false; if (expanded) for (ModuleButton b : buttons) b.mouseReleased(mx, my, st); }
    public void keyTyped(char c, int k) { if (expanded) for (ModuleButton b : buttons) b.keyTyped(c, k); }
}

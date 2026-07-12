package greencloud.impl.gui.clickgui.Clickguis;

import greencloud.GreenCloud;
import greencloud.impl.gui.clickgui.buttons.ModuleButton;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.utils.animation.animations.EasingAnimation;
import greencloud.impl.utils.animation.animations.EasingAnimation.Easing;
import greencloud.impl.utils.render.GreenRender;
import greencloud.impl.utils.font.FontUtil;
import greencloud.impl.utils.render.shaders.BlurUtil;
import greencloud.GreenCloud;
import greencloud.impl.modules.render.ClickGUIModule;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Panel {
    public final Category category;
    public float x, y, width, height;
    public boolean dragging, expanded = true;
    public float dragX, dragY;

    private final EasingAnimation expandAnim = new EasingAnimation(1.0f);
    private final List<ModuleButton> buttons = new ArrayList<>();

    public Panel(Category category, int x, int y) {
        this.category = category; this.x = x; this.y = y; this.width = 120; this.height = 20;
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

        float currentListH = fullListH * anim;
        float totalH = height + currentListH;

        ClickGUIModule clickGui = GreenCloud.moduleManager.getModule(ClickGUIModule.class);
        if (clickGui != null && clickGui.blur.enabled && !BlurUtil.isFastRenderActive()) {
            BlurUtil.blurRegionRounded(x, y, width, totalH, (float) clickGui.blurStrength.value, 6);
            GreenRender.fillRR(x, y, width, totalH, 6, new Color(10, 10, 10, 190));
        } else {
            GreenRender.fillRR(x, y, width, totalH, 6, new Color(10, 10, 10, 160));
        }

        String tabName = category.name().charAt(0) + category.name().substring(1).toLowerCase();
        FontUtil.getSafeNormal().drawString(tabName, x + 10, y + (height - FontUtil.getSafeNormal().getHeight()) / 2f, -1);

        if (anim > 0.01f && !visibleButtons.isEmpty()) {
            GreenRender.pushScissor(x, y + height, width, currentListH);

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

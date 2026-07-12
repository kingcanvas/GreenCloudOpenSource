package greencloud.impl.gui.clickgui.buttons;

import greencloud.GreenCloud;
import greencloud.impl.gui.clickgui.Clickguis.Panel;
import greencloud.impl.gui.clickgui.components.*;
import greencloud.impl.modules.Module;
import greencloud.impl.modules.render.HUD;
import greencloud.impl.settings.*;
import greencloud.impl.utils.animation.animations.EasingAnimation;
import greencloud.impl.utils.animation.animations.EasingAnimation.Easing;
import greencloud.impl.utils.font.FontUtil;
import greencloud.impl.utils.render.GreenRender;
import greencloud.impl.utils.render.shaders.BlurUtil;
import greencloud.GreenCloud;
import greencloud.impl.modules.render.ClickGUIModule;
import net.minecraft.client.renderer.GlStateManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ModuleButton {
    public final Module module;
    public final Panel parent;
    public float x, y, width, height = 18f;
    public boolean expanded = false;
    public boolean isLast = false;
    private final List<Component> components = new ArrayList<>();

    private final EasingAnimation toggleAnim;
    private final EasingAnimation hoverAnim = new EasingAnimation(0f);
    private final EasingAnimation expandAnim = new EasingAnimation(0f);
    private final EasingAnimation disableFade = new EasingAnimation(0f);
    private boolean wasToggled;

    public ModuleButton(Module module, Panel parent) {
        this.module = module;
        this.parent = parent;
        this.width = parent.width;
        this.toggleAnim = new EasingAnimation(module.isToggled() ? 1f : 0f);
        this.wasToggled = module.isToggled();

        components.add(new BindComponent(this));
        for (Setting s : module.getSettings()) {
            if      (s instanceof BooleanSetting) components.add(new BooleanComponent((BooleanSetting) s, this));
            else if (s instanceof NumberSetting)  components.add(new NumberComponent((NumberSetting)  s, this));
            else if (s instanceof ModeSetting)    components.add(new ModeComponent((ModeSetting)    s, this));
            else if (s instanceof ColorSetting)   components.add(new ColorComponent((ColorSetting)   s, this));
            else if (s instanceof StringSetting)  components.add(new StringComponent((StringSetting)  s, this));
        }
    }

    public void drawScreen(int mx, int my, float pt) {
        HUD hud = GreenCloud.instance.moduleManager.getModule(HUD.class);
        Color accent = hud != null ? new Color(hud.getHudColor()) : new Color(232, 97, 160);

        boolean isNowToggled = module.isToggled();

        if (wasToggled && !isNowToggled) {
            disableFade.set(0.6f);
            disableFade.animateTo(0f, 380, Easing.EASE_OUT_EXPO);
        }
        wasToggled = isNowToggled;

        toggleAnim.animateTo(isNowToggled ? 1f : 0f, 220, Easing.EASE_OUT_EXPO);
        float tAnim = toggleAnim.update();

        boolean hovered = mx >= x && mx <= x + width && my >= y && my <= y + height;
        hoverAnim.animateTo(hovered ? 1f : 0f, hovered ? 120 : 200, Easing.EASE_OUT_QUAD);
        float hAnim = hoverAnim.update();

        float fadeVal = disableFade.update();

        float targetH = 0;
        if (expanded) {
            for (Component c : components) {
                if (c.setting == null || c.setting.isVisible()) targetH += c.getFinalHeight() + 3f;
            }
        }
        expandAnim.animateTo(targetH, targetH > expandAnim.getValue() ? 280 : 220,
                             targetH > expandAnim.getValue() ? Easing.EASE_OUT_QUART : Easing.EASE_OUT_CUBIC);
        float eAnim = expandAnim.update();

        float rounding = (isLast && eAnim < 1.0f) ? 6.0f : 0.0f;
        float fillH = isLast ? height : height + 1f;

        if (tAnim > 0.01f) {
            GreenRender.fillRRCornersHard(x, y, width, fillH, 0, 0, rounding, rounding,
                    GreenRender.withAlpha(accent, tAnim * 0.45f));
        }

        if (hAnim > 0.01f) {
            GreenRender.fillRRCornersHard(x, y, width, fillH, 0, 0, rounding, rounding,
                    new Color(255, 255, 255, (int)(hAnim * 22)));
        }


        if (fadeVal > 0.01f) {
            GreenRender.fillRRCornersHard(x, y, width, fillH, 0, 0, rounding, rounding,
                    new Color(255, 255, 255, (int)(fadeVal * 180)));
        }

        GlStateManager.enableTexture2D();
        float textY = y + (height - FontUtil.getSafeNormal().getHeight()) / 2f;
        FontUtil.getSafeNormal().drawString(module.getName(), x + 8, textY, -1);

        if (eAnim > 1f) {
            float curY = y + height;
            GreenRender.pushScissor(x, curY, width, eAnim);

            float expandedRounding = isLast ? 6.0f : 0.0f;
            ClickGUIModule clickGui = GreenCloud.moduleManager.getModule(ClickGUIModule.class);
            if (clickGui != null && clickGui.blur.enabled && !BlurUtil.isFastRenderActive()) {
                BlurUtil.blurRegionRounded(x, curY, width, eAnim, (float) clickGui.blurStrength.value, (int) expandedRounding);
                GreenRender.fillRRCorners(x, curY, width, eAnim, 0, 0, expandedRounding, expandedRounding,
                        new Color(15, 15, 18, 180));
            } else {
                GreenRender.fillRRCorners(x, curY, width, eAnim, 0, 0, expandedRounding, expandedRounding,
                        new Color(15, 15, 18, 160));
            }

            float setY = curY + 4f;
            for (Component c : components) {
                if (c.setting != null && !c.setting.isVisible()) continue;
                c.x = x; c.y = setY; c.width = width;
                c.drawScreen(mx, my, pt);
                setY += c.getHeight() + 3f;
            }
            GreenRender.popScissor();
        }
    }

    public void mouseClicked(int mx, int my, int mb) {
        if (mx >= x && mx <= x + width && my >= y && my <= y + height) {
            if (mb == 0) module.toggle();
            else if (mb == 1) expanded = !expanded;
        }
        if (expanded) for (Component c : components) if (c.setting == null || c.setting.isVisible()) c.mouseClicked(mx, my, mb);
    }

    public void mouseReleased(int mx, int my, int st) { if (expanded) for (Component c : components) c.mouseReleased(mx, my, st); }
    public void keyTyped(char ch, int key)             { if (expanded) for (Component c : components) c.keyTyped(ch, key); }
    public float getTotalHeight()                      { return height + expandAnim.getValue(); }
}

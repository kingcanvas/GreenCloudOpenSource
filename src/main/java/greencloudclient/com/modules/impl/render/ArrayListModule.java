package greencloudclient.com.modules.impl.render;

import greencloudclient.com.GreenCloud;
import greencloudclient.com.managers.player.PositionManager;
import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.com.settings.BooleanSetting;
import greencloudclient.com.settings.ColorSetting;
import greencloudclient.com.settings.ModeSetting;
import greencloudclient.com.settings.NumberSetting;
import greencloudclient.com.settings.Setting;
import greencloudclient.com.utils.font.CustomFontRenderer;
import greencloudclient.com.utils.font.FontUtil;
import greencloudclient.com.utils.render.GreenRender;
import greencloudclient.com.utils.render.shaders.BlurUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayListModule extends Module {

    private static final String ELEMENT_NAME = "ArrayList";

    public final ModeSetting font = new ModeSetting("Font", this, "Bold", "Bold", "Regular", "Minecraft");
    public final NumberSetting scale = new NumberSetting("Scale", this, 1.0, 0.5, 2.0, 0.05);
    public final NumberSetting paddingX = new NumberSetting("Padding X", this, 3, 0, 8, 0.5);
    public final NumberSetting paddingY = new NumberSetting("Padding Y", this, 1.5, 0, 5, 0.5);
    public final NumberSetting animationSpeed = new NumberSetting("Animation Speed", this, 12, 2, 30, 1);

    public final BooleanSetting syncColors = new BooleanSetting("Sync ClickGUI Color", this, true);
    public final ColorSetting color = new ColorSetting("Color", this, new Color(101, 153, 239), () -> !syncColors.enabled);
    public final BooleanSetting gradient = new BooleanSetting("Gradient", this, false);
    public final ColorSetting color2 = new ColorSetting("Color 2", this, new Color(153, 101, 239), () -> gradient.enabled && !syncColors.enabled);

    public final BooleanSetting background = new BooleanSetting("Background", this, true);
    public final ColorSetting backgroundColor = new ColorSetting("Background Color", this, new Color(15, 15, 18), () -> background.enabled);
    public final NumberSetting backgroundAlpha = new NumberSetting("Background Alpha", this, 140, 0, 255, 5, () -> background.enabled);
    public final BooleanSetting blur = new BooleanSetting("Blur", this, false);
    public final NumberSetting blurStrength = new NumberSetting("Blur Strength", this, 8, 1, 20, 1, () -> blur.enabled);

    public final BooleanSetting suffixes = new BooleanSetting("Suffixes", this, true);
    public final ColorSetting suffixColor = new ColorSetting("Suffix Color", this, new Color(160, 160, 160), () -> suffixes.enabled);

    public final BooleanSetting lowercase = new BooleanSetting("Lowercase", this, false);
    public final BooleanSetting textShadow = new BooleanSetting("Text Shadow", this, true);

    private final Map<Module, Float> animations = new HashMap<>();
    private long lastFrame = -1;
    private boolean elementRegistered;

    public ArrayListModule() {
        super("ArrayList", Category.RENDER);
        this.setToggled(true);
        addSettings(font, scale, paddingX, paddingY, animationSpeed,
                syncColors, color, gradient, color2,
                background, backgroundColor, backgroundAlpha, blur, blurStrength,
                suffixes, suffixColor,
                lowercase, textShadow);
    }

    private static class Entry {
        String name, suffix;
        float nameW, width, anim;
    }

    @SubscribeEvent
    public void onRender2D(RenderGameOverlayEvent.Text event) {
        if (mc.gameSettings.showDebugInfo) return;
        ensureElementRegistered();
        render();
    }

    private void render() {
        long now = System.nanoTime();
        float dt = lastFrame < 0 ? 0f : Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        float ease = 1f - (float) Math.exp(-animationSpeed.value * dt);

        float padX = (float) paddingX.value;
        float padY = (float) paddingY.value;

        List<Entry> entries = new ArrayList<>();
        for (Module m : GreenCloud.moduleManager.getModules()) {
            if (m == this || m.isHidden()) continue;
            float target = m.isToggled() ? 1f : 0f;
            float anim = animations.getOrDefault(m, 0f);
            anim += (target - anim) * ease;
            if (Math.abs(target - anim) < 0.005f) anim = target;
            animations.put(m, anim);
            if (anim < 0.01f) continue;

            Entry e = new Entry();
            e.name = lowercase.enabled ? m.getName().toLowerCase() : m.getName();
            e.suffix = suffixes.enabled ? getSuffix(m) : "";
            if (lowercase.enabled) e.suffix = e.suffix.toLowerCase();
            e.nameW = textWidth(e.name);
            e.width = e.nameW + textWidth(e.suffix) + padX * 2f;
            e.anim = anim;
            entries.add(e);
        }
        if (entries.isEmpty()) return;

        entries.sort((a, b) -> Float.compare(b.width, a.width));

        float fontH = fontHeight();
        float rowH = fontH + padY * 2f;
        float listW = entries.get(0).width;
        float listH = 0f;
        for (Entry e : entries) listH += rowH * e.anim;

        float s = (float) scale.value;
        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        float x = screenW - listW * s - 2f;
        float y = 2f;
        PositionManager.DraggableElement el = findElement();
        if (el != null) {
            boolean anchoredRight = el.x + el.width / 2f >= screenW / 2f;
            x = anchoredRight ? el.x + el.width - listW * s : el.x;
            x = Math.max(0f, Math.min(x, screenW - listW * s));
            y = Math.max(0f, Math.min(el.y, screenH - listH * s));
            el.x = Math.round(x);
            el.y = Math.round(y);
            el.width = (int) Math.ceil(listW * s);
            el.height = (int) Math.ceil(listH * s);
        }
        boolean right = x + listW * s / 2f >= screenW / 2f;

        if (blur.enabled && !BlurUtil.isFastRenderActive()) {
            float strength = (float) blurStrength.value;
            float seam = 1f / sr.getScaleFactor();
            float rowY = 0f;
            for (Entry e : entries) {
                float h = rowH * e.anim;
                float rowX = right ? listW - e.width : 0f;
                BlurUtil.blurRegion(x + rowX * s, y + rowY * s, e.width * s, h * s + seam, strength);
                rowY += h;
            }
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(s, s, 1f);

        if (background.enabled) {
            int bg = Math.max(0, Math.min(255, (int) backgroundAlpha.value)) << 24 | (backgroundColor.getColor() & 0xFFFFFF);
            float rowY = 0f;
            for (Entry e : entries) {
                float h = rowH * e.anim;
                float rowX = right ? listW - e.width : 0f;
                quad(rowX, rowY, rowX + e.width, rowY + h, bg, bg);
                rowY += h;
            }
        }

        int suffixCol = suffixColor.getColor();
        float rowY = 0f;
        for (Entry e : entries) {
            float h = rowH * e.anim;
            float textX = right ? listW - e.width + padX : padX;
            float textY = rowY + (h - fontH) / 2f;

            GlStateManager.pushMatrix();
            if (e.anim < 1f) {
                float cy = rowY + h / 2f;
                GlStateManager.translate(0, cy, 0);
                GlStateManager.scale(1f, e.anim, 1f);
                GlStateManager.translate(0, -cy, 0);
            }
            drawText(e.name, textX, textY, colorAt(rowY + h / 2f, listH));
            if (!e.suffix.isEmpty()) drawText(e.suffix, textX + e.nameW, textY, suffixCol);
            GlStateManager.popMatrix();

            rowY += h;
        }

        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private int primaryColor()   { return syncColors.enabled ? ClickGUIModule.getColor() : color.getColor(); }
    private int secondaryColor() { return syncColors.enabled ? ClickGUIModule.getSecondaryColor() : color2.getColor(); }

    private int colorAt(float y, float totalH) {
        if (!gradient.enabled || totalH <= 0f) return primaryColor();
        return GreenRender.lerpARGB(primaryColor(), secondaryColor(), Math.min(1f, y / totalH));
    }

    private CustomFontRenderer customFont() {
        if (font.is("Minecraft")) return null;
        return font.is("Regular") ? FontUtil.normal : FontUtil.bold;
    }

    private float textWidth(String text) {
        CustomFontRenderer f = customFont();
        return f != null ? f.getWidth(text) : mc.fontRendererObj.getStringWidth(text);
    }

    private float fontHeight() {
        CustomFontRenderer f = customFont();
        return f != null ? f.getFontHeight() : mc.fontRendererObj.FONT_HEIGHT - 1;
    }

    private void drawText(String text, float x, float y, int color) {
        CustomFontRenderer f = customFont();
        if (f != null) {
            f.drawString(text, x, y, color, textShadow.enabled);
        } else {
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            mc.fontRendererObj.drawString(text, x, y, color, textShadow.enabled);
        }
    }

    private static void quad(float x, float y, float x2, float y2, int top, int bottom) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_QUADS);
        glColor(top);
        GL11.glVertex2f(x, y);
        glColor(bottom);
        GL11.glVertex2f(x, y2);
        GL11.glVertex2f(x2, y2);
        glColor(top);
        GL11.glVertex2f(x2, y);
        GL11.glEnd();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private static void glColor(int argb) {
        GL11.glColor4f((argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f);
    }

    private PositionManager.DraggableElement findElement() {
        for (PositionManager.DraggableElement el : PositionManager.getInstance().elements) {
            if (el.name.equals(ELEMENT_NAME)) return el;
        }
        return null;
    }

    private void ensureElementRegistered() {
        if (elementRegistered) return;
        elementRegistered = true;
        if (findElement() == null) {
            ScaledResolution sr = new ScaledResolution(mc);
            PositionManager.addElement(ELEMENT_NAME, sr.getScaledWidth() - 102, 2, 100, 20, null);
        }
    }

    private String getSuffix(Module m) {
        for (Setting s : m.getSettings()) {
            if (s instanceof ModeSetting) return " " + ((ModeSetting) s).currentMode;
        }
        for (Setting s : m.getSettings()) {
            if (s instanceof NumberSetting && ((NumberSetting) s).isRange) {
                NumberSetting ns = (NumberSetting) s;
                return " " + formatNum(ns.value) + " - " + formatNum(ns.maxValue);
            }
        }
        return "";
    }

    private String formatNum(double val) {
        if (val == (long) val) return String.valueOf((long) val);
        return String.format("%.1f", val);
    }
}

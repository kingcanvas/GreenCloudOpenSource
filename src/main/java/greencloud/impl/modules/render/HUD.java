package greencloud.impl.modules.render;

import greencloud.GreenCloud;
import greencloud.impl.managers.player.PositionManager;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ColorSetting;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.settings.Setting;
import greencloud.impl.utils.font.FontUtil;
import greencloud.impl.utils.render.AnimationUtil;
import greencloud.impl.utils.render.GreenRender;
import greencloud.impl.utils.render.shaders.BlurUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HUD extends Module {

    private static final String ARRAYLIST_NAME = "ArrayList";

    public final ColorSetting hudColor = new ColorSetting("Color", this, new Color(101, 153, 239));
    public final ColorSetting color2 = new ColorSetting("Color 2", this, new Color(153, 101, 239));
    public final BooleanSetting notificationsEnabled = new BooleanSetting("Notifications", this, true);
    public final BooleanSetting useGradient = new BooleanSetting("Gradient", this, true);
    public final ModeSetting listMode = new ModeSetting("List Mode", this, "Default", "Clean", "Default");
    public final BooleanSetting showBackground = new BooleanSetting("Background", this, true, () -> listMode.is("Default"));
    public final NumberSetting bgAlpha = new NumberSetting("Background Alpha", this, 120, 0, 255, 5, () -> listMode.is("Default") && showBackground.enabled);
    public final BooleanSetting blur = new BooleanSetting("Blur", this, false, () -> listMode.is("Default"));
    public final NumberSetting blurStrength = new NumberSetting("Blur Strength", this, 10, 1, 30, 1, () -> listMode.is("Default") && blur.enabled);

    private final Map<Module, Float> moduleAnimations = new HashMap<>();
    private boolean draggableRegistered = false;
    private float gradientAnim = 0f;
    private net.minecraft.client.shader.Framebuffer arraylistFbo;

    public HUD() {
        super("HUD", "Heads-Up Display overlay.", Category.RENDER);
        this.setToggled(true);
        addSettings(notificationsEnabled, listMode, hudColor, color2, useGradient, showBackground, bgAlpha, blur, blurStrength);
    }

    public int getHudColor() { return hudColor.getColor(); }

    public static int getColor() {
        HUD hud = GreenCloud.moduleManager.getModule(HUD.class);
        return hud != null ? hud.hudColor.getColor() : 0xFF6599EF;
    }

    @SubscribeEvent
    public void onRender2D(RenderGameOverlayEvent.Text event) {
        if (Minecraft.getMinecraft().gameSettings.showDebugInfo) return;
        ensureDraggableRegistered();
        renderArrayList();
    }

    private static class ModulePos {
        Module m;
        float bgX, bgW, textW, currentY, animH, anim;
        float tl, tr, br, bl;
    }

    private void renderArrayList() {
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        gradientAnim = AnimationUtil.moveUD(gradientAnim, useGradient.enabled ? 1f : 0f, 0.08f, 0.02f);

        List<Module> active = new ArrayList<>();
        for (Module m : GreenCloud.moduleManager.getModules()) {
            if (m == this || m.isHidden()) continue;
            float anim = AnimationUtil.moveUD(moduleAnimations.getOrDefault(m, 0f), m.isToggled() ? 1f : 0f, 0.14f, 0.05f);
            moduleAnimations.put(m, anim);
            if (anim > 0.01f) active.add(m);
        }

        active.sort(Comparator.comparingDouble(m -> -rawEntryW(m)));

        ScaledResolution sr = new ScaledResolution(mc);
        float fontH = getTextHeight();
        float px = 4.0f;
        float py = 1.0f;
        float cr = 3.0f;
        float entryH = fontH + py * 2f;

        float maxModuleW = active.isEmpty() ? 0f : rawEntryW(active.get(0)) + px * 2f;
        float maxW = maxModuleW;

        float totalH = 0f;
        for (Module m : active) {
            totalH += entryH * moduleAnimations.getOrDefault(m, 0f);
        }

        PositionManager pm = PositionManager.getInstance();
        float listX = sr.getScaledWidth() - maxW - 2f;
        float listY = 2f;

        for (PositionManager.DraggableElement el : pm.elements) {
            if (el.name.equals(ARRAYLIST_NAME)) {
                listX = el.x;
                listY = el.y;
                el.width  = (int) maxW;
                el.height = (int) totalH;
                break;
            }
        }

        boolean isLeft = listX < sr.getScaledWidth() / 2f;
        float rightEdge = listX + maxW;

        float currentY = listY;

        if (active.isEmpty()) return;

        List<ModulePos> positions = new ArrayList<>();
        for (Module m : active) {
            float anim = moduleAnimations.get(m);
            float textW = rawEntryW(m);
            float bgW = textW + px * 2f;
            float animH = entryH * anim;

            if (animH < 0.5f) { currentY += animH; continue; }

            ModulePos pos = new ModulePos();
            pos.m = m;
            pos.bgW = bgW;
            pos.bgX = isLeft ? listX : (rightEdge - bgW);
            pos.textW = textW;
            pos.currentY = currentY;
            pos.animH = animH;
            pos.anim = anim;
            positions.add(pos);
            currentY += animH;
        }

        if (positions.isEmpty()) return;

        for (int i = 0; i < positions.size(); i++) {
            ModulePos pos = positions.get(i);
            boolean isFirst = (i == 0);
            boolean isLast = (i == positions.size() - 1);

            pos.tl = isFirst ? cr : 0f;
            pos.tr = isFirst ? cr : 0f;

            if (isLeft) {
                pos.bl = isLast ? cr : 0f;
                pos.br = (isLast || pos.bgW > positions.get(i + 1).bgW) ? cr : 0f;
            } else {
                pos.bl = (isLast || pos.bgW > positions.get(i + 1).bgW) ? cr : 0f;
                pos.br = isLast ? cr : 0f;
            }

            float halfH = pos.animH / 2f;
            pos.tl = Math.min(pos.tl, halfH);
            pos.tr = Math.min(pos.tr, halfH);
            pos.bl = Math.min(pos.bl, halfH);
            pos.br = Math.min(pos.br, halfH);
        }

        if (listMode.is("Default") && showBackground.enabled) {
            boolean useFbo = net.minecraft.client.renderer.OpenGlHelper.isFramebufferEnabled();
            boolean useBlur = blur.enabled && blurStrength.value > 0
                    && !BlurUtil.isFastRenderActive();

            if (useBlur) {
                BlurUtil.captureScene();
                for (int i = 0; i < positions.size(); i++) {
                    ModulePos pos = positions.get(i);
                    boolean isLast = (i == positions.size() - 1);
                    float extH = isLast ? 0f : 1.0f;
                    int blurCr = (int) Math.max(Math.max(pos.tl, pos.tr), Math.max(pos.bl, pos.br));
                    GreenRender.blurRounded(pos.bgX, pos.currentY, pos.bgW, pos.animH + extH,
                            (float) blurStrength.value, blurCr);
                }
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                int tintColor = new Color(15, 15, 18, (int) bgAlpha.value).getRGB();
                for (int i = 0; i < positions.size(); i++) {
                    ModulePos pos = positions.get(i);
                    boolean isLast = (i == positions.size() - 1);
                    float extH = isLast ? 0f : 1.0f;
                    if (pos.tl > 0 || pos.tr > 0 || pos.bl > 0 || pos.br > 0) {
                        GreenRender.fillRRCornersHard(pos.bgX, pos.currentY, pos.bgW, pos.animH + extH,
                                pos.tl, pos.tr, pos.br, pos.bl, tintColor);
                    } else {
                        drawSolidRect(pos.bgX, pos.currentY, pos.bgX + pos.bgW, pos.currentY + pos.animH + extH, tintColor);
                    }
                }
            } else {
                if (useFbo) {
                    int sw = mc.displayWidth, sh = mc.displayHeight;
                    if (arraylistFbo == null || arraylistFbo.framebufferWidth != sw || arraylistFbo.framebufferHeight != sh) {
                        if (arraylistFbo != null) arraylistFbo.deleteFramebuffer();
                        arraylistFbo = new net.minecraft.client.shader.Framebuffer(sw, sh, true);
                    }
                    GlStateManager.colorMask(true, true, true, true);
                    arraylistFbo.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
                    arraylistFbo.framebufferClear();
                    arraylistFbo.bindFramebuffer(false);
                }

                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                int opaqueBg = useFbo
                        ? new Color(20, 20, 20, 255).getRGB()
                        : new Color(20, 20, 20, (int) bgAlpha.value).getRGB();

                for (int i = 0; i < positions.size(); i++) {
                    ModulePos pos  = positions.get(i);
                    boolean isLast = (i == positions.size() - 1);
                    float rx = pos.bgX, ry = pos.currentY, rw = pos.bgW, rh = pos.animH;
                    float extH = isLast ? 0f : 1.0f;

                    if (!useFbo) {
                        rx = (float) Math.round(rx);
                        ry = (float) Math.round(ry);
                        rw = (float) Math.round(pos.bgX + pos.bgW) - rx;
                        rh = (float) Math.round(pos.currentY + pos.animH) - ry;
                        drawSolidRect(rx, ry, rx + rw, ry + rh, opaqueBg);
                    } else if (pos.tl > 0 || pos.tr > 0 || pos.bl > 0 || pos.br > 0) {
                        GreenRender.fillRRCornersHard(rx, ry, rw, rh + extH,
                                pos.tl, pos.tr, pos.br, pos.bl, opaqueBg);
                    } else {
                        drawSolidRect(rx, ry, rx + rw, ry + rh + extH, opaqueBg);
                    }
                }

                if (useFbo) {
                    for (int i = 0; i < positions.size() - 1; i++) {
                        ModulePos pos  = positions.get(i);
                        ModulePos next = positions.get(i + 1);
                        float bridgeW = Math.min(pos.bgW, next.bgW);
                        float bridgeX = isLeft ? listX : (rightEdge - bridgeW);
                        drawSolidRect(bridgeX, pos.currentY + pos.animH - 0.5f,
                                bridgeX + bridgeW, next.currentY + 0.5f, opaqueBg);
                    }

                    arraylistFbo.unbindFramebuffer();
                    mc.getFramebuffer().bindFramebuffer(true);

                    GlStateManager.pushMatrix();
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.color(1f, 1f, 1f, (float) (bgAlpha.value / 255.0));
                    arraylistFbo.bindFramebufferTexture();

                    Tessellator tessellator = Tessellator.getInstance();
                    WorldRenderer wr = tessellator.getWorldRenderer();
                    wr.begin(7, DefaultVertexFormats.POSITION_TEX);
                    double W = sr.getScaledWidth(), H = sr.getScaledHeight();
                    float u1 = (float) arraylistFbo.framebufferWidth  / (float) arraylistFbo.framebufferTextureWidth;
                    float v1 = (float) arraylistFbo.framebufferHeight / (float) arraylistFbo.framebufferTextureHeight;
                    wr.pos(0, H, 0).tex(0,  0 ).endVertex();
                    wr.pos(W, H, 0).tex(u1, 0 ).endVertex();
                    wr.pos(W, 0, 0).tex(u1, v1).endVertex();
                    wr.pos(0, 0, 0).tex(0,  v1).endVertex();
                    tessellator.draw();

                    GlStateManager.color(1f, 1f, 1f, 1f);
                    GlStateManager.bindTexture(0);
                    GlStateManager.popMatrix();
                }
            }
        }

        float sidebarW = 3.1f;
        float gap = 3f;

        {
            float barTop = positions.get(0).currentY;
            ModulePos last = positions.get(positions.size() - 1);
            float barBot = last.currentY + last.animH;
            float barX = isLeft ? listX : (rightEdge - sidebarW);
            float sw = sidebarW;
            float r = sw / 2f;

            int gc0 = getGradientOffset(new Color(hudColor.getColor()), new Color(color2.getColor()), 0);
            int gcLast = getGradientOffset(new Color(hudColor.getColor()), new Color(color2.getColor()), (positions.size() - 1) * 0.1);
            int topC = GreenRender.lerpARGB(hudColor.getColor(), gc0, gradientAnim);
            int botC = GreenRender.lerpARGB(hudColor.getColor(), gcLast, gradientAnim);

            float tl = isLeft ? r : 0f;
            float tr = isLeft ? 0f : r;
            float br = isLeft ? 0f : r;
            float bl = isLeft ? r : 0f;

            GreenRender.fillRRCornersGradientV(barX, barTop, sw, barBot - barTop,
                    tl, tr, br, bl,
                    new Color(topC, true), new Color(botC, true));
        }

        for (int i = 0; i < positions.size(); i++) {
            ModulePos pos = positions.get(i);
            String name = pos.m.getName();
            String sfx = getSuffix(pos.m);

            float textX;
            if (listMode.is("Clean")) {
                textX = isLeft ? (pos.bgX + sidebarW + gap) : (pos.bgX + pos.bgW - sidebarW - gap - pos.textW);
            } else {
                textX = isLeft ? (pos.bgX + px) : (pos.bgX + pos.bgW - px - pos.textW);
            }

            float textY = pos.currentY + (pos.animH - fontH) / 2f;

            GlStateManager.pushMatrix();
            if (pos.anim < 1.0f) {
                GlStateManager.translate(textX + pos.textW / 2f, pos.currentY + pos.animH / 2f, 0);
                GlStateManager.scale(1.0, pos.anim, 1.0);
                GlStateManager.translate(-(textX + pos.textW / 2f), -(pos.currentY + pos.animH / 2f), 0);
            }

            int entryGc = getGradientOffset(new Color(hudColor.getColor()), new Color(color2.getColor()), i * 0.1);
            int nameColor = GreenRender.lerpARGB(hudColor.getColor(), entryGc, gradientAnim);
            int suffixColor = new Color(140, 140, 140).getRGB();
            if (FontUtil.bold != null) {
                FontUtil.bold.drawString(name, textX, textY, nameColor);
                FontUtil.bold.drawString(sfx, textX + FontUtil.bold.getStringWidth(name), textY, suffixColor);
            } else {
                mc.fontRendererObj.drawStringWithShadow(name, (int) textX, (int) textY, nameColor);
                mc.fontRendererObj.drawStringWithShadow(sfx, (int)(textX + mc.fontRendererObj.getStringWidth(name)), (int) textY, suffixColor);
            }

            GlStateManager.popMatrix();
        }
    }

    private void ensureDraggableRegistered() {
        if (draggableRegistered) return;
        draggableRegistered = true;

        ScaledResolution sr = new ScaledResolution(mc);
        PositionManager pm = PositionManager.getInstance();

        if (pm.elements.stream().noneMatch(e -> e.name.equals(ARRAYLIST_NAME))) {
            float defaultX = sr.getScaledWidth() - 100f - 2f;
            pm.addElement(ARRAYLIST_NAME, (int) defaultX, 2, 100, 20, null);
        }
    }

    private int getGradientOffset(Color color1, Color color2, double offset) {
        double speed = 2000.0;
        double time = (System.currentTimeMillis() % speed) / speed;
        double wave = (Math.sin((time + offset) * Math.PI * 2) + 1.0) / 2.0;
        return GreenRender.lerpARGB(color1.getRGB(), color2.getRGB(), (float) wave);
    }

    private void drawSolidRect(float x, float y, float x2, float y2, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8  & 255) / 255.0F;
        float b = (color       & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x,  y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y);
        GL11.glVertex2f(x,  y);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private float rawEntryW(Module m) {
        String name = m.getName(), sfx = getSuffix(m);
        if (FontUtil.bold != null)
            return FontUtil.bold.getStringWidth(name) + FontUtil.bold.getStringWidth(sfx);
        return mc.fontRendererObj.getStringWidth(name) + mc.fontRendererObj.getStringWidth(sfx);
    }

    private float getTextHeight() {
        return (FontUtil.bold != null) ? FontUtil.bold.getHeight() : mc.fontRendererObj.FONT_HEIGHT;
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
package greencloud.impl.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class RenderUtil {

    public static void drawGradientRoundedRect(float x, float y, float width, float height, float radius, int color1, int color2) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);


        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());

        float a1 = ((color1 >> 24) & 0xFF) / 255.0F;
        float r1 = ((color1 >> 16) & 0xFF) / 255.0F;
        float g1 = ((color1 >> 8) & 0xFF) / 255.0F;
        float b1 = (color1 & 0xFF) / 255.0F;


        float a2 = ((color2 >> 24) & 0xFF) / 255.0F;
        float r2 = ((color2 >> 16) & 0xFF) / 255.0F;
        float g2 = ((color2 >> 8) & 0xFF) / 255.0F;
        float b2 = (color2 & 0xFF) / 255.0F;

        GlStateManager.enableTexture2D();
    }

    public static void drawSoftShadow(float x, float y, float width, float height, float radius, int color) {
        drawGlow(x, y, width, height, radius, color);
    }

    public static void drawGlow(float x, float y, float width, float height, float glowRadius, int color) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.alphaFunc(516, 0.01f);


        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());

        float a = ((color >> 24) & 0xFF) / 255.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        float margin = glowRadius + 2;

        GlStateManager.enableTexture2D();
        GlStateManager.alphaFunc(516, 0.1f);
    }

    public static float lerp(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    public static int applyOpacity(int color, float opacity) {
        Color c = new Color(color);
        int alpha = (int) (c.getAlpha() * opacity);
        alpha = Math.max(0, Math.min(255, alpha));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha).getRGB();
    }

    public static void drawRect(float x, float y, float width, float height, int color) {
        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x, y + height, 0.0D).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).endVertex();
        worldrenderer.pos(x, y, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawShadow(float x, float y, float width, float height) {
        drawRoundedRect(x - 2, y - 2, x + width + 2, y + height + 2, 5, new Color(0, 0, 0, 40).getRGB());
        drawRoundedRect(x - 1, y - 1, x + width + 1, y + height + 1, 5, new Color(0, 0, 0, 60).getRGB());
    }

    public static void prepareScissorBox(float x, float y, float x2, float y2) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int factor = sr.getScaleFactor();
        GL11.glScissor((int) (x * factor), (int) ((sr.getScaledHeight() - y2) * factor), (int) ((x2 - x) * factor), (int) ((y2 - y) * factor));
    }

    public static void drawRoundedRect(float x, float y, float x1, float y1, float radius, int color) {
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        GL11.glPushAttrib(0);
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        x *= 2.0D;
        y *= 2.0D;
        x1 *= 2.0D;
        y1 *= 2.0D;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBegin(GL11.GL_POLYGON);

        for (int i = 0; i <= 90; i += 3)
            GL11.glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y + radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        for (int i = 90; i <= 180; i += 3)
            GL11.glVertex2d(x + radius + Math.sin(i * Math.PI / 180.0D) * radius * -1.0D, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius * -1.0D);
        for (int i = 0; i <= 90; i += 3)
            GL11.glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y1 - radius + Math.cos(i * Math.PI / 180.0D) * radius);
        for (int i = 90; i <= 180; i += 3)
            GL11.glVertex2d(x1 - radius + Math.sin(i * Math.PI / 180.0D) * radius, y + radius + Math.cos(i * Math.PI / 180.0D) * radius);

        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glScaled(2.0D, 2.0D, 2.0D);
        GL11.glPopAttrib();
        GlStateManager.color(1, 1, 1);
    }

    public static void drawRoundedOutline(float x, float y, float x2, float y2, float radius, float width, int color) {
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        GL11.glPushAttrib(0);
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        x *= 2.0D;
        y *= 2.0D;
        x2 *= 2.0D;
        y2 *= 2.0D;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINE_LOOP);

        for (int i = 180; i <= 270; i++) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.cos(angle) * radius, y + radius + Math.sin(angle) * radius);
        }
        for (int i = 270; i <= 360; i++) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius + Math.cos(angle) * radius, y + radius + Math.sin(angle) * radius);
        }
        for (int i = 0; i <= 90; i++) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius + Math.cos(angle) * radius, y2 - radius + Math.sin(angle) * radius);
        }
        for (int i = 90; i <= 180; i++) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.cos(angle) * radius, y2 - radius + Math.sin(angle) * radius);
        }

        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glScaled(2.0D, 2.0D, 2.0D);
        GL11.glPopAttrib();
        GlStateManager.color(1, 1, 1);
    }

    public static void drawGradientRect(double left, double top, double right, double bottom, int startColor, int endColor) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(right, top, 0.0D).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, top, 0.0D).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, bottom, 0.0D).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void drawGradientRectSideways(double left, double top, double right, double bottom, int startColor, int endColor) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(right, top, 0.0D).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos(left, top, 0.0D).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, bottom, 0.0D).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void drawLine(float x, float y, float x1, float y1, int color) {
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        float alpha = (float) (color >> 24 & 255) / 255.0F;

        GlStateManager.color(red, green, blue, alpha);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x, y, 0.0D).endVertex();
        worldrenderer.pos(x1, y1, 0.0D).endVertex();
        tessellator.draw();
    }

    public static void drawHorizontalGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
        float startRed   = (float)(startColor >> 16 & 255) / 255.0F;
        float startGreen = (float)(startColor >> 8 & 255) / 255.0F;
        float startBlue  = (float)(startColor & 255) / 255.0F;
        float endAlpha   = (float)(endColor >> 24 & 255) / 255.0F;
        float endRed     = (float)(endColor >> 16 & 255) / 255.0F;
        float endGreen   = (float)(endColor >> 8 & 255) / 255.0F;
        float endBlue    = (float)(endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos((double)left, (double)top, 0.0D).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        worldrenderer.pos((double)left, (double)bottom, 0.0D).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        worldrenderer.pos((double)right, (double)bottom, 0.0D).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        worldrenderer.pos((double)right, (double)top, 0.0D).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    public static void glColor(int hex) {
        float alpha = (float) (hex >> 24 & 255) / 255.0F;
        float red = (float) (hex >> 16 & 255) / 255.0F;
        float green = (float) (hex >> 8 & 255) / 255.0F;
        float blue = (float) (hex & 255) / 255.0F;
        GlStateManager.color(red, green, blue, alpha);
    }

    public static void drawBoundingBox(AxisAlignedBB aa) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        tessellator.draw();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        worldRenderer.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        tessellator.draw();
    }
}
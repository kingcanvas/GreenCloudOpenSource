package greencloud.impl.utils.render;

import greencloud.impl.utils.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class RenderHelper {


    public static void drawRect(float left, float top, float right, float bottom, int color) {
        if (left < right) { float i = left; left = right; right = i; }
        if (top < bottom) { float j = top; top = bottom; bottom = j; }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(f, f1, f2, f3);

        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(left, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, top, 0.0D).endVertex();
        worldrenderer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }


    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        GL11.glBegin(GL11.GL_POLYGON);

        for (int i = 0; i <= 90; i += 3)
            GL11.glVertex2d(x + radius + Math.sin(Math.toRadians(i + 180)) * radius, y + radius + Math.cos(Math.toRadians(i + 180)) * radius);
        for (int i = 0; i <= 90; i += 3)
            GL11.glVertex2d(x + radius + Math.sin(Math.toRadians(i + 90)) * radius, y + height - radius + Math.cos(Math.toRadians(i + 90)) * radius);
        for (int i = 0; i <= 90; i += 3)
            GL11.glVertex2d(x + width - radius + Math.sin(Math.toRadians(i)) * radius, y + height - radius + Math.cos(Math.toRadians(i)) * radius);
        for (int i = 0; i <= 90; i += 3)
            GL11.glVertex2d(x + width - radius + Math.sin(Math.toRadians(i - 90)) * radius, y + radius + Math.cos(Math.toRadians(i - 90)) * radius);

        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }


    public static void drawToggleSwitch(float x, float y, float width, float height, boolean enabled, Theme currentTheme) {
        int trackColor = enabled ? currentTheme.accentColor.getRGB() : new Color(60, 60, 60).getRGB();
        int handleColor = Color.WHITE.getRGB();


        drawRoundedRect(x, y, width, height, height / 2, trackColor);


        float handleRadius = height - 4;
        float handleX = enabled ? (x + width - handleRadius - 2) : (x + 2);


        drawRoundedRect(handleX, y + 2, handleRadius, handleRadius, handleRadius / 2, handleColor);
    }


    public static void prepareScissorBox(float x, float y, float x2, float y2) {
        ScaledResolution scale = new ScaledResolution(Minecraft.getMinecraft());
        int factor = scale.getScaleFactor();
        GL11.glScissor((int) (x * factor), (int) ((scale.getScaledHeight() - y2) * factor), (int) ((x2 - x) * factor), (int) ((y2 - y) * factor));
    }
}
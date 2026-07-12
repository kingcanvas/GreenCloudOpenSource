package greencloud.impl.modules.render;

import greencloud.GreenCloud;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ColorSetting;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ChinaHat extends Module {

    public ColorSetting hatColor = new ColorSetting("Color", this, new Color(255, 0, 0));
    public NumberSetting radius = new NumberSetting("Radius", this, 0.7, 0.3, 1.5, 0.1);
    public NumberSetting height = new NumberSetting("Cone Height", this, 0.3, 0.1, 1.0, 0.1);
    public NumberSetting opacity = new NumberSetting("Opacity", this, 100, 0, 255, 5);
    public BooleanSetting outline = new BooleanSetting("Outline", this, true);

    public ChinaHat() {
        super("ChinaHat", "Renders a stylish hat above your head.", Category.RENDER);
        addSettings(hatColor, radius, height, opacity, outline);
    }

    @SubscribeEvent
    public void onRender3D(RenderWorldLastEvent event) {
        if (mc.thePlayer == null) return;

        if (mc.gameSettings.thirdPersonView == 0) return;

        double scale = 1.0;
        PlayerModel playerModel = GreenCloud.moduleManager.getModule(PlayerModel.class);
        if (playerModel != null && playerModel.isToggled()) {
            scale = playerModel.size.getValue();
        }

        double x = interpolate(mc.thePlayer.lastTickPosX, mc.thePlayer.posX, event.partialTicks) - mc.getRenderManager().viewerPosX;
        double y = interpolate(mc.thePlayer.lastTickPosY, mc.thePlayer.posY, event.partialTicks) - mc.getRenderManager().viewerPosY;
        double z = interpolate(mc.thePlayer.lastTickPosZ, mc.thePlayer.posZ, event.partialTicks) - mc.getRenderManager().viewerPosZ;

        double yOffset = (mc.thePlayer.height * scale) + (0.1 * scale);

        if (mc.thePlayer.isSneaking()) {
            yOffset -= (0.2 * scale);
        }

        Color c = hatColor.getColorObject();
        int alphaVal = (int) opacity.getValue();
        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float b = c.getBlue() / 255f;
        float a = alphaVal / 255f;

        float yaw = interpolate(mc.thePlayer.prevRotationYawHead, mc.thePlayer.rotationYawHead, event.partialTicks);

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        GlStateManager.translate(x, y + yOffset, z);
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.rotate(-yaw, 0, 1, 0);

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.depthMask(false);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        worldrenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);

        worldrenderer.pos(0, height.getValue(), 0).color(r, g, b, Math.min(1.0f, a + 0.2f)).endVertex();

        double rad = radius.getValue();
        for (int i = 0; i <= 360; i += 5) {
            double angle = i * Math.PI / 180;
            worldrenderer.pos(Math.cos(angle) * rad, 0, Math.sin(angle) * rad)
                    .color(r, g, b, a).endVertex();
        }
        tessellator.draw();

        if (outline.enabled) {
            GL11.glLineWidth(2.0f);
            worldrenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= 360; i += 5) {
                double angle = i * Math.PI / 180;
                worldrenderer.pos(Math.cos(angle) * rad, 0, Math.sin(angle) * rad)
                        .color(r, g, b, Math.min(1.0f, a + 0.4f)).endVertex();
            }
            tessellator.draw();
        }

        GlStateManager.depthMask(true);
        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private float interpolate(float prev, float curr, float partialTicks) {
        return prev + (curr - prev) * partialTicks;
    }

    private double interpolate(double prev, double curr, float partialTicks) {
        return prev + (curr - prev) * partialTicks;
    }
}
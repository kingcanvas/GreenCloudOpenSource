package greencloud.impl.modules.render;

import greencloud.GreenCloud;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Breadcrumbs extends Module {

    private final NumberSetting thickness = new NumberSetting("Size", this, 3.0, 1.0, 10.0, 0.5);
    private final NumberSetting timeout = new NumberSetting("Despawn (s)", this, 5.0, 1.0, 30.0, 1.0);

    private final List<BreadcrumbPoint> points = new CopyOnWriteArrayList<>();

    public Breadcrumbs() {
        super("Breadcrumbs", "Shows a trail where you walk", Category.RENDER);
        addSettings(thickness, timeout);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        points.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        points.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && mc.thePlayer != null) {
            long currentTime = System.currentTimeMillis();

            points.add(new BreadcrumbPoint(mc.thePlayer.posX, mc.thePlayer.posY + 0.05, mc.thePlayer.posZ, currentTime));

            points.removeIf(point -> (currentTime - point.time) > (timeout.getValue() * 1000));
        }
    }

    @SubscribeEvent
    public void onRender3D(RenderWorldLastEvent event) {
        if (points.isEmpty()) return;

        double renderPosX = mc.getRenderManager().viewerPosX;
        double renderPosY = mc.getRenderManager().viewerPosY;
        double renderPosZ = mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        GL11.glEnable(GL11.GL_POINT_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        Color hudColor = GreenCloud.moduleManager.getModule(HUD.class).hudColor.getColorObject();
        int r = hudColor.getRed();
        int g = hudColor.getGreen();
        int b = hudColor.getBlue();
        int a = hudColor.getAlpha();

        for (BreadcrumbPoint point : points) {
            double x = point.x - renderPosX;
            double y = point.y - renderPosY;
            double z = point.z - renderPosZ;

            double dist = Math.sqrt(x * x + y * y + z * z);
            float size = (float) (thickness.getValue() * (1.0 / Math.max(1.0, dist * 0.25)));

            GL11.glPointSize(size * (mc.gameSettings.guiScale == 0 ? 2 : mc.gameSettings.guiScale));

            worldRenderer.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(x, y, z).color(r, g, b, a).endVertex();
            tessellator.draw();
        }

        GL11.glDisable(GL11.GL_POINT_SMOOTH);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static class BreadcrumbPoint {
        public final double x, y, z;
        public final long time;

        public BreadcrumbPoint(double x, double y, double z, long time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }
    }
}
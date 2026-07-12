package greencloud.impl.modules.render;

import greencloud.GreenCloud;
import greencloud.impl.managers.notification.NotificationManager;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ColorSetting;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.utils.render.GreenRender;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JumpCircle extends Module {

    public ModeSetting mode = new ModeSetting("Mode", this, "Disc", "Disc", "Ring");
    public NumberSetting radius = new NumberSetting("Radius", this, 2.5, 0.5, 5.0, 0.1);
    public NumberSetting lifeTime = new NumberSetting("Duration", this, 25, 10, 100, 1);
    public BooleanSetting useHudColors = new BooleanSetting("Use HUD Colors", this, true);
    public ColorSetting color = new ColorSetting("Color", this, new Color(0, 255, 150), () -> !useHudColors.enabled);
    public BooleanSetting glow = new BooleanSetting("Glow", this, false);

    private final List<Circle> circles = new ArrayList<>();
    private boolean prevOnGround = true;
    private boolean prevGlowEnabled;

    public JumpCircle() {
        super("JumpCircle", "Visual effect when landing.", Category.RENDER);
        this.addSettings(mode, radius, lifeTime, useHudColors, color, glow);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        prevGlowEnabled = glow.enabled;
    }

    @Override
    public void onDisable() {
        circles.clear();
        super.onDisable();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null) return;

        boolean currentOnGround = mc.thePlayer.onGround;

        if (!prevOnGround && currentOnGround && !mc.thePlayer.isSneaking()) {
            circles.add(new Circle(mc.thePlayer.getPositionVector()));
        }

        prevOnGround = currentOnGround;

        if (glow.enabled && !prevGlowEnabled) {
            NotificationManager.getInstance().addNotification(
                    "JumpCircle",
                    "Glow is a placeholder for rn.",
                    NotificationManager.NotificationType.WARNING,
                    2500
            );
        }
        prevGlowEnabled = glow.enabled;

        Iterator<Circle> iter = circles.iterator();
        while (iter.hasNext()) {
            Circle c = iter.next();
            c.ticksExisted++;
            if (c.ticksExisted > lifeTime.value) {
                iter.remove();
            }
        }
    }

    @SubscribeEvent
    public void onRender3D(RenderWorldLastEvent event) {
        if (circles.isEmpty()) return;

        float partialTicks = event.partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();

        HUD hud = GreenCloud.moduleManager.getModule(HUD.class);
        int hudC1 = hud != null ? hud.hudColor.getColor() : 0xFF6599EF;
        int hudC2 = hud != null ? hud.color2.getColor() : 0xFF9965EF;

        for (Circle c : circles) {
            double x = c.vec.xCoord - mc.getRenderManager().viewerPosX;
            double y = c.vec.yCoord - mc.getRenderManager().viewerPosY;
            double z = c.vec.zCoord - mc.getRenderManager().viewerPosZ;

            float interpolatedAge = c.ticksExisted + partialTicks;
            float progress = Math.min(1.0f, interpolatedAge / (float) lifeTime.value);

            double easedProgress = easeOutCubic(progress);
            double currentRadius = radius.value * easedProgress;

            float alphaProgress = easeOutQuad(progress);
            int alpha = (int) (255 * (1.0f - alphaProgress));
            if (alpha < 0) alpha = 0;
            float a = alpha / 255f;

            float r, g, b;

            if (useHudColors.enabled) {
                double speed = 2000.0;
                double time = (System.currentTimeMillis() % (long) speed) / speed;
                double wave = (Math.sin(time * Math.PI * 2) + 1.0) / 2.0;
                int gradARGB = GreenRender.lerpARGB(hudC1, hudC2, (float) wave);
                r = ((gradARGB >> 16) & 0xFF) / 255f;
                g = ((gradARGB >> 8) & 0xFF) / 255f;
                b = (gradARGB & 0xFF) / 255f;
            } else {
                Color col = color.getColorObject();
                r = col.getRed() / 255f;
                g = col.getGreen() / 255f;
                b = col.getBlue() / 255f;
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y + 0.05, z);

            if (mode.currentMode.equalsIgnoreCase("Disc")) {
                renderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
                renderer.pos(0, 0, 0).color(r, g, b, a).endVertex();

                for (int i = 0; i <= 360; i += 5) {
                    double angle = i * Math.PI / 180;
                    float vr = r, vg = g, vb = b;
                    if (useHudColors.enabled) {
                        double speed = 2000.0;
                        double time = (System.currentTimeMillis() % (long) speed) / speed + i * 0.02;
                        double wave = (Math.sin(time * Math.PI * 2) + 1.0) / 2.0;
                        int gradARGB = GreenRender.lerpARGB(hudC1, hudC2, (float) wave);
                        vr = ((gradARGB >> 16) & 0xFF) / 255f;
                        vg = ((gradARGB >> 8) & 0xFF) / 255f;
                        vb = (gradARGB & 0xFF) / 255f;
                    }
                    renderer.pos(Math.cos(angle) * currentRadius, 0, Math.sin(angle) * currentRadius)
                            .color(vr, vg, vb, 0.0f).endVertex();
                }
                tessellator.draw();

            } else {
                float lineWidth = 2.5f * (1.0f - alphaProgress);
                GL11.glLineWidth(Math.max(0.5f, lineWidth));

                renderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i <= 360; i += 3) {
                    double angle = i * Math.PI / 180;
                    float vr = r, vg = g, vb = b;
                    if (useHudColors.enabled) {
                        double speed = 2000.0;
                        double time = (System.currentTimeMillis() % (long) speed) / speed + i * 0.02;
                        double wave = (Math.sin(time * Math.PI * 2) + 1.0) / 2.0;
                        int gradARGB = GreenRender.lerpARGB(hudC1, hudC2, (float) wave);
                        vr = ((gradARGB >> 16) & 0xFF) / 255f;
                        vg = ((gradARGB >> 8) & 0xFF) / 255f;
                        vb = (gradARGB & 0xFF) / 255f;
                    }
                    renderer.pos(Math.cos(angle) * currentRadius, 0, Math.sin(angle) * currentRadius)
                            .color(vr, vg, vb, a).endVertex();
                }
                tessellator.draw();
            }

            GlStateManager.popMatrix();
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private double easeOutCubic(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    private float easeOutQuad(float t) {
        return t * (2 - t);
    }

    private static class Circle {
        public final Vec3 vec;
        public int ticksExisted;

        public Circle(Vec3 vec) {
            this.vec = vec;
            this.ticksExisted = 0;
        }
    }
}

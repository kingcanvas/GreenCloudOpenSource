package greencloud.impl.modules.render;

import greencloud.GreenCloud;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ColorSetting;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.client.renderer.GlStateManager;
import greencloud.impl.modules.utility.AntiBot;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

@SuppressWarnings("unused")
public class ESP extends Module {

    public static ESP instance;

    public ModeSetting    mode           = new ModeSetting   ("Mode",           this, "2D", "2D", "3D");
    public BooleanSetting useHudColor    = new BooleanSetting("Use HUD Color",  this, true);
    public ColorSetting   customColor    = new ColorSetting  ("Color",          this, new Color(255, 0, 0)) {
        @Override public boolean isVisible() { return !useHudColor.enabled; }
    };
    public NumberSetting  width          = new NumberSetting ("Line Width",     this, 1.5, 0.5, 5.0, 0.5);
    public BooleanSetting healthBar      = new BooleanSetting("Health Bar",     this, true);
    public BooleanSetting showInvisible  = new BooleanSetting("Show Invisible", this, true);
    public BooleanSetting filledBox      = new BooleanSetting("Filled",         this, false) {
        @Override public boolean isVisible() { return mode.currentMode.equals("3D"); }
    };
    public BooleanSetting smoothHealth   = new BooleanSetting("Smooth Health",  this, true);
    public NumberSetting  cornerSize     = new NumberSetting ("Corner Size",    this, 0.25, 0.1, 0.5, 0.05) {
        @Override public boolean isVisible() { return mode.currentMode.equals("2D"); }
    };

    public ESP() {
        super("ESP", "Extrasensory Perception", Category.RENDER);
        instance = this;
        addSettings(mode, useHudColor, customColor, width, healthBar, showInvisible, filledBox, smoothHealth, cornerSize);
    }

    private Color getColor() {
        if (useHudColor.enabled) {
            HUD hud = GreenCloud.moduleManager.getModule(HUD.class);
            return new Color(hud != null ? hud.getHudColor() : Color.RED.getRGB());
        }
        return new Color(customColor.getColor());
    }

    @SubscribeEvent
    public void onRender3D(RenderWorldLastEvent event) {
        if (mc.theWorld == null || mc.getRenderManager() == null) return;

        Color c = getColor();
        float r = c.getRed()   / 255f;
        float g = c.getGreen() / 255f;
        float b = c.getBlue()  / 255f;

        setupRender(true);

        try {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer) continue;
                if (player.isDead) continue;
                if (player.isInvisible() && !showInvisible.enabled) continue;
                if (AntiBot.instance != null && AntiBot.instance.isToggled() && AntiBot.instance.isBot(player)) continue;

                double x = interpolate(player.lastTickPosX, player.posX, event.partialTicks) - mc.getRenderManager().viewerPosX;
                double y = interpolate(player.lastTickPosY, player.posY, event.partialTicks) - mc.getRenderManager().viewerPosY;
                double z = interpolate(player.lastTickPosZ, player.posZ, event.partialTicks) - mc.getRenderManager().viewerPosZ;

                switch (mode.currentMode) {
                    case "2D": draw2D(player, x, y, z, r, g, b, event.partialTicks); break;
                    case "3D": draw3D(player, x, y, z, r, g, b, event.partialTicks); break;
                }
            }
        } finally {
            setupRender(false);
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
        }
    }

    private void draw2D(EntityPlayer entity, double x, double y, double z, float r, float g, float b, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0f, 1f, 0f);

        double padding = 0.15;
        double hw = entity.width / 2.0 + padding;
        double h = entity.height + padding;

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        GL11.glLineWidth((float) width.value + 2.0f);
        wr.pos(-hw, h, 0).color(0f, 0f, 0f, 1f).endVertex();
        wr.pos(hw, h, 0).color(0f, 0f, 0f, 1f).endVertex();
        wr.pos(hw, 0, 0).color(0f, 0f, 0f, 1f).endVertex();
        wr.pos(-hw, 0, 0).color(0f, 0f, 0f, 1f).endVertex();
        tess.draw();

        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        GL11.glLineWidth((float) width.value);
        wr.pos(-hw, h, 0).color(r, g, b, 1f).endVertex();
        wr.pos(hw, h, 0).color(r, g, b, 1f).endVertex();
        wr.pos(hw, 0, 0).color(r, g, b, 1f).endVertex();
        wr.pos(-hw, 0, 0).color(r, g, b, 1f).endVertex();
        tess.draw();

        GlStateManager.popMatrix();
    }

    private void draw3D(EntityPlayer entity, double x, double y, double z, float r, float g, float b, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        float yaw = interpolate(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
        GlStateManager.rotate(-yaw, 0f, 1f, 0f);

        double w = entity.width / 2.0;
        double h = entity.height;
        AxisAlignedBB box = new AxisAlignedBB(-w, 0, -w, w, h, w);

        if (filledBox.enabled) {
            GlStateManager.disableCull();
            Tessellator tess = Tessellator.getInstance();
            WorldRenderer wr = tess.getWorldRenderer();
            wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            addBoxVertices(wr, box, r, g, b, 0.15f);
            tess.draw();
            GlStateManager.enableCull();
        }

        drawOutlinedBox(box, r, g, b, 1f);
        GlStateManager.popMatrix();
    }

    private void drawHealthBar(EntityPlayer entity, WorldRenderer wr, Tessellator tess, double hw, double h, float partialTicks) {
        float health    = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float pct       = Math.max(0f, Math.min(1f, health / maxHealth));

        Color  healthColor = getHealthColor(pct);
        float  hr = healthColor.getRed()   / 255f;
        float  hg = healthColor.getGreen() / 255f;
        float  hb = healthColor.getBlue()  / 255f;

        double barW  = 0.06;
        double barX  = hw + 0.1;
        double barH  = h;
        double fillH = barH * pct;

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(barX,        0,     0).color(0.1f, 0.1f, 0.1f, 0.85f).endVertex();
        wr.pos(barX + barW, 0,     0).color(0.1f, 0.1f, 0.1f, 0.85f).endVertex();
        wr.pos(barX + barW, barH,  0).color(0.1f, 0.1f, 0.1f, 0.85f).endVertex();
        wr.pos(barX,        barH,  0).color(0.1f, 0.1f, 0.1f, 0.85f).endVertex();
        tess.draw();

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(barX,        0,      0).color(hr * 0.7f, hg * 0.7f, hb * 0.7f, 1f).endVertex();
        wr.pos(barX + barW, 0,      0).color(hr * 0.7f, hg * 0.7f, hb * 0.7f, 1f).endVertex();
        wr.pos(barX + barW, fillH,  0).color(hr, hg, hb, 1f).endVertex();
        wr.pos(barX,        fillH,  0).color(hr, hg, hb, 1f).endVertex();
        tess.draw();

        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        GL11.glLineWidth(1f);
        wr.pos(barX,        0,    0).color(0f, 0f, 0f, 1f).endVertex();
        wr.pos(barX + barW, 0,    0).color(0f, 0f, 0f, 1f).endVertex();
        wr.pos(barX + barW, barH, 0).color(0f, 0f, 0f, 1f).endVertex();
        wr.pos(barX,        barH, 0).color(0f, 0f, 0f, 1f).endVertex();
        tess.draw();
    }

    private Color getHealthColor(float pct) {
        if (pct > 0.75f) return new Color(46, 204, 113);
        if (pct > 0.5f)  return new Color(241, 196, 15);
        if (pct > 0.25f) return new Color(230, 126, 34);
        return new Color(231, 76, 60);
    }

    public static void drawOutlinedBox(AxisAlignedBB box, float r, float g, float b, float a) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(box.minX,box.minY,box.minZ).color(r,g,b,a).endVertex(); wr.pos(box.maxX,box.minY,box.minZ).color(r,g,b,a).endVertex();
        wr.pos(box.maxX,box.minY,box.minZ).color(r,g,b,a).endVertex(); wr.pos(box.maxX,box.minY,box.maxZ).color(r,g,b,a).endVertex();
        wr.pos(box.maxX,box.minY,box.maxZ).color(r,g,b,a).endVertex(); wr.pos(box.minX,box.minY,box.maxZ).color(r,g,b,a).endVertex();
        wr.pos(box.minX,box.minY,box.maxZ).color(r,g,b,a).endVertex(); wr.pos(box.minX,box.minY,box.minZ).color(r,g,b,a).endVertex();
        wr.pos(box.minX,box.maxY,box.minZ).color(r,g,b,a).endVertex(); wr.pos(box.maxX,box.maxY,box.minZ).color(r,g,b,a).endVertex();
        wr.pos(box.maxX,box.maxY,box.minZ).color(r,g,b,a).endVertex(); wr.pos(box.maxX,box.maxY,box.maxZ).color(r,g,b,a).endVertex();
        wr.pos(box.maxX,box.maxY,box.maxZ).color(r,g,b,a).endVertex(); wr.pos(box.minX,box.maxY,box.maxZ).color(r,g,b,a).endVertex();
        wr.pos(box.minX,box.maxY,box.maxZ).color(r,g,b,a).endVertex(); wr.pos(box.minX,box.maxY,box.minZ).color(r,g,b,a).endVertex();
        wr.pos(box.minX,box.minY,box.minZ).color(r,g,b,a).endVertex(); wr.pos(box.minX,box.maxY,box.minZ).color(r,g,b,a).endVertex();
        wr.pos(box.maxX,box.minY,box.minZ).color(r,g,b,a).endVertex(); wr.pos(box.maxX,box.maxY,box.minZ).color(r,g,b,a).endVertex();
        wr.pos(box.maxX,box.minY,box.maxZ).color(r,g,b,a).endVertex(); wr.pos(box.maxX,box.maxY,box.maxZ).color(r,g,b,a).endVertex();
        wr.pos(box.minX,box.minY,box.maxZ).color(r,g,b,a).endVertex(); wr.pos(box.minX,box.maxY,box.maxZ).color(r,g,b,a).endVertex();
        tess.draw();
    }

    private void addBoxVertices(WorldRenderer buf, AxisAlignedBB box, float r, float g, float b, float a) {
        buf.pos(box.minX,box.minY,box.minZ).color(r,g,b,a).endVertex(); buf.pos(box.maxX,box.minY,box.minZ).color(r,g,b,a).endVertex();
        buf.pos(box.maxX,box.minY,box.maxZ).color(r,g,b,a).endVertex(); buf.pos(box.minX,box.minY,box.maxZ).color(r,g,b,a).endVertex();
        buf.pos(box.minX,box.maxY,box.minZ).color(r,g,b,a).endVertex(); buf.pos(box.minX,box.maxY,box.maxZ).color(r,g,b,a).endVertex();
        buf.pos(box.maxX,box.maxY,box.maxZ).color(r,g,b,a).endVertex(); buf.pos(box.maxX,box.maxY,box.minZ).color(r,g,b,a).endVertex();
        buf.pos(box.minX,box.minY,box.minZ).color(r,g,b,a).endVertex(); buf.pos(box.minX,box.maxY,box.minZ).color(r,g,b,a).endVertex();
        buf.pos(box.maxX,box.maxY,box.minZ).color(r,g,b,a).endVertex(); buf.pos(box.maxX,box.minY,box.minZ).color(r,g,b,a).endVertex();
        buf.pos(box.maxX,box.minY,box.minZ).color(r,g,b,a).endVertex(); buf.pos(box.maxX,box.maxY,box.minZ).color(r,g,b,a).endVertex();
        buf.pos(box.maxX,box.maxY,box.maxZ).color(r,g,b,a).endVertex(); buf.pos(box.maxX,box.minY,box.maxZ).color(r,g,b,a).endVertex();
        buf.pos(box.maxX,box.minY,box.maxZ).color(r,g,b,a).endVertex(); buf.pos(box.maxX,box.maxY,box.maxZ).color(r,g,b,a).endVertex();
        buf.pos(box.minX,box.maxY,box.maxZ).color(r,g,b,a).endVertex(); buf.pos(box.minX,box.minY,box.maxZ).color(r,g,b,a).endVertex();
        buf.pos(box.minX,box.minY,box.maxZ).color(r,g,b,a).endVertex(); buf.pos(box.minX,box.maxY,box.maxZ).color(r,g,b,a).endVertex();
        buf.pos(box.minX,box.maxY,box.minZ).color(r,g,b,a).endVertex(); buf.pos(box.minX,box.minY,box.minZ).color(r,g,b,a).endVertex();
    }

    private void setupRender(boolean start) {
        if (start) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glLineWidth((float) width.value);
        } else {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
    }

    private float interpolate(float prev, float curr, float pt)   { return prev + (curr - prev) * pt; }
    private double interpolate(double prev, double curr, float pt) { return prev + (curr - prev) * pt; }
}
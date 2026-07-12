package greencloud.impl.modules.render;

import greencloud.GreenCloud;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.utils.font.FontUtil;
import greencloud.impl.utils.render.GreenRender;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class TargetHUD extends Module {

    private final BooleanSetting blur = new BooleanSetting("Blur", this, true);

    private EntityLivingBase target;
    private float displayHealth = 0;
    private float animation = 0;
    private Vector3f screenPos = null;

    public TargetHUD() {
        super("TargetHUD", "Displays information dynamically beside your current target.", Category.RENDER);
        addSettings(blur);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        target = getTarget();
        if (target == null) {
            screenPos = null;
            return;
        }

        double rx = target.lastTickPosX + (target.posX - target.lastTickPosX) * event.partialTicks - mc.getRenderManager().viewerPosX;
        double ry = target.lastTickPosY + (target.posY - target.lastTickPosY) * event.partialTicks - mc.getRenderManager().viewerPosY;
        double rz = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * event.partialTicks - mc.getRenderManager().viewerPosZ;

        Vector3f pos = project2D((float) rx, (float) (ry + target.height / 1.5f), (float) rz);
        if (pos != null && pos.z >= 0.0f && pos.z < 1.0f) {
            screenPos = pos;
        } else {
            screenPos = null;
        }
    }

    @SubscribeEvent
    public void onRender2D(RenderGameOverlayEvent.Text event) {
        if (target == null || screenPos == null) {
            animation = GreenRender.smooth(animation, 0, 0.15f);
            if (animation < 0.01f) return;
        } else {
            animation = GreenRender.smooth(animation, 1, 0.15f);
        }

        renderHUD(animation);
    }

    private void renderHUD(float alphaMult) {
        float width = 160;
        float height = 50;

        float posX = screenPos != null ? screenPos.x + 30f : (mc.displayWidth / 2f);
        float posY = screenPos != null ? screenPos.y - (height / 2f) : (mc.displayHeight / 2f);

        GlStateManager.pushMatrix();
        GlStateManager.translate(posX + width / 2, posY + height / 2, 0);
        GlStateManager.scale(alphaMult, alphaMult, 1);
        GlStateManager.translate(-(posX + width / 2), -(posY + height / 2), 0);

        if (blur.enabled) {
            GreenRender.blurRounded(posX, posY, width, height, 10, 10);
        }
        Color bgColor = GreenRender.withAlpha(new Color(20, 20, 20), 0.7f * alphaMult);
        GreenRender.fillRR(posX, posY, width, height, 10, bgColor);

        if (target != null || animation > 0.05f) {
            EntityLivingBase ent;
            if (target != null) {
                ent = target;
            } else {
                ent = (EntityLivingBase) mc.thePlayer;
            }

            float headSize = 34;
            float headX = posX + 8;
            float headY = posY + 8;

            drawRoundedHead(ent, headX, headY, headSize, 8);

            String name = ent.getName();
            float textX = headX + headSize + 8;
            GreenRender.drawStringBold(name, textX, headY + 2, GreenRender.withAlpha(Color.WHITE, alphaMult));

            String healthText = String.format("%.1f HP", ent.getHealth());
            GreenRender.drawStringSmall(healthText, textX, headY + 14, GreenRender.withAlpha(new Color(200, 200, 200), alphaMult));

            float barX = textX;
            float barY = posY + height - 15;
            float barW = width - (headSize + 24);
            float barH = 6;

            float healthPerc = MathHelper.clamp_float(ent.getHealth() / ent.getMaxHealth(), 0, 1);
            displayHealth = GreenRender.smooth(displayHealth, healthPerc, 0.15f);

            GreenRender.fillRR(barX, barY, barW, barH, 3, new Color(10, 10, 10, (int)(150 * alphaMult)));

            Color c1 = HUD.getColor() == 0 ? new Color(101, 153, 239) : new Color(HUD.getColor());
            Color c2 = c1.darker();

            if (displayHealth > 0.01f) {

                GreenRender.fillRRGradientH(barX, barY, barW * displayHealth, barH, 3,
                        GreenRender.withAlpha(c1, alphaMult),
                        GreenRender.withAlpha(c2, alphaMult));

                GreenRender.fillRR(barX, barY, barW * displayHealth, barH / 2f, 2, new Color(255, 255, 255, (int)(40 * alphaMult)));
            }
        }

        GlStateManager.popMatrix();
    }

    private void drawRoundedHead(EntityLivingBase entity, float x, float y, float size, float radius) {
        if (!(entity instanceof EntityPlayer)) return;

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 1);
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);
        GL11.glColorMask(false, false, false, false);

        GreenRender.fillRR(x, y, size, size, radius, Color.WHITE);

        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 1);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        mc.getTextureManager().bindTexture(((AbstractClientPlayer) entity).getLocationSkin());
        Gui.drawScaledCustomSizeModalRect((int)x, (int)y, 8, 8, 8, 8, (int)size, (int)size, 64, 64);

        Gui.drawScaledCustomSizeModalRect((int)x, (int)y, 40, 8, 8, 8, (int)size, (int)size, 64, 64);

        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }


    private EntityLivingBase getTarget() {
        if (mc.pointedEntity instanceof EntityLivingBase) {
            return (EntityLivingBase) mc.pointedEntity;
        }
        return null;
    }

    private Vector3f project2D(float x, float y, float z) {
        FloatBuffer screenPos  = BufferUtils.createFloatBuffer(3);
        IntBuffer   viewport   = BufferUtils.createIntBuffer(16);
        FloatBuffer modelview  = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        if (GLU.gluProject(x, y, z, modelview, projection, viewport, screenPos)) {
            ScaledResolution sr = new ScaledResolution(mc);
            int sf = sr.getScaleFactor();
            return new Vector3f(screenPos.get(0) / sf, (Display.getHeight() - screenPos.get(1)) / sf, screenPos.get(2));
        }
        return null;
    }
}
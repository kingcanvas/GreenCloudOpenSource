package greencloud.impl.modules.render;

import greencloud.GreenCloud;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ColorSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Trajectories extends Module {

    private final BooleanSetting useHudColor = new BooleanSetting("Use HUD Color", this, true);
    private final ColorSetting customColor = new ColorSetting("Color", this, new Color(0, 255, 255), () -> !useHudColor.enabled);

    public Trajectories() {
        super("Trajectories", "Predicts where projectiles will go", Category.RENDER);
        this.addSettings(useHudColor, customColor);
    }

    @SubscribeEvent
    public void onRender3D(RenderWorldLastEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.getHeldItem() == null) return;

        ItemStack stack = mc.thePlayer.getHeldItem();
        Item item = stack.getItem();

        boolean isBow = item instanceof ItemBow;
        boolean isPotion = item instanceof ItemPotion && ItemPotion.isSplash(stack.getMetadata());
        boolean isThrowable = item instanceof ItemSnowball || item instanceof ItemEnderPearl || item instanceof ItemEgg;

        if (!isBow && !isPotion && !isThrowable) return;

        double renderPosX = mc.getRenderManager().viewerPosX;
        double renderPosY = mc.getRenderManager().viewerPosY;
        double renderPosZ = mc.getRenderManager().viewerPosZ;

        double posX = renderPosX - (double)(MathHelper.cos(mc.thePlayer.rotationYaw / 180.0F * (float)Math.PI) * 0.16F);
        double posY = renderPosY + (double)mc.thePlayer.getEyeHeight() - 0.10000000149011612D;
        double posZ = renderPosZ - (double)(MathHelper.sin(mc.thePlayer.rotationYaw / 180.0F * (float)Math.PI) * 0.16F);

        float power = 1.0F;
        float gravity = 0.03F;
        float drag = 0.99F;

        if (isBow) {
            power = (float)(72000 - mc.thePlayer.getItemInUseCount()) / 20.0F;
            power = (power * power + power * 2.0F) / 3.0F;
            if (power > 1.0F) power = 1.0F;
            if (power <= 0.1F) power = 1.0F;
            power *= 3.0F;
            gravity = 0.05F;
        } else if (isPotion) {
            power = 0.5f;
            gravity = 0.05F;
        } else {
            power = 1.5f;
        }

        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;

        double motionX = -MathHelper.sin(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI) * power;
        double motionZ = MathHelper.cos(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI) * power;
        double motionY = -MathHelper.sin(pitch / 180.0F * (float)Math.PI) * power;

        List<Vec3> points = new ArrayList<>();
        points.add(new Vec3(posX - renderPosX, posY - renderPosY, posZ - renderPosZ));

        boolean hit = false;
        MovingObjectPosition landingPosition = null;

        for (int i = 0; i < 1000; i++) {
            Vec3 start = new Vec3(posX, posY, posZ);

            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            motionX *= drag;
            motionY *= drag;
            motionZ *= drag;
            motionY -= gravity;

            Vec3 end = new Vec3(posX, posY, posZ);
            MovingObjectPosition collision = mc.theWorld.rayTraceBlocks(start, end, false, true, false);

            if (collision != null) {
                points.add(new Vec3(collision.hitVec.xCoord - renderPosX, collision.hitVec.yCoord - renderPosY, collision.hitVec.zCoord - renderPosZ));
                landingPosition = collision;
                hit = true;
                break;
            } else {
                points.add(new Vec3(posX - renderPosX, posY - renderPosY, posZ - renderPosZ));
            }

            if (posY <= 0) break;
        }

        float r, g, b;

        if (useHudColor.enabled) {
            HUD hud = GreenCloud.moduleManager.getModule(HUD.class);
            int hudColor = hud != null ? hud.hudColor.getColor() : new Color(0, 255, 255).getRGB();
            Color color = new Color(hudColor);
            r = color.getRed() / 255f;
            g = color.getGreen() / 255f;
            b = color.getBlue() / 255f;
        } else {
            Color color = new Color(customColor.getColor());
            r = color.getRed() / 255f;
            g = color.getGreen() / 255f;
            b = color.getBlue() / 255f;
        }

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);


        GL11.glLineWidth(2.0F);
        GL11.glColor4f(r, g, b, 0.5F);
        drawPath(points);


        if (hit && landingPosition != null) {
            double hitX = landingPosition.hitVec.xCoord - renderPosX;
            double hitY = landingPosition.hitVec.yCoord - renderPosY;
            double hitZ = landingPosition.hitVec.zCoord - renderPosZ;

            GL11.glTranslated(hitX, hitY, hitZ);


            if (landingPosition.sideHit != null) {
                switch (landingPosition.sideHit) {
                    case UP:    GL11.glTranslated(0, 0.01, 0); break;
                    case DOWN:  GL11.glTranslated(0, -0.01, 0); break;
                    case NORTH: GL11.glTranslated(0, 0, -0.01); break;
                    case SOUTH: GL11.glTranslated(0, 0, 0.01); break;
                    case WEST:  GL11.glTranslated(-0.01, 0, 0); break;
                    case EAST:  GL11.glTranslated(0.01, 0, 0); break;
                }


                GL11.glColor4f(r, g, b, 1.0f);
                GL11.glLineWidth(2.5F);
                drawX(landingPosition.sideHit);
            }
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void drawPath(List<Vec3> points) {
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (Vec3 v : points) {
            GL11.glVertex3d(v.xCoord, v.yCoord, v.zCoord);
        }
        GL11.glEnd();
    }

    private void drawX(EnumFacing side) {
        double size = 0.25;

        GL11.glBegin(GL11.GL_LINES);

        switch (side) {
            case UP:
            case DOWN:

                GL11.glVertex3d(-size, 0, -size); GL11.glVertex3d(size, 0, size);
                GL11.glVertex3d(-size, 0, size); GL11.glVertex3d(size, 0, -size);
                break;

            case NORTH:
            case SOUTH:

                GL11.glVertex3d(-size, -size, 0); GL11.glVertex3d(size, size, 0);
                GL11.glVertex3d(-size, size, 0); GL11.glVertex3d(size, -size, 0);
                break;

            case WEST:
            case EAST:

                GL11.glVertex3d(0, -size, -size); GL11.glVertex3d(0, size, size);
                GL11.glVertex3d(0, -size, size); GL11.glVertex3d(0, size, -size);
                break;
        }

        GL11.glEnd();
    }
}
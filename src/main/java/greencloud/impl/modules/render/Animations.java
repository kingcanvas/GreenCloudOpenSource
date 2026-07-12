package greencloud.impl.modules.render;

import greencloud.GreenCloud;
import greencloud.impl.managers.notification.NotificationManager;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.ModeSetting;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import org.lwjgl.opengl.GL11;

public class Animations extends Module {
    public ModeSetting mode = new ModeSetting("Mode", this, "Default", "Default", "Leaked", "Astolfo", "Moon", "Sigma", "Spinning", "GreenCloud");
    private Field equipProgressField;
    private Field prevEquipProgressField;
    private float spin = 0f;
    private long lastTime = 0;

    public Animations() {
        super("Animations", "Changes the sword blocking animation.", Category.RENDER);
        addSetting(mode);
        setupReflection();
    }

    @Override
    public void onEnable() {
        NotificationManager.getInstance().addNotification(
                "Animations",
                "Requires optifine.",
                NotificationManager.NotificationType.ERROR,
                2500
        );
    }

    private void setupReflection() {
        try {
            equipProgressField = ItemRenderer.class.getDeclaredField("equippedProgress");
            prevEquipProgressField = ItemRenderer.class.getDeclaredField("prevEquippedProgress");
        } catch (NoSuchFieldException e) {
            try {
                equipProgressField = ItemRenderer.class.getDeclaredField("field_78454_c");
                prevEquipProgressField = ItemRenderer.class.getDeclaredField("field_78451_d");
            } catch (Exception ignored) {
                GreenCloud.logger.error("Failed to find ItemRenderer fields for Animations.");
            }
        }
        if (equipProgressField != null) equipProgressField.setAccessible(true);
        if (prevEquipProgressField != null) prevEquipProgressField.setAccessible(true);
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        ItemStack currentItem = mc.thePlayer.getCurrentEquippedItem();
        if (currentItem == null || !(currentItem.getItem() instanceof ItemSword)) return;
        if (!mc.thePlayer.isBlocking()) return;
        if (mc.gameSettings.thirdPersonView != 0) return;

        event.setCanceled(true);

        float partialTicks = event.partialTicks;
        float swingProgress = mc.thePlayer.getSwingProgress(partialTicks);

        float equipProgress = 0.0f;
        try {
            float f  = equipProgressField.getFloat(mc.getItemRenderer());
            float f1 = prevEquipProgressField.getFloat(mc.getItemRenderer());
            equipProgress = 1.0f - (f1 + (f - f1) * partialTicks);
        } catch (Exception ignored) {}

        GlStateManager.pushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);

        switch (mode.currentMode) {
            case "Leaked":    doLeakedAnimation(swingProgress); break;
            case "Astolfo":   doAstolfo(equipProgress, swingProgress); break;
            case "Moon":      doMoon(equipProgress, swingProgress); break;
            case "Sigma":     doSigma(equipProgress, swingProgress); break;
            case "Spinning":  doSpinning(equipProgress, swingProgress); break;
            case "GreenCloud":doGreenCloud(equipProgress, swingProgress); break;
            default:          doDefaultAnimation(equipProgress, swingProgress); break;
        }

        mc.getItemRenderer().renderItem(mc.thePlayer, currentItem, ItemCameraTransforms.TransformType.FIRST_PERSON);

        GL11.glPopAttrib();
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.depthFunc(515);
    }

    private void doDefaultAnimation(float equipProgress, float swingProgress) {
        transformFirstPersonItem(equipProgress, swingProgress);
        doBlockTransformations();
    }

    private void doLeakedAnimation(float swingProgress) {
        float var2 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        transformFirstPersonItem(0f, 0f);
        GlStateManager.translate(0.08f, 0.02f, 0f);
        doBlockTransformations();
        GlStateManager.rotate(-var2 * 41f, 1.1f, 0.8f, -0.3f);
    }

    private void doAstolfo(float equipProgress, float swingProgress) {
        float var7 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        transformFirstPersonItem(0f, 0f);
        GlStateManager.translate(-0.08f, 0.12f, 0f);
        GlStateManager.rotate(-var7 * 58f / 2f, var7 / 2f, 1f, 0.5f);
        GlStateManager.rotate(-var7 * 43f, 1f, var7 / 3f, 0f);
        doBlockTransformations();
    }

    private void doMoon(float equipProgress, float swingProgress) {
        float var8 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        transformFirstPersonItem(0f, 0f);
        GlStateManager.translate(-0.08f, 0.12f, 0f);
        GlStateManager.rotate(-var8 * 65f / 2f, var8 / 2f, 1f, 4f);
        GlStateManager.rotate(-var8 * 60f, 1f, var8 / 3f, 0f);
        doBlockTransformations();
    }

    private void doSigma(float equipProgress, float swingProgress) {
        float var9 = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
        transformFirstPersonItem(equipProgress * 0.5f, 0f);
        GlStateManager.rotate(-var9 * 55f / 2f, -8f, 0f, 9f);
        GlStateManager.rotate(-var9 * 45f, 1f, var9 / 2f, 0f);
        doBlockTransformations();
        GlStateManager.translate(1.2f, 0.3f, 0.5f);
        GlStateManager.translate(-1f, mc.thePlayer.isSneaking() ? -0.1f : -0.2f, 0.2f);
    }

    private void doSpinning(float equipProgress, float swingProgress) {
        if (lastTime == 0L) lastTime = System.currentTimeMillis();
        long delta = System.currentTimeMillis() - lastTime;
        lastTime = System.currentTimeMillis();
        spin += delta * 0.3f;
        if (spin > 360f) spin -= 360f;
        GlStateManager.translate(-0.04f, 0.1f, 0f);
        transformFirstPersonItem(equipProgress / 2.5f, 0f);
        GlStateManager.rotate(-90f, 1f, 0f, 0.2f);
        GlStateManager.rotate(spin, 0f, -1f, 0f);
    }

    private void doGreenCloud(float equipProgress, float swingProgress) {
        float bob = MathHelper.sin(System.currentTimeMillis() / 700.0f) * 0.05f;
        float smooth = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        float snappy = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
        transformFirstPersonItem(equipProgress, 0f);
        GlStateManager.translate(bob, 0.15f + bob, -0.1f);
        GlStateManager.rotate(bob * 30f, 0f, 0f, 1f);
        GlStateManager.rotate(-smooth * 60f, 1.5f, 2f, 0f);
        GlStateManager.rotate(-snappy * 30f, 0f, 0f, 1f);
        doBlockTransformations();
        GlStateManager.translate(-0.1f, 0.1f, 0f);
    }

    private void transformFirstPersonItem(float equipProgress, float swingProgress) {
        GlStateManager.translate(0.56f, -0.52f, -0.72f);
        GlStateManager.translate(0f, equipProgress * -0.6f, 0f);
        GlStateManager.rotate(45f, 0f, 1f, 0f);
        float f  = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(f  * -20f, 0f, 1f, 0f);
        GlStateManager.rotate(f1 * -20f, 0f, 0f, 1f);
        GlStateManager.rotate(f1 * -80f, 1f, 0f, 0f);
        GlStateManager.scale(0.4f, 0.4f, 0.4f);
    }

    private void doBlockTransformations() {
        GlStateManager.translate(-0.5f, 0.2f, 0f);
        GlStateManager.rotate(30f, 0f, 1f, 0f);
        GlStateManager.rotate(-80f, 1f, 0f, 0f);
        GlStateManager.rotate(60f, 0f, 1f, 0f);
    }
}
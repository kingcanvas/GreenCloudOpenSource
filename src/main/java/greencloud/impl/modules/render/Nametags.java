package greencloud.impl.modules.render;


import greencloud.GreenCloud;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.modules.utility.AntiBot;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.utils.render.GreenRender;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Nametags extends Module {

    public static Nametags instance;


    private final BooleanSetting showArmor     = new BooleanSetting("Show Armor",     this, true);
    private final BooleanSetting showEnchants  = new BooleanSetting("Show Enchants",  this, true);
    public  final BooleanSetting showInvisible = new BooleanSetting("Show Invisible", this, true);
    private final NumberSetting  blurStr       = new NumberSetting ("Blur Strength",  this, 0, 0, 40, 1);
    private final NumberSetting  bgAlpha       = new NumberSetting ("BG Alpha",       this, 120, 0, 255, 5);
    private final NumberSetting  cornerRadius  = new NumberSetting ("Corner Radius",  this, 4, 0, 12, 1);

    private final List<TagData> renderQueue = new ArrayList<>();

    public Nametags() {
        super("Nametags", "Renders beautiful 2D projected nametags.", Category.RENDER);
        instance = this;
        addSettings(showArmor, showEnchants, showInvisible, blurStr, bgAlpha, cornerRadius);
    }

    @SubscribeEvent
    public void onRenderVanillaNametag(RenderLivingEvent.Specials.Pre<EntityPlayer> event) {
        if (event.entity instanceof EntityPlayer && event.entity != mc.thePlayer) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        renderQueue.clear();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) continue;
            if (player.isDead) continue;
            if (player.isInvisible() && !showInvisible.enabled) continue;
            if (AntiBot.instance != null && AntiBot.instance.isToggled() && AntiBot.instance.isBot(player)) continue;

            double pmScale = 1.0;
            PlayerModel playerModel = GreenCloud.moduleManager.getModule(PlayerModel.class);
            if (playerModel != null && playerModel.isToggled()) {
                pmScale = playerModel.size.getValue();
            }

            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks - mc.getRenderManager().viewerPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks - mc.getRenderManager().viewerPosY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks - mc.getRenderManager().viewerPosZ;

            double visualHeight = player.height * pmScale;
            Vector3f bottom = project2D((float) x, (float) y, (float) z);
            Vector3f top    = project2D((float) x, (float) (y + visualHeight), (float) z);
            Vector3f tagPos = project2D((float) x, (float) (y + visualHeight + 0.4 * pmScale), (float) z);

            if (tagPos != null && bottom != null && top != null && tagPos.z >= 0.0f && tagPos.z < 1.0f) {
                float screenHeight = Math.abs(bottom.y - top.y);
                float autoScale = screenHeight / 100f;

                autoScale = net.minecraft.util.MathHelper.clamp_float(autoScale, 0.4f, 2.5f);

                renderQueue.add(new TagData(player, tagPos, autoScale));
            }
        }
    }

    @SubscribeEvent
    public void onRender2D(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (renderQueue.isEmpty()) return;

        renderQueue.sort((t1, t2) -> Float.compare(t2.pos.z, t1.pos.z));

        for (TagData tag : renderQueue) {
            renderNametag(tag.player, tag.pos.x, tag.pos.y, tag.scale);
        }
    }

    private void renderNametag(EntityPlayer player, float screenX, float screenY, float scale) {
        String displayString = player.getDisplayName().getFormattedText();

        float textWidth  = GreenRender.strWBold(displayString);
        float fontHeight = GreenRender.fontHBold();
        float paddingX   = 4;
        float paddingY   = 3;
        float boxW       = textWidth + paddingX * 2;
        float boxH       = fontHeight + paddingY * 2;
        float scaledW    = boxW * scale;
        float scaledH    = boxH * scale;
        float r          = Math.min((float) cornerRadius.value * scale, scaledH / 2f);
        float drawX      = screenX - (scaledW / 2f);
        float drawY      = screenY;

        if (blurStr.value > 0)
            GreenRender.blurRounded(drawX, drawY, scaledW, scaledH, (float) blurStr.value, (int) r);

        GreenRender.fillRR(drawX, drawY, scaledW, scaledH, r, new Color(0, 0, 0, (int) bgAlpha.value).getRGB());

        GlStateManager.pushMatrix();
        GlStateManager.translate(screenX, screenY, 0);
        GlStateManager.scale(scale, scale, 1);
        GreenRender.drawStringBold(displayString, -textWidth / 2f, paddingY, -1);
        GlStateManager.popMatrix();

        if (showArmor.enabled) renderEquipment(player, screenX, screenY, scale);

        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void renderEquipment(EntityPlayer player, float screenX, float screenY, float scale) {
        List<ItemStack> equipment = new ArrayList<>();

        if (player.getHeldItem() != null) equipment.add(player.getHeldItem());
        for (int i = 3; i >= 0; i--) {
            if (player.inventory.armorInventory[i] != null)
                equipment.add(player.inventory.armorInventory[i]);
        }

        if (equipment.isEmpty()) return;

        float slotW   = 18;
        float slotH   = 22;
        float gap     = 3;
        float totalW  = equipment.size() * slotW + (equipment.size() - 1) * gap;
        float sSlotW  = slotW * scale;
        float sSlotH  = slotH * scale;
        float sGap    = gap * scale;
        float sTotalW = totalW * scale;
        float startX  = screenX - (sTotalW / 2f);
        float startY  = screenY - (30 * scale);
        float r       = Math.min((float) cornerRadius.value * scale, sSlotH / 2f);
        float curX    = startX;

        for (ItemStack stack : equipment) {
            if (blurStr.value > 0)
                GreenRender.blurRounded(curX, startY, sSlotW, sSlotH, (float) blurStr.value, (int) r);
            GreenRender.fillRR(curX, startY, sSlotW, sSlotH, r, new Color(0, 0, 0, (int) bgAlpha.value).getRGB());
            curX += sSlotW + sGap;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(screenX, screenY, 0);
        GlStateManager.scale(scale, scale, 1);

        float itemStartX = -totalW / 2f;
        float itemStartY = -30;

        for (ItemStack stack : equipment) {
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemIntoGUI(stack, (int)(itemStartX + 1), (int)(itemStartY + 3));

            if (stack.stackSize > 1) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(itemStartX + 10, itemStartY + 9, 0);
                GlStateManager.scale(0.6f, 0.6f, 1f);
                GreenRender.drawStringBold(String.valueOf(stack.stackSize), 0, 0, Color.WHITE);
                GlStateManager.popMatrix();
            }

            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableTexture2D();

            if (stack.isItemStackDamageable()) {
                float pct      = 1.0f - ((float) stack.getItemDamage() / (float) stack.getMaxDamage());
                Color barColor = Color.getHSBColor(pct / 3f, 1f, 1f);
                float barX     = itemStartX + 1;
                float barY     = itemStartY + 18;
                float barW     = 16;
                float barH     = 2f;
                GreenRender.fillRR(barX, barY, barW, barH, 1f, new Color(0, 0, 0, 200).getRGB());
                GreenRender.fillRR(barX, barY, barW * pct, barH, 1f, barColor.getRGB());
            }

            GlStateManager.enableTexture2D();

            if (showEnchants.enabled) renderEnchantments(stack, itemStartX, itemStartY, slotW);

            itemStartX += slotW + gap;
        }

        GlStateManager.popMatrix();
    }

    private void renderEnchantments(ItemStack stack, float slotX, float slotY, float slotW) {
        NBTTagList enchants = stack.getEnchantmentTagList();
        if (enchants == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5f, 0.5f, 1f);

        float enchantY = (slotY - 4) * 2f;
        float centerX  = (slotX + slotW / 2f) * 2f;

        for (int i = 0; i < enchants.tagCount(); i++) {
            short  id      = enchants.getCompoundTagAt(i).getShort("id");
            short  lvl     = enchants.getCompoundTagAt(i).getShort("lvl");
            String encText = getEnchantText(id, lvl);

            if (encText != null) {
                float textW = GreenRender.strWBold(encText);
                GreenRender.drawStringBold(encText, centerX - (textW / 2f), enchantY, 0xFFFFAA);
                enchantY -= (GreenRender.fontHBold() + 1);
            }
        }

        GlStateManager.popMatrix();
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

    private String getEnchantText(int id, int level) {
        String p = "";
        switch (id) {
            case 0:  p = "P";   break; case 1:  p = "FP";  break;
            case 2:  p = "FF";  break; case 3:  p = "BP";  break;
            case 4:  p = "PP";  break; case 16: p = "S";   break;
            case 19: p = "K";   break; case 20: p = "FA";  break;
            case 34: p = "U";   break; case 48: p = "Pow"; break;
            case 50: p = "Pun"; break; case 51: p = "F";   break;
            default: return null;
        }
        return p + level;
    }

    private static class TagData {
        EntityPlayer player;
        Vector3f     pos;
        float        scale;

        TagData(EntityPlayer player, Vector3f pos, float scale) {
            this.player = player;
            this.pos    = pos;
            this.scale  = scale;
        }
    }
}
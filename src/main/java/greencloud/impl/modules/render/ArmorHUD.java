package greencloud.impl.modules.render;

import greencloud.impl.managers.player.PositionManager;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.utils.font.FontUtil;
import greencloud.impl.utils.render.AnimationUtil;
import greencloud.impl.utils.render.GreenRender;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class ArmorHUD extends Module {

    public final NumberSetting   posX         = new NumberSetting ("X",             this, 2,   0, 2000, 1, () -> false);
    public final NumberSetting   posY         = new NumberSetting ("Y",             this, 100, 0, 2000, 1, () -> false);
    private final BooleanSetting blur         = new BooleanSetting("Blur",          this, true);
    private final NumberSetting  blurStrength = new NumberSetting ("Blur Strength", this, 0,  0, 40,   1);
    private final BooleanSetting background   = new BooleanSetting("Background",    this, true);
    private final NumberSetting  bgAlpha      = new NumberSetting ("BG Alpha",      this, 60,  0, 255,  5);

    private final Map<Integer, Float> barAnimations = new HashMap<>();
    private boolean draggableRegistered = false;

    private static final int   ROW_HEIGHT   = 26;
    private static final int   ICON_SIZE    = 16;
    private static final int   PADDING      = 6;
    private static final int   DIVIDER_X    = PADDING + ICON_SIZE + 6;
    private static final int   BAR_WIDTH    = 50;
    private static final float BAR_H        = 3f;
    private static final int   PANEL_WIDTH  = DIVIDER_X + 1 + 8 + BAR_WIDTH + PADDING;
    private static final int   PANEL_HEIGHT = PADDING + ROW_HEIGHT * 4 + PADDING;
    private static final int   CORNER       = 6;

    public ArmorHUD() {
        super("ArmorHUD", "Displays your armor status.", Category.RENDER);
        addSettings(posX, posY, blur, blurStrength, background, bgAlpha);
    }

    public int getX()      { return (int) posX.value; }
    public int getY()      { return (int) posY.value; }
    public int getWidth()  { return PANEL_WIDTH; }
    public int getHeight() { return PANEL_HEIGHT; }
    public void setPosition(int x, int y) { posX.value = x; posY.value = y; }

    @SubscribeEvent
    public void onRender2D(RenderGameOverlayEvent.Text event) {
        ensureDraggableRegistered();
        if (mc.thePlayer == null) return;

        float x = (float) posX.value;
        float y = (float) posY.value;
        float w = PANEL_WIDTH;
        float h = PANEL_HEIGHT;

        if (blur.enabled && blurStrength.value > 0)
            GreenRender.blurRounded(x, y, w, h, (float) blurStrength.value, CORNER);

        GreenRender.fillRR(x, y, w, h, CORNER, new Color(0, 0, 0, background.enabled ? (int) bgAlpha.value : 30));

        float divX = x + DIVIDER_X;
        GreenRender.fillRect(divX, y + PADDING, 1, h - PADDING * 2, new Color(255, 255, 255, 40));

        for (int slot = 3; slot >= 0; slot--) {
            int       row   = 3 - slot;
            float     rowY  = y + PADDING + row * ROW_HEIGHT;
            ItemStack stack = mc.thePlayer.inventory.armorInventory[slot];
            if (stack == null) continue;

            float durability = stack.getMaxDamage() == 0 ? 1.0f
                    : (float)(stack.getMaxDamage() - stack.getItemDamage()) / stack.getMaxDamage();
            float anim = barAnimations.getOrDefault(slot, durability);
            anim = AnimationUtil.moveUD(anim, durability, 0.1f, 0.02f);
            barAnimations.put(slot, anim);

            float textX = divX + 8;
            float barY  = rowY + 16;

            GreenRender.fillRR(textX, barY, BAR_WIDTH, BAR_H, (int)(BAR_H / 2), new Color(30, 40, 50, 180));
            float fillW = BAR_WIDTH * Math.max(0, Math.min(1, anim));
            if (fillW > BAR_H)
                GreenRender.fillRR(textX, barY, fillW, BAR_H, (int)(BAR_H / 2), new Color(80, 200, 100, 255));
        }

        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);

        for (int slot = 3; slot >= 0; slot--) {
            int       row   = 3 - slot;
            float     rowY  = y + PADDING + row * ROW_HEIGHT;
            ItemStack stack = mc.thePlayer.inventory.armorInventory[slot];
            if (stack == null) continue;

            renderItemStack(stack, x + PADDING, rowY + (ROW_HEIGHT - ICON_SIZE) / 2f);

            float  durability = stack.getMaxDamage() == 0 ? 1.0f
                    : (float)(stack.getMaxDamage() - stack.getItemDamage()) / stack.getMaxDamage();
            String pctText    = (int)(durability * 100) + "%";
            float  textX      = divX + 8;
            float  textY      = rowY + 3;

            if (FontUtil.normal != null) FontUtil.normal.drawStringWithShadow(pctText, textX, textY, -1);
            else mc.fontRendererObj.drawStringWithShadow(pctText, (int) textX, (int) textY, -1);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.bindTexture(0);
    }

    private void ensureDraggableRegistered() {
        if (draggableRegistered) return;
        draggableRegistered = true;
        PositionManager pm = PositionManager.getInstance();
        pm.addElement("ArmorHUD", getX(), getY(), getWidth(), getHeight(), null);
        pm.elements.stream().filter(e -> e.name.equals("ArmorHUD")).findFirst()
                .ifPresent(e -> e.setPositionUpdateCallback((nx, ny) -> setPosition(nx, ny)));
    }

    private void renderItemStack(ItemStack stack, float x, float y) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(515);
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, (int) x, (int) y);
        GL11.glPopAttrib();
    }
}
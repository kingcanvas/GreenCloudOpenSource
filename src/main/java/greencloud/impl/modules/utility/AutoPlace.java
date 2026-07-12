/*
* Sparky don't change this.
* this is for intave ac
* */
package greencloud.impl.modules.utility;

import greencloud.impl.managers.notification.NotificationManager;
import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

public class AutoPlace extends Module {

    public BooleanSetting holdRight = new BooleanSetting("Hold Right", this, true);
    public NumberSetting minPlaceDelay = new NumberSetting("Min Delay", this, 50, 0, 500, 10);

    private long lastPlaceTime = 0;
    private BlockPos lastPos = null;
    private Field rightClickTimerField;

    public AutoPlace() {
        super("AutoPlace", "Places blocks fast", Category.UTILITY);
        addSettings(holdRight, minPlaceDelay);

        try {
            rightClickTimerField = ReflectionHelper.findField(Minecraft.class, "field_71467_ac", "rightClickDelayTimer");
            rightClickTimerField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        NotificationManager.getInstance().addNotification(
                "AutoPlace",
                "ONLY MADE FOR INTAVE AND BAD AC'S.",
                NotificationManager.NotificationType.WARNING,
                2500
        );
    }

    @Override
    public void onDisable() {
        lastPos = null;
        super.onDisable();
    }

    @SubscribeEvent
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        if (mc.currentScreen != null || mc.thePlayer.capabilities.isFlying) return;

        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof ItemBlock)) return;

        if (holdRight.enabled && !Mouse.isButtonDown(1)) return;

        MovingObjectPosition mouseOver = mc.objectMouseOver;
        if (mouseOver != null && mouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {

            if (mouseOver.sideHit == EnumFacing.UP || mouseOver.sideHit == EnumFacing.DOWN) return;

            BlockPos pos = mouseOver.getBlockPos();
            if (lastPos == null || !pos.equals(lastPos)) {
                if (mc.theWorld.getBlockState(pos).getBlock() != Blocks.air) {
                    long time = System.currentTimeMillis();
                    if (time - lastPlaceTime >= minPlaceDelay.getValue()) {

                        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, stack, pos, mouseOver.sideHit, mouseOver.hitVec)) {
                            mc.thePlayer.swingItem();
                            mc.getItemRenderer().resetEquippedProgress();

                            try {
                                if (rightClickTimerField != null) {
                                    rightClickTimerField.setInt(mc, 0);
                                }
                            } catch (Exception ignored) {}

                            lastPos = pos;
                            lastPlaceTime = time;
                        }
                    }
                }
            }
        }
    }
}
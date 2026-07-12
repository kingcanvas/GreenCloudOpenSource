package greencloud.impl.modules.combat;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ModeSetting;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class MoreKB extends Module {

    private final ModeSetting mode;
    private final BooleanSetting intelligent;
    private boolean shouldSprintReset;

    public MoreKB() {
        super("MoreKB", "Increases knockback dealt to enemies.", Category.COMBAT);

        this.mode = new ModeSetting("Mode", this, "Legit",
                "Legit", "LessPacket", "Packet", "DoublePacket", "SprintFast");
        this.intelligent = new BooleanSetting("Intelligent", this, false);
        this.shouldSprintReset = false;

        addSettings(mode, intelligent);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        EntityLivingBase entity = null;


        if (mc.objectMouseOver != null &&
                mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY &&
                mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            entity = (EntityLivingBase) mc.objectMouseOver.entityHit;
        }

        if (entity == null) {
            return;
        }


        if (intelligent.enabled) {
            double x = mc.thePlayer.posX - entity.posX;
            double z = mc.thePlayer.posZ - entity.posZ;
            float calcYaw = (float) (MathHelper.atan2(z, x) * 180.0 / Math.PI - 90.0);
            float diffY = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - entity.rotationYawHead));


            if (diffY > 120.0f) {
                return;
            }
        }


        if (entity.hurtTime != 10) {
            return;
        }

        String selectedMode = mode.currentMode;

        switch (selectedMode) {
            case "Packet":
                mc.thePlayer.sendQueue.addToSendQueue(
                        new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.thePlayer.sendQueue.addToSendQueue(
                        new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.thePlayer.setSprinting(true);
                break;

            case "DoublePacket":
                mc.thePlayer.sendQueue.addToSendQueue(
                        new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.thePlayer.sendQueue.addToSendQueue(
                        new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.thePlayer.sendQueue.addToSendQueue(
                        new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.thePlayer.sendQueue.addToSendQueue(
                        new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.thePlayer.setSprinting(true);
                break;

            case "Legit":
                shouldSprintReset = true;
                mc.thePlayer.setSprinting(false);
                mc.thePlayer.setSprinting(true);
                shouldSprintReset = false;
                break;

            case "LessPacket":
                if (mc.thePlayer.isSprinting()) {
                    mc.thePlayer.setSprinting(false);
                }
                mc.thePlayer.sendQueue.addToSendQueue(
                        new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.thePlayer.setSprinting(true);
                break;

            case "SprintFast":
                shouldSprintReset = true;
                mc.thePlayer.setSprinting(false);
                mc.thePlayer.sendQueue.addToSendQueue(
                        new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.thePlayer.setSprinting(true);
                shouldSprintReset = false;
                break;
        }
    }
}
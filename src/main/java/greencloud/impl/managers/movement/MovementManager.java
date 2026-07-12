package greencloud.impl.managers.movement;

import greencloud.event.MoveEvent;
import greencloud.event.StepEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class MovementManager {
    private final Minecraft mc = Minecraft.getMinecraft();

    public MovementManager() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null) return;

        MoveEvent moveEvent = new MoveEvent(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ);
        MinecraftForge.EVENT_BUS.post(moveEvent);

        if (!moveEvent.isCanceled()) {
            mc.thePlayer.motionX = moveEvent.x;
            mc.thePlayer.motionY = moveEvent.y;
            mc.thePlayer.motionZ = moveEvent.z;
        }

        if (mc.thePlayer.isCollidedHorizontally && mc.thePlayer.onGround) {
            StepEvent preStep = new StepEvent(0.6f, StepEvent.State.PRE);
            MinecraftForge.EVENT_BUS.post(preStep);

            mc.thePlayer.stepHeight = preStep.getStepHeight();

        } else {
            mc.thePlayer.stepHeight = 0.6f;
        }
    }
}
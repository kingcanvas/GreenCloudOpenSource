package greencloud.event;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class MoveEvent extends Event {
    public double x, y, z;
    public boolean safeWalk;

    public MoveEvent(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.safeWalk = false;
    }

    public void setSpeed(double speed) {
        float yaw = net.minecraft.client.Minecraft.getMinecraft().thePlayer.rotationYaw;
        this.x = -Math.sin(Math.toRadians(yaw)) * speed;
        this.z = Math.cos(Math.toRadians(yaw)) * speed;
    }
}
package greencloud.impl.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public class RotationUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static float getGCD() {
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        return f * f * f * 1.2F;
    }

    public static float wrapDeg(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    public static Rotation getRotations(Entity entity) {
        if (entity == null || mc.thePlayer == null) return null;
        double posX = entity.posX;
        double posY = entity.posY + (entity.height * 0.7D);
        double posZ = entity.posZ;
        return getRotationsToPosition(posX, posY, posZ);
    }

    public static Rotation getRotationsToPosition(double x, double y, double z) {
        if (mc.thePlayer == null) return null;
        double diffX = x - mc.thePlayer.posX;
        double diffY = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = z - mc.thePlayer.posZ;
        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, distance)));
        return new Rotation(yaw, pitch);
    }

    public static double getRotationDifference(Rotation target, Rotation current) {
        float yawDiff = wrapDeg(target.getYaw() - current.getYaw());
        float pitchDiff = target.getPitch() - current.getPitch();
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

}
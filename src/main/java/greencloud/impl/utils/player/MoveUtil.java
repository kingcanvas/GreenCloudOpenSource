package greencloud.impl.utils.player;

import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;

public class MoveUtil {

    public static void fixMovement(MovementInput input, float targetYaw, float visualYaw, float moveForward, float moveStrafe) {
        double intendedAngle = Math.toDegrees(getDirection(visualYaw, moveForward, moveStrafe));

        if (moveForward == 0 && moveStrafe == 0) return;

        float bestForward = 0, bestStrafe = 0, closestDiff = Float.MAX_VALUE;

        for (float f = -1F; f <= 1F; f += 1F) {
            for (float s = -1F; s <= 1F; s += 1F) {
                if (f == 0 && s == 0) continue;

                double predictedAngle = Math.toDegrees(getDirection(targetYaw, f, s));
                double diff = Math.abs(MathHelper.wrapAngleTo180_double(intendedAngle - predictedAngle));

                if (diff < closestDiff) {
                    closestDiff = (float) diff;
                    bestForward = f;
                    bestStrafe = s;
                }
            }
        }

        input.moveForward = bestForward;
        input.moveStrafe = bestStrafe;
    }

    public static double getDirection(float yaw, float forward, float strafe) {
        if (forward < 0F) yaw += 180F;
        float forwardMult = 1F;
        if (forward < 0F) forwardMult = -0.5F;
        else if (forward > 0F) forwardMult = 0.5F;

        if (strafe > 0F) yaw -= 90F * forwardMult;
        if (strafe < 0F) yaw += 90F * forwardMult;

        return Math.toRadians(yaw);
    }
}
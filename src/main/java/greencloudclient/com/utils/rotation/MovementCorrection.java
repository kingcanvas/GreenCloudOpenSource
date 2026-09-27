package greencloudclient.com.utils.rotation;

import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;

import java.util.Random;

public class MovementCorrection {

    private static final float HYSTERESIS = 8f;

    private final Random random = new Random();
    private int curF, curS;
    private int pendF, pendS, pendTicks;
    private int lastKeyF, lastKeyS;

    public void reset() {
        curF = curS = 0;
        pendF = pendS = pendTicks = 0;
        lastKeyF = lastKeyS = 0;
    }

    public void apply(MovementInput input, float serverYaw, float cameraYaw) {
        float rawF = input.moveForward, rawS = input.moveStrafe;
        int keyF = (int) Math.signum(rawF), keyS = (int) Math.signum(rawS);
        boolean keysChanged = keyF != lastKeyF || keyS != lastKeyS;
        lastKeyF = keyF;
        lastKeyS = keyS;

        if (keyF == 0 && keyS == 0) {
            curF = curS = 0;
            pendTicks = 0;
            return;
        }

        float scale = Math.max(Math.abs(rawF), Math.abs(rawS));
        float intended = direction(cameraYaw, keyF, keyS);

        int bestF = 0, bestS = 0;
        float bestDiff = Float.MAX_VALUE;
        for (int f = -1; f <= 1; f++) {
            for (int s = -1; s <= 1; s++) {
                if (f == 0 && s == 0) continue;
                float diff = Math.abs(MathHelper.wrapAngleTo180_float(intended - direction(serverYaw, f, s)));
                if (f == curF && s == curS) diff -= HYSTERESIS;
                if (diff < bestDiff) {
                    bestDiff = diff;
                    bestF = f;
                    bestS = s;
                }
            }
        }

        if (keysChanged || (curF == 0 && curS == 0)) {
            curF = bestF;
            curS = bestS;
            pendTicks = 0;
        } else if (bestF != curF || bestS != curS) {
            if (bestF != pendF || bestS != pendS) {
                pendF = bestF;
                pendS = bestS;
                pendTicks = random.nextFloat() < 0.4f ? 1 : 0;
            }
            if (pendTicks-- <= 0) {
                curF = pendF;
                curS = pendS;
            }
        } else {
            pendF = curF;
            pendS = curS;
        }

        input.moveForward = curF * scale;
        input.moveStrafe = curS * scale;
    }

    private static float direction(float yaw, int forward, int strafe) {
        float rad = yaw * 0.017453292F;
        float sin = MathHelper.sin(rad), cos = MathHelper.cos(rad);
        float mx = strafe * cos - forward * sin;
        float mz = forward * cos + strafe * sin;
        return (float) Math.toDegrees(Math.atan2(mz, mx));
    }
}

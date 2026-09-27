package greencloudclient.com.utils.rotation;

import net.minecraft.util.MathHelper;

import java.util.Random;

public class TrackingError {

    private final Random random = new Random();
    private final Noise noise = new Noise();

    private int ticksOnTarget;
    private float runVelocity;
    private float surpriseH, surpriseV;
    private int lapseTicks, lapseLength;
    private float lapseStrength;

    public void reset() {
        ticksOnTarget = 0;
        runVelocity = 0f;
        surpriseH = surpriseV = 0f;
        lapseTicks = 0;
        noise.reseed();
    }

    public float[] update(float lateral, float selfSpeed, float dist, float width, float height, float error) {
        error = MathHelper.clamp_float(error, 0f, 1f);
        dist = Math.max(dist, 0.5f);
        ticksOnTarget++;
        double t = noise.seconds();

        if (Math.signum(lateral) != Math.signum(runVelocity) && Math.abs(runVelocity) > 0.06f) {
            surpriseH += runVelocity * error * (2f + random.nextFloat() * 2.5f);
            surpriseV += (float) random.nextGaussian() * 0.04f * error;
            ticksOnTarget = Math.min(ticksOnTarget, 6 + random.nextInt(6));
            runVelocity = lateral;
        } else {
            runVelocity += (lateral - runVelocity) * 0.35f;
        }
        surpriseH *= 0.8f;
        surpriseV *= 0.8f;

        if (lapseTicks <= 0 && random.nextFloat() < error * 0.006f) {
            lapseLength = lapseTicks = 6 + random.nextInt(11);
            lapseStrength = 0.8f + random.nextFloat() * 0.8f;
        }
        float lapse = 0f;
        if (lapseTicks > 0) {
            lapse = lapseStrength * MathHelper.sin((float) Math.PI * lapseTicks / lapseLength);
            lapseTicks--;
        }

        float settle = 1f + 1.2f * (float) Math.exp(-ticksOnTarget / 16.0);
        float motion = Math.abs(lateral) * 1.2f + selfSpeed * 0.6f;
        float spread = error * (settle + lapse);

        double pace = t * (0.35 + Math.min(0.4, Math.abs(lateral) * 1.5));
        float h = (float) noise.fractal(pace, 307L) * (spread * width * 0.45f + error * motion * 0.5f) + surpriseH;
        float v = (float) noise.fractal(pace * 0.8, 409L) * spread * height * 0.07f + surpriseV;

        float yaw = (float) Math.toDegrees(Math.atan2(h, dist));
        float pitch = (float) -Math.toDegrees(Math.atan2(v, dist));
        return new float[]{yaw, pitch};
    }
}

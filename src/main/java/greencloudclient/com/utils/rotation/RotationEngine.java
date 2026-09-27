package greencloudclient.com.utils.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

import java.util.Random;

public class RotationEngine {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static final class Profile {
        public float speedMin = 5f, speedMax = 6f;
        public float reactionMs = 150f;
        public float smoothing = 0.5f;
        public float error = 0.5f;
        public float tremor = 0.5f, tremorSpeed = 1.5f;
    }

    public float yaw, pitch;

    private final Random random = new Random();
    private final Noise noise = new Noise();

    private boolean moving;
    private float moveYaw, movePitch, moveCurve, moveTicks, moveElapsed;
    private int waitTicks;
    private float pendingOvershoot;

    private float pursuitYaw, pursuitPitch;
    private float residualYaw, residualPitch;
    private float tremorYaw, tremorPitch;

    public void reset(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        moving = false;
        waitTicks = 0;
        pursuitYaw = pursuitPitch = 0f;
        residualYaw = residualPitch = 0f;
        tremorYaw = tremorPitch = 0f;
        pendingOvershoot = 0f;
        noise.reseed();
    }

    public void overshootNext(float amount) {
        pendingOvershoot = Math.max(0f, amount);
    }

    public void cancelMotion() {
        moving = false;
        waitTicks = 0;
        pursuitYaw = pursuitPitch = 0f;
    }

    public void stepHuman(float targetYaw, float targetPitch, float velYaw, float velPitch,
                          float tolerance, float targetSize, Profile p) {
        double t = noise.seconds();
        float error = MathHelper.clamp_float(p.error, 0f, 1f);
        float smoothing = MathHelper.clamp_float(p.smoothing, 0f, 1f);

        float gain = MathHelper.clamp_float(1f - error * 0.35f + (float) noise.fractal(t * 0.5, 701L) * error * 0.15f, 0.4f, 1.05f);
        float adapt = 0.55f - 0.4f * smoothing;
        pursuitYaw += (velYaw * gain - pursuitYaw) * adapt;
        pursuitPitch += (velPitch * gain - pursuitPitch) * adapt;
        float dYaw = pursuitYaw;
        float dPitch = pursuitPitch;

        if (!moving) {
            if (waitTicks > 0) {
                waitTicks--;
            } else {
                float eYaw = MathHelper.wrapAngleTo180_float(targetYaw - yaw) - pursuitYaw;
                float ePitch = targetPitch - pitch - pursuitPitch;
                float dist = MathHelper.sqrt_float(eYaw * eYaw + ePitch * ePitch);
                if (dist > tolerance) plan(eYaw, ePitch, dist, targetSize, p, error, smoothing);
            }
        }
        if (moving) {
            float[] step = advance(p, error);
            dYaw += step[0];
            dPitch += step[1];
        }

        float amount = Math.max(0f, p.tremor);
        double tt = t * p.tremorSpeed;
        float newTremorYaw = (float) (noise.fractal(tt, 11L) * amount);
        float newTremorPitch = (float) (noise.fractal(tt * 0.85, 53L) * amount * 0.6);
        dYaw += newTremorYaw - tremorYaw;
        dPitch += newTremorPitch - tremorPitch;
        tremorYaw = newTremorYaw;
        tremorPitch = newTremorPitch;

        apply(dYaw, dPitch);
    }

    private void plan(float eYaw, float ePitch, float dist, float targetSize, Profile p, float error, float smoothing) {
        float size = Math.max(targetSize, 0.5f);
        boolean primary = dist > size * 1.5f;

        float amplitude, sideways;
        float overshoot = pendingOvershoot;
        pendingOvershoot = 0f;
        if (primary && overshoot > 0f) {
            float past = overshoot * (size * (1.2f + 0.8f * random.nextFloat()) + dist * (0.1f + 0.1f * random.nextFloat()));
            amplitude = 1f + Math.min(past, 30f) / dist;
            amplitude = Math.min(amplitude, 1.6f);
            sideways = (float) random.nextGaussian() * 0.04f * (0.6f + error);
        } else if (primary) {
            amplitude = 0.93f + (float) random.nextGaussian() * 0.04f;
            if (random.nextFloat() < 0.12f + 0.2f * error) {
                amplitude = 1.04f + (float) Math.abs(random.nextGaussian()) * 0.06f * (1f + error);
            }
            sideways = (float) random.nextGaussian() * 0.025f * (0.6f + error);
        } else {
            amplitude = 1f + (float) random.nextGaussian() * (0.06f + 0.14f * error);
            sideways = (float) random.nextGaussian() * 0.05f * (0.5f + error);
        }

        float ux = eYaw / dist, uy = ePitch / dist;
        moveYaw = (ux * amplitude - uy * sideways) * dist;
        movePitch = (uy * amplitude + ux * sideways) * dist;
        moveCurve = (float) random.nextGaussian() * 0.07f;

        float speed = p.speedMin + random.nextFloat() * Math.max(0f, p.speedMax - p.speedMin);
        float id = (float) (Math.log(dist / size + 1.0) / Math.log(2.0));
        float ms = (70f + 95f * id) * speedMultiplier(speed) * (1f + 0.4f * smoothing) * (0.88f + random.nextFloat() * 0.24f);
        moveTicks = Math.max(1f, ms / 50f);
        moveElapsed = 0f;
        moving = true;
    }

    private float[] advance(Profile p, float error) {
        float t0 = moveElapsed / moveTicks;
        moveElapsed += 1f;
        float t1 = Math.min(1f, moveElapsed / moveTicks);

        float s0 = minimumJerk(t0), s1 = minimumJerk(t1);
        float along = s1 - s0;
        float side = moveCurve * (MathHelper.sin((float) Math.PI * s1) - MathHelper.sin((float) Math.PI * s0));

        if (t1 >= 1f) {
            moving = false;
            waitTicks = correctionLatency(p, error);
        }
        return new float[]{moveYaw * along - movePitch * side, movePitch * along + moveYaw * side};
    }

    private int correctionLatency(Profile p, float error) {
        float ms = p.reactionMs * 0.45f + (float) random.nextGaussian() * 20f;
        int ticks = Math.round(ms / 50f);
        if (random.nextFloat() < error * 0.15f) ticks += 1 + random.nextInt(3);
        return MathHelper.clamp_int(ticks, 0, 10);
    }

    private static float minimumJerk(float t) {
        return t * t * t * (10f + t * (-15f + t * 6f));
    }

    private static float speedMultiplier(float speed) {
        float k = (MathHelper.clamp_float(speed, 1f, 10f) - 1f) / 9f;
        return (float) (2.4 * Math.pow(0.2 / 2.4, k));
    }

    public void stepLinear(float targetYaw, float targetPitch, float degreesPerTick) {
        cancelMotion();
        float dYaw = MathHelper.wrapAngleTo180_float(targetYaw - yaw);
        float dPitch = targetPitch - pitch;
        float mag = MathHelper.sqrt_float(dYaw * dYaw + dPitch * dPitch);
        if (mag > degreesPerTick && mag > 0f) {
            float k = degreesPerTick / mag;
            dYaw *= k;
            dPitch *= k;
        }
        apply(dYaw, dPitch);
    }

    public float distanceTo(float targetYaw, float targetPitch) {
        float dYaw = MathHelper.wrapAngleTo180_float(targetYaw - yaw);
        float dPitch = targetPitch - pitch;
        return MathHelper.sqrt_float(dYaw * dYaw + dPitch * dPitch);
    }

    private void apply(float dYaw, float dPitch) {
        float gcd = gcd();
        float countsYaw = dYaw / gcd + residualYaw;
        int wholeYaw = Math.round(countsYaw);
        residualYaw = countsYaw - wholeYaw;
        yaw += wholeYaw * gcd;

        float countsPitch = dPitch / gcd + residualPitch;
        int wholePitch = Math.round(countsPitch);
        residualPitch = countsPitch - wholePitch;
        float newPitch = pitch + wholePitch * gcd;
        if (newPitch > 90f || newPitch < -90f) {
            newPitch = MathHelper.clamp_float(newPitch, -90f, 90f);
            residualPitch = 0f;
        }
        pitch = newPitch;
    }

    public static float gcd() {
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 1.2F;
        return gcd > 0 ? gcd : 0.0001f;
    }
}

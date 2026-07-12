package greencloud.impl.utils.render;

public class AnimationUtil {

    public static float moveUD(float current, float target, float speed, float deltaTime) {
        return moveUD(current, target, speed, speed, deltaTime);
    }

    public static float moveUD(float current, float target, float speed, float minSpeed, float deltaTime) {
        float diff = target - current;

        if (Math.abs(diff) < 0.001f) {
            return target;
        }

        float actualSpeed = Math.max(minSpeed, Math.abs(diff) * speed);
        float step = actualSpeed * deltaTime;

        if (Math.abs(diff) < step) {
            return target;
        }

        return current + Math.copySign(step, diff);
    }

    public static float lerp(float start, float end, float percent) {
        return start + (end - start) * percent;
    }
}
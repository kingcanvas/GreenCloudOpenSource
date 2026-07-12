package greencloud.impl.utils.animation.animations;

public class EasingAnimation {

    public enum Easing {
        LINEAR,
        EASE_OUT_QUAD,
        EASE_OUT_CUBIC,
        EASE_OUT_QUART,
        EASE_OUT_QUINT,
        EASE_OUT_EXPO,
        EASE_IN_QUAD,
        EASE_IN_CUBIC,
        EASE_IN_OUT_CUBIC,
        EASE_IN_OUT_QUART,
        EASE_SPRING
    }

    private float startValue;
    private float endValue;
    private float currentValue;
    private long startTime;
    private float durationMs;
    private Easing easing;
    private boolean done = true;

    public EasingAnimation(float initial) {
        this.currentValue = initial;
        this.startValue = initial;
        this.endValue = initial;
    }

    public void animateTo(float target, float durationMs, Easing easing) {
        if (Math.abs(target - endValue) < 0.0001f) return;
        this.startValue = currentValue;
        this.endValue = target;
        this.durationMs = durationMs;
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
        this.done = false;
    }

    public void set(float value) {
        this.currentValue = value;
        this.startValue = value;
        this.endValue = value;
        this.done = true;
    }

    public float update() {
        if (done) return currentValue;
        float elapsed = System.currentTimeMillis() - startTime;
        float t = Math.min(elapsed / durationMs, 1f);
        currentValue = startValue + (endValue - startValue) * applyEasing(t);
        if (t >= 1f) {
            currentValue = endValue;
            done = true;
        }
        return currentValue;
    }

    public float getValue() { return currentValue; }
    public float getTarget() { return endValue; }
    public boolean isDone() { return done; }

    private float applyEasing(float t) {
        switch (easing) {
            case LINEAR:          return t;
            case EASE_OUT_QUAD:   return 1f - (1f-t)*(1f-t);
            case EASE_OUT_CUBIC:  return 1f - (1f-t)*(1f-t)*(1f-t);
            case EASE_OUT_QUART:  return 1f - (1f-t)*(1f-t)*(1f-t)*(1f-t);
            case EASE_OUT_QUINT:  return 1f - (1f-t)*(1f-t)*(1f-t)*(1f-t)*(1f-t);
            case EASE_OUT_EXPO:   return t >= 1f ? 1f : 1f - (float)Math.pow(2.0, -10.0 * t);
            case EASE_IN_QUAD:    return t * t;
            case EASE_IN_CUBIC:   return t * t * t;
            case EASE_IN_OUT_CUBIC:
                return t < 0.5f ? 4*t*t*t : 1f - (float)Math.pow(-2*t+2, 3) / 2f;
            case EASE_IN_OUT_QUART:
                return t < 0.5f ? 8*t*t*t*t : 1f - (float)Math.pow(-2*t+2, 4) / 2f;
            case EASE_SPRING: {
                if (t <= 0f) return 0f;
                if (t >= 1f) return 1f;
                float c4 = (float)(2.0 * Math.PI) / 3f;
                return (float)(Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0 - 0.75) * c4) + 1.0);
            }
            default: return t;
        }
    }
}

package greencloud.impl.utils.animation;

import greencloud.impl.utils.animation.animations.EasingAnimation;
import greencloud.impl.utils.animation.animations.EasingAnimation.Easing;

import java.util.HashMap;
import java.util.Map;

public final class AnimationManager {

    private static final Map<String, EasingAnimation> registry = new HashMap<>();

    private AnimationManager() {}

    public static float animate(String key, float target, float durationMs, Easing easing) {
        EasingAnimation anim = registry.computeIfAbsent(key, k -> new EasingAnimation(target));
        anim.animateTo(target, durationMs, easing);
        return anim.update();
    }

    public static EasingAnimation get(String key, float initial) {
        return registry.computeIfAbsent(key, k -> new EasingAnimation(initial));
    }

    public static void set(String key, float value) {
        registry.computeIfAbsent(key, k -> new EasingAnimation(value)).set(value);
    }

    public static void remove(String key) { registry.remove(key); }
    public static void clear()            { registry.clear(); }
}

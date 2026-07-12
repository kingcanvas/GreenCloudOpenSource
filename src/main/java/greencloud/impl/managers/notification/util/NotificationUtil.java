package greencloud.impl.managers.notification.util;

public final class NotificationUtil {

    private NotificationUtil() {}

    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    public static float easeInCubic(float t) {
        return t * t * t;
    }

    public static String[] parseMessageParts(String message) {
        return message.split("(?=[ ])|(?<=[ ])");
    }

    public static boolean shouldHighlight(String text) {
        String clean = text.trim();
        if (clean.isEmpty()) return false;
        return clean.matches(".*\\d.*")
                || clean.equalsIgnoreCase("Enabled")
                || clean.equalsIgnoreCase("Disabled")
                || clean.equalsIgnoreCase("On")
                || clean.equalsIgnoreCase("Off");
    }
}

package greencloud.impl.managers.notification.model;

import greencloud.impl.managers.notification.NotificationManager;

public class Notification {
    public final String title;
    public final String message;
    public final NotificationManager.NotificationType type;
    private final long startTime;
    private final long duration;
    private final long animDuration = 200;
    public volatile float currentY = -1;
    public volatile float velocityY = 0f;

    public Notification(String title, String message, NotificationManager.NotificationType type, int durationMs) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.startTime = System.currentTimeMillis();
        this.duration = durationMs;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > duration + animDuration;
    }

    public float getAnimationProgress() {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed < animDuration) return (float) elapsed / animDuration;
        long timeLeft = duration + animDuration - elapsed;
        return timeLeft < animDuration ? (float) timeLeft / animDuration : 1.0f;
    }

    public float getTimeProgress() {
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, 1.0f - ((float) elapsed / duration));
    }

    public float getSlideInProgress(float slideInMs) {
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1f, elapsed / slideInMs);
    }

    public float getSlideOutRaw() {
        long elapsed = System.currentTimeMillis() - startTime;
        long timeLeft = duration + animDuration - elapsed;
        if (timeLeft >= animDuration) return 0f;
        return 1f - (float) timeLeft / animDuration;
    }
}

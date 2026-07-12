package greencloud.impl.utils.time;

public class TimeUtil {
    private long lastMS;

    public TimeUtil() {
        this.reset();
    }

    public long getCurrentMS() {
        return System.nanoTime() / 1000000L;
    }

    public boolean hasReached(double milliseconds) {
        return (getCurrentMS() - this.lastMS) >= milliseconds;
    }

    public void reset() {
        this.lastMS = getCurrentMS();
    }

    public long getDifference() {
        return getCurrentMS() - this.lastMS;
    }
}
package greencloud.impl.utils;

public class TimerUtil {

    private long lastMS = System.nanoTime() / 1000000L;

    public boolean hasReached(long milliseconds) {
        return (getCurrentMS() - this.lastMS) >= milliseconds;
    }

    public void reset() {
        this.lastMS = getCurrentMS();
    }

    public long getElapsedTime() {
        return getCurrentMS() - this.lastMS;
    }

    public long getCurrentMS() {
        return System.nanoTime() / 1000000L;
    }

    public void subtract(long milliseconds) {
        this.lastMS += milliseconds;
    }
}
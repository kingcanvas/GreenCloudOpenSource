package greencloud.impl.logger;

public final class LogRecord {

    public final long timestamp;
    public final Level level;
    public final String tag;
    public final String message;
    public final Throwable cause;

    LogRecord(Level level, String tag, String message, Throwable cause) {
        this.timestamp = System.currentTimeMillis();
        this.level = level;
        this.tag = tag;
        this.message = message;
        this.cause = cause;
    }
}

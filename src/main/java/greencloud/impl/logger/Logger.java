package greencloud.impl.logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Logger {

    private final String tag;
    private final List<Sink> sinks;
    private Level threshold;

    Logger(String tag, List<Sink> sinks, Level threshold) {
        this.tag = tag;
        this.sinks = new CopyOnWriteArrayList<>(sinks);
        this.threshold = threshold;
    }

    public void debug(String message)                    { emit(Level.DEBUG, message, null); }
    public void debug(String message, Throwable cause)   { emit(Level.DEBUG, message, cause); }
    public void info(String message)                     { emit(Level.INFO,  message, null); }
    public void info(String message, Throwable cause)    { emit(Level.INFO,  message, cause); }
    public void warn(String message)                     { emit(Level.WARN,  message, null); }
    public void warn(String message, Throwable cause)    { emit(Level.WARN,  message, cause); }
    public void error(String message)                    { emit(Level.ERROR, message, null); }
    public void error(String message, Throwable cause)   { emit(Level.ERROR, message, cause); }

    public void setThreshold(Level threshold) {
        this.threshold = threshold;
    }

    public void addSink(Sink sink) {
        sinks.add(sink);
    }

    private void emit(Level level, String message, Throwable cause) {
        if (level.ordinal() < threshold.ordinal()) return;
        LogRecord record = new LogRecord(level, tag, message, cause);
        for (Sink sink : sinks) sink.emit(record);
    }
}

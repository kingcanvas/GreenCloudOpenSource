package greencloud.impl.logger;

import greencloud.impl.logger.sink.ConsoleSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Log {

    private static final Map<String, Logger> loggers = new ConcurrentHashMap<>();
    private static final List<Sink> globalSinks = new ArrayList<>();
    private static Level globalThreshold = Level.DEBUG;

    static {
        globalSinks.add(new ConsoleSink());
    }

    private Log() {}

    public static Logger get(String tag) {
        return loggers.computeIfAbsent(tag, t -> new Logger(t, new ArrayList<>(globalSinks), globalThreshold));
    }

    public static Logger get(Class<?> clazz) {
        return get(clazz.getSimpleName());
    }

    public static void addGlobalSink(Sink sink) {
        globalSinks.add(sink);
        loggers.values().forEach(l -> l.addSink(sink));
    }

    public static void setGlobalThreshold(Level level) {
        globalThreshold = level;
        loggers.values().forEach(l -> l.setThreshold(level));
    }
}

package greencloud.impl.logger;

public interface Sink {
    void emit(LogRecord record);
}

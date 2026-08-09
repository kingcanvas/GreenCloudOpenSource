package greencloudclient.com.logger;

public interface Sink {
    void emit(LogRecord record);
}

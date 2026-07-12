package greencloud.impl.logger.sink;

import greencloud.impl.logger.LogRecord;
import greencloud.impl.logger.Sink;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ConsoleSink implements Sink {

    private static final Logger log4j = LogManager.getLogger("GreenCloud");

    @Override
    public void emit(LogRecord record) {
        String line = "[" + record.tag + "] " + record.message;
        switch (record.level) {
            case DEBUG: log4j.debug(line, record.cause); break;
            case INFO:  log4j.info(line, record.cause);  break;
            case WARN:  log4j.warn(line, record.cause);  break;
            case ERROR: log4j.error(line, record.cause); break;
        }
    }
}

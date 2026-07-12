package greencloud.impl.utils.websocket;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private final String name;
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private Logger(String name) {
        this.name = name;
    }

    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz.getSimpleName());
    }

    public static Logger getLogger(String name) {
        return new Logger(name);
    }

    private String getTimestamp() {
        return timeFormat.format(new Date());
    }

    public void info(String message) {
        System.out.println("[" + getTimestamp() + "] [" + name + "/INFO] " + message);
    }

    public void debug(String message) {
        System.out.println("[" + getTimestamp() + "] [" + name + "/DEBUG] " + message);
    }

    public void warn(String message) {
        System.out.println("[" + getTimestamp() + "] [" + name + "/WARN] " + message);
    }

    public void error(String message) {
        System.err.println("[" + getTimestamp() + "] [" + name + "/ERROR] " + message);
    }

    public void error(String message, Throwable t) {
        System.err.println("[" + getTimestamp() + "] [" + name + "/ERROR] " + message);
        if (t != null) {
            t.printStackTrace();
        }
    }

    public void trace(String message) {
    }

    public boolean isDebugEnabled() {
        return true;
    }

    public boolean isTraceEnabled() {
        return false;
    }

    public boolean isInfoEnabled() {
        return true;
    }

    public boolean isWarnEnabled() {
        return true;
    }

    public boolean isErrorEnabled() {
        return true;
    }
}
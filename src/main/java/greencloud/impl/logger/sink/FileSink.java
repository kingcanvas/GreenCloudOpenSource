package greencloud.impl.logger.sink;

import greencloud.impl.logger.LogRecord;
import greencloud.impl.logger.Sink;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class FileSink implements Sink {

    private static final SimpleDateFormat TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private final BufferedWriter writer;

    public FileSink(File file) throws IOException {
        file.getParentFile().mkdirs();
        this.writer = new BufferedWriter(new FileWriter(file, false));
    }

    public void writeHeader(String header) {
        try {
            writer.write(header);
            writer.newLine();
            writer.flush();
        } catch (IOException ignored) {}
    }

    @Override
    public void emit(LogRecord record) {
        try {
            writer.write(TIMESTAMP.format(new Date(record.timestamp))
                + " [" + record.level + "] "
                + "[" + record.tag + "] "
                + record.message);
            writer.newLine();
            if (record.cause != null) {
                writer.write(stackTraceOf(record.cause));
            }
            writer.flush();
        } catch (IOException ignored) {}
    }

    private static String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("  Exception: " + t.getClass().getName() + ": " + t.getMessage());
        for (StackTraceElement el : t.getStackTrace()) {
            pw.println("    at " + el);
        }
        Throwable cause = t.getCause();
        while (cause != null) {
            pw.println("  Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
            for (StackTraceElement el : cause.getStackTrace()) {
                pw.println("    at " + el);
            }
            cause = cause.getCause();
        }
        return sw.toString();
    }

    public void close() {
        try { writer.close(); } catch (IOException ignored) {}
    }
}

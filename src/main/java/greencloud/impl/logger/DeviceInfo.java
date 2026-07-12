package greencloud.impl.logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class DeviceInfo {

    private DeviceInfo() {}

    public static String build(String clientName, String clientVersion, String buildType) {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();

        long maxHeapMb  = memory.getHeapMemoryUsage().getMax()  / 1024 / 1024;
        long usedHeapMb = memory.getHeapMemoryUsage().getUsed() / 1024 / 1024;

        List<String> jvmArgs = runtime.getInputArguments();

        String sep = new String(new char[72]).replace('\0', '=');

        return sep + "\n"
            + "  " + clientName + " " + clientVersion + " [" + buildType + "]\n"
            + "  Session started: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date()) + "\n"
            + sep + "\n"
            + "  OS            : " + System.getProperty("os.name")    + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")\n"
            + "  User          : " + System.getProperty("user.name")  + "\n"
            + "  Java          : " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")\n"
            + "  JVM           : " + runtime.getVmName() + " " + runtime.getVmVersion() + "\n"
            + "  Heap (max)    : " + maxHeapMb  + " MB\n"
            + "  Heap (used)   : " + usedHeapMb + " MB\n"
            + "  Processors    : " + Runtime.getRuntime().availableProcessors() + "\n"
            + "  JVM uptime    : " + runtime.getUptime() + " ms\n"
            + "  JVM args      : " + (jvmArgs.isEmpty() ? "(none)" : String.join(" ", jvmArgs)) + "\n"
            + "  GPU           : " + glRenderer() + "\n"
            + "  GPU vendor    : " + glVendor() + "\n"
            + "  GL version    : " + glVersion() + "\n"
            + sep;
    }

    private static String glRenderer() {
        try {
            return org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
        } catch (Throwable t) {
            return "unavailable (" + t.getMessage() + ")";
        }
    }

    private static String glVendor() {
        try {
            return org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR);
        } catch (Throwable t) {
            return "unavailable";
        }
    }

    private static String glVersion() {
        try {
            return org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION);
        } catch (Throwable t) {
            return "unavailable";
        }
    }
}

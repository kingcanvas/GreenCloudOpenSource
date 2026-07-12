package greencloud.impl.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class HWIDUtil {

    public static String getHWID() {
        try {
            String toHash = System.getProperty("os.name", "")
                    + System.getProperty("os.arch", "")
                    + System.getProperty("os.version", "")
                    + Runtime.getRuntime().availableProcessors()
                    + System.getProperty("user.name", "")
                    + System.getProperty("java.vendor", "")
                    + getDeviceId();
            return sha256(toHash);
        } catch (Exception e) {
            return "UNKNOWN-HWID";
        }
    }

    private static String getDeviceId() {
        String name = System.getenv("COMPUTERNAME");
        if (name != null && !name.isEmpty()) return name;
        name = System.getenv("HOSTNAME");
        if (name != null && !name.isEmpty()) return name;
        String root = System.getenv("ANDROID_ROOT");
        if (root != null) {
            try {
                Class<?> build = Class.forName("android.os.Build");
                Object serial = build.getField("SERIAL").get(null);
                if (serial != null && !serial.toString().equals("unknown")) return serial.toString();
            } catch (Exception ignored) {}
        }
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "device";
        }
    }

    private static String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

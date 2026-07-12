package greencloud.impl.utils;

public final class AndroidUtil {

    private static final boolean ANDROID;

    static {
        boolean detected = System.getenv("ANDROID_ROOT") != null
                        || System.getenv("ANDROID_DATA") != null;

        if (!detected) {

            try {
                Class.forName("android.os.Build");
                detected = true;
            } catch (ClassNotFoundException ignored) {}
        }

        ANDROID = detected;
    }

    private AndroidUtil() {}

    public static boolean isAndroid() {
        return ANDROID;
    }
}

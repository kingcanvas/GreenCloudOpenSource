package greencloudclient.com.gui.loading;

import java.lang.reflect.Field;

public final class EarlyFullscreenBootstrap {
    private EarlyFullscreenBootstrap() {}

    public static void apply(Object minecraft) {
        try {
            Field settingsField = findField(minecraft.getClass(), "gameSettings", "field_71474_y", "t");
            Object settings = settingsField.get(minecraft);
            if (settings == null) return;
            Field configuredFullscreen = findField(settings.getClass(), "fullScreen", "field_74353_u", "s");
            if (!configuredFullscreen.getBoolean(settings)) return;
            Field startupFullscreen = findField(minecraft.getClass(), "fullscreen", "field_71431_Q", "T");
            startupFullscreen.setBoolean(minecraft, true);
            System.out.println("[GreenCloud] Applied fullscreen preference before Display creation");
        } catch (Throwable throwable) {
            System.err.println("[GreenCloud] Keeping normal fullscreen timing: " + throwable);
        }
    }

    private static Field findField(Class<?> owner, String... names) throws NoSuchFieldException {
        for (String name : names) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(owner.getName());
    }
}

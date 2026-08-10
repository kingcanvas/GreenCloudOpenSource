package greencloudclient.com.modules.impl.utility;

import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

public class NoClickDelay extends Module {

    private Field clickDelayField;
    private boolean initialized = false;

    public NoClickDelay() {
        super("NoClickDelay", Category.UTILITY);
    }

    private void init() {
        for (Field field : mc.getClass().getDeclaredFields()) {
            if (field.getType() == int.class) {
                field.setAccessible(true);
                try {
                    int val = (int) field.get(mc);
                    if (val >= 0 && val <= 10) {
                        clickDelayField = field;
                    }
                } catch (Exception ignored) {}
            }
        }
        initialized = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc == null) return;

        if (!initialized) init();

        if (clickDelayField != null && Mouse.isButtonDown(0)) {
            try {
                clickDelayField.set(mc, 0);
            } catch (Exception ignored) {}
        }
    }
}

package greencloud.impl.managers.movement;

import greencloud.event.TimerEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;

public class TimerManager {

    private final Minecraft mc = Minecraft.getMinecraft();
    private Field timerField;

    public TimerManager() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private Field getTimerField() {
        if (timerField != null) return timerField;
        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (f.getType() == Timer.class) {
                f.setAccessible(true);
                timerField = f;
                return timerField;
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && mc.thePlayer != null && mc.theWorld != null) {
            try {
                Field f = getTimerField();
                if (f == null) return;
                Timer timer = (Timer) f.get(mc);

                TimerEvent timerEvent = new TimerEvent(1.0f);
                MinecraftForge.EVENT_BUS.post(timerEvent);

                timer.timerSpeed = timerEvent.getSpeed();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
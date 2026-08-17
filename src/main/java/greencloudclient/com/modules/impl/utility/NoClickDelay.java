package greencloudclient.com.modules.impl.utility;

import greencloudclient.com.modules.Category;
import greencloudclient.com.modules.Module;
import greencloudclient.mixins.MinecraftAccessor;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class NoClickDelay extends Module {

    public NoClickDelay() {
        super("NoClickDelay", Category.UTILITY);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        ((MinecraftAccessor) mc).greencloud$setLeftClickCounter(0);
    }
}

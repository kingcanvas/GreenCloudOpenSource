package greencloud;

import greencloud.event.MoveEvent;
import greencloud.event.UpdateEvent;
import greencloud.impl.logger.Log;
import greencloud.impl.logger.Logger;
import greencloud.impl.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class GreenCloudMod {

    private static final Logger log = Log.get(GreenCloudMod.class);

    public static GreenCloudMod instance;
    public static final String CLIENT_NAME = "GreenCloud";
    public static final String CLIENT_VERSION = "V2.6";

    public GreenCloudMod() {
        instance = this;
    }

    public void initialize() {
        log.info("Initializing " + CLIENT_NAME + " v" + CLIENT_VERSION + " [" + GreenCloud.ClientInfo.BUILD_TYPE.getName() + "]");
        try {
            MinecraftForge.EVENT_BUS.register(this);
            log.debug("Event bus registered");
        } catch (Exception e) {
            log.error("Failed to register with event bus", e);
        }
    }

    public void postInitialize() {
        log.info(CLIENT_NAME + " post-initialization complete");
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!Keyboard.getEventKeyState()) return;

        int keyCode = Keyboard.getEventKey();
        if (keyCode == Keyboard.KEY_NONE) return;

        try {
            for (Module module : GreenCloud.moduleManager.getModules()) {
                if (module.getKeyCode() == keyCode) {
                    module.toggle();
                }
            }
        } catch (Exception e) {
            log.error("Exception during key bind processing for key " + keyCode, e);
        }
    }

    @SubscribeEvent
    public void onPlayerUpdate(TickEvent.PlayerTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || event.player != mc.thePlayer) return;

        try {
            if (event.phase == TickEvent.Phase.START) {
                MinecraftForge.EVENT_BUS.post(new UpdateEvent.Pre());

                MoveEvent moveEvent = new MoveEvent(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ);
                boolean cancelled = MinecraftForge.EVENT_BUS.post(moveEvent);

                if (cancelled) {
                    mc.thePlayer.motionX = 0;
                    mc.thePlayer.motionY = 0;
                    mc.thePlayer.motionZ = 0;
                } else {
                    mc.thePlayer.motionX = moveEvent.x;
                    mc.thePlayer.motionY = moveEvent.y;
                    mc.thePlayer.motionZ = moveEvent.z;
                }
            } else if (event.phase == TickEvent.Phase.END) {
                MinecraftForge.EVENT_BUS.post(new UpdateEvent.Post());
            }
        } catch (Exception e) {
            log.error("Exception in player tick update", e);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    }

    public String getClientName() {
        return CLIENT_NAME;
    }
}

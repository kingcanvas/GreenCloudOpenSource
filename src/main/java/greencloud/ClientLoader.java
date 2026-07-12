package greencloud;

import greencloud.impl.events.ChatListener;
import greencloud.impl.gui.MainMenuHook;
import greencloud.impl.gui.clickgui.Clickguis.ModernGUI;
import greencloud.impl.gui.clickgui.Clickguis.KingCanvasGUI;
import greencloud.impl.logger.Log;
import greencloud.impl.logger.Logger;
import greencloud.impl.managers.alt.AltManager;
import greencloud.impl.managers.notification.NotificationManager;
import greencloud.impl.managers.player.ViolationsManager;
import greencloud.impl.modules.ModuleManager;
import greencloud.impl.utils.font.FontUtil;
import greencloud.impl.utils.network.NettyInjector;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.Display;

public class ClientLoader {

    private static final Logger log = Log.get(ClientLoader.class);

    public static void init() {
        updateTitle();
    }

    public static void loadModules() {
        log.info("Loading Module Manager");
        GreenCloud.moduleManager = new ModuleManager();
        GreenCloud.moduleManager.init();
    }

    public static void initProcessors() {
        log.info("Initializing processors");
        try {
            NettyInjector.init();
            log.info("Netty packet injector registered");
        } catch (Exception e) {
            log.error("Failed to initialize Netty injector", e);
        }
    }

    public static void initGuis() {
        log.info("Initializing user interface");

        try {
            FontUtil.bootstrap();
            log.info("Custom fonts loaded");
        } catch (Exception e) {
            log.error("Failed to load custom fonts, falling back to Arial", e);
        }

        try {
            MinecraftForge.EVENT_BUS.register(new ChatListener());
            log.debug("ChatListener registered");
        } catch (Exception e) {
            log.error("Failed to register ChatListener", e);
        }

        try {
            GreenCloud.altManager = new AltManager();
            log.debug("AltManager initialized");
        } catch (Exception e) {
            log.error("Failed to initialize AltManager", e);
        }

        try {
            GreenCloud.modernGUI = new ModernGUI();
            GreenCloud.kingCanvasGUI = new KingCanvasGUI();
            log.debug("Click GUIs initialized");
        } catch (Exception e) {
            log.error("Failed to initialize click GUIs", e);
        }

        try {
            new MainMenuHook().register();
            log.debug("MainMenuHook registered");
        } catch (Exception e) {
            log.error("Failed to register MainMenuHook", e);
        }
    }

    public static void finishInit() {
        log.info("Finishing client initialization");

        try {
            NotificationManager.getInstance();
            log.debug("NotificationManager ready");
        } catch (Exception e) {
            log.error("Failed to initialize NotificationManager", e);
        }

        try {
            ViolationsManager.getInstance();
            log.debug("ViolationsManager ready");
        } catch (Exception e) {
            log.error("Failed to initialize ViolationsManager", e);
        }

        try {
            DiscordRP.getInstance().start();
            log.info("Discord RPC started");
        } catch (Throwable e) {
            log.error("Failed to start Discord RPC", e);
        }
    }

    public static void postInit() {
        log.info("GreenCloud fully loaded and ready");
    }

    private static void updateTitle() {
        try {
            Display.setTitle("GreenCloud " + GreenCloud.VERSION + " | [" + GreenCloud.ClientInfo.BUILD_TYPE.getName() + "]");
            log.debug("Window title updated");
        } catch (Exception e) {
            log.warn("Failed to update window title", e);
        }
    }
}

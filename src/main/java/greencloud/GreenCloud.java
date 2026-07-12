package greencloud;

import greencloud.impl.gui.clickgui.Clickguis.ModernGUI;
import greencloud.impl.gui.clickgui.Clickguis.KingCanvasGUI;
import greencloud.impl.logger.DeviceInfo;
import greencloud.impl.logger.Level;
import greencloud.impl.logger.Log;
import greencloud.impl.logger.Logger;
import greencloud.impl.logger.sink.FileSink;
import greencloud.impl.managers.alt.AltManager;
import greencloud.impl.managers.io.SupabaseManager;
import greencloud.impl.managers.movement.MovementManager;
import greencloud.impl.managers.movement.TimerManager;
import greencloud.impl.managers.player.InputManager;
import greencloud.impl.modules.ModuleManager;
import greencloud.impl.command.CommandManager;
import greencloud.impl.irc.IRCManager;
import greencloud.impl.scripting.ScriptManager;
import greencloud.impl.utils.AndroidUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import greencloud.impl.config.ConfigManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("unused")
@Mod(modid = GreenCloud.MODID, version = GreenCloud.VERSION, name = GreenCloud.NAME)
public class GreenCloud {

    public static final String MODID = "GreenCloud";
    public static final String VERSION = "V2.6";
    public static final String NAME = "GreenCloud";

    public static final Logger logger = Log.get("GreenCloud");

    public static GreenCloud instance;
    public static GreenCloudMod clientMod;

    public static ConfigManager configManager;
    public static ModuleManager moduleManager;
    public static ModernGUI modernGUI;
    public static KingCanvasGUI kingCanvasGUI;
    public static DiscordRP discordRP;
    public static CommandManager commandManager;
    public static TimerManager timerManager;
    public static InputManager inputManager;
    public static MovementManager movementManager;
    public static IRCManager ircManager;
    public static AltManager altManager;
    public static ScriptManager scriptManager;

    public static File mainDir;
    public static File skinDir;
    public static File altsDir;

    public static class ClientInfo {
        public static final BuildType BUILD_TYPE = BuildType.RELEASE;

        public enum BuildType {
            RELEASE("Release"),
            BETA("Beta"),
            DEVELOPER("Developer");

            private final String name;

            BuildType(String name) { this.name = name; }

            public String getName() { return name; }
        }
    }

    private boolean clientInitialized = false;

    public GreenCloud() {
        instance = this;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        mainDir = resolveMainDir();
        altsDir = new File(mainDir, "Alts");

        createDirectory(mainDir);
        createDirectory(altsDir);

        if (ClientInfo.BUILD_TYPE != ClientInfo.BuildType.RELEASE) {
            Log.setGlobalThreshold(Level.DEBUG);
        } else {
            Log.setGlobalThreshold(Level.INFO);
        }

        try {
            File logsDir = new File(mainDir, "logs");
            logsDir.mkdirs();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File logFile = new File(logsDir, timestamp + ".log");
            FileSink fileSink = new FileSink(logFile);
            fileSink.writeHeader(DeviceInfo.build(NAME, VERSION, ClientInfo.BUILD_TYPE.getName()));
            Log.addGlobalSink(fileSink);
            logger.info("Logging to: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            logger.warn("Failed to initialize file logging: " + e.getMessage(), e);
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        clientMod = new GreenCloudMod();
        clientMod.initialize();
        initializeClient();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (clientInitialized) {
            ClientLoader.postInit();
            if (clientMod != null) {
                clientMod.postInitialize();
            }
        }
    }

    private void initializeClient() {
        if (clientInitialized) return;

        logger.info("Initializing " + NAME + " " + VERSION + " [" + ClientInfo.BUILD_TYPE.getName() + "]");

        try {
            discordRP = new DiscordRP();
            ClientLoader.init();
        } catch (Throwable e) {
            logger.error("Discord RPC failed to load, feature disabled", e);
            discordRP = null;
        }

        ClientLoader.loadModules();
        configManager = new ConfigManager();
        altManager = new AltManager();
        commandManager = new CommandManager();
        commandManager.init();
        timerManager = new TimerManager();
        inputManager = new InputManager();
        movementManager = new MovementManager();
        ircManager = new IRCManager();
        scriptManager = new ScriptManager(mainDir);
        scriptManager.init();

        SupabaseManager.loadSession();

        ClientLoader.initProcessors();
        ClientLoader.initGuis();
        ClientLoader.finishInit();

        clientInitialized = true;
        logger.info(NAME + " initialization complete");
    }

    private static File resolveMainDir() {
        if (AndroidUtil.isAndroid()) {
            File mcDir = new File(System.getProperty("user.home", "/sdcard/games/PojavLauncher/.minecraft"));
            File parent = mcDir.getParentFile();
            return new File(parent != null ? parent : mcDir, "GreenCloud");
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new File("C:\\GreenCloud");
        } else if (os.contains("mac")) {
            return new File(System.getProperty("user.home"), "Library/Application Support/GreenCloud");
        } else {
            return new File(System.getProperty("user.home"), ".greencloud");
        }
    }

    private void createDirectory(File dir) {
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                logger.error("Failed to create directory: " + dir.getAbsolutePath());
            } else {
                logger.debug("Created directory: " + dir.getAbsolutePath());
            }
        }
    }
}

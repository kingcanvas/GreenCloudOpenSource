package greencloud;

import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.minecraft.client.Minecraft;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DiscordRP {

    private static final DiscordRP instance = new DiscordRP();
    private volatile boolean running = false;
    private long created = 0;
    private ScheduledExecutorService executor;

    private static final String CLIENT_ID = "1444397947394457681";
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static DiscordRP getInstance() {
        return instance;
    }

    public void start() {
        if (running) return;
        this.created = System.currentTimeMillis();

        try {
            DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().build();
            DiscordRPC.discordInitialize(CLIENT_ID, handlers, true);

            running = true;
            executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Discord RPC Updater");
                t.setDaemon(true);
                return t;
            });
            executor.scheduleAtFixedRate(this::updateStatus, 0, 2, TimeUnit.SECONDS);
        } catch (Throwable t) {
            GreenCloud.logger.error("Failed to start Discord RPC! Disabling Discord RPC", t);
        }
    }

    public void stop() {
        if (!running) return;
        running = false;
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        try {
            DiscordRPC.discordShutdown();
        } catch (Throwable ignored) {

        }
    }

    private void updateStatus() {
        try {
            String firstLine;
            String secondLine;

            if (mc.theWorld == null || mc.thePlayer == null) {
                if (mc.currentScreen != null && mc.currentScreen.getClass().getSimpleName().equals("GuiMultiplayer")) {
                    firstLine = "Selecting a server.";
                } else {
                    firstLine = "Main Menu";
                }
                secondLine = "Build [" + GreenCloud.ClientInfo.BUILD_TYPE.getName() + "]";
            } else {
                if (mc.isSingleplayer()) {
                    firstLine = "Playing Singleplayer";
                } else if (mc.getCurrentServerData() != null) {
                    String currentIP = mc.getCurrentServerData().serverIP;

                    if (currentIP.toLowerCase().contains("liquidproxy.net")) currentIP = "liquidproxy";
                    if (currentIP.toLowerCase().contains("hypixel.net")) currentIP = "Hypixel";
                    if (currentIP.toLowerCase().contains("blocksmc.com")) currentIP = "BlocksMC";
                    if (currentIP.toLowerCase().contains("minemen.club")) currentIP = "MineMen Club";
                    if (currentIP.toLowerCase().contains("hycraft.us")) currentIP = "Hycraft";
                    if (currentIP.toLowerCase().contains("hypixelhvh.org")) currentIP = "HypixelHVH";
                    if (currentIP.toLowerCase().contains("mc.themoskau.xyz")) currentIP = "Mospixel";
                    if (currentIP.toLowerCase().contains("hvh.cherry.cat")) currentIP = "Mospixel";
                    if (currentIP.toLowerCase().contains("funniesthvh.uk")) currentIP = "Mospixel";
                    if (currentIP.toLowerCase().contains("pika.host")) currentIP = "Pika Network";
                    if (currentIP.toLowerCase().contains("pika-network.net")) currentIP = "Pika Network";
                    if (currentIP.toLowerCase().contains("jartex.fun")) currentIP = "Jartex Network";
                    if (currentIP.toLowerCase().contains("mineberry.org")) currentIP = "Mineberry";
                    if (currentIP.toLowerCase().contains("twerion.net")) currentIP = "Twerion";
                    if (currentIP.toLowerCase().contains("gamster.org")) currentIP = "Gamster";
                    if (currentIP.toLowerCase().contains("supercraft.es")) currentIP = "SuperCraft";
                    if (currentIP.toLowerCase().contains("arch.mc")) currentIP = "ArchMC";
                    if (currentIP.toLowerCase().contains("mc.arch.lol")) currentIP = "ArchMC";
                    if (currentIP.toLowerCase().contains("beta.arch.lol")) currentIP = "ArchMC Beta";
                    if (currentIP.toLowerCase().contains("beta.arch.mc")) currentIP = "ArchMC Beta";
                    if (currentIP.toLowerCase().contains("beta.archmc.lol")) currentIP = "ArchMC Beta";
                    if (currentIP.toLowerCase().contains("beta.hypixel.net")) currentIP = "Hypixel Beta";
                    if (currentIP.toLowerCase().contains("deathzone.net")) currentIP = "DeathZone";

                    firstLine = "Playing on " + currentIP;
                } else {
                    firstLine = "Playing Multiplayer";
                }

                secondLine = "Build [" + GreenCloud.ClientInfo.BUILD_TYPE.getName() + "]";
            }

            DiscordRichPresence.Builder b = new DiscordRichPresence.Builder(secondLine);
            b.setDetails(firstLine);
            b.setBigImage("large", "GreenCloud");
            b.setStartTimestamps(created);

            DiscordRPC.discordUpdatePresence(b.build());
        } catch (Throwable ignored) {

        }
    }
}

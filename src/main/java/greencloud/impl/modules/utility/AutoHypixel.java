package greencloud.impl.modules.utility;

import greencloud.GreenCloud;
import greencloud.impl.modules.Module;
import greencloud.impl.modules.Category;
import greencloud.impl.settings.BooleanSetting;
import greencloud.impl.settings.ModeSetting;
import greencloud.impl.settings.NumberSetting;
import greencloud.impl.managers.notification.NotificationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.regex.Pattern;

public class AutoHypixel extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final ModeSetting gameMode = new ModeSetting("Game Mode", this, "SkyWars Solo",
            "SkyWars Solo", "SkyWars Teams");
    private final ModeSetting difficulty = new ModeSetting("Difficulty", this, "Normal",
            "Normal", "Insane");
    private final NumberSetting delaySeconds = new NumberSetting("Delay (seconds)", this, 3, 1, 10, 1);
    private final BooleanSetting onDeath = new BooleanSetting("On Death", this, true);
    private final BooleanSetting onWin = new BooleanSetting("On Win", this, true);
    private final BooleanSetting showNotifications = new BooleanSetting("Notifications", this, true);

    private int waitTicks = 0;
    private boolean isWaiting = false;
    private boolean isSkyWarsGame = false;
    private boolean wasAlive = true;
    private int notificationTicks = 0;
    private boolean victoryDetected = false;
    private boolean deathDetected = false;

    private static final Pattern VICTORY_PATTERN = Pattern.compile(
            ".*(VICTORY|Victory|WIN|Win|won the game!).*",
            Pattern.CASE_INSENSITIVE
    );

    public AutoHypixel() {
        super("AutoHypixel", "Joins new skywars gamess", Category.UTILITY);
        this.addSettings(gameMode, difficulty, delaySeconds, onDeath, onWin, showNotifications);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        checkSkyWarsGame();

        if (!isSkyWarsGame) {
            resetState();
            return;
        }

        boolean isAlive = !mc.thePlayer.isDead && mc.thePlayer.getHealth() > 0;

        if (wasAlive && !isAlive) {
            deathDetected = true;
            if (onDeath.enabled && !isWaiting) {
                startWaiting("You Died");
            }
        }

        if (victoryDetected && onWin.enabled && !isWaiting) {
            startWaiting("Victory!");
            victoryDetected = false;
        }

        if (deathDetected && onDeath.enabled && !isWaiting) {
            startWaiting("You Died");
            deathDetected = false;
        }

        wasAlive = isAlive;

        if (isWaiting) {
            handleWaiting();
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isToggled() || mc.thePlayer == null) return;

        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());

        checkVictory(message);

        checkDeath(message);

        checkGameEnd(message);
    }

    private void checkVictory(String message) {
        if (VICTORY_PATTERN.matcher(message).matches()) {
            victoryDetected = true;
            if (showNotifications.enabled) {
                NotificationManager.getInstance().addNotification(
                        "AutoHypixel",
                        "Victory!",
                        NotificationManager.NotificationType.INFO,
                        2000
                );
            }
        }
    }

    private void checkDeath(String message) {
        String lowerMessage = message.toLowerCase();
        String lowerPlayerName = mc.thePlayer.getName().toLowerCase();

        if (!lowerMessage.contains(lowerPlayerName)) return;

        boolean isDeathMessage = false;
        String[] deathPatterns = {
                " was killed by",
                " was slain by",
                " fell into the void",
                " was blown up by",
                " was shot by",
                " was burned to death",
                " was pummeled by",
                " was frozen to death",
                " tried to swim in lava",
                " withered away",
                " starved to death",
                " suffocated in a wall",
                " died"
        };

        for (String pattern : deathPatterns) {
            if (lowerMessage.contains(lowerPlayerName + pattern)) {
                isDeathMessage = true;
                break;
            }
        }

        if (isDeathMessage) {
            deathDetected = true;

            String deathType = "You died";
            if (lowerMessage.contains(lowerPlayerName + " was killed by") ||
                    lowerMessage.contains(lowerPlayerName + " was slain by")) {
                deathType = "Killed by player";
            } else if (lowerMessage.contains(lowerPlayerName + " fell into the void")) {
                deathType = "Fell into void";
            } else if (lowerMessage.contains(lowerPlayerName + " died")) {
                deathType = "Died";
            }

            if (showNotifications.enabled) {
                NotificationManager.getInstance().addNotification(
                        "AutoHypixel",
                        deathType,
                        NotificationManager.NotificationType.INFO,
                        2000
                );
            }
        }
    }

    private void checkGameEnd(String message) {
        String lowerMessage = message.toLowerCase();

        String[] endPatterns = {
                "1st killer",
                "winning team",
                "game ended",
                "winner",
                "top kills",
                "scoreboard",
                "the game has ended",
                "game over",
                "final kills",
                "coins earned",
                "play again"
        };

        for (String pattern : endPatterns) {
            if (lowerMessage.contains(pattern)) {
                victoryDetected = true;
                if (showNotifications.enabled) {
                    NotificationManager.getInstance().addNotification(
                            "AutoHypixel",
                            "Game ended",
                            NotificationManager.NotificationType.INFO,
                            2000
                    );
                }
                break;
            }
        }
    }

    private void checkSkyWarsGame() {
        if (mc.theWorld == null || mc.theWorld.getScoreboard() == null) {
            isSkyWarsGame = false;
            return;
        }

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);

        if (sidebarObjective != null) {
            String objectiveName = StringUtils.stripControlCodes(sidebarObjective.getDisplayName());

            String lowerName = objectiveName.toLowerCase();
            if (lowerName.contains("skywars") ||
                    lowerName.contains("sky wars") ||
                    lowerName.contains("skywar") ||
                    (lowerName.contains("sw") && (lowerName.contains("solo") || lowerName.contains("team")))) {
                isSkyWarsGame = true;
                return;
            }
        }

        isSkyWarsGame = false;
    }

    private void startWaiting(String reason) {
        int delay = (int) delaySeconds.value;
        waitTicks = delay * 20;
        isWaiting = true;
        notificationTicks = 0;

        if (showNotifications.enabled) {
            NotificationManager.getInstance().addNotification(
                    "AutoHypixel",
                    reason + " - Joining new game in " + delay + "s",
                    NotificationManager.NotificationType.INFO,
                    delay * 1000 + 1000
            );
        }
    }

    private void handleWaiting() {
        if (waitTicks > 0) {
            waitTicks--;

            if (waitTicks % 20 == 0 && showNotifications.enabled) {
                int secondsLeft = waitTicks / 20;
                if (secondsLeft > 0) {
                    if (notificationTicks != secondsLeft) {
                        notificationTicks = secondsLeft;
                        NotificationManager.getInstance().addNotification(
                                "AutoHypixel",
                                "Joining new game in " + secondsLeft + "s",
                                NotificationManager.NotificationType.INFO,
                                1100
                        );
                    }
                }
            }

            if (waitTicks <= 0) {
                sendJoinCommand();
                resetState();
            }
        }
    }

    private void sendJoinCommand() {
        if (mc.thePlayer == null) return;

        String command = buildCommand();
        if (command != null) {
            mc.thePlayer.sendChatMessage(command);

            if (showNotifications.enabled) {
                NotificationManager.getInstance().addNotification(
                        "AutoHypixel",
                        "Joining New Game",
                        NotificationManager.NotificationType.SUCCESS,
                        2000
                );
            }
        }
    }

    private String buildCommand() {
        String baseCommand = "/play ";
        String mode = "";

        if (gameMode.is("SkyWars Solo")) {
            mode = "solo";
        } else if (gameMode.is("SkyWars Teams")) {
            mode = "teams";
        }

        if (difficulty.is("Normal")) {
            mode += "_normal";
        } else if (difficulty.is("Insane")) {
            mode += "_insane";
        }

        return baseCommand + mode;
    }

    private void resetState() {
        waitTicks = 0;
        isWaiting = false;
        isSkyWarsGame = false;
        wasAlive = true;
        notificationTicks = 0;
        victoryDetected = false;
        deathDetected = false;
    }

    public String getStatus() {
        if (!isToggled()) return "Disabled";
        if (!isSkyWarsGame) return "Waiting for SkyWars";
        if (isWaiting) {
            int secondsLeft = (waitTicks + 19) / 20;
            return "Joining in " + secondsLeft + "s";
        }
        return "Active";
    }

    public boolean isActive() {
        return isToggled() && isSkyWarsGame;
    }

    public boolean isInCountdown() {
        return isWaiting;
    }

    public int getCountdownSeconds() {
        return (waitTicks + 19) / 20;
    }
}
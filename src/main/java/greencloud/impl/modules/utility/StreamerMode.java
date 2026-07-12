package greencloud.impl.modules.utility;

import greencloud.impl.modules.Category;
import greencloud.impl.modules.Module;
import greencloud.impl.settings.BooleanSetting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamerMode extends Module {

    public BooleanSetting hideSelf = new BooleanSetting("Hide Own Name", this, true);
    public BooleanSetting hideOthers = new BooleanSetting("Hide Others", this, true);
    public BooleanSetting scramble = new BooleanSetting("Scramble Names", this, false);
    public BooleanSetting hideServerId = new BooleanSetting("Hide Server ID", this, true);

    private final Map<String, String> idCache = new ConcurrentHashMap<>();
    private final Map<String, IChatComponent> tabCache = new ConcurrentHashMap<>();
    private final Map<String, String> chatCache = new ConcurrentHashMap<>();

    private final Pattern DATE_PATTERN = Pattern.compile("(\\d{2}/\\d{2}/\\d{2})");
    private final Map<String, String> originalPrefixes = new HashMap<>();
    private final Map<String, String> originalSuffixes = new HashMap<>();

    private boolean tabNeedsUpdate = true;
    private Set<String> trackedPlayers = new HashSet<>();
    private int ticks = 0;

    private String selfName = null;
    private String selfReplacement = null;

    public StreamerMode() {
        super("StreamerMode", "Hides player names and server IDs.", Category.UTILITY);
        addSettings(hideSelf, hideOthers, scramble, hideServerId);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        clearAll();
        tabNeedsUpdate = true;
    }

    @Override
    public void onDisable() {
        if (mc.getNetHandler() != null) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                info.setDisplayName(null);
            }
        }

        if (mc.theWorld != null) {
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            for (String teamName : originalPrefixes.keySet()) {
                ScorePlayerTeam team = scoreboard.getTeam(teamName);
                if (team != null) {
                    team.setNamePrefix(originalPrefixes.get(teamName));
                    team.setNameSuffix(originalSuffixes.get(teamName));
                }
            }
        }

        clearAll();
        super.onDisable();
    }

    @SubscribeEvent
    public void onNameFormat(PlayerEvent.NameFormat event) {
        if (mc.thePlayer == null || event.entityPlayer == null) return;

        String name = event.entityPlayer.getName();

        if (name.equals(mc.thePlayer.getName())) {
            if (hideSelf.enabled) {
                event.displayname = EnumChatFormatting.LIGHT_PURPLE + "You" + EnumChatFormatting.RESET;
            }
        } else if (hideOthers.enabled) {
            event.displayname = getNameTag(name, event.username);
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (mc.thePlayer == null) return;

        String unformatted = event.message.getUnformattedText();
        String msg = event.message.getFormattedText();

        if (hideServerId.enabled) {
            String cleanMsg = StringUtils.stripControlCodes(unformatted);
            if (cleanMsg.startsWith("Sending you to ")) {
                String serverID = cleanMsg.replace("Sending you to ", "").trim();
                if (!serverID.isEmpty()) {
                    StringBuilder hidden = new StringBuilder();
                    for (int i = 0; i < serverID.length(); i++) {
                        hidden.append("A");
                    }
                    event.message = new ChatComponentText(
                            EnumChatFormatting.GREEN + "Sending you to " +
                                    EnumChatFormatting.OBFUSCATED + hidden.toString()
                    );
                    return;
                }
            }
        }

        if (!hideSelf.enabled && !hideOthers.enabled) return;
        if (msg.isEmpty()) return;

        boolean changed = false;

        if (hideSelf.enabled) {
            String myName = getSelf();
            if (msg.contains(myName)) {
                msg = msg.replace(myName, getSelfChat());
                changed = true;
            }
        }

        if (hideOthers.enabled) {
            Collection<NetworkPlayerInfo> players = mc.getNetHandler().getPlayerInfoMap();

            for (NetworkPlayerInfo info : players) {
                if (info.getGameProfile() == null) continue;
                String name = info.getGameProfile().getName();

                if (name.equals(mc.thePlayer.getName())) continue;
                if (!msg.contains(name)) continue;

                msg = msg.replace(name, getChat(name));
                changed = true;
            }
        }

        if (changed) {
            event.message = new ChatComponentText(msg);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        if (hideServerId.enabled) {
            updateScoreboard();
        }

        if (++ticks % 10 != 0 && !tabNeedsUpdate) return;

        Collection<NetworkPlayerInfo> players = mc.getNetHandler().getPlayerInfoMap();
        Set<String> current = new HashSet<>();

        for (NetworkPlayerInfo info : players) {
            if (info.getGameProfile() == null) continue;
            current.add(info.getGameProfile().getName());
        }

        if (!current.equals(trackedPlayers) || tabNeedsUpdate) {
            updateTab(players);
            trackedPlayers = current;
            tabNeedsUpdate = false;
        }
    }

    private void updateScoreboard() {
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        for (ScorePlayerTeam team : scoreboard.getTeams()) {
            String teamName = team.getRegisteredName();
            String prefix = team.getColorPrefix();
            String suffix = team.getColorSuffix();
            String cleanPrefix = StringUtils.stripControlCodes(prefix);
            String cleanSuffix = StringUtils.stripControlCodes(suffix);
            String cleanFull = cleanPrefix + cleanSuffix;

            Matcher matcher = DATE_PATTERN.matcher(cleanFull);
            if (matcher.find()) {
                String dateString = matcher.group(1);
                if (suffix.startsWith(EnumChatFormatting.OBFUSCATED.toString())) {
                    continue;
                }

                if (!originalPrefixes.containsKey(teamName)) {
                    originalPrefixes.put(teamName, prefix);
                    originalSuffixes.put(teamName, suffix);
                }

                int dateEndIndex = cleanFull.indexOf(dateString) + dateString.length();
                String textAfterDate = "";
                if (dateEndIndex < cleanFull.length()) {
                    textAfterDate = cleanFull.substring(dateEndIndex).trim();
                }

                if (!textAfterDate.isEmpty()) {
                    StringBuilder hidden = new StringBuilder();
                    for (int i = 0; i < textAfterDate.length(); i++) {
                        hidden.append("A");
                    }

                    String rawDate = matcher.group(1);
                    int dateIndexInPrefix = prefix.indexOf(rawDate);

                    if (dateIndexInPrefix != -1) {
                        String newPrefix = prefix.substring(0, dateIndexInPrefix + rawDate.length());
                        if (!newPrefix.endsWith(" ")) newPrefix += " ";
                        team.setNamePrefix(newPrefix);
                    }
                    team.setNameSuffix(EnumChatFormatting.OBFUSCATED + hidden.toString());
                }
            }
        }
    }

    private void updateTab(Collection<NetworkPlayerInfo> players) {
        for (NetworkPlayerInfo info : players) {
            if (info.getGameProfile() == null) continue;

            String name = info.getGameProfile().getName();
            IChatComponent display = getTab(name);

            if (display != null) {
                info.setDisplayName(display);
            }
        }
    }

    private String getSelf() {
        if (selfName == null || !selfName.equals(mc.thePlayer.getName())) {
            selfName = mc.thePlayer.getName();
            selfReplacement = null;
        }
        return selfName;
    }

    private String getSelfChat() {
        if (selfReplacement == null) {
            selfReplacement = EnumChatFormatting.LIGHT_PURPLE + "You" + EnumChatFormatting.RESET;
        }
        return selfReplacement;
    }

    private String getId(String name) {
        return idCache.computeIfAbsent(name, n ->
                String.valueOf(Math.abs(n.hashCode()) % 1000)
        );
    }

    private String getChat(String name) {
        String key = name + "_" + scramble.enabled;
        return chatCache.computeIfAbsent(key, k -> {
            if (scramble.enabled) {
                return EnumChatFormatting.OBFUSCATED + "XXXXX" + EnumChatFormatting.RESET;
            } else {
                return EnumChatFormatting.GRAY + "Player " + getId(name) + EnumChatFormatting.RESET;
            }
        });
    }

    private String getNameTag(String name, String username) {
        if (scramble.enabled) {
            return EnumChatFormatting.OBFUSCATED + username;
        } else {
            return EnumChatFormatting.GRAY + "Player " + getId(name);
        }
    }

    private IChatComponent getTab(String name) {
        String key = name + "_" + scramble.enabled + "_" + hideSelf.enabled + "_" + hideOthers.enabled;

        return tabCache.computeIfAbsent(key, k -> {
            if (name.equals(mc.thePlayer.getName())) {
                if (hideSelf.enabled) {
                    return new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "You");
                }
            } else if (hideOthers.enabled) {
                if (scramble.enabled) {
                    return new ChatComponentText(EnumChatFormatting.OBFUSCATED + name);
                } else {
                    return new ChatComponentText(EnumChatFormatting.GRAY + "Player " + getId(name));
                }
            }
            return null;
        });
    }

    private void clearAll() {
        idCache.clear();
        tabCache.clear();
        chatCache.clear();
        originalPrefixes.clear();
        originalSuffixes.clear();
        selfName = null;
        selfReplacement = null;
        trackedPlayers.clear();
    }
}
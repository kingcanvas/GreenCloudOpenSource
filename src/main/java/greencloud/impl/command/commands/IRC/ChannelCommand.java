package greencloud.impl.command.commands.IRC;

import greencloud.GreenCloud;
import greencloud.impl.command.Command;
import greencloud.impl.command.CommandManager;
import greencloud.impl.irc.IRCManager;

import java.util.Arrays;
import java.util.List;

public class ChannelCommand extends Command {

    @Override
    public String getName() {
        return "users";
    }

    @Override
    public String getDescription() {
        return "View online IRC users";
    }

    @Override
    public String getUsage() {
        return ".users [refresh]";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("list", "online", "who");
    }

    @Override
    public void execute(String[] args) {
        if (GreenCloud.ircManager == null || !GreenCloud.ircManager.isConnected()) {
            CommandManager.sendMessage("§cNot connected to IRC!");
            return;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("refresh")) {
            GreenCloud.ircManager.requestClientsList();
            CommandManager.sendMessage("§aRefreshing user list...");
            return;
        }

        List<IRCManager.ConnectedUser> users = GreenCloud.ircManager.getOnlineUsers();
        int userCount = users.size();

        if (userCount == 0) {
            CommandManager.sendMessage("§7No users online");
            return;
        }

        CommandManager.sendMessage("§a§lOnline Users §7(" + userCount + "):");
        for (IRCManager.ConnectedUser user : users) {
            String guestTag = user.isGuest() ? " §7(Guest)" : "";
            String spotifyTag = user.isSpotifyEnabled() ? " §a♫" : "";
            CommandManager.sendMessage("§e" + user.getUsername() + guestTag + spotifyTag);
        }

        CommandManager.sendMessage("§7Tip: Use §e.users refresh §7to update the list");
    }
}
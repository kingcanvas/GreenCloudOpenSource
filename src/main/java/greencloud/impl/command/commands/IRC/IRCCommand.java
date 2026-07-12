package greencloud.impl.command.commands.IRC;

import greencloud.GreenCloud;
import greencloud.impl.command.Command;
import greencloud.impl.command.CommandManager;

import java.util.Arrays;
import java.util.List;

public class IRCCommand extends Command {

    @Override
    public String getName() {
        return "irc";
    }

    @Override
    public String getDescription() {
        return "Send a message to the IRC";
    }

    @Override
    public String getUsage() {
        return ".irc <message>";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("chat", "msg");
    }

    @Override
    public void execute(String[] args) {
        if (GreenCloud.ircManager == null) {
            CommandManager.sendMessage("§cIRC Manager not initialized!");
            return;
        }

        if (!GreenCloud.ircManager.isConnected()) {
            CommandManager.sendMessage("§cNot connected to IRC! Type §e.connect §cto join as guest.");
            return;
        }

        if (args.length < 2) {
            CommandManager.sendMessage("§cUsage: " + getUsage());
            return;
        }

        StringBuilder message = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            message.append(args[i]);
            if (i < args.length - 1) {
                message.append(" ");
            }
        }

        GreenCloud.ircManager.sendChatMessage(message.toString());

        String prefix = GreenCloud.ircManager.isGuest() ? "§7[Guest] " : "";
        CommandManager.sendMessage(prefix + "§aMessage sent to IRC");
    }
}
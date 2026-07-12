package greencloud.impl.command.commands.IRC;

import greencloud.GreenCloud;
import greencloud.impl.command.Command;
import greencloud.impl.command.CommandManager;

import java.util.Arrays;
import java.util.List;

public class DisconnectCommand extends Command {

    @Override
    public String getName() {
        return "disconnect";
    }

    @Override
    public String getDescription() {
        return "Disconnect from IRC";
    }

    @Override
    public String getUsage() {
        return ".disconnect";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("dc", "leave");
    }

    @Override
    public void execute(String[] args) {
        if (GreenCloud.ircManager == null) {
            CommandManager.sendMessage("§cIRC Manager not initialized!");
            return;
        }

        if (!GreenCloud.ircManager.isConnected()) {
            CommandManager.sendMessage("§cNot connected to IRC!");
            return;
        }

        String username = GreenCloud.ircManager.getUsername();
        GreenCloud.ircManager.disconnect();
        CommandManager.sendMessage("§cDisconnected from IRC (was: §e" + username + "§c)");
    }
}
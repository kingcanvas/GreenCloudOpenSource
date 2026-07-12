package greencloud.impl.command.commands.IRC;

import greencloud.GreenCloud;
import greencloud.impl.command.Command;
import greencloud.impl.command.CommandManager;

import java.util.Arrays;
import java.util.List;

public class HelpCommand extends Command {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Shows all available commands";
    }

    @Override
    public String getUsage() {
        return ".help";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("h", "commands");
    }

    @Override
    public void execute(String[] args) {
        CommandManager.sendMessage("§a§lAvailable Commands:");

        for (Command cmd : GreenCloud.commandManager.getCommands()) {
            CommandManager.sendMessage("§e" + GreenCloud.commandManager.getPrefix() + cmd.getName() +
                    " §7- §f" + cmd.getDescription());
        }

        CommandManager.sendMessage("§7Use " + GreenCloud.commandManager.getPrefix() + "<command> for more info");
    }
}
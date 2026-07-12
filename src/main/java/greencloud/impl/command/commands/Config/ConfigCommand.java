package greencloud.impl.command.commands.Config;

import greencloud.GreenCloud;
import greencloud.impl.command.Command;
import greencloud.impl.command.CommandManager;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;

public class ConfigCommand extends Command {

    @Override
    public String getName() {
        return "config";
    }

    @Override
    public String getDescription() {
        return "Manages client configurations (save, load, delete, list)";
    }

    @Override
    public String getUsage() {
        return "config <save/load/delete/list> <name>";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("c", "cfg", "configs");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            CommandManager.sendMessage(EnumChatFormatting.RED + "Usage: " + getUsage());
            return;
        }

        String action = args[1].toLowerCase();

        if (action.equals("list")) {
            CommandManager.sendMessage(EnumChatFormatting.GREEN + "Available Configs:");
            for (String cfg : GreenCloud.configManager.getConfigList()) {
                CommandManager.sendMessage(EnumChatFormatting.GRAY + "- " + EnumChatFormatting.WHITE + cfg);
            }
            return;
        }

        if (args.length < 3) {
            CommandManager.sendMessage(EnumChatFormatting.RED + "Please specify a config name.");
            return;
        }

        String configName = args[2];

        switch (action) {
            case "save":
            case "s":
                GreenCloud.configManager.saveConfig(configName);
                CommandManager.sendMessage(EnumChatFormatting.GREEN + "Saved config: " + EnumChatFormatting.WHITE + configName);
                break;

            case "load":
            case "l":
                if (configExists(configName)) {
                    GreenCloud.configManager.loadConfig(configName);
                    CommandManager.sendMessage(EnumChatFormatting.GREEN + "Loaded config: " + EnumChatFormatting.WHITE + configName);
                } else {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "Config '" + configName + "' not found.");
                }
                break;

            case "delete":
            case "d":
            case "del":
                if (configExists(configName)) {
                    GreenCloud.configManager.deleteConfig(configName);
                    CommandManager.sendMessage(EnumChatFormatting.GREEN + "Deleted config: " + EnumChatFormatting.WHITE + configName);
                } else {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "Config '" + configName + "' not found.");
                }
                break;

            default:
                CommandManager.sendMessage(EnumChatFormatting.RED + "Unknown action. Usage: " + getUsage());
                break;
        }
    }

    private boolean configExists(String name) {
        for (String cfg : GreenCloud.configManager.getConfigList()) {
            if (cfg.equalsIgnoreCase(name)) return true;
        }
        return false;
    }
}
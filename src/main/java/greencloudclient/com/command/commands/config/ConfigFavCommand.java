package greencloudclient.com.command.commands.config;

import greencloudclient.com.GreenCloud;
import greencloudclient.com.command.Command;
import greencloudclient.com.command.CommandManager;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;

public class ConfigFavCommand extends Command {

    @Override
    public String getName() {
        return "configfav";
    }

    @Override
    public String getDescription() {
        return "Sets the config loaded when the client starts";
    }

    @Override
    public String getUsage() {
        return "configfav <name>";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("cfgfav", "cfav");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            CommandManager.sendMessage(EnumChatFormatting.RED + "Usage: " + getUsage());
            return;
        }

        String requestedName = args[1];
        for (String configName : GreenCloud.configManager.getConfigList()) {
            if (configName.equalsIgnoreCase(requestedName)) {
                if (GreenCloud.configManager.setFavoriteConfig(configName)) {
                    CommandManager.sendMessage(EnumChatFormatting.GREEN + "Favorite config set: " + EnumChatFormatting.WHITE + configName);
                } else {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "Failed to set favorite config.");
                }
                return;
            }
        }

        CommandManager.sendMessage(EnumChatFormatting.RED + "Config '" + requestedName + "' not found.");
    }
}

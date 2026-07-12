package greencloud.impl.command;

import greencloud.GreenCloud;
import greencloud.impl.command.commands.Bind.BindCommand;
import greencloud.impl.command.commands.Config.ConfigCommand;
import greencloud.impl.command.commands.IRC.*;
import greencloud.impl.command.commands.Script.ScriptCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {

    private final List<Command> commands = new ArrayList<>();
    private final String prefix = ".";

    public void init() {
        commands.add(new ConnectCommand());
        commands.add(new DisconnectCommand());
        commands.add(new IRCCommand());
        commands.add(new HelpCommand());
        commands.add(new ChannelCommand());
        commands.add(new BindCommand());


        commands.add(new ConfigCommand());
        commands.add(new ScriptCommand());

        GreenCloud.logger.info("Command Manager initialized with " + commands.size() + " commands");
    }

    public boolean handleCommand(String message) {
        if (!message.startsWith(prefix)) {
            return false;
        }


        String[] args = message.substring(prefix.length()).split(" ");

        if (args.length == 0) return false;

        String commandName = args[0].toLowerCase();

        for (Command command : commands) {
            if (command.getName().equalsIgnoreCase(commandName) || command.getAliases().contains(commandName)) {
                try {
                    command.execute(args);
                    return true;
                } catch (Exception e) {
                    sendMessage("Error executing command: " + e.getMessage());
                    e.printStackTrace();
                    return true;
                }
            }
        }

        sendMessage("Unknown command. Type " + prefix + "help for a list of commands.");
        return true;
    }

    public static void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§7[§aGreenCloud§7] §f" + message));
        }
    }

    public List<Command> getCommands() {
        return commands;
    }

    public String getPrefix() {
        return prefix;
    }
}
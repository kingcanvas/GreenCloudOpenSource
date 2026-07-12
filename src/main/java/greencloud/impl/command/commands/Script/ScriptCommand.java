package greencloud.impl.command.commands.Script;

import greencloud.GreenCloud;
import greencloud.impl.command.Command;
import greencloud.impl.command.CommandManager;
import greencloud.impl.scripting.script.ScriptContext;
import greencloud.impl.scripting.script.ScriptState;
import net.minecraft.util.EnumChatFormatting;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ScriptCommand extends Command {

    @Override
    public String getName() { return "script"; }

    @Override
    public String getDescription() { return "Manages Lua scripts (list, load, unload, reload, enable, disable, info)"; }

    @Override
    public String getUsage() { return "script <list|load|unload|reload|enable|disable|info> [name]"; }

    @Override
    public List<String> getAliases() { return Arrays.asList("scripts", "sc", "lua"); }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            CommandManager.sendMessage(EnumChatFormatting.RED + "Usage: " + getUsage());
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "list":
            case "l": {
                Collection<ScriptContext> all = GreenCloud.scriptManager.getAll();
                if (all.isEmpty()) {
                    CommandManager.sendMessage("No scripts loaded.");
                    return;
                }
                for (ScriptContext ctx : all) {
                    String dot = ctx.getState() == ScriptState.RUNNING
                            ? EnumChatFormatting.GREEN + "● "
                            : EnumChatFormatting.RED + "● ";
                    CommandManager.sendMessage(dot + EnumChatFormatting.WHITE + ctx.getScript().getName()
                            + EnumChatFormatting.GRAY + " by " + ctx.getScript().getAuthor());
                }
                break;
            }

            case "load": {
                if (args.length < 3) {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "Usage: .script load <filename.lua>");
                    return;
                }
                String fileName = args[2];
                if (!fileName.endsWith(".lua")) fileName += ".lua";
                File file = new File(GreenCloud.scriptManager.getScriptsDir(), fileName);
                if (!file.exists()) {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "File not found: " + fileName);
                    return;
                }
                GreenCloud.scriptManager.loadScript(file);
                CommandManager.sendMessage(EnumChatFormatting.GREEN + "Loaded: " + EnumChatFormatting.WHITE + fileName);
                break;
            }

            case "unload": {
                if (args.length < 3) {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "Usage: .script unload <name>");
                    return;
                }
                String name = args[2];
                if (GreenCloud.scriptManager.get(name) == null) {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "No script named '" + name + "'.");
                    return;
                }
                GreenCloud.scriptManager.unloadScript(name);
                CommandManager.sendMessage(EnumChatFormatting.GREEN + "Unloaded: " + EnumChatFormatting.WHITE + name);
                break;
            }

            case "reload":
            case "r": {
                if (args.length >= 3) {
                    String name = args[2];
                    ScriptContext ctx = GreenCloud.scriptManager.get(name);
                    if (ctx == null) {
                        CommandManager.sendMessage(EnumChatFormatting.RED + "No script named '" + name + "'.");
                        return;
                    }
                    File file = ctx.getScript().getFile();
                    GreenCloud.scriptManager.unloadScript(name);
                    GreenCloud.scriptManager.loadScript(file);
                    CommandManager.sendMessage(EnumChatFormatting.GREEN + "Reloaded: " + EnumChatFormatting.WHITE + name);
                } else {
                    GreenCloud.scriptManager.reload();
                    CommandManager.sendMessage(EnumChatFormatting.GREEN + "All scripts reloaded.");
                }
                break;
            }

            case "enable":
            case "e": {
                if (args.length < 3) {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "Usage: .script enable <name>");
                    return;
                }
                String name = args[2];
                ScriptContext ctx = GreenCloud.scriptManager.get(name);
                if (ctx == null) {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "No script named '" + name + "'.");
                    return;
                }
                if (ctx.getState() == ScriptState.RUNNING) {
                    CommandManager.sendMessage(EnumChatFormatting.YELLOW + "'" + name + "' is already running.");
                    return;
                }
                ctx.setState(ScriptState.RUNNING);
                ctx.callHook("onEnable");
                CommandManager.sendMessage(EnumChatFormatting.GREEN + "Enabled: " + EnumChatFormatting.WHITE + name);
                break;
            }

            case "disable":
            case "d": {
                if (args.length < 3) {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "Usage: .script disable <name>");
                    return;
                }
                String name = args[2];
                ScriptContext ctx = GreenCloud.scriptManager.get(name);
                if (ctx == null) {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "No script named '" + name + "'.");
                    return;
                }
                if (ctx.getState() == ScriptState.DISABLED) {
                    CommandManager.sendMessage(EnumChatFormatting.YELLOW + "'" + name + "' is already disabled.");
                    return;
                }
                ctx.callHookForced("onDisable");
                ctx.setState(ScriptState.DISABLED);
                CommandManager.sendMessage(EnumChatFormatting.GREEN + "Disabled: " + EnumChatFormatting.WHITE + name);
                break;
            }

            case "info":
            case "i": {
                if (args.length < 3) {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "Usage: .script info <name>");
                    return;
                }
                String name = args[2];
                ScriptContext ctx = GreenCloud.scriptManager.get(name);
                if (ctx == null) {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "No script named '" + name + "'.");
                    return;
                }
                CommandManager.sendMessage(EnumChatFormatting.GREEN + "--- " + ctx.getScript().getName() + " ---");
                CommandManager.sendMessage(EnumChatFormatting.GRAY + "Author:  " + EnumChatFormatting.WHITE + ctx.getScript().getAuthor());
                CommandManager.sendMessage(EnumChatFormatting.GRAY + "Version: " + EnumChatFormatting.WHITE + ctx.getScript().getVersion());
                CommandManager.sendMessage(EnumChatFormatting.GRAY + "State:   " + stateColor(ctx.getState()) + ctx.getState().name());
                CommandManager.sendMessage(EnumChatFormatting.GRAY + "File:    " + EnumChatFormatting.WHITE + ctx.getScript().getFile().getName());
                if (ctx.getState() == ScriptState.ERRORED) {
                    CommandManager.sendMessage(EnumChatFormatting.RED + "Error:   " + ctx.getErrorMessage());
                }
                break;
            }

            default:
                CommandManager.sendMessage(EnumChatFormatting.RED + "Unknown action. Usage: " + getUsage());
                break;
        }
    }

    private static String stateColor(ScriptState state) {
        switch (state) {
            case RUNNING: return EnumChatFormatting.GREEN.toString();
            case ERRORED: return EnumChatFormatting.RED.toString();
            case DISABLED: return EnumChatFormatting.GRAY.toString();
            default: return EnumChatFormatting.YELLOW.toString();
        }
    }
}

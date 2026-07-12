package greencloud.impl.command.commands.Bind;

import greencloud.GreenCloud;
import greencloud.impl.command.Command;
import greencloud.impl.command.CommandManager;
import greencloud.impl.modules.Module;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;

public class BindCommand extends Command {

    @Override
    public String getName() {
        return "bind";
    }

    @Override
    public String getDescription() {
        return "Binds a module to a key";
    }

    @Override
    public String getUsage() {
        return "bind <module> <key> | bind clear";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("b", "keybind");
    }

    @Override
    public void execute(String[] args) {

        if (args.length < 2) {
            CommandManager.sendMessage(EnumChatFormatting.RED + "Usage: " + getUsage());
            return;
        }

        if (args[1].equalsIgnoreCase("clear")) {
            for (Module m : GreenCloud.moduleManager.getModules()) {
                m.setKeyCode(Keyboard.KEY_NONE);
            }
            CommandManager.sendMessage(EnumChatFormatting.GREEN + "All binds cleared.");
            return;
        }

        String moduleName = args[1];
        Module module = GreenCloud.moduleManager.getModuleByName(moduleName);

        if (module == null) {
            CommandManager.sendMessage(EnumChatFormatting.RED + "Module '" + moduleName + "' not found.");
            return;
        }

        if (args.length < 3) {
            CommandManager.sendMessage(EnumChatFormatting.RED + "Please specify a key.");
            return;
        }

        String keyName = args[2].toUpperCase();
        int key = Keyboard.getKeyIndex(keyName);

        module.setKeyCode(key);
        CommandManager.sendMessage(EnumChatFormatting.GREEN + "Bound " + module.getName() + " to " + Keyboard.getKeyName(key));
    }
}
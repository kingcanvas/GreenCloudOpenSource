package greencloud.impl.command.commands.IRC;

import greencloud.GreenCloud;
import greencloud.impl.command.Command;
import greencloud.impl.command.CommandManager;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConnectCommand extends Command {

    @Override
    public String getName() {
        return "connect";
    }

    @Override
    public String getDescription() {
        return "Connect to IRC as a guest";
    }

    @Override
    public String getUsage() {
        return ".connect";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("join", "ircconnect");
    }

    @Override
    public void execute(String[] args) {
        if (GreenCloud.ircManager == null) {
            CommandManager.sendMessage("§cIRC Manager not initialized!");
            return;
        }

        if (GreenCloud.ircManager.isConnected()) {
            CommandManager.sendMessage("§eAlready connected to IRC as §a" + GreenCloud.ircManager.getUsername());
            return;
        }

        CommandManager.sendMessage("§7Connecting to IRC as guest...");
        GreenCloud.ircManager.connectAsGuest();

        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "IRC Connect Checker");
            t.setDaemon(true);
            return t;
        }).execute(() -> {
            try {
                Thread.sleep(2000);
                if (GreenCloud.ircManager.isConnected()) {
                    CommandManager.sendMessage("§aConnected to IRC as §e" + GreenCloud.ircManager.getUsername());
                    CommandManager.sendMessage("§7Use §e.irc <message> §7to chat!");
                } else {
                    CommandManager.sendMessage("§cFailed to connect to IRC");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}

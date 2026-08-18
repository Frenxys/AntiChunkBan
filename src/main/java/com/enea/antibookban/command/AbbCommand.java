package com.enea.antibookban.command;

import com.enea.antibookban.AntiBookBan;
import com.enea.antibookban.config.PluginConfig;
import com.enea.antibookban.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class AbbCommand implements CommandExecutor {

    private final PluginConfig config;

    public AbbCommand(PluginConfig config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antibookban.admin")) {
            Messages.send(sender, config, "no-permission");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            config.reload();
            Messages.send(sender, config, "reloaded");
            return true;
        }
        sender.sendMessage("AntiBookBan v" + AntiBookBan.getInstance().getDescription().getVersion() + " by Enea - /abb reload");
        return true;
    }
}
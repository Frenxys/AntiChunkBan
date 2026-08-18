package com.enea.antibookban.command;

import com.enea.antibookban.AntiBookBan;
import com.enea.antibookban.config.PluginConfig;
import com.enea.antibookban.util.Messages;
import com.enea.antibookban.util.PlayerDataScanner;
import com.enea.antibookban.util.ShulkerScanner;
import com.enea.antibookban.util.ShulkerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class ScanCommand implements CommandExecutor {

    private final AntiBookBan plugin;
    private final PluginConfig config;

    public ScanCommand(AntiBookBan plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antibookban.scan")) {
            Messages.send(sender, config, "no-permission");
            return true;
        }
        if (args.length < 1) {
            Messages.send(sender, config, "usage-scan");
            return true;
        }

        String name = args[0];
        Messages.send(sender, config, "scan-start", "player", name);

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            // Online player: scan the live inventory.
            List<ShulkerScanner.ShulkerReport> found = ShulkerScanner.scanPlayer(online, config.getMaxShulkerSize());
            report(sender, online.getName(), found);
            return true;
        }

        // Offline player: look up and scan their player data file (async).
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Optional<Path> file = PlayerDataScanner.findPlayerDataFile(name);
            List<ShulkerScanner.ShulkerReport> found = file.isPresent()
                    ? PlayerDataScanner.scanFile(file.get(), config.getMaxShulkerSize())
                    : List.of();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (file.isEmpty()) {
                    Messages.send(sender, config, "player-not-found", "player", name);
                    return;
                }
                report(sender, name, found);
            });
        });
        return true;
    }

    private void report(CommandSender sender, String playerName, List<ShulkerScanner.ShulkerReport> found) {
        if (found.isEmpty()) {
            Messages.send(sender, config, "scan-clean", "player", playerName);
            return;
        }
        for (ShulkerScanner.ShulkerReport r : found) {
            Messages.send(sender, config, "scan-found",
                    "player", playerName,
                    "material", r.material(),
                    "slot", r.slot(),
                    "container", r.container(),
                    "size", r.humanSize(),
                    "limit", ShulkerUtil.humanSize(config.getMaxShulkerSize()));
        }
        Messages.send(sender, config, "scan-result", "player", playerName, "count", found.size());
    }
}
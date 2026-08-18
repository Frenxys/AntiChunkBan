package com.enea.antibookban.command;

import com.enea.antibookban.AntiBookBan;
import com.enea.antibookban.config.PluginConfig;
import com.enea.antibookban.util.Messages;
import com.enea.antibookban.util.PlayerDataScanner;
import com.enea.antibookban.util.ShulkerScanner;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FixAllCommand implements CommandExecutor {

    private final AntiBookBan plugin;
    private final PluginConfig config;

    public FixAllCommand(AntiBookBan plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antibookban.fixall")) {
            Messages.send(sender, config, "no-permission");
            return true;
        }

        Messages.send(sender, config, "fixall-start");

        // 1) Offline: modify the player data files (async, may take a while).
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<UUID> onlineUuids = new HashSet<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                onlineUuids.add(p.getUniqueId());
            }

            PlayerDataScanner.FixAllResult offline = PlayerDataScanner.fixAll(
                    config.getMaxShulkerSize(), config.isFixEnderChest(), onlineUuids);

            // 2) Online: remove live shulkers (must run on the main thread).
            Bukkit.getScheduler().runTask(plugin, () -> {
                Map<UUID, List<ShulkerScanner.ShulkerReport>> allFixed = new HashMap<>(offline.fixedByPlayer());
                for (Player p : Bukkit.getOnlinePlayers()) {
                    List<ShulkerScanner.ShulkerReport> removed =
                            ShulkerScanner.fixPlayer(p, config.getMaxShulkerSize(), config.isFixEnderChest());
                    if (!removed.isEmpty()) {
                        allFixed.put(p.getUniqueId(), removed);
                    }
                }

                int shulkers = allFixed.values().stream().mapToInt(List::size).sum();
                Messages.send(sender, config, "fixall-done",
                        "files", offline.filesScanned(),
                        "players", allFixed.size(),
                        "shulkers", shulkers);
            });
        });
        return true;
    }
}
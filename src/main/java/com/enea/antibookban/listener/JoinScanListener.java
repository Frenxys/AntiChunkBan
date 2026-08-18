package com.enea.antibookban.listener;

import com.enea.antibookban.AntiBookBan;
import com.enea.antibookban.config.PluginConfig;
import com.enea.antibookban.util.Messages;
import com.enea.antibookban.util.ShulkerScanner;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

/**
 * When a player joins, checks whether they have suspicious shulkers
 * and alerts operators (antibookban.notify permission).
 */
public final class JoinScanListener implements Listener {

    private final AntiBookBan plugin;
    private final PluginConfig config;

    public JoinScanListener(AntiBookBan plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<ShulkerScanner.ShulkerReport> found = ShulkerScanner.scanPlayer(player, config.getMaxShulkerSize());
        if (found.isEmpty()) {
            return;
        }

        plugin.getLogger().warning(player.getName() + " joined with " + found.size()
                + " suspicious shulkers (limit " + config.getMaxShulkerSize() + " bytes)");

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("antibookban.notify")) {
                Messages.send(p, config, "join-alert", "player", player.getName(), "count", found.size());
            }
        }
    }
}
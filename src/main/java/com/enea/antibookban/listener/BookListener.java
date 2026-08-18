package com.enea.antibookban.listener;

import com.enea.antibookban.config.PluginConfig;
import com.enea.antibookban.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Prevents the creation of "chunk ban" books:
 * - max N pages (default 25)
 * - ASCII characters only (blocks the weird characters used for the exploit)
 */
public final class BookListener implements Listener {

    private final PluginConfig config;

    public BookListener(PluginConfig config) {
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        Player player = event.getPlayer();
        BookMeta meta = event.getNewBookMeta();
        int maxPages = config.getMaxBookPages();

        if (meta.getPageCount() > maxPages) {
            event.setCancelled(true);
            Messages.send(player, config, "book-too-many-pages", "max", maxPages);
            return;
        }

        if (config.isAsciiOnly()) {
            for (String page : meta.getPages()) {
                if (!isAscii(page)) {
                    event.setCancelled(true);
                    Messages.send(player, config, "book-non-ascii");
                    return;
                }
            }
            String title = meta.getTitle();
            if (title != null && !isAscii(title)) {
                event.setCancelled(true);
                Messages.send(player, config, "book-non-ascii");
            }
        }
    }

    private static boolean isAscii(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }
}
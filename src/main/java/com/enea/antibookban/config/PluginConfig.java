package com.enea.antibookban.config;

import com.enea.antibookban.AntiBookBan;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public final class PluginConfig {

    public static final long DEFAULT_MAX_SHULKER_SIZE = 2L * 1024 * 1024; // 2 MB

    private static final Map<String, String> DEFAULT_MESSAGES = new HashMap<>();

    static {
        DEFAULT_MESSAGES.put("no-permission", "&cYou don't have permission to use this command.");
        DEFAULT_MESSAGES.put("player-not-found", "&cPlayer &e%player% &cnot found (never seen on this server?).");
        DEFAULT_MESSAGES.put("usage-scan", "&cUsage: &e/scan <player>");
        DEFAULT_MESSAGES.put("scan-start", "&7Scanning &e%player%&7...");
        DEFAULT_MESSAGES.put("scan-clean", "&a%player%: no suspicious shulker found.");
        DEFAULT_MESSAGES.put("scan-found", "&cDangerous shulker: &f%material% &8(slot &f%slot%&8 - %container%) &7- &c%size% &8(limit %limit%)");
        DEFAULT_MESSAGES.put("scan-result", "&e%player%&7: &c%count% &7suspicious shulkers found.");
        DEFAULT_MESSAGES.put("fixall-start", "&7Scanning all player data (including offline players)...");
        DEFAULT_MESSAGES.put("fixall-done", "&aDone: &e%files% &7files read, &e%players% &7players affected, &c%shulkers% &7shulkers removed. Backups saved as &f.dat.bak&7.");
        DEFAULT_MESSAGES.put("join-alert", "&c%player% &7joined with &c%count% &7suspicious shulkers!");
        DEFAULT_MESSAGES.put("book-too-many-pages", "&cA book can have at most &e%max% &cpages.");
        DEFAULT_MESSAGES.put("book-non-ascii", "&cThe book can only contain ASCII characters (no special characters).");
        DEFAULT_MESSAGES.put("reloaded", "&aConfig reloaded.");
    }

    private final AntiBookBan plugin;
    private final Map<String, String> messages = new HashMap<>();

    private long maxShulkerSize;
    private int maxBookPages;
    private boolean asciiOnly;
    private boolean scanOnJoin;
    private boolean fixEnderChest;
    private String prefix;

    public PluginConfig(AntiBookBan plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        maxShulkerSize = c.getLong("max-shulker-size", DEFAULT_MAX_SHULKER_SIZE);
        if (maxShulkerSize <= 0) {
            maxShulkerSize = DEFAULT_MAX_SHULKER_SIZE;
        }
        maxBookPages = c.getInt("max-book-pages", 25);
        asciiOnly = c.getBoolean("ascii-only", true);
        scanOnJoin = c.getBoolean("scan-on-join", true);
        fixEnderChest = c.getBoolean("fix-ender-chest", true);
        prefix = c.getString("messages.prefix", "&8[&cAntiBookBan&8]&r ");

        messages.clear();
        for (Map.Entry<String, String> entry : DEFAULT_MESSAGES.entrySet()) {
            String value = c.getString("messages." + entry.getKey(), entry.getValue());
            messages.put(entry.getKey(), value);
        }
    }

    /** Formats a message by replacing the %key% placeholders with the provided values. */
    public String message(String key, Object... placeholders) {
        String template = messages.getOrDefault(key, key);
        if (placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                template = template.replace("%" + placeholders[i] + "%", String.valueOf(placeholders[i + 1]));
            }
        }
        return prefix + template;
    }

    public long getMaxShulkerSize() {
        return maxShulkerSize;
    }

    public int getMaxBookPages() {
        return maxBookPages;
    }

    public boolean isAsciiOnly() {
        return asciiOnly;
    }

    public boolean isScanOnJoin() {
        return scanOnJoin;
    }

    public boolean isFixEnderChest() {
        return fixEnderChest;
    }
}

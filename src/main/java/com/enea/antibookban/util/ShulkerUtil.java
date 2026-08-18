package com.enea.antibookban.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public final class ShulkerUtil {

    private ShulkerUtil() {
    }

    /** True if the item is a shulker box (plain or colored). */
    public static boolean isShulkerBox(ItemStack item) {
        return item != null && isShulkerBox(item.getType());
    }

    /** True if the material is a shulker box (plain or colored). */
    public static boolean isShulkerBox(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.equals("SHULKER_BOX") || name.endsWith("_SHULKER_BOX");
    }

    /** True if the NBT id (e.g. "minecraft:red_shulker_box") is a shulker box. */
    public static boolean isShulkerBoxId(String id) {
        if (id == null) {
            return false;
        }
        String lower = id.toLowerCase(Locale.ROOT);
        return lower.equals("minecraft:shulker_box") || lower.endsWith("shulker_box");
    }

    /** Formats a byte size in a human-readable way (B, KB, MB). */
    public static String humanSize(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024.0));
        }
        if (bytes >= 1024L) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }
}

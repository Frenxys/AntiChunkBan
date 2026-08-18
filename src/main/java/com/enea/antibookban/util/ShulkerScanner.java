package com.enea.antibookban.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Scanner for ONLINE players: weighs shulker boxes by serializing the item to NBT
 * (ItemStack#serializeAsBytes, available in all Paper 1.21+ versions).
 */
public final class ShulkerScanner {

    private ShulkerScanner() {
    }

    /** A suspicious shulker found in an inventory. */
    public record ShulkerReport(String container, int slot, String material, long sizeBytes) {
        public String humanSize() {
            return ShulkerUtil.humanSize(sizeBytes);
        }
    }

    /** Scans inventory + armor + offhand + ender chest of an online player. */
    public static List<ShulkerReport> scanPlayer(Player player, long maxSize) {
        List<ShulkerReport> found = new ArrayList<>();
        PlayerInventory inv = player.getInventory();

        ItemStack[] main = inv.getContents();
        for (int i = 0; i < main.length; i++) {
            check(found, "inventory", i, main[i], maxSize);
        }
        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            check(found, "armor", i, armor[i], maxSize);
        }
        check(found, "offhand", 40, inv.getItemInOffHand(), maxSize);

        ItemStack[] ender = player.getEnderChest().getContents();
        for (int i = 0; i < ender.length; i++) {
            check(found, "ender chest", i, ender[i], maxSize);
        }
        return found;
    }

    /** Removes suspicious shulkers from a player's inventory (used by /fixall). */
    public static List<ShulkerReport> fixPlayer(Player player, long maxSize, boolean fixEnderChest) {
        List<ShulkerReport> removed = new ArrayList<>();
        PlayerInventory inv = player.getInventory();

        ItemStack[] main = inv.getContents();
        for (int i = 0; i < main.length; i++) {
            ShulkerReport r = checkAndRemove(main[i], "inventory", i, maxSize);
            if (r != null) {
                inv.setItem(i, null);
                removed.add(r);
            }
        }
        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ShulkerReport r = checkAndRemove(armor[i], "armor", i, maxSize);
            if (r != null) {
                // raw slots: boots=36, leggings=37, chestplate=38, helmet=39
                inv.setItem(36 + i, null);
                removed.add(r);
            }
        }
        ShulkerReport off = checkAndRemove(inv.getItemInOffHand(), "offhand", 40, maxSize);
        if (off != null) {
            inv.setItemInOffHand(null);
            removed.add(off);
        }

        if (fixEnderChest) {
            ItemStack[] ender = player.getEnderChest().getContents();
            for (int i = 0; i < ender.length; i++) {
                ShulkerReport r = checkAndRemove(ender[i], "ender chest", i, maxSize);
                if (r != null) {
                    player.getEnderChest().setItem(i, null);
                    removed.add(r);
                }
            }
        }
        return removed;
    }

    private static void check(List<ShulkerReport> found, String container, int slot, ItemStack item, long maxSize) {
        ShulkerReport r = checkAndRemove(item, container, slot, maxSize);
        if (r != null) {
            found.add(r);
        }
    }

    /** Returns a report if the item is a shulker that exceeds the limit, otherwise null. */
    private static ShulkerReport checkAndRemove(ItemStack item, String container, int slot, long maxSize) {
        if (!ShulkerUtil.isShulkerBox(item)) {
            return null;
        }
        long size = item.serializeAsBytes().length;
        if (size > maxSize) {
            return new ShulkerReport(container, slot, item.getType().name(), size);
        }
        return null;
    }
}

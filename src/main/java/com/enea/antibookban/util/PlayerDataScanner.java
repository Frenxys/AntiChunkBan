package com.enea.antibookban.util;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagIO.Compression;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Scanner for OFFLINE players: reads player files directly
 * (gzip NBT format) using Adventure NBT, shaded into the jar.
 * Handles both <world>/playerdata (1.21-26.0) and <world>/players/data (26.1+).
 * No NMS involved: the same jar works from 1.21 to 26.2+.
 */
public final class PlayerDataScanner {

    private PlayerDataScanner() {
    }

    /** Overall result of /fixall on offline files. */
    public record FixAllResult(int filesScanned, Map<UUID, List<ShulkerScanner.ShulkerReport>> fixedByPlayer) {
        public int totalRemoved() {
            return fixedByPlayer.values().stream().mapToInt(List::size).sum();
        }
    }

    /**
     * Looks up a player's player data file by name, scanning the player data
     * of all worlds. First searches by name (bukkit.lastKnownName field inside
     * the .dat), then by the offline player's UUID.
     */
    public static Optional<Path> findPlayerDataFile(String name) {
        List<Path> dirs = playerDataDirs();

        // 1) By name: reads the bukkit.lastKnownName field of every file.
        for (Path dir : dirs) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.dat")) {
                for (Path file : stream) {
                    try {
                        CompoundBinaryTag root = read(file);
                        String lastKnown = root.getCompound("bukkit").getString("lastKnownName");
                        if (!lastKnown.isEmpty() && lastKnown.equalsIgnoreCase(name)) {
                            return Optional.of(file);
                        }
                    } catch (IOException ignored) {
                        // unreadable/corrupt file: skip it
                    }
                }
            } catch (IOException ignored) {
                // unreadable folder
            }
        }

        // 2) UUID fallback (if the player has already been on the server,
        //    Bukkit knows their UUID even when offline).
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        UUID uuid = offline.getUniqueId();
        if (uuid != null) {
            for (Path dir : dirs) {
                Path file = dir.resolve(uuid + ".dat");
                if (Files.isRegularFile(file)) {
                    return Optional.of(file);
                }
            }
        }
        return Optional.empty();
    }

    /** Scans a single player data file without modifying it. */
    public static List<ShulkerScanner.ShulkerReport> scanFile(Path file, long maxSize) {
        List<ShulkerScanner.ShulkerReport> found = new ArrayList<>();
        try {
            CompoundBinaryTag root = read(file);
            collectOversized(root, "Inventory", "inventory", maxSize, found);
            collectOversized(root, "EnderItems", "ender chest", maxSize, found);
        } catch (IOException ignored) {
            // unreadable file: no results
        }
        return found;
    }

    /**
     * Removes suspicious shulkers from a player data file.
     * Creates a <uuid>.dat.bak backup before writing. Returns the removed shulkers.
     */
    public static List<ShulkerScanner.ShulkerReport> fixFile(Path file, long maxSize, boolean fixEnderChest) {
        try {
            Compression compression = detectCompression(file);
            CompoundBinaryTag root = read(file, compression);
            List<ShulkerScanner.ShulkerReport> removed = new ArrayList<>();

            CompoundBinaryTag newRoot = removeOversized(root, "Inventory", "inventory", maxSize, removed);
            if (fixEnderChest) {
                newRoot = removeOversized(newRoot, "EnderItems", "ender chest", maxSize, removed);
            }

            if (!removed.isEmpty()) {
                backup(file);
                write(file, newRoot, compression);
            }
            return removed;
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * Scans and cleans the player data of ALL worlds (offline players).
     * Handles both the old "playerdata" folder (1.21-26.0) and the new
     * "players/data" folder (26.1+). Online players (skipUuids) are
     * skipped: the main thread handles them.
     */
    public static FixAllResult fixAll(long maxSize, boolean fixEnderChest, Set<UUID> skipUuids) {
        int filesScanned = 0;
        Map<UUID, List<ShulkerScanner.ShulkerReport>> fixed = new HashMap<>();

        for (Path dir : playerDataDirs()) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.dat")) {
                for (Path file : stream) {
                    filesScanned++;
                    Optional<UUID> uuid = uuidFromFileName(file);
                    if (uuid.isEmpty() || skipUuids.contains(uuid.get())) {
                        continue;
                    }
                    List<ShulkerScanner.ShulkerReport> removed = fixFile(file, maxSize, fixEnderChest);
                    if (!removed.isEmpty()) {
                        fixed.put(uuid.get(), removed);
                    }
                }
            } catch (IOException ignored) {
                // unreadable folder
            }
        }
        return new FixAllResult(filesScanned, fixed);
    }

    // ── player data folders ─────────────────────────────────────

    /**
     * All folders that can contain player files, in every world:
     * - <world>/playerdata        (old format, up to 26.0)
     * - <world>/players/data      (new format, from 26.1)
     */
    private static List<Path> playerDataDirs() {
        Set<Path> dirs = new LinkedHashSet<>();
        for (World world : Bukkit.getWorlds()) {
            Path root = worldRoot(world);
            Path old = root.resolve("playerdata");
            if (Files.isDirectory(old)) {
                dirs.add(old);
            }
            Path newDir = root.resolve("players").resolve("data");
            if (Files.isDirectory(newDir)) {
                dirs.add(newDir);
            }
        }
        return new ArrayList<>(dirs);
    }

    /**
     * World root (the folder that contains level.dat).
     * Up to 26.0 getWorldFolder() is already the root; from 26.1 it points to
     * the dimension folder (e.g. world/dimensions/minecraft/overworld),
     * so we walk up until we find level.dat.
     */
    private static Path worldRoot(World world) {
        Path p = world.getWorldFolder().toPath().toAbsolutePath();
        while (p != null) {
            if (Files.isRegularFile(p.resolve("level.dat"))) {
                return p;
            }
            p = p.getParent();
        }
        return world.getWorldFolder().toPath();
    }

    // ── internals ────────────────────────────────────────────────

    private static void collectOversized(CompoundBinaryTag root, String listKey, String container,
                                         long maxSize, List<ShulkerScanner.ShulkerReport> found) throws IOException {
        ListBinaryTag list = root.getList(listKey);
        int slot = 0;
        for (BinaryTag tag : list) {
            if (tag instanceof CompoundBinaryTag item) {
                String id = item.getString("id");
                if (ShulkerUtil.isShulkerBoxId(id)) {
                    long size = nbtSize(item);
                    if (size > maxSize) {
                        found.add(new ShulkerScanner.ShulkerReport(container, slot, id, size));
                    }
                }
            }
            slot++;
        }
    }

    private static CompoundBinaryTag removeOversized(CompoundBinaryTag root, String listKey, String container,
                                                     long maxSize, List<ShulkerScanner.ShulkerReport> removed) throws IOException {
        ListBinaryTag list = root.getList(listKey);
        if (list.size() == 0) {
            return root;
        }
        ListBinaryTag.Builder builder = ListBinaryTag.builder();
        boolean changed = false;
        int slot = 0;
        for (BinaryTag tag : list) {
            boolean remove = false;
            if (tag instanceof CompoundBinaryTag item) {
                String id = item.getString("id");
                if (ShulkerUtil.isShulkerBoxId(id)) {
                    long size = nbtSize(item);
                    if (size > maxSize) {
                        removed.add(new ShulkerScanner.ShulkerReport(container, slot, id, size));
                        remove = true;
                        changed = true;
                    }
                }
            }
            if (!remove) {
                builder.add(tag);
            }
            slot++;
        }
        return changed ? root.put(listKey, builder.build()) : root;
    }

    /** Size in bytes of an item's serialized NBT (id + count + tag). */
    private static long nbtSize(CompoundBinaryTag item) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryTagIO.writer().write(item, baos);
        return baos.size();
    }

    private static CompoundBinaryTag read(Path file) throws IOException {
        return read(file, detectCompression(file));
    }

    private static CompoundBinaryTag read(Path file, Compression compression) throws IOException {
        // unlimitedReader: the infected files are precisely the huge ones,
        // and the standard reader would reject tags above its limit.
        return BinaryTagIO.unlimitedReader().read(file, compression);
    }

    private static void write(Path file, CompoundBinaryTag root, Compression compression) throws IOException {
        BinaryTagIO.writer().write(root, file, compression);
    }

    /** Detects compression from the magic bytes (gzip = 0x1f 0x8b). */
    private static Compression detectCompression(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            DataInputStream dis = new DataInputStream(in);
            int b1 = dis.readUnsignedByte();
            int b2 = dis.readUnsignedByte();
            if (b1 == 0x1F && b2 == 0x8B) {
                return Compression.GZIP;
            }
        }
        return Compression.NONE;
    }

    private static void backup(Path file) throws IOException {
        Path bak = file.resolveSibling(file.getFileName() + ".bak");
        Files.copy(file, bak, StandardCopyOption.REPLACE_EXISTING);
    }

    private static Optional<UUID> uuidFromFileName(Path file) {
        String name = file.getFileName().toString();
        if (!name.endsWith(".dat")) {
            return Optional.empty();
        }
        String uuidStr = name.substring(0, name.length() - 4);
        try {
            return Optional.of(UUID.fromString(uuidStr));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
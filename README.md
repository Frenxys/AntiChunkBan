# AntiBookBan

Paper anti-**chunk ban** plugin by **Enea**. A single jar compatible with **Paper 1.21 → 1.26.2+**.

What it does:
- **`/scan <player>`** — scans a player's inventory (including **offline** players) and reports shulker boxes that exceed the limit (default **2 MB**).
- **`/fixall`** — scans **all** player data of all worlds (including offline players) and **deletes** shulker boxes that exceed the limit. Before each modification it creates a `<uuid>.dat.bak` backup.
- **Book limits** — a book can have at most **25 pages** and **ASCII** characters only (blocks the "weird" characters used for the exploit).
- **Automatic scan on join** — if a player joins with a suspicious shulker, operators are notified.

## Compatibility

| Version | Player file | Status |
|---|---|---|
| 1.21 – 26.0 | `<world>/playerdata/<uuid>.dat` | ✅ tested on 1.21 |
| 26.1 – 26.2+ | `<world>/players/data/<uuid>.dat` | ✅ tested on 26.2 |

The plugin is compiled against the Paper **1.21** API (Paper maintains backward compatibility) and uses only stable APIs: no NMS, so the same jar runs across the whole range. Adventure NBT is **shaded into the jar** (relocated), so it does not depend on the server's classpath.

## Requirements

- Java 21+ (Paper 1.21+); Paper 26.1+ requires Java 25 on the server
- Paper (or a fork like Purpur) — not Spigot/Bukkit

## Build

```bash
./gradlew build          # or gradlew.bat on Windows
# output: build/libs/AntiBookBan.jar
```

## Installation

1. Copy `AntiBookBan.jar` into the server's `plugins/` folder.
2. Restart the server (or `/reload confirm`).
3. Configure `plugins/AntiBookBan/config.yml` and then run `/abb reload`.

## Commands and permissions

| Command | Description | Permission |
|---|---|---|
| `/scan <player>` | Scans inventory + armor + offhand + ender chest (online or offline) | `antibookban.scan` (op) |
| `/fixall` | Removes heavy shulkers from ALL player data (offline) and from online inventories | `antibookban.fixall` (op) |
| `/abb reload` | Reloads the config | `antibookban.admin` (op) |

Other permissions: `antibookban.notify` (receives join alerts, op by default).

## Configuration

```yaml
max-shulker-size: 2097152   # shulker limit in bytes (2 MB)
max-book-pages: 25          # maximum book pages
ascii-only: true            # ASCII characters only in books
scan-on-join: true          # notify ops if a player joins with suspicious shulkers
fix-ender-chest: true       # /fixall also checks the ender chest
```

> Note: `ascii-only: true` also blocks accented letters (è, à...). If you need them, set it to `false`.

## How it works

- **Online**: each item is serialized to NBT with `ItemStack#serializeAsBytes()`; if a shulker exceeds the limit, it is reported/removed.
- **Offline**: `.dat` files are read/written directly with Adventure NBT (gzip), looking for shulkers in the `Inventory` and `EnderItems` lists and measuring the item's serialized NBT size. The infected files are precisely the huge ones, so an unlimited-size reader is used.

## Future ideas

- **Scan books in offline player data**: `/fixall` currently removes only heavy shulkers; it could be extended to also remove books with too many pages/non-ASCII characters.
- **Automatic removal on join**: instead of alerting, remove the suspicious shulker immediately (config option).
- **Detailed log**: log file with date/time, player and slot for every removal.
- **Whitelist/blacklist**: exclude certain worlds or players from the scan.
- **Discord/webhook notifications** when `/fixall` finds something.
- **bStats** for usage statistics.

## Test

The `test/` folder contains scripts to try the plugin on real Paper servers:

```bash
# 1) download the servers (see test/run-test.sh) and create an "infected" player data file
python test/make-bad-playerdata.py test/11111111-1111-1111-1111-111111111111.dat
# 2) run the test on a server (injects the file, runs /scan and /fixall via RCON)
bash test/run-test.sh paper-1.21 25575 test/11111111-1111-1111-1111-111111111111.dat
```

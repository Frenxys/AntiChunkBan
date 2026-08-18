"""Generates a fake player data file with a "chunk ban" shulker box (> 2 MB).

Structure: inventory with a shulker box containing a written book
with a ~2.1 MB page of text. The resulting file is gzip NBT,
same format as <world>/playerdata/<uuid>.dat (1.21-26.0) and
<world>/players/data/<uuid>.dat (26.1+).
"""
import gzip
import struct
import sys

TAG_END = 0
TAG_BYTE = 1
TAG_INT = 3
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10

# In NBT a string is limited to 65535 bytes: we use 40 pages of
# 65535 bytes (~2.6 MB) -> the shulker exceeds the 2 MB limit.
PAGE_SIZE = 65535
PAGE_COUNT = 40
UUID = "11111111-1111-1111-1111-111111111111"
NAME = "TestPlayer"
DATAVERSION = 3700  # 1.21


def tag_string(s):
    data = s.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def tag_byte(v):
    return struct.pack(">b", v)


def tag_int(v):
    return struct.pack(">i", v)


def tag_compound(entries):
    buf = bytearray()
    for name, tag_type, payload in entries:
        buf.append(tag_type)
        buf += tag_string(name)
        buf += payload
    buf.append(TAG_END)
    return bytes(buf)


def tag_list(elem_type, items):
    buf = bytearray()
    buf.append(elem_type)
    buf += struct.pack(">i", len(items))
    for item in items:
        buf += item
    return bytes(buf)


# written book with a huge page
book = tag_compound([
    ("id", TAG_STRING, tag_string("minecraft:written_book")),
    ("Count", TAG_BYTE, tag_byte(1)),
    ("Slot", TAG_BYTE, tag_byte(0)),
    ("tag", TAG_COMPOUND, tag_compound([
        ("title", TAG_STRING, tag_string("test")),
        ("author", TAG_STRING, tag_string("test")),
        ("pages", TAG_LIST, tag_list(TAG_STRING, [tag_string("a" * PAGE_SIZE) for _ in range(PAGE_COUNT)])),
    ])),
])

# shulker box that contains the book
shulker = tag_compound([
    ("id", TAG_STRING, tag_string("minecraft:shulker_box")),
    ("Count", TAG_BYTE, tag_byte(1)),
    ("Slot", TAG_BYTE, tag_byte(0)),
    ("tag", TAG_COMPOUND, tag_compound([
        ("BlockEntityTag", TAG_COMPOUND, tag_compound([
            ("id", TAG_STRING, tag_string("minecraft:shulker_box")),
            ("Items", TAG_LIST, tag_list(TAG_COMPOUND, [book])),
        ])),
    ])),
])

root = tag_compound([
    ("DataVersion", TAG_INT, tag_int(DATAVERSION)),
    ("Inventory", TAG_LIST, tag_list(TAG_COMPOUND, [shulker])),
    ("bukkit", TAG_COMPOUND, tag_compound([
        ("lastKnownName", TAG_STRING, tag_string(NAME)),
    ])),
])

out = sys.argv[1] if len(sys.argv) > 1 else f"{UUID}.dat"
with gzip.open(out, "wb") as f:
    f.write(struct.pack(">b", TAG_COMPOUND))
    f.write(struct.pack(">H", 0))  # empty root name
    f.write(root)
print(f"written {out}: {len(root)} bytes of NBT, shulker ~{PAGE_COUNT * PAGE_SIZE // 1024 // 1024} MB > 2 MB")

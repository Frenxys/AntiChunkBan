"""Minimal RCON client: python rcon.py <host> <port> <password> <command> [wait_seconds]"""
import socket
import struct
import sys
import time


def send_packet(sock, rid, ptype, payload):
    data = struct.pack("<ii", rid, ptype) + payload.encode("utf-8") + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(data)) + data)


def recv_packet(sock):
    length = struct.unpack("<i", sock.recv(4))[0]
    data = b""
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            break
        data += chunk
    rid, ptype = struct.unpack("<ii", data[:8])
    payload = data[8:-2].decode("utf-8", errors="replace")
    return rid, ptype, payload


host, port, password, command = sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4]
wait = float(sys.argv[5]) if len(sys.argv) > 5 else 0.0

s = socket.create_connection((host, port), timeout=10)
send_packet(s, 1, 3, password)
rid, ptype, resp = recv_packet(s)
if ptype != 2:
    print("AUTH_FAILED")
    sys.exit(1)

send_packet(s, 2, 2, command)
end = time.time() + wait
while True:
    remaining = end - time.time()
    if remaining <= 0:
        break
    s.settimeout(min(1.0, remaining))
    try:
        rid, ptype, resp = recv_packet(s)
        if resp:
            print(resp)
    except socket.timeout:
        continue  # wait for more packets (async responses) until timeout
s.close()
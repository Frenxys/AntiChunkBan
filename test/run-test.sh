#!/usr/bin/env bash
# Usage: bash test/run-test.sh <server-dir> <rcon-port> [inject-playerdata]
# Starts a test Paper server, waits for it to be ready, injects a fake
# player data file (if provided), sends AntiBookBan commands via RCON and stops the server.
set -u

# Resolves the path of the file to inject relative to the caller's cwd,
# BEFORE any cd.
INJECT=""
if [ -n "${3:-}" ]; then
  INJECT="$(cd "$(dirname "$3")" 2>/dev/null && pwd)/$(basename "$3")"
  [ -f "$INJECT" ] || INJECT=""
fi

cd "$(dirname "$0")"
SERVER_DIR="$1"
RCON_PORT="$2"

cd "$SERVER_DIR"
rm -rf world world_nether world_the_end logs

nohup java -Xmx1G -jar paper.jar nogui > server.log 2>&1 < /dev/null &
SERVER_PID=$!
echo "Server PID: $SERVER_PID"

STARTED=0
for i in $(seq 1 90); do
  if grep -q "Done (" server.log 2>/dev/null; then
    STARTED=1
    break
  fi
  if grep -qiE "failed to (start|bind)|Fatal" server.log 2>/dev/null; then
    break
  fi
  sleep 2
done

if [ "$STARTED" = "1" ]; then
  # Inject the fake player data (if requested) into the old and new folders
  if [ -n "$INJECT" ]; then
    INJECT_NAME=$(basename "$INJECT")
    mkdir -p world/playerdata world/players/data
    cp "$INJECT" "world/playerdata/$INJECT_NAME"
    cp "$INJECT" "world/players/data/$INJECT_NAME"
    echo "=== injected $INJECT_NAME (size: $(stat -c%s "$INJECT") bytes) ==="
  fi

  echo "=== server ready, sending commands ==="
  python ../rcon.py 127.0.0.1 "$RCON_PORT" test1234 "abb" 2
  python ../rcon.py 127.0.0.1 "$RCON_PORT" test1234 "scan TestPlayer" 8
  python ../rcon.py 127.0.0.1 "$RCON_PORT" test1234 "fixall" 12
  python ../rcon.py 127.0.0.1 "$RCON_PORT" test1234 "stop" 2
else
  echo "=== server did NOT finish starting ==="
  tail -25 server.log
fi

for i in $(seq 1 20); do
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    break
  fi
  sleep 2
done
kill "$SERVER_PID" 2>/dev/null
wait "$SERVER_PID" 2>/dev/null

echo "=== AntiBookBan lines ==="
grep -i "antibookban" server.log | head -20
echo "=== errors/exceptions ==="
grep -iE "error|exception" server.log | grep -viE "there are no errors|errors in log" | head -10
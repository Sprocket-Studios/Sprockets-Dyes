#!/usr/bin/env bash
# Launches each node's dev client with -Pclienttest (it exits once resources are loaded) and reports any
# model/blockstate/texture problem involving our namespace or one of the new colors.
# Usage: scripts/clienttest.sh [node ...]     (default: all nodes). Opens a game window briefly per node.
set -u
cd "$(dirname "$0")/.."
LOGS=build/clienttest
mkdir -p "$LOGS"
NODES=("$@")
if [ ${#NODES[@]} -eq 0 ]; then
  for v in 1.21.1 1.21.3 1.21.4 1.21.5 1.21.8 1.21.10 1.21.11 26.1.2 26.2 26.3; do
    NODES+=("$v-fabric" "$v-neoforge")
  done
fi
COLORS=$(python3 -c "import json; d=json.load(open('palette/colors.json')); print('|'.join(c['id'] for c in d['colors'] if not c['vanilla']))")
fail=0
for node in "${NODES[@]}"; do
  log="$LOGS/$node.log"
  ./gradlew ":$node:runClient" -Pclienttest --console=plain >"$log" 2>&1
  problems=$(grep -E "(WARN|ERROR)" "$log" | grep -E "sprocket_dyes|[:/_](${COLORS})[_./'\"]" | grep -vE "logoFile|Reference map" | sort -u)
  if grep -q "\[clienttest\] resources loaded" "$log" && [ -z "$problems" ]; then
    printf '%-18s PASS\n' "$node"
  else
    fail=1
    printf '%-18s FAIL  (%s, %s problem lines)\n' "$node" "$log" "$(echo "$problems" | sed '/^$/d' | wc -l | tr -d ' ')"
    echo "$problems" | sed '/^$/d' | head -8 | cut -c1-300 | sed 's/^/    /'
  fi
done
exit $fail

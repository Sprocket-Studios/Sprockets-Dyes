#!/usr/bin/env bash
# Runs the dev self-test server (-Pselftest) on every Stonecutter node and prints a pass/fail table.
# Usage: scripts/selftest-all.sh [node ...]     (default: all nodes)
set -u
cd "$(dirname "$0")/.."
LOGS=build/selftest
mkdir -p "$LOGS"
NODES=("$@")
if [ ${#NODES[@]} -eq 0 ]; then
  for v in 1.21.1 1.21.3 1.21.4 1.21.5 1.21.8 1.21.10 1.21.11 26.1.2 26.2 26.3; do
    NODES+=("$v-fabric" "$v-neoforge")
  done
fi
fail=0
for node in "${NODES[@]}"; do
  log="$LOGS/$node.log"
  ./gradlew ":$node:runServer" -Pselftest --console=plain >"$log" 2>&1
  # Data that fails to load is only logged, so treat any error mentioning our namespace as a failure too.
  data_errors=$(grep -E "(ERROR|WARN).*(sprocket_dyes|Couldn't parse|Failed to (parse|load))" "$log" | grep -E "sprocket_dyes" | head -5)
  if grep -q "\[selftest\] PASSED" "$log" && [ -z "$data_errors" ]; then
    printf '%-18s PASS\n' "$node"
  else
    fail=1
    printf '%-18s FAIL  (%s)\n' "$node" "$log"
    { echo "$data_errors"; grep -E "\[selftest\] FAIL|Caused by|Exception" "$log" | head -5; } | sed '/^$/d; s/^/    /' | cut -c1-400
  fi
done
exit $fail

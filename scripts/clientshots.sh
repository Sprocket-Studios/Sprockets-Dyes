#!/usr/bin/env bash
# Opens the creative inventory's Colored Blocks tab in a test world and saves screenshots to run/screenshots/.
# Usage: scripts/clientshots.sh <node>   (needs the node's self-test world: scripts/selftest-all.sh <node> first)
set -eu
cd "$(dirname "$0")/.."
node=$1
world=run/server-$node/world
[ -d "$world" ] || { echo "no $world - run scripts/selftest-all.sh $node first"; exit 1; }
rm -rf run/saves/sprocket-test && mkdir -p run/saves && cp -R "$world" run/saves/sprocket-test
rm -f run/screenshots/sprocket_dyes_*.png
# Skip the first-launch accessibility screen; it would block quick play.
if [ -f run/options.txt ]; then
  sed -i '' 's/^onboardAccessibility:true/onboardAccessibility:false/' run/options.txt
else
  echo "onboardAccessibility:false" > run/options.txt
fi
./gradlew ":$node:runClient" -Pclientshots --console=plain > build/clientshots-$node.log 2>&1 || true
grep -E "\[clienttest\]|Mixin apply|Exception" build/clientshots-$node.log | head -20 || true
ls -1 run/screenshots/sprocket_dyes_*.png 2>/dev/null

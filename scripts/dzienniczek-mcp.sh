#!/usr/bin/env sh
# Przenośny punkt startowy MCP dla konfiguracji repozytorium.
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
launcher="$project_dir/cli/build/install/dzienniczek/bin/dzienniczek"

if [ -x /opt/homebrew/opt/openjdk@17/bin/java ]; then
  JAVA_HOME=/opt/homebrew/opt/openjdk@17
  PATH="$JAVA_HOME/bin:$PATH"
  export JAVA_HOME PATH
elif [ -x /usr/local/opt/openjdk@17/bin/java ]; then
  JAVA_HOME=/usr/local/opt/openjdk@17
  PATH="$JAVA_HOME/bin:$PATH"
  export JAVA_HOME PATH
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  PATH="$JAVA_HOME/bin:$PATH"
  export PATH
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Dzienniczek MCP wymaga Javy 17 lub nowszej." >&2
  exit 1
fi

java_major=$(java -version 2>&1 | awk -F '[\".]' '/version/ { print ($2 == 1 ? $3 : $2); exit }')
if [ "${java_major:-0}" -lt 17 ]; then
  echo "Dzienniczek MCP wymaga Javy 17 lub nowszej; wykryto wersję ${java_major:-nieznaną}." >&2
  exit 1
fi

needs_build=false
if [ ! -x "$launcher" ]; then
  needs_build=true
elif [ "$project_dir/cli/build.gradle.kts" -nt "$launcher" ] || \
     [ "$project_dir/gradle/libs.versions.toml" -nt "$launcher" ]; then
  needs_build=true
elif find "$project_dir/cli/src" -type f -newer "$launcher" -print -quit | grep -q .; then
  needs_build=true
fi

if [ "$needs_build" = true ]; then
  echo "Budowanie lokalnego serwera Dzienniczek MCP…" >&2
  (cd "$project_dir" && ./gradlew :cli:installDist --no-daemon --quiet 1>&2)
fi

cd "$project_dir"
exec "$launcher" mcp "$@"

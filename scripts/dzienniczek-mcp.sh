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

# Sam znacznik czasu launchera nie wystarcza: Gradle może go zachować po zmianie kodu.
# Suma obejmuje także usunięcia plików i nie przebudowuje serwera po zmianie samych testów.
stamp="$project_dir/cli/build/mcp-source-checksum"
source_checksum=$(
  cd "$project_dir"
  {
    find cli/src/main -type f
    printf '%s\n' build.gradle.kts settings.gradle.kts gradle.properties cli/build.gradle.kts \
      gradle/libs.versions.toml gradle/wrapper/gradle-wrapper.properties
  } | LC_ALL=C sort | while IFS= read -r source; do cksum "$source"; done | cksum
)
if [ ! -x "$launcher" ] || [ ! -f "$stamp" ] || [ "$(cat "$stamp")" != "$source_checksum" ]; then
  echo "Budowanie lokalnego serwera Dzienniczek MCP…" >&2
  (cd "$project_dir" && ./gradlew :cli:installDist --no-daemon --quiet 1>&2)
  printf '%s\n' "$source_checksum" > "$stamp.tmp.$$"
  mv "$stamp.tmp.$$" "$stamp"
fi

cd "$project_dir"
exec "$launcher" mcp "$@"

#!/usr/bin/env sh
# Lokalny skrypt uruchamiający Dzienniczek CLI
set -eu

launcher_path=$0
while [ -L "$launcher_path" ]; do
  link_target=$(readlink "$launcher_path")
  case "$link_target" in
    /*) launcher_path=$link_target ;;
    *) launcher_path=$(dirname -- "$launcher_path")/$link_target ;;
  esac
done
launcher_dir=$(CDPATH= cd -- "$(dirname -- "$launcher_path")" && pwd)
launcher="$launcher_dir/../lib/dzienniczek-1.1.0/bin/dzienniczek"

if [ -n "${DZIENNICZEK_JAVA_HOME:-}" ]; then
  JAVA_HOME=$DZIENNICZEK_JAVA_HOME
  export JAVA_HOME
elif [ -z "${JAVA_HOME:-}" ] && [ -x /opt/homebrew/opt/openjdk@17/bin/java ]; then
  JAVA_HOME=/opt/homebrew/opt/openjdk@17
  export JAVA_HOME
fi

exec "$launcher" "$@"

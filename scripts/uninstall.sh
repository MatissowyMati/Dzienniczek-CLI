#!/usr/bin/env sh
set -eu

install_prefix=${DZIENNICZEK_PREFIX:-"$HOME/.local"}
target="$install_prefix/lib/dzienniczek-1.1.0"
link="$install_prefix/bin/dzienniczek"

managed_link=false
if [ -f "$link" ] && grep -Eq "Dzienniczek CLI local launcher|Lokalny skrypt uruchamiający Dzienniczek CLI" "$link"; then
  managed_link=true
fi

if [ "$managed_link" = true ] && [ -e "$link.uninstalled" ]; then
  echo "Istniejąca kopia nie zostanie nadpisana: $link.uninstalled" >&2
  exit 1
fi
if [ -d "$target" ] && [ -e "$target.uninstalled" ]; then
  echo "Istniejąca kopia nie zostanie nadpisana: $target.uninstalled" >&2
  exit 1
fi

if [ "$managed_link" = true ]; then
  mv "$link" "$link.uninstalled"
fi
if [ -d "$target" ]; then
  mv "$target" "$target.uninstalled"
  echo "Przeniesiono instalację do $target.uninstalled (operację można odwrócić)."
fi

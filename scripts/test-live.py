#!/usr/bin/env python3
"""Testy odczytu aktywnego profilu; raport zawiera wyłącznie statusy i liczby."""
from __future__ import annotations

import argparse
from datetime import date, timedelta
import json
import os
from pathlib import Path
import subprocess
import time

PROJECT = Path(__file__).resolve().parent.parent


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--live", action="store_true", help="Jawnie włącz odczyty prawdziwego profilu.")
    parser.add_argument("--messages", action="store_true", help="Sprawdź także listę jednej odebranej wiadomości.")
    parser.add_argument("--cli", default=str(PROJECT / "cli/build/install/dzienniczek/bin/dzienniczek"))
    parser.add_argument("--profile")
    parser.add_argument("--account")
    args = parser.parse_args()
    if not args.live:
        parser.error("Odczyt profilu wymaga --live; test bez konta: python3 scripts/test-mcp.py")
    environment = os.environ.copy()
    for java in ("/opt/homebrew/opt/openjdk@17", "/usr/local/opt/openjdk@17"):
        if Path(java, "bin/java").is_file() and not environment.get("JAVA_HOME"):
            environment["JAVA_HOME"] = java
            break
    options = ["--json", "--compact", "--non-interactive", "--timeout", "20"]
    for option, value in (("profile", args.profile), ("account", args.account)):
        if value is not None:
            options.extend([f"--{option}", value])
    today = date.today()
    start = today - timedelta(days=today.weekday())
    interval = ["--from", start.isoformat(), "--to", (start + timedelta(days=6)).isoformat()]
    commands = [
        ["doctor"], ["capabilities"], ["periods"], ["grades"], ["grades", "averages"], ["grades", "summary"],
        ["schedule", *interval], ["schedule-extra", *interval], ["exams", *interval], ["homework", *interval],
        ["presence", *interval], ["presence", "months"], ["presence", "subjects"], ["notes"], ["announcements"],
        ["lucky-number"], ["timeslots"], ["teachers"], ["school-info"], ["dashboard"],
    ]
    if args.messages:
        commands.append(["messages", "received", "--page-size", "1"])
    failures = 0
    for command in commands:
        started = time.monotonic()
        result = {"command": " ".join(command)}
        try:
            process = subprocess.run([args.cli, *command, *options], cwd=PROJECT, env=environment,
                                     capture_output=True, text=True, timeout=45)
            result["exit"] = process.returncode
            if process.returncode:
                result["ok"] = False
                # Nie publikuj błędów dostawcy: mogą zawierać treść danych ucznia.
            else:
                data = json.loads(process.stdout)
                result["ok"] = not process.stderr.strip()
                if isinstance(data, list):
                    result["records"] = len(data)
                if command == ["doctor"]:
                    result["ok"] = result["ok"] and data.get("hasUsableProfile") is True
                if command == ["capabilities"]:
                    result["ok"] = result["ok"] and bool(data.get("mcpReadOnlyCommands"))
        except (subprocess.TimeoutExpired, OSError, ValueError):
            result["ok"] = False
        result["seconds"] = round(time.monotonic() - started, 2)
        failures += not result["ok"]
        print(json.dumps(result, ensure_ascii=False), flush=True)
        if command == ["doctor"] and not result["ok"]:
            break
    if failures:
        raise SystemExit(1)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Test dymny protokołu MCP bez dostępu do prawdziwego profilu użytkownika."""

from __future__ import annotations

import json
import os
import subprocess
import tempfile
from pathlib import Path
from typing import Any


PROJECT = Path(__file__).resolve().parent.parent
SERVER = PROJECT / "scripts/dzienniczek-mcp.sh"


def rpc(process: subprocess.Popen[str], payload: dict[str, Any]) -> dict[str, Any]:
    assert process.stdin is not None
    assert process.stdout is not None
    process.stdin.write(json.dumps(payload) + "\n")
    process.stdin.flush()
    while line := process.stdout.readline():
        response = json.loads(line)
        if response.get("id") == payload.get("id"):
            return response
    stderr = process.stderr.read() if process.stderr else ""
    raise RuntimeError(f"Serwer MCP zakończył pracę przed odpowiedzią: {stderr}")


def main() -> None:
    if not SERVER.is_file():
        raise SystemExit("Nie znaleziono skryptu scripts/dzienniczek-mcp.sh")

    with tempfile.TemporaryDirectory(prefix="dzienniczek-mcp-test-") as temporary:
        environment = os.environ.copy()
        environment["XDG_CONFIG_HOME"] = str(Path(temporary) / "config")
        process = subprocess.Popen(
            [str(SERVER), "--no-env"],
            cwd=temporary,
            env=environment,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        try:
            initialized = rpc(
                process,
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "initialize",
                    "params": {
                        "protocolVersion": "2025-06-18",
                        "capabilities": {},
                        "clientInfo": {"name": "test-dymny", "version": "1.0.0"},
                    },
                },
            )
            assert process.stdin is not None
            process.stdin.write(
                json.dumps({"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}}) + "\n"
            )
            process.stdin.flush()
            tools = rpc(process, {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}})
            doctor = rpc(
                process,
                {
                    "jsonrpc": "2.0",
                    "id": 3,
                    "method": "tools/call",
                    "params": {"name": "dzienniczek", "arguments": {"polecenie": "doctor"}},
                },
            )
            capabilities = rpc(
                process,
                {
                    "jsonrpc": "2.0",
                    "id": 4,
                    "method": "tools/call",
                    "params": {"name": "dzienniczek", "arguments": {"polecenie": "capabilities"}},
                },
            )
            blocked = rpc(
                process,
                {
                    "jsonrpc": "2.0",
                    "id": 5,
                    "method": "tools/call",
                    "params": {"name": "dzienniczek", "arguments": {"polecenie": "logout"}},
                },
            )

            assert initialized["result"]["protocolVersion"] == "2025-06-18"
            assert [tool["name"] for tool in tools["result"]["tools"]] == ["dzienniczek"]
            assert doctor["result"].get("isError", False) is False
            assert doctor["result"]["structuredContent"]["runtime"]["javaSupported"] is True
            advertised_commands = tools["result"]["tools"][0]["inputSchema"]["properties"]["polecenie"]["enum"]
            runtime_commands = capabilities["result"]["structuredContent"]["mcpReadOnlyCommands"]
            assert advertised_commands == runtime_commands
            assert "logout" not in runtime_commands
            assert "accounts" not in runtime_commands
            assert blocked["result"]["isError"] is True
            assert blocked["result"]["structuredContent"]["code"] == 2
        finally:
            process.terminate()
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)

    print("MCP: inicjalizacja, lista narzędzi, odczyt, spójność możliwości i blokada mutacji działają poprawnie.")


if __name__ == "__main__":
    main()

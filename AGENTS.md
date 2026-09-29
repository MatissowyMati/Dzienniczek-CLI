# Praca nad Dzienniczek CLI

Projekt jest aplikacją Kotlin/JVM 17. Główne polecenia znajdują się w `cli/src/main/kotlin/io/github/matissowymati/dzienniczek/cli`, protokoły dostawców w `api`, a testy regresyjne w `cli/src/test`.

- Zachowuj kontrakt JSON, kody zakończenia i rozdział stdout/stderr. Widoki terminalowe są w `HumanOutput.kt`; JSON i MCP nie korzystają z tej prezentacji.
- Komunikaty i dokumentację pisz po polsku. Nazwy poleceń i opcji pozostają po angielsku.
- Sprawdzaj długie treści, puste wyniki i wąski terminal. Nie ucinaj informacji potrzebnych użytkownikowi.
- Testy jednostkowe i MCP używają danych syntetycznych oraz izolowanej konfiguracji. Testy na aktywnym profilu uruchamiaj wyłącznie na żądanie użytkownika: `python3 scripts/test-live.py --live`. Odczyt jednej wiadomości wymaga dodatkowo `--messages`.
- Nigdy nie dołączaj `.env`, konfiguracji profilu, danych ucznia ani surowych śladów dostawcy do Git, PR lub raportu. Nie wyświetlaj zawartości plików uwierzytelniających.
- Operacje mutujące wymagają jawnej intencji użytkownika. Testowanie nie obejmuje odwoływania urządzenia, wylogowania ani modyfikowania wiadomości i powiadomień.
- Przy pytaniach o dane szkolne użyj umiejętności `.agents/skills/dzienniczek/SKILL.md` i narzędzia MCP `dzienniczek`. Zacznij od `doctor`.

Weryfikacja zmian:

```sh
./gradlew :cli:test :cli:installDist :cli:distTar
python3 scripts/test-mcp.py
sh -n scripts/install.sh scripts/uninstall.sh scripts/dzienniczek-launcher.sh scripts/dzienniczek-mcp.sh
```

Na macOS z nieodpowiednią systemową Javą ustaw `JAVA_HOME=/opt/homebrew/opt/openjdk@17` i dodaj `$JAVA_HOME/bin` do `PATH`. Konfigurację Codex sprawdza `python3 scripts/test-mcp.py --codex` (wymaga zainstalowanego Codex CLI i zaufanego projektu).

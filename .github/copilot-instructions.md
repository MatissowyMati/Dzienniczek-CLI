# Instrukcje dla współtwórców Dzienniczek CLI

- To samodzielne CLI Kotlin/JVM. Repozytorium nie zawiera modułów Android, iOS ani Compose.
- Kod CLI i integracje z dostawcami znajdują się w `cli/src/main/kotlin`.
- Przed wysłaniem zmian uruchom `./gradlew :cli:test :cli:installDist` oraz `python3 scripts/test-mcp.py`.
- Zachowuj stabilne pola JSON i udokumentowane kody zakończenia.
- Nigdy nie wyświetlaj haseł, JWT, tokenów rejestracyjnych, kluczy prywatnych ani zawartości `.env`.
- Nigdy nie dodawaj do Git `.env` ani lokalnej konfiguracji profilu.
- Interaktywne pytania muszą być opcjonalne; automatyzacja ma działać przez zmienne środowiskowe i jawne flagi.
- Kanał stdout serwera MCP jest zarezerwowany wyłącznie dla JSON-RPC. Diagnostyka może trafiać tylko na stderr.

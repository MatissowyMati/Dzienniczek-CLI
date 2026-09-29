# Weryfikacja Dzienniczek CLI — 29 września 2026

Testy wykonano na macOS arm64 z Javą 17, na aktualnym `main` (`1c5d5d6`) z poprawkami tej gałęzi. Użyto istniejącego, aktywnego profilu eduVULCAN. Raport nie zawiera danych ucznia, treści wiadomości ani danych uwierzytelniających.

## Znaleziony problem i wynik poprawki

Zapisany profil oznaczał semestr poprzedniego roku jako `Current`, mimo że zawierał już okres obejmujący wrzesień 2026. `grades`, `presence months` i `dashboard` zwracały błąd API 213: wybrany okres nie należał do bieżącego roku szkolnego.

Po poprawce wybór domyślny uwzględnia daty okresu. Numer semestru wybiera najnowszy dostępny rok, a jawny identyfikator zachowuje dokładny wybór. Oceny, średnie, statystyki frekwencji i dashboard przeszły testy bez ponownego logowania i zmiany profilu.

## Wyniki

| Sprawdzenie | Wynik |
| --- | --- |
| Testy jednostkowe i regresyjne | 29 testów, 0 błędów, 0 pominięć |
| Budowanie dystrybucji JVM i archiwum | `test`, `installDist`, `distTar` zakończone poprawnie |
| Odczyty CLI na aktywnym profilu | 21/21 przepływów poprawnych |
| Widoki rzeczywistych danych | 7 widoków × 3 szerokości (40, 80, 100), bez przekroczenia szerokości |
| Kontrola wizualna na danych syntetycznych | Plan z zastępstwem, widok tabelaryczny i pionowy, zawijanie treści |
| MCP z izolowaną konfiguracją | Inicjalizacja, narzędzie, `doctor`, zgodność capabilities i blokada mutacji poprawne |
| Konfiguracja wykryta przez Codex CLI | Serwer włączony; rzeczywisty start i test MCP z podkatalogu poprawny |
| MCP na aktywnym profilu | `doctor`, `grades`, `schedule`, `dashboard` poprawne |
| Pomoc i błędne argumenty | `--help` także przy login/profile; literówki i błędne formaty zwracają kod 2 |
| Skrypty powłoki i diff | `sh -n` i `git diff --check` poprawne |

Odczyty CLI obejmowały `doctor`, `capabilities`, `periods`, oceny, średnie, podsumowania ocen, plan, zmiany planu, sprawdziany, zadania, frekwencję i obie statystyki, uwagi, ogłoszenia, szczęśliwy numerek, godziny lekcji, nauczycieli, informacje o szkole, dashboard oraz listę jednej odebranej wiadomości. Puste odpowiedzi niektórych sekcji są poprawnym wynikiem dla badanego profilu.

Sprawdzono wybór okresu na granicy roku szkolnego, zachowanie jawnego ID, jednostronne zakresy dat, długie treści, brak wyników, zastępstwa, usuwanie sekwencji sterujących terminalem, przekazywanie ustawień startowych MCP i odrzucanie błędnych typów argumentów.

## Poprawki po review Codexa

Widok `schedule-extra` ma osobny układ, który zachowuje wszystkie trzy opisy, nauczyciela oraz dane jego zastępstwa. Tabela okresów korzysta z wyznaczonego przez CLI pola `Current`, również podczas przerw pomiędzy okresami. Plan lekcji jest sortowany według numeru lekcji po uwzględnieniu zastępstwa.

Cztery nowe testy odtwarzają te przypadki na danych syntetycznych. Wszystkie cztery wykazały błędy na poprzednim rendererze i przechodzą po poprawce; pełny zestaw obejmuje 29 testów. Sprawdzono widoki tabelaryczne i pionowe, szerokości 40–140, zapasowy wybór okresu oraz lekcje przeniesione wcześniej i później. Poprawki dotyczą prezentacji; wyniki wcześniejszych testów live powyżej opisują pierwotny zakres weryfikacji PR-a.

## Jak powtórzyć

```sh
./gradlew :cli:test :cli:installDist :cli:distTar
python3 scripts/test-mcp.py
python3 scripts/test-mcp.py --codex
python3 scripts/test-live.py --live --messages
```

Test Codex wymaga zaufanego projektu i zainstalowanego Codex CLI. Test live wymaga istniejącego profilu; `--messages` włącza dodatkowy odczyt jednej wiadomości. Skrypt raportuje wyłącznie statusy, liczby rekordów i czas, również przy błędzie dostawcy.

## Zakres potwierdzenia

Testy live potwierdzają działanie na dostępnym profilu eduVULCAN. Nie testowano logowania do Librusa i VULCAN ani operacji zmieniających stan. Te przepływy wymagają odpowiednich kont lub osobnego, jawnie określonego testu. Testy jednostkowe i MCP są niezależne od prawdziwego konta i pozostają częścią CI.

# Dzienniczek CLI

[![CLI](https://github.com/MatissowyMati/Dzienniczek-CLI/actions/workflows/cli.yml/badge.svg)](https://github.com/MatissowyMati/Dzienniczek-CLI/actions/workflows/cli.yml)
[![Licencja: MIT](https://img.shields.io/badge/licencja-MIT-blue.svg)](LICENSE)

Samodzielny klient wiersza poleceń do polskich dzienników elektronicznych. Obsługuje VULCAN, eduVULCAN i Librus w systemach Linux, macOS oraz Windows przez WSL. Wyniki są czytelne zarówno dla człowieka, jak i dla agentów uruchamianych w terminalu, m.in. OpenAI Codex i Claude Code.

Implementacja protokołów powstała na podstawie projektu [szponciciel04/DzienniczekSzpontniczek](https://github.com/szponciciel04/DzienniczekSzpontniczek), a obsługa Librusa korzysta z kodu projektu [Szkolny.eu](https://github.com/szkolny-eu/szkolny-android).

## Możliwości

- rejestracja VULCAN za pomocą tokenu i PIN-u;
- logowanie nazwą użytkownika i hasłem do eduVULCAN, również z obsługą captchy z dowodem pracy;
- ręczna rejestracja eduVULCAN za pomocą JWT;
- logowanie przez Portal Librus i wybór powiązanego konta Synergia;
- wiele lokalnych profili, uczniów i okresów szkolnych;
- oceny, średnie, podsumowania, plan lekcji, zastępstwa, sprawdziany i zadania domowe;
- frekwencja ze statystykami miesięcznymi i przedmiotowymi;
- uwagi, ogłoszenia, wiadomości, nauczyciele, informacje o szkole, wycieczki, wydarzenia i dni wolne;
- jadłospisy, zebrania, dyżury, dane przedszkolne i szczęśliwy numerek;
- stabilny JSON i kody zakończenia przeznaczone do automatyzacji;
- wczytywanie `.env` bez umieszczania sekretów w argumentach procesu;
- lokalny serwer MCP przez stdio oraz gotowe umiejętności (skills) dla Codex i Claude Code.

## Wymagania

- Java 17 lub nowsza;
- macOS, Linux albo WSL.

## Instalacja

### macOS

```sh
brew install openjdk@17
git clone https://github.com/MatissowyMati/Dzienniczek-CLI.git
cd Dzienniczek-CLI
./scripts/install.sh
```

### Linux lub WSL

```sh
sudo apt-get update
sudo apt-get install -y openjdk-17-jre
git clone https://github.com/MatissowyMati/Dzienniczek-CLI.git
cd Dzienniczek-CLI
./scripts/install.sh
```

Instalator zapisuje aplikację w `~/.local/lib`, a skrypt uruchamiający w `~/.local/bin`. Upewnij się, że `~/.local/bin` znajduje się w zmiennej `PATH`.

Budowanie bez instalacji:

```sh
./gradlew :cli:installDist
./cli/build/install/dzienniczek/bin/dzienniczek version
```

## Konfiguracja logowania przez `.env`

Skopiuj bezpieczny szablon:

```sh
cp .env.example .env
chmod 600 .env
```

Przykład dla eduVULCAN:

```dotenv
DZIENNICZEK_PROVIDER=eduvulcan
DZIENNICZEK_USERNAME=twoj-login
DZIENNICZEK_PASSWORD=twoje-haslo
DZIENNICZEK_PROFILE=domyslny
```

Następnie sprawdź konfigurację i zaloguj się bez przekazywania sekretów w argumentach:

```sh
dzienniczek env --json
dzienniczek login
```

Plik `.env` jest ignorowany przez Git. Zmienne środowiskowe procesu mają pierwszeństwo przed wartościami z pliku. Użyj `--env-file SCIEZKA`, aby wskazać inny plik, albo `--no-env`, aby wyłączyć jego wczytywanie.

Obsługiwane są też starsze klucze eduVULCAN: `EDUVULCAN_LOGIN`, `EDUVULCAN_PASSWORD` i `EDUVULCAN_PASSWRD`. Błędnie zapisany alias `PASSWRD` pozostaje wyłącznie dla zgodności wstecznej.

Pozostałe sposoby logowania:

```sh
dzienniczek login vulcan --token TOKEN --pin PIN --symbol SZKOLA
dzienniczek login jwt --tenant TENANT --token JWT
dzienniczek login librus --username EMAIL --password HASLO
```

## Szybki start

Sprawdź instalację i lokalną konfigurację bez łączenia się z usługą szkoły:

```sh
dzienniczek doctor --format table
dzienniczek env --format table
```

Zaloguj się raz, a potem wybierz ucznia:

```sh
dzienniczek login --non-interactive
dzienniczek account list
dzienniczek dashboard
```

## Codzienne użycie

```sh
dzienniczek dashboard
dzienniczek grades
dzienniczek grades averages
dzienniczek schedule --from 2026-09-01 --to 2026-09-07
dzienniczek exams
dzienniczek homework
dzienniczek presence months
dzienniczek messages received
dzienniczek notes
dzienniczek announcements
```

Profile i uczniowie:

```sh
dzienniczek profile list
dzienniczek profile use NAZWA
dzienniczek account list
dzienniczek account use INDEKS
```

Pełny opis znajduje się w [dokumentacji poleceń](docs/cli.md). Możesz też uruchomić:

```sh
dzienniczek help
dzienniczek capabilities --json
```

## MCP dla Codex i Claude Code

Repozytorium zawiera gotowe konfiguracje:

- [`.codex/config.toml`](.codex/config.toml) dla OpenAI Codex;
- [`.mcp.json`](.mcp.json) dla Claude Code;
- [`.agents/skills/dzienniczek/SKILL.md`](.agents/skills/dzienniczek/SKILL.md) jako umiejętność repozytorium dla Codex;
- [`.claude/skills/dzienniczek/SKILL.md`](.claude/skills/dzienniczek/SKILL.md) jako umiejętność projektu dla Claude Code.

Po otwarciu repozytorium klient powinien wykryć serwer `dzienniczek`. Claude Code poprosi o zatwierdzenie projektu i serwera z `.mcp.json`. Skrypt startowy automatycznie zbuduje lokalną dystrybucję, jeśli jej brakuje lub kod źródłowy jest nowszy.

Serwer można też uruchomić ręcznie:

```sh
dzienniczek mcp
# albo bez wcześniejszej instalacji:
./scripts/dzienniczek-mcp.sh
```

MCP udostępnia jedno narzędzie `dzienniczek` ze ściśle określonym zestawem operacji tylko do odczytu. Celowo blokuje logowanie, wylogowanie, zmianę profilu lub konta, ustawienia push, modyfikacje wiadomości, zwracanie tokenu automatycznego logowania i usuwanie danych uwierzytelniających. Takie czynności trzeba wykonać bezpośrednio w CLI z wyraźną intencją użytkownika.

Test dymny MCP:

```sh
./gradlew :cli:installDist
python3 scripts/test-mcp.py
```

Test sprawdza negocjację wersji protokołu, listę narzędzi, bezpieczny odczyt `doctor` i blokadę polecenia mutującego. Korzysta z tymczasowego, pustego katalogu konfiguracji i nie dotyka prawdziwego profilu.

## Agenci AI i automatyzacja

Gdy MCP nie jest dostępne, używaj następującego wzorca w Codex, Claude Code, skryptach i CI:

```sh
dzienniczek POLECENIE --json --compact --non-interactive
```

Integrację zacznij od przeczytania pliku [`docs/AI_USAGE.txt`](docs/AI_USAGE.txt). Zawiera zasady bezpieczeństwa, kontrakt wyjścia, wykrywanie poleceń w czasie działania i przykłady. Agent powinien najpierw sprawdzić:

```sh
dzienniczek doctor --json --compact --non-interactive
dzienniczek capabilities --json --compact --non-interactive
```

Opcja `--non-interactive` gwarantuje, że brakujące dane spowodują błąd zamiast oczekiwania na wpis z terminala. Przy przekierowanym wyjściu domyślnym formatem jest JSON, ale w automatyzacji warto żądać go jawnie. Poprawny wynik trafia na stdout, a błędy na stderr.

Kody zakończenia:

| Kod | Znaczenie |
| ---: | --- |
| 0 | Sukces |
| 2 | Nieprawidłowe polecenie lub argumenty |
| 3 | Błąd uwierzytelniania |
| 4 | Błąd sieci lub przekroczenie limitu czasu |
| 5 | Błąd zdalnego API |
| 6 | Błąd lokalnej konfiguracji |
| 10 | Nieoczekiwany błąd wewnętrzny |

W trybie nieinteraktywnym błędy są zapisywane jako JSON na stderr. Sekrety nie są częścią zwykłego wyjścia.

## Rozwój i testy

Pełna lokalna weryfikacja:

```sh
./gradlew :cli:test :cli:installDist :cli:distTar
python3 scripts/test-mcp.py
sh -n scripts/install.sh scripts/uninstall.sh scripts/dzienniczek-launcher.sh scripts/dzienniczek-mcp.sh
```

Jeśli systemowe `java` jest starsze, ale OpenJDK 17 z Homebrew jest zainstalowane, uruchom Gradle tak:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 \
PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
./gradlew :cli:test
```

CI wykonuje budowanie i testy na Ubuntu oraz macOS.

## Rozwiązywanie problemów

```sh
dzienniczek doctor --json
dzienniczek env --json
dzienniczek profile show --json
```

Dodaj `--debug` wyłącznie podczas lokalnej diagnostyki; ślady mogą zawierać metadane dostawcy. Na wolnym połączeniu użyj `--timeout 60`. Jeśli wybrano niewłaściwego ucznia lub okres, skorzystaj z `account list`, `account use INDEKS` oraz `--period WARTOSC`.

## Bezpieczeństwo

- Nigdy nie zatwierdzaj w Git pliku `.env` ani wyeksportowanej konfiguracji profilu.
- Preferuj `.env` lub wstrzyknięte zmienne środowiskowe zamiast haseł w argumentach procesu.
- Profile są zapisywane w `${XDG_CONFIG_HOME:-~/.config}/dzienniczek/config.json` z uprawnieniami tylko dla właściciela, jeśli system plików je obsługuje.
- `dzienniczek logout --yes` usuwa zapisany profil lokalny; nie odwołuje urządzenia po stronie dostawcy.

Zasady zgłaszania problemów opisuje [SECURITY.md](SECURITY.md).

## Dokumentacja

- [Pełna dokumentacja poleceń](docs/cli.md)
- [Przewodnik dla agentów AI i automatyzacji](docs/AI_USAGE.txt)
- [Polityka bezpieczeństwa](SECURITY.md)

## Licencja i autorstwo

Projekt jest udostępniany na licencji MIT. Zobacz [LICENSE](LICENSE) i [NOTICE.md](NOTICE.md).

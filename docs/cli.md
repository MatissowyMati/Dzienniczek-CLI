# Dokumentacja CLI

Nazwy poleceń i opcji pozostają po angielsku, aby zachować stabilny interfejs skryptowy. Opisy i komunikaty są po polsku.

## Opcje globalne

| Opcja | Działanie |
| --- | --- |
| `--format json\|table\|plain` | Wybiera format wyjścia |
| `--json` | Skrót wybierający JSON |
| `--compact` | Zapisuje JSON bez zbędnych odstępów |
| `--profile NAZWA` | Wybiera zapisany profil |
| `--account WARTOSC` | Wybiera indeks konta, identyfikator ucznia lub pasującą nazwę |
| `--period WARTOSC` | Wybiera identyfikator albo numer okresu |
| `--from DATA` | Ustawia włącznie początek zakresu w formacie `YYYY-MM-DD` |
| `--to DATA` | Ustawia włącznie koniec zakresu w formacie `YYYY-MM-DD` |
| `--config SCIEZKA` | Zastępuje ścieżkę konfiguracji profili |
| `--env-file SCIEZKA` | Wczytuje dane logowania z innego pliku dotenv |
| `--no-env` | Wyłącza wczytywanie dotenv |
| `--non-interactive` | Nie wyświetla pytań; przy braku danych zwraca błąd |
| `--timeout SEKUNDY` | Ustawia dodatni limit czasu sieci; domyślnie 30 sekund |
| `--debug` | Dołącza ślad diagnostyczny do błędu JSON |

Domyślny zakres dat obejmuje bieżący tydzień od poniedziałku do niedzieli. `--page-size` musi być dodatnie. `--last-id` nie może być ujemne. Brak wartości po opcji, np. samo `--timeout`, jest błędem użycia.

## Zmienne środowiskowe

| Zmienna | Przeznaczenie |
| --- | --- |
| `DZIENNICZEK_PROVIDER` | `eduvulcan`, `vulcan`, `jwt` albo `librus` |
| `DZIENNICZEK_USERNAME` | Nazwa użytkownika eduVULCAN lub Librus |
| `DZIENNICZEK_PASSWORD` | Hasło eduVULCAN lub Librus |
| `DZIENNICZEK_TENANT` | Tenant eduVULCAN, gdy wybór nie jest jednoznaczny |
| `DZIENNICZEK_TOKEN` | Token rejestracyjny VULCAN |
| `DZIENNICZEK_PIN` | PIN rejestracyjny VULCAN |
| `DZIENNICZEK_SYMBOL` | Symbol szkoły VULCAN |
| `DZIENNICZEK_JWT` | Co najmniej jeden JWT rozdzielony przecinkami |
| `DZIENNICZEK_PROFILE` | Domyślna nazwa profilu |
| `DZIENNICZEK_CONFIG` | Ścieżka konfiguracji profili |
| `DZIENNICZEK_ENV_FILE` | Ścieżka pliku dotenv |

Zmienne procesu mają pierwszeństwo przed `.env`. Puste wartości są traktowane jak brakujące.

Dostępność danych można sprawdzić bez wyświetlania ich wartości:

```sh
dzienniczek env --json
```

Gotowość Javy, danych logowania i aktywnego profilu sprawdza:

```sh
dzienniczek doctor --json
```

`doctor` działa lokalnie i nie loguje się ani nie łączy z dostawcą. Pole `ok` ma wartość `true`, gdy działa Java 17+ oraz istnieje użyteczny profil lub kompletne środowisko logowania.

## Uwierzytelnianie

```sh
# Dostawca i dane z .env
dzienniczek login

# Jawny dostawca; dane nadal mogą pochodzić z .env
dzienniczek login eduvulcan
dzienniczek login vulcan
dzienniczek login jwt
dzienniczek login librus
```

Opcja `--profile NAZWA` pozwala zachować kilka logowań. `--no-store-password` nie zapisuje hasła używanego do wiadomości eduVULCAN w profilu; przy odczycie wiadomości hasło musi być wtedy nadal dostępne w środowisku.

## Polecenia danych

| Polecenie | Opis |
| --- | --- |
| `dashboard` | Oceny, średnie, nadchodzące obowiązki i szczęśliwy numerek |
| `accounts` | Surowa lista kont dostawcy |
| `periods` | Okresy szkolne |
| `grades [list\|averages\|summary]` | Oceny, średnie lub podsumowania |
| `schedule` / `timetable` | Plan lekcji ze zmianami |
| `schedule-extra` | Dodatkowe zmiany planu |
| `exams` | Sprawdziany i kartkówki |
| `homework` | Zadania domowe |
| `completed-lessons` | Zrealizowane lekcje |
| `planned-lessons` | Zaplanowane lekcje |
| `presence` | Wpisy frekwencji |
| `presence months` | Statystyki miesięczne frekwencji |
| `presence subjects` | Statystyki frekwencji według przedmiotów |
| `presence info` | Szczegóły wpisu frekwencji |
| `notes` | Uwagi ucznia |
| `announcements` | Ogłoszenia |
| `messages received\|sent\|deleted` | Foldery wiadomości |
| `message --id ID` | Treść wiadomości |
| `teachers` | Nauczyciele albo użytkownicy Librus |
| `school-info` | Informacje o szkole |
| `trips` | Wycieczki szkolne |
| `events` | Wydarzenia użytkownika |
| `vacations` | Dni wolne i ferie |
| `meetings` | Zebrania z rodzicami |
| `meal-menu` | Jadłospis |
| `duties` | Dyżury szkolne |
| `lucky-number` | Szczęśliwy numerek |
| `addressbook` | Książka adresowa wiadomości |
| `timeslots` | Godziny lekcyjne |
| `kindergarten-hours` | Godziny przedszkolne |
| `kindergarten-teachers` | Nauczyciele przedszkolni |

Profile Librus obsługują dodatkowo `subjects`, `classrooms`, `notices`, podpolecenia kategorii i `auto-login-token`. Ostatnie polecenie zwraca sekret i nie jest dostępne przez MCP.

## Zarządzanie profilami

```sh
dzienniczek profile list
dzienniczek profile show
dzienniczek profile use NAZWA
dzienniczek profile remove NAZWA --yes
dzienniczek account list
dzienniczek account use INDEKS
dzienniczek logout --yes
dzienniczek logout --all --yes
```

## Polecenia mutujące

```sh
dzienniczek messages importance --id ID --important true
dzienniczek messages status --id ID --status NUMER
dzienniczek push locale --locale pl-PL
dzienniczek push all --enabled true
dzienniczek push set --option NAZWA --enabled true
dzienniczek push configure --option NAZWA=true --locale pl-PL
dzienniczek credential delete --yes
```

Zdalne usunięcie danych uwierzytelniających jest nieodwracalne. Usunięcie profilu lokalnego nie odwołuje zarejestrowanego urządzenia; wcześniej trzeba jawnie wykonać `credential delete --yes`.

## Serwer MCP

Serwer stdio uruchamia:

```sh
dzienniczek mcp
```

Repozytorium zawiera konfigurację dla Codex w `.codex/config.toml` i dla Claude Code w `.mcp.json`. Obie korzystają z `scripts/dzienniczek-mcp.sh`, który wybiera Javę 17 i w razie potrzeby buduje lokalną dystrybucję.

Narzędzie MCP nazywa się `dzienniczek`. Przyjmuje pole `polecenie` i opcjonalne, typowane pola: `profil`, `konto`, `okres`, `od`, `do`, `dzien`, `tydzien`, `identyfikator`, `skrzynka`, `weakRefId`, `typ`, `rozmiarStrony`, `ostatnieId`, `limitCzasu`, `skrot`, `hebe` oraz `api`.

Lista `polecenie` jest zamknięta i obejmuje wyłącznie odczyt. Serwer nie przyjmuje dowolnych argumentów powłoki, nie uruchamia poleceń przez powłokę i blokuje wszystkie znane operacje mutujące oraz sekrety. Wynik zawiera tekstowy JSON dla zgodności z klientami oraz `structuredContent`. Dokładną listę zwraca pole `mcpReadOnlyCommands` polecenia `capabilities`.

Test protokołu:

```sh
./gradlew :cli:installDist
python3 scripts/test-mcp.py
```

## Kontrakt wyjścia

- `--json` zapisuje jedną poprawną wartość JSON na stdout.
- `--compact` zmienia wyłącznie białe znaki.
- Błędy trafiają na stderr jako `{ "ok": false, "error": "...", "code": N }` w trybie JSON.
- Błędy czytelne dla człowieka trafiają na stderr, a poprawne tabele na stdout.
- Daty w opcjach zakresu mają format `YYYY-MM-DD`.
- Wyjście używa UTF-8. Kolejne wersje mogą dodawać pola; odbiorca powinien ignorować nieznane.
- Kod zakończenia `0` oznacza ukończenie polecenia. `doctor` ma dodatkowe pole `ok` opisujące gotowość środowiska.
- W trybie MCP stdout jest zarezerwowany wyłącznie dla komunikatów JSON-RPC.

Zasady bezpiecznej automatyzacji opisuje [AI_USAGE.txt](AI_USAGE.txt).

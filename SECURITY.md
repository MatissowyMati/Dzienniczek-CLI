# Polityka bezpieczeństwa

## Zgłaszanie problemów

Nie twórz publicznego zgłoszenia zawierającego dane logowania, tokeny, klucze prywatne, dane szkolne ani sposób odtworzenia dostępu do konta.

Problemy bezpieczeństwa zgłaszaj prywatnie przez GitHub Security Advisories tego repozytorium. Podaj wersję, wpływ problemu i minimalny przykład z usuniętymi sekretami.

## Sekrety

- Pliki `.env` są ignorowane i muszą pozostać lokalne.
- Nigdy nie dołączaj do zgłoszeń plików profilu z `~/.config/dzienniczek`.
- Po ujawnieniu danych logowania natychmiast je zmień lub odwołaj.
- Do badania integracji z dostawcą używaj wydzielonego konta testowego.
- Serwer MCP udostępnia wyłącznie polecenia tylko do odczytu i nie zwraca tokenu automatycznego logowania Librus.

Poprawki bezpieczeństwa otrzymuje wyłącznie najnowsza wydana wersja.

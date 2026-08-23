---
name: dzienniczek
description: Odczytuje oceny, plan lekcji, sprawdziany, zadania, frekwencję, wiadomości i inne dane z VULCAN, eduVULCAN lub Librus przez Dzienniczek CLI. Używaj, gdy użytkownik pyta o dane z dziennika szkolnego; nie używaj do logowania ani zmian bez wyraźnej zgody.
---

# Dzienniczek CLI

Preferuj narzędzie MCP `dzienniczek`, ponieważ ma zamknięty zestaw poleceń tylko do odczytu i zwraca dane strukturalne.

Przed pierwszym odczytem w sesji wywołaj `doctor`. Jeśli profil nie jest gotowy, opisz użytkownikowi wymagany krok, ale nie odczytuj pliku `.env` ani pliku konfiguracji profilu. Logowanie wykonuj wyłącznie na wyraźne polecenie użytkownika, bez umieszczania hasła, PIN-u, JWT ani tokenu w rozmowie.

Dobierz najwęższe polecenie do pytania. Dla zakresów czasu ustaw `od` i `do` w formacie `YYYY-MM-DD`. Przy wielu profilach lub uczniach użyj `profil` i `konto`. Nie pobieraj wiadomości ani danych innych uczniów, jeśli nie są potrzebne do odpowiedzi.

Jeśli MCP nie jest dostępne, użyj lokalnego CLI w postaci:

```sh
dzienniczek POLECENIE --json --compact --non-interactive
```

Najpierw sprawdź `dzienniczek capabilities --json --compact --non-interactive`. Nie uruchamiaj poleceń mutujących (`login`, `logout`, `profile use/remove`, `account use`, zmian wiadomości/push ani `credential delete`) bez jednoznacznej intencji użytkownika. `credential delete --yes` jest nieodwracalne.

Traktuj dane ucznia jako prywatne: pokazuj tylko zakres niezbędny do odpowiedzi i nigdy nie ujawniaj sekretów, surowych śladów debugowania ani zawartości lokalnych plików uwierzytelniających.

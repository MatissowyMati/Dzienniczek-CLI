# Dzienniczek CLI

[![CLI](https://github.com/MatissowyMati/Dzienniczek-CLI/actions/workflows/cli.yml/badge.svg)](https://github.com/MatissowyMati/Dzienniczek-CLI/actions/workflows/cli.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A standalone command-line client for Polish electronic school registers. It supports VULCAN, eduVULCAN, and Librus from Linux, macOS, and Windows through WSL. Output works equally well for people and shell-based agents such as Codex, Claude Code, OpenClaw, and Hermes.

This is an independent CLI project. Its protocol implementation was initially derived from [szponciciel04/DzienniczekSzpontniczek](https://github.com/szponciciel04/DzienniczekSzpontniczek); that project is not bundled and remains a separate mobile application.

## Features

- VULCAN token/PIN registration
- eduVULCAN username/password login with proof-of-work captcha support
- manual eduVULCAN JWT registration
- Librus Portal login and linked Synergia account selection
- multiple local profiles, pupils, and school periods
- grades, averages, summaries, timetable, substitutions, exams, and homework
- attendance with monthly and subject statistics
- notes, announcements, messages, teachers, school information, trips, events, and vacations
- meal menus, meetings, duties, kindergarten data, and lucky numbers
- stable JSON output and exit codes for automation
- `.env` loading without putting secrets on the command line

## Requirements

- Java 17 or newer
- macOS, Linux, or WSL

## Installation

### macOS

```sh
brew install openjdk@17
git clone https://github.com/MatissowyMati/Dzienniczek-CLI.git
cd Dzienniczek-CLI
./scripts/install.sh
```

### Linux or WSL

```sh
sudo apt-get update
sudo apt-get install -y openjdk-17-jre
git clone https://github.com/MatissowyMati/Dzienniczek-CLI.git
cd Dzienniczek-CLI
./scripts/install.sh
```

The installer places the application under `~/.local/lib` and its launcher in `~/.local/bin`. On Homebrew systems it also works through `/opt/homebrew/bin/dzienniczek` when linked there.

Build without installing:

```sh
./gradlew :cli:installDist
./cli/build/install/dzienniczek/bin/dzienniczek version
```

## Login from `.env`

Copy the safe template:

```sh
cp .env.example .env
chmod 600 .env
```

For eduVULCAN:

```dotenv
DZIENNICZEK_PROVIDER=eduvulcan
DZIENNICZEK_USERNAME=your-login
DZIENNICZEK_PASSWORD=your-password
DZIENNICZEK_PROFILE=default
```

Then log in without passing secrets as arguments:

```sh
dzienniczek env --json
dzienniczek login
```

`.env` is ignored by Git. External environment variables take precedence over values in the file. Use `--env-file PATH` for another file or `--no-env` to disable loading.

Legacy eduVULCAN keys `EDUVULCAN_LOGIN`, `EDUVULCAN_PASSWORD`, and `EDUVULCAN_PASSWRD` are also accepted. The misspelled `PASSWRD` alias exists for compatibility only.

Other login modes:

```sh
dzienniczek login vulcan --token TOKEN --pin PIN --symbol SCHOOL
dzienniczek login jwt --tenant TENANT --token JWT
dzienniczek login librus --username EMAIL --password PASSWORD
```

## Usage

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

Profiles and pupils:

```sh
dzienniczek profile list
dzienniczek profile use NAME
dzienniczek account list
dzienniczek account use INDEX
```

See [the complete CLI reference](docs/cli.md) or run:

```sh
dzienniczek help
dzienniczek capabilities --json
```

## Agent use

Non-interactive output defaults to JSON. Explicit automation form:

```sh
dzienniczek grades --json --compact
```

Exit codes:

| Code | Meaning |
| ---: | --- |
| 0 | Success |
| 2 | Invalid command or arguments |
| 3 | Authentication failed |
| 4 | Network failure |
| 5 | Remote API failure |
| 6 | Local configuration failure |
| 10 | Internal failure |

Errors are emitted as JSON on stderr in non-interactive mode. Secrets are never included in normal output.

## Security

- Never commit `.env` or exported profile configuration.
- Prefer `.env` or injected environment variables over command-line password arguments.
- Stored profiles are written to `${XDG_CONFIG_HOME:-~/.config}/dzienniczek/config.json` with owner-only permissions where supported.
- Use `dzienniczek logout --yes` to remove a stored profile.

See [SECURITY.md](SECURITY.md) for reporting security problems.

## License and attribution

MIT licensed. See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md).

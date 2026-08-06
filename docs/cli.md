# CLI reference

## Global options

| Option | Purpose |
| --- | --- |
| `--format json\|table\|plain` | Select output format |
| `--json` | Shortcut for JSON output |
| `--compact` | Compact JSON |
| `--profile NAME` | Select a stored profile |
| `--account VALUE` | Select account index, pupil ID, or matching name |
| `--period VALUE` | Select period ID or number |
| `--from DATE` | Inclusive start date in `YYYY-MM-DD` format |
| `--to DATE` | Inclusive end date in `YYYY-MM-DD` format |
| `--config PATH` | Override profile configuration path |
| `--env-file PATH` | Load credentials from another dotenv file |
| `--no-env` | Disable dotenv loading |
| `--non-interactive` | Never prompt; fail if required input is missing |
| `--timeout SECONDS` | Set the positive network timeout; default is 30 seconds |
| `--debug` | Include diagnostic traces in JSON errors |

The default date range is Monday through Sunday of the current week.

## Environment variables

| Variable | Purpose |
| --- | --- |
| `DZIENNICZEK_PROVIDER` | `eduvulcan`, `vulcan`, `jwt`, or `librus` |
| `DZIENNICZEK_USERNAME` | eduVULCAN or Librus login |
| `DZIENNICZEK_PASSWORD` | eduVULCAN or Librus password |
| `DZIENNICZEK_TENANT` | eduVULCAN tenant when selection is ambiguous |
| `DZIENNICZEK_TOKEN` | VULCAN registration token |
| `DZIENNICZEK_PIN` | VULCAN registration PIN |
| `DZIENNICZEK_SYMBOL` | VULCAN school symbol |
| `DZIENNICZEK_JWT` | One or more comma-separated JWTs |
| `DZIENNICZEK_PROFILE` | Default profile name |
| `DZIENNICZEK_CONFIG` | Profile configuration path |
| `DZIENNICZEK_ENV_FILE` | Dotenv file path |

External variables override `.env`. Empty values are treated as missing.

Check availability without printing values:

```sh
dzienniczek env --json
```

Check whether Java, credentials, and the active profile are ready:

```sh
dzienniczek doctor --json
```

`doctor` is local-only and does not log in or contact a provider. Its `ok` field is true when Java 17+ is available and either a usable stored profile or a complete login environment exists.

## Authentication

```sh
# Provider and credentials from .env
dzienniczek login

# Explicit provider; credentials may still come from .env
dzienniczek login eduvulcan
dzienniczek login vulcan
dzienniczek login jwt
dzienniczek login librus
```

Add `--profile NAME` to retain multiple logins. `--no-store-password` keeps the eduVULCAN messaging password out of profile storage; it must then remain available through the environment when messages are requested.

## Data commands

| Command | Description |
| --- | --- |
| `dashboard` | Grades, averages, upcoming work, and lucky number |
| `accounts` | Raw provider accounts |
| `periods` | School periods |
| `grades [list\|averages\|summary]` | Grade data |
| `schedule` / `timetable` | Timetable with changes |
| `schedule-extra` | Additional schedule changes |
| `exams` | Exams and tests |
| `homework` | Homework assignments |
| `completed-lessons` | Completed lessons |
| `planned-lessons` | Planned lessons |
| `presence` | Attendance entries |
| `presence months` | Monthly statistics |
| `presence subjects` | Subject statistics |
| `presence info` | Detailed attendance entry |
| `notes` | Student notes |
| `announcements` | Announcements |
| `messages received\|sent\|deleted` | Message folders |
| `message --id ID` | Message content |
| `teachers` | Teachers or Librus users |
| `school-info` | School information |
| `trips` | School trips |
| `events` | User events |
| `vacations` | Holidays and free days |
| `meetings` | Parent meetings |
| `meal-menu` | Cafeteria menu |
| `duties` | School duties |
| `lucky-number` | Lucky number |
| `addressbook` | Message address book |
| `timeslots` | Lesson time slots |
| `kindergarten-hours` | Kindergarten hours |
| `kindergarten-teachers` | Kindergarten teachers |

Librus profiles additionally support `subjects`, `classrooms`, `notices`, category subcommands, and `auto-login-token`.

## Profile management

```sh
dzienniczek profile list
dzienniczek profile show
dzienniczek profile use NAME
dzienniczek profile remove NAME --yes
dzienniczek account list
dzienniczek account use INDEX
dzienniczek logout --yes
dzienniczek logout --all --yes
```

## Mutating commands

```sh
dzienniczek messages importance --id ID --important true
dzienniczek messages status --id ID --status NUMBER
dzienniczek push locale --locale pl-PL
dzienniczek push all --enabled true
dzienniczek push set --option NAME --enabled true
dzienniczek push configure --option NAME=true --locale pl-PL
dzienniczek credential delete --yes
```

Remote credential deletion cannot be undone. Local removal does not revoke the registered device unless `credential delete --yes` is used first.

## Output contract

- `--json` emits one valid JSON value to stdout.
- `--compact` changes whitespace only.
- Errors go to stderr as `{ "ok": false, "error": "...", "code": N }` in JSON mode.
- Human-readable errors go to stderr and successful tables go to stdout.
- Dates accepted by global range options use `YYYY-MM-DD`.
- Output is UTF-8. Fields may be added in compatible releases; consumers should ignore unknown fields.
- Exit code `0` means the command completed. The `doctor` command also has an `ok` field describing readiness.

For AI integrations and safe automation conventions, read [AI_USAGE.txt](AI_USAGE.txt).

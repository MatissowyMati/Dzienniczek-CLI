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

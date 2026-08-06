# Security policy

## Reporting

Do not open a public issue containing credentials, tokens, private keys, school data, or reproducible account access.

Report security problems privately through GitHub Security Advisories for this repository. Include the affected version, impact, and a minimal reproduction with all secrets removed.

## Secrets

- `.env` files are ignored and must remain local.
- Never attach profile files from `~/.config/dzienniczek` to issues.
- Rotate credentials immediately if they are exposed.
- Use a dedicated test account when investigating provider behavior.

Only the latest released version receives security fixes.

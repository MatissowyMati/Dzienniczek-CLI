# Dzienniczek CLI contributor instructions

- This is a standalone Kotlin/JVM CLI. There are no Android, iOS, or Compose modules.
- CLI code and provider integrations live under `cli/src/main/kotlin`.
- Run `./gradlew :cli:test :cli:installDist` before submitting changes.
- Preserve stable JSON fields and documented exit codes.
- Never print passwords, JWTs, registration tokens, private keys, or `.env` contents.
- Never add `.env` or local profile configuration to Git.
- Keep interactive prompts optional; automation must work non-interactively through environment variables and explicit flags.

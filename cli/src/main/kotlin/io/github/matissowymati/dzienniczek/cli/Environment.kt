package io.github.matissowymati.dzienniczek.cli

import java.nio.file.Files
import java.nio.file.Path

object Env {
    private var values: Map<String, String> = emptyMap()
    var loadedPath: Path? = null
        private set

    private val aliases = mapOf(
        "DZIENNICZEK_USERNAME" to listOf("EDUVULCAN_LOGIN", "EDUVULCAN_USERNAME", "LIBRUS_LOGIN"),
        "DZIENNICZEK_PASSWORD" to listOf("EDUVULCAN_PASSWORD", "EDUVULCAN_PASSWRD", "LIBRUS_PASSWORD"),
        "DZIENNICZEK_TOKEN" to listOf("VULCAN_TOKEN"),
        "DZIENNICZEK_PIN" to listOf("VULCAN_PIN"),
        "DZIENNICZEK_SYMBOL" to listOf("VULCAN_SYMBOL"),
        "DZIENNICZEK_TENANT" to listOf("EDUVULCAN_TENANT"),
    )

    fun load(raw: Array<String>) {
        values = emptyMap()
        loadedPath = null
        if (raw.any { it == "--no-env" }) return
        val explicit = raw.indexOf("--env-file").takeIf { it >= 0 }?.let { index ->
            raw.getOrNull(index + 1) ?: throw CliError("--env-file wymaga ścieżki", Exit.USAGE)
        } ?: raw.firstOrNull { it.startsWith("--env-file=") }?.substringAfter('=')
            ?: System.getenv("DZIENNICZEK_ENV_FILE")
        val path = Path.of(explicit ?: ".env").toAbsolutePath().normalize()
        if (!Files.exists(path)) {
            if (explicit != null) throw CliError("Nie znaleziono pliku środowiska: $path", Exit.CONFIG)
            return
        }
        if (!Files.isRegularFile(path)) throw CliError("Ścieżka środowiska nie wskazuje pliku: $path", Exit.CONFIG)
        values = Files.readAllLines(path).mapNotNull(::parseLine).toMap()
        loadedPath = path
    }

    fun get(name: String): String? {
        System.getenv(name)?.takeIf(String::isNotBlank)?.let { return it }
        values[name]?.takeIf(String::isNotBlank)?.let { return it }
        aliases[name].orEmpty().forEach { alias ->
            System.getenv(alias)?.takeIf(String::isNotBlank)?.let { return it }
            values[alias]?.takeIf(String::isNotBlank)?.let { return it }
        }
        return null
    }

    fun inferredProvider(): String? = get("DZIENNICZEK_PROVIDER")?.lowercase()
        ?: when {
            hasAny("EDUVULCAN_LOGIN", "EDUVULCAN_USERNAME") -> "eduvulcan"
            hasAny("LIBRUS_LOGIN") -> "librus"
            hasAny("VULCAN_TOKEN", "DZIENNICZEK_TOKEN") -> "vulcan"
            get("DZIENNICZEK_JWT") != null -> "jwt"
            else -> null
        }

    fun available(name: String): Boolean = get(name) != null

    private fun hasAny(vararg names: String): Boolean = names.any { name ->
        System.getenv(name)?.isNotBlank() == true || values[name]?.isNotBlank() == true
    }

    private fun parseLine(source: String): Pair<String, String>? {
        var line = source.trim()
        if (line.isEmpty() || line.startsWith('#')) return null
        if (line.startsWith("export ")) line = line.removePrefix("export ").trimStart()
        val separator = line.indexOf('=')
        if (separator <= 0) return null
        val key = line.substring(0, separator).trim()
        if (!key.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) return null
        var value = line.substring(separator + 1).trim()
        if (value.length >= 2 && value.first() == value.last() && value.first() in setOf('\'', '"')) {
            val quote = value.first()
            value = value.substring(1, value.length - 1)
            if (quote == '"') value = value.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
        } else {
            value = value.replace(Regex("\\s+#.*$"), "").trimEnd()
        }
        return key to value
    }
}

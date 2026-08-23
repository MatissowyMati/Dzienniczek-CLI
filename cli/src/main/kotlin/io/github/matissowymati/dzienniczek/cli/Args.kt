package io.github.matissowymati.dzienniczek.cli

class CliArgs(tokens: List<String>) {
    val words = mutableListOf<String>()
    private val values = linkedMapOf<String, MutableList<String>>()
    private val switches = linkedSetOf<String>()
    private val missingValues = linkedSetOf<String>()

    init {
        val booleanOptions = setOf(
            "json", "compact", "debug", "yes", "all", "brief", "hebe", "api",
            "no-store-password", "no-env", "non-interactive", "help"
        )
        var index = 0
        var options = true
        while (index < tokens.size) {
            val token = tokens[index]
            if (token == "--") {
                options = false
                index++
                continue
            }
            if (options && token.startsWith("--")) {
                val body = token.removePrefix("--")
                if ('=' in body) {
                    val (key, value) = body.split('=', limit = 2)
                    values.getOrPut(key) { mutableListOf() }.add(value)
                } else if (body !in booleanOptions) {
                    if (index + 1 < tokens.size && !tokens[index + 1].startsWith("--")) {
                        values.getOrPut(body) { mutableListOf() }.add(tokens[++index])
                    } else {
                        missingValues.add(body)
                    }
                } else {
                    switches.add(body)
                }
            } else {
                words.add(token)
            }
            index++
        }
    }

    fun value(name: String): String? {
        requireValueIfPresent(name)
        return values[name]?.lastOrNull()
    }

    fun values(name: String): List<String> {
        requireValueIfPresent(name)
        return values[name].orEmpty()
    }

    fun validate() {
        missingValues.firstOrNull()?.let { name ->
            throw CliError("--$name wymaga wartości", Exit.USAGE)
        }
    }

    fun suppliedValue(name: String): String? = values[name]?.lastOrNull()

    fun flag(name: String): Boolean = name in switches || value(name)?.lowercase() in setOf("true", "1", "yes", "on")
    fun required(name: String, env: String? = null, secret: Boolean = false): String {
        value(name)?.takeIf { it.isNotBlank() }?.let { return it }
        env?.let { Env.get(it)?.takeIf(String::isNotBlank)?.let { found -> return found } }
        val console = System.console().takeUnless { flag("non-interactive") }
        if (console != null) {
            val prompt = "${name.replace('-', ' ')}: "
            val entered = if (secret) console.readPassword(prompt)?.concatToString() else console.readLine(prompt)
            if (!entered.isNullOrBlank()) return entered
        }
        val hint = if (env == null) "--$name" else "--$name or $env"
        throw CliError("Brakuje $hint", Exit.USAGE)
    }

    fun int(name: String, default: Int): Int = value(name)?.toIntOrNull()
        ?: if (value(name) == null) default else throw CliError("--$name musi być liczbą całkowitą", Exit.USAGE)

    fun positiveInt(name: String, default: Int): Int = int(name, default).also {
        if (it <= 0) throw CliError("--$name musi być większe od zera", Exit.USAGE)
    }

    fun nonNegativeInt(name: String, default: Int): Int = int(name, default).also {
        if (it < 0) throw CliError("--$name musi być równe zero lub większe", Exit.USAGE)
    }

    private fun requireValueIfPresent(name: String) {
        if (name in missingValues) throw CliError("--$name wymaga wartości", Exit.USAGE)
    }
}

object Exit {
    const val OK = 0
    const val USAGE = 2
    const val AUTH = 3
    const val NETWORK = 4
    const val API = 5
    const val CONFIG = 6
    const val INTERNAL = 10
}

class CliError(message: String, val code: Int) : RuntimeException(message)

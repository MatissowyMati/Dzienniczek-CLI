package io.github.matissowymati.dzienniczek.cli

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.nio.file.Path
import java.util.concurrent.TimeUnit

internal val MCP_READ_ONLY_COMMANDS = sortedSetOf(
    "account list",
    "addressbook",
    "announcements",
    "capabilities",
    "classrooms",
    "completed-lessons",
    "config",
    "dashboard",
    "doctor",
    "duties",
    "env",
    "event-categories",
    "events",
    "events categories",
    "exams",
    "grade-categories",
    "grades",
    "grades averages",
    "grades categories",
    "grades summary",
    "homework",
    "kindergarten-hours",
    "kindergarten-teachers",
    "lucky-number",
    "meal-menu",
    "me",
    "meetings",
    "message",
    "messages api",
    "messages deleted",
    "messages list",
    "messages received",
    "messages sent",
    "notice-categories",
    "notices",
    "notices categories",
    "notes",
    "periods",
    "planned-lessons",
    "presence",
    "presence info",
    "presence months",
    "presence subjects",
    "profile list",
    "profile show",
    "schedule",
    "schedule-extra",
    "school-info",
    "subjects",
    "teachers",
    "timeslots",
    "trips",
    "users",
    "vacations",
    "version",
)

private val mcpJson = Json { ignoreUnknownKeys = true }

internal suspend fun runMcpServer() {
    // STDIO przeznacza całe stdout na JSON-RPC; komunikat startowy biblioteki uszkodziłby protokół.
    System.setProperty("kotlin-logging.logStartupMessage", "false")
    val server = Server(
        serverInfo = Implementation(
            name = "dzienniczek-cli",
            version = CLI_VERSION,
            title = "Dzienniczek CLI",
            websiteUrl = "https://github.com/MatissowyMati/Dzienniczek-CLI",
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false),
            ),
        ),
        instructions = """
            Udostępnia dane z VULCAN, eduVULCAN i Librus przez lokalny profil Dzienniczek CLI.
            Narzędzie jest tylko do odczytu. Logowanie i wszystkie operacje zmieniające stan wykonuj bezpośrednio w CLI po potwierdzeniu przez użytkownika.
            Nie ujawniaj danych logowania, tokenów, pliku .env ani pliku konfiguracji profilu.
        """.trimIndent(),
    )

    server.addTool(
        name = "dzienniczek",
        description = "Pobiera dane z polskiego dziennika elektronicznego. Obsługuje wyłącznie bezpieczne polecenia tylko do odczytu; np. doctor, dashboard, grades, schedule, exams, homework, presence i messages received.",
        inputSchema = mcpInputSchema(),
        toolAnnotations = ToolAnnotations(
            title = "Pobierz dane z dziennika",
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = true,
        ),
    ) { request ->
        try {
            invokeCliForMcp(request.arguments ?: buildJsonObject {})
        } catch (error: CliError) {
            mcpError(error.message ?: "Nieprawidłowe wywołanie", error.code)
        } catch (error: Exception) {
            mcpError(error.message ?: "Nie udało się wykonać polecenia", Exit.INTERNAL)
        }
    }

    val transport = StdioServerTransport(
        System.`in`.asSource().buffered(),
        System.out.asSink().buffered(),
    ) {}
    try {
        server.createSession(transport)
        awaitCancellation()
    } finally {
        server.close()
    }
}

private fun mcpInputSchema() = ToolSchema(
    properties = buildJsonObject {
        put("polecenie", buildJsonObject {
            put("type", "string")
            put("description", "Polecenie tylko do odczytu, np. grades, schedule, presence months albo profile show.")
            putJsonArray("enum") { MCP_READ_ONLY_COMMANDS.forEach { add(it) } }
        })
        stringProperty("profil", "Nazwa zapisanego profilu.")
        stringProperty("konto", "Indeks konta, identyfikator ucznia albo fragment imienia i nazwiska.")
        stringProperty("okres", "Identyfikator albo numer okresu szkolnego.")
        stringProperty("od", "Początek zakresu dat w formacie YYYY-MM-DD.", "date")
        stringProperty("do", "Koniec zakresu dat w formacie YYYY-MM-DD.", "date")
        stringProperty("dzien", "Dzień w formacie YYYY-MM-DD.", "date")
        stringProperty("tydzien", "Dowolny dzień tygodnia planu w formacie YYYY-MM-DD.", "date")
        stringProperty("identyfikator", "Identyfikator wiadomości.")
        stringProperty("skrzynka", "Globalny identyfikator skrzynki wiadomości.")
        integerProperty("weakRefId", "Identyfikator wpisu frekwencji.", minimum = 0, maximum = null)
        integerProperty("typ", "Typ wpisu frekwencji.", minimum = 0, maximum = null)
        integerProperty("rozmiarStrony", "Maksymalna liczba rekordów pobieranych z API.", minimum = 1, maximum = 500)
        integerProperty("ostatnieId", "Identyfikator używany do stronicowania wiadomości.", minimum = 0, maximum = null)
        integerProperty("limitCzasu", "Limit czasu żądania sieciowego w sekundach.", minimum = 1, maximum = 120)
        booleanProperty("skrot", "Zwraca skrócone dane tam, gdzie polecenie to obsługuje.")
        booleanProperty("hebe", "Dla eduVULCAN wymusza starszy interfejs wiadomości Hebe.")
        booleanProperty("api", "Dla Librusa używa interfejsu API zamiast widoku Synergia.")
    },
    required = listOf("polecenie"),
)

private fun kotlinx.serialization.json.JsonObjectBuilder.stringProperty(
    name: String,
    description: String,
    format: String? = null,
) {
    put(name, buildJsonObject {
        put("type", "string")
        put("description", description)
        format?.let { put("format", it) }
    })
}

private fun kotlinx.serialization.json.JsonObjectBuilder.integerProperty(
    name: String,
    description: String,
    minimum: Int,
    maximum: Int?,
) {
    put(name, buildJsonObject {
        put("type", "integer")
        put("description", description)
        put("minimum", minimum)
        maximum?.let { put("maximum", it) }
    })
}

private fun kotlinx.serialization.json.JsonObjectBuilder.booleanProperty(name: String, description: String) {
    put(name, buildJsonObject {
        put("type", "boolean")
        put("description", description)
    })
}

internal fun mcpCliArguments(arguments: JsonObject): List<String> {
    val command = arguments.string("polecenie")
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?: throw CliError("Brakuje pola 'polecenie'", Exit.USAGE)
    if (command !in MCP_READ_ONLY_COMMANDS) {
        throw CliError("Polecenie '$command' nie jest dostępne przez MCP", Exit.USAGE)
    }

    val result = command.split(' ').toMutableList()
    val stringOptions = linkedMapOf(
        "profil" to "profile",
        "konto" to "account",
        "okres" to "period",
        "od" to "from",
        "do" to "to",
        "dzien" to "day",
        "tydzien" to "week",
        "identyfikator" to "id",
        "skrzynka" to "box",
    )
    stringOptions.forEach { (property, option) ->
        arguments.string(property)?.let { value -> result += listOf("--$option", value) }
    }

    val integerOptions = linkedMapOf(
        "weakRefId" to "weak-ref-id",
        "typ" to "type",
        "rozmiarStrony" to "page-size",
        "ostatnieId" to "last-id",
        "limitCzasu" to "timeout",
    )
    integerOptions.forEach { (property, option) ->
        arguments.integer(property)?.let { value -> result += listOf("--$option", value.toString()) }
    }

    if ((arguments.integer("rozmiarStrony") ?: 1) !in 1..500) {
        throw CliError("'rozmiarStrony' musi mieścić się w zakresie 1–500", Exit.USAGE)
    }
    if ((arguments.integer("limitCzasu") ?: 30) !in 1..120) {
        throw CliError("'limitCzasu' musi mieścić się w zakresie 1–120", Exit.USAGE)
    }
    if ((arguments.integer("weakRefId") ?: 0) < 0 || (arguments.integer("typ") ?: 0) < 0 ||
        (arguments.integer("ostatnieId") ?: 0) < 0
    ) {
        throw CliError("Identyfikatory liczbowe nie mogą być ujemne", Exit.USAGE)
    }

    mapOf("skrot" to "brief", "hebe" to "hebe", "api" to "api").forEach { (property, option) ->
        if (arguments.boolean(property) == true) result += "--$option"
    }
    result += listOf("--json", "--compact", "--non-interactive")
    return result
}

private fun JsonObject.string(name: String): String? = get(name)?.jsonPrimitive?.contentOrNull

private fun JsonObject.integer(name: String): Int? = get(name)?.jsonPrimitive?.intOrNull

private fun JsonObject.boolean(name: String): Boolean? = get(name)?.jsonPrimitive?.booleanOrNull

private data class ProcessResult(val code: Int, val stdout: String, val stderr: String)

private suspend fun invokeCliForMcp(arguments: JsonObject): CallToolResult {
    val cliArguments = mcpCliArguments(arguments)
    val timeout = arguments.integer("limitCzasu") ?: 30
    val result = runCliProcess(cliArguments, timeout)
    val text = (if (result.code == Exit.OK) result.stdout else result.stderr).trim()
        .ifEmpty { "Polecenie zakończyło się kodem ${result.code} bez odpowiedzi." }
    val structured = parseStructured(text, result.code)
    return CallToolResult(
        content = listOf(TextContent(text = text)),
        isError = result.code != Exit.OK,
        structuredContent = structured,
    )
}

private suspend fun runCliProcess(arguments: List<String>, timeoutSeconds: Int): ProcessResult = coroutineScope {
    val javaBinary = Path.of(
        System.getProperty("java.home"),
        "bin",
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "java.exe" else "java",
    ).toString()
    val command = listOf(
        javaBinary,
        "-Dfile.encoding=UTF-8",
        "-cp",
        System.getProperty("java.class.path"),
        "io.github.matissowymati.dzienniczek.cli.MainKt",
    ) + arguments
    val process = withContext(Dispatchers.IO) { ProcessBuilder(command).start() }
    val stdout = async(Dispatchers.IO) { process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() } }
    val stderr = async(Dispatchers.IO) { process.errorStream.bufferedReader(Charsets.UTF_8).use { it.readText() } }
    try {
        val finished = withContext(Dispatchers.IO) {
            process.waitFor((timeoutSeconds + 5).toLong(), TimeUnit.SECONDS)
        }
        if (!finished) {
            process.destroyForcibly()
            withContext(Dispatchers.IO) { process.waitFor() }
            stdout.await()
            stderr.await()
            return@coroutineScope ProcessResult(
                code = Exit.NETWORK,
                stdout = "",
                stderr = "Przekroczono limit czasu wykonania polecenia.",
            )
        }
        ProcessResult(process.exitValue(), stdout.await(), stderr.await())
    } finally {
        if (process.isAlive) process.destroyForcibly()
    }
}

private fun parseStructured(text: String, code: Int): JsonObject {
    val parsed: JsonElement? = try {
        mcpJson.parseToJsonElement(text)
    } catch (_: Exception) {
        null
    }
    return when (parsed) {
        is JsonObject -> parsed
        null -> buildJsonObject {
            put("ok", code == Exit.OK)
            if (code == Exit.OK) put("dane", text) else put("blad", text)
            put("code", code)
        }
        else -> buildJsonObject {
            put("ok", code == Exit.OK)
            put("dane", parsed)
            put("code", code)
        }
    }
}

private fun mcpError(message: String, code: Int): CallToolResult {
    val structured = buildJsonObject {
        put("ok", false)
        put("blad", message)
        put("code", code)
    }
    return CallToolResult(
        content = listOf(TextContent(text = mcpJson.encodeToString(JsonObject.serializer(), structured))),
        isError = true,
        structuredContent = structured,
    )
}

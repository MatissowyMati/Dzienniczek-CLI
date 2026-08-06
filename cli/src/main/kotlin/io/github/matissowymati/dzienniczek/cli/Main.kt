package io.github.matissowymati.dzienniczek.cli

import io.github.matissowymati.dzienniczek.api.hebe.VulcanApi
import io.github.matissowymati.dzienniczek.api.hebe.EduVulcanApi
import io.github.matissowymati.dzienniczek.api.hebe.credentials.RsaCredential
import io.github.matissowymati.dzienniczek.api.hebe.models.Account
import io.github.matissowymati.dzienniczek.api.librus.LibrusLoginHelper
import io.github.matissowymati.dzienniczek.api.librus.LibrusApi
import io.github.matissowymati.dzienniczek.api.prometheus.PrometheusLoginHelper
import io.github.matissowymati.dzienniczek.api.prometheus.decodeJWT
import io.github.matissowymati.dzienniczek.api.network.ProviderTls
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

const val CLI_VERSION = "1.1.0"

fun main(raw: Array<String>) {
    val args = CliArgs(raw.toList())
    val code = try {
        Env.load(raw)
        runBlocking { execute(args) }
        Exit.OK
    } catch (error: CliError) {
        error(error.message ?: "Error", error.code, args)
        error.code
    } catch (error: Throwable) {
        val code = classify(error)
        error(error.message ?: error::class.simpleName ?: "Error", code, args, error)
        code
    }
    if (code != Exit.OK) exitProcess(code)
}

private suspend fun execute(args: CliArgs) {
    val command = args.words.firstOrNull() ?: "help"
    if (command in setOf("help", "-h", "--help")) {
        printHelp()
        return
    }
    if (command in setOf("version", "--version", "-V")) {
        emit(buildJsonObject { put("name", "dzienniczek"); put("version", CLI_VERSION) }, args)
        return
    }
    if (command == "capabilities" || command == "schema") {
        emit(capabilities(), args)
        return
    }
    if (command == "env") {
        emit(environmentStatus(), args)
        return
    }

    val store = ConfigStore.create(args.value("config"))
    var config = store.load()
    if (command == "doctor") {
        emit(doctorStatus(store, config), args)
        return
    }
    val client = createClient(args)
    client.use {
        when (command) {
            "config" -> emit(buildJsonObject { put("path", store.path.toString()) }, args)
            "login" -> {
                val name = args.value("profile") ?: "default"
                val provider = args.words.getOrNull(1) ?: Env.inferredProvider()
                    ?: throw CliError("Set DZIENNICZEK_PROVIDER or use: dzienniczek login PROVIDER", Exit.USAGE)
                val profile = login(provider, args, client)
                config = config.copy(currentProfile = name, profiles = config.profiles + (name to profile))
                store.save(config)
                ok("Logged in", args, buildJsonObject { put("profile", name); put("provider", profile.provider); put("accounts", profile.accounts.size) })
            }
            "logout" -> {
                if (args.flag("all")) {
                    if (!args.flag("yes")) throw CliError("logout --all requires --yes", Exit.USAGE)
                    config = ConfigData()
                    store.save(config)
                    ok("All local profiles removed", args)
                } else {
                    val name = args.value("profile") ?: config.currentProfile ?: throw CliError("No active profile", Exit.CONFIG)
                    if (!args.flag("yes")) throw CliError("logout removes stored credentials; pass --yes", Exit.USAGE)
                    val remaining = config.profiles - name
                    config = config.copy(currentProfile = remaining.keys.firstOrNull(), profiles = remaining)
                    store.save(config)
                    ok("Local profile removed", args, buildJsonObject { put("profile", name) })
                }
            }
            "profile", "profiles" -> handleProfiles(args.words.getOrNull(1) ?: "list", args, store, config)
            "account" -> handleAccount(args.words.getOrNull(1) ?: "list", args, store, config, client)
            else -> {
                val (name, profile) = selectedProfile(config, args.value("profile"))
                if (profile.provider == "librus") {
                    val api = profile.librusApi(client)
                    emit(runLibrusCommand(command, args.words.getOrNull(1), args, api), args)
                } else {
                    val account = resolveAccount(profile, args.value("account"))
                    val ctx = HebeContext(name, profile, profile.hebeApi(client), account)
                    emit(runHebeCommand(command, args.words.getOrNull(1), args, ctx), args)
                }
            }
        }
    }
}

private fun createClient(args: CliArgs): HttpClient {
    val timeout = args.positiveInt("timeout", 30) * 1_000L
    return HttpClient(CIO) {
    followRedirects = true
    engine { https { trustManager = ProviderTls.trustManager } }
    install(HttpTimeout) {
        requestTimeoutMillis = timeout
        connectTimeoutMillis = timeout
        socketTimeoutMillis = timeout
    }
    install(HttpCookies) { storage = AcceptAllCookiesStorage() }
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; coerceInputValues = true }) }
    }
}

private suspend fun login(provider: String, args: CliArgs, client: HttpClient): Profile = when (provider.lowercase()) {
    "vulcan", "dzienniczek-vulcan", "hebe" -> {
        val credential = RsaCredential.createNew(deviceOs(), args.value("device") ?: "Dzienniczek CLI")
        val api = VulcanApi(credential, client)
        api.registerByToken(
            args.required("token", "DZIENNICZEK_TOKEN", true),
            args.required("pin", "DZIENNICZEK_PIN", true),
            args.required("symbol", "DZIENNICZEK_SYMBOL")
        )
        Profile("vulcan", CredentialData.from(credential), api.getAccounts())
    }
    "eduvulcan", "edu", "prometheus" -> {
        val username = args.required("username", "DZIENNICZEK_USERNAME")
        val password = args.required("password", "DZIENNICZEK_PASSWORD", true)
        val result = PrometheusLoginHelper().login(username, password, args.value("device") ?: "Dzienniczek CLI")
        val tenant = args.value("tenant") ?: Env.get("DZIENNICZEK_TENANT") ?: result.tenantTokens.keys.singleOrNull()
            ?: throw CliError("Multiple tenants available: ${result.tenantTokens.keys.joinToString()}; pass --tenant", Exit.USAGE)
        val tokens = result.allTenantTokens.filter { decodeJWT(it).tenant == tenant }
        if (tokens.isEmpty()) throw CliError("Tenant '$tenant' not returned by eduVULCAN", Exit.AUTH)
        registerEdu(args, client, tenant, tokens, username, password)
    }
    "jwt", "eduvulcan-jwt" -> {
        val tokens = args.values("token").ifEmpty { Env.get("DZIENNICZEK_JWT")?.split(',')?.filter(String::isNotBlank).orEmpty() }
        if (tokens.isEmpty()) throw CliError("Pass one or more --token values or DZIENNICZEK_JWT", Exit.USAGE)
        registerEdu(args, client, args.required("tenant", "DZIENNICZEK_TENANT"), tokens, null, null)
    }
    "librus" -> {
        val token = LibrusLoginHelper().login(
            args.required("username", "DZIENNICZEK_USERNAME"),
            args.required("password", "DZIENNICZEK_PASSWORD", true)
        )
        val api = LibrusApi(client, portalAccessToken = token.accessToken)
        val accounts = api.getSynergiaAccounts()
        val requestedAccount = args.value("librus-account")
        val selected = requestedAccount?.let { wanted ->
            accounts.firstOrNull { it.login == wanted || it.id.toString() == wanted || it.studentName.contains(wanted, true) }
                ?: throw CliError("Librus account '$wanted' not found", Exit.USAGE)
        } ?: accounts.singleOrNull()
            ?: if (accounts.size > 1) throw CliError(
                "Multiple Librus accounts available: ${accounts.joinToString { "${it.login} (${it.studentName})" }}; pass --librus-account",
                Exit.USAGE
            ) else accounts.firstOrNull()
        ?: throw CliError("No Librus Synergia accounts found", Exit.AUTH)
        val apiToken = api.getFreshApiToken(selected.login)
        Profile(
            provider = "librus", librusPortalToken = token.accessToken,
            librusApiToken = apiToken, librusAccountLogin = selected.login,
            librusStudentName = selected.studentName, librusAccounts = accounts
        )
    }
    else -> throw CliError("Unknown provider '$provider' (vulcan, eduvulcan, jwt, librus)", Exit.USAGE)
}

private suspend fun registerEdu(
    args: CliArgs,
    client: HttpClient,
    tenant: String,
    tokens: List<String>,
    username: String?,
    password: String?,
): Profile {
    val credential = RsaCredential.createNew(deviceOs(), args.value("device") ?: "Dzienniczek CLI")
    val api = EduVulcanApi(credential, client)
    api.registerByJwt(tokens, tenant)
    return Profile(
        provider = "eduvulcan", credential = CredentialData.from(credential), accounts = api.getAccounts(),
        prometheusLogin = username, prometheusPassword = if (args.flag("no-store-password")) null else password,
        prometheusTenant = tenant
    )
}

private fun deviceOs(): String = when {
    System.getProperty("os.name").contains("Mac", true) -> "iOS"
    else -> "Android"
}

private fun selectedProfile(config: ConfigData, requested: String?): Pair<String, Profile> {
    val name = requested ?: Env.get("DZIENNICZEK_PROFILE") ?: config.currentProfile
        ?: throw CliError("No active profile; run 'dzienniczek login ...'", Exit.CONFIG)
    return name to (config.profiles[name] ?: throw CliError("Profile '$name' not found", Exit.CONFIG))
}

private fun handleProfiles(action: String, args: CliArgs, store: ConfigStore, config: ConfigData) {
    when (action) {
        "list" -> emit(JsonArray(config.profiles.map { (name, profile) -> buildJsonObject {
            put("name", name); put("current", name == config.currentProfile); put("provider", profile.provider)
            put("student", profile.librusStudentName ?: profile.accounts.getOrNull(profile.selectedAccount)?.let { "${it.pupil.firstName} ${it.pupil.surname}" } ?: "")
        } }), args)
        "show" -> {
            val (name, profile) = selectedProfile(config, args.value("profile") ?: args.words.getOrNull(2))
            emit(buildJsonObject {
                put("name", name); put("provider", profile.provider); put("selectedAccount", profile.selectedAccount)
                put("accounts", JsonArray(profile.accounts.mapIndexed { index, account -> accountSummary(index, account) }))
                if (profile.provider == "librus") put("librusAccounts", JsonArray(profile.librusAccounts.mapIndexed { index, account -> buildJsonObject {
                    put("index", index); put("id", account.id); put("login", account.login); put("student", account.studentName)
                    put("school", account.schoolName ?: ""); put("current", account.login == profile.librusAccountLogin)
                } }))
                put("hasCredential", profile.credential != null); put("hasStoredMessagePassword", profile.prometheusPassword != null)
                profile.librusStudentName?.let { put("student", it) }
            }, args)
        }
        "use" -> {
            val name = args.words.getOrNull(2) ?: args.required("name")
            if (name !in config.profiles) throw CliError("Profile '$name' not found", Exit.CONFIG)
            store.save(config.copy(currentProfile = name))
            ok("Active profile changed", args, buildJsonObject { put("profile", name) })
        }
        "remove", "delete" -> {
            val name = args.words.getOrNull(2) ?: args.value("profile") ?: throw CliError("Pass profile name", Exit.USAGE)
            if (!args.flag("yes")) throw CliError("profile remove requires --yes", Exit.USAGE)
            val remaining = config.profiles - name
            store.save(config.copy(currentProfile = config.currentProfile?.takeIf { it != name } ?: remaining.keys.firstOrNull(), profiles = remaining))
            ok("Profile removed", args, buildJsonObject { put("profile", name) })
        }
        else -> throw CliError("Unknown profile command '$action'", Exit.USAGE)
    }
}

private suspend fun handleAccount(action: String, args: CliArgs, store: ConfigStore, config: ConfigData, client: HttpClient) {
    val (name, profile) = selectedProfile(config, args.value("profile"))
    if (profile.provider == "librus") {
        when (action) {
            "list" -> emit(JsonArray(profile.librusAccounts.mapIndexed { index, account -> buildJsonObject {
                put("index", index); put("id", account.id); put("login", account.login); put("student", account.studentName)
                put("school", account.schoolName ?: ""); put("current", account.login == profile.librusAccountLogin)
            } }), args)
            "use" -> {
                val selector = args.words.getOrNull(2) ?: args.required("account")
                val selected = selector.toIntOrNull()?.let { number ->
                    profile.librusAccounts.getOrNull(number) ?: profile.librusAccounts.firstOrNull { it.id == number }
                } ?: profile.librusAccounts.firstOrNull { it.login == selector || it.studentName.contains(selector, true) }
                ?: throw CliError("Librus account '$selector' not found", Exit.USAGE)
                val token = LibrusApi(client, portalAccessToken = profile.librusPortalToken).getFreshApiToken(selected.login)
                store.save(config.copy(profiles = config.profiles + (name to profile.copy(
                    librusApiToken = token, librusAccountLogin = selected.login, librusStudentName = selected.studentName
                ))))
                ok("Librus account changed", args, buildJsonObject { put("login", selected.login); put("student", selected.studentName) })
            }
            else -> throw CliError("Unknown account command '$action'", Exit.USAGE)
        }
        return
    }
    when (action) {
        "list" -> emit(JsonArray(profile.accounts.mapIndexed(::accountSummary)), args)
        "use" -> {
            val selector = args.words.getOrNull(2) ?: args.required("account")
            val selected = resolveAccount(profile, selector)
            val index = profile.accounts.indexOf(selected)
            store.save(config.copy(profiles = config.profiles + (name to profile.copy(selectedAccount = index))))
            ok("Student account changed", args, buildJsonObject { put("account", index); put("pupilId", selected.pupil.id) })
        }
        else -> throw CliError("Unknown account command '$action'", Exit.USAGE)
    }
}

private fun accountSummary(index: Int, account: Account) = buildJsonObject {
    put("index", index); put("pupilId", account.pupil.id)
    put("student", "${account.pupil.firstName} ${account.pupil.surname}")
    put("class", account.classDisplay ?: ""); put("school", account.unit.displayName)
    put("currentPeriod", account.periods.firstOrNull { it.current }?.id)
}

private fun capabilities() = buildJsonObject {
    put("name", "dzienniczek"); put("version", CLI_VERSION); put("agentSafe", true)
    put("documentation", "docs/AI_USAGE.txt")
    putJsonArray("providers") { listOf("vulcan", "eduvulcan", "eduvulcan-jwt", "librus").forEach(::add) }
    putJsonArray("commands") {
        listOf(
            "dashboard", "accounts", "periods", "heartbeat", "addressbook", "announcements", "completed-lessons",
            "duties", "exams", "grades", "grades averages", "grades summary", "homework", "kindergarten-hours",
            "kindergarten-teachers", "lucky-number", "meal-menu", "meetings", "notes", "planned-lessons",
            "presence", "presence months", "presence subjects", "presence info", "messages received", "messages sent",
            "messages deleted", "message", "schedule", "schedule-extra", "school-info", "teachers", "timeslots",
            "trips", "events", "vacations", "push locale", "push all", "push set", "push configure",
            "credential delete", "subjects", "users", "classrooms", "notices", "auto-login-token", "doctor"
        ).forEach(::add)
    }
    putJsonArray("mutatingCommands") {
        listOf("login", "logout", "profile use", "profile remove", "account use", "messages importance",
            "messages status", "push locale", "push all", "push set", "push configure", "credential delete").forEach(::add)
    }
    putJsonObject("globalOptions") {
        put("--format", "json|table|plain"); put("--json", "alias for --format json"); put("--compact", "compact JSON")
        put("--profile", "stored profile"); put("--account", "account index, pupil ID, or name"); put("--period", "period ID or number")
        put("--from/--to", "YYYY-MM-DD inclusive date range"); put("--config", "config path override")
        put("--env-file", "dotenv path (default: .env)"); put("--no-env", "disable dotenv loading")
        put("--non-interactive", "never prompt; fail when required input is missing")
        put("--timeout", "positive network timeout in seconds (default: 30)")
    }
    putJsonObject("outputContract") {
        put("stdout", "requested result only"); put("stderr", "errors and diagnostics only")
        put("defaultWhenPiped", "json"); put("recommended", "--json --compact --non-interactive")
        put("dates", "YYYY-MM-DD"); put("encoding", "UTF-8")
    }
    putJsonObject("exitCodes") {
        put("0", "success"); put("2", "usage"); put("3", "authentication"); put("4", "network"); put("5", "remote API")
        put("6", "local configuration"); put("10", "internal")
    }
}

private fun doctorStatus(store: ConfigStore, config: ConfigData): JsonObject {
    val javaVersion = System.getProperty("java.version").orEmpty()
    val javaMajor = javaVersion.substringBefore('.').toIntOrNull() ?: 0
    val provider = Env.inferredProvider()
    val environmentReady = when (provider) {
        "eduvulcan", "edu", "prometheus", "librus" -> Env.available("DZIENNICZEK_USERNAME") && Env.available("DZIENNICZEK_PASSWORD")
        "vulcan", "hebe" -> Env.available("DZIENNICZEK_TOKEN") && Env.available("DZIENNICZEK_PIN") && Env.available("DZIENNICZEK_SYMBOL")
        "jwt", "eduvulcan-jwt" -> Env.available("DZIENNICZEK_JWT") && Env.available("DZIENNICZEK_TENANT")
        else -> false
    }
    val hasProfile = config.currentProfile?.let(config.profiles::containsKey) == true
    return buildJsonObject {
        put("ok", javaMajor >= 17 && (hasProfile || environmentReady))
        put("version", CLI_VERSION)
        putJsonObject("runtime") {
            put("java", javaVersion); put("javaSupported", javaMajor >= 17)
            put("os", System.getProperty("os.name")); put("arch", System.getProperty("os.arch"))
        }
        put("configPath", store.path.toString())
        put("profiles", config.profiles.size)
        config.currentProfile?.let { put("currentProfile", it) }
        put("hasUsableProfile", hasProfile)
        put("environmentProvider", provider)
        put("environmentReadyForLogin", environmentReady)
        put("envFileLoaded", Env.loadedPath != null)
        putJsonArray("nextSteps") {
            if (!hasProfile && environmentReady) add("Run: dzienniczek login --non-interactive")
            if (!hasProfile && !environmentReady) add("Configure .env, then run: dzienniczek login --non-interactive")
            if (hasProfile) add("Run: dzienniczek capabilities --json --compact")
        }
    }
}

private fun environmentStatus() = buildJsonObject {
    put("loaded", Env.loadedPath != null)
    Env.loadedPath?.let { put("path", it.toString()) }
    put("provider", Env.inferredProvider())
    putJsonObject("available") {
        put("username", Env.available("DZIENNICZEK_USERNAME"))
        put("password", Env.available("DZIENNICZEK_PASSWORD"))
        put("token", Env.available("DZIENNICZEK_TOKEN"))
        put("pin", Env.available("DZIENNICZEK_PIN"))
        put("symbol", Env.available("DZIENNICZEK_SYMBOL"))
        put("tenant", Env.available("DZIENNICZEK_TENANT"))
        put("jwt", Env.available("DZIENNICZEK_JWT"))
    }
}

private fun printHelp() = println(
    """
    dzienniczek $CLI_VERSION — VULCAN, eduVULCAN and Librus CLI

    Login:
      dzienniczek login vulcan --token TOKEN --pin PIN --symbol SCHOOL
      dzienniczek login                         # provider and credentials from .env
      dzienniczek login eduvulcan --username USER --password PASS [--tenant TENANT]
      dzienniczek login jwt --tenant TENANT --token JWT [--token JWT...]
      dzienniczek login librus --username USER --password PASS [--librus-account LOGIN]

    Daily use:
      dzienniczek dashboard
      dzienniczek grades [averages|summary]
      dzienniczek schedule|exams|homework --from YYYY-MM-DD --to YYYY-MM-DD
      dzienniczek presence [months|subjects|info]
      dzienniczek messages [received|sent|deleted]
      dzienniczek notes|announcements|teachers|school-info|trips|events|vacations

    Profiles:
      dzienniczek profile list|show|use NAME|remove NAME --yes
      dzienniczek account list|use INDEX
      dzienniczek logout --yes

    Agents:
      dzienniczek capabilities --json
      dzienniczek doctor --json
      dzienniczek COMMAND --json --compact --non-interactive
      Secrets may use DZIENNICZEK_USERNAME, DZIENNICZEK_PASSWORD,
      DZIENNICZEK_TOKEN, DZIENNICZEK_PIN, DZIENNICZEK_SYMBOL and DZIENNICZEK_JWT.
      A local .env is loaded automatically and is never required in Git.

    Global options:
      --format json|table|plain   --profile NAME   --account VALUE
      --from DATE --to DATE       --timeout SECONDS
      --env-file PATH             --no-env         --non-interactive

    Run `dzienniczek capabilities --json` for the complete command inventory.
    """.trimIndent()
)

private fun classify(error: Throwable): Int {
    val name = error::class.qualifiedName.orEmpty()
    val message = error.message.orEmpty()
    return when {
        "WrongPin" in name || "WrongToken" in name || "Invalid credentials" in message || "Login failed" in message -> Exit.AUTH
        "ktor" in name || "timeout" in message.lowercase() || "connect" in message.lowercase() -> Exit.NETWORK
        "Dzienniczek" in name || "Status" in name || "API" in message -> Exit.API
        else -> Exit.INTERNAL
    }
}

private fun error(message: String, code: Int, args: CliArgs, throwable: Throwable? = null) {
    if (args.flag("json") || args.value("format") == "json" || System.console() == null) {
        val value = buildJsonObject {
            put("ok", false); put("error", message); put("code", code)
            if (args.flag("debug") && throwable != null) {
                val writer = StringWriter(); throwable.printStackTrace(PrintWriter(writer)); put("trace", writer.toString())
            }
        }
        System.err.println(Json { prettyPrint = !args.flag("compact") }.encodeToString(JsonObject.serializer(), value))
    } else {
        System.err.println("Error: $message")
    }
}

package io.github.matissowymati.dzienniczek.cli

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

private val prettyJson = Json { prettyPrint = true; explicitNulls = false }
private val compactJson = Json { prettyPrint = false; explicitNulls = false }

fun emit(element: JsonElement, args: CliArgs) {
    val format = args.value("format") ?: if (args.flag("json")) "json" else if (System.console() == null) "json" else "table"
    when (format) {
        "json" -> println((if (args.flag("compact")) compactJson else prettyJson).encodeToString(element))
        "table", "plain" -> print(renderHuman(element, args))
        else -> throw CliError("Nieznany format '$format' (użyj json, table albo plain)", Exit.USAGE)
    }
}

fun ok(message: String, args: CliArgs, extra: JsonObject = buildJsonObject { }) = emit(
    buildJsonObject {
        put("ok", true)
        put("message", message)
        extra.forEach { (key, value) -> put(key, value) }
    }, args
)

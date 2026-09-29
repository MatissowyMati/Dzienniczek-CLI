package io.github.matissowymati.dzienniczek.cli

import kotlinx.serialization.json.*

private val labels = mapOf(
    "dashboard" to "Podsumowanie", "grades" to "Oceny", "averages" to "Średnie",
    "summary" to "Podsumowanie ocen", "schedule" to "Plan lekcji", "schedule-extra" to "Zmiany planu",
    "exams" to "Sprawdziany", "homework" to "Zadania domowe", "presence" to "Frekwencja",
    "months" to "Frekwencja miesięczna", "subjects" to "Frekwencja przedmiotowa",
    "messages" to "Wiadomości", "received" to "Odebrane", "sent" to "Wysłane", "deleted" to "Usunięte",
    "periods" to "Okresy szkolne", "teachers" to "Nauczyciele", "timeslots" to "Godziny lekcji",
    "doctor" to "Diagnostyka", "profile" to "Profile", "account" to "Konta uczniów",
    "student" to "Uczeń", "recentGrades" to "Ostatnie oceny", "upcomingExams" to "Nadchodzące sprawdziany",
    "upcomingHomework" to "Nadchodzące zadania", "luckyNumber" to "Szczęśliwy numerek",
    "notes" to "Uwagi", "announcements" to "Ogłoszenia", "school-info" to "Informacje o szkole",
    "name" to "Nazwa", "provider" to "Dostawca", "current" to "Aktywny", "class" to "Klasa", "school" to "Szkoła",
    "ok" to "Gotowość", "version" to "Wersja", "runtime" to "Środowisko", "javaSupported" to "Java obsługiwana",
    "configPath" to "Ścieżka konfiguracji", "profiles" to "Liczba profili", "currentProfile" to "Aktywny profil",
    "hasUsableProfile" to "Profil dostępny", "environmentProvider" to "Dostawca z .env",
    "environmentReadyForLogin" to "Dane do logowania dostępne", "envFileLoaded" to "Wczytano .env",
    "nextSteps" to "Następne kroki", "message" to "Komunikat", "accounts" to "Konta",
    "StartAt" to "Od", "EndAt" to "Do", "Subject" to "Przedmiot", "Content" to "Treść",
    "Creator" to "Autor", "DateAt" to "Data", "Day" to "Dzień", "Number" to "Numer",
    "ContentRaw" to "Ocena", "Comment" to "Komentarz", "Title" to "Temat", "Body" to "Treść",
    "Sender" to "Nadawca", "index" to "Indeks", "currentPeriod" to "Bieżący okres",
)

private data class HumanColumn(val title: String, val cell: (JsonObject) -> String)
private fun column(title: String, vararg paths: String) = HumanColumn(title) { row ->
    paths.asSequence().map { row.at(it) }.firstOrNull { it != null && it != JsonNull }?.let(::humanText).orEmpty()
}
private fun JsonObject.at(path: String): JsonElement? = path.split('.').fold(this as JsonElement?) { value, key ->
    (value as? JsonObject)?.get(key)
}

/** Prezentacja jest oddzielona od kontraktu JSON używanego przez CLI i MCP. */
internal fun renderHuman(element: JsonElement, args: CliArgs, width: Int = terminalWidth()): String {
    val output = StringBuilder()
    val table = args.value("format") != "plain"
    val command = args.words.firstOrNull().orEmpty()
    val title = cleanText(labels[command] ?: command.ifEmpty { "Dzienniczek" })
    output.appendLine(title.uppercase())
    output.appendLine("─".repeat(minOf(width, maxOf(title.length, 20))))
    renderNode(element, output, width.coerceAtLeast(24), table)
    return output.toString()
}

private fun terminalWidth(): Int {
    System.getenv("COLUMNS")?.toIntOrNull()?.takeIf { it > 0 }?.let { return it.coerceIn(40, 180) }
    if (System.console() != null && !System.getProperty("os.name").startsWith("Windows")) {
        val process = try {
            ProcessBuilder("stty", "size").redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.DISCARD).start()
        } catch (_: Exception) { null }
        if (process != null) try {
            if (process.waitFor(200, java.util.concurrent.TimeUnit.MILLISECONDS) && process.exitValue() == 0) {
                process.inputStream.bufferedReader().use { it.readText() }.trim().split(Regex("\\s+"))
                    .lastOrNull()?.toIntOrNull()?.takeIf { it > 0 }?.let { return it.coerceIn(40, 180) }
            }
        } finally { if (process.isAlive) process.destroyForcibly() }
    }
    return 100
}

private fun renderNode(element: JsonElement, output: StringBuilder, width: Int, table: Boolean, indent: String = "") {
    when (element) {
        is JsonArray -> {
            if (element.isEmpty()) {
                output.appendLine("${indent}Brak wyników.")
            } else if (table && indent.isEmpty() && element.all { it is JsonObject }) {
                renderRows(element.map { it.jsonObject }, output, width)
            } else {
                element.forEachIndexed { index, value ->
                    if (value is JsonObject || value is JsonArray) {
                        output.appendLine("$indent${index + 1}.")
                        renderNode(value, output, width, false, "$indent  ")
                    } else appendWrapped(output, "$indent• ", humanText(value), width)
                }
            }
        }
        is JsonObject -> element.forEach { (key, value) ->
            val label = labels[key] ?: key
            if (value is JsonObject || value is JsonArray) {
                output.appendLine()
                output.appendLine("$indent$label${if (value is JsonArray) " (${value.size})" else ""}")
                renderNode(value, output, width, table, indent)
            } else appendWrapped(output, "$indent$label: ", humanText(value), width)
        }
        else -> appendWrapped(output, indent, humanText(element), width)
    }
}

private fun humanColumns(row: JsonObject): List<HumanColumn>? = when {
    "Column" in row && "Content" in row -> listOf(
        column("Data", "CreatedAt"), column("Przedmiot", "Column.Subject.Name"), column("Ocena", "Content"),
        column("Kategoria", "Column.Category.Name", "Column.Name"), column("Waga", "Column.Weight"), column("Komentarz", "Comment")
    )
    "TimeSlot" in row && "DateAt" in row -> listOf(
        column("Data", "DateAt"), column("Lekcja", "Substitution.TimeSlot.Position", "TimeSlot.Position"),
        HumanColumn("Godziny") { r ->
            val slot = (r["Substitution"] as? JsonObject)?.get("TimeSlot") as? JsonObject ?: r["TimeSlot"] as? JsonObject
            "${humanText(slot?.get("Start"))}–${humanText(slot?.get("End"))}"
        },
        column("Przedmiot", "Substitution.Subject.Name", "Subject.Name", "Event"),
        column("Sala", "Substitution.Room.Code", "Room.Code"),
        column("Nauczyciel", "Substitution.TeacherPrimary.DisplayName", "TeacherPrimary.DisplayName"),
        HumanColumn("Zmiana") { r -> (r["Substitution"] as? JsonObject)?.let { sub ->
            listOf("Description", "PupilNote", "Reason", "Event", "TeacherAbsenceEffectName")
                .mapNotNull { sub[it]?.takeUnless { it == JsonNull }?.let(::humanText)?.takeIf(String::isNotBlank) }
                .distinct().joinToString("; ").ifBlank { "Zastępstwo" }
        }.orEmpty() }
    )
    "DeadlineAt" in row && "Subject" in row -> listOf(
        column("Termin", "DeadlineAt"), column("Przedmiot", "Subject.Name"),
        column("Rodzaj", "Type"), column("Treść", "Content"), column("Autor", "Creator.DisplayName")
    )
    ("Average" in row || "Scope" in row) && "Subject" in row -> listOf(
        column("Przedmiot", "Subject.Name"), column("Średnia", "Average"), column("Punkty", "Points"), column("Zakres", "Scope")
    )
    "PeriodId" in row && "Subject" in row -> listOf(
        column("Przedmiot", "Subject.Name"), column("Propozycja", "Entry_1"), column("Ocena", "Entry_2"), column("Końcowa", "Entry_3")
    )
    "PresencePercentage" in row && "Month" in row -> listOf(
        column("Miesiąc", "Month"), column("Obecność %", "PresencePercentage"), column("Nieobecności", "Absences"),
        column("Uspraw.", "AbsencesJustified"), column("Spóźnienia", "LateArrivals"), column("Zwolnienia", "Exemptions")
    )
    "PresencePercentage" in row && "SubjectName" in row -> listOf(
        column("Przedmiot", "SubjectName"), column("Obecność %", "PresencePercentage"), column("Nieobecności", "Absences"),
        column("Uspraw.", "AbsencesJustified"), column("Spóźnienia", "LateArrivals"), column("Zwolnienia", "Exemptions")
    )
    "DayAt" in row && "TimeSlot" in row -> listOf(
        column("Dzień", "DayAt"), column("Lekcja", "TimeSlot.Position"), column("Godzina", "TimeSlot.Start"), column("Status", "PresenceType.Name")
    )
    "StartAt" in row && "Current" in row && "Number" in row -> listOf(
        column("ID", "Id"), column("Semestr", "Number"), column("Od", "StartAt"), column("Do", "EndAt"),
        HumanColumn("Bieżący") { r ->
            val today = java.time.LocalDate.now().toString()
            if (today >= humanText(r["StartAt"]) && today <= humanText(r["EndAt"])) "tak" else "nie"
        }
    )
    "temat" in row -> listOf(
        column("ID", "id"), column("Data", "data"), column("Temat", "temat"),
        column("Korespondenci", "korespondenci"), column("Przeczytana", "przeczytana"), column("Załączniki", "hasZalaczniki")
    )
    "DisplayName" in row && "Surname" in row -> listOf(column("Nauczyciel", "DisplayName"), column("Opis", "Description"))
    "Start" in row && "End" in row && "Position" in row -> listOf(
        column("Lekcja", "Position"), column("Od", "Start"), column("Do", "End")
    )
    else -> null
}

private fun renderRows(rows: List<JsonObject>, output: StringBuilder, width: Int) {
    val shape = JsonObject(rows.flatMap { it.entries }.associate { it.key to it.value })
    val columns = humanColumns(shape)?.filter { col -> rows.any { col.cell(it).isNotBlank() && col.cell(it) != "—" } }
    if (columns.isNullOrEmpty()) {
        rows.forEachIndexed { index, row ->
            if (index > 0) output.appendLine()
            output.appendLine("${index + 1}.")
            renderNode(row, output, width, false, "  ")
        }
        return
    }
    val ordered = when {
        "TimeSlot" in shape && "DateAt" in shape -> rows.sortedWith(compareBy<JsonObject> { humanText(it["DateAt"]) }
            .thenBy { (it.at("TimeSlot.Position") as? JsonPrimitive)?.intOrNull ?: 0 })
        "Column" in shape && "Content" in shape -> rows.sortedByDescending { humanText(it["CreatedAt"]) }
        "DeadlineAt" in shape -> rows.sortedBy { humanText(it["DeadlineAt"]) }
        else -> rows
    }
    val cells = ordered.map { row -> columns.map { cleanText(it.cell(row)).ifBlank { "—" } } }
    val widths = columns.indices.map { i ->
        maxOf(columns[i].title.length, cells.maxOf { it[i].lineSequence().maxOf(String::length) }).coerceAtMost(36)
    }.toMutableList()
    val minimums = columns.map { minOf(it.title.length, 10).coerceAtLeast(4) }
    if (minimums.sum() + (columns.size - 1) * 3 > width) {
        cells.forEachIndexed { index, row ->
            if (index > 0) output.appendLine()
            output.appendLine("${index + 1}.")
            columns.forEachIndexed { i, col -> appendWrapped(output, "  ${col.title}: ", row[i], width) }
        }
        return
    }
    while (widths.sum() + (columns.size - 1) * 3 > width) {
        val index = widths.indices.filter { widths[it] > minimums[it] }.maxByOrNull { widths[it] } ?: break
        widths[index]--
    }
    fun writeRow(values: List<String>) {
        val lines = values.mapIndexed { i, value -> wrapText(value, widths[i]) }
        repeat(lines.maxOf { it.size }) { line ->
            output.appendLine(lines.mapIndexed { i, parts -> parts.getOrElse(line) { "" }.padEnd(widths[i]) }.joinToString(" │ ").trimEnd())
        }
    }
    writeRow(columns.map { it.title })
    output.appendLine(widths.joinToString("─┼─") { "─".repeat(it) })
    cells.forEach(::writeRow)
    output.appendLine()
    output.appendLine("Liczba wyników: ${rows.size}")
}

private fun appendWrapped(output: StringBuilder, prefix: String, text: String, width: Int) {
    val safePrefix = cleanText(prefix)
    val lines = wrapText(cleanText(text), (width - safePrefix.length).coerceAtLeast(10))
    lines.forEachIndexed { index, line -> output.appendLine((if (index == 0) safePrefix else " ".repeat(safePrefix.length)) + line) }
}

internal fun wrapText(text: String, width: Int): List<String> = text.split('\n').flatMap { paragraph ->
    val result = mutableListOf<String>()
    var remaining = paragraph.trim()
    while (remaining.length > width) {
        val boundary = remaining.lastIndexOf(' ', width).takeIf { it > 0 } ?: width
        result += remaining.take(boundary)
        remaining = remaining.drop(boundary).trimStart()
    }
    result += remaining
    result
}

private fun humanText(element: JsonElement?): String = when (element) {
    null, JsonNull -> "—"
    is JsonPrimitive -> when (element.booleanOrNull) {
        true -> "tak"
        false -> "nie"
        null -> element.content.replace(Regex("(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}):\\d{2}.*"), "$1 $2")
    }
    is JsonArray -> element.joinToString(", ") { humanText(it) }
    is JsonObject -> listOf("DisplayName", "Name", "nazwa", "displayName", "name").firstNotNullOfOrNull { key ->
        element[key]?.takeUnless { it == JsonNull }?.let(::humanText)
    } ?: element.entries.joinToString(", ") { (key, value) -> "${labels[key] ?: key}: ${humanText(value)}" }
}

// Treści dostawcy nie mogą sterować terminalem (ANSI/OSC, powrót karetki, backspace).
internal fun cleanText(value: String): String = value
    .replace(Regex("\u001B\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)"), "")
    .replace(Regex("\u001B\\[[0-?]*[ -/]*[@-~]"), "")
    .replace('\t', ' ')
    .filter { it == '\n' || !it.isISOControl() }

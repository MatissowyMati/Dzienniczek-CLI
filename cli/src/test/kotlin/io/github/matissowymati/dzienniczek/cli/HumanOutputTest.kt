package io.github.matissowymati.dzienniczek.cli

import kotlinx.serialization.json.*
import kotlin.test.*

class HumanOutputTest {
    private val schedule = Json.parseToJsonElement("""[
        {"Id":7,"DateAt":"2026-09-29","TimeSlot":{"Position":1,"Start":"08:00","End":"08:45"},
         "Subject":{"Name":"Matematyka"},"Room":{"Code":"12"},"TeacherPrimary":{"DisplayName":"Anna Testowa"},
         "Substitution":{"Subject":{"Name":"Biologia"},"Room":{"Code":"14"},"Description":"Zastępstwo"}}
    ]""")

    @Test fun scheduleShowsUsefulFieldsAndSubstitutionAtDifferentWidths() {
        for (width in listOf(40, 80, 100, 140)) {
            val text = renderHuman(schedule, CliArgs(listOf("schedule", "--format", "table")), width)
            assertTrue(text.contains("Biologia"))
            assertTrue(text.contains("14"))
            assertTrue(text.contains("08:00–08:45"))
            assertTrue(text.lineSequence().all { it.length <= width }, text)
            assertFalse(text.contains("Id="))
            assertFalse(text.contains("Matematyka"))
        }
    }
    @Test fun longContentIsWrappedWithoutLosingWords() {
        val content = "Przygotuj prezentację o funkcjach kwadratowych i przynieś materiały na kolejną lekcję."
        val data = buildJsonArray { add(buildJsonObject {
            put("DeadlineAt", "2026-10-01"); put("Subject", buildJsonObject { put("Name", "Matematyka") }); put("Content", content)
        }) }
        val text = renderHuman(data, CliArgs(listOf("homework", "--format", "table")), 80)
        content.split(' ').forEach { assertTrue(text.contains(it), "Zgubiono słowo: $it") }
        assertFalse(text.contains('…'))
        assertTrue(text.lineSequence().all { it.length <= 80 })
    }
    @Test fun plainOutputPreservesFieldsBeyondOldTenColumnLimit() {
        val rows = buildJsonArray { add(buildJsonObject { repeat(15) { put("pole$it", "wartość$it") } }) }
        val text = renderHuman(rows, CliArgs(listOf("events", "--format", "plain")), 80)
        assertTrue(text.contains("pole14: wartość14"))
    }
    @Test fun dashboardHasReadableSectionsAndEmptyStates() {
        val data = Json.parseToJsonElement("""{"student":"Uczeń Testowy","recentGrades":[],"upcomingExams":[],"luckyNumber":null}""")
        val text = renderHuman(data, CliArgs(listOf("dashboard")))
        assertTrue(text.contains("Uczeń: Uczeń Testowy"))
        assertTrue(text.contains("Ostatnie oceny (0)"))
        assertTrue(text.contains("Brak wyników."))
        assertFalse(text.contains("recentGrades"))
    }
    @Test fun providerTextCannotInjectTerminalControlSequences() {
        assertEquals("Ocena 5", cleanText("\u001B[31mOcena 5\u001B[0m"))
        assertEquals("Plan", cleanText("\u001B]0;Fałszywy tytuł\u0007Plan\r\u0008"))
    }
}

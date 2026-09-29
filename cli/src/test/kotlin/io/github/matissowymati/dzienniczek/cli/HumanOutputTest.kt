package io.github.matissowymati.dzienniczek.cli

import io.github.matissowymati.dzienniczek.api.hebe.models.Period
import kotlinx.datetime.LocalDate
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
    @Test fun scheduleExtraPreservesDescriptionsAndTeacherAtDifferentWidths() {
        val data = Json.parseToJsonElement("""[
            {"Id":11,"ScheduleExtraId":22,"DateAt":"2026-09-29",
             "TimeSlot":{"Position":3,"Start":"09:50","End":"10:35"},"Room":{"Code":"21"},
             "Teacher":{"DisplayName":"Anna Testowa"},"ExtraDescription":"Warsztaty",
             "ScheduleDescription":"Przyroda","SchedulePupilDescription":"Materiały"}
        ]""")
        for (width in listOf(40, 80, 100, 140)) {
            val text = renderHuman(data, CliArgs(listOf("schedule-extra", "--format", "table")), width)
            for (content in listOf("Warsztaty", "Przyroda", "Materiały", "Anna Testowa", "09:50–10:35", "21")) {
                assertTrue(text.contains(content), "Brak '$content' przy szerokości $width:\n$text")
            }
            assertTrue(text.lineSequence().all { it.length <= width }, text)
            assertFalse(text.contains('…'))
        }
    }
    @Test fun scheduleExtraUsesReplacementTeacherAndTimes() {
        val data = Json.parseToJsonElement("""[
            {"ScheduleExtraId":22,"DateAt":"2026-09-29",
             "TimeSlot":{"Position":3,"Start":"09:50","End":"10:35"},
             "Teacher":{"DisplayName":"Anna Testowa"},"ExtraDescription":"Warsztaty",
             "Substitution":{"Teacher":{"DisplayName":"Jan Testowy"},"TimeStart":"11:00","TimeEnd":"11:45",
                             "Room":{"Code":"30"},"PupilNote":"Przenieś materiały","Reason":"Zmiana sali"}}
        ]""")
        val text = renderHuman(data, CliArgs(listOf("schedule-extra", "--format", "table")), 40)
        assertTrue(text.contains("Jan Testowy"))
        assertTrue(text.contains("11:00–11:45"))
        val unwrapped = text.replace(Regex("\\s+"), " ")
        assertTrue(unwrapped.contains("Przenieś materiały"), text)
        assertTrue(unwrapped.contains("Zmiana sali"), text)
        assertFalse(text.contains("Anna Testowa"))
        assertFalse(text.contains("09:50–10:35"))
    }
    @Test fun periodTableUsesNormalizedCurrentFlagDuringDateGaps() {
        val periods = listOf(
            Period(emptyList(), 101, 7, 1,
                LocalDate(2025, 9, 1), LocalDate(2026, 1, 31), true, false),
            Period(emptyList(), 102, 7, 2,
                LocalDate(2026, 2, 1), LocalDate(2026, 6, 30), false, true),
        )
        for (providerPeriods in listOf(periods, periods.map { it.copy(current = false) })) {
            val chosen = resolvePeriod(providerPeriods, null, LocalDate(2026, 8, 15)).id
            val normalized = providerPeriods.map { it.copy(current = it.id == chosen) }
            val data = Json.encodeToJsonElement(normalized)
            for (width in listOf(24, 40, 100)) {
                val text = renderHuman(data, CliArgs(listOf("periods", "--format", "table")), width)
                if (width == 24) {
                    val entries = text.split(Regex("(?m)^\\d+\\.\n")).drop(1)
                    assertEquals(2, entries.size)
                    normalized.zip(entries).forEach { (period, entry) ->
                        assertTrue(entry.contains("Bieżący: ${if (period.current) "tak" else "nie"}"), entry)
                    }
                } else {
                    normalized.forEach { period ->
                        val row = text.lineSequence().first { it.trimStart().startsWith("${period.id} ") }
                        assertEquals(if (period.current) "tak" else "nie", row.substringAfterLast('│').trim())
                    }
                }
            }
        }
    }
    @Test fun movedLessonsAreSortedByDisplayedSlotAndDate() {
        val data = Json.parseToJsonElement("""[
            {"DateAt":"2026-09-30","TimeSlot":{"Position":1,"Start":"08:00","End":"08:45"},
             "Subject":{"Name":"Jutro"}},
            {"DateAt":"2026-09-29","TimeSlot":{"Position":2,"Start":"08:55","End":"09:40"},
             "Subject":{"Name":"Później"},"Substitution":{"TimeSlot":{"Position":5,"Start":"11:45","End":"12:30"}}},
            {"DateAt":"2026-09-29","TimeSlot":{"Position":3,"Start":"09:50","End":"10:35"},
             "Subject":{"Name":"Środek"},"Substitution":null},
            {"DateAt":"2026-09-29","TimeSlot":{"Position":6,"Start":"12:40","End":"13:25"},
             "Subject":{"Name":"Wcześniej"},"Substitution":{"TimeSlot":{"Position":1,"Start":"08:00","End":"08:45"}}}
        ]""")
        for (width in listOf(40, 100)) {
            val text = renderHuman(data, CliArgs(listOf("schedule", "--format", "table")), width)
            val positions = listOf("Wcześniej", "Środek", "Później", "Jutro").map { text.indexOf(it) }
            assertTrue(positions.all { it >= 0 })
            assertEquals(positions.sorted(), positions, text)
            assertTrue(text.contains("11:45–12:30"))
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
    @Test fun timestampsInsideFreeTextDoNotRemoveFollowingContent() {
        val content = "Termin 2026-10-01T08:00:00: przynieś podręcznik."
        val data = buildJsonObject { put("Content", content) }
        val text = renderHuman(data, CliArgs(listOf("homework", "--format", "plain")), 100)
        assertTrue(text.contains(content))
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

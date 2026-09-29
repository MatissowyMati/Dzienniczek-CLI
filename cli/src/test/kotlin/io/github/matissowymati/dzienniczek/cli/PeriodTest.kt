package io.github.matissowymati.dzienniczek.cli

import io.github.matissowymati.dzienniczek.api.hebe.models.Period
import kotlinx.datetime.LocalDate
import kotlin.test.*

class PeriodTest {
    private val periods = listOf(
        Period(emptyList(), 101, 7, 1, LocalDate(2025, 9, 1), LocalDate(2026, 2, 9), false, false),
        Period(emptyList(), 102, 7, 2, LocalDate(2026, 2, 10), LocalDate(2026, 8, 31), true, true),
        Period(emptyList(), 103, 8, 1, LocalDate(2026, 9, 1), LocalDate(2027, 2, 9), false, false),
    )
    private val today = LocalDate(2026, 9, 29)

    @Test fun schoolYearRolloverOverridesStaleCurrentFlag() {
        assertEquals(103, resolvePeriod(periods, null, today).id)
        assertEquals(103, resolvePeriod(periods, null, LocalDate(2026, 9, 1)).id)
        assertEquals(102, resolvePeriod(periods, null, LocalDate(2026, 8, 31)).id)
    }
    @Test fun semesterNumberUsesMostRecentYearButExplicitIdIsPreserved() {
        assertEquals(103, resolvePeriod(periods, "1", today).id)
        assertEquals(101, resolvePeriod(periods, "101", today).id)
        assertFailsWith<CliError> { resolvePeriod(periods, "999", today) }
    }
    @Test fun oneSidedDateRangesFollowTheSuppliedDate() {
        val from = dates(CliArgs(listOf("--from", "2027-05-12")))
        assertEquals(LocalDate(2027, 5, 12) to LocalDate(2027, 5, 18), from)
        assertEquals(from, dates(CliArgs(listOf("--to", "2027-05-18"))))
        assertFailsWith<CliError> { dates(CliArgs(listOf("--from", "2027-05-12", "--to", "2027-05-01"))) }
    }
}

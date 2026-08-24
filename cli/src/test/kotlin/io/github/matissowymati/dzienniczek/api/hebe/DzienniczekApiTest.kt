package io.github.matissowymati.dzienniczek.api.hebe

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DzienniczekApiTest {
    @Test
    fun luckyNumberMayBeAbsent() {
        assertNull(decodeLuckyNumber(null))
    }

    @Test
    fun decodesAvailableLuckyNumber() {
        val result = decodeLuckyNumber(buildJsonObject {
            put("Day", "2026-08-23")
            put("Number", 7)
        })

        assertEquals("2026-08-23", result?.day.toString())
        assertEquals(7, result?.number)
    }
}

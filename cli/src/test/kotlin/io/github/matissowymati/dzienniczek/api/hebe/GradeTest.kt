package io.github.matissowymati.dzienniczek.api.hebe

import io.github.matissowymati.dzienniczek.api.hebe.models.Grade
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class GradeTest {
    private val outputJson = Json { encodeDefaults = true; explicitNulls = false }

    // Wyłącznie dane syntetyczne; ten sam kształt co odpowiedź mobile/grade/byPupil.
    private fun grade(id: Int, corrected: JsonElement? = null): JsonObject = buildJsonObject {
        put("Id", id)
        put("Key", "grade-$id")
        put("PupilId", 1)
        put("ContentRaw", "4+")
        put("Content", "4+")
        put("Comment", "Poprawa kartkówki")
        put("Value", 4.5)
        put("CreatedAt", "2026-10-01 10:00:00")
        put("ModifiedAt", "2026-10-02 10:00:00")
        val teacher = buildJsonObject {
            put("Id", 1)
            put("Surname", "Testowa")
            put("Name", "Anna")
            put("DisplayName", "Anna Testowa")
        }
        put("Creator", teacher)
        put("Modifier", teacher)
        put("Column", buildJsonObject {
            put("Id", 1)
            put("Key", "column-1")
            put("PeriodId", 1)
            put("Name", "Kartkówka")
            put("Code", "K")
            put("Group", "")
            put("Number", 1)
            put("Color", 0)
            put("Weight", 1.0)
            put("Subject", buildJsonObject {
                put("Id", 1)
                put("Key", "subject-1")
                put("Name", "Matematyka")
                put("Kod", "MAT")
                put("Position", 1)
            })
        })
        if (corrected != null) put("CorrectedGrade", corrected)
    }

    @Test
    fun correctedGradeObjectSurvivesDecodingAndOutput() {
        val original = grade(1, JsonNull)
        val decoded = apiJson.decodeFromJsonElement<Grade>(grade(2, original))
        val encoded = outputJson.encodeToJsonElement(decoded).jsonObject

        assertEquals(2, decoded.id)
        assertEquals("4+", decoded.content)
        assertEquals(original, encoded["CorrectedGrade"])
    }

    @Test
    fun nestedAndPartialCorrectionObjectsArePreservedWithoutLosingFields() {
        val original = buildJsonObject {
            put("Id", 1)
            put("Content", "2")
            put("AdditionalField", "Dodatkowe dane dostawcy")
            put("CorrectedGrade", JsonNull)
        }
        val correction = grade(2, original)
        val decoded = apiJson.decodeFromJsonElement<Grade>(grade(3, correction))

        assertEquals(correction, outputJson.encodeToJsonElement(decoded).jsonObject["CorrectedGrade"])
    }

    @Test
    fun textCorrectionKeepsItsExistingJsonTypeAndValue() {
        for (value in listOf("grade-1", "")) {
            val corrected = JsonPrimitive(value)
            val decoded = apiJson.decodeFromJsonElement<Grade>(grade(2, corrected))

            assertEquals(corrected, outputJson.encodeToJsonElement(decoded).jsonObject["CorrectedGrade"])
        }
    }

    @Test
    fun absentAndNullCorrectionsRemainOptionalAndOmittedFromOutput() {
        for (input in listOf(grade(1), grade(2, JsonNull))) {
            val decoded = apiJson.decodeFromJsonElement<Grade>(input)

            assertNull(decoded.correctedGrade)
            assertFalse("CorrectedGrade" in outputJson.encodeToJsonElement(decoded).jsonObject)
        }
    }

    @Test
    fun mixedGradeListAndEmptyListDecodeSuccessfully() {
        val input = JsonArray(listOf(grade(1), grade(2, grade(1, JsonNull)), grade(3, JsonPrimitive("grade-2"))))
        val decoded = apiJson.decodeFromJsonElement<List<Grade>>(input)
        val output = outputJson.encodeToJsonElement(decoded).jsonArray

        assertEquals(listOf(1, 2, 3), decoded.map { it.id })
        assertEquals(input[1].jsonObject["CorrectedGrade"], output[1].jsonObject["CorrectedGrade"])
        assertEquals(emptyList(), apiJson.decodeFromJsonElement<List<Grade>>(JsonArray(emptyList())))
    }
}

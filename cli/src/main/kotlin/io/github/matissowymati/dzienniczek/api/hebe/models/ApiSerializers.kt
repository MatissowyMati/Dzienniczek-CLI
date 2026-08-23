package io.github.matissowymati.dzienniczek.api.hebe.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Serializator ciągów daty i czasu API VULCAN.
 * Obsługuje format „YYYY-MM-DD HH:MM:SS” (separator-spacja) i ISO „YYYY-MM-DDTHH:MM:SS” (separator T).
 */
object VulcanDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val raw = decoder.decodeString()
        // Zamień separator-spację na T i usuń przyrostek strefy czasowej.
        val normalized = raw.replace(' ', 'T')
            .substringBefore('+')
            .let { if (it.endsWith('Z')) it.dropLast(1) else it }
        return LocalDateTime.parse(normalized)
    }
}

/**
 * Serializator samych dat API VULCAN w formacie „YYYY-MM-DD”.
 */
object VulcanDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString().substringBefore('T').substringBefore(' '))
    }
}

/**
 * Serializator wartości liczbowych, które mogą być całkowite (15) albo dziesiętne (15.0).
 */
@OptIn(ExperimentalSerializationApi::class)
object VulcanNullableIntSerializer : KSerializer<Int?> {
    override val descriptor = PrimitiveSerialDescriptor("NullableInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) {
            encoder.encodeNull()
            return
        }
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int? {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeInt()
        val element = jsonDecoder.decodeJsonElement()

        if (element is JsonNull) return null

        val primitive = element as? JsonPrimitive ?: return null
        primitive.intOrNull?.let { return it }

        val numeric = primitive.doubleOrNull
            ?: primitive.contentOrNull?.replace(',', '.')?.toDoubleOrNull()

        return numeric?.toInt()
    }
}

package io.github.matissowymati.dzienniczek.api.hebe.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Distribution(
    @SerialName("Id") val id: Int,
    @SerialName("Key") val key: String,
    @SerialName("Shortcut") val shortcut: String,
    @SerialName("Name") val name: String,
    @SerialName("PartType") val partType: String
)

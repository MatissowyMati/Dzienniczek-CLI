package io.github.matissowymati.dzienniczek.api.hebe.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Subject(
    @SerialName("Id") val id: Int,
    @SerialName("Key") val key: String,
    @SerialName("Name") val name: String,
    @SerialName("Kod") val code: String,
    @SerialName("Position") val position: Int
)

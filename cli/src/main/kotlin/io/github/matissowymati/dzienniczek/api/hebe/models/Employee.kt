package io.github.matissowymati.dzienniczek.api.hebe.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Employee(
    @SerialName("Id") val id: Int,
    @SerialName("Surname") val surname: String,
    @SerialName("Name") val name: String,
    @SerialName("DisplayName") val displayName: String
)

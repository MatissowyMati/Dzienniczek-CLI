package io.github.matissowymati.dzienniczek.api.librus.models.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibrusClassroomsResponse(
    @SerialName("Classrooms")
    val classrooms: List<LibrusClassroom>
)

@Serializable
data class LibrusClassroom(
    @SerialName("Id") val id: Long,
    @SerialName("Name") val name: String,
    @SerialName("Symbol") val symbol: String? = null
)

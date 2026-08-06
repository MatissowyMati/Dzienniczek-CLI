package io.github.matissowymati.dzienniczek.api.librus.models.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibrusSubjectsResponse(
    @SerialName("Subjects")
    val subjects: List<LibrusSubject>
)

@Serializable
data class LibrusSubject(
    @SerialName("Id") val id: Long,
    @SerialName("Name") val name: String,
    @SerialName("Short") val shortName: String? = null
)

@Serializable
data class LibrusUsersResponse(
    @SerialName("Users")
    val users: List<LibrusUser>
)

@Serializable
data class LibrusUser(
    @SerialName("Id") val id: Long,
    @SerialName("FirstName") val firstName: String? = null,
    @SerialName("LastName") val lastName: String? = null
)

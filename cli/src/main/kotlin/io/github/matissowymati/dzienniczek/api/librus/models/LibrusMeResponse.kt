package io.github.matissowymati.dzienniczek.api.librus.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibrusMeResponse(
    @SerialName("Me")
    val me: LibrusMeData
)

@Serializable
data class LibrusMeData(
    @SerialName("Account")
    val account: LibrusAccountData,
    @SerialName("User")
    val user: LibrusUserData
)

@Serializable
data class LibrusAccountData(
    @SerialName("Id")
    val id: Int? = null,
    @SerialName("UserId")
    val userId: Int? = null,
    @SerialName("FirstName")
    val firstName: String? = null,
    @SerialName("LastName")
    val lastName: String? = null,
    @SerialName("Email")
    val email: String? = null,
    @SerialName("IsPremium")
    val isPremium: Boolean = false,
    @SerialName("IsPremiumDemo")
    val isPremiumDemo: Boolean = false,
    @SerialName("GroupId")
    val groupId: Int? = null
)

@Serializable
data class LibrusUserData(
    @SerialName("Id")
    val id: Int? = null,
    @SerialName("FirstName")
    val firstName: String? = null,
    @SerialName("LastName")
    val lastName: String? = null
)

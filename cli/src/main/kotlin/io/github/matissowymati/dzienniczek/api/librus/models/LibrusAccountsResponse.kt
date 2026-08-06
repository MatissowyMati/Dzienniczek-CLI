package io.github.matissowymati.dzienniczek.api.librus.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibrusAccountsResponse(
    @SerialName("accounts")
    val accounts: List<LibrusSynergiaAccount>,
    @SerialName("lastModification")
    val lastModification: Long? = null
)

@Serializable
data class LibrusSynergiaAccount(
    @SerialName("id")
    val id: Int,
    @SerialName("login")
    val login: String,
    @SerialName("studentName")
    val studentName: String,
    @SerialName("schoolName")
    val schoolName: String? = null,
    @SerialName("accessToken")
    val accessToken: String? = null,
    @SerialName("group")
    val group: String? = null,
    @SerialName("state")
    val state: String? = null
)

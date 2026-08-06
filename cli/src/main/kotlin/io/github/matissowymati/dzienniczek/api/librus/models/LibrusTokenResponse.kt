package io.github.matissowymati.dzienniczek.api.librus.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibrusTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("expires_in")
    val expiresIn: Int? = null,
    @SerialName("token_type")
    val token_type: String? = null,
    @SerialName("scope")
    val scope: String? = null
)

package io.github.matissowymati.dzienniczek.api.hebe.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PushSetting(
    @SerialName("Id") val id: Int,
    @SerialName("MobileCertyfikatId") val mobileCertificateId: Int,
    @SerialName("Option") val option: String,
    @SerialName("Active") val active: Boolean
)

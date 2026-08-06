package io.github.matissowymati.dzienniczek.api.librus.models.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibrusMessagesResponse(
    @SerialName("Messages")
    val messages: List<LibrusMessage>? = null
)

@Serializable
data class LibrusMessage(
    @SerialName("Id") val id: Int,
    @SerialName("Subject") val subject: String? = null,
    @SerialName("SentDate") val date: String? = null,
    @SerialName("Sender") val sender: LibrusIdNameReference? = null,
    @SerialName("Recipient") val recipient: LibrusIdNameReference? = null,
    @SerialName("IsRead") val isRead: Boolean? = null,
    @SerialName("HasAttachment") val hasAttachment: Boolean? = null,
    @SerialName("Content") val content: String? = null
)

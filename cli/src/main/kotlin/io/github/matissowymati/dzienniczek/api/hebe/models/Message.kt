@file:UseSerializers(VulcanDateTimeSerializer::class)

package io.github.matissowymati.dzienniczek.api.hebe.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class MessageAddressExtras(
    @SerialName("DisplayedClass") val displayedClass: String? = null
)

@Serializable
data class MessageAddress(
    @SerialName("GlobalKey") val globalKey: String,
    @SerialName("Name") val name: String,
    @SerialName("HasRead") val hasRead: Boolean? = null,
    @SerialName("Extras") val extras: MessageAddressExtras? = null
)

@Serializable
data class Message(
    @SerialName("Id") val id: String,
    @SerialName("GlobalKey") val globalKey: String,
    @SerialName("ThreadKey") val threadKey: String? = null,
    @SerialName("Subject") val subject: String? = null,
    @SerialName("Content") val content: String? = null,
    @SerialName("SentAt") val sentAt: LocalDateTime,
    @SerialName("ReadAt") val readAt: LocalDateTime? = null,
    @SerialName("Status") val status: Int? = null,
    @SerialName("Sender") val sender: MessageAddress? = null,
    @SerialName("Receiver") val receiver: List<MessageAddress> = emptyList(),
    @SerialName("Attachments") val attachments: List<Attachment> = emptyList(),
    @SerialName("Withdrawn") val withdrawn: Boolean = false
)

package io.github.matissowymati.dzienniczek.api.librus.models.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibrusNoticesResponse(
    @SerialName("Notes")
    val notices: List<LibrusNotice>
)

@Serializable
data class LibrusNotice(
    @SerialName("Id") val id: Long,
    @SerialName("Text") val text: String,
    @SerialName("Date") val date: String,
    @SerialName("Positive") val positive: Int? = null,
    @SerialName("Category") val category: LibrusIdReference? = null,
    @SerialName("Teacher") val teacher: LibrusIdReference? = null
)

@Serializable
data class LibrusNoticeCategoriesResponse(
    @SerialName("Categories")
    val categories: List<LibrusNoticeCategory>
)

@Serializable
data class LibrusNoticeCategory(
    @SerialName("Id") val id: Long,
    @SerialName("CategoryName") val name: String
)

package io.github.matissowymati.dzienniczek.api.librus.models.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibrusHomeWorksResponse(
    @SerialName("HomeWorks")
    val homeWorks: List<LibrusEvent>? = null
)

@Serializable
data class LibrusHomeWorkAssignmentsResponse(
    @SerialName("HomeWorkAssignments")
    val assignments: List<LibrusHomeWorkAssignment>? = null
)

@Serializable
data class LibrusHomeWorksCategoriesResponse(
    @SerialName("Categories")
    val categories: List<LibrusIdNameReference>
)

@Serializable
data class LibrusEvent(
    @SerialName("Id") val id: Long,
    @SerialName("Date") val date: String,
    @SerialName("Content") val content: String? = null,
    @SerialName("Category") val category: LibrusIdNameReference? = null,
    @SerialName("Subject") val subject: LibrusIdNameReference? = null,
    @SerialName("CreatedBy") val createdBy: LibrusIdReference? = null,
    @SerialName("AddDate") val addDate: String? = null,
    @SerialName("LessonNo") val lessonNo: Int? = null,
    @SerialName("TimeFrom") val timeFrom: String? = null,
    @SerialName("TimeTo") val timeTo: String? = null
)

@Serializable
data class LibrusHomeWorkAssignment(
    @SerialName("Id") val id: Long,
    @SerialName("Date") val date: String, // added date
    @SerialName("DueDate") val dueDate: String, // deadline
    @SerialName("Topic") val topic: String? = null,
    @SerialName("Text") val text: String? = null,
    @SerialName("Teacher") val teacher: LibrusIdReference? = null,
    @SerialName("Subject") val subject: LibrusIdReference? = null
)

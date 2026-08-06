package io.github.matissowymati.dzienniczek.api.librus.models.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LibrusTimetableResponse(
    @SerialName("Timetable")
    val timetable: Map<String, List<List<LibrusLesson>>>
)

@Serializable
data class LibrusLesson(
    @SerialName("LessonNo") val lessonNo: Int,
    @SerialName("HourFrom") val hourFrom: String,
    @SerialName("HourTo") val hourTo: String,
    @SerialName("Subject") val subject: LibrusIdNameReference? = null,
    @SerialName("Teacher") val teacher: LibrusIdNameReference? = null,
    @SerialName("Classroom") val classroom: LibrusIdNameReference? = null,
    @SerialName("IsSubstitutionClass") val isSubstitutionClass: Boolean = false,
    @SerialName("IsCanceled") val isCanceled: Boolean = false,
    @SerialName("OrgDate") val orgDate: String? = null,
    @SerialName("OrgLessonNo") val orgLessonNo: Int? = null,
    @SerialName("NewDate") val newDate: String? = null,
    @SerialName("NewLessonNo") val newLessonNo: Int? = null
)

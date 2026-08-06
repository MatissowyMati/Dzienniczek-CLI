package io.github.matissowymati.dzienniczek.api.librus.models.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibrusGradesResponse(
    @SerialName("Grades")
    val grades: List<LibrusGrade>
)

@Serializable
data class LibrusGrade(
    @SerialName("Id") val id: Long,
    @SerialName("Grade") val grade: String,
    @SerialName("Date") val date: String? = null,
    @SerialName("AddDate") val addDate: String? = null,
    @SerialName("Semester") val semester: Int,
    @SerialName("IsConstituent") val isConstituent: Boolean = false,
    @SerialName("IsSemester") val isSemester: Boolean = false,
    @SerialName("IsSemesterProposition") val isSemesterProposition: Boolean = false,
    @SerialName("IsFinal") val isFinal: Boolean = false,
    @SerialName("IsFinalProposition") val isFinalProposition: Boolean = false,
    @SerialName("Subject") val subject: LibrusIdNameReference,
    @SerialName("Category") val category: LibrusIdReference,
    @SerialName("AddedBy") val addedBy: LibrusIdReference? = null,
    @SerialName("Comments") val comments: List<LibrusIdReference>? = null
)

@Serializable
data class LibrusGradeCategoriesResponse(
    @SerialName("Categories")
    val categories: List<LibrusGradeCategory>
)

@Serializable
data class LibrusGradeCategory(
    @SerialName("Id") val id: Long,
    @SerialName("Name") val name: String,
    @SerialName("Weight") val weight: Float? = null,
    @SerialName("CountToTheAverage") val countToTheAverage: Boolean = false,
    @SerialName("Color") val color: LibrusColorReference? = null
)

@Serializable
data class LibrusIdReference(
    @SerialName("Id") val id: Long
)

@Serializable
data class LibrusIdNameReference(
    @SerialName("Id") val id: Long,
    @SerialName("Name") val name: String? = null
)

@Serializable
data class LibrusColorReference(
    @SerialName("Id") val id: Int? = null,
    @SerialName("RGB") val rgb: String? = null
)

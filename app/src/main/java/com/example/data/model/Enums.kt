package com.example.data.model

enum class UserRole(val value: String, val label: String) {
    STUDENT("student", "Student"),
    TEACHER("teacher", "Teacher"),
    ADMIN("admin", "Owner Admin");

    companion object {
        fun fromValue(raw: String): UserRole =
            entries.firstOrNull { it.value.equals(raw, ignoreCase = true) } ?: STUDENT
    }
}

enum class CodeStatus(val value: String, val label: String) {
    AVAILABLE("available", "Available"),
    ASSIGNED("assigned", "Assigned"),
    USED("used", "Used"),
    REVOKED("revoked", "Revoked");

    companion object {
        fun fromValue(raw: String): CodeStatus =
            entries.firstOrNull { it.value.equals(raw, ignoreCase = true) } ?: AVAILABLE
    }
}

enum class PaymentStatus(val value: String, val label: String) {
    PENDING("pending", "Pending Verification"),
    VERIFIED("verified", "Verified"),
    REJECTED("rejected", "Rejected");

    companion object {
        fun fromValue(raw: String): PaymentStatus =
            entries.firstOrNull { it.value.equals(raw, ignoreCase = true) } ?: PENDING
    }
}

object CourseCategories {
    val all = listOf(
        "Programming",
        "Mathematics",
        "Science",
        "UI/UX Design",
        "Business",
        "Languages",
        "Exam Prep"
    )
}

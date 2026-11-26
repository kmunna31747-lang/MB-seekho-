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
    AVAILABLE("available", "Unused"),
    ASSIGNED("assigned", "Unused (Assigned)"),
    USED("used", "Used"),
    DISABLED("disabled", "Disabled");

    companion object {
        fun fromValue(raw: String): CodeStatus = when (raw.trim().lowercase()) {
            "available", "unused" -> AVAILABLE
            "assigned" -> ASSIGNED
            "used" -> USED
            "disabled", "revoked" -> DISABLED
            else -> AVAILABLE
        }

        fun isUnused(raw: String): Boolean {
            val s = fromValue(raw)
            return s == AVAILABLE || s == ASSIGNED
        }
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
    const val FACEBOOK = "Facebook Course"
    const val YOUTUBE = "YouTube Course"
    const val SOCIAL_MEDIA = "Social Media Course"
    const val SNAPCHAT = "Snapchat Course"
    const val EARNING_APP = "Earning App Course"
    const val EARNING_WEBSITE = "Earning Website Course"
    const val OTHER = "Other Course"

    val all = listOf(
        FACEBOOK,
        YOUTUBE,
        SOCIAL_MEDIA,
        SNAPCHAT,
        EARNING_APP,
        EARNING_WEBSITE,
        OTHER
    )

    fun normalize(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        return all.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: OTHER
    }
}

enum class VideoType(val value: String, val label: String) {
    SHORT("short", "Short Video"),
    LONG("long", "Long Video");

    companion object {
        fun fromValue(raw: String?): VideoType = when (raw?.trim()?.lowercase()) {
            "short", "shorts" -> SHORT
            else -> LONG
        }
    }
}


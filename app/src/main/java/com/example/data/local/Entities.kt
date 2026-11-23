package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "access_codes",
    indices = [Index(value = ["codeHash"], unique = true)]
)
data class AccessCodeEntity(
    @PrimaryKey val codeId: String,
    val codeHash: String,
    val role: String, // "student" | "teacher"
    val status: String, // "available" | "assigned" | "used" | "revoked"
    val assignedToName: String? = null,
    val redeemedByUid: String? = null,
    val redeemedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val photoUrl: String? = null,
    val role: String, // "student" | "teacher" | "admin"
    val codeId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val active: Boolean = true,
    val totalCourses: Int = 0
)

@Entity(
    tableName = "courses",
    indices = [Index(value = ["teacherId"]), Index(value = ["published"])]
)
data class CourseEntity(
    @PrimaryKey val courseId: String,
    val title: String,
    val description: String,
    val category: String,
    val thumbnailUrl: String? = null,
    val videoUrl: String,
    val teacherId: String,
    val teacherName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val published: Boolean = true,
    val totalViews: Int = 0
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val paymentId: String,
    val customerName: String,
    val userRole: String, // "student" | "teacher"
    val paymentAmount: String, // Entered by owner; never invented
    val transactionRef: String,
    val verificationStatus: String, // "pending" | "verified" | "rejected"
    val assignedCodeId: String? = null,
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String? = null
)

@Entity(
    tableName = "saved_courses",
    primaryKeys = ["uid", "courseId"]
)
data class SavedCourseEntity(
    val uid: String,
    val courseId: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "course_progress",
    primaryKeys = ["uid", "courseId"]
)
data class CourseProgressEntity(
    val uid: String,
    val courseId: String,
    val watchedPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val progressPercent: Int = 0,
    val completed: Boolean = false,
    val lastWatchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "owner_security")
data class OwnerSecurityEntity(
    @PrimaryKey val id: Int = 1,
    val ownerUid: String,
    val ownerName: String,
    val saltHex: String,
    val passphraseHashHex: String,
    val claimedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "login_attempts")
data class LoginAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val portal: String, // "student" | "teacher" | "admin"
    val attemptedAt: Long = System.currentTimeMillis()
)

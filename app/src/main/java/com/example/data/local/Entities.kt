package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "access_codes",
    indices = [
        Index(value = ["codeHash"], unique = true),
        Index(value = ["role", "status"])
    ]
)
data class AccessCodeEntity(
    @PrimaryKey val codeId: String,
    val codeHash: String,
    val role: String, // "student" | "teacher"
    val status: String, // "available" (Unused) | "assigned" (Unused/Assigned) | "used" | "disabled" (legacy "revoked")
    val assignedToName: String? = null,
    val redeemedByUid: String? = null,
    val redeemedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val batchId: String? = null
)

/**
 * Audit log for every imported or generated batch of Student / Teacher Access Codes.
 * Supports unlimited batches (10,000, 50,000, 100,000+ codes) with deduplication tracking.
 */
@Entity(
    tableName = "code_batches",
    indices = [Index(value = ["role"]), Index(value = ["createdAt"])]
)
data class CodeBatchEntity(
    @PrimaryKey val batchId: String,
    val role: String, // "student" | "teacher"
    val sourceLabel: String,
    val totalSubmitted: Int,
    val insertedCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int,
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
    indices = [Index(value = ["teacherId"]), Index(value = ["published"]), Index(value = ["videoType"])]
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
    val totalViews: Int = 0,
    val videoType: String = "long", // "short" | "long"
    val durationMs: Long = 0L
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

/**
 * Validated video view records powering Teacher Studio 28-day analytics
 * and the anti-fraud 10 valid views = 1 coin reward engine.
 */
@Entity(
    tableName = "video_views",
    indices = [
        Index(value = ["videoId"]),
        Index(value = ["teacherId"]),
        Index(value = ["viewedAt"]),
        Index(value = ["videoId", "viewerId"])
    ]
)
data class VideoViewEntity(
    @PrimaryKey val viewId: String,
    val videoId: String,
    val teacherId: String,
    val viewerId: String,
    val viewedAt: Long = System.currentTimeMillis(),
    val validationStatus: String = "valid" // "valid"
)

/**
 * Server/repository-maintained teacher coin balance. Teachers can never edit directly.
 */
@Entity(tableName = "teacher_coins")
data class TeacherCoinsEntity(
    @PrimaryKey val teacherId: String,
    val totalCoins: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Immutable idempotent coin reward ledger. Each 10-view milestone produces exactly one
 * transaction keyed deterministically by teacher + milestone index.
 */
@Entity(
    tableName = "coin_transactions",
    indices = [Index(value = ["teacherId"]), Index(value = ["createdAt"])]
)
data class CoinTransactionEntity(
    @PrimaryKey val transactionId: String,
    val teacherId: String,
    val videoId: String,
    val viewsCount: Int,
    val coinsEarned: Int,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * One active like per student per video.
 */
@Entity(
    tableName = "video_likes",
    primaryKeys = ["videoId", "studentId"],
    indices = [Index(value = ["videoId"])]
)
data class VideoLikeEntity(
    val videoId: String,
    val studentId: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Student video comments with moderation support (student own delete, teacher video owner, admin).
 */
@Entity(
    tableName = "video_comments",
    indices = [Index(value = ["videoId"]), Index(value = ["studentId"]), Index(value = ["createdAt"])]
)
data class VideoCommentEntity(
    @PrimaryKey val commentId: String,
    val videoId: String,
    val studentId: String,
    val studentName: String,
    val studentPhoto: String? = null,
    val commentText: String,
    val createdAt: Long = System.currentTimeMillis()
)

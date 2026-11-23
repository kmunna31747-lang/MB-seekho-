package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.CodeStatus
import kotlinx.coroutines.flow.Flow

sealed interface AtomicRedemptionOutcome {
    data class Redeemed(val user: UserEntity, val codeId: String) : AtomicRedemptionOutcome
    data object NotFound : AtomicRedemptionOutcome
    data object WrongRole : AtomicRedemptionOutcome
    data object AlreadyUsed : AtomicRedemptionOutcome
    data object Revoked : AtomicRedemptionOutcome
}

@Dao
interface MbSeekhoDao {

    // --- Access Codes ---
    @Query("SELECT COUNT(*) FROM access_codes")
    suspend fun getAccessCodeCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccessCodes(codes: List<AccessCodeEntity>)

    @Query("SELECT * FROM access_codes ORDER BY role ASC, codeId ASC")
    fun observeAllAccessCodes(): Flow<List<AccessCodeEntity>>

    @Query("SELECT * FROM access_codes ORDER BY role ASC, codeId ASC")
    suspend fun getAllAccessCodesOnce(): List<AccessCodeEntity>

    @Query("SELECT * FROM access_codes WHERE codeHash = :codeHash LIMIT 1")
    suspend fun findAccessCodeByHash(codeHash: String): AccessCodeEntity?

    @Query("SELECT * FROM access_codes WHERE codeId = :codeId LIMIT 1")
    suspend fun findAccessCodeById(codeId: String): AccessCodeEntity?

    @Query("SELECT COUNT(*) FROM access_codes WHERE role = :role")
    suspend fun countCodesByRole(role: String): Int

    @Query(
        "UPDATE access_codes SET status = :status, assignedToName = :assignedToName WHERE codeId = :codeId AND status IN ('available', 'assigned')"
    )
    suspend fun assignCode(codeId: String, status: String, assignedToName: String): Int

    @Query("UPDATE access_codes SET status = 'revoked' WHERE codeId = :codeId AND status IN ('available', 'assigned')")
    suspend fun revokeUnusedCode(codeId: String): Int

    @Query(
        "UPDATE access_codes SET status = 'used', redeemedByUid = :uid, redeemedAt = :redeemedAt WHERE codeId = :codeId AND status IN ('available', 'assigned')"
    )
    suspend fun markCodeUsedIfAvailable(codeId: String, uid: String, redeemedAt: Long): Int

    @Transaction
    suspend fun redeemCodeAtomically(
        codeHash: String,
        expectedRole: String,
        displayName: String,
        nowMillis: Long
    ): AtomicRedemptionOutcome {
        val code = findAccessCodeByHash(codeHash) ?: return AtomicRedemptionOutcome.NotFound
        if (!code.role.equals(expectedRole, ignoreCase = true)) {
            return AtomicRedemptionOutcome.WrongRole
        }
        when (CodeStatus.fromValue(code.status)) {
            CodeStatus.USED -> return AtomicRedemptionOutcome.AlreadyUsed
            CodeStatus.REVOKED -> return AtomicRedemptionOutcome.Revoked
            CodeStatus.AVAILABLE, CodeStatus.ASSIGNED -> Unit
        }

        val prefix = if (expectedRole.equals("teacher", ignoreCase = true)) "TCH" else "STU"
        val uid = CryptoUtils.generateUniqueUid(prefix)
        val rowsUpdated = markCodeUsedIfAvailable(code.codeId, uid, nowMillis)
        if (rowsUpdated != 1) {
            return AtomicRedemptionOutcome.AlreadyUsed
        }

        val resolvedName = displayName.trim().ifBlank {
            code.assignedToName?.takeIf { it.isNotBlank() }
                ?: if (expectedRole == "teacher") "Teacher ${code.codeId}" else "Student ${code.codeId}"
        }
        val newUser = UserEntity(
            uid = uid,
            name = resolvedName,
            photoUrl = null,
            role = expectedRole,
            codeId = code.codeId,
            createdAt = nowMillis,
            active = true,
            totalCourses = 0
        )
        insertUser(newUser)
        return AtomicRedemptionOutcome.Redeemed(newUser, code.codeId)
    }

    // --- Users ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun observeUserById(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE role = :role ORDER BY createdAt DESC")
    fun observeUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("UPDATE users SET name = :name WHERE uid = :uid")
    suspend fun updateUserName(uid: String, name: String)

    @Query("UPDATE users SET photoUrl = :photoUrl WHERE uid = :uid")
    suspend fun updateUserPhoto(uid: String, photoUrl: String?)

    @Query("UPDATE users SET active = :active WHERE uid = :uid")
    suspend fun updateUserActive(uid: String, active: Boolean)

    @Query("UPDATE users SET totalCourses = (SELECT COUNT(*) FROM courses WHERE teacherId = :teacherId) WHERE uid = :teacherId")
    suspend fun syncTeacherCourseCount(teacherId: String)

    // --- Courses ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Query("SELECT * FROM courses WHERE courseId = :courseId LIMIT 1")
    suspend fun getCourseById(courseId: String): CourseEntity?

    @Query("SELECT * FROM courses WHERE courseId = :courseId LIMIT 1")
    fun observeCourseById(courseId: String): Flow<CourseEntity?>

    @Query("SELECT * FROM courses WHERE published = 1 ORDER BY createdAt DESC")
    fun observePublishedCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY createdAt DESC")
    fun observeAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun observeTeacherCourses(teacherId: String): Flow<List<CourseEntity>>

    @Query("DELETE FROM courses WHERE courseId = :courseId")
    suspend fun deleteCourseById(courseId: String)

    @Query("UPDATE courses SET totalViews = totalViews + 1 WHERE courseId = :courseId")
    suspend fun incrementCourseViews(courseId: String)

    // --- Saved Courses ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCourse(savedCourse: SavedCourseEntity)

    @Query("DELETE FROM saved_courses WHERE uid = :uid AND courseId = :courseId")
    suspend fun unsaveCourse(uid: String, courseId: String)

    @Query("SELECT * FROM saved_courses WHERE uid = :uid ORDER BY savedAt DESC")
    fun observeSavedCourses(uid: String): Flow<List<SavedCourseEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_courses WHERE uid = :uid AND courseId = :courseId)")
    suspend fun isCourseSaved(uid: String, courseId: String): Boolean

    // --- Course Progress ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourseProgress(progress: CourseProgressEntity)

    @Query("SELECT * FROM course_progress WHERE uid = :uid ORDER BY lastWatchedAt DESC")
    fun observeCourseProgress(uid: String): Flow<List<CourseProgressEntity>>

    @Query("SELECT * FROM course_progress WHERE uid = :uid AND courseId = :courseId LIMIT 1")
    suspend fun getCourseProgress(uid: String, courseId: String): CourseProgressEntity?

    // --- Payments ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun observeAllPayments(): Flow<List<PaymentEntity>>

    @Query("UPDATE payments SET verificationStatus = :status, assignedCodeId = :assignedCodeId WHERE paymentId = :paymentId")
    suspend fun updatePaymentVerification(paymentId: String, status: String, assignedCodeId: String?)

    // --- Owner Security ---
    @Query("SELECT * FROM owner_security WHERE id = 1 LIMIT 1")
    suspend fun getOwnerSecurity(): OwnerSecurityEntity?

    @Query("SELECT * FROM owner_security WHERE id = 1 LIMIT 1")
    fun observeOwnerSecurity(): Flow<OwnerSecurityEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnerSecurity(security: OwnerSecurityEntity)

    // --- Rate Limiting ---
    @Insert
    suspend fun recordFailedAttempt(attempt: LoginAttemptEntity)

    @Query("SELECT COUNT(*) FROM login_attempts WHERE portal = :portal AND attemptedAt >= :sinceMillis")
    suspend fun countRecentFailedAttempts(portal: String, sinceMillis: Long): Int

    @Query("DELETE FROM login_attempts WHERE portal = :portal")
    suspend fun clearFailedAttempts(portal: String)
}

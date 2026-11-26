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
    data object Disabled : AtomicRedemptionOutcome
}

sealed interface ValidViewOutcome {
    data class Recorded(
        val validViewsTotalForTeacher: Int,
        val coinsAwardedNow: Int,
        val newTeacherCoinBalance: Int
    ) : ValidViewOutcome
    data object DuplicateWithinCooldown : ValidViewOutcome
    data object SelfViewIgnored : ValidViewOutcome
    data object CourseUnavailable : ValidViewOutcome
}

data class CodeRoleStats(
    val total: Int = 0,
    val unused: Int = 0,
    val assigned: Int = 0,
    val used: Int = 0,
    val disabled: Int = 0
)

@Dao
interface MbSeekhoDao {

    // --- Access Codes (Scalable to 100,000+ Student & Teacher Codes) ---
    @Query("SELECT COUNT(*) FROM access_codes")
    suspend fun getAccessCodeCount(): Int

    @Query("SELECT COUNT(*) FROM access_codes WHERE role = :role")
    suspend fun countCodesByRole(role: String): Int

    @Query(
        """
        SELECT
            COUNT(*) AS total,
            COALESCE(SUM(CASE WHEN status IN ('available', 'assigned') THEN 1 ELSE 0 END), 0) AS unused,
            COALESCE(SUM(CASE WHEN status = 'assigned' THEN 1 ELSE 0 END), 0) AS assigned,
            COALESCE(SUM(CASE WHEN status = 'used' THEN 1 ELSE 0 END), 0) AS used,
            COALESCE(SUM(CASE WHEN status IN ('disabled', 'revoked') THEN 1 ELSE 0 END), 0) AS disabled
        FROM access_codes
        WHERE role = :role
        """
    )
    fun observeCodeStatsByRole(role: String): Flow<CodeRoleStats>

    @Query(
        """
        SELECT * FROM access_codes
        WHERE role = :role
          AND (
            :statusFilter = 'all'
            OR (:statusFilter = 'unused' AND status IN ('available', 'assigned'))
            OR (:statusFilter = 'assigned' AND status = 'assigned')
            OR (:statusFilter = 'used' AND status = 'used')
            OR (:statusFilter = 'disabled' AND status IN ('disabled', 'revoked'))
          )
          AND (
            :searchPattern = ''
            OR codeId LIKE :searchPattern
            OR COALESCE(assignedToName, '') LIKE :searchPattern
            OR COALESCE(redeemedByUid, '') LIKE :searchPattern
            OR COALESCE(batchId, '') LIKE :searchPattern
          )
        ORDER BY createdAt DESC, codeId ASC
        LIMIT :limit
        """
    )
    fun observeFilteredCodes(
        role: String,
        statusFilter: String,
        searchPattern: String,
        limit: Int
    ): Flow<List<AccessCodeEntity>>

    @Query("SELECT * FROM access_codes WHERE role = :role AND status = 'used' ORDER BY redeemedAt DESC LIMIT :limit")
    fun observeRecentUsedCodes(role: String, limit: Int): Flow<List<AccessCodeEntity>>

    @Query("SELECT * FROM access_codes WHERE status = 'used' ORDER BY redeemedAt DESC LIMIT :limit")
    fun observeAllRecentUsedCodes(limit: Int): Flow<List<AccessCodeEntity>>

    @Query("SELECT * FROM access_codes WHERE role = :role AND status IN ('available', 'assigned') ORDER BY codeId ASC LIMIT :limit")
    fun observeAvailableCodesByRole(role: String, limit: Int): Flow<List<AccessCodeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccessCodes(codes: List<AccessCodeEntity>): List<Long>

    @Query("SELECT codeHash FROM access_codes WHERE codeHash IN (:hashes)")
    suspend fun findExistingHashes(hashes: List<String>): List<String>

    @Query("SELECT codeId FROM access_codes WHERE codeId IN (:codeIds)")
    suspend fun findExistingCodeIds(codeIds: List<String>): List<String>

    @Query("SELECT * FROM access_codes ORDER BY role ASC, codeId ASC")
    fun observeAllAccessCodes(): Flow<List<AccessCodeEntity>>

    @Query("SELECT * FROM access_codes ORDER BY role ASC, codeId ASC")
    suspend fun getAllAccessCodesOnce(): List<AccessCodeEntity>

    @Query("SELECT * FROM access_codes WHERE role = :role ORDER BY codeId ASC LIMIT :limit OFFSET :offset")
    suspend fun getCodesPageByRole(role: String, limit: Int, offset: Int): List<AccessCodeEntity>

    @Query("SELECT * FROM access_codes WHERE codeHash = :codeHash LIMIT 1")
    suspend fun findAccessCodeByHash(codeHash: String): AccessCodeEntity?

    @Query("SELECT * FROM access_codes WHERE codeId = :codeId LIMIT 1")
    suspend fun findAccessCodeById(codeId: String): AccessCodeEntity?

    @Query(
        "UPDATE access_codes SET status = :status, assignedToName = :assignedToName WHERE codeId = :codeId AND status IN ('available', 'assigned')"
    )
    suspend fun assignCode(codeId: String, status: String, assignedToName: String): Int

    @Query("UPDATE access_codes SET status = 'disabled' WHERE codeId = :codeId AND status IN ('available', 'assigned')")
    suspend fun disableUnusedCode(codeId: String): Int

    @Query("UPDATE access_codes SET status = 'available' WHERE codeId = :codeId AND status IN ('disabled', 'revoked')")
    suspend fun enableDisabledCode(codeId: String): Int

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
            CodeStatus.DISABLED -> return AtomicRedemptionOutcome.Disabled
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
        if (expectedRole.equals("teacher", ignoreCase = true)) {
            insertTeacherCoinsIfAbsent(
                TeacherCoinsEntity(
                    teacherId = uid,
                    totalCoins = 0,
                    updatedAt = nowMillis
                )
            )
        }
        return AtomicRedemptionOutcome.Redeemed(newUser, code.codeId)
    }

    // --- Access Code Batches (Unlimited Import & Generation Audit Log) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCodeBatch(batch: CodeBatchEntity)

    @Query("SELECT * FROM code_batches ORDER BY createdAt DESC LIMIT 50")
    fun observeCodeBatches(): Flow<List<CodeBatchEntity>>

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

    // --- Valid Video Views & Atomic 10-Views-Per-Coin Reward Engine ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideoView(view: VideoViewEntity): Long

    @Query("SELECT COUNT(*) FROM video_views WHERE videoId = :videoId AND viewerId = :viewerId AND validationStatus = 'valid' AND viewedAt >= :sinceMillis")
    suspend fun countRecentValidViewsByViewer(videoId: String, viewerId: String, sinceMillis: Long): Int

    @Query("SELECT COUNT(*) FROM video_views WHERE teacherId = :teacherId AND validationStatus = 'valid'")
    suspend fun countTotalValidViewsForTeacher(teacherId: String): Int

    @Query("SELECT * FROM video_views WHERE teacherId = :teacherId AND validationStatus = 'valid' ORDER BY viewedAt DESC")
    fun observeValidViewsForTeacher(teacherId: String): Flow<List<VideoViewEntity>>

    @Query("SELECT * FROM video_views WHERE validationStatus = 'valid' ORDER BY viewedAt DESC")
    fun observeAllValidViews(): Flow<List<VideoViewEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTeacherCoinsIfAbsent(coins: TeacherCoinsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeacherCoins(coins: TeacherCoinsEntity)

    @Query("SELECT * FROM teacher_coins WHERE teacherId = :teacherId LIMIT 1")
    suspend fun getTeacherCoinsOnce(teacherId: String): TeacherCoinsEntity?

    @Query("SELECT * FROM teacher_coins WHERE teacherId = :teacherId LIMIT 1")
    fun observeTeacherCoins(teacherId: String): Flow<TeacherCoinsEntity?>

    @Query("SELECT * FROM teacher_coins ORDER BY totalCoins DESC")
    fun observeAllTeacherCoins(): Flow<List<TeacherCoinsEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCoinTransaction(tx: CoinTransactionEntity): Long

    @Query("SELECT * FROM coin_transactions WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    fun observeCoinTransactionsForTeacher(teacherId: String): Flow<List<CoinTransactionEntity>>

    @Query("SELECT * FROM coin_transactions ORDER BY createdAt DESC")
    fun observeAllCoinTransactions(): Flow<List<CoinTransactionEntity>>

    @Query("SELECT COALESCE(SUM(coinsEarned), 0) FROM coin_transactions WHERE teacherId = :teacherId")
    suspend fun sumEarnedCoinsForTeacher(teacherId: String): Int

    @Transaction
    suspend fun recordValidVideoViewAtomically(
        videoId: String,
        viewerId: String,
        nowMillis: Long,
        cooldownMs: Long
    ): ValidViewOutcome {
        val course = getCourseById(videoId)
        if (course == null || !course.published) {
            return ValidViewOutcome.CourseUnavailable
        }
        if (viewerId.isBlank() || viewerId == course.teacherId) {
            return ValidViewOutcome.SelfViewIgnored
        }

        val recentSameViewer = countRecentValidViewsByViewer(
            videoId = videoId,
            viewerId = viewerId,
            sinceMillis = nowMillis - cooldownMs
        )
        if (recentSameViewer > 0) {
            return ValidViewOutcome.DuplicateWithinCooldown
        }

        insertVideoView(
            VideoViewEntity(
                viewId = CryptoUtils.generateUniqueUid("VW"),
                videoId = videoId,
                teacherId = course.teacherId,
                viewerId = viewerId,
                viewedAt = nowMillis,
                validationStatus = "valid"
            )
        )
        incrementCourseViews(videoId)

        val totalValidViews = countTotalValidViewsForTeacher(course.teacherId)
        val eligibleMilestones = totalValidViews / 10
        var coinsAwardedNow = 0

        for (milestone in 1..eligibleMilestones) {
            val deterministicTxId = "COIN_${course.teacherId}_M$milestone"
            val rowId = insertCoinTransaction(
                CoinTransactionEntity(
                    transactionId = deterministicTxId,
                    teacherId = course.teacherId,
                    videoId = videoId,
                    viewsCount = milestone * 10,
                    coinsEarned = 1,
                    createdAt = nowMillis
                )
            )
            if (rowId != -1L) {
                coinsAwardedNow += 1
            }
        }

        val newBalance = sumEarnedCoinsForTeacher(course.teacherId)
        upsertTeacherCoins(
            TeacherCoinsEntity(
                teacherId = course.teacherId,
                totalCoins = newBalance,
                updatedAt = nowMillis
            )
        )

        return ValidViewOutcome.Recorded(
            validViewsTotalForTeacher = totalValidViews,
            coinsAwardedNow = coinsAwardedNow,
            newTeacherCoinBalance = newBalance
        )
    }

    // --- Video Likes (One Active Like Per Student Per Video) ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideoLike(like: VideoLikeEntity): Long

    @Query("DELETE FROM video_likes WHERE videoId = :videoId AND studentId = :studentId")
    suspend fun deleteVideoLike(videoId: String, studentId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM video_likes WHERE videoId = :videoId AND studentId = :studentId)")
    suspend fun isVideoLikedByStudent(videoId: String, studentId: String): Boolean

    @Query("SELECT * FROM video_likes WHERE videoId = :videoId")
    fun observeLikesForVideo(videoId: String): Flow<List<VideoLikeEntity>>

    @Query("SELECT * FROM video_likes")
    fun observeAllVideoLikes(): Flow<List<VideoLikeEntity>>

    // --- Video Comments ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoComment(comment: VideoCommentEntity)

    @Query("SELECT * FROM video_comments WHERE commentId = :commentId LIMIT 1")
    suspend fun getCommentById(commentId: String): VideoCommentEntity?

    @Query("DELETE FROM video_comments WHERE commentId = :commentId")
    suspend fun deleteCommentById(commentId: String)

    @Query("SELECT * FROM video_comments WHERE videoId = :videoId ORDER BY createdAt DESC")
    fun observeCommentsForVideo(videoId: String): Flow<List<VideoCommentEntity>>

    @Query("SELECT * FROM video_comments ORDER BY createdAt DESC")
    fun observeAllComments(): Flow<List<VideoCommentEntity>>

    @Query("SELECT COUNT(*) FROM video_comments WHERE studentId = :studentId AND createdAt >= :sinceMillis")
    suspend fun countRecentCommentsByStudent(studentId: String, sinceMillis: Long): Int

    @Query("SELECT * FROM video_comments WHERE videoId = :videoId AND studentId = :studentId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestCommentByStudentOnVideo(videoId: String, studentId: String): VideoCommentEntity?

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

package com.example.data.repository

import com.example.data.local.AccessCodeEntity
import com.example.data.local.AtomicRedemptionOutcome
import com.example.data.local.CodeBatchEntity
import com.example.data.local.CodeBatchParser
import com.example.data.local.CodeRoleStats
import com.example.data.local.CoinTransactionEntity
import com.example.data.local.CourseEntity
import com.example.data.local.CourseProgressEntity
import com.example.data.local.CryptoUtils
import com.example.data.local.LoginAttemptEntity
import com.example.data.local.MbSeekhoDao
import com.example.data.local.MbSeekhoDatabase
import com.example.data.local.OwnerSecurityEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.SavedCourseEntity
import com.example.data.local.TeacherCoinsEntity
import com.example.data.local.UserEntity
import com.example.data.local.ValidViewOutcome
import com.example.data.local.VideoCommentEntity
import com.example.data.local.VideoLikeEntity
import com.example.data.local.VideoViewEntity
import com.example.data.model.CodeStatus
import com.example.data.model.CourseCategories
import com.example.data.model.PaymentStatus
import com.example.data.model.UserRole
import com.example.data.model.VideoType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface CodeRedemptionResult {
    data class Success(val user: UserEntity) : CodeRedemptionResult
    data object InvalidCode : CodeRedemptionResult
    data object AlreadyUsed : CodeRedemptionResult
    data object Disabled : CodeRedemptionResult
    data object WrongPortal : CodeRedemptionResult
    data object AccountDisabled : CodeRedemptionResult
    data class RateLimited(val waitSeconds: Int) : CodeRedemptionResult
}

data class GeneratedCodeReveal(
    val codeId: String,
    val rawCode: String,
    val role: UserRole
)

data class GeneratedBatchResult(
    val batchId: String,
    val role: UserRole,
    val generatedCount: Int,
    val sampleReveals: List<GeneratedCodeReveal>,
    val csvContent: String
)

data class CodeImportSummary(
    val batchId: String,
    val role: UserRole,
    val sourceLabel: String,
    val totalSubmitted: Int,
    val insertedCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int
)

data class TeacherStudioStats(
    val teacherId: String = "",
    val viewsLast28Days: Int = 0,
    val videosUploadedLast28Days: Int = 0,
    val coinsEarnedLast28Days: Int = 0,
    val totalCoinBalance: Int = 0,
    val totalValidViewsAllTime: Int = 0,
    val viewsTowardNextCoin: Int = 0,
    val recentTransactions: List<CoinTransactionEntity> = emptyList(),
    val topVideosLast28Days: List<TeacherVideoPerformance> = emptyList(),
    val windowStartMillis: Long = 0L,
    val windowEndMillis: Long = 0L
)

data class TeacherVideoPerformance(
    val courseId: String,
    val title: String,
    val category: String,
    val viewsLast28Days: Int,
    val totalViews: Int,
    val createdAt: Long
)

class MbSeekhoRepository(private val dao: MbSeekhoDao) {

    companion object {
        private const val RATE_LIMIT_WINDOW_MS = 60_000L
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val DB_CHUNK_SIZE = 500
        private const val MAX_SINGLE_GEN_BATCH = 50_000 // Safety bound per single operation; unlimited cumulative batches!
        const val VIEW_COOLDOWN_MS = 24L * 60L * 60L * 1000L // 1 valid view per viewer per video per 24h
        const val STUDIO_WINDOW_DAYS = 28L
        const val STUDIO_WINDOW_MS = STUDIO_WINDOW_DAYS * 24L * 60L * 60L * 1000L
        private const val COMMENT_RATE_WINDOW_MS = 60_000L
        private const val MAX_COMMENTS_PER_MINUTE = 5
        private const val DUPLICATE_COMMENT_WINDOW_MS = 30_000L
    }

    suspend fun initializeInventory() {
        MbSeekhoDatabase.ensureInitialHashesSeeded(dao)
    }

    // --- Student & Teacher Access Code Validation (SHA-256 Atomic Single-Use) ---

    suspend fun redeemStudentAccessCode(
        rawCodeInput: String,
        displayName: String = ""
    ): CodeRedemptionResult = redeemAccessCode(rawCodeInput, UserRole.STUDENT, displayName)

    suspend fun redeemTeacherAccessCode(
        rawCodeInput: String,
        displayName: String = ""
    ): CodeRedemptionResult = redeemAccessCode(rawCodeInput, UserRole.TEACHER, displayName)

    suspend fun redeemAccessCode(
        rawCodeInput: String,
        expectedRole: UserRole,
        displayName: String = ""
    ): CodeRedemptionResult {
        require(expectedRole == UserRole.STUDENT || expectedRole == UserRole.TEACHER) {
            "Access code login is only valid for Student or Teacher portals."
        }
        initializeInventory()
        val portal = expectedRole.value
        val now = System.currentTimeMillis()
        val recentFailures = dao.countRecentFailedAttempts(portal, now - RATE_LIMIT_WINDOW_MS)
        if (recentFailures >= MAX_FAILED_ATTEMPTS) {
            return CodeRedemptionResult.RateLimited(waitSeconds = 60)
        }

        val normalized = CryptoUtils.normalizeCode(rawCodeInput)
        if (normalized.length < 8) {
            dao.recordFailedAttempt(LoginAttemptEntity(portal = portal, attemptedAt = now))
            return CodeRedemptionResult.InvalidCode
        }

        val hash = CryptoUtils.sha256Hex(normalized)
        return when (val outcome = dao.redeemCodeAtomically(hash, portal, displayName, now)) {
            is AtomicRedemptionOutcome.Redeemed -> {
                dao.clearFailedAttempts(portal)
                CodeRedemptionResult.Success(outcome.user)
            }
            AtomicRedemptionOutcome.NotFound -> {
                dao.recordFailedAttempt(LoginAttemptEntity(portal = portal, attemptedAt = now))
                CodeRedemptionResult.InvalidCode
            }
            AtomicRedemptionOutcome.WrongRole -> {
                dao.recordFailedAttempt(LoginAttemptEntity(portal = portal, attemptedAt = now))
                CodeRedemptionResult.WrongPortal
            }
            AtomicRedemptionOutcome.AlreadyUsed -> {
                dao.recordFailedAttempt(LoginAttemptEntity(portal = portal, attemptedAt = now))
                CodeRedemptionResult.AlreadyUsed
            }
            AtomicRedemptionOutcome.Disabled -> {
                dao.recordFailedAttempt(LoginAttemptEntity(portal = portal, attemptedAt = now))
                CodeRedemptionResult.Disabled
            }
        }
    }

    suspend fun signInReturningMember(uidInput: String, expectedRole: UserRole): CodeRedemptionResult {
        val now = System.currentTimeMillis()
        val recentFailures = dao.countRecentFailedAttempts(expectedRole.value, now - RATE_LIMIT_WINDOW_MS)
        if (recentFailures >= MAX_FAILED_ATTEMPTS) {
            return CodeRedemptionResult.RateLimited(waitSeconds = 60)
        }
        val user = dao.getUserById(uidInput.trim().uppercase())
        if (user == null || user.role != expectedRole.value) {
            dao.recordFailedAttempt(LoginAttemptEntity(portal = expectedRole.value, attemptedAt = now))
            return CodeRedemptionResult.InvalidCode
        }
        if (!user.active) {
            return CodeRedemptionResult.AccountDisabled
        }
        dao.clearFailedAttempts(expectedRole.value)
        return CodeRedemptionResult.Success(user)
    }

    suspend fun getUserById(uid: String): UserEntity? = dao.getUserById(uid)

    fun observeUser(uid: String): Flow<UserEntity?> = dao.observeUserById(uid)

    // --- Owner Admin Security (No Hardcoded Credentials) ---

    fun observeOwnerSecurity(): Flow<OwnerSecurityEntity?> = dao.observeOwnerSecurity()

    suspend fun claimOwnerAdmin(ownerName: String, passphrase: String): Result<UserEntity> {
        if (dao.getOwnerSecurity() != null) {
            return Result.failure(IllegalStateException("Owner admin account has already been claimed."))
        }
        if (passphrase.length < 6) {
            return Result.failure(IllegalArgumentException("Owner passphrase must be at least 6 characters."))
        }
        val cleanName = ownerName.trim().ifBlank { "MB Seekho Owner" }
        val salt = CryptoUtils.generateSaltHex()
        val hash = CryptoUtils.hashOwnerPassphrase(passphrase, salt)
        val ownerUid = CryptoUtils.generateUniqueUid("OWN")
        val now = System.currentTimeMillis()

        val ownerUser = UserEntity(
            uid = ownerUid,
            name = cleanName,
            role = UserRole.ADMIN.value,
            codeId = null,
            createdAt = now,
            active = true
        )
        dao.insertUser(ownerUser)
        dao.insertOwnerSecurity(
            OwnerSecurityEntity(
                id = 1,
                ownerUid = ownerUid,
                ownerName = cleanName,
                saltHex = salt,
                passphraseHashHex = hash,
                claimedAt = now
            )
        )
        return Result.success(ownerUser)
    }

    suspend fun verifyOwnerAdmin(passphrase: String): Result<UserEntity> {
        val now = System.currentTimeMillis()
        val recentFailures = dao.countRecentFailedAttempts(UserRole.ADMIN.value, now - RATE_LIMIT_WINDOW_MS)
        if (recentFailures >= MAX_FAILED_ATTEMPTS) {
            return Result.failure(SecurityException("Too many failed attempts. Wait 60 seconds and try again."))
        }
        val security = dao.getOwnerSecurity()
            ?: return Result.failure(IllegalStateException("Owner account has not been claimed yet."))
        val valid = CryptoUtils.verifyOwnerPassphrase(passphrase, security.saltHex, security.passphraseHashHex)
        if (!valid) {
            dao.recordFailedAttempt(LoginAttemptEntity(portal = UserRole.ADMIN.value, attemptedAt = now))
            return Result.failure(SecurityException("Invalid owner credentials."))
        }
        dao.clearFailedAttempts(UserRole.ADMIN.value)
        val owner = dao.getUserById(security.ownerUid)
            ?: UserEntity(
                uid = security.ownerUid,
                name = security.ownerName,
                role = UserRole.ADMIN.value,
                active = true
            ).also { dao.insertUser(it) }
        return Result.success(owner)
    }

    private suspend fun requireAdmin(actorUid: String) {
        val actor = dao.getUserById(actorUid)
        if (actor == null || actor.role != UserRole.ADMIN.value || !actor.active) {
            throw SecurityException("Unauthorized: Owner Admin permission required.")
        }
    }

    private suspend fun requireActiveTeacher(actorUid: String): UserEntity {
        val actor = dao.getUserById(actorUid)
        if (actor == null || actor.role != UserRole.TEACHER.value || !actor.active) {
            throw SecurityException("Unauthorized: Verified active Teacher permission required.")
        }
        return actor
    }

    private suspend fun requireActiveUser(actorUid: String): UserEntity {
        val actor = dao.getUserById(actorUid)
        if (actor == null || !actor.active) {
            throw SecurityException("Unauthorized: Active account required.")
        }
        return actor
    }

    // --- Profile Management ---

    suspend fun updateProfileName(uid: String, newName: String): Result<Unit> = runCatching {
        val clean = newName.trim()
        require(clean.length in 2..60) { "Name must be between 2 and 60 characters." }
        dao.updateUserName(uid, clean)
    }

    suspend fun updateProfilePhoto(uid: String, photoUri: String?): Result<Unit> = runCatching {
        dao.updateUserPhoto(uid, photoUri)
    }

    // --- Student Courses, Library & Progress ---

    fun observePublishedCourses(): Flow<List<CourseEntity>> = dao.observePublishedCourses()

    fun observeSavedCourses(uid: String): Flow<List<SavedCourseEntity>> = dao.observeSavedCourses(uid)

    fun observeCourseProgress(uid: String): Flow<List<CourseProgressEntity>> = dao.observeCourseProgress(uid)

    suspend fun toggleSaveCourse(uid: String, courseId: String): Boolean {
        val currentlySaved = dao.isCourseSaved(uid, courseId)
        return if (currentlySaved) {
            dao.unsaveCourse(uid, courseId)
            false
        } else {
            dao.saveCourse(SavedCourseEntity(uid = uid, courseId = courseId))
            true
        }
    }

    suspend fun recordValidatedVideoView(
        videoId: String,
        viewerUid: String,
        nowMillis: Long = System.currentTimeMillis()
    ): ValidViewOutcome {
        val viewer = dao.getUserById(viewerUid) ?: return ValidViewOutcome.CourseUnavailable
        if (!viewer.active) return ValidViewOutcome.CourseUnavailable
        return dao.recordValidVideoViewAtomically(
            videoId = videoId,
            viewerId = viewer.uid,
            nowMillis = nowMillis,
            cooldownMs = VIEW_COOLDOWN_MS
        )
    }

    suspend fun updateCourseProgress(
        uid: String,
        courseId: String,
        positionMs: Long,
        durationMs: Long
    ) {
        val safeDuration = durationMs.coerceAtLeast(1L)
        val safePos = positionMs.coerceIn(0L, safeDuration)
        val percent = ((safePos.toDouble() / safeDuration.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        val existing = dao.getCourseProgress(uid, courseId)
        val completed = (existing?.completed == true) || percent >= 90
        dao.upsertCourseProgress(
            CourseProgressEntity(
                uid = uid,
                courseId = courseId,
                watchedPositionMs = safePos,
                durationMs = safeDuration,
                progressPercent = if (completed) 100 else percent,
                completed = completed,
                lastWatchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markCourseCompleted(uid: String, courseId: String) {
        val existing = dao.getCourseProgress(uid, courseId)
        dao.upsertCourseProgress(
            CourseProgressEntity(
                uid = uid,
                courseId = courseId,
                watchedPositionMs = existing?.durationMs ?: 1000L,
                durationMs = existing?.durationMs ?: 1000L,
                progressPercent = 100,
                completed = true,
                lastWatchedAt = System.currentTimeMillis()
            )
        )
    }

    // --- Video Likes (One Active Like Per Student Per Video) ---

    fun observeLikesForVideo(videoId: String): Flow<List<VideoLikeEntity>> =
        dao.observeLikesForVideo(videoId)

    fun observeAllVideoLikes(): Flow<List<VideoLikeEntity>> =
        dao.observeAllVideoLikes()

    suspend fun toggleVideoLike(videoId: String, studentUid: String): Result<Boolean> = runCatching {
        val user = requireActiveUser(studentUid)
        val course = dao.getCourseById(videoId) ?: error("Video not found.")
        require(course.published) { "Only published videos can be liked." }
        val alreadyLiked = dao.isVideoLikedByStudent(videoId, user.uid)
        if (alreadyLiked) {
            dao.deleteVideoLike(videoId, user.uid)
            false
        } else {
            dao.insertVideoLike(
                VideoLikeEntity(
                    videoId = videoId,
                    studentId = user.uid,
                    createdAt = System.currentTimeMillis()
                )
            )
            true
        }
    }

    // --- Video Comments (Anti-Spam + Role-Based Moderation) ---

    fun observeCommentsForVideo(videoId: String): Flow<List<VideoCommentEntity>> =
        dao.observeCommentsForVideo(videoId)

    fun observeAllVideoComments(): Flow<List<VideoCommentEntity>> =
        dao.observeAllComments()

    fun observeAllCommentsForAdmin(): Flow<List<VideoCommentEntity>> =
        dao.observeAllComments()

    suspend fun postVideoComment(
        videoId: String,
        actorUid: String,
        rawText: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Result<VideoCommentEntity> = runCatching {
        val user = requireActiveUser(actorUid)
        val course = dao.getCourseById(videoId) ?: error("Video not found.")
        require(course.published) { "Cannot comment on an unpublished video." }

        val cleanText = rawText.trim().replace(Regex("\\s+"), " ")
        require(cleanText.isNotEmpty()) { "Please write a comment before posting." }
        require(cleanText.length >= 2) { "Comment must be at least 2 characters long." }
        require(cleanText.length <= 500) { "Comment cannot exceed 500 characters." }

        val recentCount = dao.countRecentCommentsByStudent(user.uid, nowMillis - COMMENT_RATE_WINDOW_MS)
        require(recentCount < MAX_COMMENTS_PER_MINUTE) {
            "You are posting too quickly. Please wait a moment before commenting again."
        }

        val latestSameVideo = dao.getLatestCommentByStudentOnVideo(videoId, user.uid)
        if (latestSameVideo != null &&
            latestSameVideo.commentText.equals(cleanText, ignoreCase = true) &&
            (nowMillis - latestSameVideo.createdAt) < DUPLICATE_COMMENT_WINDOW_MS
        ) {
            throw IllegalArgumentException("Duplicate comment detected. Please write something new.")
        }

        val comment = VideoCommentEntity(
            commentId = CryptoUtils.generateUniqueUid("CMT"),
            videoId = videoId,
            studentId = user.uid,
            studentName = user.name,
            studentPhoto = user.photoUrl,
            commentText = cleanText,
            createdAt = nowMillis
        )
        dao.insertVideoComment(comment)
        comment
    }

    suspend fun deleteVideoComment(actorUid: String, commentId: String): Result<Unit> = runCatching {
        val actor = requireActiveUser(actorUid)
        val comment = dao.getCommentById(commentId) ?: error("Comment not found.")
        val course = dao.getCourseById(comment.videoId)

        val isOwnComment = actor.uid == comment.studentId
        val isOwningTeacher = actor.role == UserRole.TEACHER.value && course?.teacherId == actor.uid
        val isAdmin = actor.role == UserRole.ADMIN.value

        if (!isOwnComment && !isOwningTeacher && !isAdmin) {
            throw SecurityException("You do not have permission to delete this comment.")
        }
        dao.deleteCommentById(commentId)
    }

    // --- Teacher Course Management & Teacher Studio (28-Day Analytics) ---

    fun observeTeacherCourses(teacherId: String): Flow<List<CourseEntity>> =
        dao.observeTeacherCourses(teacherId)

    fun observeTeacherStudioStats(
        teacherUid: String,
        nowMillisProvider: () -> Long = { System.currentTimeMillis() }
    ): Flow<TeacherStudioStats> = combine(
        dao.observeValidViewsForTeacher(teacherUid),
        dao.observeTeacherCourses(teacherUid),
        dao.observeTeacherCoins(teacherUid),
        dao.observeCoinTransactionsForTeacher(teacherUid)
    ) { validViews, courses, coinsEntity, transactions ->
        val now = nowMillisProvider()
        val windowStart = now - STUDIO_WINDOW_MS

        val viewsIn28Days = validViews.filter { it.viewedAt >= windowStart }
        val videosIn28Days = courses.count { it.createdAt >= windowStart }
        val coinsIn28Days = transactions
            .filter { it.createdAt >= windowStart }
            .sumOf { it.coinsEarned }
        val totalBalance = coinsEntity?.totalCoins ?: transactions.sumOf { it.coinsEarned }

        val viewsByVideo28d = viewsIn28Days.groupingBy { it.videoId }.eachCount()
        val topVideos = courses
            .map { course ->
                TeacherVideoPerformance(
                    courseId = course.courseId,
                    title = course.title,
                    category = CourseCategories.normalize(course.category),
                    viewsLast28Days = viewsByVideo28d[course.courseId] ?: 0,
                    totalViews = course.totalViews,
                    createdAt = course.createdAt
                )
            }
            .sortedWith(compareByDescending<TeacherVideoPerformance> { it.viewsLast28Days }.thenByDescending { it.createdAt })

        TeacherStudioStats(
            teacherId = teacherUid,
            viewsLast28Days = viewsIn28Days.size,
            videosUploadedLast28Days = videosIn28Days,
            coinsEarnedLast28Days = coinsIn28Days,
            totalCoinBalance = totalBalance,
            totalValidViewsAllTime = validViews.size,
            viewsTowardNextCoin = validViews.size % 10,
            recentTransactions = transactions,
            topVideosLast28Days = topVideos,
            windowStartMillis = windowStart,
            windowEndMillis = now
        )
    }

    suspend fun publishCourse(
        teacherUid: String,
        title: String,
        description: String,
        category: String = CourseCategories.OTHER,
        thumbnailUrl: String? = null,
        videoUrl: String,
        published: Boolean = true,
        videoType: String = VideoType.LONG.value,
        durationMs: Long = 0L,
        createdAtMillis: Long = System.currentTimeMillis()
    ): Result<CourseEntity> = runCatching {
        val teacher = requireActiveTeacher(teacherUid)
        val cleanTitle = title.trim()
        val cleanDesc = description.trim()
        val cleanVideo = videoUrl.trim()
        require(cleanTitle.isNotEmpty()) { "Please enter a video title." }
        require(cleanTitle.length in 3..120) { "Video title must be between 3 and 120 characters." }
        require(cleanDesc.isNotEmpty()) { "Please enter a video description." }
        require(cleanDesc.length in 5..1000) { "Video description must be at least 5 characters." }
        require(cleanVideo.isNotBlank()) { "Please select a video from your gallery first." }

        val normalizedCategory = CourseCategories.normalize(category)
        val normalizedVideoType = VideoType.fromValue(videoType).value
        val course = CourseEntity(
            courseId = CryptoUtils.generateUniqueUid("CRS"),
            title = cleanTitle,
            description = cleanDesc,
            category = normalizedCategory,
            thumbnailUrl = thumbnailUrl?.takeIf { it.isNotBlank() },
            videoUrl = cleanVideo,
            teacherId = teacher.uid,
            teacherName = teacher.name,
            createdAt = createdAtMillis,
            published = published,
            totalViews = 0,
            videoType = normalizedVideoType,
            durationMs = durationMs.coerceAtLeast(0L)
        )
        dao.insertCourse(course)
        dao.syncTeacherCourseCount(teacher.uid)
        course
    }

    suspend fun updateTeacherCourse(
        teacherUid: String,
        courseId: String,
        title: String,
        description: String,
        category: String,
        published: Boolean
    ): Result<Unit> = runCatching {
        val teacher = requireActiveTeacher(teacherUid)
        val existing = dao.getCourseById(courseId) ?: error("Course not found.")
        if (existing.teacherId != teacher.uid) {
            throw SecurityException("Teachers can only edit their own videos.")
        }
        dao.updateCourse(
            existing.copy(
                title = title.trim().ifBlank { existing.title },
                description = description.trim().ifBlank { existing.description },
                category = CourseCategories.normalize(category),
                published = published
            )
        )
    }

    suspend fun deleteTeacherCourse(teacherUid: String, courseId: String): Result<Unit> = runCatching {
        val teacher = requireActiveTeacher(teacherUid)
        val existing = dao.getCourseById(courseId) ?: error("Course not found.")
        if (existing.teacherId != teacher.uid) {
            throw SecurityException("Teachers can only delete their own videos.")
        }
        dao.deleteCourseById(courseId)
        dao.syncTeacherCourseCount(teacher.uid)
    }

    // --- Owner Admin Operations (Scalable Unlimited Student & Teacher Code Management) ---

    fun observeCodeStats(role: UserRole): Flow<CodeRoleStats> =
        dao.observeCodeStatsByRole(role.value)

    fun observeFilteredCodes(
        role: UserRole,
        statusFilter: String,
        searchQuery: String,
        limit: Int
    ): Flow<List<AccessCodeEntity>> {
        val trimmed = searchQuery.trim()
        val pattern = if (trimmed.isEmpty()) "" else "%$trimmed%"
        return dao.observeFilteredCodes(
            role = role.value,
            statusFilter = statusFilter.lowercase(),
            searchPattern = pattern,
            limit = limit.coerceIn(25, 1000)
        )
    }

    fun observeCodeBatches(): Flow<List<CodeBatchEntity>> = dao.observeCodeBatches()

    fun observeRecentRedemptions(limit: Int = 15): Flow<List<AccessCodeEntity>> =
        dao.observeAllRecentUsedCodes(limit)

    fun observeAvailableCodesByRole(role: UserRole, limit: Int = 50): Flow<List<AccessCodeEntity>> =
        dao.observeAvailableCodesByRole(role.value, limit)

    fun observeAllUsers(): Flow<List<UserEntity>> = dao.observeAllUsers()

    fun observeAllCoursesForAdmin(): Flow<List<CourseEntity>> = dao.observeAllCourses()

    fun observeAllPayments(): Flow<List<PaymentEntity>> = dao.observeAllPayments()

    fun observeAllTeacherCoinsForAdmin(): Flow<List<TeacherCoinsEntity>> = dao.observeAllTeacherCoins()

    fun observeAllCoinTransactionsForAdmin(): Flow<List<CoinTransactionEntity>> = dao.observeAllCoinTransactions()

    fun observeAllValidViewsForAdmin(): Flow<List<VideoViewEntity>> = dao.observeAllValidViews()

    suspend fun assignAccessCode(
        adminUid: String,
        codeId: String,
        customerName: String
    ): Result<Unit> = runCatching {
        requireAdmin(adminUid)
        val cleanCustomer = customerName.trim()
        require(cleanCustomer.isNotBlank()) { "Customer name is required to assign a code." }
        val updated = dao.assignCode(codeId, CodeStatus.ASSIGNED.value, cleanCustomer)
        require(updated == 1) { "Code $codeId is not available for assignment." }
    }

    suspend fun disableAccessCode(adminUid: String, codeId: String): Result<Unit> = runCatching {
        requireAdmin(adminUid)
        val rows = dao.disableUnusedCode(codeId)
        require(rows == 1) { "Only unused codes can be disabled." }
    }

    suspend fun enableAccessCode(adminUid: String, codeId: String): Result<Unit> = runCatching {
        requireAdmin(adminUid)
        val rows = dao.enableDisabledCode(codeId)
        require(rows == 1) { "Only disabled codes can be re-enabled." }
    }

    /**
     * Imports any size CSV or TXT file (1,000, 10,000, 50,000, 100,000+ codes) in memory-safe
     * 500-row chunks. Computes SHA-256 hashes on the fly, prevents duplicates across all existing
     * and future batches, and logs an immutable batch audit record.
     */
    suspend fun importAccessCodesFromLines(
        adminUid: String,
        role: UserRole,
        sourceLabel: String,
        lines: Sequence<String>,
        onProgress: (processedCount: Int) -> Unit = {}
    ): Result<CodeImportSummary> = runCatching {
        requireAdmin(adminUid)
        require(role == UserRole.STUDENT || role == UserRole.TEACHER) {
            "Codes can only be imported for Student or Teacher roles."
        }

        val now = System.currentTimeMillis()
        val batchId = CryptoUtils.generateUniqueUid("BATCH")
        val idPrefix = if (role == UserRole.STUDENT) "STU" else "TCH"
        var seqCounter = dao.countCodesByRole(role.value)

        var totalSubmitted = 0
        var insertedCount = 0
        var duplicateCount = 0
        var invalidCount = 0

        val seenHashesInRun = HashSet<String>(4096)
        val seenIdsInRun = HashSet<String>(4096)
        val pendingChunk = ArrayList<Pair<String?, String>>(DB_CHUNK_SIZE) // (suggestedId, codeHash)

        suspend fun flushChunk() {
            if (pendingChunk.isEmpty()) return
            val existingHashes = dao.findExistingHashes(pendingChunk.map { it.second }).toHashSet()
            val candidateIds = pendingChunk.mapNotNull { it.first }.distinct()
            val existingIds = if (candidateIds.isNotEmpty()) {
                dao.findExistingCodeIds(candidateIds).toHashSet()
            } else {
                emptySet()
            }

            val entitiesToInsert = ArrayList<AccessCodeEntity>(pendingChunk.size)
            for ((suggestedId, hash) in pendingChunk) {
                if (existingHashes.contains(hash)) {
                    duplicateCount += 1
                    continue
                }
                var chosenId = if (suggestedId != null &&
                    !existingIds.contains(suggestedId) &&
                    !seenIdsInRun.contains(suggestedId)
                ) {
                    suggestedId
                } else {
                    seqCounter += 1
                    formatSequentialCodeId(idPrefix, seqCounter)
                }
                while (existingIds.contains(chosenId) ||
                    seenIdsInRun.contains(chosenId) ||
                    (chosenId != suggestedId && dao.findAccessCodeById(chosenId) != null)
                ) {
                    seqCounter += 1
                    chosenId = formatSequentialCodeId(idPrefix, seqCounter)
                }
                seenIdsInRun.add(chosenId)

                entitiesToInsert += AccessCodeEntity(
                    codeId = chosenId,
                    codeHash = hash,
                    role = role.value,
                    status = CodeStatus.AVAILABLE.value,
                    createdAt = now,
                    batchId = batchId
                )
            }

            if (entitiesToInsert.isNotEmpty()) {
                val results = dao.insertAccessCodes(entitiesToInsert)
                val actuallyInserted = results.count { it != -1L }
                insertedCount += actuallyInserted
                duplicateCount += (entitiesToInsert.size - actuallyInserted)
            }

            pendingChunk.clear()
            onProgress(totalSubmitted)
        }

        for (line in lines) {
            when (val parsed = CodeBatchParser.parseLine(line)) {
                CodeBatchParser.LineParseResult.Ignored -> Unit
                CodeBatchParser.LineParseResult.Invalid -> {
                    invalidCount += 1
                }
                is CodeBatchParser.LineParseResult.Parsed -> {
                    totalSubmitted += 1
                    val hash = CryptoUtils.sha256Hex(parsed.entry.normalizedRawCode)
                    if (!seenHashesInRun.add(hash)) {
                        duplicateCount += 1
                    } else {
                        pendingChunk += parsed.entry.suggestedCodeId to hash
                        if (pendingChunk.size >= DB_CHUNK_SIZE) {
                            flushChunk()
                        }
                    }
                }
            }
        }
        flushChunk()

        require(totalSubmitted > 0) {
            "No valid access codes were found in the file. Use formats like STU0001 MBST-XXXX-XXXX or one code per line."
        }

        val cleanLabel = sourceLabel.trim().ifBlank { "Manual Import" }.take(80)
        dao.insertCodeBatch(
            CodeBatchEntity(
                batchId = batchId,
                role = role.value,
                sourceLabel = cleanLabel,
                totalSubmitted = totalSubmitted,
                insertedCount = insertedCount,
                duplicateCount = duplicateCount,
                invalidCount = invalidCount,
                createdAt = now
            )
        )

        CodeImportSummary(
            batchId = batchId,
            role = role,
            sourceLabel = cleanLabel,
            totalSubmitted = totalSubmitted,
            insertedCount = insertedCount,
            duplicateCount = duplicateCount,
            invalidCount = invalidCount
        )
    }

    /**
     * Generates a new unlimited batch of cryptographically unique Student or Teacher Access Codes.
     * Enforces global uniqueness across all existing and future batches (no 10,000 total cap).
     * Stores ONLY SHA-256 hashes in the database; returns raw codes ONCE for private export/distribution.
     */
    suspend fun generateAccessCodesBatch(
        adminUid: String,
        role: UserRole,
        count: Int,
        batchLabel: String? = null,
        onProgress: (generatedSoFar: Int) -> Unit = {}
    ): Result<GeneratedBatchResult> = runCatching {
        requireAdmin(adminUid)
        require(role == UserRole.STUDENT || role == UserRole.TEACHER) {
            "Codes can only be generated for Student or Teacher roles."
        }
        require(count in 1..MAX_SINGLE_GEN_BATCH) {
            "Generate between 1 and $MAX_SINGLE_GEN_BATCH codes per batch (unlimited batches supported)."
        }

        val now = System.currentTimeMillis()
        val batchId = CryptoUtils.generateUniqueUid("BATCH")
        val idPrefix = if (role == UserRole.STUDENT) "STU" else "TCH"
        val rawPrefix = if (role == UserRole.STUDENT) "MBST" else "MBTC"
        var seqCounter = dao.countCodesByRole(role.value)

        val seenHashes = HashSet<String>(count.coerceAtMost(16_384))
        val usedIdsInRun = HashSet<String>(count.coerceAtMost(16_384))
        val sampleReveals = ArrayList<GeneratedCodeReveal>(25)
        val csvBuilder = StringBuilder(count * 32).apply {
            appendLine("codeId,accessCode,role,batchId")
        }

        var totalGenerated = 0
        while (totalGenerated < count) {
            val targetChunkSize = minOf(DB_CHUNK_SIZE, count - totalGenerated)
            val candidates = ArrayList<Pair<String, String>>(targetChunkSize) // (rawCode, hash)

            var safetyAttempts = 0
            while (candidates.size < targetChunkSize && safetyAttempts < targetChunkSize * 10) {
                safetyAttempts += 1
                val raw = CryptoUtils.generateRawAccessCode(rawPrefix)
                val hash = CryptoUtils.sha256Hex(raw)
                if (seenHashes.add(hash)) {
                    candidates += raw to hash
                }
            }

            val existingHashes = dao.findExistingHashes(candidates.map { it.second }).toHashSet()
            val uniqueCandidates = candidates.filterNot { existingHashes.contains(it.second) }
            if (uniqueCandidates.isEmpty()) continue

            val candidateIds = ArrayList<String>(uniqueCandidates.size)
            val startSeq = seqCounter
            for (i in 1..uniqueCandidates.size) {
                candidateIds += formatSequentialCodeId(idPrefix, startSeq + i)
            }
            val existingIds = dao.findExistingCodeIds(candidateIds).toHashSet()

            val entities = ArrayList<AccessCodeEntity>(uniqueCandidates.size)
            val chunkReveals = ArrayList<GeneratedCodeReveal>(uniqueCandidates.size)

            for ((raw, hash) in uniqueCandidates) {
                seqCounter += 1
                var codeId = formatSequentialCodeId(idPrefix, seqCounter)
                while (existingIds.contains(codeId) ||
                    usedIdsInRun.contains(codeId) ||
                    (seqCounter > startSeq + uniqueCandidates.size && dao.findAccessCodeById(codeId) != null)
                ) {
                    seqCounter += 1
                    codeId = formatSequentialCodeId(idPrefix, seqCounter)
                }
                usedIdsInRun.add(codeId)
                entities += AccessCodeEntity(
                    codeId = codeId,
                    codeHash = hash,
                    role = role.value,
                    status = CodeStatus.AVAILABLE.value,
                    createdAt = now,
                    batchId = batchId
                )
                chunkReveals += GeneratedCodeReveal(codeId = codeId, rawCode = raw, role = role)
            }

            val insertResults = dao.insertAccessCodes(entities)
            insertResults.forEachIndexed { idx, rowId ->
                if (rowId != -1L) {
                    val reveal = chunkReveals[idx]
                    totalGenerated += 1
                    if (sampleReveals.size < 25) {
                        sampleReveals += reveal
                    }
                    csvBuilder.appendLine(
                        "${reveal.codeId},${reveal.rawCode},${role.value},$batchId"
                    )
                }
            }
            onProgress(totalGenerated)
        }

        val label = batchLabel?.trim()?.takeIf { it.isNotBlank() }
            ?: "Admin Generated (${role.label} × $totalGenerated)"
        dao.insertCodeBatch(
            CodeBatchEntity(
                batchId = batchId,
                role = role.value,
                sourceLabel = label.take(80),
                totalSubmitted = count,
                insertedCount = totalGenerated,
                duplicateCount = 0,
                invalidCount = 0,
                createdAt = now
            )
        )

        GeneratedBatchResult(
            batchId = batchId,
            role = role,
            generatedCount = totalGenerated,
            sampleReveals = sampleReveals,
            csvContent = csvBuilder.toString()
        )
    }

    private fun formatSequentialCodeId(prefix: String, number: Int): String {
        val width = if (prefix == "STU") 4 else 3
        return "$prefix${number.toString().padStart(width, '0')}"
    }

    suspend fun setUserActiveStatus(adminUid: String, targetUid: String, active: Boolean): Result<Unit> =
        runCatching {
            requireAdmin(adminUid)
            require(targetUid != adminUid) { "Owner cannot disable their own account." }
            dao.updateUserActive(targetUid, active)
        }

    suspend fun removeCourseAsAdmin(adminUid: String, courseId: String): Result<Unit> = runCatching {
        requireAdmin(adminUid)
        val existing = dao.getCourseById(courseId) ?: error("Course not found.")
        dao.deleteCourseById(courseId)
        dao.syncTeacherCourseCount(existing.teacherId)
    }

    suspend fun updateCourseCategoryAsAdmin(
        adminUid: String,
        courseId: String,
        newCategory: String
    ): Result<Unit> = runCatching {
        requireAdmin(adminUid)
        val existing = dao.getCourseById(courseId) ?: error("Course not found.")
        dao.updateCourse(existing.copy(category = CourseCategories.normalize(newCategory)))
    }

    suspend fun recordManualPayment(
        adminUid: String,
        customerName: String,
        userRole: UserRole,
        paymentAmount: String,
        transactionRef: String,
        notes: String? = null
    ): Result<PaymentEntity> = runCatching {
        requireAdmin(adminUid)
        val cleanName = customerName.trim()
        val cleanAmount = paymentAmount.trim()
        val cleanRef = transactionRef.trim()
        require(cleanName.isNotBlank()) { "Customer name is required." }
        require(cleanAmount.isNotBlank()) { "Payment amount must be specified by the owner." }
        require(cleanRef.isNotBlank()) { "Transaction reference is required." }

        val payment = PaymentEntity(
            paymentId = CryptoUtils.generateUniqueUid("PAY"),
            customerName = cleanName,
            userRole = userRole.value,
            paymentAmount = cleanAmount,
            transactionRef = cleanRef,
            verificationStatus = PaymentStatus.PENDING.value,
            assignedCodeId = null,
            paymentDate = System.currentTimeMillis(),
            notes = notes?.trim()?.takeIf { it.isNotBlank() }
        )
        dao.insertPayment(payment)
        payment
    }

    suspend fun verifyOrRejectPayment(
        adminUid: String,
        paymentId: String,
        status: PaymentStatus,
        assignedCodeId: String?,
        customerName: String
    ): Result<Unit> = runCatching {
        requireAdmin(adminUid)
        if (status == PaymentStatus.VERIFIED && !assignedCodeId.isNullOrBlank()) {
            val code = dao.findAccessCodeById(assignedCodeId)
                ?: error("Selected access code $assignedCodeId was not found.")
            require(CodeStatus.isUnused(code.status)) {
                "Selected access code $assignedCodeId is not unused."
            }
            dao.assignCode(assignedCodeId, CodeStatus.ASSIGNED.value, customerName)
        }
        dao.updatePaymentVerification(
            paymentId = paymentId,
            status = status.value,
            assignedCodeId = if (status == PaymentStatus.VERIFIED) assignedCodeId else null
        )
    }

    suspend fun exportAccessCodesCsv(adminUid: String, role: UserRole): Result<String> = runCatching {
        requireAdmin(adminUid)
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("codeId,role,status,batchId,assignedToName,redeemedByUid,redeemedAt,sha256Prefix")

        var offset = 0
        val pageSize = 1000
        while (true) {
            val page = dao.getCodesPageByRole(role.value, pageSize, offset)
            if (page.isEmpty()) break
            for (c in page) {
                val redeemedText = c.redeemedAt?.let { df.format(Date(it)) } ?: ""
                val displayStatus = CodeStatus.fromValue(c.status).label
                sb.appendLine(
                    listOf(
                        CryptoUtils.escapeCsvField(c.codeId),
                        CryptoUtils.escapeCsvField(c.role),
                        CryptoUtils.escapeCsvField(displayStatus),
                        CryptoUtils.escapeCsvField(c.batchId),
                        CryptoUtils.escapeCsvField(c.assignedToName),
                        CryptoUtils.escapeCsvField(c.redeemedByUid),
                        CryptoUtils.escapeCsvField(redeemedText),
                        CryptoUtils.escapeCsvField(c.codeHash.take(12))
                    ).joinToString(",")
                )
            }
            offset += page.size
            if (page.size < pageSize) break
        }
        sb.toString()
    }
}

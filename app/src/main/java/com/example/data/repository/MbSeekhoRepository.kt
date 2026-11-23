package com.example.data.repository

import com.example.data.local.AccessCodeEntity
import com.example.data.local.AtomicRedemptionOutcome
import com.example.data.local.CourseEntity
import com.example.data.local.CourseProgressEntity
import com.example.data.local.CryptoUtils
import com.example.data.local.LoginAttemptEntity
import com.example.data.local.MbSeekhoDao
import com.example.data.local.MbSeekhoDatabase
import com.example.data.local.OwnerSecurityEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.SavedCourseEntity
import com.example.data.local.UserEntity
import com.example.data.model.CodeStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface CodeRedemptionResult {
    data class Success(val user: UserEntity) : CodeRedemptionResult
    data object InvalidCode : CodeRedemptionResult
    data object AlreadyUsed : CodeRedemptionResult
    data object Revoked : CodeRedemptionResult
    data object WrongPortal : CodeRedemptionResult
    data object AccountDisabled : CodeRedemptionResult
    data class RateLimited(val waitSeconds: Int) : CodeRedemptionResult
}

data class GeneratedCodeReveal(
    val codeId: String,
    val rawCode: String,
    val role: UserRole
)

class MbSeekhoRepository(private val dao: MbSeekhoDao) {

    companion object {
        private const val RATE_LIMIT_WINDOW_MS = 60_000L
        private const val MAX_FAILED_ATTEMPTS = 5
    }

    suspend fun initializeInventory() {
        MbSeekhoDatabase.ensureInitialHashesSeeded(dao)
    }

    // --- Authentication & Private Access Code Validation ---

    suspend fun redeemAccessCode(
        rawCodeInput: String,
        expectedRole: UserRole,
        displayName: String = ""
    ): CodeRedemptionResult {
        initializeInventory()
        require(expectedRole == UserRole.STUDENT || expectedRole == UserRole.TEACHER) {
            "Access codes are strictly for student or teacher portals."
        }

        val now = System.currentTimeMillis()
        val recentFailures = dao.countRecentFailedAttempts(expectedRole.value, now - RATE_LIMIT_WINDOW_MS)
        if (recentFailures >= MAX_FAILED_ATTEMPTS) {
            return CodeRedemptionResult.RateLimited(waitSeconds = 60)
        }

        val normalized = CryptoUtils.normalizeCode(rawCodeInput)
        if (normalized.length < 8) {
            dao.recordFailedAttempt(LoginAttemptEntity(portal = expectedRole.value, attemptedAt = now))
            return CodeRedemptionResult.InvalidCode
        }

        val hash = CryptoUtils.sha256Hex(normalized)
        return when (val outcome = dao.redeemCodeAtomically(hash, expectedRole.value, displayName, now)) {
            is AtomicRedemptionOutcome.Redeemed -> {
                dao.clearFailedAttempts(expectedRole.value)
                CodeRedemptionResult.Success(outcome.user)
            }
            AtomicRedemptionOutcome.NotFound -> {
                dao.recordFailedAttempt(LoginAttemptEntity(portal = expectedRole.value, attemptedAt = now))
                CodeRedemptionResult.InvalidCode
            }
            AtomicRedemptionOutcome.WrongRole -> {
                dao.recordFailedAttempt(LoginAttemptEntity(portal = expectedRole.value, attemptedAt = now))
                CodeRedemptionResult.WrongPortal
            }
            AtomicRedemptionOutcome.AlreadyUsed -> {
                dao.recordFailedAttempt(LoginAttemptEntity(portal = expectedRole.value, attemptedAt = now))
                CodeRedemptionResult.AlreadyUsed
            }
            AtomicRedemptionOutcome.Revoked -> {
                dao.recordFailedAttempt(LoginAttemptEntity(portal = expectedRole.value, attemptedAt = now))
                CodeRedemptionResult.Revoked
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

    suspend fun isOwnerClaimed(): Boolean = dao.getOwnerSecurity() != null

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

    fun observeCourseById(courseId: String): Flow<CourseEntity?> = dao.observeCourseById(courseId)

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

    suspend fun recordCourseView(courseId: String) {
        dao.incrementCourseViews(courseId)
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

    // --- Teacher Course Management ---

    fun observeTeacherCourses(teacherId: String): Flow<List<CourseEntity>> =
        dao.observeTeacherCourses(teacherId)

    suspend fun publishCourse(
        teacherUid: String,
        title: String,
        description: String,
        category: String,
        thumbnailUrl: String?,
        videoUrl: String,
        published: Boolean = true
    ): Result<CourseEntity> = runCatching {
        val teacher = requireActiveTeacher(teacherUid)
        val cleanTitle = title.trim()
        val cleanDesc = description.trim()
        val cleanVideo = videoUrl.trim()
        require(cleanTitle.length in 3..120) { "Course title must be 3 to 120 characters." }
        require(cleanDesc.length in 5..1000) { "Course description must be at least 5 characters." }
        require(cleanVideo.isNotBlank()) { "Please select a video file or provide a valid video URL." }

        val course = CourseEntity(
            courseId = CryptoUtils.generateUniqueUid("CRS"),
            title = cleanTitle,
            description = cleanDesc,
            category = category.ifBlank { "Programming" },
            thumbnailUrl = thumbnailUrl?.takeIf { it.isNotBlank() },
            videoUrl = cleanVideo,
            teacherId = teacher.uid,
            teacherName = teacher.name,
            createdAt = System.currentTimeMillis(),
            published = published,
            totalViews = 0
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
            throw SecurityException("Teachers can only edit their own courses.")
        }
        dao.updateCourse(
            existing.copy(
                title = title.trim().ifBlank { existing.title },
                description = description.trim().ifBlank { existing.description },
                category = category.ifBlank { existing.category },
                published = published
            )
        )
    }

    suspend fun deleteTeacherCourse(teacherUid: String, courseId: String): Result<Unit> = runCatching {
        val teacher = requireActiveTeacher(teacherUid)
        val existing = dao.getCourseById(courseId) ?: error("Course not found.")
        if (existing.teacherId != teacher.uid) {
            throw SecurityException("Teachers can only delete their own courses.")
        }
        dao.deleteCourseById(courseId)
        dao.syncTeacherCourseCount(teacher.uid)
    }

    // --- Owner Admin Operations (Strictly Privileged) ---

    fun observeAllAccessCodes(): Flow<List<AccessCodeEntity>> = dao.observeAllAccessCodes()

    fun observeAllUsers(): Flow<List<UserEntity>> = dao.observeAllUsers()

    fun observeAllCoursesForAdmin(): Flow<List<CourseEntity>> = dao.observeAllCourses()

    fun observeAllPayments(): Flow<List<PaymentEntity>> = dao.observeAllPayments()

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

    suspend fun generateAdditionalCodes(
        adminUid: String,
        role: UserRole,
        count: Int
    ): Result<List<GeneratedCodeReveal>> = runCatching {
        requireAdmin(adminUid)
        require(role == UserRole.STUDENT || role == UserRole.TEACHER) { "Invalid code role." }
        require(count in 1..25) { "Generate between 1 and 25 codes at a time." }

        val existingCount = dao.countCodesByRole(role.value)
        val idPrefix = if (role == UserRole.STUDENT) "STU" else "TCH"
        val rawPrefix = if (role == UserRole.STUDENT) "MBST" else "MBTC"
        val now = System.currentTimeMillis()

        val entities = mutableListOf<AccessCodeEntity>()
        val reveals = mutableListOf<GeneratedCodeReveal>()

        for (i in 1..count) {
            val number = existingCount + i
            val codeId = "$idPrefix${number.toString().padStart(3, '0')}"
            val rawCode = CryptoUtils.generateRawAccessCode(rawPrefix)
            val hash = CryptoUtils.sha256Hex(rawCode)
            entities += AccessCodeEntity(
                codeId = codeId,
                codeHash = hash,
                role = role.value,
                status = CodeStatus.AVAILABLE.value,
                createdAt = now
            )
            reveals += GeneratedCodeReveal(codeId = codeId, rawCode = rawCode, role = role)
        }
        dao.insertAccessCodes(entities)
        reveals
    }

    suspend fun revokeUnusedCode(adminUid: String, codeId: String): Result<Unit> = runCatching {
        requireAdmin(adminUid)
        val rows = dao.revokeUnusedCode(codeId)
        require(rows == 1) { "Only unused codes can be revoked." }
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
            verificationStatus = PaymentStatus.PENDING.value, // NEVER auto-verified
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
            require(code.status == CodeStatus.AVAILABLE.value || code.status == CodeStatus.ASSIGNED.value) {
                "Selected access code $assignedCodeId is not available."
            }
            dao.assignCode(assignedCodeId, CodeStatus.ASSIGNED.value, customerName)
        }
        dao.updatePaymentVerification(
            paymentId = paymentId,
            status = status.value,
            assignedCodeId = if (status == PaymentStatus.VERIFIED) assignedCodeId else null
        )
    }

    suspend fun exportAccessCodesCsv(adminUid: String): Result<String> = runCatching {
        requireAdmin(adminUid)
        val codes = dao.getAllAccessCodesOnce()
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        buildString {
            appendLine("codeId,role,status,assignedToName,redeemedByUid,redeemedAt,sha256Prefix")
            for (c in codes) {
                val redeemedText = c.redeemedAt?.let { df.format(Date(it)) } ?: ""
                appendLine(
                    listOf(
                        CryptoUtils.escapeCsvField(c.codeId),
                        CryptoUtils.escapeCsvField(c.role),
                        CryptoUtils.escapeCsvField(c.status),
                        CryptoUtils.escapeCsvField(c.assignedToName),
                        CryptoUtils.escapeCsvField(c.redeemedByUid),
                        CryptoUtils.escapeCsvField(redeemedText),
                        CryptoUtils.escapeCsvField(c.codeHash.take(12))
                    ).joinToString(",")
                )
            }
        }
    }
}

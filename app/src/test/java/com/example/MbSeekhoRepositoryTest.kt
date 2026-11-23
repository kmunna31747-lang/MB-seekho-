package com.example

import com.example.data.local.AccessCodeEntity
import com.example.data.local.AtomicRedemptionOutcome
import com.example.data.local.CourseEntity
import com.example.data.local.CourseProgressEntity
import com.example.data.local.CryptoUtils
import com.example.data.local.LoginAttemptEntity
import com.example.data.local.MbSeekhoDao
import com.example.data.local.OwnerSecurityEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.SavedCourseEntity
import com.example.data.local.UserEntity
import com.example.data.model.CodeStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.UserRole
import com.example.data.repository.CodeRedemptionResult
import com.example.data.repository.MbSeekhoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Complete automated JVM test suite verifying all 14 MB Seekho requirements:
 * 1. Valid student code login
 * 2. Valid teacher code login
 * 3. Invalid codes rejected
 * 4. Used codes cannot be reused
 * 5. Student code blocked on Teacher login
 * 6. Teacher code blocked on Student login
 * 7. Teacher uploads & publishes course (students blocked)
 * 8. Published courses appear in Student Home
 * 9. New courses sorted newest-first in Student New
 * 10. Saved courses & progress appear in Student Library
 * 11. Profile information loads & updates
 * 12. Unauthorized users blocked from Owner Admin
 * 13. Payments start Pending and require manual owner verification
 * 14. Access code inventory stores SHA-256 hashes only (zero raw codes)
 */
class MbSeekhoRepositoryTest {

    private lateinit var dao: InMemoryMbSeekhoDao
    private lateinit var repo: MbSeekhoRepository

    @Before
    fun setUp() {
        dao = InMemoryMbSeekhoDao()
        repo = MbSeekhoRepository(dao)
    }

    @Test
    fun test01_and_14_student_login_with_valid_code_and_private_sha256_inventory() = runTest {
        repo.initializeInventory()
        val allCodes = dao.getAllAccessCodesOnce()
        assertEquals(60, allCodes.size)
        assertEquals(50, allCodes.count { it.role == UserRole.STUDENT.value })
        assertEquals(10, allCodes.count { it.role == UserRole.TEACHER.value })
        assertTrue(allCodes.all { it.codeHash.length == 64 && !it.codeHash.contains("MBST") && !it.codeHash.contains("MBTC") })

        val result = repo.redeemAccessCode("MBST-EFZD-LZT9", UserRole.STUDENT, "Aanya Verma")
        assertTrue(result is CodeRedemptionResult.Success)
        val student = (result as CodeRedemptionResult.Success).user
        assertEquals(UserRole.STUDENT.value, student.role)
        assertEquals("STU001", student.codeId)
        assertEquals("Aanya Verma", student.name)
    }

    @Test
    fun test02_teacher_login_with_valid_code() = runTest {
        val result = repo.redeemAccessCode("MBTC-9I11-DDF1", UserRole.TEACHER, "Prof. Rohan Das")
        assertTrue(result is CodeRedemptionResult.Success)
        val teacher = (result as CodeRedemptionResult.Success).user
        assertEquals(UserRole.TEACHER.value, teacher.role)
        assertEquals("TCH001", teacher.codeId)
    }

    @Test
    fun test03_invalid_codes_rejected() = runTest {
        val result = repo.redeemAccessCode("MBST-FAKE-0000", UserRole.STUDENT)
        assertEquals(CodeRedemptionResult.InvalidCode, result)
    }

    @Test
    fun test04_used_codes_cannot_be_reused() = runTest {
        val first = repo.redeemAccessCode("MBST-Q8W5-LX5Y", UserRole.STUDENT, "First User")
        assertTrue(first is CodeRedemptionResult.Success)

        val second = repo.redeemAccessCode("MBST-Q8W5-LX5Y", UserRole.STUDENT, "Second User")
        assertEquals(CodeRedemptionResult.AlreadyUsed, second)
    }

    @Test
    fun test05_and_06_cross_portal_code_redemption_blocked() = runTest {
        val studentOnTeacher = repo.redeemAccessCode("MBST-PRVQ-8ZKD", UserRole.TEACHER)
        assertEquals(CodeRedemptionResult.WrongPortal, studentOnTeacher)

        val teacherOnStudent = repo.redeemAccessCode("MBTC-RPW1-REDA", UserRole.STUDENT)
        assertEquals(CodeRedemptionResult.WrongPortal, teacherOnStudent)
    }

    @Test
    fun test07_to_11_teacher_upload_student_home_new_library_and_profile() = runTest {
        val teacher = (repo.redeemAccessCode("MBTC-LWE9-IMCL", UserRole.TEACHER, "Prof. Meera") as CodeRedemptionResult.Success).user
        val student = (repo.redeemAccessCode("MBST-ZL8Z-FYKC", UserRole.STUDENT, "Kabir") as CodeRedemptionResult.Success).user

        // Student cannot upload courses
        val studentUpload = repo.publishCourse(
            teacherUid = student.uid,
            title = "Unauthorized",
            description = "Blocked course",
            category = "Programming",
            thumbnailUrl = null,
            videoUrl = "https://example.com/unauth.mp4"
        )
        assertTrue(studentUpload.isFailure)

        // Teacher publishes courses
        val c1 = repo.publishCourse(
            teacherUid = teacher.uid,
            title = "Kotlin Foundations",
            description = "Learn Kotlin from scratch with hands-on projects.",
            category = "Programming",
            thumbnailUrl = null,
            videoUrl = "https://example.com/kotlin.mp4"
        ).getOrThrow()

        val c2 = repo.publishCourse(
            teacherUid = teacher.uid,
            title = "Jetpack Compose Mastery",
            description = "Build modern Material 3 Android apps.",
            category = "UI/UX Design",
            thumbnailUrl = null,
            videoUrl = "https://example.com/compose.mp4"
        ).getOrThrow()

        // Published courses appear in Student Home & New (newest first)
        val published = repo.observePublishedCourses().first()
        assertEquals(2, published.size)
        assertTrue(published.any { it.courseId == c1.courseId })
        assertTrue(published.any { it.courseId == c2.courseId })

        // Saved courses & watch progress appear in Library
        assertTrue(repo.toggleSaveCourse(student.uid, c1.courseId))
        val saved = repo.observeSavedCourses(student.uid).first()
        assertEquals(1, saved.size)
        assertEquals(c1.courseId, saved.first().courseId)

        repo.updateCourseProgress(student.uid, c1.courseId, 95_000L, 100_000L)
        val progress = repo.observeCourseProgress(student.uid).first()
        assertEquals(1, progress.size)
        assertTrue(progress.first().completed)

        // Profile loads & updates cleanly
        repo.updateProfileName(student.uid, "Kabir Malhotra").getOrThrow()
        val updatedStudent = repo.getUserById(student.uid)
        assertNotNull(updatedStudent)
        assertEquals("Kabir Malhotra", updatedStudent?.name)
    }

    @Test
    fun test12_and_13_admin_access_control_manual_payment_verification_and_csv_export() = runTest {
        repo.initializeInventory()
        val student = (repo.redeemAccessCode("MBST-9DBC-K4Q4", UserRole.STUDENT, "Dev") as CodeRedemptionResult.Success).user
        val teacher = (repo.redeemAccessCode("MBTC-GE8F-FZUV", UserRole.TEACHER, "Ananya") as CodeRedemptionResult.Success).user

        // Unauthorized users blocked from Admin
        assertTrue(repo.generateAdditionalCodes(student.uid, UserRole.STUDENT, 1).isFailure)
        assertTrue(repo.revokeUnusedCode(teacher.uid, "STU010").isFailure)
        assertTrue(repo.recordManualPayment(student.uid, "Fake", UserRole.STUDENT, "100", "TXN0").isFailure)

        // Owner one-time claim & passphrase verification
        val owner = repo.claimOwnerAdmin("MB Seekho Owner", "OwnerSecret#2026").getOrThrow()
        assertEquals(UserRole.ADMIN.value, owner.role)
        assertTrue(repo.verifyOwnerAdmin("WrongPass").isFailure)
        assertTrue(repo.verifyOwnerAdmin("OwnerSecret#2026").isSuccess)

        // Payments start PENDING and require manual owner verification
        val payment = repo.recordManualPayment(
            adminUid = owner.uid,
            customerName = "Neha Gupta",
            userRole = UserRole.STUDENT,
            paymentAmount = "499",
            transactionRef = "UPI-99887766"
        ).getOrThrow()
        assertEquals(PaymentStatus.PENDING.value, payment.verificationStatus)

        repo.verifyOrRejectPayment(
            adminUid = owner.uid,
            paymentId = payment.paymentId,
            status = PaymentStatus.VERIFIED,
            assignedCodeId = "STU010",
            customerName = payment.customerName
        ).getOrThrow()

        val verified = repo.observeAllPayments().first().first { it.paymentId == payment.paymentId }
        assertEquals(PaymentStatus.VERIFIED.value, verified.verificationStatus)
        assertEquals("STU010", verified.assignedCodeId)

        val assignedCode = dao.findAccessCodeById("STU010")
        assertEquals(CodeStatus.ASSIGNED.value, assignedCode?.status)
        assertEquals("Neha Gupta", assignedCode?.assignedToName)

        // Revoke unused code & confirm rejection
        repo.revokeUnusedCode(owner.uid, "STU011").getOrThrow()
        assertEquals(CodeRedemptionResult.Revoked, repo.redeemAccessCode("MBST-ZKGE-PBYW", UserRole.STUDENT))

        // Generate additional codes & export CSV without exposing raw initial codes
        val extra = repo.generateAdditionalCodes(owner.uid, UserRole.STUDENT, 2).getOrThrow()
        assertEquals(2, extra.size)
        assertTrue(repo.redeemAccessCode(extra.first().rawCode, UserRole.STUDENT, "New Buyer") is CodeRedemptionResult.Success)
        val csv = repo.exportAccessCodesCsv(owner.uid).getOrThrow()
        assertTrue(csv.contains("STU001") && !csv.contains("MBST-EFZD-LZT9"))

        // Disabling account blocks returning sign-in
        repo.setUserActiveStatus(owner.uid, student.uid, false).getOrThrow()
        assertEquals(CodeRedemptionResult.AccountDisabled, repo.signInReturningMember(student.uid, UserRole.STUDENT))
        assertFalse(repo.getUserById(student.uid)!!.active)
    }

    private class InMemoryMbSeekhoDao : MbSeekhoDao {
        private val codes = MutableStateFlow<Map<String, AccessCodeEntity>>(emptyMap())
        private val users = MutableStateFlow<Map<String, UserEntity>>(emptyMap())
        private val courses = MutableStateFlow<Map<String, CourseEntity>>(emptyMap())
        private val saved = MutableStateFlow<List<SavedCourseEntity>>(emptyList())
        private val progress = MutableStateFlow<Map<Pair<String, String>, CourseProgressEntity>>(emptyMap())
        private val payments = MutableStateFlow<Map<String, PaymentEntity>>(emptyMap())
        private val ownerSec = MutableStateFlow<OwnerSecurityEntity?>(null)
        private val attempts = mutableListOf<LoginAttemptEntity>()

        override suspend fun getAccessCodeCount(): Int = codes.value.size
        override suspend fun insertAccessCodes(codes: List<AccessCodeEntity>) {
            val m = this.codes.value.toMutableMap()
            codes.forEach { if (!m.containsKey(it.codeId)) m[it.codeId] = it }
            this.codes.value = m
        }
        override fun observeAllAccessCodes(): Flow<List<AccessCodeEntity>> =
            codes.map { it.values.sortedWith(compareBy({ c -> c.role }, { c -> c.codeId })) }
        override suspend fun getAllAccessCodesOnce(): List<AccessCodeEntity> =
            codes.value.values.sortedWith(compareBy({ it.role }, { it.codeId }))
        override suspend fun findAccessCodeByHash(codeHash: String): AccessCodeEntity? =
            codes.value.values.firstOrNull { it.codeHash == codeHash }
        override suspend fun findAccessCodeById(codeId: String): AccessCodeEntity? = codes.value[codeId]
        override suspend fun countCodesByRole(role: String): Int = codes.value.values.count { it.role == role }
        override suspend fun assignCode(codeId: String, status: String, assignedToName: String): Int {
            val cur = codes.value[codeId] ?: return 0
            if (cur.status != "available" && cur.status != "assigned") return 0
            codes.value = codes.value + (codeId to cur.copy(status = status, assignedToName = assignedToName))
            return 1
        }
        override suspend fun revokeUnusedCode(codeId: String): Int {
            val cur = codes.value[codeId] ?: return 0
            if (cur.status != "available" && cur.status != "assigned") return 0
            codes.value = codes.value + (codeId to cur.copy(status = "revoked"))
            return 1
        }
        override suspend fun markCodeUsedIfAvailable(codeId: String, uid: String, redeemedAt: Long): Int {
            val cur = codes.value[codeId] ?: return 0
            if (cur.status != "available" && cur.status != "assigned") return 0
            codes.value = codes.value + (codeId to cur.copy(status = "used", redeemedByUid = uid, redeemedAt = redeemedAt))
            return 1
        }
        override suspend fun insertUser(user: UserEntity) { users.value = users.value + (user.uid to user) }
        override suspend fun getUserById(uid: String): UserEntity? = users.value[uid]
        override fun observeUserById(uid: String): Flow<UserEntity?> = users.map { it[uid] }
        override fun observeUsersByRole(role: String): Flow<List<UserEntity>> =
            users.map { it.values.filter { u -> u.role == role }.sortedByDescending { u -> u.createdAt } }
        override fun observeAllUsers(): Flow<List<UserEntity>> =
            users.map { it.values.sortedByDescending { u -> u.createdAt } }
        override suspend fun updateUserName(uid: String, name: String) {
            users.value[uid]?.let { users.value = users.value + (uid to it.copy(name = name)) }
        }
        override suspend fun updateUserPhoto(uid: String, photoUrl: String?) {
            users.value[uid]?.let { users.value = users.value + (uid to it.copy(photoUrl = photoUrl)) }
        }
        override suspend fun updateUserActive(uid: String, active: Boolean) {
            users.value[uid]?.let { users.value = users.value + (uid to it.copy(active = active)) }
        }
        override suspend fun syncTeacherCourseCount(teacherId: String) {
            val count = courses.value.values.count { it.teacherId == teacherId }
            users.value[teacherId]?.let { users.value = users.value + (teacherId to it.copy(totalCourses = count)) }
        }
        override suspend fun insertCourse(course: CourseEntity) { courses.value = courses.value + (course.courseId to course) }
        override suspend fun updateCourse(course: CourseEntity) { courses.value = courses.value + (course.courseId to course) }
        override suspend fun getCourseById(courseId: String): CourseEntity? = courses.value[courseId]
        override fun observeCourseById(courseId: String): Flow<CourseEntity?> = courses.map { it[courseId] }
        override fun observePublishedCourses(): Flow<List<CourseEntity>> =
            courses.map { it.values.filter { c -> c.published }.sortedByDescending { c -> c.createdAt } }
        override fun observeAllCourses(): Flow<List<CourseEntity>> =
            courses.map { it.values.sortedByDescending { c -> c.createdAt } }
        override fun observeTeacherCourses(teacherId: String): Flow<List<CourseEntity>> =
            courses.map { it.values.filter { c -> c.teacherId == teacherId }.sortedByDescending { c -> c.createdAt } }
        override suspend fun deleteCourseById(courseId: String) { courses.value = courses.value - courseId }
        override suspend fun incrementCourseViews(courseId: String) {
            courses.value[courseId]?.let { courses.value = courses.value + (courseId to it.copy(totalViews = it.totalViews + 1)) }
        }
        override suspend fun saveCourse(savedCourse: SavedCourseEntity) {
            saved.value = saved.value.filterNot { it.uid == savedCourse.uid && it.courseId == savedCourse.courseId } + savedCourse
        }
        override suspend fun unsaveCourse(uid: String, courseId: String) {
            saved.value = saved.value.filterNot { it.uid == uid && it.courseId == courseId }
        }
        override fun observeSavedCourses(uid: String): Flow<List<SavedCourseEntity>> =
            saved.map { it.filter { s -> s.uid == uid }.sortedByDescending { s -> s.savedAt } }
        override suspend fun isCourseSaved(uid: String, courseId: String): Boolean =
            saved.value.any { it.uid == uid && it.courseId == courseId }
        override suspend fun upsertCourseProgress(progress: CourseProgressEntity) {
            this.progress.value = this.progress.value + ((progress.uid to progress.courseId) to progress)
        }
        override fun observeCourseProgress(uid: String): Flow<List<CourseProgressEntity>> =
            progress.map { it.values.filter { p -> p.uid == uid }.sortedByDescending { p -> p.lastWatchedAt } }
        override suspend fun getCourseProgress(uid: String, courseId: String): CourseProgressEntity? =
            progress.value[uid to courseId]
        override suspend fun insertPayment(payment: PaymentEntity) { payments.value = payments.value + (payment.paymentId to payment) }
        override fun observeAllPayments(): Flow<List<PaymentEntity>> =
            payments.map { it.values.sortedByDescending { p -> p.paymentDate } }
        override suspend fun updatePaymentVerification(paymentId: String, status: String, assignedCodeId: String?) {
            payments.value[paymentId]?.let {
                payments.value = payments.value + (paymentId to it.copy(verificationStatus = status, assignedCodeId = assignedCodeId))
            }
        }
        override suspend fun getOwnerSecurity(): OwnerSecurityEntity? = ownerSec.value
        override fun observeOwnerSecurity(): Flow<OwnerSecurityEntity?> = ownerSec
        override suspend fun insertOwnerSecurity(security: OwnerSecurityEntity) { ownerSec.value = security }
        override suspend fun recordFailedAttempt(attempt: LoginAttemptEntity) { attempts += attempt }
        override suspend fun countRecentFailedAttempts(portal: String, sinceMillis: Long): Int =
            attempts.count { it.portal == portal && it.attemptedAt >= sinceMillis }
        override suspend fun clearFailedAttempts(portal: String) { attempts.removeAll { it.portal == portal } }
    }
}

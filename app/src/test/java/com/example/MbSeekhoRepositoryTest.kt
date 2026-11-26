package com.example

import com.example.data.local.AccessCodeEntity
import com.example.data.local.CodeBatchEntity
import com.example.data.local.CodeBatchParser
import com.example.data.local.CodeRoleStats
import com.example.data.local.CoinTransactionEntity
import com.example.data.local.CourseEntity
import com.example.data.local.CourseProgressEntity
import com.example.data.local.LoginAttemptEntity
import com.example.data.local.MbSeekhoDao
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
import com.example.data.repository.CodeRedemptionResult
import com.example.data.repository.MbSeekhoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Comprehensive JVM test suite verifying all MB Seekho features including the
 * Student Access Code System Update:
 * 1. Zero hardcoded Student Access Codes in app; Teacher codes unchanged (SHA-256 only)
 * 2. Streaming import of provided Student Access Code file format (`STU0001  MBST-XXXX-XXXX`),
 *    header skipping, global duplicate prevention across re-imports and future batches
 * 3. Student Access Code redemption: valid unused activates student & marks Used; rejects
 *    already-used, disabled, invalid, and wrong-portal (Teacher code on Student portal & vice versa);
 *    Admin disable & re-enable workflow
 * 4. Unlimited future Student Access Code batches (no 10,000 cap; immediate validation without app update;
 *    SHA-256 only stored in DB)
 * 5. Teacher Access Code validation, 7 Course Categories, Teacher Coin Reward Engine (10 valid views = 1 coin),
 *    Teacher Studio 28-day analytics, Video Likes & Comments moderation, Owner Admin security & payments
 */
class MbSeekhoRepositoryTest {

    private lateinit var dao: InMemoryMbSeekhoDao
    private lateinit var repo: MbSeekhoRepository

    @Before
    fun setUp() {
        dao = InMemoryMbSeekhoDao()
        repo = MbSeekhoRepository(dao)
    }

    private suspend fun createStudentViaAdminBatch(ownerUid: String, displayName: String): UserEntity {
        val batch = repo.generateAccessCodesBatch(ownerUid, UserRole.STUDENT, 1).getOrThrow()
        val rawCode = batch.sampleReveals.first().rawCode
        val res = repo.redeemStudentAccessCode(rawCode, displayName)
        assertTrue("Expected student code redemption to succeed, got $res", res is CodeRedemptionResult.Success)
        return (res as CodeRedemptionResult.Success).user
    }

    @Test
    fun test01_zero_hardcoded_student_codes_and_teacher_codes_intact() = runTest {
        repo.initializeInventory()
        val allCodes = dao.getAllAccessCodesOnce()

        // Zero Student Access Codes are hardcoded in the Android app
        assertEquals(0, allCodes.count { it.role == UserRole.STUDENT.value })
        // Existing 10 Teacher Access Codes remain intact as SHA-256 hashes only
        assertEquals(10, allCodes.count { it.role == UserRole.TEACHER.value })
        assertTrue(allCodes.all { it.codeHash.length == 64 && !it.codeHash.contains("MBTC") && !it.codeHash.contains("MBST") })

        val studentStats = repo.observeCodeStats(UserRole.STUDENT).first()
        assertEquals(0, studentStats.total)
        val teacherStats = repo.observeCodeStats(UserRole.TEACHER).first()
        assertEquals(10, teacherStats.total)
        assertEquals(10, teacherStats.unused)
    }

    @Test
    fun test02_student_access_code_import_dedup_redemption_and_disable_enable() = runTest {
        val owner = repo.claimOwnerAdmin("MB Seekho Owner", "OwnerSecret#2026").getOrThrow()

        // Verify parser handles the exact provided file format (headers, blank lines, double-space, CSV, single-column)
        val sampleLines = sequenceOf(
            "MB SEEKHO - 1000 STUDENT ACCESS CODES",
            "Previous 50 + 950 new codes",
            "",
            "STU0001  MBST-ABCD-1234",
            "STU0002  MBST-EFGH-5678",
            "STU0003,MBST-IJKL-9012",
            "MBST-MNOP-3456",
            "STU0001  MBST-ABCD-1234" // intentional in-file duplicate
        )

        val firstImport = repo.importAccessCodesFromLines(
            adminUid = owner.uid,
            role = UserRole.STUDENT,
            sourceLabel = "Initial Student Batch",
            lines = sampleLines
        ).getOrThrow()

        assertEquals(5, firstImport.totalSubmitted)
        assertEquals(4, firstImport.insertedCount)
        assertEquals(1, firstImport.duplicateCount)
        assertEquals(0, firstImport.invalidCount)

        // Re-importing the exact same codes must detect 100% duplicates across existing batches
        val secondImport = repo.importAccessCodesFromLines(
            adminUid = owner.uid,
            role = UserRole.STUDENT,
            sourceLabel = "Duplicate Re-import Check",
            lines = sequenceOf("STU0001  MBST-ABCD-1234", "STU0002  MBST-EFGH-5678")
        ).getOrThrow()
        assertEquals(0, secondImport.insertedCount)
        assertEquals(2, secondImport.duplicateCount)

        // Confirm raw codes are NEVER stored in the database (only 64-char SHA-256 hashes)
        val storedStudents = dao.getAllAccessCodesOnce().filter { it.role == UserRole.STUDENT.value }
        assertEquals(4, storedStudents.size)
        assertTrue(storedStudents.all { it.codeHash.length == 64 && !it.codeHash.contains("MBST") })

        // 1. Valid unused Student code activates Student account and marks code as Used
        val redeemed = repo.redeemStudentAccessCode("mbst-abcd-1234", "Aanya Verma")
        assertTrue(redeemed is CodeRedemptionResult.Success)
        val student = (redeemed as CodeRedemptionResult.Success).user
        assertEquals(UserRole.STUDENT.value, student.role)
        assertEquals("STU0001", student.codeId)
        assertEquals(CodeStatus.USED.value, dao.findAccessCodeById("STU0001")?.status)

        // Returning Student ID login works seamlessly
        val returning = repo.signInReturningMember(student.uid, UserRole.STUDENT)
        assertTrue(returning is CodeRedemptionResult.Success)

        // 2. Reusing an already-used Student code is rejected
        assertEquals(CodeRedemptionResult.AlreadyUsed, repo.redeemStudentAccessCode("MBST-ABCD-1234", "Second Try"))

        // 3. Invalid code is rejected
        assertEquals(CodeRedemptionResult.InvalidCode, repo.redeemStudentAccessCode("MBST-FAKE-0000", "Nobody"))

        // 4. Cross-portal code isolation: Teacher code rejected on Student portal & Student code rejected on Teacher portal
        assertEquals(CodeRedemptionResult.WrongPortal, repo.redeemStudentAccessCode("MBTC-9I11-DDF1", "Wrong Portal"))
        assertEquals(CodeRedemptionResult.WrongPortal, repo.redeemTeacherAccessCode("MBST-EFGH-5678", "Wrong Portal"))

        // 5. Disabling an unused Student code rejects login; cannot disable an already-used code; re-enabling restores login
        assertTrue(repo.disableAccessCode(owner.uid, "STU0001").isFailure) // already used
        repo.disableAccessCode(owner.uid, "STU0002").getOrThrow()
        assertEquals(CodeStatus.DISABLED.value, dao.findAccessCodeById("STU0002")?.status)
        assertEquals(CodeRedemptionResult.Disabled, repo.redeemStudentAccessCode("MBST-EFGH-5678", "Blocked Student"))

        repo.enableAccessCode(owner.uid, "STU0002").getOrThrow()
        assertEquals(CodeStatus.AVAILABLE.value, dao.findAccessCodeById("STU0002")?.status)
        val afterEnable = repo.redeemStudentAccessCode("MBST-EFGH-5678", "Re-enabled Student")
        assertTrue(afterEnable is CodeRedemptionResult.Success)

        // Verify live SQL stats (4 total: 2 used, 2 unused, 0 disabled)
        val stats = repo.observeCodeStats(UserRole.STUDENT).first()
        assertEquals(4, stats.total)
        assertEquals(2, stats.used)
        assertEquals(2, stats.unused)
        assertEquals(0, stats.disabled)

        // 6. Verify numbered list format with 4-part codes (00001. MBST-XXXX-XXXX-XXXX) and 3-char suffix
        val numberedImport = repo.importAccessCodesFromLines(
            adminUid = owner.uid,
            role = UserRole.STUDENT,
            sourceLabel = "Numbered 4-Part Batch",
            lines = sequenceOf(
                "00001. MBST-AA11-BB22-CC33",
                "00002. MBST-DD44-EE55-FF66",
                "00400. MBST-GG77-HH88-JJ9"
            )
        ).getOrThrow()
        assertEquals(3, numberedImport.insertedCount)
        assertEquals(0, numberedImport.invalidCount)
        assertEquals(CodeStatus.AVAILABLE.value, dao.findAccessCodeById("STU00001")?.status)
        assertEquals(CodeStatus.AVAILABLE.value, dao.findAccessCodeById("STU00400")?.status)

        val redeemed4Part = repo.redeemStudentAccessCode("MBST-AA11-BB22-CC33", "Rohan Sharma")
        assertTrue(redeemed4Part is CodeRedemptionResult.Success)
        assertEquals("STU00001", (redeemed4Part as CodeRedemptionResult.Success).user.codeId)
        assertEquals(CodeRedemptionResult.AlreadyUsed, repo.redeemStudentAccessCode("MBST-AA11-BB22-CC33"))
    }

    @Test
    fun test03_unlimited_future_batches_and_large_scale_batch_import_support() = runTest {
        val owner = repo.claimOwnerAdmin("MB Seekho Owner", "OwnerSecret#2026").getOrThrow()

        // Generate 2 sequential large batches (1,200 + 800 = 2,000) to prove unlimited cumulative batch support
        // with zero collisions and immediate redemption without any app update
        val batch1 = repo.generateAccessCodesBatch(owner.uid, UserRole.STUDENT, 1200, "Future Batch #1").getOrThrow()
        val batch2 = repo.generateAccessCodesBatch(owner.uid, UserRole.STUDENT, 800, "Future Batch #2").getOrThrow()

        assertEquals(1200, batch1.generatedCount)
        assertEquals(800, batch2.generatedCount)

        val stats = repo.observeCodeStats(UserRole.STUDENT).first()
        assertEquals(2000, stats.total)
        assertEquals(2000, stats.unused)
        assertEquals(2, repo.observeCodeBatches().first().size)

        // Newly generated codes validate immediately
        val fromBatch1 = batch1.sampleReveals.first().rawCode
        val fromBatch2 = batch2.sampleReveals.last().rawCode
        assertTrue(repo.redeemStudentAccessCode(fromBatch1, "Future Student 1") is CodeRedemptionResult.Success)
        assertTrue(repo.redeemStudentAccessCode(fromBatch2, "Future Student 2") is CodeRedemptionResult.Success)

        // Also test parsing the real attached 1,000-code file if present on disk (without hardcoding any codes!)
        val attachedFile = listOf(
            File("../public/assets/aistudio/MB_Seekho_1000_Student_Access_Codes-1_7a7e23fa.txt"),
            File("public/assets/aistudio/MB_Seekho_1000_Student_Access_Codes-1_7a7e23fa.txt")
        ).firstOrNull { it.exists() && it.isFile }
        if (attachedFile != null) {
            val importRes = attachedFile.bufferedReader().useLines { lines ->
                repo.importAccessCodesFromLines(
                    adminUid = owner.uid,
                    role = UserRole.STUDENT,
                    sourceLabel = "Uploaded 1000 Codes",
                    lines = lines
                ).getOrThrow()
            }
            assertEquals(1000, importRes.insertedCount)
            assertEquals(0, importRes.duplicateCount)
            assertEquals(3000, repo.observeCodeStats(UserRole.STUDENT).first().total)
        }

        // Verify CodeBatchParser ignores headers & comments cleanly
        assertEquals(CodeBatchParser.LineParseResult.Ignored, CodeBatchParser.parseLine("MB SEEKHO - 10000 STUDENT ACCESS CODES"))
        assertEquals(CodeBatchParser.LineParseResult.Ignored, CodeBatchParser.parseLine("codeId,accessCode,role"))
        assertEquals(CodeBatchParser.LineParseResult.Ignored, CodeBatchParser.parseLine("   "))
    }

    @Test
    fun test04_teacher_access_code_validation_single_use_and_rejection() = runTest {
        val first = repo.redeemTeacherAccessCode("MBTC-9I11-DDF1", "Prof. Rohan Das")
        assertTrue(first is CodeRedemptionResult.Success)
        val teacher = (first as CodeRedemptionResult.Success).user
        assertEquals(UserRole.TEACHER.value, teacher.role)
        assertEquals("TCH001", teacher.codeId)
        assertEquals(0, dao.getTeacherCoinsOnce(teacher.uid)?.totalCoins)

        // Already used teacher code cannot be reused
        assertEquals(CodeRedemptionResult.AlreadyUsed, repo.redeemTeacherAccessCode("MBTC-9I11-DDF1", "Impostor"))

        // Invalid code rejected
        assertEquals(CodeRedemptionResult.InvalidCode, repo.redeemTeacherAccessCode("MBTC-FAKE-0000", "Nobody"))
    }

    @Test
    fun test05_seven_course_categories_and_normalization_to_other_course() = runTest {
        assertEquals(
            listOf(
                "Facebook Course",
                "YouTube Course",
                "Social Media Course",
                "Snapchat Course",
                "Earning App Course",
                "Earning Website Course",
                "Other Course"
            ),
            CourseCategories.all
        )
        assertEquals("YouTube Course", CourseCategories.normalize("youtube course"))
        assertEquals("Other Course", CourseCategories.normalize("Legacy Programming"))
        assertEquals("Other Course", CourseCategories.normalize(""))

        val owner = repo.claimOwnerAdmin("MB Seekho Owner", "OwnerSecret#2026").getOrThrow()
        val teacher = (repo.redeemTeacherAccessCode("MBTC-LWE9-IMCL", "Prof. Meera") as CodeRedemptionResult.Success).user
        val student = createStudentViaAdminBatch(owner.uid, "Kabir")

        // Student cannot upload videos
        val studentUpload = repo.publishCourse(
            teacherUid = student.uid,
            title = "Unauthorized Upload",
            description = "Blocked video upload from student.",
            category = CourseCategories.YOUTUBE,
            videoUrl = "file:///unauth.mp4"
        )
        assertTrue(studentUpload.isFailure)

        val ytCourse = repo.publishCourse(
            teacherUid = teacher.uid,
            title = "YouTube Growth Blueprint",
            description = "Complete guide to growing your YouTube channel.",
            category = CourseCategories.YOUTUBE,
            videoUrl = "file:///yt.mp4"
        ).getOrThrow()
        assertEquals(CourseCategories.YOUTUBE, ytCourse.category)

        val otherCourse = repo.publishCourse(
            teacherUid = teacher.uid,
            title = "Freelancing Basics",
            description = "General online skills outside main categories.",
            category = "Unlisted Legacy Topic",
            videoUrl = "file:///other.mp4"
        ).getOrThrow()
        assertEquals(CourseCategories.OTHER, otherCourse.category)

        assertEquals(2, repo.observePublishedCourses().first().size)
        assertTrue(repo.toggleSaveCourse(student.uid, ytCourse.courseId))
        assertEquals(1, repo.observeSavedCourses(student.uid).first().size)
        repo.updateCourseProgress(student.uid, ytCourse.courseId, 95_000L, 100_000L)
        assertTrue(repo.observeCourseProgress(student.uid).first().first().completed)
        repo.updateProfileName(student.uid, "Kabir Malhotra").getOrThrow()
        assertEquals("Kabir Malhotra", repo.getUserById(student.uid)?.name)
    }

    @Test
    fun test06_teacher_coin_reward_engine_10_views_per_coin_and_anti_fraud() = runTest {
        val owner = repo.claimOwnerAdmin("MB Seekho Owner", "OwnerSecret#2026").getOrThrow()
        val teacher = (repo.redeemTeacherAccessCode("MBTC-RPW1-REDA", "Coach Arjun") as CodeRedemptionResult.Success).user
        val baseTime = 1_800_000_000_000L

        val video1 = repo.publishCourse(
            teacherUid = teacher.uid,
            title = "Facebook Monetization",
            description = "How to monetize Facebook pages properly.",
            category = CourseCategories.FACEBOOK,
            videoUrl = "file:///fb.mp4",
            createdAtMillis = baseTime
        ).getOrThrow()

        val video2 = repo.publishCourse(
            teacherUid = teacher.uid,
            title = "Snapchat Spotlight Guide",
            description = "Grow on Snapchat Spotlight step by step.",
            category = CourseCategories.SNAPCHAT,
            videoUrl = "file:///snap.mp4",
            createdAtMillis = baseTime + 1_000L
        ).getOrThrow()

        // Teacher self-view ignored
        assertEquals(ValidViewOutcome.SelfViewIgnored, repo.recordValidatedVideoView(video1.courseId, teacher.uid, baseTime + 5_000L))
        assertEquals(0, dao.getCourseById(video1.courseId)?.totalViews)

        val students = (1..10).map { idx -> createStudentViaAdminBatch(owner.uid, "Student $idx") }

        val v1 = repo.recordValidatedVideoView(video1.courseId, students[0].uid, baseTime + 10_000L) as ValidViewOutcome.Recorded
        assertEquals(1, v1.validViewsTotalForTeacher)
        assertEquals(0, v1.coinsAwardedNow)

        // Duplicate within 24h blocked
        assertEquals(ValidViewOutcome.DuplicateWithinCooldown, repo.recordValidatedVideoView(video1.courseId, students[0].uid, baseTime + 60_000L))

        for (i in 1..8) {
            val out = repo.recordValidatedVideoView(video1.courseId, students[i].uid, baseTime + 100_000L + i * 1000L) as ValidViewOutcome.Recorded
            assertEquals(0, out.coinsAwardedNow)
        }
        assertEquals(0, dao.getTeacherCoinsOnce(teacher.uid)?.totalCoins)

        // 10th valid view unlocks 1 coin
        val r10 = repo.recordValidatedVideoView(video1.courseId, students[9].uid, baseTime + 200_000L) as ValidViewOutcome.Recorded
        assertEquals(10, r10.validViewsTotalForTeacher)
        assertEquals(1, r10.coinsAwardedNow)
        assertEquals(1, r10.newTeacherCoinBalance)

        // 20th valid view unlocks 2nd coin
        var lastOutcome: ValidViewOutcome? = null
        students.forEachIndexed { idx, s ->
            lastOutcome = repo.recordValidatedVideoView(video2.courseId, s.uid, baseTime + 300_000L + idx * 1000L)
        }
        val r20 = lastOutcome as ValidViewOutcome.Recorded
        assertEquals(20, r20.validViewsTotalForTeacher)
        assertEquals(1, r20.coinsAwardedNow)
        assertEquals(2, r20.newTeacherCoinBalance)
        assertEquals(2, dao.observeCoinTransactionsForTeacher(teacher.uid).first().size)
    }

    @Test
    fun test07_teacher_studio_strict_28_day_analytics_window() = runTest {
        val owner = repo.claimOwnerAdmin("MB Seekho Owner", "OwnerSecret#2026").getOrThrow()
        val teacher = (repo.redeemTeacherAccessCode("MBTC-GE8F-FZUV", "Prof. Kavya") as CodeRedemptionResult.Success).user
        val now = 1_800_000_000_000L
        val oldTimestamp = now - (35L * 24L * 60L * 60L * 1000L)
        val recentTimestamp = now - (3L * 24L * 60L * 60L * 1000L)

        val oldVideo = repo.publishCourse(
            teacherUid = teacher.uid,
            title = "Old Earning App Video",
            description = "Uploaded 35 days ago outside 28-day window.",
            category = CourseCategories.EARNING_APP,
            videoUrl = "file:///old.mp4",
            createdAtMillis = oldTimestamp
        ).getOrThrow()

        val recentVideo = repo.publishCourse(
            teacherUid = teacher.uid,
            title = "Recent Earning Website Guide",
            description = "Uploaded 3 days ago inside 28-day window.",
            category = CourseCategories.EARNING_WEBSITE,
            videoUrl = "file:///recent.mp4",
            createdAtMillis = recentTimestamp
        ).getOrThrow()

        val students = (1..12).map { createStudentViaAdminBatch(owner.uid, "Viewer $it") }
        for (i in 0 until 10) {
            repo.recordValidatedVideoView(oldVideo.courseId, students[i].uid, oldTimestamp + i * 1000L)
        }
        for (i in 0 until 10) {
            repo.recordValidatedVideoView(recentVideo.courseId, students[i].uid, recentTimestamp + i * 1000L)
        }
        repo.recordValidatedVideoView(recentVideo.courseId, students[10].uid, recentTimestamp + 20_000L)
        repo.recordValidatedVideoView(recentVideo.courseId, students[11].uid, recentTimestamp + 30_000L)

        val stats = repo.observeTeacherStudioStats(teacher.uid, nowMillisProvider = { now }).first()
        assertEquals(12, stats.viewsLast28Days)
        assertEquals(1, stats.videosUploadedLast28Days)
        assertEquals(1, stats.coinsEarnedLast28Days)
        assertEquals(2, stats.totalCoinBalance)
        assertEquals(22, stats.totalValidViewsAllTime)
        assertEquals(2, stats.viewsTowardNextCoin)
    }

    @Test
    fun test08_video_likes_and_comments_with_anti_spam_and_role_moderation() = runTest {
        val owner = repo.claimOwnerAdmin("MB Seekho Owner", "OwnerSecret#2026").getOrThrow()
        val teacher1 = (repo.redeemTeacherAccessCode("MBTC-9I11-DDF1", "Teacher One") as CodeRedemptionResult.Success).user
        val teacher2 = (repo.redeemTeacherAccessCode("MBTC-LWE9-IMCL", "Teacher Two") as CodeRedemptionResult.Success).user
        val student1 = createStudentViaAdminBatch(owner.uid, "Riya")
        val student2 = createStudentViaAdminBatch(owner.uid, "Aman")

        val video = repo.publishCourse(
            teacherUid = teacher1.uid,
            title = "Social Media Mastery",
            description = "Grow your audience across social platforms.",
            category = CourseCategories.SOCIAL_MEDIA,
            videoUrl = "file:///social.mp4"
        ).getOrThrow()

        assertTrue(repo.toggleVideoLike(video.courseId, student1.uid).getOrThrow())
        assertTrue(repo.toggleVideoLike(video.courseId, student2.uid).getOrThrow())
        assertEquals(2, repo.observeLikesForVideo(video.courseId).first().size)
        assertFalse(repo.toggleVideoLike(video.courseId, student1.uid).getOrThrow())
        assertEquals(1, repo.observeLikesForVideo(video.courseId).first().size)

        assertTrue(repo.postVideoComment(video.courseId, student1.uid, "   ").isFailure)
        assertTrue(repo.postVideoComment(video.courseId, student1.uid, "a").isFailure)

        val baseTime = 1_800_000_000_000L
        val c1 = repo.postVideoComment(video.courseId, student1.uid, "Very helpful tutorial!", baseTime).getOrThrow()
        assertTrue(repo.postVideoComment(video.courseId, student1.uid, "Very helpful tutorial!", baseTime + 5_000L).isFailure)
        val c2 = repo.postVideoComment(video.courseId, student2.uid, "Loved the practical tips!", baseTime + 10_000L).getOrThrow()
        val c3 = repo.postVideoComment(video.courseId, student1.uid, "Can you cover Reels next?", baseTime + 20_000L).getOrThrow()

        val list = repo.observeCommentsForVideo(video.courseId).first()
        assertEquals(listOf(c3.commentId, c2.commentId, c1.commentId), list.map { it.commentId })

        assertTrue(repo.deleteVideoComment(student1.uid, c2.commentId).isFailure)
        assertTrue(repo.deleteVideoComment(teacher2.uid, c2.commentId).isFailure)
        assertTrue(repo.deleteVideoComment(student1.uid, c1.commentId).isSuccess)
        assertTrue(repo.deleteVideoComment(teacher1.uid, c2.commentId).isSuccess)
        assertTrue(repo.deleteVideoComment(owner.uid, c3.commentId).isSuccess)
        assertTrue(repo.observeCommentsForVideo(video.courseId).first().isEmpty())
    }

    @Test
    fun test09_owner_admin_security_payments_teacher_and_student_csv_export() = runTest {
        repo.initializeInventory()
        val owner = repo.claimOwnerAdmin("MB Seekho Owner", "OwnerSecret#2026").getOrThrow()
        val student = createStudentViaAdminBatch(owner.uid, "Dev")
        val teacher = (repo.redeemTeacherAccessCode("MBTC-GE8F-FZUV", "Ananya") as CodeRedemptionResult.Success).user

        // Unauthorized users blocked from all Admin actions
        assertTrue(repo.generateAccessCodesBatch(student.uid, UserRole.STUDENT, 5).isFailure)
        assertTrue(repo.importAccessCodesFromLines(student.uid, UserRole.STUDENT, "Hack", sequenceOf("MBST-AAAA-BBBB")).isFailure)
        assertTrue(repo.disableAccessCode(teacher.uid, "TCH001").isFailure)
        assertTrue(repo.recordManualPayment(student.uid, "Fake", UserRole.TEACHER, "500", "TXN0").isFailure)

        assertTrue(repo.verifyOwnerAdmin("WrongPass").isFailure)
        assertTrue(repo.verifyOwnerAdmin("OwnerSecret#2026").isSuccess)

        val payment = repo.recordManualPayment(
            adminUid = owner.uid,
            customerName = "Neha Gupta",
            userRole = UserRole.TEACHER,
            paymentAmount = "999",
            transactionRef = "UPI-99887766"
        ).getOrThrow()
        assertEquals(PaymentStatus.PENDING.value, payment.verificationStatus)

        repo.verifyOrRejectPayment(
            adminUid = owner.uid,
            paymentId = payment.paymentId,
            status = PaymentStatus.VERIFIED,
            assignedCodeId = "TCH001",
            customerName = payment.customerName
        ).getOrThrow()
        assertEquals(CodeStatus.ASSIGNED.value, dao.findAccessCodeById("TCH001")?.status)

        // Disable unused teacher code & confirm rejection
        repo.disableAccessCode(owner.uid, "TCH002").getOrThrow()
        assertEquals(CodeRedemptionResult.Disabled, repo.redeemTeacherAccessCode("MBTC-RPW1-REDA"))

        // Export CSV without exposing raw access codes
        val teacherCsv = repo.exportAccessCodesCsv(owner.uid, UserRole.TEACHER).getOrThrow()
        assertTrue(teacherCsv.contains("TCH001") && !teacherCsv.contains("MBTC-9I11-DDF1"))
        val studentCsv = repo.exportAccessCodesCsv(owner.uid, UserRole.STUDENT).getOrThrow()
        assertTrue(studentCsv.contains("STU0001") && !studentCsv.contains("MBST-"))

        // Disabling account blocks returning sign-in
        repo.setUserActiveStatus(owner.uid, student.uid, false).getOrThrow()
        assertEquals(CodeRedemptionResult.AccountDisabled, repo.signInReturningMember(student.uid, UserRole.STUDENT))
        assertFalse(repo.getUserById(student.uid)!!.active)
    }

    @Test
    fun test10_short_and_long_video_upload_duration_and_engagement() = runTest {
        val owner = repo.claimOwnerAdmin("MB Seekho Owner", "OwnerSecret#2026").getOrThrow()
        val teacher = (repo.redeemTeacherAccessCode("MBTC-9I11-DDF1", "Mentor Aarav") as CodeRedemptionResult.Success).user
        val student = createStudentViaAdminBatch(owner.uid, "Priya")

        val shortVideo = repo.publishCourse(
            teacherUid = teacher.uid,
            title = "30-Second YouTube Hook Tip",
            description = "Quick vertical short on grabbing viewer attention in 3 seconds.",
            category = CourseCategories.YOUTUBE,
            videoUrl = "file:///short_hook.mp4",
            videoType = VideoType.SHORT.value,
            durationMs = 32_000L
        ).getOrThrow()

        val longVideo = repo.publishCourse(
            teacherUid = teacher.uid,
            title = "Complete Facebook Page Monetization Course",
            description = "Full step-by-step landscape course covering reels, stars, and in-stream ads.",
            category = CourseCategories.FACEBOOK,
            videoUrl = "file:///long_fb_course.mp4",
            videoType = VideoType.LONG.value,
            durationMs = 1_245_000L
        ).getOrThrow()

        val defaultVideo = repo.publishCourse(
            teacherUid = teacher.uid,
            title = "Default Long Lesson",
            description = "Ensures default videoType remains long and durationMs defaults to 0.",
            category = CourseCategories.OTHER,
            videoUrl = "file:///default_lesson.mp4"
        ).getOrThrow()

        assertEquals(VideoType.SHORT.value, shortVideo.videoType)
        assertEquals(32_000L, shortVideo.durationMs)
        assertEquals(VideoType.LONG.value, longVideo.videoType)
        assertEquals(1_245_000L, longVideo.durationMs)
        assertEquals(VideoType.LONG.value, defaultVideo.videoType)
        assertEquals(0L, defaultVideo.durationMs)

        val published = repo.observePublishedCourses().first()
        assertEquals(1, published.count { VideoType.fromValue(it.videoType) == VideoType.SHORT })
        assertEquals(2, published.count { VideoType.fromValue(it.videoType) == VideoType.LONG })

        // Real views, likes & comments work across both Short and Long Videos
        val baseTime = 1_800_000_000_000L
        assertTrue(repo.recordValidatedVideoView(shortVideo.courseId, student.uid, baseTime) is ValidViewOutcome.Recorded)
        assertTrue(repo.recordValidatedVideoView(longVideo.courseId, student.uid, baseTime + 1_000L) is ValidViewOutcome.Recorded)
        assertEquals(1, dao.getCourseById(shortVideo.courseId)?.totalViews)
        assertEquals(1, dao.getCourseById(longVideo.courseId)?.totalViews)

        assertTrue(repo.toggleVideoLike(shortVideo.courseId, student.uid).getOrThrow())
        assertTrue(repo.toggleVideoLike(longVideo.courseId, student.uid).getOrThrow())
        assertEquals(2, repo.observeAllVideoLikes().first().size)

        repo.postVideoComment(shortVideo.courseId, student.uid, "Super crisp short tip!", baseTime + 2_000L).getOrThrow()
        repo.postVideoComment(longVideo.courseId, student.uid, "Very detailed full lesson!", baseTime + 12_000L).getOrThrow()
        assertEquals(2, repo.observeAllVideoComments().first().size)
    }

    private class InMemoryMbSeekhoDao : MbSeekhoDao {
        private val codes = MutableStateFlow<Map<String, AccessCodeEntity>>(emptyMap())
        private val batches = MutableStateFlow<Map<String, CodeBatchEntity>>(emptyMap())
        private val users = MutableStateFlow<Map<String, UserEntity>>(emptyMap())
        private val courses = MutableStateFlow<Map<String, CourseEntity>>(emptyMap())
        private val videoViews = MutableStateFlow<List<VideoViewEntity>>(emptyList())
        private val teacherCoins = MutableStateFlow<Map<String, TeacherCoinsEntity>>(emptyMap())
        private val coinTransactions = MutableStateFlow<Map<String, CoinTransactionEntity>>(emptyMap())
        private val videoLikes = MutableStateFlow<List<VideoLikeEntity>>(emptyList())
        private val videoComments = MutableStateFlow<Map<String, VideoCommentEntity>>(emptyMap())
        private val saved = MutableStateFlow<List<SavedCourseEntity>>(emptyList())
        private val progress = MutableStateFlow<Map<Pair<String, String>, CourseProgressEntity>>(emptyMap())
        private val payments = MutableStateFlow<Map<String, PaymentEntity>>(emptyMap())
        private val ownerSec = MutableStateFlow<OwnerSecurityEntity?>(null)
        private val attempts = mutableListOf<LoginAttemptEntity>()

        override suspend fun getAccessCodeCount(): Int = codes.value.size
        override suspend fun countCodesByRole(role: String): Int = codes.value.values.count { it.role == role }

        override fun observeCodeStatsByRole(role: String): Flow<CodeRoleStats> = codes.map { map ->
            val list = map.values.filter { it.role == role }
            CodeRoleStats(
                total = list.size,
                unused = list.count { it.status == "available" || it.status == "assigned" },
                assigned = list.count { it.status == "assigned" },
                used = list.count { it.status == "used" },
                disabled = list.count { it.status == "disabled" || it.status == "revoked" }
            )
        }

        override fun observeFilteredCodes(
            role: String,
            statusFilter: String,
            searchPattern: String,
            limit: Int
        ): Flow<List<AccessCodeEntity>> = codes.map { map ->
            val needle = searchPattern.trim('%').lowercase()
            map.values
                .asSequence()
                .filter { it.role == role }
                .filter {
                    when (statusFilter) {
                        "all" -> true
                        "unused" -> it.status == "available" || it.status == "assigned"
                        "assigned" -> it.status == "assigned"
                        "used" -> it.status == "used"
                        "disabled" -> it.status == "disabled" || it.status == "revoked"
                        else -> true
                    }
                }
                .filter {
                    needle.isEmpty() ||
                        it.codeId.lowercase().contains(needle) ||
                        (it.assignedToName ?: "").lowercase().contains(needle) ||
                        (it.redeemedByUid ?: "").lowercase().contains(needle) ||
                        (it.batchId ?: "").lowercase().contains(needle)
                }
                .sortedWith(compareByDescending<AccessCodeEntity> { it.createdAt }.thenBy { it.codeId })
                .take(limit)
                .toList()
        }

        override fun observeRecentUsedCodes(role: String, limit: Int): Flow<List<AccessCodeEntity>> = codes.map { map ->
            map.values.filter { it.role == role && it.status == "used" }
                .sortedByDescending { it.redeemedAt ?: 0L }
                .take(limit)
        }

        override fun observeAllRecentUsedCodes(limit: Int): Flow<List<AccessCodeEntity>> = codes.map { map ->
            map.values.filter { it.status == "used" }
                .sortedByDescending { it.redeemedAt ?: 0L }
                .take(limit)
        }

        override fun observeAvailableCodesByRole(role: String, limit: Int): Flow<List<AccessCodeEntity>> = codes.map { map ->
            map.values.filter { it.role == role && (it.status == "available" || it.status == "assigned") }
                .sortedBy { it.codeId }
                .take(limit)
        }

        override suspend fun insertAccessCodes(codes: List<AccessCodeEntity>): List<Long> {
            val m = this.codes.value.toMutableMap()
            val existingHashes = m.values.mapTo(HashSet()) { it.codeHash }
            val results = ArrayList<Long>(codes.size)
            var nextRow = m.size.toLong()
            for (entity in codes) {
                if (m.containsKey(entity.codeId) || existingHashes.contains(entity.codeHash)) {
                    results += -1L
                } else {
                    m[entity.codeId] = entity
                    existingHashes.add(entity.codeHash)
                    nextRow += 1L
                    results += nextRow
                }
            }
            this.codes.value = m
            return results
        }

        override suspend fun findExistingHashes(hashes: List<String>): List<String> {
            val set = hashes.toHashSet()
            return codes.value.values.mapNotNull { if (set.contains(it.codeHash)) it.codeHash else null }
        }

        override suspend fun findExistingCodeIds(codeIds: List<String>): List<String> {
            val map = codes.value
            return codeIds.filter { map.containsKey(it) }
        }

        override fun observeAllAccessCodes(): Flow<List<AccessCodeEntity>> =
            codes.map { it.values.sortedWith(compareBy({ c -> c.role }, { c -> c.codeId })) }

        override suspend fun getAllAccessCodesOnce(): List<AccessCodeEntity> =
            codes.value.values.sortedWith(compareBy({ it.role }, { it.codeId }))

        override suspend fun getCodesPageByRole(role: String, limit: Int, offset: Int): List<AccessCodeEntity> =
            codes.value.values.filter { it.role == role }.sortedBy { it.codeId }.drop(offset).take(limit)

        override suspend fun findAccessCodeByHash(codeHash: String): AccessCodeEntity? =
            codes.value.values.firstOrNull { it.codeHash == codeHash }

        override suspend fun findAccessCodeById(codeId: String): AccessCodeEntity? = codes.value[codeId]

        override suspend fun assignCode(codeId: String, status: String, assignedToName: String): Int {
            val cur = codes.value[codeId] ?: return 0
            if (cur.status != "available" && cur.status != "assigned") return 0
            codes.value = codes.value + (codeId to cur.copy(status = status, assignedToName = assignedToName))
            return 1
        }

        override suspend fun disableUnusedCode(codeId: String): Int {
            val cur = codes.value[codeId] ?: return 0
            if (cur.status != "available" && cur.status != "assigned") return 0
            codes.value = codes.value + (codeId to cur.copy(status = "disabled"))
            return 1
        }

        override suspend fun enableDisabledCode(codeId: String): Int {
            val cur = codes.value[codeId] ?: return 0
            if (cur.status != "disabled" && cur.status != "revoked") return 0
            codes.value = codes.value + (codeId to cur.copy(status = "available"))
            return 1
        }

        override suspend fun markCodeUsedIfAvailable(codeId: String, uid: String, redeemedAt: Long): Int {
            val cur = codes.value[codeId] ?: return 0
            if (cur.status != "available" && cur.status != "assigned") return 0
            codes.value = codes.value + (codeId to cur.copy(status = "used", redeemedByUid = uid, redeemedAt = redeemedAt))
            return 1
        }

        override suspend fun insertCodeBatch(batch: CodeBatchEntity) {
            batches.value = batches.value + (batch.batchId to batch)
        }

        override fun observeCodeBatches(): Flow<List<CodeBatchEntity>> =
            batches.map { it.values.sortedByDescending { b -> b.createdAt }.take(50) }

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

        // Valid Video Views & Teacher Coins
        override suspend fun insertVideoView(view: VideoViewEntity): Long {
            if (videoViews.value.any { it.viewId == view.viewId }) return -1L
            videoViews.value = videoViews.value + view
            return videoViews.value.size.toLong()
        }
        override suspend fun countRecentValidViewsByViewer(videoId: String, viewerId: String, sinceMillis: Long): Int =
            videoViews.value.count {
                it.videoId == videoId && it.viewerId == viewerId && it.validationStatus == "valid" && it.viewedAt >= sinceMillis
            }
        override suspend fun countTotalValidViewsForTeacher(teacherId: String): Int =
            videoViews.value.count { it.teacherId == teacherId && it.validationStatus == "valid" }
        override fun observeValidViewsForTeacher(teacherId: String): Flow<List<VideoViewEntity>> =
            videoViews.map { list -> list.filter { it.teacherId == teacherId && it.validationStatus == "valid" }.sortedByDescending { it.viewedAt } }
        override fun observeAllValidViews(): Flow<List<VideoViewEntity>> =
            videoViews.map { list -> list.filter { it.validationStatus == "valid" }.sortedByDescending { it.viewedAt } }
        override suspend fun insertTeacherCoinsIfAbsent(coins: TeacherCoinsEntity) {
            if (!teacherCoins.value.containsKey(coins.teacherId)) {
                teacherCoins.value = teacherCoins.value + (coins.teacherId to coins)
            }
        }
        override suspend fun upsertTeacherCoins(coins: TeacherCoinsEntity) {
            teacherCoins.value = teacherCoins.value + (coins.teacherId to coins)
        }
        override suspend fun getTeacherCoinsOnce(teacherId: String): TeacherCoinsEntity? = teacherCoins.value[teacherId]
        override fun observeTeacherCoins(teacherId: String): Flow<TeacherCoinsEntity?> = teacherCoins.map { it[teacherId] }
        override fun observeAllTeacherCoins(): Flow<List<TeacherCoinsEntity>> =
            teacherCoins.map { it.values.sortedByDescending { tc -> tc.totalCoins } }
        override suspend fun insertCoinTransaction(tx: CoinTransactionEntity): Long {
            if (coinTransactions.value.containsKey(tx.transactionId)) return -1L
            coinTransactions.value = coinTransactions.value + (tx.transactionId to tx)
            return coinTransactions.value.size.toLong()
        }
        override fun observeCoinTransactionsForTeacher(teacherId: String): Flow<List<CoinTransactionEntity>> =
            coinTransactions.map { map -> map.values.filter { it.teacherId == teacherId }.sortedByDescending { it.createdAt } }
        override fun observeAllCoinTransactions(): Flow<List<CoinTransactionEntity>> =
            coinTransactions.map { map -> map.values.sortedByDescending { it.createdAt } }
        override suspend fun sumEarnedCoinsForTeacher(teacherId: String): Int =
            coinTransactions.value.values.filter { it.teacherId == teacherId }.sumOf { it.coinsEarned }

        // Video Likes
        override suspend fun insertVideoLike(like: VideoLikeEntity): Long {
            if (videoLikes.value.any { it.videoId == like.videoId && it.studentId == like.studentId }) return -1L
            videoLikes.value = videoLikes.value + like
            return videoLikes.value.size.toLong()
        }
        override suspend fun deleteVideoLike(videoId: String, studentId: String): Int {
            val before = videoLikes.value.size
            videoLikes.value = videoLikes.value.filterNot { it.videoId == videoId && it.studentId == studentId }
            return before - videoLikes.value.size
        }
        override suspend fun isVideoLikedByStudent(videoId: String, studentId: String): Boolean =
            videoLikes.value.any { it.videoId == videoId && it.studentId == studentId }
        override fun observeLikesForVideo(videoId: String): Flow<List<VideoLikeEntity>> =
            videoLikes.map { list -> list.filter { it.videoId == videoId } }
        override fun observeAllVideoLikes(): Flow<List<VideoLikeEntity>> = videoLikes

        // Video Comments
        override suspend fun insertVideoComment(comment: VideoCommentEntity) {
            videoComments.value = videoComments.value + (comment.commentId to comment)
        }
        override suspend fun getCommentById(commentId: String): VideoCommentEntity? = videoComments.value[commentId]
        override suspend fun deleteCommentById(commentId: String) {
            videoComments.value = videoComments.value - commentId
        }
        override fun observeCommentsForVideo(videoId: String): Flow<List<VideoCommentEntity>> =
            videoComments.map { map -> map.values.filter { it.videoId == videoId }.sortedByDescending { it.createdAt } }
        override fun observeAllComments(): Flow<List<VideoCommentEntity>> =
            videoComments.map { map -> map.values.sortedByDescending { it.createdAt } }
        override suspend fun countRecentCommentsByStudent(studentId: String, sinceMillis: Long): Int =
            videoComments.value.values.count { it.studentId == studentId && it.createdAt >= sinceMillis }
        override suspend fun getLatestCommentByStudentOnVideo(videoId: String, studentId: String): VideoCommentEntity? =
            videoComments.value.values
                .filter { it.videoId == videoId && it.studentId == studentId }
                .maxByOrNull { it.createdAt }

        // Saved Courses & Progress
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

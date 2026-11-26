package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.MbSeekhoDatabase
import com.example.data.local.MediaStorageManager
import com.example.data.local.SessionPreferences
import com.example.data.model.CourseCategories
import com.example.data.model.UserRole
import com.example.data.model.VideoType
import com.example.data.repository.MbSeekhoRepository
import com.example.ui.AppScreen
import com.example.ui.MbSeekhoViewModel
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.OwnerAdminGateScreen
import com.example.ui.screens.RoleLoginScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.screens.ShortVideoScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StudentDashboardScreen
import com.example.ui.screens.TeacherDashboardScreen
import com.example.ui.screens.TeacherStudioScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.MbSeekhoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MbSeekhoTheme {
                MbSeekhoAppRoot()
            }
        }
    }
}

@Composable
fun MbSeekhoAppRoot() {
    val context = LocalContext.current.applicationContext
    val viewModel: MbSeekhoViewModel = viewModel(
        factory = MbSeekhoViewModel.Factory(
            repository = MbSeekhoRepository(MbSeekhoDatabase.getInstance(context).dao()),
            sessionPreferences = SessionPreferences(context),
            mediaStorageManager = MediaStorageManager(context)
        )
    )

    val screen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val loginUiState by viewModel.loginUiState.collectAsStateWithLifecycle()
    val uploadUiState by viewModel.uploadUiState.collectAsStateWithLifecycle()
    val commentActionState by viewModel.commentActionState.collectAsStateWithLifecycle()
    val publishedCourses by viewModel.publishedCourses.collectAsStateWithLifecycle()
    val allVideoLikes by viewModel.allVideoLikes.collectAsStateWithLifecycle()
    val allVideoComments by viewModel.allVideoComments.collectAsStateWithLifecycle()
    val savedCourses by viewModel.savedCourses.collectAsStateWithLifecycle()
    val progressList by viewModel.courseProgressList.collectAsStateWithLifecycle()
    val teacherCourses by viewModel.teacherCourses.collectAsStateWithLifecycle()
    val teacherStudioStats by viewModel.teacherStudioStats.collectAsStateWithLifecycle()
    val activeVideoLikes by viewModel.activeVideoLikes.collectAsStateWithLifecycle()
    val activeVideoComments by viewModel.activeVideoComments.collectAsStateWithLifecycle()
    val ownerSecurity by viewModel.ownerSecurity.collectAsStateWithLifecycle()
    val adminStudentCodeStats by viewModel.adminStudentCodeStats.collectAsStateWithLifecycle()
    val adminTeacherCodeStats by viewModel.adminTeacherCodeStats.collectAsStateWithLifecycle()
    val adminCodeFilter by viewModel.adminCodeFilter.collectAsStateWithLifecycle()
    val adminFilteredCodes by viewModel.adminFilteredCodes.collectAsStateWithLifecycle()
    val adminCodeBatches by viewModel.adminCodeBatches.collectAsStateWithLifecycle()
    val adminRecentRedemptions by viewModel.adminRecentRedemptions.collectAsStateWithLifecycle()
    val adminAvailableStudentCodes by viewModel.adminAvailableStudentCodes.collectAsStateWithLifecycle()
    val adminAvailableTeacherCodes by viewModel.adminAvailableTeacherCodes.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminCourses by viewModel.adminCourses.collectAsStateWithLifecycle()
    val adminPayments by viewModel.adminPayments.collectAsStateWithLifecycle()
    val adminTeacherCoins by viewModel.adminTeacherCoins.collectAsStateWithLifecycle()
    val adminCoinTransactions by viewModel.adminCoinTransactions.collectAsStateWithLifecycle()
    val adminValidViews by viewModel.adminValidViews.collectAsStateWithLifecycle()
    val adminComments by viewModel.adminComments.collectAsStateWithLifecycle()
    val adminFeedback by viewModel.adminFeedback.collectAsStateWithLifecycle()

    Crossfade(targetState = screen, label = "mb_seekho_screen") { current ->
        when (current) {
            AppScreen.Splash -> SplashScreen()

            AppScreen.RoleSelect -> RoleSelectionScreen(
                onSelectStudent = { viewModel.navigateTo(AppScreen.RoleLogin(UserRole.STUDENT)) },
                onSelectTeacher = { viewModel.navigateTo(AppScreen.RoleLogin(UserRole.TEACHER)) },
                onOpenOwnerConsole = { viewModel.navigateTo(AppScreen.OwnerAdminGate) }
            )

            is AppScreen.RoleLogin -> RoleLoginScreen(
                role = current.role,
                uiState = loginUiState,
                onRedeemStudentCode = { rawCode, displayName ->
                    viewModel.redeemStudentAccessCode(rawCode, displayName)
                },
                onRedeemTeacherCode = { rawCode, displayName ->
                    viewModel.redeemTeacherAccessCode(rawCode, displayName)
                },
                onReturningMemberSignIn = { memberId ->
                    viewModel.signInReturningMember(memberId, current.role)
                },
                onClearError = viewModel::clearLoginError,
                onBack = { viewModel.navigateTo(AppScreen.RoleSelect) }
            )

            AppScreen.StudentDashboard -> StudentDashboardScreen(
                student = currentUser,
                publishedCourses = publishedCourses,
                allVideoLikes = allVideoLikes,
                allVideoComments = allVideoComments,
                savedCourses = savedCourses,
                progressList = progressList,
                onOpenCourse = viewModel::openVideoPlayer,
                onOpenShortVideo = viewModel::openShortVideo,
                onToggleLike = viewModel::toggleVideoLike,
                onToggleSaveCourse = viewModel::toggleSaveCourse,
                onUpdateProfileName = { viewModel.updateProfileName(it) },
                onUpdateProfilePhoto = { viewModel.updateProfilePhoto(it) },
                onLogout = viewModel::logout
            )

            AppScreen.TeacherDashboard -> TeacherDashboardScreen(
                teacher = currentUser,
                teacherCourses = teacherCourses,
                studioStats = teacherStudioStats,
                uploadUiState = uploadUiState,
                onOpenCourse = viewModel::openVideoPlayer,
                onOpenTeacherStudio = viewModel::openTeacherStudio,
                onPublishVideo = viewModel::publishCourseFromTeacher,
                onInspectVideo = viewModel::inspectSelectedVideo,
                onExtractVideoPreview = viewModel::extractSelectedVideoPreview,
                onClearUploadStatus = viewModel::clearUploadStatus,
                onEditCourse = { id, title, desc, category, pub ->
                    viewModel.updateTeacherCourse(id, title, desc, category, pub)
                },
                onDeleteCourse = { viewModel.deleteTeacherCourse(it) },
                onUpdateProfileName = { viewModel.updateProfileName(it) },
                onUpdateProfilePhoto = { viewModel.updateProfilePhoto(it) },
                onLogout = viewModel::logout
            )

            AppScreen.TeacherStudio -> {
                // Hard UI guard: Students cannot access Teacher Studio
                if (currentUser?.role == UserRole.TEACHER.value && currentUser?.active == true) {
                    TeacherStudioScreen(
                        teacher = currentUser,
                        stats = teacherStudioStats,
                        onOpenVideo = viewModel::openVideoPlayer,
                        onBack = viewModel::closeTeacherStudio
                    )
                } else {
                    RoleSelectionScreen(
                        onSelectStudent = { viewModel.navigateTo(AppScreen.RoleLogin(UserRole.STUDENT)) },
                        onSelectTeacher = { viewModel.navigateTo(AppScreen.RoleLogin(UserRole.TEACHER)) },
                        onOpenOwnerConsole = { viewModel.navigateTo(AppScreen.OwnerAdminGate) }
                    )
                }
            }

            is AppScreen.ShortVideoFeed -> {
                val shortVideos = remember(publishedCourses, teacherCourses, adminCourses, current.initialCourseId) {
                    val publishedShorts = publishedCourses.filter {
                        VideoType.fromValue(it.videoType) == VideoType.SHORT
                    }
                    if (publishedShorts.any { it.courseId == current.initialCourseId }) {
                        publishedShorts
                    } else {
                        val fallbackTarget = teacherCourses.firstOrNull { it.courseId == current.initialCourseId }
                            ?: adminCourses.firstOrNull { it.courseId == current.initialCourseId }
                        if (fallbackTarget != null) {
                            listOf(fallbackTarget) + publishedShorts
                        } else {
                            publishedShorts
                        }
                    }
                }
                ShortVideoScreen(
                    shortVideos = shortVideos,
                    initialCourseId = current.initialCourseId,
                    openCommentsInitially = current.openCommentsInitially,
                    currentUser = currentUser,
                    allVideoLikes = allVideoLikes,
                    allVideoComments = allVideoComments,
                    commentActionState = commentActionState,
                    onToggleLike = viewModel::toggleVideoLike,
                    onPostComment = { videoId, text, onSuccess ->
                        viewModel.postVideoComment(videoId, text, onSuccess)
                    },
                    onDeleteComment = viewModel::deleteVideoComment,
                    onClearCommentFeedback = viewModel::clearCommentFeedback,
                    onPageViewed = viewModel::recordVideoView,
                    onBack = viewModel::closeVideoPlayer
                )
            }

            is AppScreen.VideoPlayer -> {
                val course = publishedCourses.firstOrNull { it.courseId == current.courseId }
                    ?: teacherCourses.firstOrNull { it.courseId == current.courseId }
                    ?: adminCourses.firstOrNull { it.courseId == current.courseId }
                val isSaved = savedCourses.any { it.courseId == current.courseId }
                val progress = progressList.firstOrNull { it.courseId == current.courseId }
                val relatedVideos = remember(publishedCourses, course) {
                    val activeCategory = course?.let { CourseCategories.normalize(it.category) }
                    publishedCourses
                        .filter {
                            it.courseId != current.courseId &&
                                VideoType.fromValue(it.videoType) == VideoType.LONG
                        }
                        .sortedWith(
                            compareByDescending<com.example.data.local.CourseEntity> {
                                activeCategory != null &&
                                    CourseCategories.normalize(it.category).equals(activeCategory, ignoreCase = true)
                            }.thenByDescending { it.createdAt }
                        )
                        .take(8)
                }
                val likeCountsByVideo = remember(allVideoLikes) {
                    allVideoLikes.groupingBy { it.videoId }.eachCount()
                }
                val commentCountsByVideo = remember(allVideoComments) {
                    allVideoComments.groupingBy { it.videoId }.eachCount()
                }
                VideoPlayerScreen(
                    course = course,
                    relatedVideos = relatedVideos,
                    likeCountsByVideo = likeCountsByVideo,
                    commentCountsByVideo = commentCountsByVideo,
                    currentUser = currentUser,
                    isSaved = isSaved,
                    progress = progress,
                    likes = activeVideoLikes,
                    comments = activeVideoComments,
                    commentActionState = commentActionState,
                    onToggleLike = { viewModel.toggleVideoLike(current.courseId) },
                    onToggleSave = { viewModel.toggleSaveCourse(current.courseId) },
                    onPostComment = { text, onSuccess ->
                        viewModel.postVideoComment(current.courseId, text, onSuccess)
                    },
                    onDeleteComment = viewModel::deleteVideoComment,
                    onClearCommentFeedback = viewModel::clearCommentFeedback,
                    onUpdateProgress = { pos, dur -> viewModel.updateCourseProgress(current.courseId, pos, dur) },
                    onMarkCompleted = { viewModel.markCourseCompleted(current.courseId) },
                    onSelectRelatedVideo = viewModel::openVideoPlayer,
                    onBack = viewModel::closeVideoPlayer
                )
            }

            AppScreen.OwnerAdminGate -> OwnerAdminGateScreen(
                ownerSecurity = ownerSecurity,
                uiState = loginUiState,
                onClaimOwner = viewModel::claimOwnerAdmin,
                onVerifyOwner = viewModel::verifyOwnerAdmin,
                onClearError = viewModel::clearLoginError,
                onBack = { viewModel.navigateTo(AppScreen.RoleSelect) }
            )

            AppScreen.OwnerAdminPanel -> {
                // Hard UI guard: never render Admin Panel unless verified Owner Admin is active
                if (currentUser?.role != UserRole.ADMIN.value) {
                    OwnerAdminGateScreen(
                        ownerSecurity = ownerSecurity,
                        uiState = loginUiState,
                        onClaimOwner = viewModel::claimOwnerAdmin,
                        onVerifyOwner = viewModel::verifyOwnerAdmin,
                        onClearError = viewModel::clearLoginError,
                        onBack = { viewModel.navigateTo(AppScreen.RoleSelect) }
                    )
                } else {
                    AdminPanelScreen(
                        studentCodeStats = adminStudentCodeStats,
                        teacherCodeStats = adminTeacherCodeStats,
                        codeFilter = adminCodeFilter,
                        filteredCodes = adminFilteredCodes,
                        codeBatches = adminCodeBatches,
                        recentRedemptions = adminRecentRedemptions,
                        availableStudentCodes = adminAvailableStudentCodes,
                        availableTeacherCodes = adminAvailableTeacherCodes,
                        users = adminUsers,
                        courses = adminCourses,
                        payments = adminPayments,
                        teacherCoins = adminTeacherCoins,
                        coinTransactions = adminCoinTransactions,
                        validViews = adminValidViews,
                        comments = adminComments,
                        feedback = adminFeedback,
                        onCodeRoleFilterChange = viewModel::setAdminCodeRoleFilter,
                        onCodeStatusFilterChange = viewModel::setAdminCodeStatusFilter,
                        onCodeSearchQueryChange = viewModel::setAdminCodeSearchQuery,
                        onLoadMoreCodes = viewModel::loadMoreAdminCodes,
                        onImportCodesFile = { uri, fileName, role ->
                            viewModel.importCodesFromUriAsAdmin(context.contentResolver, uri, fileName, role)
                        },
                        onImportCodesText = viewModel::importCodesFromTextAsAdmin,
                        onGenerateCodesBatch = viewModel::generateCodesBatchAsAdmin,
                        onShareGeneratedBatchCsv = viewModel::shareGeneratedBatchCsv,
                        onAssignCode = viewModel::assignCodeAsAdmin,
                        onDisableCode = viewModel::disableCodeAsAdmin,
                        onEnableCode = viewModel::enableCodeAsAdmin,
                        onToggleUserActive = viewModel::setUserActiveAsAdmin,
                        onRemoveCourse = viewModel::removeCourseAsAdmin,
                        onUpdateCourseCategory = viewModel::updateCourseCategoryAsAdmin,
                        onDeleteComment = viewModel::deleteCommentAsAdmin,
                        onRecordPayment = viewModel::recordPaymentAsAdmin,
                        onVerifyPayment = viewModel::verifyPaymentAsAdmin,
                        onExportCsv = viewModel::exportCodesCsvAsAdmin,
                        onClearGeneratedReveal = viewModel::clearGeneratedBatchReveal,
                        onClearExportedCsv = viewModel::clearExportedCsv,
                        onLogout = viewModel::logout
                    )
                }
            }
        }
    }
}

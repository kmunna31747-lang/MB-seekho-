package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.MbSeekhoDatabase
import com.example.data.local.MediaStorageManager
import com.example.data.local.SessionPreferences
import com.example.data.model.UserRole
import com.example.data.repository.MbSeekhoRepository
import com.example.ui.AppScreen
import com.example.ui.MbSeekhoViewModel
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.OwnerAdminGateScreen
import com.example.ui.screens.RoleLoginScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StudentDashboardScreen
import com.example.ui.screens.TeacherDashboardScreen
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
    val publishedCourses by viewModel.publishedCourses.collectAsStateWithLifecycle()
    val savedCourses by viewModel.savedCourses.collectAsStateWithLifecycle()
    val progressList by viewModel.courseProgressList.collectAsStateWithLifecycle()
    val teacherCourses by viewModel.teacherCourses.collectAsStateWithLifecycle()
    val ownerSecurity by viewModel.ownerSecurity.collectAsStateWithLifecycle()
    val adminCodes by viewModel.adminAccessCodes.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminCourses by viewModel.adminCourses.collectAsStateWithLifecycle()
    val adminPayments by viewModel.adminPayments.collectAsStateWithLifecycle()
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
                onRedeemCode = { rawCode, displayName ->
                    viewModel.redeemAccessCode(rawCode, current.role, displayName)
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
                savedCourses = savedCourses,
                progressList = progressList,
                onOpenCourse = viewModel::openVideoPlayer,
                onToggleSaveCourse = viewModel::toggleSaveCourse,
                onUpdateProfileName = { viewModel.updateProfileName(it) },
                onUpdateProfilePhoto = { viewModel.updateProfilePhoto(it) },
                onLogout = viewModel::logout
            )

            AppScreen.TeacherDashboard -> TeacherDashboardScreen(
                teacher = currentUser,
                teacherCourses = teacherCourses,
                uploadUiState = uploadUiState,
                onOpenCourse = viewModel::openVideoPlayer,
                onPublishCourse = viewModel::publishCourseFromTeacher,
                onClearUploadStatus = viewModel::clearUploadStatus,
                onEditCourse = { id, title, desc, cat, pub ->
                    viewModel.updateTeacherCourse(id, title, desc, cat, pub)
                },
                onDeleteCourse = { viewModel.deleteTeacherCourse(it) },
                onUpdateProfileName = { viewModel.updateProfileName(it) },
                onUpdateProfilePhoto = { viewModel.updateProfilePhoto(it) },
                onLogout = viewModel::logout
            )

            is AppScreen.VideoPlayer -> {
                val course = publishedCourses.firstOrNull { it.courseId == current.courseId }
                    ?: teacherCourses.firstOrNull { it.courseId == current.courseId }
                val isSaved = savedCourses.any { it.courseId == current.courseId }
                val progress = progressList.firstOrNull { it.courseId == current.courseId }
                VideoPlayerScreen(
                    course = course,
                    isSaved = isSaved,
                    progress = progress,
                    isStudent = currentUser?.role == UserRole.STUDENT.value,
                    onToggleSave = { viewModel.toggleSaveCourse(current.courseId) },
                    onUpdateProgress = { pos, dur -> viewModel.updateCourseProgress(current.courseId, pos, dur) },
                    onMarkCompleted = { viewModel.markCourseCompleted(current.courseId) },
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
                        codes = adminCodes,
                        users = adminUsers,
                        courses = adminCourses,
                        payments = adminPayments,
                        feedback = adminFeedback,
                        onAssignCode = viewModel::assignCodeAsAdmin,
                        onGenerateCodes = viewModel::generateCodesAsAdmin,
                        onRevokeCode = viewModel::revokeCodeAsAdmin,
                        onToggleUserActive = viewModel::setUserActiveAsAdmin,
                        onRemoveCourse = viewModel::removeCourseAsAdmin,
                        onRecordPayment = viewModel::recordPaymentAsAdmin,
                        onVerifyPayment = viewModel::verifyPaymentAsAdmin,
                        onExportCsv = viewModel::exportCodesCsvAsAdmin,
                        onClearGeneratedReveal = viewModel::clearGeneratedCodesReveal,
                        onClearExportedCsv = viewModel::clearExportedCsv,
                        onLogout = viewModel::logout
                    )
                }
            }
        }
    }
}

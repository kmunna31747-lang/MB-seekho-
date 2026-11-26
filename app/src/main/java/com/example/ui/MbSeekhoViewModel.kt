package com.example.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccessCodeEntity
import com.example.data.local.CodeBatchEntity
import com.example.data.local.CodeRoleStats
import com.example.data.local.CoinTransactionEntity
import com.example.data.local.CourseEntity
import com.example.data.local.CourseProgressEntity
import com.example.data.local.MediaStorageManager
import com.example.data.local.OwnerSecurityEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.SavedCourseEntity
import com.example.data.local.SelectedVideoInfo
import com.example.data.local.SessionPreferences
import com.example.data.local.TeacherCoinsEntity
import com.example.data.local.UserEntity
import com.example.data.local.VideoCommentEntity
import com.example.data.local.VideoLikeEntity
import com.example.data.local.VideoViewEntity
import com.example.data.model.CourseCategories
import com.example.data.model.PaymentStatus
import com.example.data.model.UserRole
import com.example.data.model.VideoType
import com.example.data.repository.CodeRedemptionResult
import com.example.data.repository.GeneratedBatchResult
import com.example.data.repository.MbSeekhoRepository
import com.example.data.repository.TeacherStudioStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

sealed interface AppScreen {
    data object Splash : AppScreen
    data object RoleSelect : AppScreen
    data class RoleLogin(val role: UserRole) : AppScreen
    data object StudentDashboard : AppScreen
    data object TeacherDashboard : AppScreen
    data object TeacherStudio : AppScreen
    data class VideoPlayer(val courseId: String, val returnTo: AppScreen) : AppScreen
    data class ShortVideoFeed(
        val initialCourseId: String,
        val returnTo: AppScreen,
        val openCommentsInitially: Boolean = false
    ) : AppScreen
    data object OwnerAdminGate : AppScreen
    data object OwnerAdminPanel : AppScreen
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class UploadUiState(
    val isUploading: Boolean = false,
    val progress: Float = 0f,
    val statusText: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastUploadedCourseId: String? = null
)

data class CommentActionState(
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class AdminCodeFilterState(
    val role: UserRole = UserRole.STUDENT,
    val statusFilter: String = "all", // "all" | "unused" | "used" | "disabled"
    val searchQuery: String = "",
    val pageLimit: Int = 100
)

data class AdminFeedbackState(
    val message: String? = null,
    val error: String? = null,
    val isBatchBusy: Boolean = false,
    val batchProgressText: String? = null,
    val generatedBatch: GeneratedBatchResult? = null,
    val exportedCsv: String? = null,
    val exportedCsvSubject: String = "MB Seekho Access Codes Export"
)

@OptIn(ExperimentalCoroutinesApi::class)
class MbSeekhoViewModel(
    private val repository: MbSeekhoRepository,
    private val sessionPreferences: SessionPreferences,
    private val mediaStorageManager: MediaStorageManager
) : ViewModel() {

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Splash)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _activeUid = MutableStateFlow<String?>(null)
    val activeUid: StateFlow<String?> = _activeUid.asStateFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _uploadUiState = MutableStateFlow(UploadUiState())
    val uploadUiState: StateFlow<UploadUiState> = _uploadUiState.asStateFlow()

    private val _commentActionState = MutableStateFlow(CommentActionState())
    val commentActionState: StateFlow<CommentActionState> = _commentActionState.asStateFlow()

    private val _adminCodeFilter = MutableStateFlow(AdminCodeFilterState())
    val adminCodeFilter: StateFlow<AdminCodeFilterState> = _adminCodeFilter.asStateFlow()

    private val _adminFeedback = MutableStateFlow(AdminFeedbackState())
    val adminFeedback: StateFlow<AdminFeedbackState> = _adminFeedback.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _activeUid
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(null) else repository.observeUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val publishedCourses: StateFlow<List<CourseEntity>> = repository.observePublishedCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVideoLikes: StateFlow<List<VideoLikeEntity>> = repository.observeAllVideoLikes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVideoComments: StateFlow<List<VideoCommentEntity>> = repository.observeAllVideoComments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedCourses: StateFlow<List<SavedCourseEntity>> = _activeUid
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.observeSavedCourses(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val courseProgressList: StateFlow<List<CourseProgressEntity>> = _activeUid
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.observeCourseProgress(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teacherCourses: StateFlow<List<CourseEntity>> = _activeUid
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.observeTeacherCourses(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Strictly scoped to verified active Teachers only (Students cannot access Teacher Studio data)
    val teacherStudioStats: StateFlow<TeacherStudioStats> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.TEACHER.value && user.active) {
                repository.observeTeacherStudioStats(user.uid)
            } else {
                flowOf(TeacherStudioStats())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TeacherStudioStats())

    // Active Video Player Likes & Comments
    val activeVideoLikes: StateFlow<List<VideoLikeEntity>> = _currentScreen
        .flatMapLatest { screen ->
            if (screen is AppScreen.VideoPlayer) repository.observeLikesForVideo(screen.courseId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeVideoComments: StateFlow<List<VideoCommentEntity>> = _currentScreen
        .flatMapLatest { screen ->
            if (screen is AppScreen.VideoPlayer) repository.observeCommentsForVideo(screen.courseId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ownerSecurity: StateFlow<OwnerSecurityEntity?> = repository.observeOwnerSecurity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Strictly Admin-Scoped Scalable Code Management & Analytics Flows ---

    val adminStudentCodeStats: StateFlow<CodeRoleStats> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeCodeStats(UserRole.STUDENT)
            else flowOf(CodeRoleStats())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CodeRoleStats())

    val adminTeacherCodeStats: StateFlow<CodeRoleStats> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeCodeStats(UserRole.TEACHER)
            else flowOf(CodeRoleStats())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CodeRoleStats())

    val adminFilteredCodes: StateFlow<List<AccessCodeEntity>> = combine(
        currentUser,
        _adminCodeFilter
    ) { user, filter ->
        if (user?.role == UserRole.ADMIN.value && user.active) filter else null
    }.flatMapLatest { filter ->
        if (filter == null) flowOf(emptyList())
        else repository.observeFilteredCodes(
            role = filter.role,
            statusFilter = filter.statusFilter,
            searchQuery = filter.searchQuery,
            limit = filter.pageLimit
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminCodeBatches: StateFlow<List<CodeBatchEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeCodeBatches()
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminRecentRedemptions: StateFlow<List<AccessCodeEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeRecentRedemptions(20)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminAvailableStudentCodes: StateFlow<List<AccessCodeEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeAvailableCodesByRole(UserRole.STUDENT, 50)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminAvailableTeacherCodes: StateFlow<List<AccessCodeEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeAvailableCodesByRole(UserRole.TEACHER, 50)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminUsers: StateFlow<List<UserEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeAllUsers()
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminCourses: StateFlow<List<CourseEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeAllCoursesForAdmin()
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminPayments: StateFlow<List<PaymentEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeAllPayments()
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminTeacherCoins: StateFlow<List<TeacherCoinsEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeAllTeacherCoinsForAdmin()
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminCoinTransactions: StateFlow<List<CoinTransactionEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeAllCoinTransactionsForAdmin()
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminValidViews: StateFlow<List<VideoViewEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeAllValidViewsForAdmin()
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminComments: StateFlow<List<VideoCommentEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeAllCommentsForAdmin()
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeInventory()
            delay(1300L)
            val savedUid = sessionPreferences.activeUidFlow.firstOrNull()
            val existingUser = savedUid?.let { repository.getUserById(it) }
            if (existingUser != null && existingUser.active && existingUser.role != UserRole.ADMIN.value) {
                _activeUid.value = existingUser.uid
                _currentScreen.value = when (UserRole.fromValue(existingUser.role)) {
                    UserRole.STUDENT -> AppScreen.StudentDashboard
                    UserRole.TEACHER -> AppScreen.TeacherDashboard
                    UserRole.ADMIN -> AppScreen.RoleSelect
                }
            } else {
                _currentScreen.value = AppScreen.RoleSelect
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _loginUiState.value = LoginUiState()
        _commentActionState.value = CommentActionState()
        if (screen == AppScreen.TeacherStudio && currentUser.value?.role != UserRole.TEACHER.value) {
            return
        }
        _currentScreen.value = screen
    }

    fun openTeacherStudio() {
        val user = currentUser.value
        if (user?.role == UserRole.TEACHER.value && user.active) {
            _currentScreen.value = AppScreen.TeacherStudio
        }
    }

    fun closeTeacherStudio() {
        if (_currentScreen.value == AppScreen.TeacherStudio) {
            _currentScreen.value = AppScreen.TeacherDashboard
        }
    }

    fun openVideoPlayer(courseId: String) {
        val targetCourse = publishedCourses.value.firstOrNull { it.courseId == courseId }
            ?: teacherCourses.value.firstOrNull { it.courseId == courseId }
            ?: adminCourses.value.firstOrNull { it.courseId == courseId }
        if (targetCourse != null && VideoType.fromValue(targetCourse.videoType) == VideoType.SHORT) {
            openShortVideo(courseId = courseId, openComments = false)
            return
        }

        val current = _currentScreen.value
        val returnScreen = when (current) {
            is AppScreen.VideoPlayer -> current.returnTo
            is AppScreen.ShortVideoFeed -> current.returnTo
            else -> current
        }
        _commentActionState.value = CommentActionState()
        _currentScreen.value = AppScreen.VideoPlayer(courseId = courseId, returnTo = returnScreen)
        recordVideoView(courseId)
    }

    fun openShortVideo(courseId: String, openComments: Boolean = false) {
        val current = _currentScreen.value
        val returnScreen = when (current) {
            is AppScreen.VideoPlayer -> current.returnTo
            is AppScreen.ShortVideoFeed -> current.returnTo
            else -> current
        }
        _commentActionState.value = CommentActionState()
        _currentScreen.value = AppScreen.ShortVideoFeed(
            initialCourseId = courseId,
            returnTo = returnScreen,
            openCommentsInitially = openComments
        )
        recordVideoView(courseId)
    }

    fun recordVideoView(courseId: String) {
        val viewerUid = _activeUid.value
        if (!viewerUid.isNullOrBlank()) {
            viewModelScope.launch {
                repository.recordValidatedVideoView(videoId = courseId, viewerUid = viewerUid)
            }
        }
    }

    fun closeVideoPlayer() {
        val current = _currentScreen.value
        _commentActionState.value = CommentActionState()
        when (current) {
            is AppScreen.VideoPlayer -> _currentScreen.value = current.returnTo
            is AppScreen.ShortVideoFeed -> _currentScreen.value = current.returnTo
            else -> Unit
        }
    }

    fun clearLoginError() {
        _loginUiState.update { it.copy(errorMessage = null) }
    }

    // --- Student & Teacher Access Code Sign-In (SHA-256 Single-Use) ---

    fun redeemStudentAccessCode(rawCode: String, displayName: String) {
        redeemAccessCodeForRole(rawCode, UserRole.STUDENT, displayName)
    }

    fun redeemTeacherAccessCode(rawCode: String, displayName: String) {
        redeemAccessCodeForRole(rawCode, UserRole.TEACHER, displayName)
    }

    private fun redeemAccessCodeForRole(rawCode: String, role: UserRole, displayName: String) {
        val roleTitle = role.label
        viewModelScope.launch {
            _loginUiState.value = LoginUiState(isLoading = true, errorMessage = null)
            when (val result = repository.redeemAccessCode(rawCode, role, displayName)) {
                is CodeRedemptionResult.Success -> {
                    _activeUid.value = result.user.uid
                    sessionPreferences.saveActiveUid(result.user.uid)
                    _loginUiState.value = LoginUiState()
                    _currentScreen.value = if (role == UserRole.TEACHER) {
                        AppScreen.TeacherDashboard
                    } else {
                        AppScreen.StudentDashboard
                    }
                }
                CodeRedemptionResult.InvalidCode -> {
                    _loginUiState.value = LoginUiState(
                        errorMessage = "Invalid $roleTitle Access Code. Please verify your code or contact the MB Seekho owner."
                    )
                }
                CodeRedemptionResult.AlreadyUsed -> {
                    _loginUiState.value = LoginUiState(
                        errorMessage = "Already Used Code: This $roleTitle Access Code has already been redeemed and cannot be reused."
                    )
                }
                CodeRedemptionResult.Disabled -> {
                    _loginUiState.value = LoginUiState(
                        errorMessage = "Disabled Code: This $roleTitle Access Code has been disabled by the administrator."
                    )
                }
                CodeRedemptionResult.WrongPortal -> {
                    _loginUiState.value = LoginUiState(
                        errorMessage = "Wrong Portal: This access code is not valid for $roleTitle Login."
                    )
                }
                CodeRedemptionResult.AccountDisabled -> {
                    _loginUiState.value = LoginUiState(
                        errorMessage = "This account has been disabled by the administrator."
                    )
                }
                is CodeRedemptionResult.RateLimited -> {
                    _loginUiState.value = LoginUiState(
                        errorMessage = "Too many failed attempts. Please wait ${result.waitSeconds} seconds and try again."
                    )
                }
            }
        }
    }

    fun signInReturningMember(memberId: String, role: UserRole) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState(isLoading = true, errorMessage = null)
            when (val result = repository.signInReturningMember(memberId, role)) {
                is CodeRedemptionResult.Success -> {
                    _activeUid.value = result.user.uid
                    sessionPreferences.saveActiveUid(result.user.uid)
                    _loginUiState.value = LoginUiState()
                    _currentScreen.value = when (role) {
                        UserRole.STUDENT -> AppScreen.StudentDashboard
                        UserRole.TEACHER -> AppScreen.TeacherDashboard
                        UserRole.ADMIN -> AppScreen.OwnerAdminPanel
                    }
                }
                CodeRedemptionResult.AccountDisabled -> {
                    _loginUiState.value = LoginUiState(
                        errorMessage = "This account has been disabled by the administrator."
                    )
                }
                is CodeRedemptionResult.RateLimited -> {
                    _loginUiState.value = LoginUiState(
                        errorMessage = "Too many failed attempts. Please wait ${result.waitSeconds} seconds."
                    )
                }
                else -> {
                    _loginUiState.value = LoginUiState(
                        errorMessage = if (role == UserRole.TEACHER) {
                            "Teacher ID not found. New teachers must activate with a private Teacher Access Code."
                        } else {
                            "Student ID not found. New students must activate with a valid Student Access Code."
                        }
                    )
                }
            }
        }
    }

    fun claimOwnerAdmin(ownerName: String, passphrase: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState(isLoading = true, errorMessage = null)
            repository.claimOwnerAdmin(ownerName, passphrase)
                .onSuccess { owner ->
                    _activeUid.value = owner.uid
                    _loginUiState.value = LoginUiState()
                    _currentScreen.value = AppScreen.OwnerAdminPanel
                }
                .onFailure { err ->
                    _loginUiState.value = LoginUiState(errorMessage = err.message ?: "Owner setup failed.")
                }
        }
    }

    fun verifyOwnerAdmin(passphrase: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState(isLoading = true, errorMessage = null)
            repository.verifyOwnerAdmin(passphrase)
                .onSuccess { owner ->
                    _activeUid.value = owner.uid
                    _loginUiState.value = LoginUiState()
                    _currentScreen.value = AppScreen.OwnerAdminPanel
                }
                .onFailure { err ->
                    _loginUiState.value = LoginUiState(errorMessage = err.message ?: "Invalid owner credentials.")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionPreferences.clearSession()
            _activeUid.value = null
            _adminFeedback.value = AdminFeedbackState()
            _uploadUiState.value = UploadUiState()
            _commentActionState.value = CommentActionState()
            _currentScreen.value = AppScreen.RoleSelect
        }
    }

    // --- Profile ---

    fun updateProfileName(newName: String, onDone: (String?) -> Unit = {}) {
        val uid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.updateProfileName(uid, newName)
                .onSuccess { onDone(null) }
                .onFailure { onDone(it.message ?: "Failed to update name.") }
        }
    }

    fun updateProfilePhoto(sourceUri: Uri, onDone: (String?) -> Unit = {}) {
        val uid = _activeUid.value ?: return
        viewModelScope.launch {
            mediaStorageManager.copyMediaToAppStorage(sourceUri, "avatars", "jpg")
                .onSuccess { storedUri ->
                    repository.updateProfilePhoto(uid, storedUri)
                    onDone(null)
                }
                .onFailure { err ->
                    onDone(err.message ?: "Unable to save profile photo.")
                }
        }
    }

    // --- Student & Video Watch Interactions (Save, Progress, Likes, Comments) ---

    fun toggleSaveCourse(courseId: String) {
        val uid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.toggleSaveCourse(uid, courseId)
        }
    }

    fun toggleVideoLike(videoId: String) {
        val uid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.toggleVideoLike(videoId, uid)
        }
    }

    fun clearCommentFeedback() {
        _commentActionState.value = CommentActionState()
    }

    fun postVideoComment(videoId: String, commentText: String, onSuccess: () -> Unit = {}) {
        val uid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.postVideoComment(videoId = videoId, actorUid = uid, rawText = commentText)
                .onSuccess {
                    _commentActionState.value = CommentActionState(successMessage = "Comment posted!")
                    onSuccess()
                }
                .onFailure { err ->
                    _commentActionState.value = CommentActionState(
                        errorMessage = err.message ?: "Unable to post comment."
                    )
                }
        }
    }

    fun deleteVideoComment(commentId: String) {
        val uid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.deleteVideoComment(actorUid = uid, commentId = commentId)
                .onSuccess {
                    _commentActionState.value = CommentActionState(successMessage = "Comment removed.")
                }
                .onFailure { err ->
                    _commentActionState.value = CommentActionState(
                        errorMessage = err.message ?: "Unable to delete comment."
                    )
                }
        }
    }

    fun updateCourseProgress(courseId: String, positionMs: Long, durationMs: Long) {
        val uid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.updateCourseProgress(uid, courseId, positionMs, durationMs)
        }
    }

    fun markCourseCompleted(courseId: String) {
        val uid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.markCourseCompleted(uid, courseId)
        }
    }

    // --- Teacher Video Upload & Management ---

    fun clearUploadStatus() {
        _uploadUiState.value = UploadUiState()
    }

    suspend fun inspectSelectedVideo(uri: Uri): Result<SelectedVideoInfo> =
        mediaStorageManager.inspectVideoUri(uri)

    suspend fun extractSelectedVideoPreview(uri: Uri): Bitmap? =
        mediaStorageManager.extractVideoPreviewBitmap(uri)

    fun publishCourseFromTeacher(
        title: String,
        description: String,
        category: String,
        videoType: VideoType = VideoType.LONG,
        videoUri: Uri?
    ) {
        val teacherUid = _activeUid.value ?: return
        val cleanTitle = title.trim()
        val cleanDescription = description.trim()
        val cleanCategory = CourseCategories.normalize(category)

        if (videoUri == null) {
            _uploadUiState.value = UploadUiState(errorMessage = "Please select a video from your gallery first.")
            return
        }
        if (cleanTitle.isEmpty()) {
            _uploadUiState.value = UploadUiState(errorMessage = "Please enter a video title.")
            return
        }
        if (cleanTitle.length < 3) {
            _uploadUiState.value = UploadUiState(errorMessage = "Video title must be at least 3 characters.")
            return
        }
        if (cleanDescription.isEmpty()) {
            _uploadUiState.value = UploadUiState(errorMessage = "Please enter a video description.")
            return
        }
        if (cleanDescription.length < 5) {
            _uploadUiState.value = UploadUiState(errorMessage = "Video description must be at least 5 characters.")
            return
        }

        viewModelScope.launch {
            _uploadUiState.value = UploadUiState(
                isUploading = true,
                progress = 0.06f,
                statusText = "Validating selected ${videoType.label.lowercase()}..."
            )

            val videoResult = mediaStorageManager.validateAndCopyVideo(videoUri) { frac ->
                val pct = (frac * 100).toInt().coerceIn(0, 100)
                val status = if (frac >= 0.94f) "Generating video preview..." else "Uploading ${videoType.label.lowercase()} ($pct%)..."
                _uploadUiState.update {
                    it.copy(
                        progress = (0.08f + frac * 0.82f).coerceIn(0.08f, 0.90f),
                        statusText = status
                    )
                }
            }

            if (videoResult.isFailure) {
                _uploadUiState.value = UploadUiState(
                    isUploading = false,
                    errorMessage = videoResult.exceptionOrNull()?.message
                        ?: "Failed to upload the selected video. Please try again."
                )
                return@launch
            }

            val storedVideo = videoResult.getOrThrow()
            _uploadUiState.update {
                it.copy(progress = 0.95f, statusText = "Publishing ${videoType.label} to $cleanCategory...")
            }

            repository.publishCourse(
                teacherUid = teacherUid,
                title = cleanTitle,
                description = cleanDescription,
                category = cleanCategory,
                thumbnailUrl = storedVideo.previewImageUrl,
                videoUrl = storedVideo.videoUrl,
                published = true,
                videoType = videoType.value,
                durationMs = storedVideo.durationMs
            ).onSuccess { publishedCourse ->
                _uploadUiState.value = UploadUiState(
                    isUploading = false,
                    progress = 1f,
                    successMessage = "${videoType.label} \"${publishedCourse.title}\" uploaded to $cleanCategory! It is now live on Student Home.",
                    lastUploadedCourseId = publishedCourse.courseId
                )
            }.onFailure { err ->
                _uploadUiState.value = UploadUiState(
                    isUploading = false,
                    errorMessage = err.message ?: "Failed to save video details."
                )
            }
        }
    }

    fun updateTeacherCourse(
        courseId: String,
        title: String,
        description: String,
        category: String,
        published: Boolean,
        onDone: (String?) -> Unit = {}
    ) {
        val teacherUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.updateTeacherCourse(teacherUid, courseId, title, description, category, published)
                .onSuccess { onDone(null) }
                .onFailure { onDone(it.message ?: "Unable to update video.") }
        }
    }

    fun deleteTeacherCourse(courseId: String, onDone: (String?) -> Unit = {}) {
        val teacherUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.deleteTeacherCourse(teacherUid, courseId)
                .onSuccess { onDone(null) }
                .onFailure { onDone(it.message ?: "Unable to delete video.") }
        }
    }

    // --- Owner Admin Scalable Access Code Controls ---

    fun setAdminCodeRoleFilter(role: UserRole) {
        _adminCodeFilter.update { it.copy(role = role, pageLimit = 100) }
    }

    fun setAdminCodeStatusFilter(status: String) {
        _adminCodeFilter.update { it.copy(statusFilter = status, pageLimit = 100) }
    }

    fun setAdminCodeSearchQuery(query: String) {
        _adminCodeFilter.update { it.copy(searchQuery = query, pageLimit = 100) }
    }

    fun loadMoreAdminCodes() {
        _adminCodeFilter.update { it.copy(pageLimit = (it.pageLimit + 100).coerceAtMost(1000)) }
    }

    fun clearAdminFeedback() {
        _adminFeedback.update { it.copy(message = null, error = null) }
    }

    fun clearGeneratedBatchReveal() {
        _adminFeedback.update { it.copy(generatedBatch = null) }
    }

    fun clearExportedCsv() {
        _adminFeedback.update { it.copy(exportedCsv = null) }
    }

    fun shareGeneratedBatchCsv() {
        val batch = _adminFeedback.value.generatedBatch ?: return
        _adminFeedback.update {
            it.copy(
                exportedCsv = batch.csvContent,
                exportedCsvSubject = "MB Seekho ${batch.role.label} Access Codes (${batch.batchId})"
            )
        }
    }

    fun importCodesFromUriAsAdmin(
        contentResolver: ContentResolver,
        uri: Uri,
        fileName: String,
        role: UserRole
    ) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            _adminFeedback.update {
                it.copy(
                    isBatchBusy = true,
                    batchProgressText = "Reading $fileName...",
                    error = null,
                    message = null
                )
            }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                            repository.importAccessCodesFromLines(
                                adminUid = adminUid,
                                role = role,
                                sourceLabel = "File Import: $fileName",
                                lines = reader.lineSequence(),
                                onProgress = { processed ->
                                    _adminFeedback.update {
                                        it.copy(batchProgressText = "Hashing & importing $processed codes...")
                                    }
                                }
                            ).getOrThrow()
                        }
                    } ?: error("Unable to open the selected file.")
                }
            }
            result.onSuccess { summary ->
                _adminFeedback.update {
                    it.copy(
                        isBatchBusy = false,
                        batchProgressText = null,
                        error = null,
                        message = "Imported ${summary.insertedCount} unique ${role.label} code(s) (${summary.duplicateCount} duplicates skipped, ${summary.invalidCount} invalid lines)."
                    )
                }
            }.onFailure { err ->
                _adminFeedback.update {
                    it.copy(
                        isBatchBusy = false,
                        batchProgressText = null,
                        error = err.message ?: "Code import failed."
                    )
                }
            }
        }
    }

    fun importCodesFromTextAsAdmin(rawText: String, role: UserRole) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            _adminFeedback.update {
                it.copy(
                    isBatchBusy = true,
                    batchProgressText = "Validating & hashing pasted codes...",
                    error = null,
                    message = null
                )
            }
            val result = withContext(Dispatchers.IO) {
                repository.importAccessCodesFromLines(
                    adminUid = adminUid,
                    role = role,
                    sourceLabel = "Admin Paste Import",
                    lines = rawText.lineSequence(),
                    onProgress = { processed ->
                        _adminFeedback.update {
                            it.copy(batchProgressText = "Importing $processed codes...")
                        }
                    }
                )
            }
            result.onSuccess { summary ->
                _adminFeedback.update {
                    it.copy(
                        isBatchBusy = false,
                        batchProgressText = null,
                        error = null,
                        message = "Imported ${summary.insertedCount} unique ${role.label} code(s) (${summary.duplicateCount} duplicates skipped)."
                    )
                }
            }.onFailure { err ->
                _adminFeedback.update {
                    it.copy(
                        isBatchBusy = false,
                        batchProgressText = null,
                        error = err.message ?: "Paste import failed."
                    )
                }
            }
        }
    }

    fun generateCodesBatchAsAdmin(role: UserRole, count: Int, batchLabel: String = "") {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            _adminFeedback.update {
                it.copy(
                    isBatchBusy = true,
                    batchProgressText = "Generating $count unique ${role.label} codes...",
                    error = null,
                    message = null
                )
            }
            val result = withContext(Dispatchers.IO) {
                repository.generateAccessCodesBatch(
                    adminUid = adminUid,
                    role = role,
                    count = count,
                    batchLabel = batchLabel,
                    onProgress = { generated ->
                        _adminFeedback.update {
                            it.copy(batchProgressText = "Generated $generated / $count unique codes...")
                        }
                    }
                )
            }
            result.onSuccess { batch ->
                _adminFeedback.update {
                    it.copy(
                        isBatchBusy = false,
                        batchProgressText = null,
                        error = null,
                        message = "Generated ${batch.generatedCount} new unique ${role.label} Access Code(s). Export or copy them privately now — only SHA-256 hashes are stored.",
                        generatedBatch = batch
                    )
                }
            }.onFailure { err ->
                _adminFeedback.update {
                    it.copy(
                        isBatchBusy = false,
                        batchProgressText = null,
                        error = err.message ?: "Code batch generation failed."
                    )
                }
            }
        }
    }

    fun assignCodeAsAdmin(codeId: String, customerName: String) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.assignAccessCode(adminUid, codeId, customerName)
                .onSuccess {
                    _adminFeedback.update { it.copy(message = "Assigned $codeId to $customerName.", error = null) }
                }
                .onFailure { err ->
                    _adminFeedback.update { it.copy(error = err.message ?: "Assignment failed.") }
                }
        }
    }

    fun disableCodeAsAdmin(codeId: String) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.disableAccessCode(adminUid, codeId)
                .onSuccess {
                    _adminFeedback.update { it.copy(message = "Access code $codeId disabled.", error = null) }
                }
                .onFailure { err ->
                    _adminFeedback.update { it.copy(error = err.message ?: "Disable failed.") }
                }
        }
    }

    fun enableCodeAsAdmin(codeId: String) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.enableAccessCode(adminUid, codeId)
                .onSuccess {
                    _adminFeedback.update { it.copy(message = "Access code $codeId re-enabled as Unused.", error = null) }
                }
                .onFailure { err ->
                    _adminFeedback.update { it.copy(error = err.message ?: "Re-enable failed.") }
                }
        }
    }

    fun setUserActiveAsAdmin(targetUid: String, active: Boolean) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.setUserActiveStatus(adminUid, targetUid, active)
                .onSuccess {
                    val verb = if (active) "enabled" else "disabled"
                    _adminFeedback.update { it.copy(message = "Account $targetUid $verb.", error = null) }
                }
                .onFailure { err ->
                    _adminFeedback.update { it.copy(error = err.message ?: "Account update failed.") }
                }
        }
    }

    fun removeCourseAsAdmin(courseId: String) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.removeCourseAsAdmin(adminUid, courseId)
                .onSuccess {
                    _adminFeedback.update { it.copy(message = "Video removed from platform.", error = null) }
                }
                .onFailure { err ->
                    _adminFeedback.update { it.copy(error = err.message ?: "Video removal failed.") }
                }
        }
    }

    fun updateCourseCategoryAsAdmin(courseId: String, newCategory: String) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.updateCourseCategoryAsAdmin(adminUid, courseId, newCategory)
                .onSuccess {
                    _adminFeedback.update { it.copy(message = "Updated video category to $newCategory.", error = null) }
                }
                .onFailure { err ->
                    _adminFeedback.update { it.copy(error = err.message ?: "Category update failed.") }
                }
        }
    }

    fun deleteCommentAsAdmin(commentId: String) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.deleteVideoComment(adminUid, commentId)
                .onSuccess {
                    _adminFeedback.update { it.copy(message = "Comment removed by Admin.", error = null) }
                }
                .onFailure { err ->
                    _adminFeedback.update { it.copy(error = err.message ?: "Comment deletion failed.") }
                }
        }
    }

    fun recordPaymentAsAdmin(
        customerName: String,
        role: UserRole,
        amount: String,
        transactionRef: String,
        notes: String
    ) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.recordManualPayment(adminUid, customerName, role, amount, transactionRef, notes)
                .onSuccess {
                    _adminFeedback.update {
                        it.copy(
                            message = "Payment recorded as Pending Verification. Verify manually before sharing an access code.",
                            error = null
                        )
                    }
                }
                .onFailure { err ->
                    _adminFeedback.update { it.copy(error = err.message ?: "Could not save payment record.") }
                }
        }
    }

    fun verifyPaymentAsAdmin(
        paymentId: String,
        status: PaymentStatus,
        assignedCodeId: String?,
        customerName: String
    ) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.verifyOrRejectPayment(adminUid, paymentId, status, assignedCodeId, customerName)
                .onSuccess {
                    _adminFeedback.update {
                        it.copy(message = "Payment marked ${status.label}.", error = null)
                    }
                }
                .onFailure { err ->
                    _adminFeedback.update { it.copy(error = err.message ?: "Verification update failed.") }
                }
        }
    }

    fun exportCodesCsvAsAdmin(role: UserRole) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.exportAccessCodesCsv(adminUid, role)
            }
            result.onSuccess { csv ->
                _adminFeedback.update {
                    it.copy(
                        exportedCsv = csv,
                        exportedCsvSubject = "MB Seekho ${role.label} Codes Status Export",
                        error = null
                    )
                }
            }.onFailure { err ->
                _adminFeedback.update { it.copy(error = err.message ?: "CSV export failed.") }
            }
        }
    }

    class Factory(
        private val repository: MbSeekhoRepository,
        private val sessionPreferences: SessionPreferences,
        private val mediaStorageManager: MediaStorageManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MbSeekhoViewModel(repository, sessionPreferences, mediaStorageManager) as T
        }
    }
}

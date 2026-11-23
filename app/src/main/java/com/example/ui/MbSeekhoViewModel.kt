package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccessCodeEntity
import com.example.data.local.CourseEntity
import com.example.data.local.CourseProgressEntity
import com.example.data.local.MediaStorageManager
import com.example.data.local.OwnerSecurityEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.SavedCourseEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.UserEntity
import com.example.data.model.PaymentStatus
import com.example.data.model.UserRole
import com.example.data.repository.CodeRedemptionResult
import com.example.data.repository.GeneratedCodeReveal
import com.example.data.repository.MbSeekhoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppScreen {
    data object Splash : AppScreen
    data object RoleSelect : AppScreen
    data class RoleLogin(val role: UserRole) : AppScreen
    data object StudentDashboard : AppScreen
    data object TeacherDashboard : AppScreen
    data class VideoPlayer(val courseId: String, val returnTo: AppScreen) : AppScreen
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
    val successMessage: String? = null
)

data class AdminFeedbackState(
    val message: String? = null,
    val error: String? = null,
    val newlyGeneratedCodes: List<GeneratedCodeReveal> = emptyList(),
    val exportedCsv: String? = null
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

    private val _adminFeedback = MutableStateFlow(AdminFeedbackState())
    val adminFeedback: StateFlow<AdminFeedbackState> = _adminFeedback.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _activeUid
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(null) else repository.observeUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val publishedCourses: StateFlow<List<CourseEntity>> = repository.observePublishedCourses()
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

    val ownerSecurity: StateFlow<OwnerSecurityEntity?> = repository.observeOwnerSecurity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Strictly observed only when an authenticated Admin is active
    val adminAccessCodes: StateFlow<List<AccessCodeEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.value && user.active) repository.observeAllAccessCodes()
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

    init {
        viewModelScope.launch {
            repository.initializeInventory()
            delay(1450L)
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
        _currentScreen.value = screen
    }

    fun openVideoPlayer(courseId: String) {
        val current = _currentScreen.value
        val returnScreen = if (current is AppScreen.VideoPlayer) current.returnTo else current
        _currentScreen.value = AppScreen.VideoPlayer(courseId = courseId, returnTo = returnScreen)
        viewModelScope.launch {
            repository.recordCourseView(courseId)
        }
    }

    fun closeVideoPlayer() {
        val current = _currentScreen.value
        if (current is AppScreen.VideoPlayer) {
            _currentScreen.value = current.returnTo
        }
    }

    fun clearLoginError() {
        _loginUiState.update { it.copy(errorMessage = null) }
    }

    fun redeemAccessCode(rawCode: String, role: UserRole, displayName: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState(isLoading = true, errorMessage = null)
            when (val result = repository.redeemAccessCode(rawCode, role, displayName)) {
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
                CodeRedemptionResult.InvalidCode -> {
                    _loginUiState.value = LoginUiState(
                        errorMessage = "Invalid access code. Please verify your code or contact the MB Seekho owner."
                    )
                }
                CodeRedemptionResult.AlreadyUsed -> {
                    _loginUiState.value = LoginUiState(
                        errorMessage = "Already Used Code: This access code has already been redeemed and cannot be used again."
                    )
                }
                CodeRedemptionResult.Revoked -> {
                    _loginUiState.value = LoginUiState(
                        errorMessage = "This access code has been revoked by the administrator."
                    )
                }
                CodeRedemptionResult.WrongPortal -> {
                    val expectedLabel = if (role == UserRole.STUDENT) "Student" else "Teacher"
                    _loginUiState.value = LoginUiState(
                        errorMessage = "Invalid code for $expectedLabel Login. Please use the correct portal for your access code."
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
                        errorMessage = "Member ID not found for this role. New users must activate with a private Access Code."
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

    // --- Student Actions ---

    fun toggleSaveCourse(courseId: String) {
        val uid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.toggleSaveCourse(uid, courseId)
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

    // --- Teacher Course Upload & Edit ---

    fun clearUploadStatus() {
        _uploadUiState.value = UploadUiState()
    }

    fun publishCourseFromTeacher(
        title: String,
        description: String,
        category: String,
        thumbnailUri: Uri?,
        videoUri: Uri?,
        remoteVideoUrl: String
    ) {
        val teacherUid = _activeUid.value ?: return
        viewModelScope.launch {
            _uploadUiState.value = UploadUiState(
                isUploading = true,
                progress = 0.08f,
                statusText = "Preparing course media..."
            )
            var finalThumbUrl: String? = null
            if (thumbnailUri != null) {
                _uploadUiState.update { it.copy(progress = 0.2f, statusText = "Saving course thumbnail...") }
                val thumbResult = mediaStorageManager.copyMediaToAppStorage(thumbnailUri, "thumbnails", "jpg")
                if (thumbResult.isFailure) {
                    _uploadUiState.value = UploadUiState(errorMessage = "Failed to process selected thumbnail.")
                    return@launch
                }
                finalThumbUrl = thumbResult.getOrNull()
            }

            val finalVideoUrl: String = if (videoUri != null) {
                _uploadUiState.update { it.copy(progress = 0.35f, statusText = "Uploading video file...") }
                val vidResult = mediaStorageManager.copyMediaToAppStorage(videoUri, "videos", "mp4") { frac ->
                    _uploadUiState.update {
                        it.copy(
                            progress = 0.35f + (frac * 0.55f),
                            statusText = "Uploading video (${(frac * 100).toInt()}%)..."
                        )
                    }
                }
                if (vidResult.isFailure) {
                    _uploadUiState.value = UploadUiState(errorMessage = "Failed to upload selected video file.")
                    return@launch
                }
                vidResult.getOrThrow()
            } else {
                val trimmed = remoteVideoUrl.trim()
                if (!trimmed.startsWith("https://")) {
                    _uploadUiState.value = UploadUiState(
                        errorMessage = "Select a video from your gallery or enter a valid HTTPS video URL."
                    )
                    return@launch
                }
                trimmed
            }

            _uploadUiState.update { it.copy(progress = 0.94f, statusText = "Publishing course metadata...") }
            repository.publishCourse(
                teacherUid = teacherUid,
                title = title,
                description = description,
                category = category,
                thumbnailUrl = finalThumbUrl,
                videoUrl = finalVideoUrl,
                published = true
            ).onSuccess {
                _uploadUiState.value = UploadUiState(
                    isUploading = false,
                    progress = 1f,
                    successMessage = "Course \"${it.title}\" published! Students can now watch it."
                )
            }.onFailure { err ->
                _uploadUiState.value = UploadUiState(
                    isUploading = false,
                    errorMessage = err.message ?: "Failed to publish course."
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
                .onFailure { onDone(it.message ?: "Unable to update course.") }
        }
    }

    fun deleteTeacherCourse(courseId: String, onDone: (String?) -> Unit = {}) {
        val teacherUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.deleteTeacherCourse(teacherUid, courseId)
                .onSuccess { onDone(null) }
                .onFailure { onDone(it.message ?: "Unable to delete course.") }
        }
    }

    // --- Owner Admin Actions ---

    fun clearAdminFeedback() {
        _adminFeedback.update { it.copy(message = null, error = null) }
    }

    fun clearGeneratedCodesReveal() {
        _adminFeedback.update { it.copy(newlyGeneratedCodes = emptyList()) }
    }

    fun clearExportedCsv() {
        _adminFeedback.update { it.copy(exportedCsv = null) }
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

    fun generateCodesAsAdmin(role: UserRole, count: Int) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.generateAdditionalCodes(adminUid, role, count)
                .onSuccess { reveals ->
                    _adminFeedback.update {
                        it.copy(
                            message = "Generated ${reveals.size} new ${role.label} code(s). Copy them now — only SHA-256 hashes are stored.",
                            error = null,
                            newlyGeneratedCodes = reveals
                        )
                    }
                }
                .onFailure { err ->
                    _adminFeedback.update { it.copy(error = err.message ?: "Generation failed.") }
                }
        }
    }

    fun revokeCodeAsAdmin(codeId: String) {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.revokeUnusedCode(adminUid, codeId)
                .onSuccess {
                    _adminFeedback.update { it.copy(message = "Code $codeId revoked.", error = null) }
                }
                .onFailure { err ->
                    _adminFeedback.update { it.copy(error = err.message ?: "Revoke failed.") }
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
                    _adminFeedback.update { it.copy(message = "Course removed from platform.", error = null) }
                }
                .onFailure { err ->
                    _adminFeedback.update { it.copy(error = err.message ?: "Course removal failed.") }
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
                            message = "Payment recorded as Pending Verification. Verify manually before sharing a code.",
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

    fun exportCodesCsvAsAdmin() {
        val adminUid = _activeUid.value ?: return
        viewModelScope.launch {
            repository.exportAccessCodesCsv(adminUid)
                .onSuccess { csv ->
                    _adminFeedback.update { it.copy(exportedCsv = csv, error = null) }
                }
                .onFailure { err ->
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

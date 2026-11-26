package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.StayCurrentPortrait
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CourseEntity
import com.example.data.local.SelectedVideoInfo
import com.example.data.local.UserEntity
import com.example.data.model.CourseCategories
import com.example.data.model.VideoType
import com.example.data.repository.TeacherStudioStats
import com.example.ui.UploadUiState
import com.example.ui.components.EditProfileNameDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPreviewImage
import com.example.ui.components.formatDateShort
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatDurationMs
import com.example.ui.components.formatFileSize
import com.example.ui.theme.CobaltBright
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.DeepNavyBg
import com.example.ui.theme.DeepNavyBorder
import com.example.ui.theme.DeepNavyCard
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinBright
import com.example.ui.theme.GoldCoinDarkContainer
import com.example.ui.theme.GoldCoinSoft
import com.example.ui.theme.InkDarkSecondary
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.theme.VioletBright

private enum class TeacherTab(val label: String) {
    VIDEOS("Videos"),
    NEW_VIDEO("New Video"),
    UPLOAD("Upload (+)"),
    PROFILE("Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    teacher: UserEntity?,
    teacherCourses: List<CourseEntity>,
    studioStats: TeacherStudioStats,
    uploadUiState: UploadUiState,
    onOpenCourse: (String) -> Unit,
    onOpenTeacherStudio: () -> Unit,
    onPublishVideo: (title: String, description: String, category: String, videoType: VideoType, videoUri: Uri?) -> Unit,
    onInspectVideo: suspend (Uri) -> Result<SelectedVideoInfo>,
    onExtractVideoPreview: suspend (Uri) -> Bitmap?,
    onClearUploadStatus: () -> Unit,
    onEditCourse: (courseId: String, title: String, desc: String, category: String, published: Boolean) -> Unit,
    onDeleteCourse: (String) -> Unit,
    onUpdateProfileName: (String) -> Unit,
    onUpdateProfilePhoto: (Uri) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val selectedTab = TeacherTab.entries[selectedTabIndex]

    var editingCourse by remember { mutableStateOf<CourseEntity?>(null) }
    var deletingCourse by remember { mutableStateOf<CourseEntity?>(null) }

    editingCourse?.let { course ->
        EditTeacherCourseDialog(
            course = course,
            onDismiss = { editingCourse = null },
            onSave = { t, d, cat, pub ->
                onEditCourse(course.courseId, t, d, cat, pub)
                editingCourse = null
            }
        )
    }

    deletingCourse?.let { course ->
        AlertDialog(
            onDismissRequest = { deletingCourse = null },
            title = { Text("Delete Video?") },
            text = { Text("Remove \"${course.title}\" permanently? Students will no longer be able to watch it.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCourse(course.courseId)
                        deletingCourse = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCourse = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            TeacherTab.VIDEOS -> "My Course Videos"
                            TeacherTab.NEW_VIDEO -> "Recently Uploaded"
                            TeacherTab.UPLOAD -> stringResource(R.string.upload_video_screen_title)
                            TeacherTab.PROFILE -> "Teacher Profile"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                val navItems = listOf(
                    Triple(TeacherTab.VIDEOS, Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
                    Triple(TeacherTab.NEW_VIDEO, Icons.Filled.Schedule, Icons.Outlined.Schedule),
                    Triple(TeacherTab.UPLOAD, Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
                    Triple(TeacherTab.PROFILE, Icons.Filled.Person, Icons.Outlined.Person)
                )
                navItems.forEachIndexed { index, (tab, activeIcon, inactiveIcon) ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTabIndex == index) activeIcon else inactiveIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("teacher_nav_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                TeacherTab.VIDEOS -> TeacherVideosTab(
                    courses = teacherCourses,
                    onGoToUpload = { selectedTabIndex = TeacherTab.UPLOAD.ordinal },
                    onPlayCourse = onOpenCourse,
                    onEditCourse = { editingCourse = it },
                    onDeleteCourse = { deletingCourse = it }
                )
                TeacherTab.NEW_VIDEO -> TeacherRecentVideosTab(
                    courses = teacherCourses,
                    onPlayCourse = onOpenCourse,
                    onGoToUpload = { selectedTabIndex = TeacherTab.UPLOAD.ordinal }
                )
                TeacherTab.UPLOAD -> TeacherUploadTab(
                    uploadUiState = uploadUiState,
                    onPublishVideo = onPublishVideo,
                    onInspectVideo = onInspectVideo,
                    onExtractVideoPreview = onExtractVideoPreview,
                    onClearStatus = onClearUploadStatus
                )
                TeacherTab.PROFILE -> TeacherProfileTab(
                    teacher = teacher,
                    totalUploaded = teacherCourses.size,
                    publishedCount = teacherCourses.count { it.published },
                    studioStats = studioStats,
                    onOpenTeacherStudio = onOpenTeacherStudio,
                    onUpdateName = onUpdateProfileName,
                    onUpdatePhoto = onUpdateProfilePhoto,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
private fun TeacherVideosTab(
    courses: List<CourseEntity>,
    onGoToUpload: () -> Unit,
    onPlayCourse: (String) -> Unit,
    onEditCourse: (CourseEntity) -> Unit,
    onDeleteCourse: (CourseEntity) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("teacher_videos_list")
    ) {
        item {
            Surface(
                onClick = onGoToUpload,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_quick_upload_card")
            ) {
                Row(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(RoyalPurple, VioletBright)))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Upload Video",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Upload New Video",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select a video, choose a category, and earn 1 coin per 10 valid views.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        if (courses.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Movie,
                    title = "No Videos Uploaded Yet",
                    subtitle = "Tap Upload (+) to select a video from your gallery and publish it to students.",
                    actionLabel = "Upload Video (+)",
                    onAction = onGoToUpload
                )
            }
        } else {
            items(courses, key = { it.courseId }) { course ->
                TeacherCourseManageCard(
                    course = course,
                    onPlay = { onPlayCourse(course.courseId) },
                    onEdit = { onEditCourse(course) },
                    onDelete = { onDeleteCourse(course) }
                )
            }
        }
    }
}

@Composable
private fun TeacherCourseManageCard(
    course: CourseEntity,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryLabel = remember(course.category) { CourseCategories.normalize(course.category) }
    val isShort = remember(course.videoType) { VideoType.fromValue(course.videoType) == VideoType.SHORT }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("teacher_course_${course.courseId}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                VideoPreviewImage(
                    videoUrl = course.videoUrl,
                    previewImageUrl = course.thumbnailUrl,
                    contentDescription = course.title,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(
                            label = if (isShort) "SHORT" else "LONG",
                            containerColor = if (isShort) GoldCoinDarkContainer else DeepNavyBg.copy(alpha = 0.85f),
                            contentColor = if (isShort) GoldCoinBright else CyanHighlight
                        )
                        StatusPill(
                            label = categoryLabel,
                            containerColor = CobaltPrimary.copy(alpha = 0.92f),
                            contentColor = Color.White
                        )
                    }
                    StatusPill(
                        label = if (course.published) "PUBLISHED" else "DRAFT",
                        containerColor = if (course.published) StatusSuccessContainer else StatusWarningContainer,
                        contentColor = if (course.published) StatusSuccess else StatusWarning
                    )
                }
                if (course.durationMs > 0L) {
                    Surface(
                        color = DeepNavyBg.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = formatDurationMs(course.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (course.description.isNotBlank()) {
                    Text(
                        text = course.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Uploaded ${formatDateShort(course.createdAt)} • ${course.totalViews} valid view(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPlay,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Watch & Moderate")
                    }
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("edit_course_${course.courseId}")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit video", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_course_${course.courseId}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete video",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherRecentVideosTab(
    courses: List<CourseEntity>,
    onPlayCourse: (String) -> Unit,
    onGoToUpload: () -> Unit
) {
    val latestFirst = remember(courses) { courses.sortedByDescending { it.createdAt } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (latestFirst.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.CloudUpload,
                    title = "No Recent Uploads",
                    subtitle = "Your latest uploaded videos with upload timestamps will appear here.",
                    actionLabel = "Upload Video (+)",
                    onAction = onGoToUpload
                )
            }
        } else {
            items(latestFirst, key = { it.courseId }) { course ->
                val categoryLabel = remember(course.category) { CourseCategories.normalize(course.category) }
                Card(
                    onClick = { onPlayCourse(course.courseId) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VideoPreviewImage(
                            videoUrl = course.videoUrl,
                            previewImageUrl = course.thumbnailUrl,
                            contentDescription = course.title,
                            modifier = Modifier
                                .size(width = 112.dp, height = 74.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isShort = VideoType.fromValue(course.videoType) == VideoType.SHORT
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPill(
                                    label = if (isShort) "SHORT" else "LONG",
                                    containerColor = if (isShort) GoldCoinDarkContainer else DeepNavyCard,
                                    contentColor = if (isShort) GoldCoinBright else CyanHighlight
                                )
                                StatusPill(
                                    label = categoryLabel,
                                    containerColor = RoyalPurple.copy(alpha = 0.14f),
                                    contentColor = RoyalPurple
                                )
                            }
                            Text(
                                text = course.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatDateTime(course.createdAt)} • ${course.totalViews} views",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean Teacher Video Upload Form containing:
 * 1. Upload Mode Selector: Upload Short Video (9:16) vs Upload Long Video (16:9)
 * 2. Select Video (opens Android Gallery / Video Picker; shows filename & real video preview)
 * 3. Video Title
 * 4. Video Description
 * 5. Course Category (the 7 MB Seekho categories)
 * 6. UPLOAD SHORT VIDEO / UPLOAD LONG VIDEO button
 * No separate thumbnail field anywhere.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeacherUploadTab(
    uploadUiState: UploadUiState,
    onPublishVideo: (title: String, description: String, category: String, videoType: VideoType, videoUri: Uri?) -> Unit,
    onInspectVideo: suspend (Uri) -> Result<SelectedVideoInfo>,
    onExtractVideoPreview: suspend (Uri) -> Bitmap?,
    onClearStatus: () -> Unit
) {
    var selectedVideoType by remember { mutableStateOf(VideoType.SHORT) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CourseCategories.all.first()) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoInfo by remember { mutableStateOf<SelectedVideoInfo?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPreview by remember { mutableStateOf(false) }

    LaunchedEffect(selectedVideoUri) {
        val uri = selectedVideoUri
        if (uri == null) {
            selectedVideoInfo = null
            previewBitmap = null
            isLoadingPreview = false
        } else {
            isLoadingPreview = true
            selectedVideoInfo = onInspectVideo(uri).getOrNull()
            previewBitmap = onExtractVideoPreview(uri)
            isLoadingPreview = false
        }
    }

    // Reset fields ONLY after a verified successful upload
    LaunchedEffect(uploadUiState.lastUploadedCourseId) {
        if (uploadUiState.lastUploadedCourseId != null) {
            title = ""
            description = ""
            selectedVideoUri = null
            selectedVideoInfo = null
            previewBitmap = null
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedVideoUri = uri
            if (uploadUiState.errorMessage != null || uploadUiState.successMessage != null) {
                onClearStatus()
            }
        }
    }

    val isShortSelected = selectedVideoType == VideoType.SHORT

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("teacher_upload_form")
    ) {
        // 0. TWO UPLOAD OPTIONS: Upload Short Video & Upload Long Video
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DeepNavyBg),
                border = BorderStroke(1.dp, DeepNavyBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Choose Upload Type",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        StatusPill(
                            label = if (isShortSelected) "9:16 SHORTS" else "16:9 LESSON",
                            containerColor = if (isShortSelected) GoldCoinDarkContainer else CobaltPrimary.copy(alpha = 0.3f),
                            contentColor = if (isShortSelected) GoldCoinBright else CyanHighlight
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UploadTypeOptionCard(
                            title = "Upload Short Video",
                            subtitle = "Vertical 9:16 • Shorts Feed",
                            icon = Icons.Rounded.StayCurrentPortrait,
                            selected = isShortSelected,
                            accentColor = GoldCoinBright,
                            enabled = !uploadUiState.isUploading,
                            onClick = { selectedVideoType = VideoType.SHORT },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_option_short_video")
                        )

                        UploadTypeOptionCard(
                            title = "Upload Long Video",
                            subtitle = "Horizontal 16:9 • Full Video",
                            icon = Icons.Rounded.OndemandVideo,
                            selected = !isShortSelected,
                            accentColor = CyanHighlight,
                            enabled = !uploadUiState.isUploading,
                            onClick = { selectedVideoType = VideoType.LONG },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_option_long_video")
                        )
                    }
                }
            }
        }

        // 1. SELECT VIDEO — Large clickable gallery trigger with live filename & video preview
        item {
            Surface(
                onClick = {
                    if (!uploadUiState.isUploading) {
                        videoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (selectedVideoUri != null) {
                        if (isShortSelected) GoldCoin else CobaltPrimary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                ),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("select_video_button")
            ) {
                if (selectedVideoUri == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    if (isShortSelected) {
                                        listOf(
                                            DeepNavyBg.copy(alpha = 0.06f),
                                            GoldCoinSoft.copy(alpha = 0.22f)
                                        )
                                    } else {
                                        listOf(
                                            CobaltPrimary.copy(alpha = 0.08f),
                                            RoyalPurple.copy(alpha = 0.12f)
                                        )
                                    }
                                )
                            )
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        if (isShortSelected) listOf(DeepNavyCard, CobaltPrimary)
                                        else listOf(CobaltPrimary, RoyalPurple)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isShortSelected) Icons.Rounded.Bolt else Icons.Rounded.VideoFile,
                                contentDescription = stringResource(R.string.select_video_label),
                                tint = if (isShortSelected) GoldCoinBright else Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = if (isShortSelected) "Select Short Video" else "Select Long Video",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isShortSelected) {
                                "Choose a vertical 9:16 video from your phone gallery"
                            } else {
                                stringResource(R.string.select_video_subtitle)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = if (isShortSelected) {
                                Modifier
                                    .fillMaxWidth()
                                    .background(DeepNavyBg)
                                    .padding(vertical = 12.dp)
                            } else {
                                Modifier.fillMaxWidth()
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = if (isShortSelected) {
                                    Modifier
                                        .width(190.dp)
                                        .aspectRatio(9f / 16f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(listOf(DeepNavyBg, CobaltDark, CobaltPrimary)))
                                } else {
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .background(Brush.linearGradient(listOf(CobaltDark, CobaltPrimary, RoyalPurple)))
                                },
                                contentAlignment = Alignment.Center
                            ) {
                            val bmp = previewBitmap
                            when {
                                bmp != null -> {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Selected video preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                isLoadingPreview -> {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Rounded.Movie,
                                        contentDescription = "Selected video preview",
                                        tint = Color.White.copy(alpha = 0.45f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.22f))
                            )

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Selected video",
                                        tint = CobaltPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            selectedVideoInfo?.durationMs?.takeIf { it > 0L }?.let { duration ->
                                Surface(
                                    color = Color.Black.copy(alpha = 0.72f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = formatDurationMs(duration),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Video selected",
                                tint = StatusSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedVideoInfo?.displayName
                                        ?: selectedVideoUri?.lastPathSegment
                                        ?: "Selected Video",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("selected_video_filename")
                                )
                                val sizeLabel = selectedVideoInfo?.sizeBytes?.let { formatFileSize(it) }.orEmpty()
                                Text(
                                    text = if (sizeLabel.isNotBlank()) {
                                        "$sizeLabel • ${stringResource(R.string.change_video_hint)}"
                                    } else {
                                        stringResource(R.string.change_video_hint)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. VIDEO TITLE, 3. VIDEO DESCRIPTION, 4. COURSE CATEGORY, 5. UPLOAD VIDEO BUTTON
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 2. Video Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (uploadUiState.errorMessage != null || uploadUiState.successMessage != null) {
                                onClearStatus()
                            }
                        },
                        label = { Text(stringResource(R.string.video_title_label)) },
                        placeholder = { Text(stringResource(R.string.video_title_placeholder)) },
                        singleLine = true,
                        enabled = !uploadUiState.isUploading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_video_title_input")
                    )

                    // 3. Video Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            if (uploadUiState.errorMessage != null || uploadUiState.successMessage != null) {
                                onClearStatus()
                            }
                        },
                        label = { Text(stringResource(R.string.video_description_label)) },
                        placeholder = { Text(stringResource(R.string.video_description_placeholder)) },
                        minLines = 3,
                        maxLines = 6,
                        enabled = !uploadUiState.isUploading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_video_description_input")
                    )

                    // 4. Course Category (Exact 7 MB Seekho Categories)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Category,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Course Category",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_category_selector")
                        ) {
                            CourseCategories.all.forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = {
                                        if (!uploadUiState.isUploading) {
                                            selectedCategory = category
                                        }
                                    },
                                    label = { Text(category) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CobaltPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag(
                                        "upload_category_${category.lowercase().replace(" ", "_")}"
                                    )
                                )
                            }
                        }
                    }

                    // Upload Progress Bar
                    if (uploadUiState.isUploading) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = uploadUiState.statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = CobaltPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            LinearProgressIndicator(
                                progress = { uploadUiState.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .testTag("upload_video_progress")
                            )
                        }
                    }

                    // Inline Validation / Upload Error
                    if (!uploadUiState.errorMessage.isNullOrBlank()) {
                        ErrorBanner(
                            message = uploadUiState.errorMessage,
                            modifier = Modifier.testTag("upload_video_error")
                        )
                    }

                    // Inline Success Message
                    if (!uploadUiState.successMessage.isNullOrBlank()) {
                        Surface(
                            color = StatusSuccessContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_video_success")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = StatusSuccess)
                                Text(
                                    text = uploadUiState.successMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusSuccess
                                )
                            }
                        }
                    }

                    // 5. UPLOAD SHORT VIDEO / UPLOAD LONG VIDEO Button
                    Button(
                        onClick = {
                            onPublishVideo(
                                title,
                                description,
                                selectedCategory,
                                selectedVideoType,
                                selectedVideoUri
                            )
                        },
                        enabled = !uploadUiState.isUploading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isShortSelected) CobaltBright else CobaltPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("upload_video_button")
                    ) {
                        Icon(
                            imageVector = if (isShortSelected) Icons.Rounded.Bolt else Icons.Rounded.CloudUpload,
                            contentDescription = null,
                            tint = if (isShortSelected) GoldCoinBright else Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                uploadUiState.isUploading -> stringResource(R.string.uploading_video_button)
                                isShortSelected -> "UPLOAD SHORT VIDEO"
                                else -> "UPLOAD LONG VIDEO"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadTypeOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accentColor.copy(alpha = 0.16f) else DeepNavySurface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accentColor else DeepNavyBorder
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) accentColor.copy(alpha = 0.24f) else DeepNavyCard,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (selected) accentColor else InkDarkSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) accentColor else InkDarkSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TeacherProfileTab(
    teacher: UserEntity?,
    totalUploaded: Int,
    publishedCount: Int,
    studioStats: TeacherStudioStats,
    onOpenTeacherStudio: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdatePhoto: (Uri) -> Unit,
    onLogout: () -> Unit
) {
    var showEditName by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onUpdatePhoto(uri) }

    if (showEditName && teacher != null) {
        EditProfileNameDialog(
            currentName = teacher.name,
            onDismiss = { showEditName = false },
            onSave = {
                onUpdateName(it)
                showEditName = false
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("teacher_profile_view")
    ) {
        // Teacher Studio Prominent Creator Entry Card (Deep Navy + Gold + Cyan)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DeepNavyBg),
                border = BorderStroke(1.5.dp, GoldCoin.copy(alpha = 0.65f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenTeacherStudio)
                    .testTag("open_teacher_studio_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(DeepNavyBg, DeepNavyCard, GoldCoinDarkContainer.copy(alpha = 0.85f))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(GoldCoin.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoGraph,
                                        contentDescription = "Teacher Studio",
                                        tint = GoldCoinBright,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Teacher Studio",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Last 28 days analytics & coin rewards",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CyanHighlight
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Open Teacher Studio",
                                tint = GoldCoinBright,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MonetizationOn,
                                        contentDescription = null,
                                        tint = GoldCoinBright,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "${studioStats.totalCoinBalance} Coins",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = GoldCoinSoft,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "Balance",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "${studioStats.viewsLast28Days} Views",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = CyanHighlight,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Last 28 days",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UserAvatar(
                        photoUrl = teacher?.photoUrl,
                        name = teacher?.name ?: "Teacher",
                        size = 96.dp
                    )
                    Text(
                        text = teacher?.name ?: "MB Seekho Teacher",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Teacher ID: ${teacher?.uid ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusPill(
                        label = "VERIFIED INSTRUCTOR",
                        containerColor = RoyalPurple.copy(alpha = 0.14f),
                        contentColor = RoyalPurple
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { showEditName = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit Profile")
                        }
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Change Photo")
                        }
                    }
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileStatCard(
                    title = "Uploaded Videos",
                    value = totalUploaded.toString(),
                    icon = Icons.Rounded.School,
                    accent = RoyalPurple,
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    title = "Published Videos",
                    value = publishedCount.toString(),
                    icon = Icons.Rounded.Movie,
                    accent = CobaltPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Instructor Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    ProfileDetailRow("Account Created", teacher?.createdAt?.let { formatDateShort(it) } ?: "—")
                    ProfileDetailRow("Status", if (teacher?.active == true) "Active" else "Disabled")
                    ProfileDetailRow("Reward Rate", "1 Coin per 10 Valid Views")
                    ProfileDetailRow("Permissions", "Upload & Manage Own Videos")
                }
            }
        }

        item {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("teacher_logout_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.logout_button), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EditTeacherCourseDialog(
    course: CourseEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(course.title) }
    var description by remember { mutableStateOf(course.description) }
    var category by remember { mutableStateOf(CourseCategories.normalize(course.category)) }
    var published by remember { mutableStateOf(course.published) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Video") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Video Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Video Description") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Course Category", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CourseCategories.all) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Published to Students")
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, description, category, published) },
                enabled = title.trim().length >= 3
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

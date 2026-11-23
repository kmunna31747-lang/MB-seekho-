package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CourseEntity
import com.example.data.local.UserEntity
import com.example.data.model.CourseCategories
import com.example.ui.UploadUiState
import com.example.ui.components.CourseThumbnailImage
import com.example.ui.components.EditProfileNameDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatDateShort
import com.example.ui.components.formatDateTime
import com.example.ui.theme.CobaltPrimary
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
    uploadUiState: UploadUiState,
    onOpenCourse: (String) -> Unit,
    onPublishCourse: (
        title: String,
        description: String,
        category: String,
        thumbnailUri: Uri?,
        videoUri: Uri?,
        remoteVideoUrl: String
    ) -> Unit,
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
            onSave = { t, d, c, pub ->
                onEditCourse(course.courseId, t, d, c, pub)
                editingCourse = null
            }
        )
    }

    deletingCourse?.let { course ->
        AlertDialog(
            onDismissRequest = { deletingCourse = null },
            title = { Text("Delete Course?") },
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
                            TeacherTab.UPLOAD -> "Upload New Course"
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
                    onPublish = onPublishCourse,
                    onClearStatus = onClearUploadStatus
                )
                TeacherTab.PROFILE -> TeacherProfileTab(
                    teacher = teacher,
                    totalUploaded = teacherCourses.size,
                    publishedCount = teacherCourses.count { it.published },
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
        // Quick Upload (+) banner
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
                            contentDescription = "Upload New Video",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Upload New Course Video",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pick a video from your phone and publish directly to students.",
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
                    subtitle = "Tap the Upload (+) tab to publish your first lesson for MB Seekho students.",
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
                CourseThumbnailImage(
                    thumbnailUrl = course.thumbnailUrl,
                    category = course.category,
                    contentDescription = course.title,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusPill(
                        label = course.category,
                        containerColor = CobaltPrimary.copy(alpha = 0.92f),
                        contentColor = Color.White
                    )
                    StatusPill(
                        label = if (course.published) "PUBLISHED" else "DRAFT",
                        containerColor = if (course.published) StatusSuccessContainer else StatusWarningContainer,
                        contentColor = if (course.published) StatusSuccess else StatusWarning
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Uploaded ${formatDateShort(course.createdAt)} • ${course.totalViews} view(s)",
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
                        Text("Preview")
                    }
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("edit_course_${course.courseId}")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit course", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_course_${course.courseId}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete course",
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
                    subtitle = "Your latest uploaded videos with exact upload dates and timestamps will appear here.",
                    actionLabel = "Upload Video (+)",
                    onAction = onGoToUpload
                )
            }
        } else {
            items(latestFirst, key = { it.courseId }) { course ->
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
                        CourseThumbnailImage(
                            thumbnailUrl = course.thumbnailUrl,
                            category = course.category,
                            contentDescription = course.title,
                            modifier = Modifier
                                .size(width = 112.dp, height = 74.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StatusPill(
                                label = "NEW UPLOAD",
                                containerColor = RoyalPurple.copy(alpha = 0.14f),
                                contentColor = RoyalPurple
                            )
                            Text(
                                text = course.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatDateTime(course.createdAt),
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

@Composable
private fun TeacherUploadTab(
    uploadUiState: UploadUiState,
    onPublish: (String, String, String, Uri?, Uri?, String) -> Unit,
    onClearStatus: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CourseCategories.all.first()) }
    var selectedThumbUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var remoteVideoUrl by remember { mutableStateOf("") }

    val thumbPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selectedThumbUri = uri
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedVideoUri = uri
            remoteVideoUrl = ""
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("teacher_upload_form")
    ) {
        // Large attractive Plus Upload trigger card
        item {
            Surface(
                onClick = {
                    videoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_video_gallery_button")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 26.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(CobaltPrimary, RoyalPurple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Select Video File",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Text(
                        text = if (selectedVideoUri != null) "Video Selected from Gallery ✓" else "Tap (+) to Select Video from Phone",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedVideoUri?.lastPathSegment ?: "Uses Android Photo Picker — zero storage permission needed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (uploadUiState.errorMessage != null || uploadUiState.successMessage != null) onClearStatus()
                        },
                        label = { Text("1. Course Title") },
                        placeholder = { Text("e.g. Complete Kotlin & Jetpack Compose") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_course_title_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("2. Course Description") },
                        placeholder = { Text("Describe what students will learn in this lesson...") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_course_desc_input")
                    )

                    Text(
                        text = "3. Course Category",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CourseCategories.all) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }

                    Text(
                        text = "4. Course Thumbnail",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = {
                            thumbPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pick_thumbnail_button")
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (selectedThumbUri != null) "Thumbnail Selected ✓ (Tap to Change)"
                            else "Choose Thumbnail Image from Gallery"
                        )
                    }

                    Text(
                        text = "5. Or Paste Direct HTTPS Video URL (Optional)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = remoteVideoUrl,
                        onValueChange = {
                            remoteVideoUrl = it
                            if (it.isNotBlank()) selectedVideoUri = null
                        },
                        label = { Text("HTTPS Video Stream URL") },
                        placeholder = { Text("https://example.com/lesson.mp4") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_video_url_input")
                    )

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
                            )
                        }
                    }

                    if (!uploadUiState.errorMessage.isNullOrBlank()) {
                        ErrorBanner(message = uploadUiState.errorMessage)
                    }

                    if (!uploadUiState.successMessage.isNullOrBlank()) {
                        Surface(
                            color = StatusSuccessContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
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

                    Button(
                        onClick = {
                            onPublish(
                                title,
                                description,
                                selectedCategory,
                                selectedThumbUri,
                                selectedVideoUri,
                                remoteVideoUrl
                            )
                            if (title.trim().length >= 3 && description.trim().length >= 5 && (selectedVideoUri != null || remoteVideoUrl.startsWith("https://"))) {
                                title = ""
                                description = ""
                                selectedThumbUri = null
                                selectedVideoUri = null
                                remoteVideoUrl = ""
                            }
                        },
                        enabled = !uploadUiState.isUploading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalPurple,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("publish_course_button")
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (uploadUiState.isUploading) "Publishing..." else "Publish Course",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherProfileTab(
    teacher: UserEntity?,
    totalUploaded: Int,
    publishedCount: Int,
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
                    title = "Uploaded Courses",
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
                    ProfileDetailRow("Permissions", "Upload & Manage Own Courses")
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
    var category by remember { mutableStateOf(course.category) }
    var published by remember { mutableStateOf(course.published) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Course") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
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

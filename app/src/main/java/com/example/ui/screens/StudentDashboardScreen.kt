package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CourseEntity
import com.example.data.local.CourseProgressEntity
import com.example.data.local.SavedCourseEntity
import com.example.data.local.UserEntity
import com.example.data.model.CourseCategories
import com.example.ui.components.CourseCard
import com.example.ui.components.EditProfileNameDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatDateShort
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer

private enum class StudentTab(val label: String) {
    HOME("Home"),
    NEW("New"),
    LIBRARY("Library"),
    PROFILE("Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    student: UserEntity?,
    publishedCourses: List<CourseEntity>,
    savedCourses: List<SavedCourseEntity>,
    progressList: List<CourseProgressEntity>,
    onOpenCourse: (String) -> Unit,
    onToggleSaveCourse: (String) -> Unit,
    onUpdateProfileName: (String) -> Unit,
    onUpdateProfilePhoto: (Uri) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val selectedTab = StudentTab.entries[selectedTabIndex]

    val savedIds = remember(savedCourses) { savedCourses.map { it.courseId }.toSet() }
    val progressByCourse = remember(progressList) { progressList.associateBy { it.courseId } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (selectedTab) {
                                StudentTab.HOME -> stringResource(R.string.app_name)
                                StudentTab.NEW -> "New Courses"
                                StudentTab.LIBRARY -> "My Library"
                                StudentTab.PROFILE -> "Student Profile"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                    Triple(StudentTab.HOME, Icons.Filled.Home, Icons.Outlined.Home),
                    Triple(StudentTab.NEW, Icons.Filled.NewReleases, Icons.Outlined.NewReleases),
                    Triple(StudentTab.LIBRARY, Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
                    Triple(StudentTab.PROFILE, Icons.Filled.Person, Icons.Outlined.Person)
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
                        modifier = Modifier.testTag("student_nav_${tab.name.lowercase()}")
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
                StudentTab.HOME -> StudentHomeTab(
                    studentName = student?.name ?: "Student",
                    courses = publishedCourses,
                    savedIds = savedIds,
                    progressByCourse = progressByCourse,
                    onOpenCourse = onOpenCourse,
                    onToggleSaveCourse = onToggleSaveCourse
                )
                StudentTab.NEW -> StudentNewCoursesTab(
                    courses = publishedCourses,
                    savedIds = savedIds,
                    onOpenCourse = onOpenCourse,
                    onToggleSaveCourse = onToggleSaveCourse
                )
                StudentTab.LIBRARY -> StudentLibraryTab(
                    courses = publishedCourses,
                    savedIds = savedIds,
                    progressByCourse = progressByCourse,
                    onOpenCourse = onOpenCourse,
                    onToggleSaveCourse = onToggleSaveCourse
                )
                StudentTab.PROFILE -> StudentProfileTab(
                    student = student,
                    watchedCount = progressList.size,
                    completedCount = progressList.count { it.completed },
                    onUpdateName = onUpdateProfileName,
                    onUpdatePhoto = onUpdateProfilePhoto,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
private fun StudentHomeTab(
    studentName: String,
    courses: List<CourseEntity>,
    savedIds: Set<String>,
    progressByCourse: Map<String, CourseProgressEntity>,
    onOpenCourse: (String) -> Unit,
    onToggleSaveCourse: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val filtered = remember(courses, searchQuery, selectedCategory) {
        courses.filter { course ->
            val matchesCat = selectedCategory == "All" || course.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                course.title.contains(searchQuery, ignoreCase = true) ||
                course.teacherName.contains(searchQuery, ignoreCase = true) ||
                course.category.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_home_list")
    ) {
        // Hero welcome card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(CobaltDark, CobaltPrimary, RoyalPurple)))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_learning),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.30f,
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusPill(
                        label = "MB SEEKHO STUDENT",
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                    Column {
                        Text(
                            text = "Welcome back, $studentName",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${courses.size} published course(s) ready to watch",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Search input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search courses, topics, or teachers...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_search_input")
            )
        }

        // Category chips
        item {
            val categories = remember { listOf("All") + CourseCategories.all }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.VideoLibrary,
                    title = "No Published Courses Yet",
                    subtitle = if (courses.isEmpty()) {
                        "Courses uploaded and published by verified MB Seekho teachers will appear right here."
                    } else {
                        "No courses matched your current search or category filter."
                    }
                )
            }
        } else {
            items(filtered, key = { it.courseId }) { course ->
                CourseCard(
                    course = course,
                    isSaved = course.courseId in savedIds,
                    progressPercent = progressByCourse[course.courseId]?.progressPercent,
                    primaryButtonLabel = "Play Course",
                    onOpenCourse = { onOpenCourse(course.courseId) },
                    onToggleSave = { onToggleSaveCourse(course.courseId) }
                )
            }
        }
    }
}

@Composable
private fun StudentNewCoursesTab(
    courses: List<CourseEntity>,
    savedIds: Set<String>,
    onOpenCourse: (String) -> Unit,
    onToggleSaveCourse: (String) -> Unit
) {
    val latestFirst = remember(courses) { courses.sortedByDescending { it.createdAt } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_new_list")
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = RoyalPurple,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Fresh Releases",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Latest courses uploaded by MB Seekho teachers, newest first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        if (latestFirst.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "No New Courses Yet",
                    subtitle = "Check back soon as teachers publish brand-new video lessons."
                )
            }
        } else {
            items(latestFirst, key = { it.courseId }) { course ->
                CourseCard(
                    course = course,
                    isSaved = course.courseId in savedIds,
                    showNewBadge = true,
                    showExactTimestamp = true,
                    primaryButtonLabel = "Open Course",
                    onOpenCourse = { onOpenCourse(course.courseId) },
                    onToggleSave = { onToggleSaveCourse(course.courseId) }
                )
            }
        }
    }
}

@Composable
private fun StudentLibraryTab(
    courses: List<CourseEntity>,
    savedIds: Set<String>,
    progressByCourse: Map<String, CourseProgressEntity>,
    onOpenCourse: (String) -> Unit,
    onToggleSaveCourse: (String) -> Unit
) {
    var filter by remember { mutableStateOf("Saved") }
    val filters = listOf("Saved", "Watched", "Completed", "All Available")

    val filteredCourses = remember(courses, savedIds, progressByCourse, filter) {
        when (filter) {
            "Saved" -> courses.filter { it.courseId in savedIds }
            "Watched" -> courses.filter { progressByCourse.containsKey(it.courseId) }
            "Completed" -> courses.filter { progressByCourse[it.courseId]?.completed == true }
            else -> courses
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_library_list")
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(item) },
                        modifier = Modifier.testTag("library_filter_${item.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }

        if (filteredCourses.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.PlayCircle,
                    title = "Nothing in $filter Yet",
                    subtitle = when (filter) {
                        "Saved" -> "Tap the bookmark icon on any course to save it for quick access."
                        "Watched" -> "Start watching a course video to track your watch progress here."
                        "Completed" -> "Finish watching a course to earn your completion badge."
                        else -> "No published courses are available yet."
                    }
                )
            }
        } else {
            items(filteredCourses, key = { it.courseId }) { course ->
                val progress = progressByCourse[course.courseId]
                CourseCard(
                    course = course,
                    isSaved = course.courseId in savedIds,
                    progressPercent = progress?.progressPercent,
                    primaryButtonLabel = if (progress != null && !progress.completed) "Resume Video" else "Watch Video",
                    onOpenCourse = { onOpenCourse(course.courseId) },
                    onToggleSave = { onToggleSaveCourse(course.courseId) }
                )
            }
        }
    }
}

@Composable
private fun StudentProfileTab(
    student: UserEntity?,
    watchedCount: Int,
    completedCount: Int,
    onUpdateName: (String) -> Unit,
    onUpdatePhoto: (Uri) -> Unit,
    onLogout: () -> Unit
) {
    var showEditNameDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onUpdatePhoto(uri)
    }

    if (showEditNameDialog && student != null) {
        EditProfileNameDialog(
            currentName = student.name,
            onDismiss = { showEditNameDialog = false },
            onSave = {
                onUpdateName(it)
                showEditNameDialog = false
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_profile_view")
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
                        photoUrl = student?.photoUrl,
                        name = student?.name ?: "Student",
                        size = 96.dp
                    )
                    Text(
                        text = student?.name ?: "MB Seekho Student",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("student_profile_name")
                    )
                    Text(
                        text = "Student ID: ${student?.uid ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("student_profile_id")
                    )
                    StatusPill(
                        label = if (student?.active == true) "ACTIVE ACCESS" else "DISABLED",
                        containerColor = if (student?.active == true) StatusSuccessContainer else MaterialTheme.colorScheme.errorContainer,
                        contentColor = if (student?.active == true) StatusSuccess else MaterialTheme.colorScheme.error
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { showEditNameDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("edit_student_profile_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit Profile")
                        }
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("change_student_photo_button")
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Change Photo")
                        }
                    }
                }
            }
        }

        // Learning stats
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileStatCard(
                    title = "Courses Watched",
                    value = watchedCount.toString(),
                    icon = Icons.Rounded.PlayCircle,
                    accent = CobaltPrimary,
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    title = "Completed",
                    value = completedCount.toString(),
                    icon = Icons.Rounded.CheckCircle,
                    accent = StatusSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Account details card (never exposes raw access code!)
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
                        text = "Account Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    ProfileDetailRow("Account Created", student?.createdAt?.let { formatDateShort(it) } ?: "—")
                    ProfileDetailRow("Access Status", if (student?.active == true) "Verified Student" else "Suspended")
                    ProfileDetailRow("Platform", "MB Seekho Learning")
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
                    .testTag("student_logout_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.logout_button), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun ProfileStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

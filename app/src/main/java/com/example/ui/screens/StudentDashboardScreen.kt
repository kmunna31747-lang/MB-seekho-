package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CourseEntity
import com.example.data.local.CourseProgressEntity
import com.example.data.local.SavedCourseEntity
import com.example.data.local.UserEntity
import com.example.data.local.VideoCommentEntity
import com.example.data.local.VideoLikeEntity
import com.example.data.model.CourseCategories
import com.example.data.model.VideoType
import com.example.ui.components.EditProfileNameDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPreviewImage
import com.example.ui.components.formatDateShort
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.CobaltBright
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.DeepNavyBg
import com.example.ui.theme.DeepNavyBorder
import com.example.ui.theme.DeepNavyCard
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.GoldCoinBright
import com.example.ui.theme.GoldCoinDarkContainer
import com.example.ui.theme.InkDarkSecondary
import com.example.ui.theme.LikeRose
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer

private enum class StudentTab(val label: String) {
    HOME("Home"),
    NEW("New"),
    LIBRARY("Library"),
    PROFILE("Profile")
}

private enum class HomeSectionFilter(val label: String) {
    ALL("All Videos"),
    SHORTS("Short Videos"),
    LONG("Long Videos")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    student: UserEntity?,
    publishedCourses: List<CourseEntity>,
    allVideoLikes: List<VideoLikeEntity>,
    allVideoComments: List<VideoCommentEntity>,
    savedCourses: List<SavedCourseEntity>,
    progressList: List<CourseProgressEntity>,
    onOpenCourse: (String) -> Unit,
    onOpenShortVideo: (courseId: String, openComments: Boolean) -> Unit,
    onToggleLike: (String) -> Unit,
    onToggleSaveCourse: (String) -> Unit,
    onUpdateProfileName: (String) -> Unit,
    onUpdateProfilePhoto: (Uri) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val selectedTab = StudentTab.entries[selectedTabIndex]

    val savedIds = remember(savedCourses) { savedCourses.map { it.courseId }.toSet() }
    val progressByCourse = remember(progressList) { progressList.associateBy { it.courseId } }
    val likeCountsByVideo = remember(allVideoLikes) {
        allVideoLikes.groupingBy { it.videoId }.eachCount()
    }
    val likedVideoIdsByMe = remember(allVideoLikes, student?.uid) {
        if (student == null) emptySet()
        else allVideoLikes.filter { it.studentId == student.uid }.map { it.videoId }.toSet()
    }
    val commentCountsByVideo = remember(allVideoComments) {
        allVideoComments.groupingBy { it.videoId }.eachCount()
    }

    Scaffold(
        containerColor = DeepNavyBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = when (selectedTab) {
                                StudentTab.HOME -> stringResource(R.string.app_name)
                                StudentTab.NEW -> "New Videos"
                                StudentTab.LIBRARY -> "My Library"
                                StudentTab.PROFILE -> "Student Profile"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        if (selectedTab == StudentTab.HOME) {
                            StatusPill(
                                label = "LEARN",
                                containerColor = GoldCoinDarkContainer,
                                contentColor = GoldCoinBright
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepNavySurface,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DeepNavySurface,
                contentColor = Color.White,
                windowInsets = WindowInsets.navigationBars
            ) {
                val navItems = listOf(
                    Triple(StudentTab.HOME, Icons.Filled.Home, Icons.Outlined.Home),
                    Triple(StudentTab.NEW, Icons.Filled.NewReleases, Icons.Outlined.NewReleases),
                    Triple(StudentTab.LIBRARY, Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
                    Triple(StudentTab.PROFILE, Icons.Filled.Person, Icons.Outlined.Person)
                )
                navItems.forEachIndexed { index, (tab, activeIcon, inactiveIcon) ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) activeIcon else inactiveIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldCoinBright,
                            selectedTextColor = GoldCoinBright,
                            indicatorColor = CobaltPrimary.copy(alpha = 0.35f),
                            unselectedIconColor = InkDarkSecondary,
                            unselectedTextColor = InkDarkSecondary
                        ),
                        modifier = Modifier.testTag("student_nav_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepNavyBg)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
            ) {
                when (selectedTab) {
                    StudentTab.HOME -> StudentHomeTab(
                        studentName = student?.name ?: "Student",
                        courses = publishedCourses,
                        savedIds = savedIds,
                        likedIds = likedVideoIdsByMe,
                        likeCountsByVideo = likeCountsByVideo,
                        commentCountsByVideo = commentCountsByVideo,
                        progressByCourse = progressByCourse,
                        onOpenLongVideo = onOpenCourse,
                        onOpenShortVideo = onOpenShortVideo,
                        onToggleLike = onToggleLike,
                        onToggleSaveCourse = onToggleSaveCourse
                    )
                    StudentTab.NEW -> StudentNewCoursesTab(
                        courses = publishedCourses,
                        savedIds = savedIds,
                        likedIds = likedVideoIdsByMe,
                        likeCountsByVideo = likeCountsByVideo,
                        commentCountsByVideo = commentCountsByVideo,
                        onOpenCourse = onOpenCourse,
                        onToggleLike = onToggleLike,
                        onToggleSaveCourse = onToggleSaveCourse
                    )
                    StudentTab.LIBRARY -> StudentLibraryTab(
                        courses = publishedCourses,
                        savedIds = savedIds,
                        likedIds = likedVideoIdsByMe,
                        likeCountsByVideo = likeCountsByVideo,
                        commentCountsByVideo = commentCountsByVideo,
                        progressByCourse = progressByCourse,
                        onOpenCourse = onOpenCourse,
                        onToggleLike = onToggleLike,
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
}

@Composable
private fun StudentHomeTab(
    studentName: String,
    courses: List<CourseEntity>,
    savedIds: Set<String>,
    likedIds: Set<String>,
    likeCountsByVideo: Map<String, Int>,
    commentCountsByVideo: Map<String, Int>,
    progressByCourse: Map<String, CourseProgressEntity>,
    onOpenLongVideo: (String) -> Unit,
    onOpenShortVideo: (courseId: String, openComments: Boolean) -> Unit,
    onToggleLike: (String) -> Unit,
    onToggleSaveCourse: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedSection by remember { mutableStateOf(HomeSectionFilter.ALL) }

    val filtered = remember(courses, searchQuery, selectedCategory) {
        courses.filter { course ->
            val normalizedCat = CourseCategories.normalize(course.category)
            val matchesCat = selectedCategory == "All" ||
                normalizedCat.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                course.title.contains(searchQuery, ignoreCase = true) ||
                course.description.contains(searchQuery, ignoreCase = true) ||
                course.teacherName.contains(searchQuery, ignoreCase = true) ||
                normalizedCat.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    val shortVideos = remember(filtered) {
        filtered.filter { VideoType.fromValue(it.videoType) == VideoType.SHORT }
    }
    val longVideos = remember(filtered) {
        filtered.filter { VideoType.fromValue(it.videoType) == VideoType.LONG }
    }
    val shortVideoRows = remember(shortVideos) { shortVideos.chunked(2) }

    val totalShortsCount = remember(courses) {
        courses.count { VideoType.fromValue(it.videoType) == VideoType.SHORT }
    }
    val totalLongCount = remember(courses) {
        courses.count { VideoType.fromValue(it.videoType) == VideoType.LONG }
    }

    val showShortsSection = selectedSection == HomeSectionFilter.ALL || selectedSection == HomeSectionFilter.SHORTS
    val showLongSection = selectedSection == HomeSectionFilter.ALL || selectedSection == HomeSectionFilter.LONG

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_home_list")
    ) {
        // Hero Welcome Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(DeepNavyCard, CobaltDark, CobaltPrimary)
                        )
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_learning),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.22f,
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            label = "MB SEEKHO STUDENT",
                            containerColor = GoldCoinDarkContainer,
                            contentColor = GoldCoinBright
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(
                                label = "$totalShortsCount Shorts",
                                containerColor = DeepNavyBg.copy(alpha = 0.65f),
                                contentColor = GoldCoinBright
                            )
                            StatusPill(
                                label = "$totalLongCount Videos",
                                containerColor = DeepNavyBg.copy(alpha = 0.65f),
                                contentColor = CyanHighlight
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Welcome back, $studentName",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Watch quick Shorts or full lessons from verified MB Seekho teachers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkDarkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search short videos, long lessons, or teachers...",
                        color = InkDarkSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = CyanHighlight
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DeepNavySurface,
                    unfocusedContainerColor = DeepNavySurface,
                    focusedBorderColor = CyanHighlight,
                    unfocusedBorderColor = DeepNavyBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = CyanHighlight
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_search_input")
            )
        }

        // Section Filter Row (All Videos | Short Videos | Long Videos)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_section_filter_row")
            ) {
                HomeSectionFilter.entries.forEach { filter ->
                    val selected = selectedSection == filter
                    val accent = when (filter) {
                        HomeSectionFilter.SHORTS -> GoldCoinBright
                        HomeSectionFilter.LONG -> CyanHighlight
                        HomeSectionFilter.ALL -> CobaltBright
                    }
                    Surface(
                        onClick = { selectedSection = filter },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) accent.copy(alpha = 0.18f) else DeepNavySurface,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) accent else DeepNavyBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("student_section_tab_${filter.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (filter) {
                                HomeSectionFilter.SHORTS -> Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = if (selected) GoldCoinBright else InkDarkSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                HomeSectionFilter.LONG -> Icon(
                                    imageVector = Icons.Rounded.PlayCircle,
                                    contentDescription = null,
                                    tint = if (selected) CyanHighlight else InkDarkSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                HomeSectionFilter.ALL -> Unit
                            }
                            if (filter != HomeSectionFilter.ALL) {
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (selected) Color.White else InkDarkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 7 Course Category Filter Chips
        item {
            val categories = remember { listOf("All") + CourseCategories.all }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("student_category_filter_row")
            ) {
                items(categories) { category ->
                    val selected = selectedCategory == category
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DeepNavySurface,
                            labelColor = InkDarkSecondary,
                            selectedContainerColor = CobaltBright,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = DeepNavyBorder,
                            selectedBorderColor = CyanHighlight
                        ),
                        modifier = Modifier.testTag(
                            "student_cat_${category.lowercase().replace(" ", "_")}"
                        )
                    )
                }
            }
        }

        // ============================================================
        // SECTION 1: SHORT VIDEOS (Vertical 9:16 cards, smooth vertical scrolling)
        // ============================================================
        if (showShortsSection) {
            item {
                SectionHeaderRow(
                    icon = Icons.Rounded.Bolt,
                    iconTint = GoldCoinBright,
                    title = "Short Videos",
                    subtitle = "Vertical 9:16 quick learning clips",
                    count = shortVideos.size,
                    countContainer = GoldCoinDarkContainer,
                    countContent = GoldCoinBright,
                    modifier = Modifier.testTag("student_shorts_section")
                )
            }

            if (shortVideos.isEmpty()) {
                item {
                    NavyEmptyStateCard(
                        icon = Icons.Rounded.Bolt,
                        iconTint = GoldCoinBright,
                        title = if (totalShortsCount == 0) "No Short Videos Yet" else "No Matching Short Videos",
                        subtitle = if (totalShortsCount == 0) {
                            "Short vertical videos uploaded by verified MB Seekho teachers will appear here."
                        } else {
                            "No Short Videos matched your selected category or search query."
                        }
                    )
                }
            } else {
                items(
                    items = shortVideoRows,
                    key = { row -> "short_row_${row.joinToString("_") { it.courseId }}" }
                ) { rowPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowPair.forEach { shortVideo ->
                            ShortVideoGridCard(
                                course = shortVideo,
                                isLiked = shortVideo.courseId in likedIds,
                                likeCount = likeCountsByVideo[shortVideo.courseId] ?: 0,
                                commentCount = commentCountsByVideo[shortVideo.courseId] ?: 0,
                                onOpenShort = { onOpenShortVideo(shortVideo.courseId, false) },
                                onToggleLike = { onToggleLike(shortVideo.courseId) },
                                onOpenComments = { onOpenShortVideo(shortVideo.courseId, true) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowPair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // ============================================================
        // SECTION 2: LONG VIDEOS (Horizontal 16:9 video thumbnails + duration + stats)
        // ============================================================
        if (showLongSection) {
            item {
                SectionHeaderRow(
                    icon = Icons.Rounded.PlayCircle,
                    iconTint = CyanHighlight,
                    title = "Long Videos",
                    subtitle = "Full 16:9 lessons with chapters & comments",
                    count = longVideos.size,
                    countContainer = CobaltPrimary.copy(alpha = 0.25f),
                    countContent = CyanHighlight,
                    modifier = Modifier.testTag("student_long_videos_section")
                )
            }

            if (longVideos.isEmpty()) {
                item {
                    NavyEmptyStateCard(
                        icon = Icons.Rounded.VideoLibrary,
                        iconTint = CyanHighlight,
                        title = if (totalLongCount == 0) "No Long Videos Yet" else "No Matching Long Videos",
                        subtitle = if (totalLongCount == 0) {
                            "Full-length lessons uploaded by verified MB Seekho teachers will appear here."
                        } else {
                            "No Long Videos matched your selected category or search query."
                        }
                    )
                }
            } else {
                items(longVideos, key = { "long_${it.courseId}" }) { course ->
                    LongVideoFeedCard(
                        course = course,
                        isSaved = course.courseId in savedIds,
                        isLiked = course.courseId in likedIds,
                        likeCount = likeCountsByVideo[course.courseId] ?: 0,
                        commentCount = commentCountsByVideo[course.courseId] ?: 0,
                        progressPercent = progressByCourse[course.courseId]?.progressPercent,
                        onOpenVideo = { onOpenLongVideo(course.courseId) },
                        onToggleLike = { onToggleLike(course.courseId) },
                        onToggleSave = { onToggleSaveCourse(course.courseId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    count: Int,
    countContainer: Color,
    countContent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, iconTint.copy(alpha = 0.35f)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkDarkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        StatusPill(
            label = "$count",
            containerColor = countContainer,
            contentColor = countContent
        )
    }
}

@Composable
private fun ShortVideoGridCard(
    course: CourseEntity,
    isLiked: Boolean,
    likeCount: Int,
    commentCount: Int,
    onOpenShort: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heartTint by animateColorAsState(
        targetValue = if (isLiked) LikeRose else Color.White,
        label = "short_card_like_tint"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
        border = BorderStroke(1.dp, DeepNavyBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenShort)
            .testTag("short_video_card_${course.courseId}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Video preview generated from the actual uploaded video
            VideoPreviewImage(
                videoUrl = course.videoUrl,
                previewImageUrl = course.thumbnailUrl,
                contentDescription = course.title,
                modifier = Modifier.fillMaxSize()
            )

            // Top dark scrim for readable badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(DeepNavyBg.copy(alpha = 0.72f), Color.Transparent)
                        )
                    )
            )

            // Top overlay: SHORT badge & optional duration
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = GoldCoinDarkContainer.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, GoldCoinBright.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = GoldCoinBright,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "SHORT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldCoinBright
                        )
                    }
                }
                if (course.durationMs > 0L) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DeepNavyBg.copy(alpha = 0.78f)
                    ) {
                        Text(
                            text = formatDurationMs(course.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Center subtle play cue
            Surface(
                shape = CircleShape,
                color = DeepNavyBg.copy(alpha = 0.56f),
                border = BorderStroke(1.dp, CyanHighlight.copy(alpha = 0.5f)),
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.Center)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play short video",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Bottom gradient + Title + Teacher Name + Like & Comment buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                DeepNavyBg.copy(alpha = 0.86f),
                                DeepNavyBg.copy(alpha = 0.96f)
                            )
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.School,
                        contentDescription = null,
                        tint = CyanHighlight,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = course.teacherName,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkDarkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Interactive Like & Comment buttons right on the 9:16 card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onToggleLike,
                        shape = RoundedCornerShape(10.dp),
                        color = if (isLiked) LikeRose.copy(alpha = 0.22f) else DeepNavySurface.copy(alpha = 0.85f),
                        border = BorderStroke(
                            1.dp,
                            if (isLiked) LikeRose.copy(alpha = 0.6f) else DeepNavyBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("short_card_like_${course.courseId}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isLiked) "Unlike short video" else "Like short video",
                                tint = heartTint,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "$likeCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        onClick = onOpenComments,
                        shape = RoundedCornerShape(10.dp),
                        color = DeepNavySurface.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, DeepNavyBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("short_card_comment_${course.courseId}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = "Open comments",
                                tint = CyanHighlight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "$commentCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LongVideoFeedCard(
    course: CourseEntity,
    isSaved: Boolean,
    isLiked: Boolean,
    likeCount: Int,
    commentCount: Int,
    progressPercent: Int? = null,
    showNewBadge: Boolean = false,
    showExactTimestamp: Boolean = false,
    onOpenVideo: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit
) {
    val categoryLabel = remember(course.category) { CourseCategories.normalize(course.category) }
    val isShort = remember(course.videoType) { VideoType.fromValue(course.videoType) == VideoType.SHORT }
    val heartTint by animateColorAsState(
        targetValue = if (isLiked) LikeRose else InkDarkSecondary,
        label = "long_card_like_tint"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
        border = BorderStroke(1.dp, DeepNavyBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenVideo)
            .testTag("long_video_card_${course.courseId}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Horizontal 16:9 thumbnail generated directly from the video
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(DeepNavySurface)
            ) {
                VideoPreviewImage(
                    videoUrl = course.videoUrl,
                    previewImageUrl = course.thumbnailUrl,
                    contentDescription = course.title,
                    modifier = Modifier.fillMaxSize()
                )

                // Top badges + Bookmark button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(
                            label = categoryLabel,
                            containerColor = CobaltPrimary.copy(alpha = 0.92f),
                            contentColor = Color.White
                        )
                        if (isShort) {
                            StatusPill(
                                label = "SHORT",
                                containerColor = GoldCoinDarkContainer,
                                contentColor = GoldCoinBright
                            )
                        } else if (showNewBadge) {
                            StatusPill(
                                label = "NEW",
                                containerColor = GoldCoinDarkContainer,
                                contentColor = GoldCoinBright
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = DeepNavyBg.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, DeepNavyBorder)
                    ) {
                        IconButton(
                            onClick = onToggleSave,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("save_course_${course.courseId}")
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isSaved) "Remove from library" else "Save to library",
                                tint = if (isSaved) GoldCoinBright else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Center Play Icon
                Surface(
                    shape = CircleShape,
                    color = DeepNavyBg.copy(alpha = 0.62f),
                    border = BorderStroke(1.5.dp, CyanHighlight.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .size(52.dp)
                        .align(Alignment.Center)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Watch video",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom-right Video Duration Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DeepNavyBg.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, DeepNavyBorder),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = CyanHighlight,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (course.durationMs > 0L) formatDurationMs(course.durationMs) else "Video",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Watch Progress Bar at bottom edge
                if (progressPercent != null && progressPercent > 0) {
                    LinearProgressIndicator(
                        progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                        color = CyanHighlight,
                        trackColor = DeepNavyBg.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            // Video Details & Metrics Body
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = null,
                            tint = CyanHighlight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = course.teacherName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = InkDarkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (showExactTimestamp) formatDateTime(course.createdAt) else formatDateShort(course.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = InkDarkSecondary
                    )
                }

                // Views, Likes, Comments + Watch CTA Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = "Views",
                                tint = CyanHighlight,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "${course.totalViews}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = InkDarkSecondary
                            )
                        }

                        Surface(
                            onClick = onToggleLike,
                            shape = RoundedCornerShape(10.dp),
                            color = if (isLiked) LikeRose.copy(alpha = 0.18f) else DeepNavySurface,
                            border = BorderStroke(
                                1.dp,
                                if (isLiked) LikeRose.copy(alpha = 0.5f) else DeepNavyBorder
                            ),
                            modifier = Modifier.testTag("long_card_like_${course.courseId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (isLiked) "Unlike video" else "Like video",
                                    tint = heartTint,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "$likeCount",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLiked) Color.White else InkDarkSecondary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = GoldCoinBright,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$commentCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = InkDarkSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CobaltBright
                    ) {
                        Text(
                            text = if (progressPercent != null && progressPercent in 1..99) "Resume" else "Watch",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavyEmptyStateCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavySurface),
        border = BorderStroke(1.dp, DeepNavyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 26.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.14f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = InkDarkSecondary
            )
        }
    }
}

@Composable
private fun StudentNewCoursesTab(
    courses: List<CourseEntity>,
    savedIds: Set<String>,
    likedIds: Set<String>,
    likeCountsByVideo: Map<String, Int>,
    commentCountsByVideo: Map<String, Int>,
    onOpenCourse: (String) -> Unit,
    onToggleLike: (String) -> Unit,
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
                color = DeepNavyCard,
                border = BorderStroke(1.dp, DeepNavyBorder),
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
                        tint = GoldCoinBright,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Fresh Releases",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Latest Short & Long Videos uploaded by MB Seekho teachers, newest first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkDarkSecondary
                        )
                    }
                }
            }
        }

        if (latestFirst.isEmpty()) {
            item {
                NavyEmptyStateCard(
                    icon = Icons.Rounded.AutoAwesome,
                    iconTint = GoldCoinBright,
                    title = "No New Videos Yet",
                    subtitle = "Check back soon as teachers publish brand-new Short and Long video lessons."
                )
            }
        } else {
            items(latestFirst, key = { it.courseId }) { course ->
                LongVideoFeedCard(
                    course = course,
                    isSaved = course.courseId in savedIds,
                    isLiked = course.courseId in likedIds,
                    likeCount = likeCountsByVideo[course.courseId] ?: 0,
                    commentCount = commentCountsByVideo[course.courseId] ?: 0,
                    showNewBadge = true,
                    showExactTimestamp = true,
                    onOpenVideo = { onOpenCourse(course.courseId) },
                    onToggleLike = { onToggleLike(course.courseId) },
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
    likedIds: Set<String>,
    likeCountsByVideo: Map<String, Int>,
    commentCountsByVideo: Map<String, Int>,
    progressByCourse: Map<String, CourseProgressEntity>,
    onOpenCourse: (String) -> Unit,
    onToggleLike: (String) -> Unit,
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
                    val selected = filter == item
                    FilterChip(
                        selected = selected,
                        onClick = { filter = item },
                        label = {
                            Text(
                                text = item,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DeepNavySurface,
                            labelColor = InkDarkSecondary,
                            selectedContainerColor = CobaltBright,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = DeepNavyBorder,
                            selectedBorderColor = CyanHighlight
                        ),
                        modifier = Modifier.testTag("library_filter_${item.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }

        if (filteredCourses.isEmpty()) {
            item {
                NavyEmptyStateCard(
                    icon = Icons.Rounded.PlayCircle,
                    iconTint = CyanHighlight,
                    title = "Nothing in $filter Yet",
                    subtitle = when (filter) {
                        "Saved" -> "Tap the bookmark icon on any video to save it for quick access."
                        "Watched" -> "Start watching a video to track your watch progress here."
                        "Completed" -> "Finish watching a video to earn your completion badge."
                        else -> "No published videos are available yet."
                    }
                )
            }
        } else {
            items(filteredCourses, key = { it.courseId }) { course ->
                val progress = progressByCourse[course.courseId]
                LongVideoFeedCard(
                    course = course,
                    isSaved = course.courseId in savedIds,
                    isLiked = course.courseId in likedIds,
                    likeCount = likeCountsByVideo[course.courseId] ?: 0,
                    commentCount = commentCountsByVideo[course.courseId] ?: 0,
                    progressPercent = progress?.progressPercent,
                    onOpenVideo = { onOpenCourse(course.courseId) },
                    onToggleLike = { onToggleLike(course.courseId) },
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
                colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
                border = BorderStroke(1.dp, DeepNavyBorder),
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
                        color = Color.White,
                        modifier = Modifier.testTag("student_profile_name")
                    )
                    Text(
                        text = "Student ID: ${student?.uid ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkDarkSecondary,
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
                            border = BorderStroke(1.dp, CyanHighlight.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("edit_student_profile_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, tint = CyanHighlight, modifier = Modifier.size(18.dp))
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
                            border = BorderStroke(1.dp, GoldCoinBright.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("change_student_photo_button")
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = GoldCoinBright, modifier = Modifier.size(18.dp))
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
                StudentNavyStatCard(
                    title = "Videos Watched",
                    value = watchedCount.toString(),
                    icon = Icons.Rounded.PlayCircle,
                    accent = CyanHighlight,
                    modifier = Modifier.weight(1f)
                )
                StudentNavyStatCard(
                    title = "Completed",
                    value = completedCount.toString(),
                    icon = Icons.Rounded.CheckCircle,
                    accent = GoldCoinBright,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Account details card (never exposes raw access code!)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
                border = BorderStroke(1.dp, DeepNavyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Account Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    StudentNavyDetailRow("Account Created", student?.createdAt?.let { formatDateShort(it) } ?: "—")
                    StudentNavyDetailRow("Access Status", if (student?.active == true) "Verified Student" else "Suspended")
                    StudentNavyDetailRow("Platform", "MB Seekho Learning")
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
private fun StudentNavyStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
        border = BorderStroke(1.dp, DeepNavyBorder)
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
                color = Color.White
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = InkDarkSecondary
            )
        }
    }
}

@Composable
private fun StudentNavyDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = InkDarkSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
internal fun ProfileStatCard(
    title: String,
    value: String,
    icon: ImageVector,
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

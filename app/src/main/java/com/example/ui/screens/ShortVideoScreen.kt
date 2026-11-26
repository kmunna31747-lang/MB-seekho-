package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.CourseEntity
import com.example.data.local.UserEntity
import com.example.data.local.VideoCommentEntity
import com.example.data.local.VideoLikeEntity
import com.example.data.model.CourseCategories
import com.example.data.model.UserRole
import com.example.ui.CommentActionState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.UserAvatar
import com.example.ui.components.VideoPreviewImage
import com.example.ui.components.formatDateTime
import com.example.ui.theme.CobaltBright
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.DeepNavyBg
import com.example.ui.theme.DeepNavyBorder
import com.example.ui.theme.DeepNavyCard
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.GoldCoinBright
import com.example.ui.theme.InkDarkSecondary
import com.example.ui.theme.LikeRose

/**
 * Full-screen vertical Short Video watching interface with smooth vertical swiping,
 * live likes, comments modal sheet, native share, and real validated view tracking.
 * Does NOT create any new profile pages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortVideoScreen(
    shortVideos: List<CourseEntity>,
    initialCourseId: String,
    openCommentsInitially: Boolean,
    currentUser: UserEntity?,
    allVideoLikes: List<VideoLikeEntity>,
    allVideoComments: List<VideoCommentEntity>,
    commentActionState: CommentActionState,
    onToggleLike: (videoId: String) -> Unit,
    onPostComment: (videoId: String, text: String, onSuccess: () -> Unit) -> Unit,
    onDeleteComment: (commentId: String) -> Unit,
    onClearCommentFeedback: () -> Unit,
    onPageViewed: (videoId: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current

    if (shortVideos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepNavyBg)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = GoldCoinBright,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No Short Videos Available Yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBright)
                ) {
                    Text("Go Back", color = Color.White)
                }
            }
        }
        return
    }

    val startIndex = remember(shortVideos, initialCourseId) {
        shortVideos.indexOfFirst { it.courseId == initialCourseId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = startIndex) { shortVideos.size }

    var activeCommentsCourseId by remember {
        mutableStateOf(if (openCommentsInitially) initialCourseId else null)
    }

    LaunchedEffect(pagerState.currentPage, shortVideos) {
        shortVideos.getOrNull(pagerState.currentPage)?.let { currentShort ->
            onPageViewed(currentShort.courseId)
        }
    }

    val likesByVideo = remember(allVideoLikes) { allVideoLikes.groupBy { it.videoId } }
    val commentsByVideo = remember(allVideoComments) { allVideoComments.groupBy { it.videoId } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavyBg)
            .testTag("short_video_screen")
    ) {
        VerticalPager(
            state = pagerState,
            key = { index -> shortVideos[index].courseId },
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val shortCourse = shortVideos[pageIndex]
            val isActivePage = pagerState.currentPage == pageIndex
            val videoLikes = likesByVideo[shortCourse.courseId].orEmpty()
            val videoComments = commentsByVideo[shortCourse.courseId].orEmpty()
            val isLikedByMe = currentUser != null && videoLikes.any { it.studentId == currentUser.uid }

            ShortVideoPage(
                course = shortCourse,
                isActive = isActivePage,
                likeCount = videoLikes.size,
                isLiked = isLikedByMe,
                commentCount = videoComments.size,
                onToggleLike = { onToggleLike(shortCourse.courseId) },
                onOpenComments = {
                    onClearCommentFeedback()
                    activeCommentsCourseId = shortCourse.courseId
                },
                onShare = {
                    val shareText = "Watch \"${shortCourse.title}\" by ${shortCourse.teacherName} on MB Seekho — Learn Anything • Grow Every Day!"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, shortCourse.title)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Short Video"))
                },
                onBack = onBack
            )
        }

        // In-screen Comments Sheet (no extra profile page)
        val commentsTargetId = activeCommentsCourseId
        val commentsCourse = remember(commentsTargetId, shortVideos) {
            shortVideos.firstOrNull { it.courseId == commentsTargetId }
        }
        if (commentsCourse != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val commentsForSheet = commentsByVideo[commentsCourse.courseId].orEmpty()
            ModalBottomSheet(
                onDismissRequest = {
                    activeCommentsCourseId = null
                    onClearCommentFeedback()
                },
                sheetState = sheetState,
                containerColor = DeepNavySurface,
                contentColor = Color.White,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(42.dp)
                            .height(4.dp)
                            .background(CyanHighlight.copy(alpha = 0.5f), RoundedCornerShape(50))
                    )
                }
            ) {
                ShortVideoCommentsSheetContent(
                    course = commentsCourse,
                    currentUser = currentUser,
                    comments = commentsForSheet,
                    commentActionState = commentActionState,
                    onPostComment = { text, onSuccess ->
                        onPostComment(commentsCourse.courseId, text, onSuccess)
                    },
                    onDeleteComment = onDeleteComment,
                    onClearFeedback = onClearCommentFeedback
                )
            }
        }
    }
}

@Composable
private fun ShortVideoPage(
    course: CourseEntity,
    isActive: Boolean,
    likeCount: Int,
    isLiked: Boolean,
    commentCount: Int,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit
) {
    var isBuffering by remember(course.courseId) { mutableStateOf(true) }
    var isPlaying by remember(course.courseId) { mutableStateOf(false) }
    var userPaused by remember(course.courseId) { mutableStateOf(false) }
    var playbackError by remember(course.courseId) { mutableStateOf<String?>(null) }
    var reloadKey by remember(course.courseId) { mutableIntStateOf(0) }
    var descriptionExpanded by remember(course.courseId) { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.14f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "short_like_scale"
    )

    LaunchedEffect(isActive, userPaused, videoViewRef) {
        val vv = videoViewRef ?: return@LaunchedEffect
        runCatching {
            if (isActive && !userPaused && playbackError == null) {
                if (!vv.isPlaying) vv.start()
                isPlaying = true
            } else {
                if (vv.isPlaying) vv.pause()
                isPlaying = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                val vv = videoViewRef ?: return@clickable
                if (playbackError != null) return@clickable
                runCatching {
                    if (vv.isPlaying) {
                        vv.pause()
                        userPaused = true
                        isPlaying = false
                    } else {
                        vv.start()
                        userPaused = false
                        isPlaying = true
                    }
                }
            }
            .testTag("short_video_page_${course.courseId}")
    ) {
        // Fallback preview frame underneath while video prepares
        VideoPreviewImage(
            videoUrl = course.videoUrl,
            previewImageUrl = course.thumbnailUrl,
            contentDescription = course.title,
            modifier = Modifier.fillMaxSize()
        )

        // Vertical Video Player
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    videoViewRef = this
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        isBuffering = false
                        playbackError = null
                        if (isActive && !userPaused) {
                            start()
                            isPlaying = true
                        }
                    }
                    setOnErrorListener { _, what, extra ->
                        isBuffering = false
                        isPlaying = false
                        playbackError = "Unable to play Short Video (code $what/$extra)."
                        true
                    }
                }
            },
            update = { vv ->
                videoViewRef = vv
                val targetTag = "${course.videoUrl}#$reloadKey"
                if (vv.tag != targetTag) {
                    vv.tag = targetTag
                    isBuffering = true
                    playbackError = null
                    runCatching {
                        vv.setVideoURI(Uri.parse(course.videoUrl))
                    }.onFailure { err ->
                        isBuffering = false
                        playbackError = err.message ?: "Invalid video source."
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        )

        // Top & bottom cinematic navy gradients for crisp legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DeepNavyBg.copy(alpha = 0.84f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            DeepNavyBg.copy(alpha = 0.78f),
                            DeepNavyBg.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        // Buffering spinner
        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = CyanHighlight,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
            )
        }

        // Play / Pause center pulse indicator when paused by user
        AnimatedVisibility(
            visible = userPaused && playbackError == null && !isBuffering,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = DeepNavyBg.copy(alpha = 0.72f),
                border = BorderStroke(1.5.dp, CyanHighlight.copy(alpha = 0.75f)),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        // Playback error overlay
        if (playbackError != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DeepNavySurface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, DeepNavyBorder),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = "Playback error",
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = playbackError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Button(
                        onClick = {
                            playbackError = null
                            isBuffering = true
                            reloadKey++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBright)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry", color = Color.White)
                    }
                }
            }
        }

        // Top Bar: Back + Shorts Badge + Category
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = DeepNavySurface.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, CyanHighlight.copy(alpha = 0.35f))
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("short_player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = DeepNavySurface.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, GoldCoinBright.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = GoldCoinBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SHORTS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldCoinBright
                        )
                    }
                }
            }

            StatusPill(
                label = CourseCategories.normalize(course.category),
                containerColor = CobaltPrimary.copy(alpha = 0.85f),
                contentColor = Color.White
            )
        }

        // Right-side floating vertical action column (Like, Comment, Share, Views)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 14.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShortRailActionButton(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = likeCount.toString(),
                contentDescription = if (isLiked) "Unlike Short Video" else "Like Short Video",
                iconTint = if (isLiked) LikeRose else Color.White,
                borderColor = if (isLiked) LikeRose else CyanHighlight.copy(alpha = 0.45f),
                iconModifier = Modifier.scale(likeScale),
                testTag = "short_like_button",
                onClick = onToggleLike
            )

            ShortRailActionButton(
                icon = Icons.Rounded.ChatBubbleOutline,
                label = commentCount.toString(),
                contentDescription = "Open comments",
                iconTint = CyanHighlight,
                borderColor = CyanHighlight.copy(alpha = 0.45f),
                testTag = "short_comment_button",
                onClick = onOpenComments
            )

            ShortRailActionButton(
                icon = Icons.Rounded.Share,
                label = "Share",
                contentDescription = "Share Short Video",
                iconTint = GoldCoinBright,
                borderColor = GoldCoinBright.copy(alpha = 0.45f),
                testTag = "short_share_button",
                onClick = onShare
            )

            // Real Views Counter
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.testTag("short_views_counter")
            ) {
                Surface(
                    shape = CircleShape,
                    color = DeepNavySurface.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, DeepNavyBorder),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = "Views count",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text(
                    text = "${course.totalViews}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Bottom-left metadata overlay: Teacher Name, Video Title & Description
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .fillMaxWidth(0.76f)
                .padding(start = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = DeepNavyCard.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, CyanHighlight.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.School,
                        contentDescription = null,
                        tint = CyanHighlight,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = course.teacherName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("short_teacher_name")
                    )
                }
            }

            Text(
                text = course.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("short_video_title")
            )

            if (course.description.isNotBlank()) {
                Text(
                    text = course.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkDarkSecondary,
                    maxLines = if (descriptionExpanded) 6 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clickable { descriptionExpanded = !descriptionExpanded }
                        .testTag("short_video_description")
                )
            }
        }
    }
}

@Composable
private fun ShortRailActionButton(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    iconTint: Color,
    borderColor: Color,
    testTag: String,
    iconModifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = DeepNavySurface.copy(alpha = 0.82f),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .size(50.dp)
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = iconTint,
                    modifier = iconModifier.size(24.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun ShortVideoCommentsSheetContent(
    course: CourseEntity,
    currentUser: UserEntity?,
    comments: List<VideoCommentEntity>,
    commentActionState: CommentActionState,
    onPostComment: (text: String, onSuccess: () -> Unit) -> Unit,
    onDeleteComment: (commentId: String) -> Unit,
    onClearFeedback: () -> Unit
) {
    var draft by remember(course.courseId) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = CyanHighlight)
            Text(
                text = "Comments (${comments.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        HorizontalDivider(color = DeepNavyBorder)

        if (comments.isEmpty()) {
            Text(
                text = "No comments yet. Be the first to comment on this Short Video!",
                style = MaterialTheme.typography.bodyMedium,
                color = InkDarkSecondary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .testTag("short_comments_list")
            ) {
                items(comments, key = { it.commentId }) { comment ->
                    val canDelete = currentUser != null && (
                        currentUser.uid == comment.studentId ||
                            (currentUser.role == UserRole.TEACHER.value && course.teacherId == currentUser.uid) ||
                            currentUser.role == UserRole.ADMIN.value
                        )
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
                        border = BorderStroke(1.dp, DeepNavyBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            UserAvatar(
                                photoUrl = comment.studentPhoto,
                                name = comment.studentName,
                                size = 38.dp
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = comment.studentName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = formatDateTime(comment.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = InkDarkSecondary
                                        )
                                    }
                                    if (canDelete) {
                                        IconButton(
                                            onClick = { onDeleteComment(comment.commentId) },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Delete comment",
                                                tint = Color(0xFFFCA5A5),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = comment.commentText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!commentActionState.errorMessage.isNullOrBlank()) {
            ErrorBanner(message = commentActionState.errorMessage)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = {
                    if (it.length <= 500) {
                        draft = it
                        if (commentActionState.errorMessage != null) onClearFeedback()
                    }
                },
                placeholder = { Text("Add a comment...", color = InkDarkSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyanHighlight,
                    unfocusedBorderColor = DeepNavyBorder,
                    focusedContainerColor = DeepNavyCard,
                    unfocusedContainerColor = DeepNavyCard,
                    cursorColor = CyanHighlight
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("short_comment_input")
            )
            Surface(
                onClick = {
                    if (draft.trim().isNotEmpty()) {
                        onPostComment(draft) { draft = "" }
                    }
                },
                shape = CircleShape,
                color = if (draft.trim().isNotEmpty()) CobaltBright else DeepNavyBorder,
                modifier = Modifier
                    .size(50.dp)
                    .testTag("short_post_comment_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Post comment",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

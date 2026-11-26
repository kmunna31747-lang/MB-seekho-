package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.CourseEntity
import com.example.data.local.CourseProgressEntity
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
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.CobaltBright
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
import kotlinx.coroutines.delay

/**
 * Dedicated Long Video Watch Screen styled in MB Seekho Deep Navy + Bright Blue/Cyan + Gold.
 * Includes:
 * 1. Large 16:9 Video Player with fullscreen, seekbar & playback controls
 * 2. Video Title, Category, Teacher Name & Real Views Counter
 * 3. Action Bar: Like Button (live count), Share Button, Student Save & Complete
 * 4. Video Description Card
 * 5. Comments Section (Heading, Write Comment Box, Post Button, Newest-First Comments List + Moderation)
 * 6. Related Videos Section (Real published Long Videos from same category first)
 */
@Composable
fun VideoPlayerScreen(
    course: CourseEntity?,
    relatedVideos: List<CourseEntity>,
    likeCountsByVideo: Map<String, Int>,
    commentCountsByVideo: Map<String, Int>,
    currentUser: UserEntity?,
    isSaved: Boolean,
    progress: CourseProgressEntity?,
    likes: List<VideoLikeEntity>,
    comments: List<VideoCommentEntity>,
    commentActionState: CommentActionState,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onPostComment: (text: String, onSuccess: () -> Unit) -> Unit,
    onDeleteComment: (commentId: String) -> Unit,
    onClearCommentFeedback: () -> Unit,
    onUpdateProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onMarkCompleted: () -> Unit,
    onSelectRelatedVideo: (courseId: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isStudent = currentUser?.role == UserRole.STUDENT.value
    val isLikedByMe = remember(likes, currentUser?.uid) {
        currentUser != null && likes.any { it.studentId == currentUser.uid }
    }

    var isFullscreen by remember { mutableStateOf(false) }
    var isBuffering by remember(course?.courseId) { mutableStateOf(true) }
    var isPlaying by remember(course?.courseId) { mutableStateOf(false) }
    var playbackError by remember(course?.courseId) { mutableStateOf<String?>(null) }
    var currentPosMs by remember(course?.courseId) { mutableLongStateOf(progress?.watchedPositionMs ?: 0L) }
    var durationMs by remember(course?.courseId) {
        mutableLongStateOf((progress?.durationMs ?: 0L).coerceAtLeast(course?.durationMs ?: 0L))
    }
    var controlsVisible by remember { mutableStateOf(true) }
    var reloadKey by remember(course?.courseId) { mutableIntStateOf(0) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var commentDraft by remember(course?.courseId) { mutableStateOf("") }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onBack()
        }
    }

    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(videoViewRef, isPlaying, course?.courseId) {
        var tick = 0
        while (true) {
            delay(500L)
            val vv = videoViewRef
            if (vv != null && playbackError == null) {
                runCatching {
                    val dur = vv.duration.toLong()
                    val pos = vv.currentPosition.toLong()
                    if (dur > 0L) durationMs = dur
                    if (pos >= 0L) currentPosMs = pos
                    isPlaying = vv.isPlaying
                    if (isStudent && isPlaying && dur > 0L && tick % 6 == 0) {
                        onUpdateProgress(pos, dur)
                    }
                }
            }
            tick++
        }
    }

    if (course == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepNavyBg)
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Video not found.", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBright)
                ) { Text("Go Back", color = Color.White) }
            }
        }
        return
    }

    val categoryLabel = remember(course.category) { CourseCategories.normalize(course.category) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isFullscreen) Color.Black else DeepNavyBg)
            .then(if (isFullscreen) Modifier else Modifier.safeDrawingPadding())
            .testTag("long_video_screen")
    ) {
        // 1. LARGE 16:9 VIDEO PLAYER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isFullscreen) Modifier.fillMaxSize()
                    else Modifier.aspectRatio(16f / 9f)
                )
                .background(Color.Black)
                .clickable { controlsVisible = !controlsVisible }
                .testTag("video_player_surface")
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        videoViewRef = this
                        setOnPreparedListener { mp ->
                            isBuffering = false
                            playbackError = null
                            durationMs = mp.duration.toLong().coerceAtLeast(0L)
                            val resumeAt = progress?.watchedPositionMs ?: 0L
                            if (resumeAt in 1000L until (durationMs - 2000L)) {
                                seekTo(resumeAt.toInt())
                            }
                            start()
                            isPlaying = true
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            if (isStudent && durationMs > 0L) {
                                onUpdateProgress(durationMs, durationMs)
                                onMarkCompleted()
                            }
                        }
                        setOnErrorListener { _, what, extra ->
                            isBuffering = false
                            isPlaying = false
                            playbackError = "Unable to play this video (code $what/$extra). Verify the video file or connection."
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
                modifier = Modifier.fillMaxSize()
            )

            if (isBuffering && playbackError == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepNavyBg.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyanHighlight)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Loading video...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (playbackError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepNavyBg.copy(alpha = 0.88f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = "Playback error",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = playbackError!!,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
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
                            Text("Retry Playback", color = Color.White)
                        }
                    }
                }
            }

            if (controlsVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    DeepNavyBg.copy(alpha = 0.65f),
                                    Color.Black.copy(alpha = 0.25f),
                                    DeepNavyBg.copy(alpha = 0.80f)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                if (isFullscreen) isFullscreen = false else onBack()
                            },
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = course.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier.testTag("player_fullscreen_button")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                tint = CyanHighlight
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val vv = videoViewRef ?: return@IconButton
                                val target = (currentPosMs - 10_000L).coerceAtLeast(0L)
                                vv.seekTo(target.toInt())
                                currentPosMs = target
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Replay10,
                                contentDescription = "Rewind 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(Modifier.width(18.dp))

                        Surface(
                            shape = CircleShape,
                            color = CobaltBright.copy(alpha = 0.94f),
                            border = BorderStroke(1.5.dp, CyanHighlight.copy(alpha = 0.7f)),
                            modifier = Modifier.size(56.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val vv = videoViewRef ?: return@IconButton
                                    if (vv.isPlaying) {
                                        vv.pause()
                                        isPlaying = false
                                        if (isStudent && durationMs > 0L) {
                                            onUpdateProgress(currentPosMs, durationMs)
                                        }
                                    } else {
                                        vv.start()
                                        isPlaying = true
                                    }
                                },
                                modifier = Modifier.testTag("player_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(18.dp))

                        IconButton(
                            onClick = {
                                val vv = videoViewRef ?: return@IconButton
                                val target = (currentPosMs + 10_000L).coerceAtMost(durationMs.coerceAtLeast(0L))
                                vv.seekTo(target.toInt())
                                currentPosMs = target
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Forward10,
                                contentDescription = "Forward 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        val safeDuration = durationMs.coerceAtLeast(1L)
                        Slider(
                            value = currentPosMs.coerceIn(0L, safeDuration).toFloat(),
                            onValueChange = { newVal ->
                                currentPosMs = newVal.toLong()
                                videoViewRef?.seekTo(newVal.toInt())
                            },
                            valueRange = 0f..safeDuration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = GoldCoinBright,
                                activeTrackColor = CyanHighlight,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_seek_bar")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDurationMs(currentPosMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                            Text(
                                text = formatDurationMs(durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Scrollable Details, Actions, Comments & Related Videos (hidden in fullscreen)
        if (!isFullscreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepNavyBg)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Badges: Category, Duration & Completed
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = categoryLabel,
                        containerColor = CobaltPrimary,
                        contentColor = Color.White
                    )
                    if (durationMs > 0L) {
                        StatusPill(
                            label = formatDurationMs(durationMs),
                            containerColor = GoldCoinDarkContainer,
                            contentColor = GoldCoinBright
                        )
                    }
                    if (progress?.completed == true) {
                        StatusPill(
                            label = "COMPLETED",
                            containerColor = StatusSuccessContainer,
                            contentColor = StatusSuccess
                        )
                    }
                }

                // 2. VIDEO TITLE
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.testTag("player_video_title")
                )

                // Teacher Name & Real Views Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DeepNavyCard,
                            border = BorderStroke(1.dp, CyanHighlight.copy(alpha = 0.5f)),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.School,
                                    contentDescription = null,
                                    tint = CyanHighlight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = course.teacherName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.testTag("player_teacher_name")
                            )
                            Text(
                                text = "Published ${formatDateTime(course.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkDarkSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = DeepNavyCard,
                        border = BorderStroke(1.dp, DeepNavyBorder),
                        modifier = Modifier.testTag("player_views_counter")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = "Views count",
                                tint = CyanHighlight,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${course.totalViews} views",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // 3. ACTION BAR: Like Button, Share Button, Save & Complete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onToggleLike,
                        shape = RoundedCornerShape(14.dp),
                        color = if (isLikedByMe) LikeRose.copy(alpha = 0.18f) else DeepNavyCard,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isLikedByMe) LikeRose else DeepNavyBorder
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("player_like_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isLikedByMe) "Unlike video" else "Like video",
                                tint = if (isLikedByMe) LikeRose else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${likes.size}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isLikedByMe) LikeRose else Color.White,
                                modifier = Modifier.testTag("player_like_count")
                            )
                        }
                    }

                    // Share Button
                    Surface(
                        onClick = {
                            val shareText = "Watch \"${course.title}\" by ${course.teacherName} on MB Seekho — Learn Anything • Grow Every Day!"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, course.title)
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Video"))
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = DeepNavyCard,
                        border = BorderStroke(1.dp, GoldCoinBright.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("player_share_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share video",
                                tint = GoldCoinBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Share",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (isStudent) {
                        OutlinedButton(
                            onClick = onToggleSave,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isSaved) CyanHighlight else DeepNavyBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = DeepNavyCard,
                                contentColor = if (isSaved) CyanHighlight else Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("player_save_course_button")
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isSaved) "Saved" else "Save", maxLines = 1)
                        }

                        Button(
                            onClick = onMarkCompleted,
                            enabled = progress?.completed != true,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusSuccess,
                                contentColor = Color.White,
                                disabledContainerColor = StatusSuccess.copy(alpha = 0.35f),
                                disabledContentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("player_mark_complete_button")
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (progress?.completed == true) "Done" else "Done", maxLines = 1)
                        }
                    }
                }

                // 4. VIDEO DESCRIPTION
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepNavySurface),
                    border = BorderStroke(1.dp, DeepNavyBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_video_description")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanHighlight
                        )
                        Text(
                            text = course.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkDarkSecondary
                        )
                    }
                }

                HorizontalDivider(color = DeepNavyBorder)

                // 5. COMMENTS SECTION
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("player_comments_heading")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        tint = CyanHighlight,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Comments (${comments.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = {
                        if (it.length <= 500) {
                            commentDraft = it
                            if (commentActionState.errorMessage != null || commentActionState.successMessage != null) {
                                onClearCommentFeedback()
                            }
                        }
                    },
                    label = { Text("Write a comment", color = InkDarkSecondary) },
                    placeholder = { Text("Share your question or feedback about this video...", color = InkDarkSecondary.copy(alpha = 0.7f)) },
                    minLines = 2,
                    maxLines = 4,
                    supportingText = {
                        Text("${commentDraft.length}/500", color = InkDarkSecondary)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanHighlight,
                        unfocusedBorderColor = DeepNavyBorder,
                        focusedContainerColor = DeepNavySurface,
                        unfocusedContainerColor = DeepNavySurface,
                        cursorColor = CyanHighlight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_comment_input")
                )

                if (!commentActionState.errorMessage.isNullOrBlank()) {
                    ErrorBanner(
                        message = commentActionState.errorMessage,
                        modifier = Modifier.testTag("player_comment_error")
                    )
                }

                Button(
                    onClick = {
                        onPostComment(commentDraft) {
                            commentDraft = ""
                        }
                    },
                    enabled = commentDraft.trim().isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CobaltBright,
                        contentColor = Color.White,
                        disabledContainerColor = DeepNavyCard,
                        disabledContentColor = InkDarkSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("player_post_comment_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Post Comment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                if (comments.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DeepNavySurface,
                        border = BorderStroke(1.dp, DeepNavyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No comments yet. Be the first student to share your thoughts!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkDarkSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_comments_list")
                    ) {
                        comments.forEach { comment ->
                            val canDelete = currentUser != null && (
                                currentUser.uid == comment.studentId ||
                                    (currentUser.role == UserRole.TEACHER.value && course.teacherId == currentUser.uid) ||
                                    currentUser.role == UserRole.ADMIN.value
                                )
                            VideoCommentCard(
                                comment = comment,
                                canDelete = canDelete,
                                isTeacherModeration = currentUser?.role == UserRole.TEACHER.value &&
                                    course.teacherId == currentUser.uid &&
                                    currentUser.uid != comment.studentId,
                                onDelete = { onDeleteComment(comment.commentId) }
                            )
                        }
                    }
                }

                // 6. RELATED VIDEOS SECTION
                if (relatedVideos.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = DeepNavyBorder)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("player_related_videos_heading")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircleOutline,
                            contentDescription = null,
                            tint = GoldCoinBright,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Related Videos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_related_videos_list")
                    ) {
                        relatedVideos.forEach { rel ->
                            RelatedLongVideoCard(
                                course = rel,
                                likeCount = likeCountsByVideo[rel.courseId] ?: 0,
                                commentCount = commentCountsByVideo[rel.courseId] ?: 0,
                                onClick = { onSelectRelatedVideo(rel.courseId) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun RelatedLongVideoCard(
    course: CourseEntity,
    likeCount: Int,
    commentCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavySurface),
        border = BorderStroke(1.dp, DeepNavyBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("related_video_${course.courseId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(136.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                VideoPreviewImage(
                    videoUrl = course.videoUrl,
                    previewImageUrl = course.thumbnailUrl,
                    contentDescription = course.title,
                    modifier = Modifier.fillMaxSize()
                )
                if (course.durationMs > 0L) {
                    Surface(
                        color = DeepNavyBg.copy(alpha = 0.86f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = GoldCoinBright,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = formatDurationMs(course.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = course.teacherName,
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanHighlight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${course.totalViews} views • $likeCount likes • $commentCount comments",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkDarkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VideoCommentCard(
    comment: VideoCommentEntity,
    canDelete: Boolean,
    isTeacherModeration: Boolean,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavySurface),
        border = BorderStroke(1.dp, DeepNavyBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("comment_item_${comment.commentId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            UserAvatar(
                photoUrl = comment.studentPhoto,
                name = comment.studentName,
                size = 42.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                            onClick = onDelete,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("delete_comment_${comment.commentId}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = if (isTeacherModeration) "Remove comment (Moderate)" else "Delete comment",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(20.dp)
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

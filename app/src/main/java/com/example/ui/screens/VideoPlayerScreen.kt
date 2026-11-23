package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.CourseEntity
import com.example.data.local.CourseProgressEntity
import com.example.ui.components.StatusPill
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    course: CourseEntity?,
    isSaved: Boolean,
    progress: CourseProgressEntity?,
    isStudent: Boolean,
    onToggleSave: () -> Unit,
    onUpdateProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onMarkCompleted: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isFullscreen by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var currentPosMs by remember { mutableLongStateOf(progress?.watchedPositionMs ?: 0L) }
    var durationMs by remember { mutableLongStateOf(progress?.durationMs ?: 0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onBack()
        }
    }

    // Apply orientation when toggling fullscreen
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

    // Poll playback position and persist progress cleanly
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
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Course not found.", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onBack) { Text("Go Back") }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isFullscreen) Color.Black else MaterialTheme.colorScheme.background)
            .then(if (isFullscreen) Modifier else Modifier.safeDrawingPadding())
    ) {
        // Video Player Surface & Overlay Controls
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
                            playbackError = "Unable to play this video (code $what/$extra). Verify the video file or network connection."
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

            // Loading / Buffering indicator
            if (isBuffering && playbackError == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Loading video...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Playback Error Overlay
            if (playbackError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.82f))
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
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry Playback")
                        }
                    }
                }
            }

            // Custom Controls Overlay
            if (controlsVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top bar
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
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier.testTag("player_fullscreen_button")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                tint = Color.White
                            )
                        }
                    }

                    // Center Play/Pause + 10s Skip Controls
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
                            color = CobaltPrimary.copy(alpha = 0.92f),
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

                    // Bottom Seek Bar & Duration
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
                                thumbColor = Color.White,
                                activeTrackColor = CobaltPrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.35f)
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

        // Course metadata & Student controls (hidden in fullscreen)
        if (!isFullscreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = course.category,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = CobaltPrimary
                    )
                    if (progress?.completed == true) {
                        StatusPill(
                            label = "COMPLETED",
                            containerColor = StatusSuccessContainer,
                            contentColor = StatusSuccess
                        )
                    }
                }

                Text(
                    text = course.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.School, contentDescription = null, tint = RoyalPurple, modifier = Modifier.size(18.dp))
                        Text(course.teacherName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Text("${course.totalViews} views", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    text = "Published on ${formatDateTime(course.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isStudent) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onToggleSave,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("player_save_course_button")
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isSaved) "Saved" else "Save Course")
                        }

                        Button(
                            onClick = onMarkCompleted,
                            enabled = progress?.completed != true,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("player_mark_complete_button")
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (progress?.completed == true) "Completed" else "Mark Complete")
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "About This Course",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = course.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

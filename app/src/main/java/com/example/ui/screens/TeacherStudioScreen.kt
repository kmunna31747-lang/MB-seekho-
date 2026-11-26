package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CoinTransactionEntity
import com.example.data.local.UserEntity
import com.example.data.repository.TeacherStudioStats
import com.example.data.repository.TeacherVideoPerformance
import com.example.ui.components.StatusPill
import com.example.ui.components.formatDateShort
import com.example.ui.components.formatDateTime
import com.example.ui.theme.CobaltBright
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CyanGlowContainer
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.DeepNavyBg
import com.example.ui.theme.DeepNavyBorder
import com.example.ui.theme.DeepNavyCard
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinBright
import com.example.ui.theme.GoldCoinDarkContainer
import com.example.ui.theme.GoldCoinSoft
import com.example.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStudioScreen(
    teacher: UserEntity?,
    stats: TeacherStudioStats,
    onOpenVideo: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val nextCoinProgress by animateFloatAsState(
        targetValue = (stats.viewsTowardNextCoin / 10f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450),
        label = "next_coin_progress"
    )

    Scaffold(
        containerColor = DeepNavyBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Teacher Studio",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = teacher?.name ?: "Creator Analytics",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanHighlight
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("teacher_studio_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Teacher Profile",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepNavyBg,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(DeepNavyBg, DeepNavySurface, DeepNavyBg)
                    )
                )
                .padding(innerPadding)
                .testTag("teacher_studio_dashboard")
        ) {
            // 28-Day Analytics Window Banner
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CyanGlowContainer.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, CyanHighlight.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = CyanHighlight,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Last 28 Days Performance",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            val rangeText = if (stats.windowStartMillis > 0L && stats.windowEndMillis > 0L) {
                                "${formatDateShort(stats.windowStartMillis)} — ${formatDateShort(stats.windowEndMillis)}"
                            } else {
                                "Rolling 28-day creator window"
                            }
                            Text(
                                text = rangeText,
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanHighlight.copy(alpha = 0.92f)
                            )
                        }
                        StatusPill(
                            label = "LIVE",
                            containerColor = StatusSuccess.copy(alpha = 0.24f),
                            contentColor = Color(0xFF6EE7B7)
                        )
                    }
                }
            }

            // D. Total Coin Balance — Hero Gold Highlight Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
                    border = BorderStroke(1.5.dp, GoldCoin.copy(alpha = 0.65f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_total_coin_balance_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        GoldCoinDarkContainer.copy(alpha = 0.85f),
                                        DeepNavyCard,
                                        DeepNavySurface
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Total Coin Balance",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = GoldCoinSoft.copy(alpha = 0.88f)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stats.totalCoinBalance.toString(),
                                            style = MaterialTheme.typography.displayMedium,
                                            color = GoldCoinBright,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.testTag("studio_coin_balance_value")
                                        )
                                        Text(
                                            text = if (stats.totalCoinBalance == 1) "Coin" else "Coins",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = GoldCoinSoft,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(GoldCoin, GoldCoinBright))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MonetizationOn,
                                        contentDescription = "Teacher Coins",
                                        tint = DeepNavyBg,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            // Next Coin Progress Bar (Every 10 Valid Views = 1 Coin)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Reward Rule: 10 Valid Views = 1 Coin",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyanHighlight
                                    )
                                    Text(
                                        text = "${stats.viewsTowardNextCoin} / 10 views",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { nextCoinProgress },
                                    color = GoldCoinBright,
                                    trackColor = Color.White.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                                Text(
                                    text = "${10 - stats.viewsTowardNextCoin} more valid student view(s) to unlock +1 coin • All-time valid views: ${stats.totalValidViewsAllTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }
                }
            }

            // Section Heading: Last 28 Days Statistics
            item {
                Text(
                    text = "Analytics — Last 28 Days",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // A. Total Views & B. Total Videos Uploaded (Last 28 Days)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudioMetricCard(
                        title = "Total Views",
                        subtitle = "Last 28 days",
                        value = stats.viewsLast28Days.toString(),
                        icon = Icons.Rounded.Visibility,
                        accentColor = CyanHighlight,
                        testTag = "studio_28d_views_card",
                        modifier = Modifier.weight(1f)
                    )
                    StudioMetricCard(
                        title = "Videos Uploaded",
                        subtitle = "Last 28 days",
                        value = stats.videosUploadedLast28Days.toString(),
                        icon = Icons.Rounded.VideoLibrary,
                        accentColor = CobaltBright,
                        testTag = "studio_28d_videos_card",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // C. Total Coins Earned (Last 28 Days)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
                    border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_28d_coins_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(GoldCoin.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.TrendingUp,
                                    contentDescription = null,
                                    tint = GoldCoinBright,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Coins Earned (Last 28 Days)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Automatically credited from verified views",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }
                        Text(
                            text = "+${stats.coinsEarnedLast28Days}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = GoldCoinBright,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Security / Integrity Notice
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DeepNavySurface,
                    border = BorderStroke(1.dp, DeepNavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = CyanHighlight,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Views & coin balances are strictly verified and protected against repeated refreshes or self-views. Analytics are private to your account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }

            // Video Breakdown (Last 28 Days)
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoGraph,
                        contentDescription = null,
                        tint = CyanHighlight,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Your Videos Performance",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (stats.topVideosLast28Days.isEmpty()) {
                item {
                    StudioEmptyCard(
                        title = "No Videos Uploaded Yet",
                        subtitle = "Upload a course video from the Upload tab. Valid student views will automatically track here and earn you 1 coin per 10 valid views."
                    )
                }
            } else {
                items(stats.topVideosLast28Days, key = { it.courseId }) { video ->
                    StudioVideoRow(
                        video = video,
                        onClick = { onOpenVideo(video.courseId) }
                    )
                }
            }

            // Recent Coin Transactions Ledger
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MonetizationOn,
                        contentDescription = null,
                        tint = GoldCoinBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Coin Reward History",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (stats.recentTransactions.isEmpty()) {
                item {
                    StudioEmptyCard(
                        title = "No Coin Transactions Yet",
                        subtitle = "Every 10 verified student views across your videos automatically unlocks 1 coin and logs a verified reward transaction here."
                    )
                }
            } else {
                items(stats.recentTransactions, key = { it.transactionId }) { tx ->
                    StudioCoinTransactionRow(tx = tx)
                }
            }
        }
    }
}

@Composable
private fun StudioMetricCard(
    title: String,
    subtitle: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
        border = BorderStroke(1.dp, DeepNavyBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanHighlight.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun StudioVideoRow(
    video: TeacherVideoPerformance,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
        border = BorderStroke(1.dp, DeepNavyBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CobaltPrimary.copy(alpha = 0.35f))
                    .border(1.dp, CyanHighlight.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Open ${video.title}",
                    tint = CyanHighlight,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${video.category} • Uploaded ${formatDateShort(video.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${video.viewsLast28Days}",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyanHighlight,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "28d views",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun StudioCoinTransactionRow(tx: CoinTransactionEntity) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavyCard),
        border = BorderStroke(1.dp, GoldCoin.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GoldCoin.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MonetizationOn,
                        contentDescription = null,
                        tint = GoldCoinBright,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Milestone: ${tx.viewsCount} Valid Views",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDateTime(tx.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                }
            }
            StatusPill(
                label = "+${tx.coinsEarned} COIN",
                containerColor = GoldCoin.copy(alpha = 0.22f),
                contentColor = GoldCoinBright
            )
        }
    }
}

@Composable
private fun StudioEmptyCard(
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavySurface),
        border = BorderStroke(1.dp, DeepNavyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

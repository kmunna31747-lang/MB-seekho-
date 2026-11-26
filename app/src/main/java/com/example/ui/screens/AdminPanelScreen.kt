package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AccessCodeEntity
import com.example.data.local.CodeBatchEntity
import com.example.data.local.CodeRoleStats
import com.example.data.local.CoinTransactionEntity
import com.example.data.local.CourseEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.TeacherCoinsEntity
import com.example.data.local.UserEntity
import com.example.data.local.VideoCommentEntity
import com.example.data.local.VideoViewEntity
import com.example.data.model.CodeStatus
import com.example.data.model.CourseCategories
import com.example.data.model.PaymentStatus
import com.example.data.model.UserRole
import com.example.data.repository.MbSeekhoRepository
import com.example.ui.AdminCodeFilterState
import com.example.ui.AdminFeedbackState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.formatDateShort
import com.example.ui.components.formatDateTime
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinDarkContainer
import com.example.ui.theme.GoldCoinSoft
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer

private enum class AdminTab(val label: String) {
    OVERVIEW("Overview"),
    CODES("Codes"),
    PAYMENTS("Payments"),
    USERS_COURSES("Manage")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    studentCodeStats: CodeRoleStats,
    teacherCodeStats: CodeRoleStats,
    codeFilter: AdminCodeFilterState,
    filteredCodes: List<AccessCodeEntity>,
    codeBatches: List<CodeBatchEntity>,
    recentRedemptions: List<AccessCodeEntity>,
    availableStudentCodes: List<AccessCodeEntity>,
    availableTeacherCodes: List<AccessCodeEntity>,
    users: List<UserEntity>,
    courses: List<CourseEntity>,
    payments: List<PaymentEntity>,
    teacherCoins: List<TeacherCoinsEntity>,
    coinTransactions: List<CoinTransactionEntity>,
    validViews: List<VideoViewEntity>,
    comments: List<VideoCommentEntity>,
    feedback: AdminFeedbackState,
    onCodeRoleFilterChange: (UserRole) -> Unit,
    onCodeStatusFilterChange: (String) -> Unit,
    onCodeSearchQueryChange: (String) -> Unit,
    onLoadMoreCodes: () -> Unit,
    onImportCodesFile: (uri: Uri, fileName: String, role: UserRole) -> Unit,
    onImportCodesText: (rawText: String, role: UserRole) -> Unit,
    onGenerateCodesBatch: (role: UserRole, count: Int, batchLabel: String) -> Unit,
    onShareGeneratedBatchCsv: () -> Unit,
    onAssignCode: (codeId: String, customerName: String) -> Unit,
    onDisableCode: (codeId: String) -> Unit,
    onEnableCode: (codeId: String) -> Unit,
    onToggleUserActive: (uid: String, active: Boolean) -> Unit,
    onRemoveCourse: (courseId: String) -> Unit,
    onUpdateCourseCategory: (courseId: String, category: String) -> Unit,
    onDeleteComment: (commentId: String) -> Unit,
    onRecordPayment: (customerName: String, role: UserRole, amount: String, ref: String, notes: String) -> Unit,
    onVerifyPayment: (paymentId: String, status: PaymentStatus, assignedCodeId: String?, customerName: String) -> Unit,
    onExportCsv: (UserRole) -> Unit,
    onClearGeneratedReveal: () -> Unit,
    onClearExportedCsv: () -> Unit,
    onLogout: () -> Unit
) {
    BackHandler(onBack = onLogout)

    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val selectedTab = AdminTab.entries[selectedTabIndex]

    // Share CSV whenever generated
    LaunchedEffect(feedback.exportedCsv) {
        val csv = feedback.exportedCsv ?: return@LaunchedEffect
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, feedback.exportedCsvSubject)
            putExtra(Intent.EXTRA_TEXT, csv)
        }
        runCatching {
            context.startActivity(Intent.createChooser(shareIntent, "Export Access Codes CSV"))
        }
        onClearExportedCsv()
    }

    // One-time reveal & CSV export dialog for newly generated unlimited access code batches
    feedback.generatedBatch?.let { batch ->
        AlertDialog(
            onDismissRequest = onClearGeneratedReveal,
            title = { Text("New ${batch.role.label} Access Code Batch") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Generated ${batch.generatedCount} unique ${batch.role.label} code(s) (${batch.batchId}). Export or copy them privately right now — only SHA-256 hashes are saved in the database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onShareGeneratedBatchCsv,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export / Share All ${batch.generatedCount} Codes (CSV)")
                    }
                    if (batch.generatedCount > batch.sampleReveals.size) {
                        Text(
                            text = "Previewing first ${batch.sampleReveals.size} of ${batch.generatedCount} codes:",
                            style = MaterialTheme.typography.labelSmall,
                            color = CobaltPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    batch.sampleReveals.forEach { reveal ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(reveal.codeId, style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = reveal.rawCode,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText(reveal.codeId, reveal.rawCode))
                                    }
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy ${reveal.codeId}")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onClearGeneratedReveal) { Text("Done") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Owner Admin Panel",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("admin_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Lock & Exit Owner Console"
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
                val items = listOf(
                    AdminTab.OVERVIEW to Icons.Rounded.Dashboard,
                    AdminTab.CODES to Icons.Rounded.Key,
                    AdminTab.PAYMENTS to Icons.Rounded.Payments,
                    AdminTab.USERS_COURSES to Icons.Rounded.Group
                )
                items.forEachIndexed { idx, (tab, icon) ->
                    NavigationBarItem(
                        selected = selectedTabIndex == idx,
                        onClick = { selectedTabIndex = idx },
                        icon = { Icon(icon, contentDescription = tab.label) },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("admin_nav_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (feedback.isBatchBusy) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(18.dp),
                                color = CobaltPrimary
                            )
                            Text(
                                text = feedback.batchProgressText ?: "Processing access code batch...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CobaltDark
                            )
                        }
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            } else if (!feedback.error.isNullOrBlank()) {
                ErrorBanner(
                    message = feedback.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else if (!feedback.message.isNullOrBlank()) {
                Surface(
                    color = StatusSuccessContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = feedback.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusSuccess,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    AdminTab.OVERVIEW -> AdminOverviewTab(
                        studentStats = studentCodeStats,
                        teacherStats = teacherCodeStats,
                        codeBatchesCount = codeBatches.size,
                        recentRedemptions = recentRedemptions,
                        users = users,
                        courses = courses,
                        payments = payments,
                        teacherCoins = teacherCoins,
                        coinTransactions = coinTransactions,
                        validViews = validViews,
                        comments = comments
                    )
                    AdminTab.CODES -> AdminCodesTab(
                        studentStats = studentCodeStats,
                        teacherStats = teacherCodeStats,
                        codeFilter = codeFilter,
                        filteredCodes = filteredCodes,
                        codeBatches = codeBatches,
                        isBusy = feedback.isBatchBusy,
                        onRoleChange = onCodeRoleFilterChange,
                        onStatusChange = onCodeStatusFilterChange,
                        onSearchChange = onCodeSearchQueryChange,
                        onLoadMore = onLoadMoreCodes,
                        onImportFile = onImportCodesFile,
                        onImportText = onImportCodesText,
                        onGenerateBatch = onGenerateCodesBatch,
                        onAssignCode = onAssignCode,
                        onDisableCode = onDisableCode,
                        onEnableCode = onEnableCode,
                        onExportCsv = onExportCsv
                    )
                    AdminTab.PAYMENTS -> AdminPaymentsTab(
                        payments = payments,
                        availableStudentCodes = availableStudentCodes,
                        availableTeacherCodes = availableTeacherCodes,
                        onRecordPayment = onRecordPayment,
                        onVerifyPayment = onVerifyPayment
                    )
                    AdminTab.USERS_COURSES -> AdminUsersAndCoursesTab(
                        users = users.filter { it.role != UserRole.ADMIN.value },
                        courses = courses,
                        teacherCoins = teacherCoins,
                        coinTransactions = coinTransactions,
                        validViews = validViews,
                        comments = comments,
                        onToggleUserActive = onToggleUserActive,
                        onRemoveCourse = onRemoveCourse,
                        onUpdateCourseCategory = onUpdateCourseCategory,
                        onDeleteComment = onDeleteComment
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminOverviewTab(
    studentStats: CodeRoleStats,
    teacherStats: CodeRoleStats,
    codeBatchesCount: Int,
    recentRedemptions: List<AccessCodeEntity>,
    users: List<UserEntity>,
    courses: List<CourseEntity>,
    payments: List<PaymentEntity>,
    teacherCoins: List<TeacherCoinsEntity>,
    coinTransactions: List<CoinTransactionEntity>,
    validViews: List<VideoViewEntity>,
    comments: List<VideoCommentEntity>
) {
    val totalStudents = users.count { it.role == UserRole.STUDENT.value }
    val totalTeachers = users.count { it.role == UserRole.TEACHER.value }
    val publishedCourses = courses.count { it.published }
    val totalCoinsAwarded = teacherCoins.sumOf { it.totalCoins }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_overview_tab")
    ) {
        item {
            Text("Access Code Inventory & Platform Metrics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Student Codes", studentStats.total.toString(), CobaltPrimary, Modifier.weight(1f))
                AdminMetricCard("Teacher Codes", teacherStats.total.toString(), RoyalPurple, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard(
                    "Student Unused / Used",
                    "${studentStats.unused} / ${studentStats.used}",
                    StatusSuccess,
                    Modifier.weight(1f)
                )
                AdminMetricCard(
                    "Teacher Unused / Used",
                    "${teacherStats.unused} / ${teacherStats.used}",
                    StatusWarning,
                    Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Active Students", totalStudents.toString(), CobaltDark, Modifier.weight(1f))
                AdminMetricCard("Verified Teachers", totalTeachers.toString(), RoyalPurple, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Published / Videos", "$publishedCourses / ${courses.size}", CobaltPrimary, Modifier.weight(1f))
                AdminMetricCard("Valid Video Views", validViews.size.toString(), StatusSuccess, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Coins Distributed", "$totalCoinsAwarded", GoldCoin, Modifier.weight(1f))
                AdminMetricCard("Batches / Payments", "$codeBatchesCount / ${payments.size}", CobaltPrimary, Modifier.weight(1f))
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Recent Code Redemptions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (recentRedemptions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No access codes have been redeemed yet. Use the Codes tab to import the Student CSV/TXT batch or generate new batches.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentRedemptions.take(10), key = { it.codeId }) { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${item.codeId} (${item.role.uppercase()})", fontWeight = FontWeight.Bold)
                            Text(
                                text = "Redeemed by: ${item.redeemedByUid ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = item.redeemedAt?.let { formatDateShort(it) } ?: "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusSuccess
                        )
                    }
                }
            }
        }

        if (coinTransactions.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Recent Teacher Coin Rewards (${comments.size} comments)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(coinTransactions.take(8), key = { it.transactionId }) { tx ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "+${tx.coinsEarned} Coin • ${tx.teacherId}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${tx.viewsCount} valid views milestone • ${formatDateTime(tx.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            label = "+${tx.coinsEarned} COIN",
                            containerColor = GoldCoinDarkContainer,
                            contentColor = GoldCoinSoft
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMetricCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminCodesTab(
    studentStats: CodeRoleStats,
    teacherStats: CodeRoleStats,
    codeFilter: AdminCodeFilterState,
    filteredCodes: List<AccessCodeEntity>,
    codeBatches: List<CodeBatchEntity>,
    isBusy: Boolean,
    onRoleChange: (UserRole) -> Unit,
    onStatusChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onImportFile: (Uri, String, UserRole) -> Unit,
    onImportText: (String, UserRole) -> Unit,
    onGenerateBatch: (UserRole, Int, String) -> Unit,
    onAssignCode: (String, String) -> Unit,
    onDisableCode: (String) -> Unit,
    onEnableCode: (String) -> Unit,
    onExportCsv: (UserRole) -> Unit
) {
    val context = LocalContext.current
    var assignTarget by remember { mutableStateOf<AccessCodeEntity?>(null) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showPasteImportDialog by remember { mutableStateOf(false) }

    val activeRole = codeFilter.role
    val activeStats = if (activeRole == UserRole.STUDENT) studentStats else teacherStats
    val roleBatches = remember(codeBatches, activeRole) {
        codeBatches.filter { it.role == activeRole.value }
    }

    // Zero-permission Android Storage Access Framework (SAF) file picker for CSV / TXT batches
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = resolveDocumentDisplayName(context, uri)
            onImportFile(uri, fileName, activeRole)
        }
    }

    assignTarget?.let { target ->
        var customerName by remember { mutableStateOf(target.assignedToName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { assignTarget = null },
            title = { Text("Assign ${activeRole.label} Code ${target.codeId}") },
            text = {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAssignCode(target.codeId, customerName)
                        assignTarget = null
                    },
                    enabled = customerName.trim().isNotBlank()
                ) { Text("Assign") }
            },
            dismissButton = {
                TextButton(onClick = { assignTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (showGenerateDialog) {
        var genRole by remember { mutableStateOf(activeRole) }
        var countText by remember { mutableStateOf(if (activeRole == UserRole.STUDENT) "1000" else "10") }
        var batchLabel by remember { mutableStateOf("") }
        val presets = if (genRole == UserRole.STUDENT) listOf(100, 1000, 5000, 10000) else listOf(5, 10, 25, 50)

        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("Generate New Access Code Batch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Unlimited batches supported (10,000, 50,000, 100,000+ total). Duplicate codes are automatically blocked across all existing and future batches.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = genRole == UserRole.STUDENT,
                            onClick = {
                                genRole = UserRole.STUDENT
                                countText = "1000"
                            },
                            label = { Text("Student") }
                        )
                        FilterChip(
                            selected = genRole == UserRole.TEACHER,
                            onClick = {
                                genRole = UserRole.TEACHER
                                countText = "10"
                            },
                            label = { Text("Teacher") }
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            FilterChip(
                                selected = countText == preset.toString(),
                                onClick = { countText = preset.toString() },
                                label = { Text("+$preset") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Batch Quantity (up to 50,000 per batch)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = batchLabel,
                        onValueChange = { batchLabel = it },
                        label = { Text("Batch Label (Optional)") },
                        placeholder = { Text("e.g. Batch #2 — 10,000 New Codes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = countText.toIntOrNull()?.coerceIn(1, 50_000) ?: 10
                        onGenerateBatch(genRole, qty, batchLabel)
                        showGenerateDialog = false
                    }
                ) { Text("Generate") }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPasteImportDialog) {
        var pastedText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasteImportDialog = false },
            title = { Text("Paste & Import ${activeRole.label} Codes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste lines in TXT, CSV, or numbered format (e.g. 00001. MBST-XXXX-XXXX-XXXX, STU0001 MBST-XXXX-XXXX, or one code per line). Duplicates are skipped automatically and only SHA-256 hashes are saved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pastedText,
                        onValueChange = { pastedText = it },
                        label = { Text("Paste Access Codes") },
                        placeholder = { Text("00001. MBST-XXXX-XXXX-XXXX\n00002. MBST-YYYY-YYYY-YYYY") },
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportText(pastedText, activeRole)
                        showPasteImportDialog = false
                    },
                    enabled = pastedText.isNotBlank()
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showPasteImportDialog = false }) { Text("Cancel") }
            }
        )
    }

    val expectedFilteredTotal = when (codeFilter.statusFilter) {
        "unused" -> activeStats.unused
        "used" -> activeStats.used
        "disabled" -> activeStats.disabled
        else -> activeStats.total
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_codes_list")
    ) {
        // Role switcher: Student Access Codes vs Teacher Access Codes
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = activeRole == UserRole.STUDENT,
                    onClick = { onRoleChange(UserRole.STUDENT) },
                    label = { Text("Student Codes (${studentStats.total})") },
                    modifier = Modifier.testTag("admin_role_student_codes")
                )
                FilterChip(
                    selected = activeRole == UserRole.TEACHER,
                    onClick = { onRoleChange(UserRole.TEACHER) },
                    label = { Text("Teacher Codes (${teacherStats.total})") },
                    modifier = Modifier.testTag("admin_role_teacher_codes")
                )
            }
        }

        // Scalable SQL KPI strip (Unused / Used / Disabled)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MiniStatChip("Total", activeStats.total, CobaltPrimary, Modifier.weight(1f))
                MiniStatChip("Unused", activeStats.unused, StatusSuccess, Modifier.weight(1f))
                MiniStatChip("Used", activeStats.used, CobaltDark, Modifier.weight(1f))
                MiniStatChip("Disabled", activeStats.disabled, StatusError, Modifier.weight(1f))
            }
        }

        // Primary Admin Actions: Import CSV/TXT, Generate Unlimited Batch, Paste Import, Export CSV
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            importFileLauncher.launch(
                                arrayOf("text/plain", "text/csv", "text/comma-separated-values", "text/*")
                            )
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_import_file_button")
                    ) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Import CSV/TXT")
                    }
                    Button(
                        onClick = { showGenerateDialog = true },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_generate_codes_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Generate Batch")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showPasteImportDialog = true },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_paste_import_button")
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Paste Import")
                    }
                    OutlinedButton(
                        onClick = { onExportCsv(activeRole) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_export_csv_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export Status CSV")
                    }
                }
            }
        }

        // Batch History strip
        if (roleBatches.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Recent ${activeRole.label} Batches (${roleBatches.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(roleBatches.take(10), key = { it.batchId }) { batch ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = batch.sourceLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "+${batch.insertedCount} added • ${batch.duplicateCount} dup skipped",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusSuccess
                                    )
                                    Text(
                                        text = formatDateShort(batch.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search input + Status Filter chips (All / Unused / Used / Disabled)
        item {
            OutlinedTextField(
                value = codeFilter.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search Code ID (e.g. STU0001), name, UID, or batch...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_code_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf(
                    "all" to "All",
                    "unused" to "Unused",
                    "used" to "Used",
                    "disabled" to "Disabled"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = codeFilter.statusFilter == key,
                        onClick = { onStatusChange(key) },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (filteredCodes.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (activeStats.total == 0 && activeRole == UserRole.STUDENT) {
                            "No Student Access Codes imported yet. Tap \"Import CSV/TXT\" to import your 10,000+ Student Access Codes file or tap \"Generate Batch\" to create a new unlimited batch."
                        } else {
                            "No ${activeRole.label} codes match this filter."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "Showing ${filteredCodes.size} of $expectedFilteredTotal ${activeRole.label} code(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(filteredCodes, key = { it.codeId }) { code ->
                val statusEnum = CodeStatus.fromValue(code.status)
                val (pillBg, pillFg) = when (statusEnum) {
                    CodeStatus.AVAILABLE -> StatusSuccessContainer to StatusSuccess
                    CodeStatus.ASSIGNED -> StatusWarningContainer to StatusWarning
                    CodeStatus.USED -> MaterialTheme.colorScheme.primaryContainer to CobaltPrimary
                    CodeStatus.DISABLED -> StatusErrorContainer to StatusError
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${code.codeId} • ${code.role.uppercase()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(
                                label = statusEnum.label.uppercase(),
                                containerColor = pillBg,
                                contentColor = pillFg
                            )
                        }
                        Text(
                            text = "SHA-256: ${code.codeHash.take(18)}..." +
                                (code.batchId?.let { " • $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!code.assignedToName.isNullOrBlank()) {
                            Text(
                                text = "Assigned to: ${code.assignedToName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (!code.redeemedByUid.isNullOrBlank()) {
                            Text(
                                text = "Redeemed by ${code.redeemedByUid} on ${code.redeemedAt?.let { formatDateTime(it) } ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CobaltPrimary
                            )
                        }
                        if (statusEnum == CodeStatus.AVAILABLE || statusEnum == CodeStatus.ASSIGNED) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { assignTarget = code },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Assign")
                                }
                                OutlinedButton(
                                    onClick = { onDisableCode(code.codeId) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Disable")
                                }
                            }
                        } else if (statusEnum == CodeStatus.DISABLED) {
                            OutlinedButton(
                                onClick = { onEnableCode(code.codeId) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusSuccess),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Re-enable as Unused")
                            }
                        }
                    }
                }
            }

            if (filteredCodes.size >= codeFilter.pageLimit && filteredCodes.size < expectedFilteredTotal) {
                item {
                    OutlinedButton(
                        onClick = onLoadMore,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Load More Codes (+100)")
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStatChip(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun resolveDocumentDisplayName(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment ?: "codes_import.txt"
}

@Composable
private fun AdminPaymentsTab(
    payments: List<PaymentEntity>,
    availableStudentCodes: List<AccessCodeEntity>,
    availableTeacherCodes: List<AccessCodeEntity>,
    onRecordPayment: (String, UserRole, String, String, String) -> Unit,
    onVerifyPayment: (String, PaymentStatus, String?, String) -> Unit
) {
    var showAddPayment by remember { mutableStateOf(false) }
    var verifyTarget by remember { mutableStateOf<PaymentEntity?>(null) }

    if (showAddPayment) {
        var customerName by remember { mutableStateOf("") }
        var userRole by remember { mutableStateOf(UserRole.STUDENT) }
        var amount by remember { mutableStateOf("") }
        var txnRef by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPayment = false },
            title = { Text("Record Manual Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Payments start as Pending Verification and never auto-activate Student or Teacher accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = userRole == UserRole.STUDENT,
                            onClick = { userRole = UserRole.STUDENT },
                            label = { Text("Student") }
                        )
                        FilterChip(
                            selected = userRole == UserRole.TEACHER,
                            onClick = { userRole = UserRole.TEACHER },
                            label = { Text("Teacher") }
                        )
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Payment Amount (Owner Decided)") },
                        placeholder = { Text("Enter amount paid") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = txnRef,
                        onValueChange = { txnRef = it },
                        label = { Text("Transaction Reference") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRecordPayment(customerName, userRole, amount, txnRef, notes)
                        showAddPayment = false
                    },
                    enabled = customerName.isNotBlank() && amount.isNotBlank() && txnRef.isNotBlank()
                ) { Text("Save Record") }
            },
            dismissButton = {
                TextButton(onClick = { showAddPayment = false }) { Text("Cancel") }
            }
        )
    }

    verifyTarget?.let { payment ->
        val roleCodes = if (payment.userRole.equals(UserRole.TEACHER.value, ignoreCase = true)) {
            availableTeacherCodes
        } else {
            availableStudentCodes
        }
        var selectedCodeId by remember { mutableStateOf(roleCodes.firstOrNull()?.codeId.orEmpty()) }
        AlertDialog(
            onDismissRequest = { verifyTarget = null },
            title = { Text("Manual Payment Verification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Customer: ${payment.customerName} (${payment.userRole.uppercase()})")
                    Text("Amount: ${payment.paymentAmount} • Ref: ${payment.transactionRef}")
                    OutlinedTextField(
                        value = selectedCodeId,
                        onValueChange = { selectedCodeId = it.uppercase() },
                        label = { Text("Assign Unused Code ID (e.g. STU0001 / TCH001)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (roleCodes.isNotEmpty()) {
                        Text(
                            text = "Next unused: ${roleCodes.take(5).joinToString(", ") { it.codeId }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onVerifyPayment(
                            payment.paymentId,
                            PaymentStatus.VERIFIED,
                            selectedCodeId.takeIf { it.isNotBlank() },
                            payment.customerName
                        )
                        verifyTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                ) { Text("Verify & Assign") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onVerifyPayment(payment.paymentId, PaymentStatus.REJECTED, null, payment.customerName)
                            verifyTarget = null
                        }
                    ) { Text("Reject", color = StatusError) }
                    TextButton(onClick = { verifyTarget = null }) { Text("Cancel") }
                }
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_payments_list")
    ) {
        item {
            Button(
                onClick = { showAddPayment = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_add_payment_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add Private Payment Record")
            }
        }

        if (payments.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No payment records yet. Log Student or Teacher payments here and verify them manually before sharing a private Access Code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(payments, key = { it.paymentId }) { p ->
                val statusEnum = PaymentStatus.fromValue(p.verificationStatus)
                val (bg, fg) = when (statusEnum) {
                    PaymentStatus.VERIFIED -> StatusSuccessContainer to StatusSuccess
                    PaymentStatus.PENDING -> StatusWarningContainer to StatusWarning
                    PaymentStatus.REJECTED -> StatusErrorContainer to StatusError
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            StatusPill(statusEnum.label.uppercase(), bg, fg)
                        }
                        Text(
                            text = "Role: ${p.userRole.uppercase()} • Amount: ${p.paymentAmount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Txn Ref: ${p.transactionRef} • Date: ${formatDateShort(p.paymentDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!p.assignedCodeId.isNullOrBlank()) {
                            Text(
                                text = "Assigned Code ID: ${p.assignedCodeId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CobaltPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (statusEnum == PaymentStatus.PENDING) {
                            Button(
                                onClick = { verifyTarget = p },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Verify Manually")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminUsersAndCoursesTab(
    users: List<UserEntity>,
    courses: List<CourseEntity>,
    teacherCoins: List<TeacherCoinsEntity>,
    coinTransactions: List<CoinTransactionEntity>,
    validViews: List<VideoViewEntity>,
    comments: List<VideoCommentEntity>,
    onToggleUserActive: (String, Boolean) -> Unit,
    onRemoveCourse: (String) -> Unit,
    onUpdateCourseCategory: (String, String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    var section by remember { mutableStateOf("Teachers") }
    var categoryEditCourse by remember { mutableStateOf<CourseEntity?>(null) }

    val students = remember(users) { users.filter { it.role == UserRole.STUDENT.value } }
    val teachers = remember(users) { users.filter { it.role == UserRole.TEACHER.value } }
    val coinsByTeacher = remember(teacherCoins) { teacherCoins.associateBy { it.teacherId } }
    val courseTitleById = remember(courses) { courses.associate { it.courseId to it.title } }
    val windowStart = remember(validViews, coinTransactions) {
        System.currentTimeMillis() - MbSeekhoRepository.STUDIO_WINDOW_MS
    }

    categoryEditCourse?.let { target ->
        var chosenCategory by remember(target.courseId) {
            mutableStateOf(CourseCategories.normalize(target.category))
        }
        AlertDialog(
            onDismissRequest = { categoryEditCourse = null },
            title = { Text("Update Course Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = target.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CourseCategories.all.forEach { cat ->
                            FilterChip(
                                selected = chosenCategory == cat,
                                onClick = { chosenCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateCourseCategory(target.courseId, chosenCategory)
                        categoryEditCourse = null
                    }
                ) { Text("Save Category") }
            },
            dismissButton = {
                TextButton(onClick = { categoryEditCourse = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_manage_tab")
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Teachers", "Students", "Videos", "Comments")) { tab ->
                    FilterChip(
                        selected = section == tab,
                        onClick = { section = tab },
                        label = {
                            val badge = when (tab) {
                                "Teachers" -> " (${teachers.size})"
                                "Students" -> " (${students.size})"
                                "Videos" -> " (${courses.size})"
                                else -> " (${comments.size})"
                            }
                            Text("$tab$badge")
                        },
                        modifier = Modifier.testTag("admin_manage_filter_${tab.lowercase()}")
                    )
                }
            }
        }

        when (section) {
            "Students" -> {
                if (students.isEmpty()) {
                    item { Text("No activated students yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(students, key = { it.uid }) { user ->
                        AdminUserRow(
                            user = user,
                            subtitle = "Code: ${user.codeId ?: "Legacy"} • Joined ${formatDateShort(user.createdAt)}",
                            onToggleActive = { onToggleUserActive(user.uid, !user.active) }
                        )
                    }
                }
            }

            "Teachers" -> {
                if (teachers.isEmpty()) {
                    item { Text("No verified teachers yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(teachers, key = { it.uid }) { teacher ->
                        val totalCoins = coinsByTeacher[teacher.uid]?.totalCoins ?: 0
                        val views28d = validViews.count { it.teacherId == teacher.uid && it.viewedAt >= windowStart }
                        val coins28d = coinTransactions
                            .filter { it.teacherId == teacher.uid && it.createdAt >= windowStart }
                            .sumOf { it.coinsEarned }
                        val teacherVideoCount = courses.count { it.teacherId == teacher.uid }

                        AdminTeacherRow(
                            teacher = teacher,
                            totalCoins = totalCoins,
                            viewsLast28Days = views28d,
                            coinsLast28Days = coins28d,
                            videoCount = teacherVideoCount,
                            onToggleActive = { onToggleUserActive(teacher.uid, !teacher.active) }
                        )
                    }
                }
            }

            "Videos" -> {
                if (courses.isEmpty()) {
                    item { Text("No videos uploaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(courses, key = { it.courseId }) { course ->
                        val normalizedCategory = CourseCategories.normalize(course.category)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = course.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "By ${course.teacherName} • ${course.totalViews} valid views",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        StatusPill(
                                            label = normalizedCategory,
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = CobaltPrimary
                                        )
                                    }
                                    IconButton(onClick = { onRemoveCourse(course.courseId) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Remove video",
                                            tint = StatusError
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { categoryEditCourse = course },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Change Category")
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                if (comments.isEmpty()) {
                    item { Text("No student comments posted yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(comments, key = { it.commentId }) { comment ->
                        val videoTitle = courseTitleById[comment.videoId] ?: comment.videoId
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${comment.studentName} • ${formatDateTime(comment.createdAt)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "On: $videoTitle",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CobaltPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = comment.commentText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteComment(comment.commentId) },
                                    modifier = Modifier.testTag("admin_delete_comment_${comment.commentId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete comment",
                                        tint = StatusError
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTeacherRow(
    teacher: UserEntity,
    totalCoins: Int,
    viewsLast28Days: Int,
    coinsLast28Days: Int,
    videoCount: Int,
    onToggleActive: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(teacher.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${teacher.uid} • Code: ${teacher.codeId ?: "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onToggleActive,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (teacher.active) StatusError else StatusSuccess
                    )
                ) {
                    Text(if (teacher.active) "Disable" else "Enable")
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.MonetizationOn,
                            contentDescription = null,
                            tint = GoldCoin,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "$totalCoins Coins",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = CobaltPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$viewsLast28Days views (28d)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "+$coinsLast28Days (28d) • $videoCount vids",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminUserRow(
    user: UserEntity,
    subtitle: String,
    onToggleActive: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = user.uid,
                    style = MaterialTheme.typography.labelSmall,
                    color = CobaltPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onToggleActive,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (user.active) StatusError else StatusSuccess
                )
            ) {
                Text(if (user.active) "Disable" else "Enable")
            }
        }
    }
}

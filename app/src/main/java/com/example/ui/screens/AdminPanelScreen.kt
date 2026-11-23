package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
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
import androidx.compose.ui.unit.dp
import com.example.data.local.AccessCodeEntity
import com.example.data.local.CourseEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.UserEntity
import com.example.data.model.CodeStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.UserRole
import com.example.ui.AdminFeedbackState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.formatDateShort
import com.example.ui.components.formatDateTime
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
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
    codes: List<AccessCodeEntity>,
    users: List<UserEntity>,
    courses: List<CourseEntity>,
    payments: List<PaymentEntity>,
    feedback: AdminFeedbackState,
    onAssignCode: (codeId: String, customerName: String) -> Unit,
    onGenerateCodes: (role: UserRole, count: Int) -> Unit,
    onRevokeCode: (codeId: String) -> Unit,
    onToggleUserActive: (uid: String, active: Boolean) -> Unit,
    onRemoveCourse: (courseId: String) -> Unit,
    onRecordPayment: (customerName: String, role: UserRole, amount: String, ref: String, notes: String) -> Unit,
    onVerifyPayment: (paymentId: String, status: PaymentStatus, assignedCodeId: String?, customerName: String) -> Unit,
    onExportCsv: () -> Unit,
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
            putExtra(Intent.EXTRA_SUBJECT, "MB Seekho Access Codes Export")
            putExtra(Intent.EXTRA_TEXT, csv)
        }
        runCatching {
            context.startActivity(Intent.createChooser(shareIntent, "Export Access Codes CSV"))
        }
        onClearExportedCsv()
    }

    // One-time reveal dialog for newly generated access codes
    if (feedback.newlyGeneratedCodes.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onClearGeneratedReveal,
            title = { Text("New Private Access Codes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Copy and save these codes privately right now. Only their SHA-256 hashes are stored in the database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    feedback.newlyGeneratedCodes.forEach { reveal ->
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
                                        style = MaterialTheme.typography.titleMedium,
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
            if (!feedback.error.isNullOrBlank()) {
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
                    AdminTab.OVERVIEW -> AdminOverviewTab(codes, users, courses, payments)
                    AdminTab.CODES -> AdminCodesTab(
                        codes = codes,
                        onAssignCode = onAssignCode,
                        onGenerateCodes = onGenerateCodes,
                        onRevokeCode = onRevokeCode,
                        onExportCsv = onExportCsv
                    )
                    AdminTab.PAYMENTS -> AdminPaymentsTab(
                        payments = payments,
                        availableCodes = codes.filter {
                            it.status == CodeStatus.AVAILABLE.value || it.status == CodeStatus.ASSIGNED.value
                        },
                        onRecordPayment = onRecordPayment,
                        onVerifyPayment = onVerifyPayment
                    )
                    AdminTab.USERS_COURSES -> AdminUsersAndCoursesTab(
                        users = users.filter { it.role != UserRole.ADMIN.value },
                        courses = courses,
                        onToggleUserActive = onToggleUserActive,
                        onRemoveCourse = onRemoveCourse
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminOverviewTab(
    codes: List<AccessCodeEntity>,
    users: List<UserEntity>,
    courses: List<CourseEntity>,
    payments: List<PaymentEntity>
) {
    val studentCodes = codes.filter { it.role == UserRole.STUDENT.value }
    val teacherCodes = codes.filter { it.role == UserRole.TEACHER.value }
    val availStudent = studentCodes.count { it.status == CodeStatus.AVAILABLE.value || it.status == CodeStatus.ASSIGNED.value }
    val usedStudent = studentCodes.count { it.status == CodeStatus.USED.value }
    val availTeacher = teacherCodes.count { it.status == CodeStatus.AVAILABLE.value || it.status == CodeStatus.ASSIGNED.value }
    val usedTeacher = teacherCodes.count { it.status == CodeStatus.USED.value }
    val totalStudents = users.count { it.role == UserRole.STUDENT.value }
    val totalTeachers = users.count { it.role == UserRole.TEACHER.value }
    val publishedCourses = courses.count { it.published }
    val recentRedemptions = codes.filter { it.status == CodeStatus.USED.value }
        .sortedByDescending { it.redeemedAt ?: 0L }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_overview_tab")
    ) {
        item {
            Text("Inventory & Platform Metrics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Total Student Codes", studentCodes.size.toString(), CobaltPrimary, Modifier.weight(1f))
                AdminMetricCard("Total Teacher Codes", teacherCodes.size.toString(), RoyalPurple, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Avail / Used Student", "$availStudent / $usedStudent", StatusSuccess, Modifier.weight(1f))
                AdminMetricCard("Avail / Used Teacher", "$availTeacher / $usedTeacher", StatusWarning, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Total Students", totalStudents.toString(), CobaltDark, Modifier.weight(1f))
                AdminMetricCard("Total Teachers", totalTeachers.toString(), RoyalPurple, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Published / Videos", "$publishedCourses / ${courses.size}", CobaltPrimary, Modifier.weight(1f))
                AdminMetricCard("Payment Records", payments.size.toString(), StatusSuccess, Modifier.weight(1f))
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Code Redemption History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (recentRedemptions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No access codes have been redeemed yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentRedemptions, key = { it.codeId }) { item ->
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

@Composable
private fun AdminCodesTab(
    codes: List<AccessCodeEntity>,
    onAssignCode: (String, String) -> Unit,
    onGenerateCodes: (UserRole, Int) -> Unit,
    onRevokeCode: (String) -> Unit,
    onExportCsv: () -> Unit
) {
    var roleFilter by remember { mutableStateOf("All") }
    var assignTarget by remember { mutableStateOf<AccessCodeEntity?>(null) }
    var showGenerateDialog by remember { mutableStateOf(false) }

    assignTarget?.let { target ->
        var customerName by remember { mutableStateOf(target.assignedToName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { assignTarget = null },
            title = { Text("Assign Code ${target.codeId}") },
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
        var genRole by remember { mutableStateOf(UserRole.STUDENT) }
        var countText by remember { mutableStateOf("5") }
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("Generate Additional Codes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = genRole == UserRole.STUDENT,
                            onClick = { genRole = UserRole.STUDENT },
                            label = { Text("Student") }
                        )
                        FilterChip(
                            selected = genRole == UserRole.TEACHER,
                            onClick = { genRole = UserRole.TEACHER },
                            label = { Text("Teacher") }
                        )
                    }
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Quantity (1–25)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = countText.toIntOrNull()?.coerceIn(1, 25) ?: 1
                        onGenerateCodes(genRole, qty)
                        showGenerateDialog = false
                    }
                ) { Text("Generate") }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) { Text("Cancel") }
            }
        )
    }

    val filtered = remember(codes, roleFilter) {
        when (roleFilter) {
            "Student" -> codes.filter { it.role == UserRole.STUDENT.value }
            "Teacher" -> codes.filter { it.role == UserRole.TEACHER.value }
            "Available" -> codes.filter { it.status == CodeStatus.AVAILABLE.value }
            "Used" -> codes.filter { it.status == CodeStatus.USED.value }
            else -> codes
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_codes_list")
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showGenerateDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_generate_codes_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Generate Codes")
                }
                OutlinedButton(
                    onClick = onExportCsv,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_export_csv_button")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export CSV")
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Student", "Teacher", "Available", "Used")) { f ->
                    FilterChip(selected = roleFilter == f, onClick = { roleFilter = f }, label = { Text(f) })
                }
            }
        }

        items(filtered, key = { it.codeId }) { code ->
            val statusEnum = CodeStatus.fromValue(code.status)
            val (pillBg, pillFg) = when (statusEnum) {
                CodeStatus.AVAILABLE -> StatusSuccessContainer to StatusSuccess
                CodeStatus.ASSIGNED -> StatusWarningContainer to StatusWarning
                CodeStatus.USED -> MaterialTheme.colorScheme.primaryContainer to CobaltPrimary
                CodeStatus.REVOKED -> StatusErrorContainer to StatusError
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
                        StatusPill(label = statusEnum.label.uppercase(), containerColor = pillBg, contentColor = pillFg)
                    }
                    Text(
                        text = "SHA-256: ${code.codeHash.take(18)}...",
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
                                onClick = { onRevokeCode(code.codeId) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Revoke")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPaymentsTab(
    payments: List<PaymentEntity>,
    availableCodes: List<AccessCodeEntity>,
    onRecordPayment: (String, UserRole, String, String, String) -> Unit,
    onVerifyPayment: (String, PaymentStatus, String?, String) -> Unit
) {
    var showAddPayment by remember { mutableStateOf(false) }
    var verifyTarget by remember { mutableStateOf<PaymentEntity?>(null) }

    if (showAddPayment) {
        var customerName by remember { mutableStateOf("") }
        var userRole by remember { mutableStateOf(UserRole.STUDENT) }
        var amount by remember { mutableStateOf("") } // Never prefilled with invented price
        var txnRef by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPayment = false },
            title = { Text("Record Manual Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Payments start as Pending Verification and never auto-activate accounts.",
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
        val roleCodes = availableCodes.filter { it.role.equals(payment.userRole, ignoreCase = true) }
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
                        label = { Text("Assign Access Code ID (e.g. STU001)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (roleCodes.isNotEmpty()) {
                        Text(
                            text = "Next available: ${roleCodes.take(5).joinToString(", ") { it.codeId }}",
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
                        text = "No payment records yet. When a student or teacher pays for access, log the payment here and manually verify it before assigning a private access code.",
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

@Composable
private fun AdminUsersAndCoursesTab(
    users: List<UserEntity>,
    courses: List<CourseEntity>,
    onToggleUserActive: (String, Boolean) -> Unit,
    onRemoveCourse: (String) -> Unit
) {
    var section by remember { mutableStateOf("Students") }
    val students = users.filter { it.role == UserRole.STUDENT.value }
    val teachers = users.filter { it.role == UserRole.TEACHER.value }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Students", "Teachers", "Courses").forEach { tab ->
                    FilterChip(
                        selected = section == tab,
                        onClick = { section = tab },
                        label = { Text(tab) }
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
                        AdminUserRow(user = user, onToggleActive = { onToggleUserActive(user.uid, !user.active) })
                    }
                }
            }
            "Teachers" -> {
                if (teachers.isEmpty()) {
                    item { Text("No activated teachers yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(teachers, key = { it.uid }) { user ->
                        AdminUserRow(user = user, onToggleActive = { onToggleUserActive(user.uid, !user.active) })
                    }
                }
            }
            else -> {
                if (courses.isEmpty()) {
                    item { Text("No courses uploaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(courses, key = { it.courseId }) { course ->
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
                                    Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${course.category} • By ${course.teacherName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onRemoveCourse(course.courseId) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Remove course",
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
private fun AdminUserRow(
    user: UserEntity,
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
                    text = "ID: ${user.uid} • Code: ${user.codeId ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onToggleActive,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (user.active) StatusError else StatusSuccess
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (user.active) "Disable" else "Enable")
            }
        }
    }
}

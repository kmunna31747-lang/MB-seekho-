package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserRole
import com.example.ui.LoginUiState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.MbSeekhoLogoBadge
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.VioletBright

@Composable
fun RoleLoginScreen(
    role: UserRole,
    uiState: LoginUiState,
    onRedeemStudentCode: (rawCode: String, displayName: String) -> Unit,
    onRedeemTeacherCode: (rawCode: String, displayName: String) -> Unit,
    onReturningMemberSignIn: (memberId: String) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val isTeacher = role == UserRole.TEACHER
    val heading = if (isTeacher) {
        stringResource(R.string.teacher_login_heading)
    } else {
        stringResource(R.string.student_login_heading)
    }
    val accentGradient = if (isTeacher) listOf(RoyalPurple, VioletBright) else listOf(CobaltDark, CobaltPrimary)
    val primaryColor = if (isTeacher) RoyalPurple else CobaltPrimary

    var isReturningMember by remember { mutableStateOf(false) }
    var accessCode by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var memberId by remember { mutableStateOf("") }

    fun submitCurrentForm() {
        if (uiState.isLoading) return
        if (isReturningMember) {
            if (memberId.isNotBlank()) onReturningMemberSignIn(memberId)
        } else if (isTeacher) {
            if (accessCode.isNotBlank()) onRedeemTeacherCode(accessCode, displayName)
        } else {
            if (accessCode.isNotBlank()) onRedeemStudentCode(accessCode, displayName)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Brush.linearGradient(accentGradient))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .widthIn(max = 540.dp)
                .align(Alignment.TopCenter)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("login_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to role selection",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MbSeekhoLogoBadge(size = 76.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = heading,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.app_name) + " • " + stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = !isReturningMember,
                            onClick = {
                                isReturningMember = false
                                onClearError()
                            },
                            label = {
                                Text(if (isTeacher) "Teacher Access Code" else "Student Access Code")
                            },
                            modifier = Modifier.testTag("tab_new_access_code")
                        )
                        FilterChip(
                            selected = isReturningMember,
                            onClick = {
                                isReturningMember = true
                                onClearError()
                            },
                            label = { Text(if (isTeacher) "Returning Teacher" else "Returning Student") },
                            modifier = Modifier.testTag("tab_returning_member")
                        )
                    }

                    if (!isReturningMember) {
                        Text(
                            text = if (isTeacher) {
                                "Enter your unique single-use Teacher Access Code issued by the MB Seekho owner to activate your instructor account."
                            } else {
                                "Enter your unique single-use Student Access Code to activate your MB Seekho Student account."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = accessCode,
                            onValueChange = {
                                accessCode = it.uppercase()
                                if (uiState.errorMessage != null) onClearError()
                            },
                            label = {
                                Text(if (isTeacher) "Teacher Access Code" else "Student Access Code")
                            },
                            placeholder = {
                                Text(if (isTeacher) "MBTC-XXXX-XXXX" else "MBST-XXXX-XXXX-XXXX")
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Key, contentDescription = null, tint = primaryColor)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("access_code_input")
                        )

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Your Full Name (Optional)") },
                            placeholder = {
                                Text(if (isTeacher) "e.g. Prof. Aarav Sharma" else "e.g. Priya Verma")
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = primaryColor)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { submitCurrentForm() }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("display_name_input")
                        )
                    } else {
                        Text(
                            text = if (isTeacher) {
                                "Already activated your teacher account? Sign back in using your Teacher ID shown on your Profile."
                            } else {
                                "Already activated your student account? Sign back in using your Student ID shown on your Profile to restore your saved courses and progress."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = memberId,
                            onValueChange = {
                                memberId = it.uppercase()
                                if (uiState.errorMessage != null) onClearError()
                            },
                            label = { Text(if (isTeacher) "Teacher ID" else "Student ID") },
                            placeholder = { Text(if (isTeacher) "TCH_XXXXXXXXXXXXXXXX" else "STU_XXXXXXXXXXXXXXXX") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Badge, contentDescription = null, tint = primaryColor)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { submitCurrentForm() }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_id_input")
                        )
                    }

                    if (!uiState.errorMessage.isNullOrBlank()) {
                        ErrorBanner(
                            message = uiState.errorMessage,
                            modifier = Modifier.testTag("login_error_banner")
                        )
                    }

                    Button(
                        onClick = { submitCurrentForm() },
                        enabled = !uiState.isLoading && (
                            if (isReturningMember) memberId.isNotBlank()
                            else accessCode.isNotBlank()
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("login_continue_button")
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Verifying Code...")
                        } else {
                            Text(
                                text = stringResource(R.string.continue_button),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Mandatory Future Code Notice below Student Access Code entry area
                    if (!isTeacher) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_future_code_notice")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = "Future Student Access Codes Notice",
                                    tint = CobaltPrimary,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(20.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Future Access Code Batches",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = CobaltDark
                                    )
                                    Text(
                                        text = stringResource(R.string.student_future_code_notice),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isTeacher) {
                                    "Teacher Access Codes are SHA-256 hashed, verified atomically on the backend, and redeemable strictly once."
                                } else {
                                    "Student Access Codes are SHA-256 hashed, validated securely on the backend, and each valid code can be redeemed only once."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

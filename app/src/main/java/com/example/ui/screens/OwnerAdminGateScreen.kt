package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.OwnerSecurityEntity
import com.example.ui.LoginUiState
import com.example.ui.components.ErrorBanner
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.RoyalPurple

/**
 * Private Owner Admin authentication gate.
 * Zero hardcoded passwords: on first run the application owner claims ownership by setting a
 * salted PBKDF2-hashed passphrase; on subsequent runs only the verified owner passphrase unlocks
 * the Admin Panel. In Firebase production mode, Admin access is enforced via Firebase Custom Claims.
 */
@Composable
fun OwnerAdminGateScreen(
    ownerSecurity: OwnerSecurityEntity?,
    uiState: LoginUiState,
    onClaimOwner: (ownerName: String, passphrase: String) -> Unit,
    onVerifyOwner: (passphrase: String) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val isAlreadyClaimed = ownerSecurity != null
    var ownerName by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF0F172A), CobaltDark, RoyalPurple)))
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
                modifier = Modifier.testTag("admin_gate_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AdminPanelSettings,
                    contentDescription = "Private Owner Admin",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = if (isAlreadyClaimed) "Owner Admin Verification" else "One-Time Owner Setup",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Restricted exclusively to the MB Seekho application owner",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isAlreadyClaimed) {
                            "Enter your private owner passphrase to manage access codes, payments, students, teachers, and courses."
                        } else {
                            "No owner passphrase is configured yet. Create your private owner passphrase once to lock and protect the Admin Panel."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isAlreadyClaimed) {
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner Display Name") },
                            placeholder = { Text("MB Seekho Owner") },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_claim_name_input")
                        )
                    }

                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = {
                            passphrase = it
                            localError = null
                            if (uiState.errorMessage != null) onClearError()
                        },
                        label = { Text(if (isAlreadyClaimed) "Owner Passphrase" else "Create Private Passphrase (min 6 chars)") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showPassword) "Hide passphrase" else "Show passphrase"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_passphrase_input")
                    )

                    if (!isAlreadyClaimed) {
                        OutlinedTextField(
                            value = confirmPassphrase,
                            onValueChange = {
                                confirmPassphrase = it
                                localError = null
                            },
                            label = { Text("Confirm Private Passphrase") },
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_confirm_passphrase_input")
                        )
                    }

                    val displayError = localError ?: uiState.errorMessage
                    if (!displayError.isNullOrBlank()) {
                        ErrorBanner(message = displayError)
                    }

                    Button(
                        onClick = {
                            if (isAlreadyClaimed) {
                                onVerifyOwner(passphrase)
                            } else {
                                when {
                                    passphrase.length < 6 -> localError = "Passphrase must be at least 6 characters."
                                    passphrase != confirmPassphrase -> localError = "Passphrases do not match."
                                    else -> onClaimOwner(ownerName, passphrase)
                                }
                            }
                        },
                        enabled = !uiState.isLoading && passphrase.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("owner_auth_submit_button")
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Verifying...")
                        } else {
                            Text(
                                text = if (isAlreadyClaimed) "Unlock Owner Admin Panel" else "Claim Owner Admin Access",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

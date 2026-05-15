package com.nexus.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexus.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val user    by vm.user.collectAsState()
    val uiState by vm.uiState.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title   = { Text("Log Out") },
            text    = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = { showLogoutConfirm = false; vm.logout(); onLogout() }) {
                    Text("Log Out", color = NexusRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusDarkMedium),
            )
        },
        containerColor = NexusDark,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Account card
            Surface(
                color = NexusDarkMedium,
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(NexusBlurple),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            user?.username?.take(1)?.uppercase() ?: "?",
                            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user?.username ?: "", color = NexusTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${user?.username ?: ""}#${user?.discriminator ?: "0000"}", color = NexusTextMuted, fontSize = 12.sp)
                        Text(user?.email ?: "", color = NexusTextMuted, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsSection("Account Settings") {
                SettingsRow("Account") {}
                SettingsRow("Privacy & Safety") {}
                SettingsRow("Authorized Apps") {}
                SettingsRow("Devices") {}
                SettingsRow("Connections") {}
            }

            Spacer(Modifier.height(8.dp))

            SettingsSection("App Settings") {
                SettingsRow("Appearance") {}
                SettingsRow("Notifications") {}
                SettingsRow("Text & Images") {}
                SettingsRow("Accessibility") {}
            }

            Spacer(Modifier.height(8.dp))

            SettingsSection("") {
                SettingsRow("Log Out", tint = NexusRed, onClick = { showLogoutConfirm = true })
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Nexus v1.0.0",
                color = NexusTextMuted, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    if (title.isNotBlank()) {
        Text(
            title.uppercase(),
            color = NexusTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
        )
    }
    Surface(color = NexusDarkMedium, modifier = Modifier.fillMaxWidth()) {
        Column { content() }
    }
}

@Composable
fun SettingsRow(label: String, tint: Color = NexusTextPrimary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = tint, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = NexusTextMuted, modifier = Modifier.size(16.dp))
    }
    Divider(color = NexusOutline.copy(alpha = 0.5f), modifier = Modifier.padding(start = 16.dp))
}

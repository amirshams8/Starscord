package com.nexus.android.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexus.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    val user    by vm.user.collectAsState()
    val uiState by vm.uiState.collectAsState()
    val scroll  = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = vm::toggleEdit) {
                        Icon(Icons.Default.Edit, "Edit", tint = if (uiState.editing) NexusBlurple else NexusTextMuted)
                    }
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
                .verticalScroll(scroll),
        ) {
            // Banner + avatar
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.horizontalGradient(listOf(NexusBlurple, Color(0xFF7289DA)))
                        )
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp)
                        .clip(CircleShape)
                        .background(NexusDarkMedium),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        user?.username?.take(1)?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user?.username ?: "", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = NexusTextPrimary)
                        Text("${user?.username ?: ""}#${user?.discriminator ?: "0000"}", color = NexusTextMuted, fontSize = 13.sp)
                    }
                    // Online status badge
                    Surface(shape = RoundedCornerShape(12.dp), color = NexusDarkMedium) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NexusGreen))
                            Spacer(Modifier.width(6.dp))
                            Text(user?.status?.replaceFirstChar { it.uppercase() } ?: "Online", color = NexusTextPrimary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Edit mode fields
                if (uiState.editing) {
                    ProfileEditSection(
                        state   = uiState,
                        onSave  = vm::saveProfile,
                        onChange = vm::onFieldChange,
                        loading = uiState.saving,
                        error   = uiState.error,
                    )
                } else {
                    // Read-only sections
                    ProfileInfoCard(title = "About Me") {
                        Text(
                            user?.bio?.ifBlank { "No bio yet." } ?: "No bio yet.",
                            color = NexusTextMuted, fontSize = 14.sp,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    ProfileInfoCard(title = "Member Since") {
                        Text(user?.createdAt?.take(10) ?: "—", color = NexusTextMuted, fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    ProfileInfoCard(title = "Nitro") {
                        val nitro = user?.nitroType ?: "none"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (nitro == "none") "No Nitro" else nitro.replaceFirstChar { it.uppercase() },
                                color = if (nitro == "none") NexusTextMuted else NitroMid,
                                fontSize = 14.sp,
                                fontWeight = if (nitro != "none") FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoCard(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(8.dp), color = NexusDarkMedium, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NexusTextMuted)
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
fun ProfileEditSection(
    state: ProfileUiState,
    onSave: () -> Unit,
    onChange: (ProfileField, String) -> Unit,
    loading: Boolean,
    error: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.username,
            onValueChange = { onChange(ProfileField.Username, it) },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.bio,
            onValueChange = { onChange(ProfileField.Bio, it) },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
        )
        OutlinedTextField(
            value = state.pronouns,
            onValueChange = { onChange(ProfileField.Pronouns, it) },
            label = { Text("Pronouns") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.customStatus,
            onValueChange = { onChange(ProfileField.CustomStatus, it) },
            label = { Text("Custom Status") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = NexusRed, fontSize = 12.sp) }
        Button(
            onClick = onSave,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NexusBlurple),
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            else Text("Save Changes", fontWeight = FontWeight.Bold)
        }
    }
}

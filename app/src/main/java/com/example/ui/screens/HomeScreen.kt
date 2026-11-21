package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ConversationSummary
import com.example.data.model.ToolModule
import com.example.data.model.UiState
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import com.example.ui.components.NexoraGlowOrb
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.moduleVisualSpec
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CrimsonSos
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldLife
import com.example.ui.theme.IndigoNeural
import com.example.ui.viewmodel.NexoraMainViewModel
import com.example.util.formatBdt
import com.example.util.isNetworkAvailable
import java.util.Calendar

@Composable
fun HomeScreen(
    mainViewModel: NexoraMainViewModel,
    onAskAi: (prompt: String, startVoice: Boolean) -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenTool: (ToolModule) -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val profileState by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val tasksState by mainViewModel.tasks.collectAsStateWithLifecycle()
    val txState by mainViewModel.transactions.collectAsStateWithLifecycle()
    val healthState by mainViewModel.healthRecords.collectAsStateWithLifecycle()
    val convState by mainViewModel.conversations.collectAsStateWithLifecycle()
    val adaptiveModules by mainViewModel.adaptiveModules.collectAsStateWithLifecycle()
    val cachedBriefing by mainViewModel.cachedBriefing.collectAsStateWithLifecycle()
    val briefingLoading by mainViewModel.briefingLoading.collectAsStateWithLifecycle()
    val offlineSmartMode by mainViewModel.offlineSmartMode.collectAsStateWithLifecycle()
    val offlineScratchpad by mainViewModel.offlineScratchpad.collectAsStateWithLifecycle()

    val s = stringsFor(language)
    val isOnline = remember { isNetworkAvailable(context) }
    var quickQuery by remember { mutableStateOf("") }
    var showOfflineNote by remember { mutableStateOf(false) }

    val displayName = (profileState as? UiState.Success)?.data?.displayName?.ifBlank { "Friend" } ?: "Friend"
    val pendingTasks = (tasksState as? UiState.Success)?.data?.count { !it.completed } ?: 0
    val medsRemaining = (healthState as? UiState.Success)?.data?.count { it.recordType == "MEDICINE" && !it.takenToday } ?: 0
    val totalExpenseToday = (txState as? UiState.Success)?.data?.filter { it.txType == "EXPENSE" }?.sumOf { it.amount } ?: 0.0

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when {
        hour < 12 -> s.welcomeMorning
        hour < 17 -> s.welcomeAfternoon
        else -> s.welcomeEvening
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Profile, Alerts & Bilingual Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenProfile)
                            .padding(end = 8.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("home_profile_chip")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(CyanPulse, IndigoNeural))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "$greeting, $displayName",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = s.tagline,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onOpenTool(ToolModule.EMERGENCY_SAFETY) },
                            modifier = Modifier.testTag("home_sos_shortcut")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sos,
                                contentDescription = "Emergency SOS",
                                tint = CrimsonSos
                            )
                        }
                        IconButton(
                            onClick = { onOpenTool(ToolModule.LIFE_MANAGER) },
                            modifier = Modifier.testTag("home_notifications_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Pending reminders",
                                tint = if (pendingTasks + medsRemaining > 0) AmberAlert else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilterChip(
                            selected = language == AppLanguage.BN,
                            onClick = { mainViewModel.toggleLanguage() },
                            label = { Text(if (language == AppLanguage.BN) "বাং" else "EN") },
                            modifier = Modifier.testTag("home_language_chip")
                        )
                    }
                }
            }

            // 2. Connectivity & Offline Smart Mode Banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPillBadge(
                        text = if (isOnline) s.onlineMode else s.offlineSmartModeActive,
                        accent = if (isOnline) EmeraldLife else AmberAlert
                    )
                    if (offlineSmartMode) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { showOfflineNote = !showOfflineNote }
                                .testTag("offline_scratchpad_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "অফলাইন নোট" else "Offline Note",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(visible = showOfflineNote) {
                    var noteDraft by remember(offlineScratchpad) { mutableStateOf(offlineScratchpad) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (language == AppLanguage.BN) "অফলাইন স্মার্ট নোট (ইন্টারনেট ছাড়াই কাজ করে)" else "Offline Smart Scratchpad (Works without internet)",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = noteDraft,
                                onValueChange = { noteDraft = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = {
                                    mainViewModel.saveOfflineScratchpad(noteDraft)
                                    showOfflineNote = false
                                }) {
                                    Text(s.saveLabel)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Central AI Intelligence Hero + Big Search Bar + Voice Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box {
                        Image(
                            painter = painterResource(id = R.drawable.img_nexora_hero),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentScale = ContentScale.Crop,
                            alpha = 0.32f
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            NexoraGlowOrb(
                                size = 76.dp,
                                isActive = true,
                                onClick = { onAskAi("", true) },
                                modifier = Modifier.testTag("home_center_orb")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (language == AppLanguage.BN) "এক অ্যাপেই সম্পূর্ণ ডিজিটাল জীবন" else "One Search, One AI Brain",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = quickQuery,
                                    onValueChange = { quickQuery = it },
                                    placeholder = {
                                        Text(
                                            text = s.askAnythingHint,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { onAskAi("", true) },
                                            modifier = Modifier.testTag("home_voice_input_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = s.voiceInput,
                                                tint = CyanPulse
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(28.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.75f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_search_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        val q = quickQuery.trim()
                                        quickQuery = ""
                                        onAskAi(q, false)
                                    },
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(CyanPulse, IndigoNeural)))
                                        .testTag("home_send_ai_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Ask NEXORA AI",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Live Life Pulse Metrics + Morning Intelligence Briefing
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PulseMetricCard(
                        label = if (language == AppLanguage.BN) "বাকি কাজ" else "Tasks Due",
                        value = "$pendingTasks",
                        accent = CyanPulse,
                        onClick = { onOpenTool(ToolModule.LIFE_MANAGER) },
                        modifier = Modifier.weight(1f)
                    )
                    PulseMetricCard(
                        label = if (language == AppLanguage.BN) "ওষুধ বাকি" else "Meds Left",
                        value = "$medsRemaining",
                        accent = EmeraldLife,
                        onClick = { onOpenTool(ToolModule.HEALTH_ASSISTANT) },
                        modifier = Modifier.weight(1f)
                    )
                    PulseMetricCard(
                        label = if (language == AppLanguage.BN) "মোট খরচ" else "Spent",
                        value = formatBdt(totalExpenseToday),
                        accent = AmberAlert,
                        onClick = { onOpenTool(ToolModule.FINANCE_MANAGER) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = AmberAlert,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = s.morningIntelligence, style = MaterialTheme.typography.titleMedium)
                            }
                            Button(
                                onClick = { mainViewModel.generateMorningBriefing() },
                                enabled = !briefingLoading,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("generate_briefing_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoNeural)
                            ) {
                                if (briefingLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == AppLanguage.BN) "আপডেট" else "Briefing",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = cachedBriefing.ifBlank {
                                if (language == AppLanguage.BN) {
                                    "আপনার কাজ, ওষুধ ও পার্সোনাল মেমোরি বিশ্লেষণ করে সকালের স্মার্ট ব্রিফিং পেতে 'আপডেট' বাটনে ট্যাপ করুন।"
                                } else {
                                    "Tap 'Briefing' for an AI summary of your pending tasks, medicine reminders, and Predictive AI tips."
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 5. Adaptive Quick Access Features
            item {
                Text(
                    text = s.quickAccess,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(adaptiveModules.take(8), key = { it.route }) { module ->
                        val (icon, color) = moduleVisualSpec(module)
                        Card(
                            modifier = Modifier
                                .width(136.dp)
                                .clickable { onOpenTool(module) }
                                .testTag("quick_module_${module.route}"),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(color.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) module.titleBn else module.titleEn,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) module.subtitleBn else module.subtitleEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 6. Recent Conversations
            item {
                Text(
                    text = s.recentConversations,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val conversations = (convState as? UiState.Success)?.data.orEmpty()
            if (conversations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = s.noConversationsYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(conversations.take(5), key = { it.id }) { conv ->
                    ConversationPreviewCard(
                        conv = conv,
                        onClick = { onOpenConversation(conv.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PulseMetricCard(
    label: String,
    value: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConversationPreviewCard(
    conv: ConversationSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("recent_conv_${conv.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conv.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                StatusPillBadge(text = conv.expertMode, accent = CyanPulse)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conv.lastMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

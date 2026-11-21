package com.example.ui.screens.modules

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpertMode
import com.example.data.model.ToolModule
import com.example.data.model.UiState
import com.example.firebase.signOutUser
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import com.example.ui.components.AiResponseCard
import com.example.ui.components.NexoraModuleTopBar
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CrimsonSos
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldLife
import com.example.ui.theme.IndigoNeural
import com.example.ui.theme.VioletExpert
import com.example.ui.viewmodel.AiBrainViewModel
import com.example.ui.viewmodel.NexoraMainViewModel

@Composable
fun MemoryDigitalTwinScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onBack: () -> Unit
) {
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val memoriesState by mainViewModel.memories.collectAsStateWithLifecycle()
    val tasksState by mainViewModel.tasks.collectAsStateWithLifecycle()
    val toolAiState by aiViewModel.toolAiState.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    var showAddDialog by remember { mutableStateOf(false) }
    val memories = (memoriesState as? UiState.Success)?.data.orEmpty()
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.MEMORY_TWIN.titleBn else ToolModule.MEMORY_TWIN.titleEn,
                language = language,
                onBack = onBack,
                onToggleLanguage = { mainViewModel.toggleLanguage() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = VioletExpert,
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, contentDescription = "Add Memory") }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VioletExpert.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, VioletExpert.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = VioletExpert)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "পার্সোনাল ডিজিটাল টুইন ও প্রেডিক্টিভ এআই" else "Personal Digital Twin & Predictive AI",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(text = s.privacyNotice, style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = {
                                    val taskText = tasks.take(8).joinToString(", ") { it.title }
                                    aiViewModel.runToolPrompt(
                                        "Based on my approved Digital Twin memories and current tasks ($taskText), act as my Predictive AI assistant: anticipate what I likely need next today and suggest 3 smart actions (wait for my approval before deciding anything).",
                                        language, ExpertMode.GENERAL, memories
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (language == AppLanguage.BN) "প্রেডিক্টিভ সাজেশন দেখুন" else "Run Predictive AI Insights")
                            }
                        }
                    }
                }
                item { AiResponseCard(state = toolAiState, language = language) }

                items(memories, key = { it.id }) { mem ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mem.title, style = MaterialTheme.typography.titleSmall)
                                    StatusPillBadge(mem.memoryType, VioletExpert)
                                }
                                IconButton(onClick = {
                                    mainViewModel.runRepoAction({
                                        mainViewModel.coreRepo.updateMemory(mem.id, mem.title, mem.content, !mem.pinned)
                                    })
                                }) {
                                    Icon(Icons.Default.PushPin, contentDescription = "Pin", tint = if (mem.pinned) AmberAlert else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    mainViewModel.runRepoAction({ mainViewModel.coreRepo.deleteMemory(mem.id) })
                                }) { Icon(Icons.Default.Delete, contentDescription = s.deleteLabel) }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(mem.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("PREFERENCE") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Personal Memory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("PREFERENCE", "GOAL", "HABIT", "NOTE", "KNOWLEDGE")) { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("What should NEXORA remember?") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        mainViewModel.runRepoAction({ mainViewModel.coreRepo.addMemory(title, content, type, false) })
                        showAddDialog = false
                    }
                }) { Text(s.saveLabel) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(s.cancelLabel) } }
        )
    }
}

@Composable
fun MeetingIntelligenceScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val toolAiState by aiViewModel.toolAiState.collectAsStateWithLifecycle()

    var transcript by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    DisposableEffect(Unit) { onDispose { recognizer?.destroy() } }

    fun startSpeechCapture() {
        val rec = recognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        rec.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) { isRecording = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() { isRecording = false }
            override fun onError(p0: Int) { isRecording = false }
            override fun onResults(res: Bundle?) {
                isRecording = false
                val spoken = res?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    transcript = if (transcript.isBlank()) spoken else "$transcript\n$spoken"
                }
            }
            override fun onPartialResults(p0: Bundle?) {}
            override fun onEvent(p0: Int, p1: Bundle?) {}
        })
        rec.startListening(intent)
    }

    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startSpeechCapture()
    }

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.MEETING_INTELLIGENCE.titleBn else ToolModule.MEETING_INTELLIGENCE.titleEn,
                language = language,
                onBack = onBack,
                onToggleLanguage = { mainViewModel.toggleLanguage() }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = transcript,
                        onValueChange = { transcript = it },
                        label = { Text(if (language == AppLanguage.BN) "মিটিং বা ক্লাস লেকচারের ট্রান্সক্রিপ্ট / নোট" else "Meeting or Lecture Transcript / Notes") },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (ok) startSpeechCapture() else micPerm.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = if (isRecording) CrimsonSos else CyanPulse)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRecording) "Listening..." else "Record Voice")
                        }
                        Button(
                            onClick = {
                                if (transcript.isNotBlank()) {
                                    aiViewModel.runToolPrompt(
                                        "Create a structured Meeting/Lecture Intelligence report with: 1) Executive Summary, 2) Key Study/Discussion Notes, and 3) Actionable Tasks list from:\n$transcript",
                                        language
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (language == AppLanguage.BN) "সারাংশ ও নোট" else "Summarize & Tasks")
                        }
                    }
                }
                item {
                    AiResponseCard(
                        state = toolAiState,
                        language = language,
                        onSaveDoc = { txt ->
                            mainViewModel.runRepoAction(
                                { mainViewModel.coreRepo.saveDocument("Meeting/Lecture Notes", "MEETING", txt) },
                                "Saved meeting notes"
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FamilySpaceScreen(
    mainViewModel: NexoraMainViewModel,
    onBack: () -> Unit
) {
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val spacesState by mainViewModel.familySpaces.collectAsStateWithLifecycle()
    val activeSpace by mainViewModel.activeFamilySpace.collectAsStateWithLifecycle()
    val itemsState by mainViewModel.activeFamilyItems.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    val spaces = (spacesState as? UiState.Success)?.data.orEmpty()
    val familyItems = (itemsState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.FAMILY_SPACE.titleBn else ToolModule.FAMILY_SPACE.titleEn,
                language = language,
                onBack = onBack,
                onToggleLanguage = { mainViewModel.toggleLanguage() }
            )
        },
        floatingActionButton = {
            if (activeSpace != null) {
                FloatingActionButton(
                    onClick = { showAddItemDialog = true },
                    containerColor = CyanPulse,
                    contentColor = Color.White
                ) { Icon(Icons.Default.Add, contentDescription = "Add Family Item") }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { showCreateDialog = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Groups, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == AppLanguage.BN) "ফ্যামিলি তৈরি" else "Create Family")
                        }
                        OutlinedButton(onClick = { showJoinDialog = true }, modifier = Modifier.weight(1f)) {
                            Text(if (language == AppLanguage.BN) "কোড দিয়ে যোগ দিন" else "Join via Code")
                        }
                    }
                }

                if (spaces.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(spaces, key = { it.id }) { sp ->
                                FilterChip(
                                    selected = sp.id == activeSpace?.id,
                                    onClick = { mainViewModel.selectFamilySpace(sp.id) },
                                    label = { Text("${sp.name} (${sp.inviteCode})") }
                                )
                            }
                        }
                    }
                }

                if (activeSpace != null) {
                    item {
                        StatusPillBadge("Invite Code: ${activeSpace!!.inviteCode} • Members: ${activeSpace!!.memberUids.size}", EmeraldLife)
                    }
                    items(familyItems, key = { it.id }) { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = item.completed,
                                    onCheckedChange = { checked ->
                                        mainViewModel.runRepoAction({
                                            mainViewModel.modulesRepo.toggleFamilyItem(activeSpace!!.id, item.id, checked)
                                        })
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        StatusPillBadge(item.itemType, CyanPulse)
                                        if (item.details.isNotBlank()) {
                                            Text(item.details, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("FAM2026") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Family Space") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Family Name") }, singleLine = true)
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Invite Code (4-20 chars)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank() && code.length >= 4) {
                        mainViewModel.runRepoAction({ mainViewModel.modulesRepo.createFamilySpace(name, code) })
                        showCreateDialog = false
                    }
                }) { Text(s.saveLabel) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text(s.cancelLabel) } }
        )
    }

    if (showJoinDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Family Space") },
            text = { OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Enter Invite Code") }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    if (code.length >= 4) {
                        mainViewModel.runRepoAction({ mainViewModel.modulesRepo.joinFamilySpace(code) })
                        showJoinDialog = false
                    }
                }) { Text("Join") }
            },
            dismissButton = { TextButton(onClick = { showJoinDialog = false }) { Text(s.cancelLabel) } }
        )
    }

    if (showAddItemDialog && activeSpace != null) {
        var title by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("SHOPPING") }
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add Shared Family Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("CALENDAR", "SHOPPING", "REMINDER", "EMERGENCY")) { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        mainViewModel.runRepoAction({
                            mainViewModel.modulesRepo.addFamilyItem(activeSpace!!, title, type, details)
                        })
                        showAddItemDialog = false
                    }
                }) { Text(s.saveLabel) }
            },
            dismissButton = { TextButton(onClick = { showAddItemDialog = false }) { Text(s.cancelLabel) } }
        )
    }
}

@Composable
fun ProfileScreen(
    mainViewModel: NexoraMainViewModel,
    onOpenMemories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val darkTheme by mainViewModel.darkTheme.collectAsStateWithLifecycle()
    val offlineSmartMode by mainViewModel.offlineSmartMode.collectAsStateWithLifecycle()
    val profileState by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    var showDeleteConfirm by remember { mutableStateOf(false) }
    val profile = (profileState as? UiState.Success)?.data
    val currentTier = profile?.planTier ?: "FREE"

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(profile?.displayName ?: "NEXORA User", style = MaterialTheme.typography.headlineSmall)
                        if (!profile?.email.isNullOrBlank()) {
                            Text(profile!!.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusPillBadge("Plan: $currentTier", CyanPulse)
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(s.languageLabel, style = MaterialTheme.typography.titleSmall)
                            FilterChip(
                                selected = language == AppLanguage.BN,
                                onClick = { mainViewModel.toggleLanguage() },
                                label = { Text(if (language == AppLanguage.BN) "বাংলা (BN)" else "English (EN)") }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(s.themeLabel, style = MaterialTheme.typography.titleSmall)
                            Switch(checked = darkTheme, onCheckedChange = { mainViewModel.setDarkTheme(it) })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (language == AppLanguage.BN) "অফলাইন স্মার্ট মোড" else "Offline Smart Mode", style = MaterialTheme.typography.titleSmall)
                            Switch(checked = offlineSmartMode, onCheckedChange = { mainViewModel.setOfflineSmartMode(it) })
                        }
                        OutlinedButton(onClick = onOpenMemories, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Psychology, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == AppLanguage.BN) "পার্সোনাল মেমোরি ও প্রাইভেসি দেখুন" else "Manage Personal Memory & Digital Twin")
                        }
                    }
                }
            }

            // Freemium Plans
            item {
                Text(s.subscriptionPlansTitle, style = MaterialTheme.typography.titleMedium)
            }
            val plans = listOf(
                Triple("FREE", "Basic AI Chat, Search & Core Life Tools", "৳0"),
                Triple("PLUS", "Enhanced Gemini, Document AI & Study Center", "৳299/mo"),
                Triple("PRO", "Advanced Agent Mode, Voice & Vision AI", "৳699/mo"),
                Triple("FAMILY", "Shared Family AI Space (Up to 10 Members)", "৳999/mo"),
                Triple("BUSINESS", "Team Workspace & Enterprise API", "৳2,499/mo")
            )
            items(plans) { (tier, desc, price) ->
                val selected = currentTier == tier
                Card(
                    onClick = { mainViewModel.selectPlanTier(tier) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (selected) IndigoNeural else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = if (selected) CyanPulse else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$tier • $price", style = MaterialTheme.typography.titleSmall)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (selected) StatusPillBadge("ACTIVE", EmeraldLife)
                    }
                }
            }

            // Account Actions
            item {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonSos),
                    border = BorderStroke(1.dp, CrimsonSos),
                    modifier = Modifier.fillMaxWidth().testTag("delete_all_data_btn")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(s.deleteAllDataLabel)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { signOutUser(context, scope) },
                    modifier = Modifier.fillMaxWidth().testTag("sign_out_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(s.signOutLabel)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(s.deleteAllDataLabel) },
            text = {
                Text(
                    if (language == AppLanguage.BN) {
                        "এতে আপনার সকল কাজ, খরচ, স্বাস্থ্য রেকর্ড, মেমোরি ও চ্যাট ইতিহাস স্থায়ীভাবে মুছে যাবে। নিশ্চিত?"
                    } else {
                        "This permanently deletes all your tasks, transactions, health records, memories, documents, and chat history. Continue?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        mainViewModel.runRepoAction({ mainViewModel.coreRepo.deleteAllUserData() }, "All personal data deleted.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonSos)
                ) { Text(s.deleteLabel, color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(s.cancelLabel) } }
        )
    }
}

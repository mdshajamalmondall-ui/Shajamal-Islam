package com.example.ui.screens.modules

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpertMode
import com.example.data.model.ToolModule
import com.example.data.model.UiState
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
import com.example.ui.viewmodel.AiBrainViewModel
import com.example.ui.viewmodel.NexoraMainViewModel
import com.example.util.NexoraTtsHelper
import com.example.util.loadBitmapFromUri

@Composable
fun HealthAssistantScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val healthState by mainViewModel.healthRecords.collectAsStateWithLifecycle()
    val memoriesState by mainViewModel.memories.collectAsStateWithLifecycle()
    val toolAiState by aiViewModel.toolAiState.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    var healthQuery by remember { mutableStateOf("") }
    var reportBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val tts = remember { NexoraTtsHelper(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) reportBitmap = loadBitmapFromUri(context, uri)
    }

    val records = (healthState as? UiState.Success)?.data.orEmpty()
    val memories = (memoriesState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.HEALTH_ASSISTANT.titleBn else ToolModule.HEALTH_ASSISTANT.titleEn,
                language = language,
                onBack = onBack,
                onToggleLanguage = { mainViewModel.toggleLanguage() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldLife,
                contentColor = Color.White,
                modifier = Modifier.testTag("health_add_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Medicine or Health Record")
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AmberAlert.copy(alpha = 0.14f)),
                        border = BorderStroke(1.dp, AmberAlert.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = AmberAlert)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = s.healthDisclaimer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Medical Report / Medicine AI Analyzer
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (language == AppLanguage.BN) "মেডিকেল রিপোর্ট ও ওষুধ বিশ্লেষণ" else "Medical Report & Medicine Info AI",
                                style = MaterialTheme.typography.titleMedium
                            )
                            OutlinedTextField(
                                value = healthQuery,
                                onValueChange = { healthQuery = it },
                                placeholder = {
                                    Text(
                                        if (language == AppLanguage.BN) "ওষুধের নাম বা রিপোর্ট সম্পর্কে লিখুন..."
                                        else "Ask about a medicine, symptom, or attach a lab report..."
                                    )
                                },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (reportBitmap != null) {
                                Image(
                                    bitmap = reportBitmap!!.asImageBitmap(),
                                    contentDescription = "Medical report",
                                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (language == AppLanguage.BN) "রিপোর্ট ছবি" else "Report Photo")
                                }
                                Button(
                                    onClick = {
                                        val prompt = healthQuery.ifBlank {
                                            "Explain this medical report or prescription clearly in simple language, highlighting key values and recommending doctor consultation if needed."
                                        }
                                        aiViewModel.runToolPrompt(prompt, language, ExpertMode.DOCTOR_INFO, memories, reportBitmap)
                                    },
                                    modifier = Modifier.weight(1f).testTag("health_analyze_btn")
                                ) {
                                    Text(if (language == AppLanguage.BN) "বিশ্লেষণ করুন" else "Analyze")
                                }
                            }
                        }
                    }
                }

                item {
                    AiResponseCard(
                        state = toolAiState,
                        language = language,
                        onSpeak = { tts.speak(it, language.localeTag) },
                        onSaveDoc = { txt ->
                            mainViewModel.runRepoAction(
                                { mainViewModel.coreRepo.saveDocument(healthQuery.ifBlank { "Health Analysis" }, "SUMMARY", txt) },
                                "Saved"
                            )
                        }
                    )
                }

                // Emergency First Aid Quick Guide
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrimsonSos.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, CrimsonSos.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MedicalServices, contentDescription = null, tint = CrimsonSos)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "জরুরি ফার্স্ট এইড গাইড" else "Emergency First Aid Guide",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CrimsonSos
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (language == AppLanguage.BN) {
                                    "• পোড়া: ১৫-২০ মিনিট পরিষ্কার ঠান্ডা পানি ঢালুন (বরফ বা টুথপেস্ট দেবেন না)\n• রক্তপাত: পরিষ্কার কাপড় দিয়ে সরাসরি চাপ দিন\n• অজ্ঞান: শ্বাস-প্রশ্বাস পরীক্ষা করুন ও ৯৯৯ নম্বরে কল করুন"
                                } else {
                                    "• Burns: Cool under clean running water 15-20 mins (never ice or toothpaste)\n• Bleeding: Apply firm direct pressure with clean cloth\n• Unconscious: Check breathing & call emergency 999"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Medicine Reminders & Records
                item {
                    Text(
                        text = if (language == AppLanguage.BN) "ওষুধ রিমাইন্ডার ও হেলথ রেকর্ড" else "Medicine Reminders & Health Records",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(records, key = { it.id }) { rec ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (rec.recordType == "MEDICINE") {
                                Checkbox(
                                    checked = rec.takenToday,
                                    onCheckedChange = { taken ->
                                        mainViewModel.runRepoAction({ mainViewModel.modulesRepo.toggleMedicineTaken(rec.id, taken) })
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.title, style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusPillBadge(rec.recordType, EmeraldLife)
                                    Text(
                                        text = "${rec.dosageOrValue} • ${rec.scheduleTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = {
                                mainViewModel.runRepoAction({ mainViewModel.coreRepo.deleteUserDoc("health_records", rec.id) })
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = s.deleteLabel)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var dosage by remember { mutableStateOf("") }
        var schedule by remember { mutableStateOf("After Meal • 9:00 PM") }
        var recordType by remember { mutableStateOf("MEDICINE") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (language == AppLanguage.BN) "ওষুধ বা হেলথ রেকর্ড যোগ করুন" else "Add Medicine / Health Record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("MEDICINE", "CHECKUP", "REPORT").forEach { t ->
                            FilterChip(selected = recordType == t, onClick = { recordType = t }, label = { Text(t) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(value = dosage, onValueChange = { dosage = it }, label = { Text("Dosage / Value (e.g. 500mg)") }, singleLine = true)
                    OutlinedTextField(value = schedule, onValueChange = { schedule = it }, label = { Text("Schedule / Date") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        mainViewModel.runRepoAction({
                            mainViewModel.modulesRepo.addHealthRecord(title, recordType, dosage, schedule)
                        })
                        showAddDialog = false
                    }
                }) { Text(s.saveLabel) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(s.cancelLabel) } }
        )
    }
}

@Composable
fun EducationCenterScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val memoriesState by mainViewModel.memories.collectAsStateWithLifecycle()
    val toolAiState by aiViewModel.toolAiState.collectAsStateWithLifecycle()
    val mcqList by aiViewModel.mcqQuiz.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    var selectedTab by remember { mutableIntStateOf(0) }
    var studyInput by remember { mutableStateOf("") }
    var questionBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var mcqTopic by remember { mutableStateOf("") }
    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() }
    val tts = remember { NexoraTtsHelper(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) questionBitmap = loadBitmapFromUri(context, uri)
    }

    val memories = (memoriesState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.EDUCATION_CENTER.titleBn else ToolModule.EDUCATION_CENTER.titleEn,
                language = language,
                onBack = onBack,
                onToggleLanguage = { mainViewModel.toggleLanguage() }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(if (language == AppLanguage.BN) "AI শিক্ষক ও সমাধান" else "Tutor & Photo") },
                        icon = { Icon(Icons.Default.School, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("MCQ Test") },
                        icon = { Icon(Icons.Default.Quiz, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(if (language == AppLanguage.BN) "Spoken English" else "Speaking & Plan") },
                        icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) }
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            item {
                                OutlinedTextField(
                                    value = studyInput,
                                    onValueChange = { studyInput = it },
                                    label = { Text(if (language == AppLanguage.BN) "প্রশ্ন বা নোটের বিষয় লিখুন" else "Ask a question or paste study notes") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (questionBitmap != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Image(
                                        bitmap = questionBitmap!!.asImageBitmap(),
                                        contentDescription = "Question photo",
                                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(MaterialTheme.shapes.medium),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (language == AppLanguage.BN) "প্রশ্নের ছবি" else "Photo Solve")
                                    }
                                    Button(
                                        onClick = {
                                            val p = studyInput.ifBlank { "Solve the problem shown in the image step by step." }
                                            aiViewModel.runToolPrompt(p, language, ExpertMode.TEACHER, memories, questionBitmap)
                                        },
                                        modifier = Modifier.weight(1f).testTag("edu_solve_btn")
                                    ) {
                                        Text(if (language == AppLanguage.BN) "সমাধান ও নোট" else "Explain & Notes")
                                    }
                                }
                            }
                            item {
                                AiResponseCard(
                                    state = toolAiState,
                                    language = language,
                                    onSpeak = { tts.speak(it, language.localeTag) },
                                    onSaveDoc = { txt ->
                                        mainViewModel.runRepoAction(
                                            { mainViewModel.coreRepo.saveDocument(studyInput.ifBlank { "Study Note" }, "STUDY_NOTE", txt) },
                                            "Saved study note"
                                        )
                                    }
                                )
                            }
                        }

                        1 -> {
                            item {
                                OutlinedTextField(
                                    value = mcqTopic,
                                    onValueChange = { mcqTopic = it },
                                    label = { Text(if (language == AppLanguage.BN) "MCQ পরীক্ষার বিষয় (যেমন: বাংলাদেশের ইতিহাস, Biology)" else "Quiz Topic (e.g., Physics, BCS, Nursing)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (mcqTopic.isNotBlank()) {
                                            selectedAnswers.clear()
                                            aiViewModel.generateMcqQuiz(mcqTopic, 5, language)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("edu_generate_mcq_btn")
                                ) {
                                    Text(if (language == AppLanguage.BN) "৫টি MCQ মডেল টেস্ট তৈরি করুন" else "Generate 5-Question Model Test")
                                }
                            }
                            if (mcqList.isNotEmpty()) {
                                val answeredCount = selectedAnswers.size
                                val score = mcqList.indices.count { selectedAnswers[it] == mcqList[it].correctIndex }
                                item {
                                    StatusPillBadge("Score: $score / ${mcqList.size} ($answeredCount answered)", IndigoNeural)
                                }
                                items(mcqList.indices.toList()) { idx ->
                                    val q = mcqList[idx]
                                    val chosen = selectedAnswers[idx]
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("${idx + 1}. ${q.question}", style = MaterialTheme.typography.titleSmall)
                                            q.options.forEachIndexed { optIdx, optText ->
                                                val isChosen = chosen == optIdx
                                                val isCorrect = optIdx == q.correctIndex
                                                val tint = when {
                                                    chosen == null -> MaterialTheme.colorScheme.outline
                                                    isCorrect -> EmeraldLife
                                                    isChosen -> CrimsonSos
                                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                                }
                                                OutlinedButton(
                                                    onClick = { selectedAnswers[idx] = optIdx },
                                                    border = BorderStroke(1.dp, tint),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(optText, modifier = Modifier.fillMaxWidth())
                                                }
                                            }
                                            if (chosen != null && q.explanation.isNotBlank()) {
                                                Text(
                                                    text = q.explanation,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = EmeraldLife
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            item {
                                var practiceText by remember { mutableStateOf("") }
                                OutlinedTextField(
                                    value = practiceText,
                                    onValueChange = { practiceText = it },
                                    label = { Text(if (language == AppLanguage.BN) "ইংরেজি বাক্য লিখুন বা লার্নিং প্ল্যানের লক্ষ্য দিন" else "Write English sentence to practice or learning goal") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            aiViewModel.runToolPrompt(
                                                "Act as a friendly Spoken English Coach for a Bangladeshi learner. Correct grammar gently, give a natural reply, and provide 1 pronunciation tip for: $practiceText",
                                                language, ExpertMode.TEACHER, memories
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Spoken Practice") }
                                    OutlinedButton(
                                        onClick = {
                                            aiViewModel.runToolPrompt(
                                                "Create a structured 4-week Personal Learning Plan with daily milestones for: ${practiceText.ifBlank { "IELTS & Career English" }}",
                                                language, ExpertMode.TEACHER, memories
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Learning Plan") }
                                }
                            }
                            item {
                                AiResponseCard(
                                    state = toolAiState,
                                    language = language,
                                    onSpeak = { tts.speak(it, "en-US") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AiResponseState
import com.example.data.model.ExpertMode
import com.example.data.model.UiState
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import com.example.ui.components.NexoraGlowOrb
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldLife
import com.example.ui.theme.IndigoNeural
import com.example.ui.theme.VioletExpert
import com.example.ui.viewmodel.AiBrainViewModel
import com.example.ui.viewmodel.NexoraMainViewModel
import com.example.util.NexoraTtsHelper
import com.example.util.formatBdt
import com.example.util.loadBitmapFromUri

@Composable
fun AiBrainScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    initialPrompt: String = "",
    startVoiceOnEntry: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val memoriesState by mainViewModel.memories.collectAsStateWithLifecycle()
    val messages by aiViewModel.activeMessages.collectAsStateWithLifecycle()
    val chatState by aiViewModel.chatState.collectAsStateWithLifecycle()
    val pendingProposal by aiViewModel.pendingProposal.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    var inputText by remember(initialPrompt) { mutableStateOf(initialPrompt) }
    var agentMode by remember { mutableStateOf(true) }
    var selectedExpert by remember { mutableStateOf(ExpertMode.GENERAL) }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }

    val memories = (memoriesState as? UiState.Success)?.data.orEmpty()
    val tts = remember { NexoraTtsHelper(context) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.shutdown()
            speechRecognizer?.destroy()
        }
    }

    fun startVoiceRecognition() {
        val recognizer = speechRecognizer ?: run {
            voiceError = if (language == AppLanguage.BN) {
                "এই ডিভাইসে স্পিচ রিকগনিশন উপলব্ধ নয়।"
            } else {
                "Speech recognition service unavailable on this device."
            }
            return
        }
        voiceError = null
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                voiceError = if (language == AppLanguage.BN) "ভয়েস বোঝা যায়নি (কোড $error)" else "Voice input error ($error)"
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    inputText = text
                    aiViewModel.sendChatMessage(
                        prompt = text,
                        language = language,
                        expertMode = selectedExpert,
                        memories = memories,
                        agentMode = agentMode,
                        bitmap = attachedBitmap
                    )
                    attachedBitmap = null
                    inputText = ""
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) inputText = partial
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceRecognition()
        else voiceError = if (language == AppLanguage.BN) "ভয়েস অ্যাসিস্ট্যান্টের জন্য মাইক্রোফোন অনুমতি প্রয়োজন।" else "Microphone permission required for Voice Assistant."
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) attachedBitmap = loadBitmapFromUri(context, uri)
    }

    LaunchedEffect(startVoiceOnEntry) {
        if (startVoiceOnEntry) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (granted) startVoiceRecognition()
            else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            // Header with Orb, Agent Mode Toggle, Memory badge & New Chat
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NexoraGlowOrb(size = 42.dp, isActive = isListening || chatState is AiResponseState.Loading)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = s.aiBrainTitle, style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusPillBadge(
                                        text = "${memories.size} Memories",
                                        accent = VioletExpert
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { tts.stop() },
                                modifier = Modifier.testTag("ai_stop_tts_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = s.stopSpeaking)
                            }
                            IconButton(
                                onClick = { aiViewModel.selectConversation(null) },
                                modifier = Modifier.testTag("ai_new_chat_btn")
                            ) {
                                Icon(Icons.Default.AddComment, contentDescription = "New Conversation")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanPulse, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = s.agentModeLabel, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = s.agentModeDesc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = agentMode,
                            onCheckedChange = { agentMode = it },
                            modifier = Modifier.testTag("agent_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ExpertMode.entries, key = { it.id }) { mode ->
                            FilterChip(
                                selected = selectedExpert == mode,
                                onClick = { selectedExpert = mode },
                                label = { Text(if (language == AppLanguage.BN) mode.titleBn else mode.titleEn) }
                            )
                        }
                    }
                }
            }

            // Messages Stream
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty() && chatState is AiResponseState.Idle) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = CyanPulse)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == AppLanguage.BN) "কীভাবে শুরু করবেন?" else "Try Asking NEXORA Brain",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                val example = if (language == AppLanguage.BN) {
                                    "\"আগামীকাল সকাল ৭টায় ব্যায়াম রিমাইন্ডার দাও, ৩৫০ টাকা দুপুরের খাবার খরচ লেখো এবং মনে রাখো আমি গ্রিন টি পছন্দ করি\""
                                } else {
                                    "\"Remind me to study at 7 AM tomorrow, log 350 BDT lunch expense, and remember I prefer concise Bangla summaries\""
                                }
                                Text(text = example, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier.widthIn(max = 520.dp),
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isUser) 18.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 18.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) IndigoNeural else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                if (!isUser) {
                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                        IconButton(
                                            onClick = { tts.speak(msg.text, language.localeTag) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = s.speakResponse,
                                                tint = CyanPulse,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Streaming / Loading / Error Bubble
                if (chatState is AiResponseState.Loading || chatState is AiResponseState.Streaming || chatState is AiResponseState.Error) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (chatState is AiResponseState.Error) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                when (val st = chatState) {
                                    is AiResponseState.Loading -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(s.loadingLabel, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    is AiResponseState.Streaming -> {
                                        Text(text = st.text, style = MaterialTheme.typography.bodyLarge)
                                    }
                                    is AiResponseState.Error -> {
                                        Text(
                                            text = st.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }

                // AI Agent Mode Permission Confirmation Card
                if (pendingProposal != null) {
                    val prop = pendingProposal!!
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("agent_permission_card"),
                            colors = CardDefaults.cardColors(containerColor = EmeraldLife.copy(alpha = 0.12f)),
                            border = BorderStroke(1.5.dp, EmeraldLife)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldLife)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = s.agentConfirmTitle, style = MaterialTheme.typography.titleSmall, color = EmeraldLife)
                                }
                                if (prop.summaryDescription.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = prop.summaryDescription, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                prop.tasks.forEach { t ->
                                    Text("• Task: ${t.title} (${t.dueDateLabel} • ${t.timeLabel})", style = MaterialTheme.typography.bodySmall)
                                }
                                prop.expenses.forEach { e ->
                                    Text("• ${e.txType}: ${e.title} — ${formatBdt(e.amount)}", style = MaterialTheme.typography.bodySmall)
                                }
                                prop.memories.forEach { m ->
                                    Text("• Memory: ${m.title} — ${m.content}", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            aiViewModel.executeApprovedProposal { msg ->
                                                mainViewModel.showBanner(msg)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldLife),
                                        modifier = Modifier.testTag("agent_approve_btn")
                                    ) {
                                        Text(s.approveExecute, color = Color.White)
                                    }
                                    OutlinedButton(
                                        onClick = { aiViewModel.dismissProposal() },
                                        modifier = Modifier.testTag("agent_cancel_btn")
                                    ) {
                                        Text(s.cancelAction)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Voice status & Attachment preview
            AnimatedVisibility(visible = isListening || voiceError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CyanPulse.copy(alpha = 0.14f))
                ) {
                    Text(
                        text = if (isListening) s.listeningNow else (voiceError ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanPulse,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (attachedBitmap != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .height(80.dp)
                        .width(120.dp)
                ) {
                    Image(
                        bitmap = attachedBitmap!!.asImageBitmap(),
                        contentDescription = "Attached image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { attachedBitmap = null },
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                    }
                }
            }

            // Bottom Composer Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.testTag("ai_attach_image_btn")
                ) {
                    Icon(Icons.Default.Image, contentDescription = s.attachImage)
                }
                IconButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (granted) startVoiceRecognition()
                        else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    modifier = Modifier.testTag("ai_voice_mic_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = s.tapToSpeak,
                        tint = if (isListening) EmeraldLife else CyanPulse
                    )
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(s.askAnythingHint) },
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val msg = inputText.trim()
                        val bmp = attachedBitmap
                        inputText = ""
                        attachedBitmap = null
                        aiViewModel.sendChatMessage(
                            prompt = msg,
                            language = language,
                            expertMode = selectedExpert,
                            memories = memories,
                            agentMode = agentMode,
                            bitmap = bmp
                        )
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(CyanPulse, IndigoNeural)))
                        .testTag("ai_send_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

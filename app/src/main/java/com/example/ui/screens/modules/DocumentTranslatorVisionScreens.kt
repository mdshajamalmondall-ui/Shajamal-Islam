package com.example.ui.screens.modules

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpertMode
import com.example.data.model.ToolModule
import com.example.data.model.UiState
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import com.example.ui.components.AiResponseCard
import com.example.ui.components.NexoraModuleTopBar
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.CyanPulse
import com.example.ui.viewmodel.AiBrainViewModel
import com.example.ui.viewmodel.NexoraMainViewModel
import com.example.util.NexoraTtsHelper
import com.example.util.loadBitmapFromUri
import com.example.util.readTextFromUri

@Composable
fun DocumentAiScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val docsState by mainViewModel.savedDocuments.collectAsStateWithLifecycle()
    val toolAiState by aiViewModel.toolAiState.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    var docText by remember { mutableStateOf("") }
    var docBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var docAction by remember { mutableStateOf("SUMMARIZE") }
    val tts = remember { NexoraTtsHelper(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val extractedText = readTextFromUri(context, uri)
            if (!extractedText.isNullOrBlank()) {
                docText = extractedText
                docBitmap = null
            } else {
                docBitmap = loadBitmapFromUri(context, uri)
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) docBitmap = loadBitmapFromUri(context, uri)
    }

    val savedDocs = (docsState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.DOCUMENT_AI.titleBn else ToolModule.DOCUMENT_AI.titleEn,
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("SUMMARIZE", "OCR_TEXT", "TRANSLATE_BN_EN", "CV_APPLICATION")) { act ->
                            FilterChip(
                                selected = docAction == act,
                                onClick = { docAction = act },
                                label = { Text(act.replace("_", " ")) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = docText,
                        onValueChange = { docText = it },
                        label = {
                            Text(
                                if (language == AppLanguage.BN) "ডকুমেন্টের লেখা পেস্ট করুন বা সিভি/আবেদনের তথ্য দিন"
                                else "Paste document text, or enter details for CV / Formal Application"
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (docBitmap != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = docBitmap!!.asImageBitmap(),
                            contentDescription = "Document page or scan",
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { filePicker.launch(arrayOf("application/pdf", "text/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF / File")
                        }
                        OutlinedButton(
                            onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language == AppLanguage.BN) "ডকুমেন্ট স্ক্যান" else "Scan Photo")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val prompt = when (docAction) {
                                "OCR_TEXT" -> "Extract all text accurately from this document/image and format it cleanly. $docText"
                                "TRANSLATE_BN_EN" -> "Translate this document cleanly between Bangla and English while preserving professional formatting:\n$docText"
                                "CV_APPLICATION" -> "Write a polished professional CV or formal application letter based on these details:\n$docText"
                                else -> "Read and summarize this document with key bullet points and action items:\n$docText"
                            }
                            aiViewModel.runToolPrompt(prompt, language, ExpertMode.CAREER, emptyList(), docBitmap)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("doc_process_btn")
                    ) {
                        Text(if (language == AppLanguage.BN) "AI দিয়ে প্রসেস করুন" else "Process with Document AI")
                    }
                }

                item {
                    AiResponseCard(
                        state = toolAiState,
                        language = language,
                        onSpeak = { tts.speak(it, language.localeTag) },
                        onSaveDoc = { txt ->
                            val type = if (docAction == "CV_APPLICATION") "CV" else if (docAction == "TRANSLATE_BN_EN") "TRANSLATION" else "SUMMARY"
                            mainViewModel.runRepoAction(
                                { mainViewModel.coreRepo.saveDocument(docText.take(40).ifBlank { docAction }, type, txt) },
                                "Document saved"
                            )
                        }
                    )
                }

                item {
                    Text(if (language == AppLanguage.BN) "সংরক্ষিত ডকুমেন্ট" else "Saved Documents", style = MaterialTheme.typography.titleMedium)
                }

                items(savedDocs, key = { it.id }) { doc ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(doc.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                StatusPillBadge(doc.docType, CyanPulse)
                                IconButton(onClick = {
                                    mainViewModel.runRepoAction({ mainViewModel.coreRepo.deleteSavedDocument(doc.id) })
                                }) { Icon(Icons.Default.Delete, contentDescription = s.deleteLabel) }
                            }
                            Text(doc.content, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TranslatorScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val toolAiState by aiViewModel.toolAiState.collectAsStateWithLifecycle()

    var sourceText by remember { mutableStateOf("") }
    var targetLang by remember { mutableStateOf("English") }
    var translateBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isListening by remember { mutableStateOf(false) }

    val tts = remember { NexoraTtsHelper(context) }
    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    DisposableEffect(Unit) {
        onDispose {
            tts.shutdown()
            recognizer?.destroy()
        }
    }

    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && recognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeTag)
            }
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p0: Bundle?) { isListening = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(p0: Float) {}
                override fun onBufferReceived(p0: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }
                override fun onError(p0: Int) { isListening = false }
                override fun onResults(res: Bundle?) {
                    isListening = false
                    val spoken = res?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!spoken.isNullOrBlank()) sourceText = spoken
                }
                override fun onPartialResults(p0: Bundle?) {}
                override fun onEvent(p0: Int, p1: Bundle?) {}
            })
            recognizer.startListening(intent)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) translateBitmap = loadBitmapFromUri(context, uri)
    }

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.TRANSLATOR.titleBn else ToolModule.TRANSLATOR.titleEn,
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Bangla", "English", "Arabic", "Hindi", "Japanese", "Spanish")) { lang ->
                            FilterChip(selected = targetLang == lang, onClick = { targetLang = lang }, label = { Text("→ $lang") })
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = sourceText,
                        onValueChange = { sourceText = it },
                        label = { Text(if (language == AppLanguage.BN) "যেকোনো লেখা লিখুন বা বলুন" else "Enter text or speak to translate") },
                        minLines = 3,
                        trailingIcon = {
                            IconButton(onClick = {
                                val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (ok) micPerm.launch(Manifest.permission.RECORD_AUDIO) else micPerm.launch(Manifest.permission.RECORD_AUDIO)
                            }) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Translate", tint = if (isListening) CyanPulse else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (translateBitmap != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = translateBitmap!!.asImageBitmap(),
                            contentDescription = "Image to translate",
                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Photo Text")
                        }
                        Button(
                            onClick = {
                                val p = "Translate accurately into $targetLang, also provide pronunciation phonetics and grammar notes if helpful:\n$sourceText"
                                aiViewModel.runToolPrompt(p, language, ExpertMode.GENERAL, emptyList(), translateBitmap)
                            },
                            modifier = Modifier.weight(1f).testTag("translate_btn")
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Translate")
                        }
                    }
                }
                item {
                    val ttsTag = if (targetLang == "Bangla") "bn-BD" else "en-US"
                    AiResponseCard(
                        state = toolAiState,
                        language = language,
                        onSpeak = { tts.speak(it, ttsTag) }
                    )
                }
            }
        }
    }
}

@Composable
fun AiVisionScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val toolAiState by aiViewModel.toolAiState.collectAsStateWithLifecycle()

    var visionBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var visionTask by remember { mutableStateOf("IDENTIFY") }
    var customQuestion by remember { mutableStateOf("") }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) visionBitmap = loadBitmapFromUri(context, uri)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) visionBitmap = bmp
    }

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.AI_VISION.titleBn else ToolModule.AI_VISION.titleEn,
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("IDENTIFY", "READ_TEXT", "FOOD_NUTRITION", "SOLVE_PROBLEM")) { mode ->
                            FilterChip(selected = visionTask == mode, onClick = { visionTask = mode }, label = { Text(mode.replace("_", " ")) })
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == AppLanguage.BN) "গ্যালারি" else "Pick Photo")
                        }
                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == AppLanguage.BN) "ক্যামেরা" else "Take Photo")
                        }
                    }
                }
                if (visionBitmap != null) {
                    item {
                        Image(
                            bitmap = visionBitmap!!.asImageBitmap(),
                            contentDescription = "Vision input",
                            modifier = Modifier.fillMaxWidth().height(210.dp).clip(MaterialTheme.shapes.large),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = customQuestion,
                        onValueChange = { customQuestion = it },
                        label = { Text(if (language == AppLanguage.BN) "ছবি সম্পর্কে নির্দিষ্ট প্রশ্ন (ঐচ্ছিক)" else "Specific question about photo (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (visionBitmap != null) {
                                val basePrompt = when (visionTask) {
                                    "READ_TEXT" -> "Read and transcribe all visible text in this image accurately."
                                    "FOOD_NUTRITION" -> "Identify the food and ingredients in this image, and estimate approximate nutritional info and allergens."
                                    "SOLVE_PROBLEM" -> "Analyze the visual problem or diagram in this image and solve it step-by-step."
                                    else -> "Identify the main object, scene, and key details visible in this image."
                                }
                                val finalPrompt = if (customQuestion.isNotBlank()) "$basePrompt\nUser question: $customQuestion" else basePrompt
                                aiViewModel.runToolPrompt(finalPrompt, language, ExpertMode.GENERAL, emptyList(), visionBitmap)
                            }
                        },
                        enabled = visionBitmap != null,
                        modifier = Modifier.fillMaxWidth().testTag("vision_analyze_btn")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (language == AppLanguage.BN) "ছবি বিশ্লেষণ করুন" else "Analyze Image")
                    }
                }
                item { AiResponseCard(state = toolAiState, language = language) }
            }
        }
    }
}

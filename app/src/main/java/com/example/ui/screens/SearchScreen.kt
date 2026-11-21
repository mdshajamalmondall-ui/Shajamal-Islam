package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AiResponseState
import com.example.data.model.ExpertMode
import com.example.data.model.UiState
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import com.example.ui.components.AiResponseCard
import com.example.ui.components.expertIconFor
import com.example.ui.viewmodel.AiBrainViewModel
import com.example.ui.viewmodel.NexoraMainViewModel
import com.example.util.NexoraTtsHelper
import com.example.util.loadBitmapFromUri

@Composable
fun SearchScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onContinueInChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val memoriesState by mainViewModel.memories.collectAsStateWithLifecycle()
    val searchState by aiViewModel.searchState.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    var query by remember { mutableStateOf("") }
    var selectedExpert by remember { mutableStateOf(ExpertMode.GENERAL) }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val tts = remember { NexoraTtsHelper(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedBitmap = loadBitmapFromUri(context, uri)
        }
    }

    val memories = (memoriesState as? UiState.Success)?.data.orEmpty()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(text = s.searchHeader, style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = if (language == AppLanguage.BN) {
                        "বাংলা বা ইংরেজিতে লিখুন, স্ক্রিনশট/ছবি দিন এবং যেকোনো এক্সপার্ট বেছে নিন"
                    } else {
                        "Search in Bangla or English, analyze screenshots/photos, and switch AI Experts"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Multi-AI Expert Selector
            item {
                Text(text = s.selectExpert, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExpertMode.entries, key = { it.id }) { mode ->
                        FilterChip(
                            selected = selectedExpert == mode,
                            onClick = { selectedExpert = mode },
                            leadingIcon = {
                                Icon(
                                    imageVector = expertIconFor(mode.id),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(if (language == AppLanguage.BN) mode.titleBn else mode.titleEn)
                            },
                            modifier = Modifier.testTag("expert_chip_${mode.id}")
                        )
                    }
                }
            }

            // Search Input + Image / Ask About Screen
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(s.askAnythingHint) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    minLines = 2,
                    maxLines = 4,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("universal_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_attach_photo_btn")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.attachImage)
                    }
                    OutlinedButton(
                        onClick = {
                            if (query.isBlank()) {
                                query = if (language == AppLanguage.BN) {
                                    "এই স্ক্রিনশট/ছবির সম্পূর্ণ বিষয়বস্তু সহজভাবে বুঝিয়ে দাও।"
                                } else {
                                    "Explain everything visible on this screen or screenshot clearly."
                                }
                            }
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ask_about_screen_btn")
                    ) {
                        Icon(Icons.Default.ScreenshotMonitor, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.askAboutScreen)
                    }
                }

                if (attachedBitmap != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            bitmap = attachedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { attachedBitmap = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove image")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        aiViewModel.runUniversalSearch(
                            query = query,
                            language = language,
                            expertMode = selectedExpert,
                            memories = memories,
                            bitmap = attachedBitmap
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("run_universal_search_btn")
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(s.searchButton)
                }
            }

            // Streaming / Result Card
            item {
                AiResponseCard(
                    state = searchState,
                    language = language,
                    onSpeak = { text -> tts.speak(text, language.localeTag) },
                    onSaveDoc = { text ->
                        mainViewModel.runRepoAction(
                            { mainViewModel.coreRepo.saveDocument(query.ifBlank { "AI Search" }, "SUMMARY", text) },
                            if (language == AppLanguage.BN) "ডকুমেন্টে সংরক্ষণ করা হয়েছে" else "Saved to Documents"
                        )
                    },
                    onRetry = {
                        aiViewModel.runUniversalSearch(query, language, selectedExpert, memories, attachedBitmap)
                    }
                )
            }

            if (searchState is AiResponseState.Success) {
                item {
                    OutlinedButton(
                        onClick = { onContinueInChat(query) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (language == AppLanguage.BN) "AI ব্রেইনে কথা চালিয়ে যান" else "Continue in Full AI Brain")
                    }
                }
            }

            // Quick Search Prompts
            item {
                val prompts = if (language == AppLanguage.BN) {
                    listOf(
                        "আজকের জন্য একটি স্বাস্থ্যকর ও প্রোডাক্টিভ রুটিন দাও",
                        "বাংলাদেশে ১৫,০০০ টাকায় ভালো স্মার্টফোনের তুলনা করো",
                        "রক্তের CBC রিপোর্টের সাধারণ মানগুলো বুঝিয়ে বলো"
                    )
                } else {
                    listOf(
                        "Compare best budget laptops under 60,000 BDT in Bangladesh",
                        "Explain IELTS speaking band 7.5 tips with examples",
                        "Create a balanced weekly Bangladeshi meal & nutrition guide"
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    prompts.forEach { suggestion ->
                        Card(
                            onClick = {
                                query = suggestion
                                aiViewModel.runUniversalSearch(suggestion, language, selectedExpert, memories, attachedBitmap)
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

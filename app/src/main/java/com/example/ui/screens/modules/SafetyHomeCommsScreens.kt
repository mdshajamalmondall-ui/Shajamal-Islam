package com.example.ui.screens.modules

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sos
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CrimsonSos
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldLife
import com.example.ui.viewmodel.AiBrainViewModel
import com.example.ui.viewmodel.NexoraMainViewModel
import com.google.android.gms.location.LocationServices

@Composable
fun EmergencySafetyScreen(
    mainViewModel: NexoraMainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val contactsState by mainViewModel.emergencyContacts.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    var showAddDialog by remember { mutableStateOf(false) }
    var locationSummary by remember { mutableStateOf<String?>(null) }

    val contacts = (contactsState as? UiState.Success)?.data.orEmpty()

    fun fetchAndShareLocation() {
        val client = LocationServices.getFusedLocationProviderClient(context)
        try {
            client.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val mapUrl = "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
                    locationSummary = "Lat: ${loc.latitude}, Lng: ${loc.longitude}"
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "EMERGENCY SOS from NEXORA AI! My live location: $mapUrl")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share SOS Location"))
                } else {
                    locationSummary = if (language == AppLanguage.BN) "লোকেশন পাওয়া যায়নি — GPS চালু করুন" else "Location unavailable — enable GPS"
                }
            }
        } catch (_: SecurityException) {
            locationSummary = "Location permission required"
        }
    }

    val locPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetchAndShareLocation()
        else locationSummary = if (language == AppLanguage.BN) "লোকেশন অনুমতি দেওয়া হয়নি" else "Location permission denied"
    }

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.EMERGENCY_SAFETY.titleBn else ToolModule.EMERGENCY_SAFETY.titleEn,
                language = language,
                onBack = onBack,
                onToggleLanguage = { mainViewModel.toggleLanguage() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CrimsonSos,
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, contentDescription = "Add Contact or Blood Donor") }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // One-Tap SOS & Live Location Share
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrimsonSos.copy(alpha = 0.14f)),
                        border = BorderStroke(1.5.dp, CrimsonSos)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sos, contentDescription = null, tint = CrimsonSos)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "ওয়ান-ট্যাপ জরুরি SOS ও লোকেশন" else "One-Tap Emergency SOS & Location",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = CrimsonSos
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:999"))
                                        context.startActivity(dialIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonSos),
                                    modifier = Modifier.weight(1f).testTag("sos_call_999_btn")
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call 999", color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        if (ok) fetchAndShareLocation()
                                        else locPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    },
                                    modifier = Modifier.weight(1f).testTag("sos_share_location_btn")
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.BN) "লোকেশন শেয়ার" else "Share GPS")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=hospital+near+me")))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.LocalHospital, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.BN) "নিকটস্থ হাসপাতাল" else "Nearby Hospitals")
                                }
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:16263")))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Health 16263")
                                }
                            }
                            if (locationSummary != null) {
                                Text(locationSummary!!, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Emergency Contacts & Blood Donor Directory
                item {
                    Text(
                        text = if (language == AppLanguage.BN) "জরুরি কন্টাক্ট ও রক্তদাতা ডিরেক্টরি" else "Emergency Contacts & Blood Donor Directory",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(contacts, key = { it.id }) { contact ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${contact.name} (${contact.relation})", style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(contact.phone, style = MaterialTheme.typography.bodySmall)
                                    if (contact.bloodDonor) {
                                        StatusPillBadge("Donor: ${contact.bloodGroup}", CrimsonSos)
                                    }
                                }
                            }
                            IconButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}")))
                            }) { Icon(Icons.Default.Call, contentDescription = "Call", tint = EmeraldLife) }
                            IconButton(onClick = {
                                mainViewModel.runRepoAction({ mainViewModel.coreRepo.deleteUserDoc("emergency_contacts", contact.id) })
                            }) { Icon(Icons.Default.Delete, contentDescription = s.deleteLabel) }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var relation by remember { mutableStateOf("Family") }
        var bloodGroup by remember { mutableStateOf("O+") }
        var isBloodDonor by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Emergency Contact / Donor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true)
                    OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("Relation") }, singleLine = true)
                    OutlinedTextField(value = bloodGroup, onValueChange = { bloodGroup = it }, label = { Text("Blood Group (e.g. A+)") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isBloodDonor, onCheckedChange = { isBloodDonor = it })
                        Text("Available Blood Donor")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank() && phone.length >= 3) {
                        mainViewModel.runRepoAction({
                            mainViewModel.modulesRepo.addEmergencyContact(name, phone, relation, bloodGroup, isBloodDonor)
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
fun SmartCommunicationScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val toolAiState by aiViewModel.toolAiState.collectAsStateWithLifecycle()

    var draftInput by remember { mutableStateOf("") }
    var commTask by remember { mutableStateOf("EMAIL") }
    var tone by remember { mutableStateOf("Professional") }

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.SMART_COMMUNICATION.titleBn else ToolModule.SMART_COMMUNICATION.titleEn,
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("EMAIL", "SOCIAL_CAPTION", "GRAMMAR_FIX", "SUMMARIZE")) { task ->
                            FilterChip(selected = commTask == task, onClick = { commTask = task }, label = { Text(task.replace("_", " ")) })
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Professional", "Friendly", "Polite Formal", "Concise")) { t ->
                            FilterChip(selected = tone == t, onClick = { tone = t }, label = { Text(t) })
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = draftInput,
                        onValueChange = { draftInput = it },
                        label = { Text(if (language == AppLanguage.BN) "ইমেইল/মেসেজের বিষয় বা খসড়া লিখুন" else "Topic or draft text") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (draftInput.isNotBlank()) {
                                val prompt = "Task: $commTask. Desired tone: $tone. Write/polish this text clearly:\n$draftInput"
                                aiViewModel.runToolPrompt(prompt, language, ExpertMode.CAREER)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (language == AppLanguage.BN) "ড্রাফট তৈরি করুন" else "Generate Draft")
                    }
                }
                item { AiResponseCard(state = toolAiState, language = language) }
                if (toolAiState is com.example.data.model.AiResponseState.Success) {
                    item {
                        val txt = (toolAiState as com.example.data.model.AiResponseState.Success).text
                        OutlinedButton(
                            onClick = {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, txt)
                                }
                                context.startActivity(Intent.createChooser(share, "Share message"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Draft")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartHomeScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onBack: () -> Unit
) {
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val devicesState by mainViewModel.smartDevices.collectAsStateWithLifecycle()
    val toolAiState by aiViewModel.toolAiState.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    var showAddDialog by remember { mutableStateOf(false) }
    val devices = (devicesState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.SMART_HOME.titleBn else ToolModule.SMART_HOME.titleEn,
                language = language,
                onBack = onBack,
                onToggleLanguage = { mainViewModel.toggleLanguage() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldLife,
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, contentDescription = "Add Smart Device") }
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
                        colors = CardDefaults.cardColors(containerColor = AmberAlert.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, AmberAlert.copy(alpha = 0.4f))
                    ) {
                        Text(text = s.smartHomeDisclaimer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }
                item {
                    Button(
                        onClick = {
                            val listDesc = devices.joinToString(", ") { "${it.name} (${it.room}, ${it.deviceType})" }
                            aiViewModel.runToolPrompt(
                                "Suggest 3 energy-saving Smart Home automation routines for these devices: ${listDesc.ifBlank { "Living room light, Bedroom AC, Main door lock" }}.",
                                language
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (language == AppLanguage.BN) "AI অটোমেশন রুটিন সাজেশন" else "Suggest AI Automation Routines")
                    }
                }
                item { AiResponseCard(state = toolAiState, language = language) }

                items(devices, key = { it.id }) { dev ->
                    var sliderLevel by remember(dev.id, dev.level) { mutableFloatStateOf(dev.level.toFloat()) }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dev.name, style = MaterialTheme.typography.titleSmall)
                                    Text("${dev.room} • ${dev.deviceType}", style = MaterialTheme.typography.labelSmall, color = CyanPulse)
                                }
                                Switch(
                                    checked = dev.powerOn,
                                    onCheckedChange = { on ->
                                        mainViewModel.runRepoAction({
                                            mainViewModel.modulesRepo.updateSmartDevice(dev.id, on, dev.level)
                                        })
                                    }
                                )
                                IconButton(onClick = {
                                    mainViewModel.runRepoAction({ mainViewModel.coreRepo.deleteUserDoc("smart_devices", dev.id) })
                                }) { Icon(Icons.Default.Delete, contentDescription = s.deleteLabel) }
                            }
                            Slider(
                                value = sliderLevel,
                                onValueChange = { sliderLevel = it },
                                onValueChangeFinished = {
                                    mainViewModel.runRepoAction({
                                        mainViewModel.modulesRepo.updateSmartDevice(dev.id, dev.powerOn, sliderLevel.toLong())
                                    })
                                },
                                valueRange = 0f..100f
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Level: ${sliderLevel.toInt()}%", style = MaterialTheme.typography.labelSmall)
                                if (dev.automationRule.isNotBlank()) {
                                    Text(dev.automationRule, style = MaterialTheme.typography.labelSmall, color = EmeraldLife)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var room by remember { mutableStateOf("Living Room") }
        var type by remember { mutableStateOf("LIGHT") }
        var rule by remember { mutableStateOf("Turn off at 11:30 PM") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Smart Device") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Device Name") }, singleLine = true)
                    OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Room") }, singleLine = true)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("LIGHT", "AC", "FAN", "LOCK", "PLUG")) { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                        }
                    }
                    OutlinedTextField(value = rule, onValueChange = { rule = it }, label = { Text("Automation Rule") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        mainViewModel.runRepoAction({
                            mainViewModel.modulesRepo.addSmartDevice(name, room, type, 75L, rule)
                        })
                        showAddDialog = false
                    }
                }) { Text(s.saveLabel) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(s.cancelLabel) } }
        )
    }
}

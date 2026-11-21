package com.example.ui.screens.modules

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AiResponseState
import com.example.data.model.ToolModule
import com.example.data.model.UiState
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import com.example.ui.components.NexoraModuleTopBar
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CrimsonSos
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldLife
import com.example.ui.viewmodel.AiBrainViewModel
import com.example.ui.viewmodel.NexoraMainViewModel

@Composable
fun LifeManagerScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onBack: () -> Unit
) {
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val tasksState by mainViewModel.tasks.collectAsStateWithLifecycle()
    val dailyPlan by aiViewModel.dailyPlan.collectAsStateWithLifecycle()
    val toolAiState by aiViewModel.toolAiState.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    var selectedCategory by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var focusGoal by remember { mutableStateOf("") }

    val categories = listOf("ALL", "TASK", "ROUTINE", "EVENT", "BIRTHDAY")
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val filteredTasks = remember(tasks, selectedCategory) {
        if (selectedCategory == "ALL") tasks else tasks.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.LIFE_MANAGER.titleBn else ToolModule.LIFE_MANAGER.titleEn,
                language = language,
                onBack = onBack,
                onToggleLanguage = { mainViewModel.toggleLanguage() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CyanPulse,
                contentColor = Color.White,
                modifier = Modifier.testTag("life_add_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // AI Daily Planning Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, CyanPulse.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanPulse)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "AI ডেইলি প্ল্যানিং ও রুটিন" else "AI Daily Planning & Schedule",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = focusGoal,
                                onValueChange = { focusGoal = it },
                                label = { Text(if (language == AppLanguage.BN) "আজকের মূল লক্ষ্য (ঐচ্ছিক)" else "Today's focus goal (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { aiViewModel.generateDailyPlan(tasks, focusGoal, language) },
                                enabled = toolAiState !is AiResponseState.Loading,
                                modifier = Modifier.fillMaxWidth().testTag("generate_daily_plan_btn")
                            ) {
                                if (toolAiState is AiResponseState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Text(if (language == AppLanguage.BN) "স্মার্ট ডেইলি প্ল্যান তৈরি করুন" else "Generate AI Daily Plan")
                                }
                            }
                            if (toolAiState is AiResponseState.Error) {
                                Text(
                                    text = (toolAiState as AiResponseState.Error).message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (dailyPlan.isNotEmpty()) {
                    items(dailyPlan) { slot ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(slot.timeSlot, style = MaterialTheme.typography.labelLarge, color = CyanPulse)
                                    StatusPillBadge(slot.priority, accent = EmeraldLife)
                                }
                                Text(slot.activity, style = MaterialTheme.typography.titleSmall)
                                if (slot.tip.isNotBlank()) {
                                    Text(slot.tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Category Filter Chips
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                if (filteredTasks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(36.dp), tint = CyanPulse)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "কোনো কাজ বা রিমাইন্ডার নেই — নিচের + বাটনে যোগ করুন" else "No tasks or events yet — tap + to create one",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        val priorityColor = when (task.priority) {
                            "HIGH" -> CrimsonSos
                            "MEDIUM" -> AmberAlert
                            else -> EmeraldLife
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.completed,
                                    onCheckedChange = { checked ->
                                        mainViewModel.runRepoAction({ mainViewModel.coreRepo.toggleTask(task.id, checked) })
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (task.category == "BIRTHDAY") {
                                            Icon(Icons.Default.Cake, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        StatusPillBadge(task.category, CyanPulse)
                                        StatusPillBadge(task.priority, priorityColor)
                                        Text(
                                            text = "${task.dueDateLabel} • ${task.timeLabel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        mainViewModel.runRepoAction({ mainViewModel.coreRepo.deleteTask(task.id) })
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = s.deleteLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("TASK") }
        var priority by remember { mutableStateOf("MEDIUM") }
        var dateLabel by remember { mutableStateOf("Today") }
        var timeLabel by remember { mutableStateOf("09:00 AM") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (language == AppLanguage.BN) "নতুন কাজ / রুটিন / জন্মদিন" else "New Task / Routine / Event") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (language == AppLanguage.BN) "শিরোনাম" else "Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("TASK", "ROUTINE", "EVENT", "BIRTHDAY")) { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                            FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dateLabel,
                            onValueChange = { dateLabel = it },
                            label = { Text("Date") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = timeLabel,
                            onValueChange = { timeLabel = it },
                            label = { Text("Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            mainViewModel.runRepoAction(
                                { mainViewModel.coreRepo.addTask(title, category, priority, dateLabel, timeLabel) }
                            )
                            showAddDialog = false
                        }
                    }
                ) { Text(s.saveLabel) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(s.cancelLabel) }
            }
        )
    }
}

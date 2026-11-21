package com.example.ui.screens.modules

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
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
import com.example.util.formatBdt

@Composable
fun FinanceManagerScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onBack: () -> Unit
) {
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val profileState by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val txState by mainViewModel.transactions.collectAsStateWithLifecycle()
    val goalsState by mainViewModel.savingsGoals.collectAsStateWithLifecycle()
    val toolAiState by aiViewModel.toolAiState.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    var showTxDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    val transactions = (txState as? UiState.Success)?.data.orEmpty()
    val goals = (goalsState as? UiState.Success)?.data.orEmpty()
    val budget = (profileState as? UiState.Success)?.data?.monthlyBudgetBdt ?: 25000.0
    val totalIncome = transactions.filter { it.txType == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.txType == "EXPENSE" || it.txType == "BILL" }.sumOf { it.amount }
    val budgetRatio = if (budget > 0) (totalExpense / budget).toFloat().coerceIn(0f, 1f) else 0f

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.FINANCE_MANAGER.titleBn else ToolModule.FINANCE_MANAGER.titleEn,
                language = language,
                onBack = onBack,
                onToggleLanguage = { mainViewModel.toggleLanguage() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTxDialog = true },
                containerColor = EmeraldLife,
                contentColor = Color.White,
                modifier = Modifier.testTag("finance_add_tx_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary & Budget Progress Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, EmeraldLife.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(if (language == AppLanguage.BN) "মোট আয়" else "Income", style = MaterialTheme.typography.labelSmall)
                                    Text(formatBdt(totalIncome), style = MaterialTheme.typography.titleLarge, color = EmeraldLife)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(if (language == AppLanguage.BN) "মোট ব্যয় ও বিল" else "Expenses & Bills", style = MaterialTheme.typography.labelSmall)
                                    Text(formatBdt(totalExpense), style = MaterialTheme.typography.titleLarge, color = CrimsonSos)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BN) "মাসিক বাজেট: ${formatBdt(budget)}" else "Monthly Budget: ${formatBdt(budget)}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                TextButton(onClick = { showBudgetDialog = true }) {
                                    Text(if (language == AppLanguage.BN) "পরিবর্তন" else "Edit")
                                }
                            }
                            LinearProgressIndicator(
                                progress = { budgetRatio },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = if (budgetRatio > 0.85f) CrimsonSos else EmeraldLife
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val summary = transactions.take(15).joinToString("; ") { "${it.txType}: ${it.title} ${formatBdt(it.amount)} (${it.category})" }
                                    aiViewModel.runToolPrompt(
                                        "Analyze my BDT finances (Budget: ${formatBdt(budget)}, Income: ${formatBdt(totalIncome)}, Expense: ${formatBdt(totalExpense)}). Transactions: $summary. Give 4 actionable Bangladeshi savings tips.",
                                        language, ExpertMode.FINANCIAL
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("finance_ai_analysis_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (language == AppLanguage.BN) "AI খরচ বিশ্লেষণ" else "AI Spending Analysis")
                            }
                        }
                    }
                }

                item { AiResponseCard(state = toolAiState, language = language) }

                // Savings Goals
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (language == AppLanguage.BN) "সঞ্চয় লক্ষ্য (Savings Goals)" else "Savings Goals", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { showGoalDialog = true }) {
                            Icon(Icons.Default.Savings, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(s.addLabel)
                        }
                    }
                }
                items(goals, key = { it.id }) { goal ->
                    val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(goal.title, style = MaterialTheme.typography.titleSmall)
                                Text("${formatBdt(goal.savedAmount)} / ${formatBdt(goal.targetAmount)}", style = MaterialTheme.typography.labelLarge, color = CyanPulse)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = {
                                    mainViewModel.runRepoAction({
                                        mainViewModel.modulesRepo.contributeToGoal(goal.id, goal.savedAmount + 500.0)
                                    })
                                }) { Text("+ ৳500") }
                                TextButton(onClick = {
                                    mainViewModel.runRepoAction({ mainViewModel.coreRepo.deleteUserDoc("savings_goals", goal.id) })
                                }) { Text(s.deleteLabel) }
                            }
                        }
                    }
                }

                // Transactions List
                item {
                    Text(if (language == AppLanguage.BN) "লেনদেন ও বিল" else "Transactions & Bills", style = MaterialTheme.typography.titleMedium)
                }
                items(transactions, key = { it.id }) { tx ->
                    val color = if (tx.txType == "INCOME") EmeraldLife else if (tx.txType == "BILL") AmberAlert else CrimsonSos
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.title, style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusPillBadge(tx.txType, color)
                                    Text("${tx.category} • ${tx.dateLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(formatBdt(tx.amount), style = MaterialTheme.typography.titleMedium, color = color)
                            IconButton(onClick = {
                                mainViewModel.runRepoAction({ mainViewModel.coreRepo.deleteUserDoc("transactions", tx.id) })
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = s.deleteLabel)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTxDialog) {
        var title by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var txType by remember { mutableStateOf("EXPENSE") }
        var category by remember { mutableStateOf("Food") }
        AlertDialog(
            onDismissRequest = { showTxDialog = false },
            title = { Text(if (language == AppLanguage.BN) "নতুন লেনদেন যোগ করুন" else "Add Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("EXPENSE", "INCOME", "BILL").forEach { t ->
                            FilterChip(selected = txType == t, onClick = { txType = t }, label = { Text(t) })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Amount (BDT)") }, singleLine = true)
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0.0) {
                        mainViewModel.runRepoAction({ mainViewModel.modulesRepo.addTransaction(title, amt, txType, category, "Today") })
                        showTxDialog = false
                    }
                }) { Text(s.saveLabel) }
            },
            dismissButton = { TextButton(onClick = { showTxDialog = false }) { Text(s.cancelLabel) } }
        )
    }

    if (showGoalDialog) {
        var title by remember { mutableStateOf("") }
        var targetStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("New Savings Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal Name") }, singleLine = true)
                    OutlinedTextField(value = targetStr, onValueChange = { targetStr = it }, label = { Text("Target Amount (BDT)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val target = targetStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && target > 0.0) {
                        mainViewModel.runRepoAction({ mainViewModel.modulesRepo.addSavingsGoal(title, target, 0.0, "2027") })
                        showGoalDialog = false
                    }
                }) { Text(s.saveLabel) }
            },
            dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text(s.cancelLabel) } }
        )
    }

    if (showBudgetDialog) {
        var budgetStr by remember { mutableStateOf(budget.toLong().toString()) }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Monthly Budget (BDT)") },
            text = {
                OutlinedTextField(value = budgetStr, onValueChange = { budgetStr = it }, label = { Text("Monthly Limit (BDT)") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = {
                    budgetStr.toDoubleOrNull()?.let { mainViewModel.updateMonthlyBudget(it) }
                    showBudgetDialog = false
                }) { Text(s.saveLabel) }
            },
            dismissButton = { TextButton(onClick = { showBudgetDialog = false }) { Text(s.cancelLabel) } }
        )
    }
}

@Composable
fun ShoppingAssistantScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onBack: () -> Unit
) {
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val itemsState by mainViewModel.shoppingItems.collectAsStateWithLifecycle()
    val toolAiState by aiViewModel.toolAiState.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    var productQuery by remember { mutableStateOf("") }
    var budgetBdt by remember { mutableStateOf("20000") }
    var showAddDialog by remember { mutableStateOf(false) }

    val shoppingList = (itemsState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.SHOPPING_ASSISTANT.titleBn else ToolModule.SHOPPING_ASSISTANT.titleEn,
                language = language,
                onBack = onBack,
                onToggleLanguage = { mainViewModel.toggleLanguage() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AmberAlert,
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, contentDescription = "Add Shopping Item") }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (language == AppLanguage.BN) "পণ্যের তুলনা ও বাজেট সাজেশন" else "Product Comparison & Budget Recommendations",
                                style = MaterialTheme.typography.titleMedium
                            )
                            OutlinedTextField(
                                value = productQuery,
                                onValueChange = { productQuery = it },
                                label = { Text(if (language == AppLanguage.BN) "পণ্যের নাম বা তুলনা (যেমন: Samsung A35 vs Redmi Note 13)" else "Product name or comparison") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = budgetBdt,
                                onValueChange = { budgetBdt = it },
                                label = { Text("Budget (BDT)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (productQuery.isNotBlank()) {
                                        aiViewModel.runToolPrompt(
                                            "Act as a Bangladeshi AI Shopping Assistant. Compare options, summarize pros/cons & user reviews, and recommend the best value for '$productQuery' under $budgetBdt BDT.",
                                            language
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (language == AppLanguage.BN) "তুলনা ও সাজেশন দেখুন" else "Compare & Recommend")
                            }
                        }
                    }
                }

                item { AiResponseCard(state = toolAiState, language = language) }

                item {
                    Text(if (language == AppLanguage.BN) "শপিং লিস্ট" else "Shopping List", style = MaterialTheme.typography.titleMedium)
                }

                items(shoppingList, key = { it.id }) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = item.purchased,
                                onCheckedChange = { checked ->
                                    mainViewModel.runRepoAction({ mainViewModel.modulesRepo.toggleShoppingItem(item.id, checked) })
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    textDecoration = if (item.purchased) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Text("${item.category} • ${formatBdt(item.estimatedPriceBdt)}", style = MaterialTheme.typography.labelSmall, color = AmberAlert)
                            }
                            IconButton(onClick = {
                                mainViewModel.runRepoAction({ mainViewModel.coreRepo.deleteUserDoc("shopping_items", item.id) })
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
        var name by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var cat by remember { mutableStateOf("Groceries") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Shopping Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") }, singleLine = true)
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Est. Price (BDT)") }, singleLine = true)
                    OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("Category") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        mainViewModel.runRepoAction({
                            mainViewModel.modulesRepo.addShoppingItem(name, price.toDoubleOrNull() ?: 0.0, cat)
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
fun TravelPlannerScreen(
    mainViewModel: NexoraMainViewModel,
    aiViewModel: AiBrainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val travelPlan by aiViewModel.travelPlan.collectAsStateWithLifecycle()
    val toolAiState by aiViewModel.toolAiState.collectAsStateWithLifecycle()

    var destination by remember { mutableStateOf("") }
    var daysStr by remember { mutableStateOf("3") }
    var budgetStr by remember { mutableStateOf("12000") }

    Scaffold(
        topBar = {
            NexoraModuleTopBar(
                title = if (language == AppLanguage.BN) ToolModule.TRAVEL_PLANNER.titleBn else ToolModule.TRAVEL_PLANNER.titleEn,
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
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = destination,
                                onValueChange = { destination = it },
                                label = { Text(if (language == AppLanguage.BN) "গন্তব্য (যেমন: Cox's Bazar, Sajek, Sylhet)" else "Destination") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = daysStr,
                                    onValueChange = { daysStr = it },
                                    label = { Text("Days") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = budgetStr,
                                    onValueChange = { budgetStr = it },
                                    label = { Text("Budget (BDT)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (destination.isNotBlank()) {
                                            aiViewModel.generateTravelPlan(
                                                destination = destination,
                                                days = (daysStr.toIntOrNull() ?: 3).coerceIn(1, 14),
                                                budgetBdt = budgetStr.toDoubleOrNull() ?: 10000.0,
                                                language = language
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("travel_generate_btn")
                                ) {
                                    Icon(Icons.Default.FlightTakeoff, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (language == AppLanguage.BN) "ট্যুর প্ল্যান তৈরি" else "Build Tour Plan")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val q = Uri.encode(destination.ifBlank { "Bangladesh" })
                                        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q"))
                                        context.startActivity(mapIntent)
                                    }
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = "Open Maps")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Maps")
                                }
                            }
                        }
                    }
                }

                if (travelPlan != null) {
                    val plan = travelPlan!!
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, IndigoNeural.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${plan.destination} • Est. ${formatBdt(plan.totalEstimatedBdt)}", style = MaterialTheme.typography.titleLarge, color = CyanPulse)
                                if (plan.hotelSuggestions.isNotBlank()) {
                                    Text("Hotels: ${plan.hotelSuggestions}", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (plan.checklist.isNotEmpty()) {
                                    Text("Checklist: ${plan.checklist.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(onClick = {
                                    val summary = plan.days.joinToString("\n\n") { "${it.dayTitle} (${formatBdt(it.estimatedCostBdt)}):\n${it.activities}" }
                                    mainViewModel.runRepoAction(
                                        { mainViewModel.coreRepo.saveDocument("Trip: ${plan.destination}", "TRAVEL_PLAN", summary) },
                                        "Saved itinerary to Documents"
                                    )
                                }) {
                                    Text(if (language == AppLanguage.BN) "প্ল্যান সংরক্ষণ করুন" else "Save Itinerary")
                                }
                            }
                        }
                    }
                    items(plan.days) { day ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(day.dayTitle, style = MaterialTheme.typography.titleSmall)
                                    StatusPillBadge(formatBdt(day.estimatedCostBdt), EmeraldLife)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(day.activities, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    item { AiResponseCard(state = toolAiState, language = language) }
                }
            }
        }
    }
}

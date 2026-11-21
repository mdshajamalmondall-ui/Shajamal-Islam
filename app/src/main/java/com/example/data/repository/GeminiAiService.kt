package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AgentActionProposal
import com.example.data.model.DailyPlanItem
import com.example.data.model.ExpertMode
import com.example.data.model.HealthRecord
import com.example.data.model.LifeTask
import com.example.data.model.McqQuestion
import com.example.data.model.PersonalMemory
import com.example.data.model.ProposedExpense
import com.example.data.model.ProposedMemory
import com.example.data.model.ProposedTask
import com.example.data.model.TravelDayPlan
import com.example.data.model.TravelItinerary
import com.example.localization.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAiService"
private const val DEFAULT_MODEL = "gemini-3.8-flash"
private const val LITE_MODEL = "gemini-3.5-flash-lite"
private const val AI_TIMEOUT_MS = 60_000L

class GeminiAiService {

    private val defaultModel by lazy {
        Firebase.ai.generativeModel(modelName = DEFAULT_MODEL)
    }

    private val liteModel by lazy {
        Firebase.ai.generativeModel(modelName = LITE_MODEL)
    }

    private val agentProposalSchema = Schema.obj(
        properties = mapOf(
            "replyText" to Schema.string(description = "Helpful conversational answer in the requested language."),
            "summaryDescription" to Schema.string(description = "Short 1-sentence summary of proposed actions for user permission."),
            "tasks" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(),
                        "category" to Schema.enumeration(listOf("TASK", "ROUTINE", "EVENT", "BIRTHDAY", "MEDICINE")),
                        "priority" to Schema.enumeration(listOf("HIGH", "MEDIUM", "LOW")),
                        "dueDateLabel" to Schema.string(),
                        "timeLabel" to Schema.string()
                    )
                )
            ),
            "expenses" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(),
                        "amount" to Schema.double(),
                        "txType" to Schema.enumeration(listOf("INCOME", "EXPENSE", "BILL")),
                        "category" to Schema.string()
                    )
                )
            ),
            "memories" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(),
                        "content" to Schema.string(),
                        "memoryType" to Schema.enumeration(listOf("PREFERENCE", "GOAL", "NOTE", "HABIT", "KNOWLEDGE"))
                    )
                )
            )
        )
    )

    private val mcqSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string(),
                        "options" to Schema.array(items = Schema.string()),
                        "correctIndex" to Schema.integer(),
                        "explanation" to Schema.string()
                    )
                )
            )
        )
    )

    private val dailyPlanSchema = Schema.obj(
        properties = mapOf(
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "timeSlot" to Schema.string(),
                        "activity" to Schema.string(),
                        "priority" to Schema.enumeration(listOf("HIGH", "MEDIUM", "LOW")),
                        "tip" to Schema.string()
                    )
                )
            )
        )
    )

    private val travelSchema = Schema.obj(
        properties = mapOf(
            "destination" to Schema.string(),
            "totalEstimatedBdt" to Schema.double(),
            "hotelSuggestions" to Schema.string(),
            "checklist" to Schema.array(items = Schema.string()),
            "days" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "dayTitle" to Schema.string(),
                        "activities" to Schema.string(),
                        "estimatedCostBdt" to Schema.double()
                    )
                )
            )
        )
    )

    private fun buildSystemContext(
        language: AppLanguage,
        expertMode: ExpertMode,
        memories: List<PersonalMemory>
    ): String {
        val langRule = if (language == AppLanguage.BN) {
            "Respond naturally in clear, polite Bengali (বাংলা) unless the user asks in English or requests English output."
        } else {
            "Respond clearly and concisely in English unless the user asks in Bengali."
        }
        val memoryBlock = if (memories.isNotEmpty()) {
            val topMemories = memories.take(8).joinToString("\n") { "- ${it.title}: ${it.content}" }
            "\n\nUser-approved Personal Memory & Digital Twin Context:\n$topMemories"
        } else ""
        return "${expertMode.roleInstruction}\n$langRule\nNever fabricate unavailable real-time sensor readings or unverified citations; note uncertainty clearly when appropriate.$memoryBlock"
    }

    fun streamChatOrSearch(
        prompt: String,
        language: AppLanguage,
        expertMode: ExpertMode,
        memories: List<PersonalMemory>,
        historyPairs: List<Pair<String, String>> = emptyList(),
        bitmap: Bitmap? = null
    ): Flow<String> = flow {
        val sysContext = buildSystemContext(language, expertMode, memories)
        val historyContext = if (historyPairs.isNotEmpty()) {
            val formatted = historyPairs.takeLast(6).joinToString("\n") { (role, msg) ->
                "${if (role == "user") "User" else "NEXORA"}: $msg"
            }
            "\n\nRecent conversation context:\n$formatted\n"
        } else ""

        val fullPrompt = "$sysContext$historyContext\nUser Request: $prompt"
        val scaledBitmap = bitmap?.let { downscaleBitmap(it) }

        val inputContent = content {
            if (scaledBitmap != null) {
                image(scaledBitmap)
            }
            text(fullPrompt)
        }

        val builder = StringBuilder()
        withTimeout(AI_TIMEOUT_MS) {
            defaultModel.generateContentStream(inputContent).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    builder.append(chunkText)
                    emit(builder.toString())
                }
            }
        }
        if (builder.isBlank()) {
            throw IllegalStateException("AI returned an empty response.")
        }
    }

    suspend fun generateText(
        prompt: String,
        language: AppLanguage,
        expertMode: ExpertMode = ExpertMode.GENERAL,
        memories: List<PersonalMemory> = emptyList(),
        bitmap: Bitmap? = null,
        useLite: Boolean = false
    ): String {
        val sysContext = buildSystemContext(language, expertMode, memories)
        val fullPrompt = "$sysContext\n\n$prompt"
        val scaledBitmap = bitmap?.let { downscaleBitmap(it) }
        val model = if (useLite && scaledBitmap == null) liteModel else defaultModel

        val response = withTimeout(AI_TIMEOUT_MS) {
            if (scaledBitmap != null) {
                model.generateContent(
                    content {
                        image(scaledBitmap)
                        text(fullPrompt)
                    }
                )
            } else {
                model.generateContent(fullPrompt)
            }
        }
        return response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("AI returned an empty response.")
    }

    suspend fun generateAgentProposal(
        prompt: String,
        language: AppLanguage,
        memories: List<PersonalMemory>
    ): AgentActionProposal {
        val model = Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = agentProposalSchema
            }
        )
        val sys = buildSystemContext(language, ExpertMode.GENERAL, memories)
        val instruction = """
            $sys
            You are in NEXORA Agent Mode. Analyze the user's request and:
            1. Provide a helpful replyText.
            2. If the user asks to plan, schedule, remind, track spending/income in BDT, or remember personal preferences/goals/notes, populate structured tasks, expenses, and/or memories arrays so the app can ask the user for permission before saving them.
            3. If no actionable records are needed, leave tasks, expenses, and memories empty.
            User request: $prompt
        """.trimIndent()

        val response = withTimeout(AI_TIMEOUT_MS) { model.generateContent(instruction) }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty agent JSON"))
        val tasksArr = json.optJSONArray("tasks")
        val tasks = buildList {
            if (tasksArr != null) {
                for (i in 0 until tasksArr.length()) {
                    val o = tasksArr.getJSONObject(i)
                    add(
                        ProposedTask(
                            title = o.optString("title", "Task"),
                            category = o.optString("category", "TASK"),
                            priority = o.optString("priority", "MEDIUM"),
                            dueDateLabel = o.optString("dueDateLabel", "Today"),
                            timeLabel = o.optString("timeLabel", "09:00 AM")
                        )
                    )
                }
            }
        }
        val expArr = json.optJSONArray("expenses")
        val expenses = buildList {
            if (expArr != null) {
                for (i in 0 until expArr.length()) {
                    val o = expArr.getJSONObject(i)
                    val amt = o.optDouble("amount", 0.0)
                    if (amt > 0.0) {
                        add(
                            ProposedExpense(
                                title = o.optString("title", "Expense"),
                                amount = amt,
                                txType = o.optString("txType", "EXPENSE"),
                                category = o.optString("category", "General")
                            )
                        )
                    }
                }
            }
        }
        val memArr = json.optJSONArray("memories")
        val mems = buildList {
            if (memArr != null) {
                for (i in 0 until memArr.length()) {
                    val o = memArr.getJSONObject(i)
                    add(
                        ProposedMemory(
                            title = o.optString("title", "Memory"),
                            content = o.optString("content", ""),
                            memoryType = o.optString("memoryType", "NOTE")
                        )
                    )
                }
            }
        }
        return AgentActionProposal(
            replyText = json.optString("replyText", ""),
            summaryDescription = json.optString("summaryDescription", ""),
            tasks = tasks,
            expenses = expenses,
            memories = mems
        )
    }

    suspend fun generateMcqQuiz(
        topic: String,
        questionCount: Int,
        language: AppLanguage
    ): List<McqQuestion> {
        val model = Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mcqSchema
            }
        )
        val langLabel = if (language == AppLanguage.BN) "Bangla" else "English"
        val prompt = "Create $questionCount multiple-choice quiz questions with 4 options each on the topic '$topic' in $langLabel. correctIndex must be 0..3."
        val response = withTimeout(AI_TIMEOUT_MS) { model.generateContent(prompt) }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty quiz JSON"))
        val arr = json.getJSONArray("questions")
        return (0 until arr.length()).map { idx ->
            val obj = arr.getJSONObject(idx)
            val optsArr = obj.getJSONArray("options")
            val opts = (0 until optsArr.length()).map { optsArr.getString(it) }
            McqQuestion(
                question = obj.getString("question"),
                options = opts,
                correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0)),
                explanation = obj.optString("explanation", "")
            )
        }
    }

    suspend fun generateDailyPlan(
        tasks: List<LifeTask>,
        focusGoal: String,
        language: AppLanguage
    ): List<DailyPlanItem> {
        val model = Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = dailyPlanSchema
            }
        )
        val langLabel = if (language == AppLanguage.BN) "Bangla" else "English"
        val taskListText = tasks.filter { !it.completed }.take(10)
            .joinToString("; ") { "${it.title} (${it.priority}, ${it.timeLabel})" }
        val prompt = """
            Create a realistic, motivating 6-slot daily schedule in $langLabel.
            User's pending tasks: ${taskListText.ifBlank { "General productive day" }}
            User's focus goal: ${focusGoal.ifBlank { "Balanced work, health, and learning" }}
        """.trimIndent()
        val response = withTimeout(AI_TIMEOUT_MS) { model.generateContent(prompt) }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty plan JSON"))
        val arr = json.getJSONArray("items")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            DailyPlanItem(
                timeSlot = o.getString("timeSlot"),
                activity = o.getString("activity"),
                priority = o.optString("priority", "MEDIUM"),
                tip = o.optString("tip", "")
            )
        }
    }

    suspend fun generateTravelItinerary(
        destination: String,
        daysCount: Int,
        budgetBdt: Double,
        language: AppLanguage
    ): TravelItinerary {
        val model = Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = travelSchema
            }
        )
        val langLabel = if (language == AppLanguage.BN) "Bangla" else "English"
        val prompt = "Plan a $daysCount-day trip to $destination within a budget of $budgetBdt BDT in $langLabel. Include realistic hotel suggestions, a packing checklist, and day-by-day activities with BDT cost estimates."
        val response = withTimeout(AI_TIMEOUT_MS) { model.generateContent(prompt) }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty travel JSON"))
        val checklistArr = json.optJSONArray("checklist")
        val checklist = if (checklistArr != null) {
            (0 until checklistArr.length()).map { checklistArr.getString(it) }
        } else emptyList()
        val daysArr = json.optJSONArray("days")
        val days = if (daysArr != null) {
            (0 until daysArr.length()).map { i ->
                val o = daysArr.getJSONObject(i)
                TravelDayPlan(
                    dayTitle = o.getString("dayTitle"),
                    activities = o.getString("activities"),
                    estimatedCostBdt = o.optDouble("estimatedCostBdt", 0.0)
                )
            }
        } else emptyList()
        return TravelItinerary(
            destination = json.optString("destination", destination),
            totalEstimatedBdt = json.optDouble("totalEstimatedBdt", budgetBdt),
            hotelSuggestions = json.optString("hotelSuggestions", ""),
            checklist = checklist,
            days = days
        )
    }

    suspend fun generateMorningBriefing(
        displayName: String,
        tasks: List<LifeTask>,
        healthRecords: List<HealthRecord>,
        memories: List<PersonalMemory>,
        language: AppLanguage
    ): String {
        val pendingTasks = tasks.filter { !it.completed }.take(5).joinToString(", ") { it.title }
        val medicines = healthRecords.filter { it.recordType == "MEDICINE" && !it.takenToday }
            .take(3).joinToString(", ") { "${it.title} (${it.scheduleTime})" }
        val goals = memories.filter { it.memoryType == "GOAL" || it.pinned }
            .take(3).joinToString(", ") { it.content }

        val prompt = """
            Generate a crisp, encouraging Morning Intelligence & Predictive AI Briefing (max 5 bullet points) for $displayName.
            Pending tasks: ${pendingTasks.ifBlank { "None yet" }}
            Today's medicine reminders: ${medicines.ifBlank { "None" }}
            Personal Digital Twin goals/preferences: ${goals.ifBlank { "Healthy, productive day" }}
            Include 1 predictive recommendation based on their routine.
        """.trimIndent()
        return try {
            generateText(prompt, language, ExpertMode.GENERAL, memories, useLite = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Morning briefing failed", e)
            throw e
        }
    }

    private fun downscaleBitmap(bitmap: Bitmap, maxDim: Int = 1280): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val scale = maxDim.toFloat() / maxOf(w, h).toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * scale).toInt().coerceAtLeast(1),
            (h * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}

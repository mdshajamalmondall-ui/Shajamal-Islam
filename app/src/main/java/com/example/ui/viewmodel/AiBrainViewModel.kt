package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AgentActionProposal
import com.example.data.model.AiResponseState
import com.example.data.model.ChatMessageRecord
import com.example.data.model.DailyPlanItem
import com.example.data.model.ExpertMode
import com.example.data.model.LifeTask
import com.example.data.model.McqQuestion
import com.example.data.model.PersonalMemory
import com.example.data.model.TravelItinerary
import com.example.data.repository.GeminiAiService
import com.example.data.repository.NexoraCoreRepository
import com.example.data.repository.NexoraModulesRepository
import com.example.localization.AppLanguage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "AiBrainVM"

@OptIn(ExperimentalCoroutinesApi::class)
class AiBrainViewModel(
    private val aiService: GeminiAiService,
    private val coreRepo: NexoraCoreRepository,
    private val modulesRepo: NexoraModulesRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val activeMessages: StateFlow<List<ChatMessageRecord>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId.isNullOrBlank()) flowOf(emptyList())
            else coreRepo.observeMessages(currentUserId, convId).catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                Log.w(TAG, "Error loading messages", fsErr ?: error)
                emit(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _chatState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    val chatState: StateFlow<AiResponseState> = _chatState.asStateFlow()

    private val _searchState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    val searchState: StateFlow<AiResponseState> = _searchState.asStateFlow()

    private val _toolAiState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    val toolAiState: StateFlow<AiResponseState> = _toolAiState.asStateFlow()

    private val _pendingProposal = MutableStateFlow<AgentActionProposal?>(null)
    val pendingProposal: StateFlow<AgentActionProposal?> = _pendingProposal.asStateFlow()

    private val _mcqQuiz = MutableStateFlow<List<McqQuestion>>(emptyList())
    val mcqQuiz: StateFlow<List<McqQuestion>> = _mcqQuiz.asStateFlow()

    private val _dailyPlan = MutableStateFlow<List<DailyPlanItem>>(emptyList())
    val dailyPlan: StateFlow<List<DailyPlanItem>> = _dailyPlan.asStateFlow()

    private val _travelPlan = MutableStateFlow<TravelItinerary?>(null)
    val travelPlan: StateFlow<TravelItinerary?> = _travelPlan.asStateFlow()

    fun selectConversation(conversationId: String?) {
        _activeConversationId.value = conversationId
        _chatState.value = AiResponseState.Idle
        _pendingProposal.value = null
    }

    fun clearToolAiState() {
        _toolAiState.value = AiResponseState.Idle
    }

    fun sendChatMessage(
        prompt: String,
        language: AppLanguage,
        expertMode: ExpertMode,
        memories: List<PersonalMemory>,
        agentMode: Boolean,
        bitmap: Bitmap? = null
    ) {
        if (prompt.isBlank() && bitmap == null) return
        viewModelScope.launch {
            _chatState.value = AiResponseState.Loading
            _pendingProposal.value = null
            try {
                if (agentMode && bitmap == null) {
                    val proposal = aiService.generateAgentProposal(prompt, language, memories)
                    val reply = proposal.replyText.ifBlank { proposal.summaryDescription }
                    _chatState.value = AiResponseState.Success(reply)
                    if (proposal.hasActions) {
                        _pendingProposal.value = proposal
                    }
                    val convRes = coreRepo.saveChatExchange(
                        conversationId = _activeConversationId.value,
                        userText = prompt,
                        modelText = reply,
                        expertMode = expertMode.id,
                        hasImage = false
                    )
                    convRes.getOrNull()?.let { _activeConversationId.value = it }
                } else {
                    val historyPairs = activeMessages.value.map { it.role to it.text }
                    var latest = ""
                    aiService.streamChatOrSearch(
                        prompt = prompt.ifBlank { "Analyze this image in detail." },
                        language = language,
                        expertMode = expertMode,
                        memories = memories,
                        historyPairs = historyPairs,
                        bitmap = bitmap
                    ).collect { accumulated ->
                        latest = accumulated
                        _chatState.value = AiResponseState.Streaming(accumulated)
                    }
                    _chatState.value = AiResponseState.Success(latest)
                    val convRes = coreRepo.saveChatExchange(
                        conversationId = _activeConversationId.value,
                        userText = prompt.ifBlank { "Image analysis" },
                        modelText = latest,
                        expertMode = expertMode.id,
                        hasImage = bitmap != null
                    )
                    convRes.getOrNull()?.let { _activeConversationId.value = it }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat timed out", e)
                _chatState.value = AiResponseState.Error("Request timed out after 60s. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat error", e)
                _chatState.value = AiResponseState.Error(e.localizedMessage ?: "AI generation failed.")
            }
        }
    }

    fun executeApprovedProposal(onDone: (String) -> Unit) {
        val proposal = _pendingProposal.value ?: return
        viewModelScope.launch {
            _pendingProposal.value = null
            var count = 0
            proposal.tasks.forEach { t ->
                if (coreRepo.addTask(t.title, t.category, t.priority, t.dueDateLabel, t.timeLabel).isSuccess) count++
            }
            proposal.expenses.forEach { e ->
                if (modulesRepo.addTransaction(e.title, e.amount, e.txType, e.category, "Today").isSuccess) count++
            }
            proposal.memories.forEach { m ->
                if (coreRepo.addMemory(m.title, m.content, m.memoryType).isSuccess) count++
            }
            onDone("Executed $count approved AI Agent action(s).")
        }
    }

    fun dismissProposal() {
        _pendingProposal.value = null
    }

    fun runUniversalSearch(
        query: String,
        language: AppLanguage,
        expertMode: ExpertMode,
        memories: List<PersonalMemory>,
        bitmap: Bitmap? = null
    ) {
        if (query.isBlank() && bitmap == null) return
        viewModelScope.launch {
            _searchState.value = AiResponseState.Loading
            try {
                var latest = ""
                aiService.streamChatOrSearch(
                    prompt = query.ifBlank { "Explain everything shown on this screen or image." },
                    language = language,
                    expertMode = expertMode,
                    memories = memories,
                    bitmap = bitmap
                ).collect { accumulated ->
                    latest = accumulated
                    _searchState.value = AiResponseState.Streaming(accumulated)
                }
                _searchState.value = AiResponseState.Success(latest)
            } catch (e: TimeoutCancellationException) {
                _searchState.value = AiResponseState.Error("Search timed out. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Search error", e)
                _searchState.value = AiResponseState.Error(e.localizedMessage ?: "Search failed.")
            }
        }
    }

    fun runToolPrompt(
        prompt: String,
        language: AppLanguage,
        expertMode: ExpertMode = ExpertMode.GENERAL,
        memories: List<PersonalMemory> = emptyList(),
        bitmap: Bitmap? = null
    ) {
        viewModelScope.launch {
            _toolAiState.value = AiResponseState.Loading
            try {
                var latest = ""
                aiService.streamChatOrSearch(
                    prompt = prompt,
                    language = language,
                    expertMode = expertMode,
                    memories = memories,
                    bitmap = bitmap
                ).collect { accumulated ->
                    latest = accumulated
                    _toolAiState.value = AiResponseState.Streaming(accumulated)
                }
                _toolAiState.value = AiResponseState.Success(latest)
            } catch (e: TimeoutCancellationException) {
                _toolAiState.value = AiResponseState.Error("AI request timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Tool AI error", e)
                _toolAiState.value = AiResponseState.Error(e.localizedMessage ?: "AI request failed.")
            }
        }
    }

    fun generateMcqQuiz(topic: String, count: Int, language: AppLanguage) {
        viewModelScope.launch {
            _toolAiState.value = AiResponseState.Loading
            _mcqQuiz.value = emptyList()
            try {
                val questions = aiService.generateMcqQuiz(topic, count, language)
                _mcqQuiz.value = questions
                _toolAiState.value = AiResponseState.Success("Generated ${questions.size} questions.")
            } catch (e: TimeoutCancellationException) {
                _toolAiState.value = AiResponseState.Error("Quiz generation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "MCQ error", e)
                _toolAiState.value = AiResponseState.Error(e.localizedMessage ?: "Failed to generate MCQ test.")
            }
        }
    }

    fun generateDailyPlan(tasks: List<LifeTask>, focusGoal: String, language: AppLanguage) {
        viewModelScope.launch {
            _toolAiState.value = AiResponseState.Loading
            try {
                val plan = aiService.generateDailyPlan(tasks, focusGoal, language)
                _dailyPlan.value = plan
                _toolAiState.value = AiResponseState.Success("Daily plan ready.")
            } catch (e: TimeoutCancellationException) {
                _toolAiState.value = AiResponseState.Error("Daily planning timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Daily plan error", e)
                _toolAiState.value = AiResponseState.Error(e.localizedMessage ?: "Failed to generate daily plan.")
            }
        }
    }

    fun generateTravelPlan(destination: String, days: Int, budgetBdt: Double, language: AppLanguage) {
        viewModelScope.launch {
            _toolAiState.value = AiResponseState.Loading
            try {
                val itinerary = aiService.generateTravelItinerary(destination, days, budgetBdt, language)
                _travelPlan.value = itinerary
                _toolAiState.value = AiResponseState.Success("Travel itinerary ready.")
            } catch (e: TimeoutCancellationException) {
                _toolAiState.value = AiResponseState.Error("Travel planning timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Travel plan error", e)
                _toolAiState.value = AiResponseState.Error(e.localizedMessage ?: "Failed to generate travel plan.")
            }
        }
    }
}

package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConversationSummary
import com.example.data.model.EmergencyContact
import com.example.data.model.FamilyItem
import com.example.data.model.FamilySpace
import com.example.data.model.FinanceTransaction
import com.example.data.model.HealthRecord
import com.example.data.model.LifeTask
import com.example.data.model.PersonalMemory
import com.example.data.model.SavedDocument
import com.example.data.model.SavingsGoal
import com.example.data.model.ShoppingItem
import com.example.data.model.SmartDevice
import com.example.data.model.ToolModule
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.GeminiAiService
import com.example.data.repository.NexoraCoreRepository
import com.example.data.repository.NexoraModulesRepository
import com.example.data.repository.PreferencesRepository
import com.example.localization.AppLanguage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "NexoraMainVM"
private const val STOP_TIMEOUT_MS = 5000L

@OptIn(ExperimentalCoroutinesApi::class)
class NexoraMainViewModel(
    val coreRepo: NexoraCoreRepository,
    val modulesRepo: NexoraModulesRepository,
    private val prefsRepo: PreferencesRepository,
    private val aiService: GeminiAiService,
    private val currentUserId: String,
    userDisplayName: String,
    userEmail: String
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _briefingLoading = MutableStateFlow(false)
    val briefingLoading: StateFlow<Boolean> = _briefingLoading.asStateFlow()

    private val _selectedFamilySpaceId = MutableStateFlow<String?>(null)

    val language: StateFlow<AppLanguage> = prefsRepo.languageFlow
        .map { AppLanguage.fromCode(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppLanguage.BN)

    val darkTheme: StateFlow<Boolean> = prefsRepo.darkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

    val offlineSmartMode: StateFlow<Boolean> = prefsRepo.offlineSmartModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

    val offlineScratchpad: StateFlow<String> = prefsRepo.offlineScratchpadFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), "")

    val cachedBriefing: StateFlow<String> = prefsRepo.cachedBriefingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), "")

    val adaptiveModules: StateFlow<List<ToolModule>> = prefsRepo.toolUsageCountsFlow
        .map { counts ->
            ToolModule.entries.sortedByDescending { counts[it.route] ?: 0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ToolModule.entries)

    val userProfile: StateFlow<UiState<UserProfile?>> =
        unwrapStateFlow(coreRepo.observeUserProfile(currentUserId), "profile")

    val conversations: StateFlow<UiState<List<ConversationSummary>>> =
        unwrapStateFlow(coreRepo.observeConversations(currentUserId), "conversations")

    val tasks: StateFlow<UiState<List<LifeTask>>> =
        unwrapStateFlow(coreRepo.observeTasks(currentUserId), "tasks")

    val transactions: StateFlow<UiState<List<FinanceTransaction>>> =
        unwrapStateFlow(modulesRepo.observeTransactions(currentUserId), "transactions")

    val savingsGoals: StateFlow<UiState<List<SavingsGoal>>> =
        unwrapStateFlow(modulesRepo.observeSavingsGoals(currentUserId), "savings goals")

    val healthRecords: StateFlow<UiState<List<HealthRecord>>> =
        unwrapStateFlow(modulesRepo.observeHealthRecords(currentUserId), "health records")

    val memories: StateFlow<UiState<List<PersonalMemory>>> =
        unwrapStateFlow(coreRepo.observeMemories(currentUserId), "memories")

    val shoppingItems: StateFlow<UiState<List<ShoppingItem>>> =
        unwrapStateFlow(modulesRepo.observeShoppingItems(currentUserId), "shopping items")

    val emergencyContacts: StateFlow<UiState<List<EmergencyContact>>> =
        unwrapStateFlow(modulesRepo.observeEmergencyContacts(currentUserId), "emergency contacts")

    val smartDevices: StateFlow<UiState<List<SmartDevice>>> =
        unwrapStateFlow(modulesRepo.observeSmartDevices(currentUserId), "smart devices")

    val savedDocuments: StateFlow<UiState<List<SavedDocument>>> =
        unwrapStateFlow(coreRepo.observeSavedDocuments(currentUserId), "saved documents")

    val familySpaces: StateFlow<UiState<List<FamilySpace>>> =
        unwrapStateFlow(modulesRepo.observeFamilySpaces(currentUserId), "family spaces")

    val activeFamilySpace: StateFlow<FamilySpace?> = combine(familySpaces, _selectedFamilySpaceId) { state, selectedId ->
        val list = (state as? UiState.Success)?.data.orEmpty()
        list.firstOrNull { it.id == selectedId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val activeFamilyItems: StateFlow<UiState<List<FamilyItem>>> = activeFamilySpace
        .flatMapLatest { space ->
            if (space == null) flowOf(UiState.Success(emptyList()))
            else unwrapRawFlow(modulesRepo.observeFamilyItems(space.id, currentUserId), "family items")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    init {
        viewModelScope.launch {
            coreRepo.ensureUserProfile(userDisplayName, userEmail, language.value.code)
        }
    }

    private fun <T> unwrapRawFlow(source: Flow<T>, label: String): Flow<UiState<T>> =
        source
            .map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }

    private fun <T> unwrapStateFlow(source: Flow<T>, label: String): StateFlow<UiState<T>> =
        unwrapRawFlow(source, label).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showBanner(message: String) {
        _statusBanner.value = message
    }

    fun toggleLanguage() {
        val next = if (language.value == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN
        viewModelScope.launch {
            prefsRepo.setLanguage(next.code)
            coreRepo.updateProfilePreferences(language = next.code)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setDarkTheme(enabled)
            coreRepo.updateProfilePreferences(darkTheme = enabled)
        }
    }

    fun setOfflineSmartMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setOfflineSmartMode(enabled)
            coreRepo.updateProfilePreferences(offlineSmartMode = enabled)
        }
    }

    fun saveOfflineScratchpad(text: String) {
        viewModelScope.launch { prefsRepo.saveOfflineScratchpad(text) }
    }

    fun updateMonthlyBudget(amount: Double) {
        viewModelScope.launch {
            val res = coreRepo.updateProfilePreferences(monthlyBudgetBdt = amount)
            if (res.isFailure) _statusBanner.value = res.exceptionOrNull()?.message
        }
    }

    fun selectPlanTier(tier: String) {
        viewModelScope.launch {
            val res = coreRepo.updateProfilePreferences(planTier = tier)
            _statusBanner.value = if (res.isSuccess) "Plan updated to $tier" else res.exceptionOrNull()?.message
        }
    }

    fun trackToolOpened(route: String) {
        viewModelScope.launch { prefsRepo.incrementToolUsage(route) }
    }

    fun selectFamilySpace(spaceId: String) {
        _selectedFamilySpaceId.value = spaceId
    }

    fun generateMorningBriefing() {
        if (_briefingLoading.value) return
        viewModelScope.launch {
            _briefingLoading.value = true
            try {
                val profileName = (userProfile.value as? UiState.Success)?.data?.displayName ?: "Friend"
                val taskList = (tasks.value as? UiState.Success)?.data.orEmpty()
                val healthList = (healthRecords.value as? UiState.Success)?.data.orEmpty()
                val memList = (memories.value as? UiState.Success)?.data.orEmpty()
                val briefing = aiService.generateMorningBriefing(
                    displayName = profileName,
                    tasks = taskList,
                    healthRecords = healthList,
                    memories = memList,
                    language = language.value
                )
                prefsRepo.saveCachedBriefing(briefing)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Briefing timed out", e)
                _statusBanner.value = "Briefing generation timed out. Please retry."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Briefing failed", e)
                _statusBanner.value = e.localizedMessage ?: "Failed to generate briefing."
            } finally {
                _briefingLoading.value = false
            }
        }
    }

    fun runRepoAction(action: suspend () -> Result<Unit>, successMsg: String? = null) {
        viewModelScope.launch {
            val result = action()
            if (result.isSuccess && successMsg != null) {
                _statusBanner.value = successMsg
            } else if (result.isFailure) {
                _statusBanner.value = result.exceptionOrNull()?.localizedMessage ?: "Operation failed"
            }
        }
    }
}

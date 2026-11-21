package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ToolModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nexoraDataStore by preferencesDataStore(name = "nexora_prefs")

class PreferencesRepository(private val context: Context) {
    private val langKey = stringPreferencesKey("language_code")
    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    private val offlineModeKey = booleanPreferencesKey("offline_smart_mode")
    private val offlineScratchpadKey = stringPreferencesKey("offline_scratchpad")
    private val cachedBriefingKey = stringPreferencesKey("cached_briefing")

    val languageFlow: Flow<String> = context.nexoraDataStore.data.map { prefs ->
        prefs[langKey] ?: "bn"
    }

    val darkThemeFlow: Flow<Boolean> = context.nexoraDataStore.data.map { prefs ->
        prefs[darkThemeKey] ?: true
    }

    val offlineSmartModeFlow: Flow<Boolean> = context.nexoraDataStore.data.map { prefs ->
        prefs[offlineModeKey] ?: true
    }

    val offlineScratchpadFlow: Flow<String> = context.nexoraDataStore.data.map { prefs ->
        prefs[offlineScratchpadKey] ?: ""
    }

    val cachedBriefingFlow: Flow<String> = context.nexoraDataStore.data.map { prefs ->
        prefs[cachedBriefingKey] ?: ""
    }

    val toolUsageCountsFlow: Flow<Map<String, Int>> = context.nexoraDataStore.data.map { prefs ->
        ToolModule.entries.associate { module ->
            module.route to (prefs[intPreferencesKey("usage_${module.route}")] ?: 0)
        }
    }

    suspend fun setLanguage(code: String) {
        context.nexoraDataStore.edit { it[langKey] = code }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.nexoraDataStore.edit { it[darkThemeKey] = enabled }
    }

    suspend fun setOfflineSmartMode(enabled: Boolean) {
        context.nexoraDataStore.edit { it[offlineModeKey] = enabled }
    }

    suspend fun saveOfflineScratchpad(text: String) {
        context.nexoraDataStore.edit { it[offlineScratchpadKey] = text }
    }

    suspend fun saveCachedBriefing(text: String) {
        context.nexoraDataStore.edit { it[cachedBriefingKey] = text }
    }

    suspend fun incrementToolUsage(route: String) {
        val key = intPreferencesKey("usage_$route")
        context.nexoraDataStore.edit { prefs ->
            prefs[key] = (prefs[key] ?: 0) + 1
        }
    }
}

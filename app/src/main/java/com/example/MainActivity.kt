package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.example.data.repository.PreferencesRepository
import com.example.firebase.configureAppCheck
import com.example.ui.navigation.AppContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must run synchronously before Firebase.ai / FirebaseFirestore / ViewModels initialize
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val prefsRepo = remember(applicationContext) { PreferencesRepository(applicationContext) }
            AppContent(prefsRepo = prefsRepo)
        }
    }
}

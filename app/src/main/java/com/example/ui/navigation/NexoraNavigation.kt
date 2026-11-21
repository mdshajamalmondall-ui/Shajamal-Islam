package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.ToolModule
import com.example.data.repository.GeminiAiService
import com.example.data.repository.NexoraCoreRepository
import com.example.data.repository.NexoraModulesRepository
import com.example.data.repository.PreferencesRepository
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.authStateFlow
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import com.example.ui.screens.AiBrainScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.ToolsHubScreen
import com.example.ui.screens.modules.AiVisionScreen
import com.example.ui.screens.modules.DocumentAiScreen
import com.example.ui.screens.modules.EducationCenterScreen
import com.example.ui.screens.modules.EmergencySafetyScreen
import com.example.ui.screens.modules.FamilySpaceScreen
import com.example.ui.screens.modules.FinanceManagerScreen
import com.example.ui.screens.modules.HealthAssistantScreen
import com.example.ui.screens.modules.LifeManagerScreen
import com.example.ui.screens.modules.MeetingIntelligenceScreen
import com.example.ui.screens.modules.MemoryDigitalTwinScreen
import com.example.ui.screens.modules.ProfileScreen
import com.example.ui.screens.modules.ShoppingAssistantScreen
import com.example.ui.screens.modules.SmartCommunicationScreen
import com.example.ui.screens.modules.SmartHomeScreen
import com.example.ui.screens.modules.TranslatorScreen
import com.example.ui.screens.modules.TravelPlannerScreen
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.NexoraTheme
import com.example.ui.viewmodel.AiBrainViewModel
import com.example.ui.viewmodel.NexoraMainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_AI = "ai"
private const val ROUTE_TOOLS = "tools"
private const val ROUTE_PROFILE = "profile"

private val TOP_LEVEL_ROUTES = setOf(
    ROUTE_HOME,
    ROUTE_SEARCH,
    ROUTE_AI,
    ROUTE_TOOLS,
    ROUTE_PROFILE
)

private data class NavTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AppContent(prefsRepo: PreferencesRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val darkTheme by prefsRepo.darkThemeFlow.collectAsStateWithLifecycle(initialValue = true)
    val languageCode by prefsRepo.languageFlow.collectAsStateWithLifecycle(initialValue = "bn")
    val language = AppLanguage.fromCode(languageCode)

    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        val credentialManager = CredentialManager.create(context)
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = this
        )
    }

    NexoraTheme(darkTheme = darkTheme) {
        val user = currentUser
        if (user == null) {
            AuthScreen(
                language = language,
                onToggleLanguage = {
                    val next = if (language == AppLanguage.BN) AppLanguage.EN.code else AppLanguage.BN.code
                    scope.launch { prefsRepo.setLanguage(next) }
                },
                onAuthSuccess = {}
            )
        } else {
            key(user.uid) {
                NexoraAuthenticatedApp(
                    user = user,
                    prefsRepo = prefsRepo
                )
            }
        }
    }
}

@Composable
private fun NexoraAuthenticatedApp(
    user: FirebaseUser,
    prefsRepo: PreferencesRepository
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val db = remember(appContext) {
        FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
    }
    val coreRepo = remember(db) { NexoraCoreRepository(db) }
    val modulesRepo = remember(db) { NexoraModulesRepository(db) }
    val aiService = remember { GeminiAiService() }

    val mainViewModel: NexoraMainViewModel = viewModel(
        key = "main_${user.uid}",
        factory = viewModelFactory {
            initializer {
                NexoraMainViewModel(
                    coreRepo = coreRepo,
                    modulesRepo = modulesRepo,
                    prefsRepo = prefsRepo,
                    aiService = aiService,
                    currentUserId = user.uid,
                    userDisplayName = user.displayName ?: "Friend",
                    userEmail = user.email ?: ""
                )
            }
        }
    )

    val aiViewModel: AiBrainViewModel = viewModel(
        key = "ai_${user.uid}",
        factory = viewModelFactory {
            initializer {
                AiBrainViewModel(
                    aiService = aiService,
                    coreRepo = coreRepo,
                    modulesRepo = modulesRepo,
                    currentUserId = user.uid
                )
            }
        }
    )

    val language by mainViewModel.language.collectAsStateWithLifecycle()
    val statusBanner by mainViewModel.statusBanner.collectAsStateWithLifecycle()
    val s = stringsFor(language)

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME
    val isTopLevel = currentRoute in TOP_LEVEL_ROUTES

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearBanner()
        }
    }

    var pendingAiPrompt by remember { mutableStateOf("") }
    var pendingStartVoice by remember { mutableStateOf(false) }

    fun navigateToTab(route: String) {
        if (route != ROUTE_AI) {
            pendingStartVoice = false
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openAiBrain(prompt: String = "", startVoice: Boolean = false) {
        pendingAiPrompt = prompt
        pendingStartVoice = startVoice
        navigateToTab(ROUTE_AI)
    }

    fun openToolModule(module: ToolModule) {
        mainViewModel.trackToolOpened(module.route)
        navController.navigate(module.route) {
            launchSingleTop = true
        }
    }

    val tabs = listOf(
        NavTabItem(ROUTE_HOME, s.navHome, Icons.Filled.Home, Icons.Outlined.Home),
        NavTabItem(ROUTE_SEARCH, s.navSearch, Icons.Filled.Search, Icons.Outlined.Search),
        NavTabItem(ROUTE_AI, s.navAi, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
        NavTabItem(ROUTE_TOOLS, s.navTools, Icons.Filled.Apps, Icons.Outlined.Apps),
        NavTabItem(ROUTE_PROFILE, s.navProfile, Icons.Filled.Person, Icons.Outlined.Person)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = if (isTopLevel) WindowInsets.statusBars else WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isTopLevel && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPulse,
                                    selectedTextColor = CyanPulse,
                                    indicatorColor = CyanPulse.copy(alpha = 0.16f)
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevel && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                mainViewModel = mainViewModel,
                                onAskAi = { prompt, startVoice ->
                                    openAiBrain(prompt = prompt, startVoice = startVoice)
                                },
                                onOpenConversation = { convId ->
                                    aiViewModel.selectConversation(convId)
                                    openAiBrain()
                                },
                                onOpenTool = { module -> openToolModule(module) },
                                onOpenProfile = { navigateToTab(ROUTE_PROFILE) }
                            )
                        }

                        composable(ROUTE_SEARCH) {
                            SearchScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onContinueInChat = { query ->
                                    openAiBrain(prompt = query, startVoice = false)
                                }
                            )
                        }

                        composable(ROUTE_AI) {
                            AiBrainScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                initialPrompt = pendingAiPrompt,
                                startVoiceOnEntry = pendingStartVoice
                            )
                        }

                        composable(ROUTE_TOOLS) {
                            ToolsHubScreen(
                                mainViewModel = mainViewModel,
                                onOpenTool = { module -> openToolModule(module) }
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                mainViewModel = mainViewModel,
                                onOpenMemories = { openToolModule(ToolModule.MEMORY_TWIN) }
                            )
                        }

                        composable(ToolModule.LIFE_MANAGER.route) {
                            LifeManagerScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.HEALTH_ASSISTANT.route) {
                            HealthAssistantScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.EDUCATION_CENTER.route) {
                            EducationCenterScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.FINANCE_MANAGER.route) {
                            FinanceManagerScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.DOCUMENT_AI.route) {
                            DocumentAiScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.TRANSLATOR.route) {
                            TranslatorScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.EMERGENCY_SAFETY.route) {
                            EmergencySafetyScreen(
                                mainViewModel = mainViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.AI_VISION.route) {
                            AiVisionScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.SHOPPING_ASSISTANT.route) {
                            ShoppingAssistantScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.TRAVEL_PLANNER.route) {
                            TravelPlannerScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.SMART_COMMUNICATION.route) {
                            SmartCommunicationScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.SMART_HOME.route) {
                            SmartHomeScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.MEETING_INTELLIGENCE.route) {
                            MeetingIntelligenceScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.FAMILY_SPACE.route) {
                            FamilySpaceScreen(
                                mainViewModel = mainViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ToolModule.MEMORY_TWIN.route) {
                            MemoryDigitalTwinScreen(
                                mainViewModel = mainViewModel,
                                aiViewModel = aiViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

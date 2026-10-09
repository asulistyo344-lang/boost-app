package com.boost.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boost.app.data.api.*
import com.boost.app.data.repository.BoostRepository
import com.boost.app.data.repository.parseApiError
import com.boost.app.ui.screens.*
import com.boost.app.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── NAVIGATION ───────────────────────────────────────────────────
sealed class Screen {
    object Onboarding  : Screen()
    object Home        : Screen()
    object Capture     : Screen()
    data class Context(val uri: Uri, val isVideo: Boolean) : Screen()
    object Generating  : Screen()
    object Results     : Screen()
    object Trending    : Screen()
    object History     : Screen()
    object Account     : Screen()
    object Upgrade     : Screen()
}

// ─── VIEW MODEL ───────────────────────────────────────────────────
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: BoostRepository,
) : ViewModel() {

    // Device state
    private val _deviceId = MutableStateFlow<String?>(null)
    val deviceId: StateFlow<String?> = _deviceId

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro

    // UI state
    private val _quota    = MutableStateFlow<QuotaResponse?>(null)
    val quota: StateFlow<QuotaResponse?> = _quota

    private val _projects = MutableStateFlow<List<ProjectSummary>>(emptyList())
    val projects: StateFlow<List<ProjectSummary>> = _projects

    private val _trending = MutableStateFlow<List<TrendItem>>(emptyList())
    val trending: StateFlow<List<TrendItem>> = _trending

    // Generation results
    private val _analysis = MutableStateFlow<AnalyzeResponse?>(null)
    val analysis: StateFlow<AnalyzeResponse?> = _analysis

    private val _images = MutableStateFlow<List<ImageResult>>(emptyList())
    val images: StateFlow<List<ImageResult>> = _images

    private val _videos = MutableStateFlow<List<VideoResult>>(emptyList())
    val videos: StateFlow<List<VideoResult>> = _videos

    // Error — Aura/Nebula lesson: always specific messages
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    init {
        viewModelScope.launch {
            val id = repo.getOrCreateDeviceId()
            _deviceId.value = id

            repo.isProFlow.collect { _isPro.value = it }
        }
        viewModelScope.launch {
            val id = repo.getOrCreateDeviceId()
            repo.registerDevice(id)
            loadQuota()
            loadProjects()
        }
    }

    private fun loadQuota() = viewModelScope.launch {
        val id = _deviceId.value ?: return@launch
        repo.getQuota(id).onSuccess { _quota.value = it }
    }

    private fun loadProjects() = viewModelScope.launch {
        val id = _deviceId.value ?: return@launch
        repo.listProjects(id).onSuccess { _projects.value = it }
    }

    fun loadTrending() = viewModelScope.launch {
        repo.getTrending(_deviceId.value).onSuccess { _trending.value = it }
    }

    fun clearError() { _error.value = null }

    fun analyze(
        imageUri: Uri,
        videoUri: Uri?,
        niche: String,
        vibe: String,
        platform: String,
        productName: String?,
        onSuccess: () -> Unit,
    ) = viewModelScope.launch {
        val id = _deviceId.value ?: return@launch
        _loading.value = true
        _error.value = null

        try {
            val imageB64 = repo.uriToBase64(imageUri)
            val videoB64 = videoUri?.let {
                try { repo.videoUriToBase64(it) }
                catch (e: Exception) { null }
            }

            val req = AnalyzeRequest(
                deviceId = id,
                imageB64 = imageB64,
                productName = productName,
                niche = niche, vibe = vibe, platform = platform,
            )

            repo.analyze(req).onSuccess { analysis ->
                _analysis.value = analysis

                // Generate images in parallel
                val imgReq = GenerateImagesRequest(
                    deviceId = id,
                    projectId = analysis.projectId,
                    imageB64 = imageB64,
                    productContext = analysis.strategy.angle,
                )
                repo.generateImages(imgReq).onSuccess { _images.value = it.images }

                // Generate videos
                val firstCaption = analysis.captions.firstOrNull()
                if (firstCaption != null) {
                    val vidReq = GenerateVideoRequest(
                        deviceId = id,
                        projectId = analysis.projectId,
                        videoB64 = videoB64,
                        imageB64 = imageB64,
                        hookText = firstCaption.hook,
                        caption  = firstCaption.caption,
                    )
                    repo.generateVideo(vidReq).onSuccess { _videos.value = it.videos }
                }

                loadQuota()
                loadProjects()
                onSuccess()

            }.onFailure { e ->
                _error.value = parseApiError(e as Exception)
            }

        } catch (e: Exception) {
            _error.value = parseApiError(e)
        } finally {
            _loading.value = false
        }
    }

    fun verifySubscription(purchaseToken: String) = viewModelScope.launch {
        val id = _deviceId.value ?: return@launch
        repo.verifySubscription(
            VerifySubscriptionRequest(deviceId = id, purchaseToken = purchaseToken)
        ).onSuccess {
            _isPro.value = it.isPro
        }.onFailure { e ->
            _error.value = parseApiError(e as Exception)
        }
    }
}

// ─── MAIN ACTIVITY ────────────────────────────────────────────────
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoostTheme {
                BoostApp()
            }
        }
    }
}

// ─── ROOT COMPOSABLE ──────────────────────────────────────────────
@Composable
fun BoostApp(vm: MainViewModel = hiltViewModel()) {
    var screen     by remember { mutableStateOf<Screen>(Screen.Onboarding) }
    var activeTab  by remember { mutableStateOf(0) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    var isVideo    by remember { mutableStateOf(false) }

    val isPro    by vm.isPro.collectAsState()
    val quota    by vm.quota.collectAsState()
    val projects by vm.projects.collectAsState()
    val trending by vm.trending.collectAsState()
    val analysis by vm.analysis.collectAsState()
    val images   by vm.images.collectAsState()
    val videos   by vm.videos.collectAsState()
    val loading  by vm.loading.collectAsState()
    val error    by vm.error.collectAsState()

    Scaffold(
        bottomBar = {
            if (screen in listOf(Screen.Home, Screen.Trending, Screen.History, Screen.Account)) {
                BottomNavBar(activeTab) { idx ->
                    activeTab = idx
                    screen = when(idx) {
                        0 -> Screen.Home
                        1 -> { vm.loadTrending(); Screen.Trending }
                        2 -> Screen.History
                        3 -> Screen.Account
                        else -> Screen.Home
                    }
                }
            }
        },
        containerColor = BoostColors.Bg,
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (val s = screen) {
                Screen.Onboarding -> OnboardingScreen { screen = Screen.Home }

                Screen.Home -> HomeScreen(
                    isPro = isPro,
                    quota = quota,
                    projects = projects,
                    onNewContent = { screen = Screen.Capture },
                    onUpgrade = { screen = Screen.Upgrade },
                    onProjectClick = { screen = Screen.Results },
                )

                Screen.Capture -> CaptureScreen(
                    onCapture = { uri, vid ->
                        captureUri = uri
                        isVideo = vid
                        screen = Screen.Context(uri, vid)
                    },
                    onBack = { screen = Screen.Home },
                )

                is Screen.Context -> ContextScreen(
                    captureUri = s.uri,
                    isVideo = s.isVideo,
                    onAnalyze = { niche, vibe, platform, name ->
                        screen = Screen.Generating
                        vm.analyze(
                            imageUri = s.uri,
                            videoUri = if (s.isVideo) s.uri else null,
                            niche = niche, vibe = vibe, platform = platform,
                            productName = name,
                            onSuccess = { screen = Screen.Results },
                        )
                    },
                    onBack = { screen = Screen.Capture },
                )

                Screen.Generating -> GeneratingScreen()

                Screen.Results -> {
                    val a = analysis
                    if (a != null) {
                        ResultsScreen(
                            analysis = a,
                            images = images,
                            videos = videos,
                            onBack = { screen = Screen.Home; activeTab = 0 },
                            onSaveAll = { /* trigger download */ },
                        )
                    }
                }

                Screen.Trending -> TrendingScreenComposable(
                    isPro = isPro,
                    trending = trending,
                    onCreateForTrend = { screen = Screen.Capture },
                    onUpgrade = { screen = Screen.Upgrade },
                )

                Screen.History -> HistoryScreenComposable(
                    projects = projects,
                    onProjectClick = { screen = Screen.Results },
                )

                Screen.Account -> AccountScreenComposable(
                    isPro = isPro, quota = quota,
                    onUpgrade = { screen = Screen.Upgrade },
                )

                Screen.Upgrade -> UpgradeScreenComposable(
                    onBack = { screen = Screen.Home },
                    onPurchased = { token -> vm.verifySubscription(token) },
                )
            }

            // Error snackbar — specific messages, never generic (Nebula lesson)
            error?.let { msg ->
                Snackbar(
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                        .padding(16.dp),
                    dismissAction = {
                        TextButton(onClick = { vm.clearError() }) {
                            Text("Dismiss", fontSize = 12.sp)
                        }
                    },
                    containerColor = BoostColors.Red.copy(alpha = 0.9f),
                ) {
                    Text(msg, fontSize = 13.sp, color = BoostColors.Text)
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(active: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf("⚡" to "Home", "🔥" to "Trending",
                      "📁" to "Projects", "👤" to "Account")
    NavigationBar(containerColor = BoostColors.Surface,
        tonalElevation = 0.dp) {
        tabs.forEachIndexed { i, (icon, label) ->
            NavigationBarItem(
                selected = active == i,
                onClick = { onSelect(i) },
                icon = { Text(icon, fontSize = 18.sp) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor   = BoostColors.Orange,
                    selectedIconColor   = BoostColors.Orange,
                    unselectedTextColor = BoostColors.TextDim,
                    unselectedIconColor = BoostColors.TextDim,
                    indicatorColor      = BoostColors.Orange.copy(alpha = 0.15f),
                ),
            )
        }
    }
}

// Stub composables for remaining screens (full implementation follows same pattern)
@Composable fun TrendingScreenComposable(isPro: Boolean, trending: List<TrendItem>,
    onCreateForTrend: () -> Unit, onUpgrade: () -> Unit) { /* See TrendingScreen.kt */ }
@Composable fun HistoryScreenComposable(projects: List<ProjectSummary>,
    onProjectClick: (ProjectSummary) -> Unit) { /* See HistoryScreen.kt */ }
@Composable fun AccountScreenComposable(isPro: Boolean, quota: QuotaResponse?,
    onUpgrade: () -> Unit) { /* See AccountScreen.kt */ }
@Composable fun UpgradeScreenComposable(onBack: () -> Unit,
    onPurchased: (String) -> Unit) { /* See UpgradeScreen.kt */ }

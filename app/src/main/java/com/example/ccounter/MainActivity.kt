package com.example.ccounter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val Bg = Color(0xFF121212)
private val CardBg = Color(0xFF1E1E1E)
private val Border = Color(0xFF2A2A2A)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF9E9E9E)
private val TextMuted = Color(0xFF6B6B6B)
private val Accent = Color(0xFF4CAF50)
private val Danger = Color(0xFFFF5252)

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private var pendingOpenRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingOpenRoute = intent?.getStringExtra(EXTRA_OPEN_ROUTE)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                CCounterApp(
                    viewModel = mainViewModel,
                    pendingRoute = pendingOpenRoute,
                    onPendingRouteConsumed = { pendingOpenRoute = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingOpenRoute = intent.getStringExtra(EXTRA_OPEN_ROUTE)
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Accent,
            background = Bg,
            surface = CardBg,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
        ),
        content = content,
    )
}

object Routes {
    const val Landing = "landing"
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Log = "log"
    const val Stats = "stats"
    const val Weight = "weight"
    const val Profile = "profile"
    const val Notifications = "notifications"
    const val AddMeal = "add_meal"
    const val AiResult = "ai_result"
}

private fun tr(language: AppLanguage, en: String, ru: String, uk: String): String = when (language) {
    AppLanguage.ENGLISH -> en
    AppLanguage.RUSSIAN -> ru
    AppLanguage.UKRAINIAN -> uk
}

@Composable
private fun CCounterApp(
    viewModel: MainViewModel,
    pendingRoute: String?,
    onPendingRouteConsumed: () -> Unit,
) {
    val appData = viewModel.appData
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val mainRoutes = setOf(Routes.Home, Routes.Log, Routes.Stats, Routes.Weight, Routes.Profile)
    val swipeRoutes = listOf(Routes.Home, Routes.Stats, Routes.Weight, Routes.Profile)
    val startRoute = if (appData.onboardingCompleted) Routes.Home else Routes.Landing
    val isInternetAvailable by rememberInternetAvailableState()
    var dragAccum by remember(currentRoute) { mutableFloatStateOf(0f) }

    LaunchedEffect(pendingRoute, appData.onboardingCompleted) {
        if (pendingRoute.isNullOrBlank()) return@LaunchedEffect
        if (!appData.onboardingCompleted) return@LaunchedEffect
        val route = when (pendingRoute) {
            Routes.AddMeal -> Routes.AddMeal
            Routes.Weight -> Routes.Weight
            else -> null
        }
        if (route != null) {
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
        onPendingRouteConsumed()
    }

    LaunchedEffect(isInternetAvailable, appData.onboardingCompleted, appData.deviceId) {
        if (!isInternetAvailable) return@LaunchedEffect
        viewModel.refreshRemotePolicy(force = true)
        while (isInternetAvailable) {
            delay(60_000)
            viewModel.refreshRemotePolicy(force = false)
        }
    }

    val swipeModifier = if (currentRoute in swipeRoutes && isInternetAvailable) {
        Modifier.pointerInput(currentRoute) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, dragAmount ->
                    dragAccum += dragAmount
                },
                onDragEnd = {
                    val currentIndex = swipeRoutes.indexOf(currentRoute)
                    if (currentIndex >= 0) {
                        when {
                            dragAccum > 90f && currentIndex > 0 -> {
                                val prev = swipeRoutes[currentIndex - 1]
                                navController.navigate(prev) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            dragAccum < -90f && currentIndex < swipeRoutes.lastIndex -> {
                                val next = swipeRoutes[currentIndex + 1]
                                navController.navigate(next) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                    dragAccum = 0f
                },
                onDragCancel = {
                    dragAccum = 0f
                },
            )
        }
    } else {
        Modifier
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                if (currentRoute in mainRoutes) {
                    AppBottomBar(
                        navController = navController,
                        currentRoute = currentRoute ?: Routes.Home,
                        language = appData.language,
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = startRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .then(swipeModifier)
                    .padding(bottom = padding.calculateBottomPadding()),
            ) {
                composable(Routes.Landing) {
                    LandingScreen(
                        onStart = { navController.navigate(Routes.Onboarding) },
                    )
                }
                composable(Routes.Onboarding) {
                    OnboardingScreen(
                        initialLanguage = appData.language,
                        onComplete = { profile, selectedLanguage ->
                            viewModel.completeOnboarding(profile, selectedLanguage)
                            navController.navigate(Routes.Home) {
                                popUpTo(Routes.Landing) { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.Home) {
                    HomeScreen(
                        appData = appData,
                        meals = viewModel.todaysMeals(),
                        promptQuotaInfo = viewModel.promptQuotaInfo,
                        onAddMeal = { navController.navigate(Routes.AddMeal) },
                        onOpenLog = { navController.navigate(Routes.Log) },
                        onUpdateMeal = { id, name, description, kcal, protein, carbs, fat, timestamp, photoDataUrl ->
                            viewModel.updateMeal(id, name, description, kcal, protein, carbs, fat, timestamp, photoDataUrl)
                        },
                        onDeleteMeal = { id -> viewModel.deleteMeal(id) },
                    )
                }
                composable(Routes.Log) {
                    DailyLogScreen(
                        meals = viewModel.todaysMeals(),
                        target = appData.profile.dailyTargetCalories,
                        onAddMeal = { navController.navigate(Routes.AddMeal) },
                    )
                }
                composable(Routes.Stats) {
                    StatsScreen(
                        meals = appData.meals,
                        target = appData.profile.dailyTargetCalories,
                    )
                }
                composable(Routes.Weight) {
                    WeightScreen(
                        profile = appData.profile,
                        weights = appData.weights,
                        onAddWeight = { viewModel.addWeight(it) },
                        onUpdateWeight = { id, value, timestamp -> viewModel.updateWeight(id, value, timestamp) },
                        onDeleteWeight = { id -> viewModel.deleteWeight(id) },
                    )
                }
                composable(Routes.Profile) {
                    ProfileScreen(
                        profile = appData.profile,
                        apiKey = appData.openAiApiKey,
                        language = appData.language,
                        onSaveProfile = { viewModel.updateProfile(it) },
                        onSaveApiKey = { viewModel.updateApiKey(it) },
                        onSaveLanguage = { viewModel.updateLanguage(it) },
                        onOpenNotifications = { navController.navigate(Routes.Notifications) },
                        onSignOut = {
                            viewModel.signOut()
                            navController.navigate(Routes.Landing) {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(Routes.Notifications) {
                    NotificationsScreen(
                        settings = appData.notifications,
                        onSave = { viewModel.updateNotificationSettings(it) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.AddMeal) {
                    AddMealScreen(
                        isAnalyzing = viewModel.isAnalyzingMeal,
                        analyzeError = viewModel.analyzeError,
                        initialApiKey = appData.openAiApiKey,
                        isApiKeyMissing = appData.openAiApiKey.isBlank(),
                        onDismissError = viewModel::clearAnalyzeError,
                        onSaveApiKey = viewModel::updateApiKey,
                        onBack = { navController.popBackStack() },
                        onSaveManual = { name, description, kcal, protein, carbs, fat ->
                            viewModel.addManualMeal(name, description, kcal, protein, carbs, fat)
                            navController.navigate(Routes.Home) {
                                popUpTo(Routes.Home) { inclusive = true }
                            }
                        },
                        onAnalyze = { description, imageDataUrl ->
                            viewModel.analyzeMeal(description, imageDataUrl) {
                                navController.navigate(Routes.AiResult)
                            }
                        },
                    )
                }
                composable(Routes.AiResult) {
                    AiResultScreen(
                        draft = viewModel.pendingAiDraft,
                        onBack = { navController.popBackStack() },
                        onUpdateDraft = { updated -> viewModel.updatePendingAiDraft(updated) },
                        onCalculateComponentCalories = { name, grams ->
                            viewModel.calculateComponentCalories(name, grams)
                        },
                        onConfirm = {
                            viewModel.confirmAiDraft()
                            navController.navigate(Routes.Home) {
                                popUpTo(Routes.Home) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }

        if (!isInternetAvailable) {
            AppBlockingOverlay(
                title = "Internet required",
                message = "Internet connection must stay enabled to use the app.",
                showRetry = false,
                onRetry = {},
            )
        } else if (appData.onboardingCompleted && !viewModel.isAccessAllowed) {
            AppBlockingOverlay(
                title = "Access required",
                message = viewModel.accessMessage ?: "Device is waiting for admin approval.",
                showRetry = true,
                onRetry = { viewModel.refreshRemotePolicy(force = true) },
            )
        }
    }
}

@Composable
private fun rememberInternetAvailableState(): androidx.compose.runtime.State<Boolean> {
    val context = LocalContext.current
    val connectivityManager = remember(context) {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    val isOnline = remember { mutableStateOf(isCurrentlyOnline(connectivityManager)) }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline.value = true
            }

            override fun onLost(network: Network) {
                isOnline.value = isCurrentlyOnline(connectivityManager)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                isOnline.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }
        runCatching { connectivityManager.registerDefaultNetworkCallback(callback) }
        onDispose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }

    return isOnline
}

private fun isCurrentlyOnline(connectivityManager: ConnectivityManager): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
private fun AppBlockingOverlay(
    title: String,
    message: String,
    showRetry: Boolean,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9121212)),
        contentAlignment = Alignment.Center,
    ) {
        CardBlock(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            background = CardBg,
            borderColor = Border,
        ) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = TextSecondary, fontSize = 14.sp)
            if (showRetry) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController, currentRoute: String, language: AppLanguage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(CardBg)
            .drawBehind {
                drawLine(
                    color = Border,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTabItem(
                icon = Icons.Filled.Home,
                label = tr(language, "Home", "Главная", "Головна"),
                selected = currentRoute == Routes.Home,
            ) {
                navController.navigate(Routes.Home) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            BottomTabItem(
                icon = Icons.Filled.BarChart,
                label = tr(language, "Stats", "Статистика", "Статистика"),
                selected = currentRoute == Routes.Stats,
            ) {
                navController.navigate(Routes.Stats) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            Spacer(modifier = Modifier.width(72.dp))

            BottomTabItem(
                icon = Icons.Filled.MonitorWeight,
                label = tr(language, "Weight", "Вес", "Вага"),
                selected = currentRoute == Routes.Weight,
            ) {
                navController.navigate(Routes.Weight) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            BottomTabItem(
                icon = Icons.Filled.Person,
                label = tr(language, "Profile", "Профиль", "Профіль"),
                selected = currentRoute == Routes.Profile,
            ) {
                navController.navigate(Routes.Profile) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        Button(
            onClick = { navController.navigate(Routes.AddMeal) },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(width = 58.dp, height = 58.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add meal", tint = Color.White)
        }
    }
}

@Composable
private fun BottomTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) Accent else TextMuted,
        )
        Text(
            label,
            color = if (selected) Accent else TextMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun LandingScreen(onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x40121212),
                            Color(0xB3121212),
                            Bg,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.logo_auth),
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("CCounter", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Column {
                Text(
                    "Track your calories effortlessly",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 38.sp,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Log meals with photo or text in seconds",
                    color = TextSecondary,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Get Started", modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    initialLanguage: AppLanguage,
    onComplete: (UserProfile, AppLanguage) -> Unit,
    onBack: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var selectedLanguage by rememberSaveable { mutableStateOf(initialLanguage) }
    var name by rememberSaveable { mutableStateOf("") }
    var profilePhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var goal by rememberSaveable { mutableStateOf(GoalType.MAINTAIN) }
    var gender by rememberSaveable { mutableStateOf(GenderType.MALE) }
    var age by rememberSaveable { mutableStateOf("25") }
    var height by rememberSaveable { mutableStateOf("170") }
    var weight by rememberSaveable { mutableStateOf("70") }
    var goalWeight by rememberSaveable { mutableStateOf("70") }
    var activity by rememberSaveable { mutableStateOf(ActivityLevel.MEDIUM) }
    val profilePhotoUri = profilePhotoUriString?.let(Uri::parse)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        profilePhotoUriString = uri?.toString()
    }

    val fallbackWeights = onboardingGoalDefaults(goal)
    val parsedWeight = weight.toDoubleOrNull() ?: fallbackWeights.first
    val parsedGoalWeight = goalWeight.toDoubleOrNull() ?: fallbackWeights.second
    val resolvedGoal = resolveGoalTypeByWeights(parsedWeight, parsedGoalWeight)

    val profileDraft = UserProfile(
        name = name.ifBlank { "User" },
        profilePhotoUri = profilePhotoUriString,
        goal = resolvedGoal,
        gender = gender,
        age = age.toIntOrNull() ?: 25,
        heightCm = height.toIntOrNull() ?: 170,
        weightKg = parsedWeight,
        activityLevel = activity,
        startWeightKg = parsedWeight,
        goalWeightKg = parsedGoalWeight,
    )
    val calculatedCalories = calculateDailyCalories(profileDraft)
    val canProceed = when (step) {
        4 -> name.isNotBlank()
        else -> true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    when {
                        step > 1 -> step -= 1
                        else -> onBack()
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(5) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(if (index + 1 == step) 24.dp else 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (index + 1 <= step) Accent else Border),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(40.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                when (step) {
                    1 -> {
                        Text("Language", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Choose app language", color = TextMuted)
                        Spacer(modifier = Modifier.height(24.dp))
                        AppLanguage.entries.forEach { language ->
                            SelectableCard(
                                selected = selectedLanguage == language,
                                title = language.label(),
                                description = when (language) {
                                    AppLanguage.ENGLISH -> "Use English interface"
                                    AppLanguage.RUSSIAN -> "Использовать русский интерфейс"
                                    AppLanguage.UKRAINIAN -> "Використовувати український інтерфейс"
                                },
                                onClick = { selectedLanguage = language },
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CardBlock(background = Color(0x1F4CAF50), borderColor = Color(0x334CAF50)) {
                            Text(
                                "You can always change language in Profile -> Settings -> Language.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    2 -> {
                        Text("What's your goal?", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("We'll personalize your calorie target", color = TextMuted)
                        Spacer(modifier = Modifier.height(24.dp))
                        GoalType.entries.forEach { item ->
                            SelectableCard(
                                selected = goal == item,
                                title = item.label(),
                                description = when (item) {
                                    GoalType.LOSE -> "Calorie deficit"
                                    GoalType.MAINTAIN -> "Stay balanced"
                                    GoalType.GAIN -> "Calorie surplus"
                                },
                                onClick = {
                                    goal = item
                                    val defaults = onboardingGoalDefaults(item)
                                    weight = defaults.first.toInt().toString()
                                    goalWeight = defaults.second.toInt().toString()
                                },
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                    3 -> {
                        Text("Activity level", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("How active are you daily?", color = TextMuted)
                        Spacer(modifier = Modifier.height(24.dp))
                        ActivityLevel.entries.forEach { item ->
                            SelectableCard(
                                selected = activity == item,
                                title = item.label(),
                                description = when (item) {
                                    ActivityLevel.LOW -> "Sedentary, mostly sitting"
                                    ActivityLevel.MEDIUM -> "Light exercise 3-5 days/week"
                                    ActivityLevel.HIGH -> "Intense exercise 6-7 days/week"
                                },
                                suffix = when (item) {
                                    ActivityLevel.LOW -> "x1.375"
                                    ActivityLevel.MEDIUM -> "x1.55"
                                    ActivityLevel.HIGH -> "x1.725"
                                },
                                onClick = { activity = item },
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                    4 -> {
                        Text("Profile", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add your name and photo", color = TextMuted)
                        Spacer(modifier = Modifier.height(16.dp))

                        CardBlock {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = profilePhotoUri ?: R.drawable.profile_default,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(20.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (name.isBlank()) "Add profile photo" else name,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text("Visible in your profile", color = TextMuted, fontSize = 12.sp)
                                }
                                TextButton(onClick = { picker.launch("image/*") }) {
                                    Text("Choose", color = Accent)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        DarkTextField("Name", name) { name = it }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Gender", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GenderType.entries.forEach { item ->
                                FilterButton(
                                    text = item.label(),
                                    selected = gender == item,
                                    modifier = Modifier.weight(1f),
                                    onClick = { gender = item },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        NumberInput("Age", age, "yrs") { age = it }
                        NumberInput("Height", height, "cm") { height = it }
                        NumberInput("Weight", weight, "kg") { weight = it }
                        NumberInput("Goal Weight", goalWeight, "kg") { goalWeight = it }
                    }
                    else -> {
                        Text("Your daily target", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Based on your profile", color = TextMuted)
                        Spacer(modifier = Modifier.height(20.dp))
                        CardBlock {
                            Text(
                                calculatedCalories.toString(),
                                color = Accent,
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("kcal / day", color = TextSecondary)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (step < 5) {
                        step += 1
                    } else {
                        onComplete(
                            profileDraft.copy(
                                dailyTargetCalories = calculatedCalories,
                            ),
                            selectedLanguage,
                        )
                    }
                },
                enabled = canProceed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canProceed) Accent else Border,
                    disabledContainerColor = Border,
                    disabledContentColor = TextSecondary,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Text(if (step == 5) "Continue" else "Next", modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun HomeScreen(
    appData: AppData,
    meals: List<MealEntry>,
    promptQuotaInfo: PromptQuotaInfo,
    onAddMeal: () -> Unit,
    onOpenLog: () -> Unit,
    onUpdateMeal: (Long, String, String, Int, Int, Int, Int, Long, String?) -> Unit,
    onDeleteMeal: (Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val todayStats = dayStats(meals)
    val target = appData.profile.dailyTargetCalories
    var editingMealId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editMealName by rememberSaveable { mutableStateOf("") }
    var editMealDescription by rememberSaveable { mutableStateOf("") }
    var editMealCalories by rememberSaveable { mutableStateOf("") }
    var editMealProtein by rememberSaveable { mutableStateOf("") }
    var editMealCarbs by rememberSaveable { mutableStateOf("") }
    var editMealFat by rememberSaveable { mutableStateOf("") }
    var editMealTimeMinutes by rememberSaveable { mutableIntStateOf(8 * 60) }
    var editMealPhotoDataUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var showMealTimePicker by rememberSaveable { mutableStateOf(false) }
    val editingMeal = meals.firstOrNull { it.id == editingMealId }

    val editPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val dataUrl = withContext(Dispatchers.IO) { uriToDataUrl(context, uri) }
                if (!dataUrl.isNullOrBlank()) {
                    editMealPhotoDataUrl = dataUrl
                }
            }
        }
    }

    LaunchedEffect(editingMealId, meals) {
        if (editingMealId != null && meals.none { it.id == editingMealId }) {
            editingMealId = null
        }
        if (editingMealId == null) {
            showMealTimePicker = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            // Keep content below status bar/cutout on devices with notches.
            .statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Good morning", color = TextMuted, fontSize = 13.sp)
                    Text(appData.profile.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${promptQuotaInfo.remainingToday}/80",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "${promptQuotaInfo.remainingWindow}/20",
                        color = TextMuted,
                        fontSize = 10.sp,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                AsyncImage(
                    model = appData.profile.profilePhotoUri?.let(Uri::parse) ?: R.drawable.profile_default,
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        item {
            CardBlock {
                CalorieRing(consumed = todayStats.consumed, target = target)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroCard("Protein", "${todayStats.protein}g", Icons.Outlined.LocalFireDepartment, Color(0xFF66BB6A), Modifier.weight(1f))
                MacroCard("Carbs", "${todayStats.carbs}g", Icons.Default.TextFields, Color(0xFF81C784), Modifier.weight(1f))
                MacroCard("Fat", "${todayStats.fat}g", Icons.Outlined.WaterDrop, Color(0xFF81C784), Modifier.weight(1f))
            }
        }
        item {
            CardBlock {
                Text("Today's intake", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                IntakeBars(meals = meals, target = target)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Today's meals", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "View all",
                    color = Accent,
                    modifier = Modifier.clickable(onClick = onOpenLog),
                )
            }
        }
        items(meals.sortedBy { it.timestamp }) { meal ->
            CardBlock(modifier = Modifier.clickable {
                editingMealId = meal.id
                editMealName = meal.name
                editMealDescription = meal.description
                editMealCalories = meal.totalKcal.toString()
                editMealProtein = meal.proteinG.toString()
                editMealCarbs = meal.carbsG.toString()
                editMealFat = meal.fatG.toString()
                editMealTimeMinutes = timeMinutesFromTimestamp(meal.timestamp)
                editMealPhotoDataUrl = meal.photoDataUrl
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mealEmoji(meal), fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(meal.name, color = Color.White, fontWeight = FontWeight.Medium)
                        Text(formatTime(meal.timestamp), color = TextMuted, fontSize = 12.sp)
                        Text(
                            "P ${meal.proteinG}g  C ${meal.carbsG}g  F ${meal.fatG}g",
                            color = TextMuted,
                            fontSize = 11.sp,
                        )
                    }
                    Text("${meal.totalKcal} kcal", color = Accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Button(
                onClick = onAddMeal,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add meal", modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }

    if (editingMeal != null && editingMealId != null) {
        val parsedKcal = editMealCalories.toIntOrNull()?.coerceAtLeast(0)
        val parsedProtein = editMealProtein.toIntOrNull()?.coerceAtLeast(0)
        val parsedCarbs = editMealCarbs.toIntOrNull()?.coerceAtLeast(0)
        val parsedFat = editMealFat.toIntOrNull()?.coerceAtLeast(0)
        val canSave = editMealName.isNotBlank() &&
            parsedKcal != null &&
            parsedProtein != null &&
            parsedCarbs != null &&
            parsedFat != null
        Dialog(
            onDismissRequest = { editingMealId = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 24.dp)
                    .heightIn(max = 620.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF232323), Color(0xFF171717)),
                        ),
                    )
                    .border(1.dp, Border, RoundedCornerShape(24.dp))
                    .padding(18.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val editMealPhotoModel = remember(editMealPhotoDataUrl) { resolveMealPhotoModel(editMealPhotoDataUrl) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Edit meal",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { editingMealId = null },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2D2D2D)),
                        ) {
                            Text("×", color = TextSecondary, fontSize = 20.sp, textAlign = TextAlign.Center)
                        }
                    }

                    if (editMealPhotoModel != null) {
                        AsyncImage(
                            model = editMealPhotoModel,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }

                    Button(
                        onClick = { editPhotoPicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242424)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (editMealPhotoModel == null) "Add photo" else "Change photo", color = Color.White)
                    }

                    DarkTextField("Meal name", editMealName) { editMealName = it }
                    DarkTextField("Description", editMealDescription, KeyboardType.Text, minLines = 3) { editMealDescription = it }
                    DarkTextField("Calories", editMealCalories, KeyboardType.Number) { editMealCalories = it }
                    DarkTextField("Protein (g)", editMealProtein, KeyboardType.Number) { editMealProtein = it }
                    DarkTextField("Carbs (g)", editMealCarbs, KeyboardType.Number) { editMealCarbs = it }
                    DarkTextField("Fat (g)", editMealFat, KeyboardType.Number) { editMealFat = it }
                    NotificationTimeRow(label = "Meal time", minutes = editMealTimeMinutes) {
                        showMealTimePicker = true
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onDeleteMeal(editingMeal.id)
                                editingMealId = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF242424),
                                contentColor = Accent,
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Accent),
                        ) {
                            Text("Delete")
                        }
                        Button(
                            onClick = {
                                val updatedTimestamp = combineDateAndMinutes(editingMeal.timestamp, editMealTimeMinutes)
                                onUpdateMeal(
                                    editingMeal.id,
                                    editMealName,
                                    editMealDescription,
                                    parsedKcal ?: editingMeal.totalKcal,
                                    parsedProtein ?: editingMeal.proteinG,
                                    parsedCarbs ?: editingMeal.carbsG,
                                    parsedFat ?: editingMeal.fatG,
                                    updatedTimestamp,
                                    editMealPhotoDataUrl,
                                )
                                editingMealId = null
                            },
                            enabled = canSave,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        ) {
                            Text("Save changes")
                        }
                    }
                }
            }
        }
    }

    if (showMealTimePicker && editingMealId != null) {
        AppTimePickerDialog(
            title = "Meal time",
            initialMinutes = editMealTimeMinutes,
            onDismiss = { showMealTimePicker = false },
            onSave = { minutes ->
                editMealTimeMinutes = minutes
                showMealTimePicker = false
            },
        )
    }
}

@Composable
private fun DailyLogScreen(meals: List<MealEntry>, target: Int, onAddMeal: () -> Unit) {
    val consumed = meals.sumOf { it.totalKcal }
    val totalProtein = meals.sumOf { it.proteinG }
    val totalCarbs = meals.sumOf { it.carbsG }
    val totalFat = meals.sumOf { it.fatG }
    val remaining = target - consumed
    val grouped = meals.groupBy { mealBucket(it.timestamp) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Daily Log", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM dd")), color = TextMuted, fontSize = 13.sp)
        }
        item {
            CardBlock {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SummaryCell("consumed", consumed.toString(), Accent)
                    SummaryCell("target", target.toString(), Color.White)
                    SummaryCell(if (remaining < 0) "over" else "remaining", abs(remaining).toString(), if (remaining < 0) Danger else Color(0xFF81C784))
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgress(progress = (consumed.toFloat() / target.toFloat()).coerceIn(0f, 1f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MacroCard(
                        label = "Protein",
                        value = "${totalProtein}g",
                        icon = Icons.Outlined.LocalFireDepartment,
                        color = Accent,
                        modifier = Modifier.weight(1f),
                    )
                    MacroCard(
                        label = "Carbs",
                        value = "${totalCarbs}g",
                        icon = Icons.Default.TextFields,
                        color = Accent,
                        modifier = Modifier.weight(1f),
                    )
                    MacroCard(
                        label = "Fat",
                        value = "${totalFat}g",
                        icon = Icons.Outlined.WaterDrop,
                        color = Accent,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        grouped.forEach { (bucket, items) ->
            item {
                CardBlock {
                    val bucketProtein = items.sumOf { it.proteinG }
                    val bucketCarbs = items.sumOf { it.carbsG }
                    val bucketFat = items.sumOf { it.fatG }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(bucket, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${items.sumOf { it.totalKcal }} kcal", color = Accent, fontWeight = FontWeight.Bold)
                            Text(
                                "P ${bucketProtein}g  C ${bucketCarbs}g  F ${bucketFat}g",
                                color = TextMuted,
                                fontSize = 11.sp,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onAddMeal) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Accent)
                        }
                    }
                    Divider(color = Border)
                    items.forEachIndexed { index, meal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(meal.name, color = TextSecondary)
                                Text(
                                    "P ${meal.proteinG}g  C ${meal.carbsG}g  F ${meal.fatG}g",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                )
                            }
                            Text("${meal.totalKcal} kcal", color = TextMuted, fontSize = 13.sp)
                        }
                        if (index < items.lastIndex) Divider(color = Border)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsScreen(meals: List<MealEntry>, target: Int) {
    data class StatsDaySummary(
        val day: LocalDate,
        val kcal: Int,
        val protein: Int,
        val carbs: Int,
        val fat: Int,
    )

    var period by rememberSaveable { mutableStateOf("day") }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val density = LocalDensity.current
    val weekStart = today.minusDays(6)

    val weekBreakdown = (0..6).map { offset ->
        val day = today.minusDays((6 - offset).toLong())
        val dayMeals = meals.filter {
            Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == day
        }
        StatsDaySummary(
            day = day,
            kcal = dayMeals.sumOf { it.totalKcal },
            protein = dayMeals.sumOf { it.proteinG },
            carbs = dayMeals.sumOf { it.carbsG },
            fat = dayMeals.sumOf { it.fatG },
        )
    }
    val weekData = weekBreakdown.map { it.day to it.kcal }
    val weekMeals = meals.filter {
        val mealDate = Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
        mealDate in weekStart..today
    }
    val todayMeals = meals.filter {
        Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == today
    }
    val dayProtein = todayMeals.sumOf { it.proteinG }
    val dayCarbs = todayMeals.sumOf { it.carbsG }
    val dayFat = todayMeals.sumOf { it.fatG }
    val weekProtein = weekMeals.sumOf { it.proteinG }
    val weekCarbs = weekMeals.sumOf { it.carbsG }
    val weekFat = weekMeals.sumOf { it.fatG }
    val periodProtein = if (period == "week") weekProtein else dayProtein
    val periodCarbs = if (period == "week") weekCarbs else dayCarbs
    val periodFat = if (period == "week") weekFat else dayFat
    val dayHours = listOf(6, 8, 10, 12, 14, 15, 17, 19, 21)
    val dayData = dayHours.map { hour ->
        val total = todayMeals
            .filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).hour <= hour }
            .sumOf { it.totalKcal }
        formatHourLabel(hour) to total
    }

    val avg = if (weekData.isNotEmpty()) weekData.sumOf { it.second } / weekData.size else 0
    val delta = avg - target
    val deltaText = when {
        delta < 0 -> "↘ ${abs(delta)}"
        delta > 0 -> "↗ ${abs(delta)}"
        else -> "0"
    }
    val deltaColor = when {
        delta < 0 -> Accent
        delta > 0 -> Danger
        else -> TextSecondary
    }
    val deltaHint = when {
        delta < 0 -> "under"
        delta > 0 -> "over"
        else -> "on target"
    }
    val bestDay = weekData.minByOrNull { abs(it.second - target) }?.first
    val bestDayLabel = bestDay?.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)) ?: "-"

    CompositionLocalProvider(LocalDensity provides Density(density = density.density, fontScale = 1f)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Statistics", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Calorie tracking overview", color = TextMuted, fontSize = 13.sp)
            }
            item {
                StatsPeriodToggle(period = period, onChange = { period = it })
            }
            if (period == "week") {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatsMetricCard(
                            title = "Daily avg",
                            value = avg.toString(),
                            subtitle = "kcal",
                            valueColor = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        StatsMetricCard(
                            title = "vs Target",
                            value = deltaText,
                            subtitle = deltaHint,
                            valueColor = deltaColor,
                            modifier = Modifier.weight(1f),
                        )
                        StatsMetricCard(
                            title = "Best day",
                            value = bestDayLabel,
                            subtitle = "closest to goal",
                            valueColor = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                if (period == "week") {
                    WeekCaloriesChartCard(weekData = weekData, target = target)
                } else {
                    TodayCaloriesChartCard(dayData = dayData, target = target)
                }
            }
            item {
                CardBlock(background = Color(0xFF1E1E1E), borderColor = Color(0xFF2A2A2A)) {
                    Text(
                        if (period == "week") "Macros this week" else "Macros today",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroCard(
                            label = "Protein",
                            value = "${periodProtein}g",
                            icon = Icons.Outlined.LocalFireDepartment,
                            color = Accent,
                            modifier = Modifier.weight(1f),
                        )
                        MacroCard(
                            label = "Carbs",
                            value = "${periodCarbs}g",
                            icon = Icons.Default.TextFields,
                            color = Accent,
                            modifier = Modifier.weight(1f),
                        )
                        MacroCard(
                            label = "Fat",
                            value = "${periodFat}g",
                            icon = Icons.Outlined.WaterDrop,
                            color = Accent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (period == "week") {
                item {
                    Text(
                        "DAILY BREAKDOWN",
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(weekBreakdown) { daySummary ->
                    DailyBreakdownRow(
                        day = daySummary.day,
                        kcal = daySummary.kcal,
                        protein = daySummary.protein,
                        carbs = daySummary.carbs,
                        fat = daySummary.fat,
                        target = target,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsPeriodToggle(period: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("day" to "Today", "week" to "This Week").forEach { (value, label) ->
            val selected = period == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color(0xFF2D2D2D) else Color.Transparent)
                    .clickable { onChange(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (selected) Color.White else TextSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun StatsMetricCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    CardBlock(
        modifier = modifier.height(126.dp),
        background = Color(0xFF1E1E1E),
        borderColor = Color(0xFF2A2A2A),
    ) {
        Text(title, color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = valueColor, fontSize = 34.sp * 0.72f, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, color = TextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun WeekCaloriesChartCard(weekData: List<Pair<LocalDate, Int>>, target: Int) {
    CardBlock(background = Color(0xFF1E1E1E), borderColor = Color(0xFF2A2A2A)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Calories this week", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("—  Target $target", color = TextMuted, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        WeekLineChart(
            points = weekData.map { dayEntry ->
                dayEntry.first.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)) to dayEntry.second
            },
            target = target,
        )
    }
}

@Composable
private fun TodayCaloriesChartCard(dayData: List<Pair<String, Int>>, target: Int) {
    CardBlock(background = Color(0xFF1E1E1E), borderColor = Color(0xFF2A2A2A)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today's intake", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("—  Target $target", color = TextMuted, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        TodayLineChart(points = dayData, target = target)
    }
}

@Composable
private fun TodayLineChart(
    points: List<Pair<String, Int>>,
    target: Int,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("No data yet", color = TextMuted)
        }
        return
    }

    val rawMax = maxOf(points.maxOf { it.second }, target)
    val maxValue = (((rawMax + 599) / 600) * 600 + 600).coerceAtLeast(1200)
    val minValue = 0
    val valueRange = (maxValue - minValue).coerceAtLeast(1)
    val yMarks = (0..4).map { maxValue - (maxValue / 4) * it }

    val chartHeight = 170.dp
    val leftPadding = 34.dp
    val rightPadding = 8.dp
    val topPadding = 8.dp
    val bottomPadding = 22.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val left = leftPadding.toPx()
            val right = rightPadding.toPx()
            val top = topPadding.toPx()
            val bottom = bottomPadding.toPx()

            val chartWidth = size.width - left - right
            val chartHeightPx = size.height - top - bottom
            val lastIndex = (points.size - 1).coerceAtLeast(1)

            fun xFor(index: Int): Float = left + (chartWidth * (index.toFloat() / lastIndex.toFloat()))
            fun yFor(value: Int): Float = top + ((maxValue - value).toFloat() / valueRange.toFloat()) * chartHeightPx

            val targetY = yFor(target)
            drawLine(
                color = Color(0x66FFFFFF),
                start = Offset(left, targetY),
                end = Offset(size.width - right, targetY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )

            val path = Path()
            points.forEachIndexed { index, point ->
                val x = xFor(index)
                val y = yFor(point.second)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = Accent,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )

            points.forEachIndexed { index, point ->
                drawCircle(
                    color = Accent,
                    radius = 4.dp.toPx(),
                    center = Offset(xFor(index), yFor(point.second)),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = topPadding)
                .height(chartHeight - topPadding - bottomPadding),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            yMarks.forEach { mark ->
                Text(mark.toString(), color = TextMuted, fontSize = 11.sp)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = leftPadding, end = rightPadding, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            points.forEach { (label, _) ->
                Text(label, color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun WeekLineChart(
    points: List<Pair<String, Int>>,
    target: Int,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("No data yet", color = TextMuted)
        }
        return
    }

    val rawMin = minOf(points.minOf { it.second }, target)
    val rawMax = maxOf(points.maxOf { it.second }, target)

    var minValue = (((rawMin - 200).coerceAtLeast(0)) / 200) * 200
    var maxValue = (((rawMax + 200) + 199) / 200) * 200
    if (maxValue - minValue < 800) {
        maxValue = minValue + 800
    }
    val valueRange = (maxValue - minValue).coerceAtLeast(1)
    val stepValue = valueRange / 4f
    val yMarks = (0..4).map { (maxValue - (stepValue * it)).roundToInt() }

    val chartHeight = 170.dp
    val leftPadding = 34.dp
    val rightPadding = 8.dp
    val topPadding = 8.dp
    val bottomPadding = 22.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val left = leftPadding.toPx()
            val right = rightPadding.toPx()
            val top = topPadding.toPx()
            val bottom = bottomPadding.toPx()

            val chartWidth = size.width - left - right
            val chartHeightPx = size.height - top - bottom
            val lastIndex = (points.size - 1).coerceAtLeast(1)

            fun xFor(index: Int): Float = left + (chartWidth * (index.toFloat() / lastIndex.toFloat()))
            fun yFor(value: Int): Float = top + ((maxValue - value).toFloat() / valueRange.toFloat()) * chartHeightPx

            val targetY = yFor(target)
            drawLine(
                color = Color(0x66FFFFFF),
                start = Offset(left, targetY),
                end = Offset(size.width - right, targetY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )

            val path = Path()
            points.forEachIndexed { index, point ->
                val x = xFor(index)
                val y = yFor(point.second)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = Accent,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )

            points.forEachIndexed { index, point ->
                drawCircle(
                    color = Accent,
                    radius = 4.dp.toPx(),
                    center = Offset(xFor(index), yFor(point.second)),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = topPadding)
                .height(chartHeight - topPadding - bottomPadding),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            yMarks.forEach { mark ->
                Text(mark.toString(), color = TextMuted, fontSize = 11.sp)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = leftPadding, end = rightPadding, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            points.forEach { (label, _) ->
                Text(label.take(3), color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DailyBreakdownRow(day: LocalDate, kcal: Int, protein: Int, carbs: Int, fat: Int, target: Int) {
    val isOverTarget = target > 0 && kcal > target
    val barProgress = if (target > 0) (kcal.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val dayLabel = day.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))
    val dateLabel = day.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
    val barColor = if (isOverTarget) Danger else Accent

    CardBlock(background = Color(0xFF1E1E1E), borderColor = Color(0xFF2A2A2A)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(dayLabel, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Text(dateLabel, color = TextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                "$kcal kcal",
                color = if (isOverTarget) Danger else Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "P ${protein}g  C ${carbs}g  F ${fat}g",
            color = TextMuted,
            fontSize = 11.sp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF2A2A2A)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barProgress)
                    .fillMaxHeight()
                    .background(barColor),
            )
        }
    }
}

@Composable
private fun WeightScreen(
    profile: UserProfile,
    weights: List<WeightEntry>,
    onAddWeight: (Double) -> Unit,
    onUpdateWeight: (Long, Double, Long) -> Unit,
    onDeleteWeight: (Long) -> Unit,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var newWeight by rememberSaveable { mutableStateOf((weights.lastOrNull()?.weightKg ?: profile.weightKg).toString()) }
    var editingWeightId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editDateDraft by rememberSaveable { mutableStateOf("") }
    var editWeightDraft by rememberSaveable { mutableStateOf("") }
    val editingWeight = weights.firstOrNull { it.id == editingWeightId }
    val sortedWeights = weights.sortedBy { it.timestamp }
    val recentEntries = sortedWeights.sortedByDescending { it.timestamp }.take(8)
    val trendEntries = sortedWeights.takeLast(7)
    val trendMonthLabel = (trendEntries.lastOrNull()?.timestamp ?: System.currentTimeMillis()).let(::formatMonthName)

    LaunchedEffect(editingWeightId, weights) {
        if (editingWeightId != null && weights.none { it.id == editingWeightId }) {
            editingWeightId = null
        }
    }

    val current = sortedWeights.lastOrNull()?.weightKg ?: profile.weightKg
    val startWeight = profile.startWeightKg
    val goalWeight = profile.goalWeightKg
    val resolvedGoal = resolveGoalTypeByWeights(startWeight, goalWeight)
    val range = abs(startWeight - goalWeight)
    val progress = if (range < 0.0001) {
        if (abs(current - goalWeight) < 0.0001) 100.0 else 0.0
    } else {
        ((startWeight - current) / (startWeight - goalWeight) * 100.0).coerceIn(0.0, 100.0)
    }
    val progressPercent = progress.roundToInt().coerceIn(0, 100)
    val deltaFromStart = current - profile.startWeightKg
    val isCurrentOutOfBounds = isWeightOutOfBounds(current, startWeight, goalWeight, resolvedGoal)
    val remainingToGoal = abs(goalWeight - current)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Weight", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Track your progress", color = TextMuted, fontSize = 13.sp)
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add weight")
                }
            }
        }
        item {
            WeightOverviewCard(
                current = current,
                deltaFromStart = deltaFromStart,
                progress = (progress / 100f).toFloat(),
                progressPercent = progressPercent,
                startWeight = startWeight,
                goalWeight = goalWeight,
                remainingToGoal = remainingToGoal,
                isOutOfBounds = isCurrentOutOfBounds,
            )
        }
        item {
            WeightTrendCard(
                entries = trendEntries,
                goalWeight = goalWeight,
                monthLabel = trendMonthLabel,
            )
        }
        if (recentEntries.isNotEmpty()) {
            item {
                Text(
                    "RECENT ENTRIES",
                    color = Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            item {
                CardBlock(background = Color(0xFF1E1E1E), borderColor = Color(0xFF2A2A2A)) {
                    Text("No weight entries yet", color = TextMuted)
                }
            }
        }
        itemsIndexed(recentEntries) { index, item ->
            val previous = recentEntries.getOrNull(index + 1)
            val delta = previous?.let { item.weightKg - it.weightKg }
            val deltaText = delta?.let {
                val normalized = if (abs(it) < 0.05) 0.0 else it
                if (normalized > 0) "+${"%.1f".format(normalized)}" else "%.1f".format(normalized)
            }
            val deltaColor = if (deltaText == null) TextMuted else Accent

            CardBlock(
                modifier = Modifier.clickable {
                    editingWeightId = item.id
                    editDateDraft = formatWeightDateInput(item.timestamp)
                    editWeightDraft = "%.1f".format(item.weightKg)
                },
                background = Color(0xFF1E1E1E),
                borderColor = Color(0x334CAF50),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatDateMonthDay(item.timestamp),
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                        fontSize = 23.sp * 0.72f,
                    )
                    Text(
                        "${"%.1f".format(item.weightKg)} kg",
                        color = Accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp * 0.72f,
                    )
                    if (deltaText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(deltaText, color = deltaColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CardBg,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Log weight") },
            text = {
                OutlinedTextField(
                    value = newWeight,
                    onValueChange = { newWeight = it },
                    singleLine = true,
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Border,
                        focusedBorderColor = Accent,
                        unfocusedContainerColor = CardBg,
                        focusedContainerColor = CardBg,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedLabelColor = TextMuted,
                        focusedLabelColor = Accent,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = newWeight.replace(',', '.').toDoubleOrNull()
                    if (value != null) {
                        onAddWeight(value)
                    }
                    showAddDialog = false
                }) { Text("Save", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    if (editingWeight != null && editingWeightId != null) {
        val parsedWeight = editWeightDraft.replace(',', '.').toDoubleOrNull()
        val parsedTimestamp = parseWeightDateInputToTimestamp(editDateDraft)
        val canSave = parsedWeight != null && parsedWeight > 0.0 && parsedTimestamp != null

        Dialog(
            onDismissRequest = { editingWeightId = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF232323), Color(0xFF171717)),
                        ),
                    )
                    .border(1.dp, Border, RoundedCornerShape(24.dp))
                    .padding(18.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Edit weight",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { editingWeightId = null },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2D2D2D)),
                        ) {
                            Text("×", color = TextSecondary, fontSize = 20.sp, textAlign = TextAlign.Center)
                        }
                    }

                    Text(
                        "DATE (YYYY-MM-DD)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = editDateDraft,
                        onValueChange = { editDateDraft = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Border,
                            focusedBorderColor = Accent,
                            unfocusedContainerColor = Color(0xFF242424),
                            focusedContainerColor = Color(0xFF242424),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedLabelColor = TextMuted,
                            focusedLabelColor = Accent,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )

                    Text(
                        "WEIGHT (KG)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = editWeightDraft,
                        onValueChange = { editWeightDraft = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Border,
                            focusedBorderColor = Accent,
                            unfocusedContainerColor = Color(0xFF242424),
                            focusedContainerColor = Color(0xFF242424),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedLabelColor = TextMuted,
                            focusedLabelColor = Accent,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onDeleteWeight(editingWeight.id)
                                editingWeightId = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF242424),
                                contentColor = Accent,
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Accent),
                        ) {
                            Text("Delete")
                        }
                        Button(
                            onClick = {
                                onUpdateWeight(editingWeight.id, parsedWeight!!, parsedTimestamp!!)
                                editingWeightId = null
                            },
                            enabled = canSave,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        ) {
                            Text("Save changes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightOverviewCard(
    current: Double,
    deltaFromStart: Double,
    progress: Float,
    progressPercent: Int,
    startWeight: Double,
    goalWeight: Double,
    remainingToGoal: Double,
    isOutOfBounds: Boolean,
) {
    val chipPrefix = when {
        deltaFromStart > 0.05 -> "↗"
        deltaFromStart < -0.05 -> "↘"
        else -> "•"
    }
    val normalizedDelta = if (abs(deltaFromStart) < 0.05) 0.0 else deltaFromStart
    val deltaText = if (normalizedDelta > 0) "+${"%.1f".format(normalizedDelta)} kg" else "${"%.1f".format(normalizedDelta)} kg"
    val accentColor = if (isOutOfBounds) Danger else Accent

    CardBlock(background = Color(0xFF1E1E1E), borderColor = Color(0xFF2A2A2A)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Current weight", color = TextMuted, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("%.1f".format(current), color = Color.White, fontSize = 56.sp * 0.72f, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("kg", color = TextSecondary, fontSize = 32.sp * 0.72f)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isOutOfBounds) Color(0x33FF5252) else Color(0x224CAF50))
                    .border(1.dp, accentColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text("$chipPrefix  $deltaText", color = accentColor, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Progress to goal", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("$progressPercent%", color = accentColor, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF2A2A2A)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(accentColor),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row {
            Column {
                Text("Start", color = TextMuted, fontSize = 12.sp)
                Text("${"%.1f".format(startWeight)} kg", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Goal", color = TextMuted, fontSize = 12.sp)
                Text("${"%.1f".format(goalWeight)} kg", color = accentColor, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF2D2D2D))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Remaining to goal", color = TextSecondary, modifier = Modifier.weight(1f))
                Text("${"%.1f".format(remainingToGoal)} kg", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WeightTrendCard(entries: List<WeightEntry>, goalWeight: Double, monthLabel: String) {
    CardBlock(background = Color(0xFF1E1E1E), borderColor = Color(0xFF2A2A2A)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Weight trend — $monthLabel", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("—  Goal ${"%.0f".format(goalWeight)}kg", color = TextMuted, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        WeightTrendChart(entries = entries, goalWeight = goalWeight)
    }
}

@Composable
private fun WeightTrendChart(entries: List<WeightEntry>, goalWeight: Double, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("No data yet", color = TextMuted)
        }
        return
    }

    val values = entries.map { it.weightKg.toFloat() } + goalWeight.toFloat()
    val minRaw = values.minOrNull() ?: goalWeight.toFloat()
    val maxRaw = values.maxOrNull() ?: goalWeight.toFloat()
    var minY = kotlin.math.floor((minRaw - 1.0f).toDouble()).toFloat()
    var maxY = kotlin.math.ceil((maxRaw + 1.0f).toDouble()).toFloat()
    if (maxY - minY < 6f) maxY = minY + 6f
    val range = (maxY - minY).coerceAtLeast(1f)
    val yMarks = (0..3).map { maxY - (range / 3f) * it }

    val first = entries.first()
    val middle = entries[entries.lastIndex / 2]
    val last = entries.last()
    val xLabels = listOf(first, middle, last).distinctBy { it.id }

    val chartHeight = 170.dp
    val leftPadding = 28.dp
    val rightPadding = 8.dp
    val topPadding = 8.dp
    val bottomPadding = 22.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val left = leftPadding.toPx()
            val right = rightPadding.toPx()
            val top = topPadding.toPx()
            val bottom = bottomPadding.toPx()

            val chartWidth = size.width - left - right
            val chartHeightPx = size.height - top - bottom
            val lastIndex = (entries.size - 1).coerceAtLeast(1)

            fun xFor(index: Int): Float = left + (chartWidth * (index.toFloat() / lastIndex.toFloat()))
            fun yFor(value: Float): Float = top + ((maxY - value) / range) * chartHeightPx

            val goalY = yFor(goalWeight.toFloat())
            drawLine(
                color = Color(0x55FFFFFF),
                start = Offset(left, goalY),
                end = Offset(size.width - right, goalY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )

            val path = Path()
            entries.forEachIndexed { index, item ->
                val x = xFor(index)
                val y = yFor(item.weightKg.toFloat())
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = Accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

            entries.forEachIndexed { index, item ->
                drawCircle(
                    color = Accent,
                    radius = 3.8.dp.toPx(),
                    center = Offset(xFor(index), yFor(item.weightKg.toFloat())),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = topPadding)
                .height(chartHeight - topPadding - bottomPadding),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            yMarks.forEach { mark ->
                Text(formatWeightAxisValue(mark), color = TextMuted, fontSize = 11.sp)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = leftPadding, end = rightPadding, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            xLabels.forEach { item ->
                Text(formatDateMonthDay(item.timestamp), color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NotificationsScreen(
    settings: NotificationSettings,
    onSave: (NotificationSettings) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var customDaysDraft by rememberSaveable { mutableStateOf(settings.weightCustomEveryDays.toString()) }
    var pickingTimeField by rememberSaveable { mutableStateOf<NotificationTimeField?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }

    LaunchedEffect(settings.weightCustomEveryDays, settings.weightFrequency) {
        customDaysDraft = settings.weightCustomEveryDays.toString()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    val toggleColors = SwitchDefaults.colors(
        checkedThumbColor = Accent,
        checkedTrackColor = Color(0x334CAF50),
        checkedBorderColor = Accent,
        uncheckedThumbColor = Color(0xFF7A7A7A),
        uncheckedTrackColor = Color(0xFF242424),
        uncheckedBorderColor = Border,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
    ) {
        TopAppBar(
            title = { Text("Notifications", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Bg,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
            ),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                item {
                    CardBlock(background = Color(0x1F4CAF50), borderColor = Color(0x554CAF50)) {
                        Text("Enable notifications permission to receive reminders.", color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Allow notifications")
                        }
                    }
                }
            }

            item {
                CardBlock(background = CardBg, borderColor = Border) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Meal reminders", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.mealRemindersEnabled,
                            onCheckedChange = { onSave(settings.copy(mealRemindersEnabled = it)) },
                            colors = toggleColors,
                        )
                    }

                    if (settings.mealRemindersEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Meals per day", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..3).forEach { count ->
                                NotificationChoiceChip(
                                    label = count.toString(),
                                    selected = settings.mealsPerDay == count,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSave(settings.copy(mealsPerDay = count)) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (settings.mealsPerDay >= 1) {
                            NotificationTimeRow(
                                label = "Breakfast",
                                minutes = settings.breakfastMinutes,
                            ) {
                                pickingTimeField = NotificationTimeField.BREAKFAST
                            }
                        }
                        if (settings.mealsPerDay >= 2) {
                            NotificationTimeRow(
                                label = "Lunch",
                                minutes = settings.lunchMinutes,
                            ) {
                                pickingTimeField = NotificationTimeField.LUNCH
                            }
                        }
                        if (settings.mealsPerDay >= 3) {
                            NotificationTimeRow(
                                label = "Dinner",
                                minutes = settings.dinnerMinutes,
                            ) {
                                pickingTimeField = NotificationTimeField.DINNER
                            }
                        }
                    }
                }
            }

            item {
                CardBlock(background = CardBg, borderColor = Border) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Weight reminder", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.weightReminderEnabled,
                            onCheckedChange = { onSave(settings.copy(weightReminderEnabled = it)) },
                            colors = toggleColors,
                        )
                    }

                    if (settings.weightReminderEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Frequency", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WeightReminderFrequency.entries.forEach { frequency ->
                                NotificationChoiceChip(
                                    label = frequency.label(),
                                    selected = settings.weightFrequency == frequency,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSave(settings.copy(weightFrequency = frequency)) },
                                )
                            }
                        }

                        if (settings.weightFrequency == WeightReminderFrequency.WEEKLY) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Day", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WeekDay.entries.take(4).forEach { day ->
                                    NotificationChoiceChip(
                                        label = day.label(),
                                        selected = settings.weightWeekDays.contains(day),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            onSave(
                                                settings.copy(
                                                    weightWeekDays = toggleWeekDaySelection(settings.weightWeekDays, day),
                                                ),
                                            )
                                        },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WeekDay.entries.drop(4).forEach { day ->
                                    NotificationChoiceChip(
                                        label = day.label(),
                                        selected = settings.weightWeekDays.contains(day),
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            onSave(
                                                settings.copy(
                                                    weightWeekDays = toggleWeekDaySelection(settings.weightWeekDays, day),
                                                ),
                                            )
                                        },
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        if (settings.weightFrequency == WeightReminderFrequency.CUSTOM) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Every N days", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customDaysDraft,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }.take(2)
                                    customDaysDraft = filtered
                                    val parsed = filtered.toIntOrNull()
                                    if (parsed != null) {
                                        onSave(settings.copy(weightCustomEveryDays = parsed.coerceIn(2, 30)))
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("Days (2-30)") },
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Border,
                                    focusedBorderColor = Accent,
                                    unfocusedContainerColor = Color(0xFF242424),
                                    focusedContainerColor = Color(0xFF242424),
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White,
                                    unfocusedLabelColor = TextMuted,
                                    focusedLabelColor = Accent,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        NotificationTimeRow(
                            label = "Time",
                            minutes = settings.weightTimeMinutes,
                        ) {
                            pickingTimeField = NotificationTimeField.WEIGHT
                        }
                    }
                }
            }

            item {
                Text(
                    "Settings auto-save automatically.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }

    val timePickerInitial = when (pickingTimeField) {
        NotificationTimeField.BREAKFAST -> settings.breakfastMinutes
        NotificationTimeField.LUNCH -> settings.lunchMinutes
        NotificationTimeField.DINNER -> settings.dinnerMinutes
        NotificationTimeField.WEIGHT -> settings.weightTimeMinutes
        null -> null
    }

    if (timePickerInitial != null) {
        val title = when (pickingTimeField) {
            NotificationTimeField.BREAKFAST -> "Breakfast time"
            NotificationTimeField.LUNCH -> "Lunch time"
            NotificationTimeField.DINNER -> "Dinner time"
            NotificationTimeField.WEIGHT -> "Weight reminder time"
            null -> "Time"
        }
        AppTimePickerDialog(
            title = title,
            initialMinutes = timePickerInitial,
            onDismiss = { pickingTimeField = null },
            onSave = { minutes ->
                when (pickingTimeField) {
                    NotificationTimeField.BREAKFAST -> onSave(settings.copy(breakfastMinutes = minutes))
                    NotificationTimeField.LUNCH -> onSave(settings.copy(lunchMinutes = minutes))
                    NotificationTimeField.DINNER -> onSave(settings.copy(dinnerMinutes = minutes))
                    NotificationTimeField.WEIGHT -> onSave(settings.copy(weightTimeMinutes = minutes))
                    null -> Unit
                }
                pickingTimeField = null
            },
        )
    }
}

@Composable
private fun NotificationChoiceChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0x224CAF50) else Color(0xFF242424),
            contentColor = if (selected) Accent else TextSecondary,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Accent else Border),
    ) {
        Text(label)
    }
}

private fun toggleWeekDaySelection(current: List<WeekDay>, day: WeekDay): List<WeekDay> {
    val normalized = current.distinct().ifEmpty { listOf(WeekDay.MON) }.toMutableSet()
    if (normalized.contains(day)) {
        if (normalized.size == 1) return WeekDay.entries.filter { normalized.contains(it) }
        normalized.remove(day)
    } else {
        normalized.add(day)
    }
    return WeekDay.entries.filter { normalized.contains(it) }
}

private enum class NotificationTimeField {
    BREAKFAST,
    LUNCH,
    DINNER,
    WEIGHT,
}

@Composable
private fun NotificationTimeRow(label: String, minutes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF242424))
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(formatReminderTime(minutes), color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AppTimePickerDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    val safeInitial = initialMinutes.coerceIn(0, 23 * 60 + 59)
    var hour by remember(safeInitial) { mutableIntStateOf(safeInitial / 60) }
    var minute by remember(safeInitial) { mutableIntStateOf(safeInitial % 60) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF232323), Color(0xFF171717)),
                    ),
                )
                .border(1.dp, Border, RoundedCornerShape(24.dp))
                .padding(18.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2D2D2D)),
                    ) {
                        Text("×", color = TextSecondary, fontSize = 20.sp, textAlign = TextAlign.Center)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1F1F1F))
                        .border(1.dp, Border, RoundedCornerShape(18.dp))
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimeWheelColumn(
                        value = hour,
                        range = 0..23,
                        modifier = Modifier.weight(1f),
                        onValueChange = { hour = it },
                    )
                    Text(
                        ":",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Light,
                    )
                    TimeWheelColumn(
                        value = minute,
                        range = 0..59,
                        modifier = Modifier.weight(1f),
                        onValueChange = { minute = it },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF242424))
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        formatReminderTime(hour * 60 + minute),
                        color = Accent,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF242424),
                            contentColor = TextSecondary,
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(hour * 60 + minute) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeWheelColumn(
    value: Int,
    range: IntRange,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit,
) {
    val valuesCount = range.last - range.first + 1
    val visibleRows = 5
    val centerOffset = visibleRows / 2
    val itemHeight = 58.dp
    val middleBase = remember(valuesCount) {
        val middle = Int.MAX_VALUE / 2
        middle - middle % valuesCount
    }
    val initialCenter = remember(value, middleBase, range.first, valuesCount) {
        middleBase + (value - range.first).positiveMod(valuesCount)
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialCenter.coerceAtLeast(0),
    )
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centeredIndex by remember(listState) {
        derivedStateOf { centeredVisibleItemIndex(listState) }
    }
    val centeredValue = range.first + centeredIndex.positiveMod(valuesCount)

    LaunchedEffect(centeredValue) {
        if (centeredValue != value) {
            onValueChange(centeredValue)
        }
    }

    LaunchedEffect(centeredIndex, middleBase, valuesCount) {
        if (centeredIndex < 1_500 || centeredIndex > Int.MAX_VALUE - 1_500) {
            val normalizedCenter = middleBase + centeredIndex.positiveMod(valuesCount)
            if (normalizedCenter != centeredIndex) {
                listState.scrollToItem(normalizedCenter.coerceAtLeast(0))
            }
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleRows)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF202020))
            .border(1.dp, Border, RoundedCornerShape(16.dp)),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = itemHeight * centerOffset),
            flingBehavior = snapFlingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(Int.MAX_VALUE) { index ->
                val display = range.first + index.positiveMod(valuesCount)
                val selected = index == centeredIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = String.format(Locale.ENGLISH, "%02d", display),
                        color = if (selected) Color.White else TextMuted,
                        fontSize = if (selected) 56.sp else 44.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Light,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x1A4CAF50))
                .border(1.dp, Color(0x554CAF50), RoundedCornerShape(12.dp)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(54.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF202020), Color.Transparent))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(54.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF202020)))),
        )
    }
}

private fun centeredVisibleItemIndex(listState: LazyListState): Int {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) {
        return listState.firstVisibleItemIndex
    }
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return visibleItems.minByOrNull { item ->
        abs((item.offset + item.size / 2) - viewportCenter)
    }?.index ?: listState.firstVisibleItemIndex
}

private fun Int.positiveMod(modulus: Int): Int {
    if (modulus <= 0) return 0
    val raw = this % modulus
    return if (raw < 0) raw + modulus else raw
}

private val weightInParenthesesRegex = Regex("""\(\s*(\d+(?:[.,]\d+)?)\s*[gг]\s*\)""", RegexOption.IGNORE_CASE)
private val weightSuffixRegex = Regex("""\b(\d+(?:[.,]\d+)?)\s*[gг]\b""", RegexOption.IGNORE_CASE)

private fun extractNameAndWeight(rawName: String): Pair<String, Int?> {
    var cleanedName = rawName.trim()
    var extractedWeight: Int? = null

    val parenthesizedWeight = weightInParenthesesRegex.find(cleanedName)
    if (parenthesizedWeight != null) {
        extractedWeight = parenthesizedWeight.groupValues.getOrNull(1)
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            ?.roundToInt()
            ?.coerceAtLeast(0)
        cleanedName = cleanedName.replace(weightInParenthesesRegex, " ")
    }

    if (extractedWeight == null) {
        val suffixWeight = weightSuffixRegex.find(cleanedName)
        if (suffixWeight != null) {
            extractedWeight = suffixWeight.groupValues.getOrNull(1)
                ?.replace(',', '.')
                ?.toDoubleOrNull()
                ?.roundToInt()
                ?.coerceAtLeast(0)
            cleanedName = cleanedName.replace(weightSuffixRegex, " ")
        }
    }

    val normalizedName = cleanedName
        .replace(Regex("\\s+"), " ")
        .trim()
        .trim(',', ';', '-', '—')

    return normalizedName to extractedWeight
}

private fun recalculateItemCalories(baseKcal: Int, baseGrams: Int, targetGrams: Int): Int {
    return recalculateItemCalories(
        productName = "",
        baseKcal = baseKcal,
        baseGrams = baseGrams,
        targetGrams = targetGrams,
    )
}

private data class FoodCaloriesRule(
    val kcalPer100g: Int,
    val keywords: List<String>,
)

private val foodCaloriesRules = listOf(
    FoodCaloriesRule(230, listOf("fried chicken", "жареная курица", "курица жареная")),
    FoodCaloriesRule(165, listOf("chicken breast", "куриная грудка", "грудка курицы")),
    FoodCaloriesRule(250, listOf("chicken thigh", "куриное бедро", "бедро курицы")),
    FoodCaloriesRule(208, listOf("beef", "говядина")),
    FoodCaloriesRule(242, listOf("pork", "свинина")),
    FoodCaloriesRule(143, listOf("egg", "eggs", "яйцо", "яйца")),
    FoodCaloriesRule(200, listOf("salmon", "лосось", "семга")),
    FoodCaloriesRule(128, listOf("rice", "рис")),
    FoodCaloriesRule(110, listOf("pasta", "макароны")),
    FoodCaloriesRule(265, listOf("bread", "хлеб")),
    FoodCaloriesRule(160, listOf("potato", "картофель", "картошка")),
    FoodCaloriesRule(35, listOf("cabbage", "капуста")),
    FoodCaloriesRule(86, listOf("corn", "кукуруза")),
    FoodCaloriesRule(18, listOf("tomato", "tomatoes", "помидор", "томаты")),
    FoodCaloriesRule(15, listOf("cucumber", "огурец")),
    FoodCaloriesRule(160, listOf("avocado", "авокадо")),
    FoodCaloriesRule(575, listOf("olive oil", "оливковое масло", "oil", "масло")),
    FoodCaloriesRule(680, listOf("mayonnaise", "майонез", "dressing", "заправка")),
    FoodCaloriesRule(380, listOf("cheese", "сыр")),
    FoodCaloriesRule(350, listOf("nuts", "орехи", "almond", "миндаль")),
    FoodCaloriesRule(52, listOf("apple", "яблоко")),
    FoodCaloriesRule(89, listOf("banana", "банан")),
)

private fun estimateCaloriesPer100gByName(productName: String): Int? {
    val normalized = productName
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
        .trim()
    if (normalized.isBlank()) return null

    var bestMatchLength = -1
    var bestKcal: Int? = null
    foodCaloriesRules.forEach { rule ->
        rule.keywords.forEach { keyword ->
            if (keyword.isNotBlank() && normalized.contains(keyword)) {
                val score = keyword.length
                if (score > bestMatchLength) {
                    bestMatchLength = score
                    bestKcal = rule.kcalPer100g
                }
            }
        }
    }
    return bestKcal
}

private fun recalculateItemCalories(
    productName: String,
    baseKcal: Int,
    baseGrams: Int,
    targetGrams: Int,
): Int {
    val safeBaseKcal = baseKcal.coerceAtLeast(0)
    val safeBaseGrams = baseGrams.coerceAtLeast(0)
    val safeTargetGrams = targetGrams.coerceAtLeast(0)
    if (safeTargetGrams == 0) return 0
    val fromNamePer100g = estimateCaloriesPer100gByName(productName)?.toDouble()
    val fromBasePer100g = if (safeBaseGrams > 0) {
        (safeBaseKcal.toDouble() / safeBaseGrams.toDouble()) * 100.0
    } else {
        null
    }
    val usedPer100g = fromNamePer100g ?: fromBasePer100g ?: 100.0
    return ((usedPer100g / 100.0) * safeTargetGrams).roundToInt().coerceAtLeast(0)
}

private data class MacroTotals(
    val protein: Int,
    val carbs: Int,
    val fat: Int,
)

private fun scaleMacrosByCalories(
    baseCalories: Int,
    baseProtein: Int,
    baseCarbs: Int,
    baseFat: Int,
    targetCalories: Int,
): MacroTotals {
    val safeTarget = targetCalories.coerceAtLeast(0)
    if (safeTarget == 0) {
        return MacroTotals(protein = 0, carbs = 0, fat = 0)
    }

    val safeBaseCalories = baseCalories.coerceAtLeast(0)
    if (safeBaseCalories == 0) {
        return MacroTotals(
            protein = baseProtein.coerceAtLeast(0),
            carbs = baseCarbs.coerceAtLeast(0),
            fat = baseFat.coerceAtLeast(0),
        )
    }

    val ratio = safeTarget.toDouble() / safeBaseCalories.toDouble()
    return MacroTotals(
        protein = (baseProtein.coerceAtLeast(0) * ratio).roundToInt().coerceAtLeast(0),
        carbs = (baseCarbs.coerceAtLeast(0) * ratio).roundToInt().coerceAtLeast(0),
        fat = (baseFat.coerceAtLeast(0) * ratio).roundToInt().coerceAtLeast(0),
    )
}

private val urlInTextRegex = Regex("""https?://[^\s)]+""", RegexOption.IGNORE_CASE)

private fun extractFirstUrl(text: String): String? {
    return urlInTextRegex.find(text)
        ?.value
        ?.trimEnd('.', ',', ';')
}

private fun isApiKeyRelatedError(message: String?): Boolean {
    val normalized = message?.lowercase(Locale.ROOT)?.trim().orEmpty()
    if (normalized.isBlank()) return false
    return normalized.contains("incorrect api key") ||
        normalized.contains("invalid_api_key") ||
        normalized.contains("invalid api key") ||
        normalized.contains("api key is invalid") ||
        normalized.contains("api key not valid")
}

@Composable
private fun ClickableErrorText(
    message: String,
    url: String,
    modifier: Modifier = Modifier,
    onUrlClick: (String) -> Unit,
) {
    val annotationTag = "error_url"
    val annotated = remember(message, url) {
        val start = message.indexOf(url)
        if (start < 0) {
            buildAnnotatedString { append(message) }
        } else {
            buildAnnotatedString {
                append(message.substring(0, start))
                pushStringAnnotation(tag = annotationTag, annotation = url)
                withStyle(
                    SpanStyle(
                        color = Accent,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) {
                    append(url)
                }
                pop()
                append(message.substring(start + url.length))
            }
        }
    }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = TextStyle(color = Color.White, fontSize = 13.sp, lineHeight = 20.sp),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = annotationTag, start = offset, end = offset)
                .firstOrNull()
                ?.let { onUrlClick(it.item) }
        },
    )
}

private fun formatReminderTime(minutes: Int): String {
    val safeMinutes = minutes.coerceIn(0, 23 * 60 + 59)
    val hour = safeMinutes / 60
    val minute = safeMinutes % 60
    return String.format(Locale.ENGLISH, "%02d:%02d", hour, minute)
}

@Composable
private fun ProfileScreen(
    profile: UserProfile,
    apiKey: String,
    language: AppLanguage,
    onSaveProfile: (UserProfile) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onSaveLanguage: (AppLanguage) -> Unit,
    onOpenNotifications: () -> Unit,
    onSignOut: () -> Unit,
) {
    var showIdentityDialog by rememberSaveable { mutableStateOf(false) }
    var showBodyDialog by rememberSaveable { mutableStateOf(false) }
    var showApiDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    var identityName by rememberSaveable { mutableStateOf(profile.name) }
    var identityPhotoUriString by rememberSaveable { mutableStateOf(profile.profilePhotoUri) }

    var bodyGender by rememberSaveable { mutableStateOf(profile.gender) }
    var bodyAge by rememberSaveable { mutableStateOf(profile.age.toString()) }
    var bodyHeight by rememberSaveable { mutableStateOf(profile.heightCm.toString()) }
    var bodyStartWeight by rememberSaveable { mutableStateOf(profile.startWeightKg.toString()) }
    var bodyGoalWeight by rememberSaveable { mutableStateOf(profile.goalWeightKg.toString()) }
    var bodyActivity by rememberSaveable { mutableStateOf(profile.activityLevel) }

    var apiDraftKey by rememberSaveable { mutableStateOf(apiKey) }
    var languageDraft by rememberSaveable { mutableStateOf(language) }
    val resolvedProfileGoal = resolveGoalTypeByWeights(profile.startWeightKg, profile.goalWeightKg)

    val avatarModel = profile.profilePhotoUri?.let(Uri::parse) ?: R.drawable.profile_default

    val profilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        identityPhotoUriString = uri?.toString()
    }

    LaunchedEffect(profile, apiKey) {
        identityName = profile.name
        identityPhotoUriString = profile.profilePhotoUri
        bodyGender = profile.gender
        bodyAge = profile.age.toString()
        bodyHeight = profile.heightCm.toString()
        bodyStartWeight = profile.startWeightKg.toString()
        bodyGoalWeight = profile.goalWeightKg.toString()
        bodyActivity = profile.activityLevel
        apiDraftKey = apiKey
        languageDraft = language
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Profile", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        CardBlock(modifier = Modifier.padding(top = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    AsyncImage(
                        model = avatarModel,
                        contentDescription = null,
                        modifier = Modifier
                            .size(78.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Accent)
                            .border(2.dp, CardBg, CircleShape),
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(profile.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x1F4CAF50))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(resolvedProfileGoal.label(), color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { showIdentityDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit name and photo", tint = Accent)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatMiniCard("Target", "${profile.dailyTargetCalories} kcal", Modifier.weight(1f))
            StatMiniCard("Weight", "${"%.1f".format(profile.weightKg)} kg", Modifier.weight(1f))
            StatMiniCard("Activity", profile.activityLevel.label(), Modifier.weight(1f))
        }

        CardBlock {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("BODY INFO", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { showBodyDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit body info", tint = Accent)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            ProfileInfoRow("Gender", profile.gender.label())
            ProfileInfoRow("Age", "${profile.age} years")
            ProfileInfoRow("Height", "${profile.heightCm} cm")
            ProfileInfoRow("Starting weight", "${"%.1f".format(profile.startWeightKg)} kg")
            ProfileInfoRow("Goal weight", "${"%.1f".format(profile.goalWeightKg)} kg", isLast = true)
        }

        CardBlock {
            Text("SETTINGS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            SettingsRow("Notifications") { onOpenNotifications() }
            SettingsRow("Language (${language.label()})") { showLanguageDialog = true }
            SettingsRow("OpenAI API", isLast = true) { showApiDialog = true }
        }

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x33B71C1C),
                contentColor = Color(0xFFFF6B6B),
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66B71C1C)),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign out", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (showIdentityDialog) {
        AlertDialog(
            onDismissRequest = { showIdentityDialog = false },
            containerColor = CardBg,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Edit profile") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = identityPhotoUriString?.let(Uri::parse) ?: R.drawable.profile_default,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        TextButton(onClick = { profilePicker.launch("image/*") }) {
                            Text("Change photo")
                        }
                    }
                    DarkTextField("Name", identityName) { identityName = it }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveProfile(
                            profile.copy(
                                name = identityName.ifBlank { profile.name },
                                profilePhotoUri = identityPhotoUriString,
                            ),
                        )
                        showIdentityDialog = false
                    },
                ) { Text("Save", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showIdentityDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    if (showBodyDialog) {
        Dialog(
            onDismissRequest = { showBodyDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 24.dp)
                    .heightIn(max = 560.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBg)
                    .border(1.dp, Border, RoundedCornerShape(24.dp))
                    .padding(18.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Edit body info",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { showBodyDialog = false },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2D2D2D)),
                        ) {
                            Text("×", color = TextSecondary, fontSize = 20.sp, textAlign = TextAlign.Center)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileDialogFieldCompact(
                            label = "Age",
                            value = bodyAge,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                        ) { bodyAge = it }
                        ProfileDialogFieldCompact(
                            label = "Height (cm)",
                            value = bodyHeight,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                        ) { bodyHeight = it }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileDialogFieldCompact(
                            label = "Goal weight (kg)",
                            value = bodyGoalWeight,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.fillMaxWidth(),
                        ) { bodyGoalWeight = it }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileDialogFieldCompact("Starting weight (kg)", bodyStartWeight, KeyboardType.Number) { bodyStartWeight = it }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Gender", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GenderType.entries.forEach { item ->
                            FilterButton(item.label(), bodyGender == item, Modifier.weight(1f)) { bodyGender = item }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Activity", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActivityLevel.entries.forEach { item ->
                            FilterButton(item.label(), bodyActivity == item, Modifier.weight(1f)) { bodyActivity = item }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = {
                            onSaveProfile(
                                profile.copy(
                                    gender = bodyGender,
                                    age = bodyAge.toIntOrNull() ?: profile.age,
                                    heightCm = bodyHeight.toIntOrNull() ?: profile.heightCm,
                                    startWeightKg = bodyStartWeight.toDoubleOrNull() ?: profile.startWeightKg,
                                    goalWeightKg = bodyGoalWeight.toDoubleOrNull() ?: profile.goalWeightKg,
                                    activityLevel = bodyActivity,
                                ),
                            )
                            showBodyDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    ) {
                        Text("Save changes", modifier = Modifier.padding(vertical = 5.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showApiDialog) {
        AlertDialog(
            onDismissRequest = { showApiDialog = false },
            containerColor = CardBg,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("OpenAI API key") },
            text = {
                OutlinedTextField(
                    value = apiDraftKey,
                    onValueChange = { apiDraftKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Border,
                        focusedBorderColor = Accent,
                        unfocusedContainerColor = CardBg,
                        focusedContainerColor = CardBg,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedLabelColor = TextMuted,
                        focusedLabelColor = Accent,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveApiKey(apiDraftKey.trim())
                        showApiDialog = false
                    },
                ) { Text("Save", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showApiDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = CardBg,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Language") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { item ->
                        SelectableCard(
                            selected = languageDraft == item,
                            title = item.label(),
                            description = when (item) {
                                AppLanguage.ENGLISH -> "English interface"
                                AppLanguage.RUSSIAN -> "Русский интерфейс"
                                AppLanguage.UKRAINIAN -> "Український інтерфейс"
                            },
                            onClick = { languageDraft = item },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveLanguage(languageDraft)
                        showLanguageDialog = false
                    },
                ) { Text("Save", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }
}
@Composable
private fun ProfileInfoRow(label: String, value: String, isLast: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
    if (!isLast) {
        Divider(color = Border)
    }
}

@Composable
private fun SettingsRow(label: String, isLast: Boolean = false, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
    }
    if (!isLast) {
        Divider(color = Border)
    }
}
private enum class AddMealMode { PHOTO, PROMPT, MANUAL }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AddMealScreen(
    isAnalyzing: Boolean,
    analyzeError: String?,
    initialApiKey: String,
    isApiKeyMissing: Boolean,
    onDismissError: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onBack: () -> Unit,
    onSaveManual: (String, String, Int, Int, Int, Int) -> Unit,
    onAnalyze: (String, String?) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf<AddMealMode?>(null) }
    var description by rememberSaveable { mutableStateOf("") }
    var mealName by rememberSaveable { mutableStateOf("") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var selectedImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var showApiKeyDialog by rememberSaveable { mutableStateOf(false) }
    var apiDraftKey by rememberSaveable { mutableStateOf(initialApiKey) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val selectedImageUri = selectedImageUriString?.let(Uri::parse)
    val canSaveApiKey = apiDraftKey.trim().isNotEmpty()
    val detectedErrorUrl = remember(analyzeError) { analyzeError?.let(::extractFirstUrl) }
    val isApiKeyError = remember(analyzeError) { isApiKeyRelatedError(analyzeError) }
    val showApiKeyAction = isApiKeyMissing || isApiKeyError
    val apiActionLabel = if (isApiKeyMissing) "Add API Key" else "Change Key"

    LaunchedEffect(initialApiKey) {
        apiDraftKey = initialApiKey
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUriString = uri.toString()
            capturedBitmap = null
            mode = AddMealMode.PHOTO
            onDismissError()
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            selectedImageUriString = null
            mode = AddMealMode.PHOTO
            onDismissError()
        }
    }

    val handleBack = {
        when (mode) {
            AddMealMode.PROMPT,
            AddMealMode.MANUAL,
            -> {
                mode = null
                onDismissError()
            }
            else -> onBack()
        }
    }

    BackHandler(onBack = handleBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = handleBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg),
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Add Meal", color = Color.White, fontSize = 30.sp * 0.72f, fontWeight = FontWeight.Bold)
            }
            Text("How do you want to log your meal?", color = TextMuted, fontSize = 15.sp)

            if (analyzeError != null) {
                CardBlock(background = Color(0x33FF5252), borderColor = Danger) {
                    Row(verticalAlignment = Alignment.Top) {
                        if (detectedErrorUrl != null) {
                            ClickableErrorText(
                                message = analyzeError,
                                url = detectedErrorUrl,
                                modifier = Modifier.weight(1f),
                                onUrlClick = { link -> uriHandler.openUri(link) },
                            )
                        } else {
                            Text(
                                analyzeError,
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onDismissError,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x66B71C1C)),
                        ) {
                            Text("×", color = Color.White, fontSize = 16.sp)
                        }
                    }
                    if (showApiKeyAction) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "You can always edit it later in Profile -> OpenAI API.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showApiKeyDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        ) {
                            Text(apiActionLabel, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (mode == null) {
                ModeCard(Icons.Default.CameraAlt, "Take photo", "Snap a photo of your meal") {
                    camera.launch(null)
                }
                ModeCard(Icons.Default.PhotoLibrary, "Upload photo", "Choose from gallery") {
                    picker.launch("image/*")
                }
                ModeCard(Icons.Default.TextFields, "Describe meal", "Type what you ate") {
                    mode = AddMealMode.PROMPT
                    onDismissError()
                }
                ModeCard(Icons.Default.Edit, "Manual input", "Enter calories yourself") {
                    mode = AddMealMode.MANUAL
                    onDismissError()
                }
            } else {
                if (mode == AddMealMode.PHOTO) {
                    CardBlock {
                        Text("Meal photo", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val previewModel: Any? = capturedBitmap ?: selectedImageUri
                        if (previewModel != null) {
                            AsyncImage(
                                model = previewModel,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { camera.launch(null) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x224CAF50)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Accent),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Accent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (previewModel == null) "Take photo" else "Retake",
                                    color = Accent,
                                )
                            }
                            Button(
                                onClick = { picker.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242424)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (selectedImageUri == null && capturedBitmap == null) "Upload" else "Change",
                                    color = Color.White,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        DarkTextField("Describe your meal", description, KeyboardType.Text, minLines = 4) { description = it }
                    }
                }

                if (mode == AddMealMode.PROMPT) {
                    CardBlock {
                        DarkTextField("Describe your meal", description, KeyboardType.Text, minLines = 4) { description = it }
                    }
                }

                if (mode == AddMealMode.MANUAL) {
                    CardBlock {
                        DarkTextField("Meal name", mealName) { mealName = it }
                        DarkTextField("Calories", kcal, KeyboardType.Number) { kcal = it }
                        DarkTextField("Protein (g)", protein, KeyboardType.Number) { protein = it }
                        DarkTextField("Carbs (g)", carbs, KeyboardType.Number) { carbs = it }
                        DarkTextField("Fat (g)", fat, KeyboardType.Number) { fat = it }
                    }
                }
            }
        }
        if (mode != null) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        when (mode) {
                            AddMealMode.MANUAL -> onSaveManual(
                                mealName,
                                "Manual meal",
                                kcal.toIntOrNull() ?: 0,
                                protein.toIntOrNull() ?: 0,
                                carbs.toIntOrNull() ?: 0,
                                fat.toIntOrNull() ?: 0,
                            )

                            AddMealMode.PHOTO,
                            AddMealMode.PROMPT,
                            null,
                            -> {
                                val currentMode = mode
                                val currentUri = selectedImageUri
                                val currentBitmap = capturedBitmap
                                scope.launch {
                                    val imageDataUrl = if (currentMode == AddMealMode.PHOTO) {
                                        when {
                                            currentBitmap != null -> withContext(Dispatchers.Default) { bitmapToDataUrl(currentBitmap) }
                                            currentUri != null -> withContext(Dispatchers.IO) { uriToDataUrl(context, currentUri) }
                                            else -> null
                                        }
                                    } else {
                                        null
                                    }
                                    onAnalyze(description, imageDataUrl)
                                }
                            }
                        }
                    },
                    enabled = !isAnalyzing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (mode == AddMealMode.MANUAL) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save meal")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isAnalyzing) "Analyzing..." else "Analyze with AI")
                    }
                }
                TextButton(
                    onClick = {
                        mode = null
                        description = ""
                        selectedImageUriString = null
                        capturedBitmap = null
                        onDismissError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Change method", color = TextSecondary)
                }
            }
        }
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            containerColor = CardBg,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("OpenAI API key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = apiDraftKey,
                        onValueChange = { apiDraftKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Border,
                            focusedBorderColor = Accent,
                            unfocusedContainerColor = CardBg,
                            focusedContainerColor = CardBg,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedLabelColor = TextMuted,
                            focusedLabelColor = Accent,
                        ),
                    )
                    Text(
                        "You can always change this key later in Profile -> OpenAI API.",
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveApiKey(apiDraftKey.trim())
                        showApiKeyDialog = false
                        onDismissError()
                    },
                    enabled = canSaveApiKey,
                ) { Text("Save", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }

}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AiResultScreen(
    draft: AiMealDraft?,
    onBack: () -> Unit,
    onUpdateDraft: (AiMealDraft) -> Unit,
    onCalculateComponentCalories: suspend (String, Int) -> Result<OpenAiMealAnalyzer.ComponentCaloriesEstimate>,
    onConfirm: () -> Unit,
) {
    if (draft == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No AI result yet", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Back")
            }
        }
        return
    }

    var editingItemIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var isAddingItem by rememberSaveable { mutableStateOf(false) }
    var itemNameDraft by rememberSaveable { mutableStateOf("") }
    var itemGramsDraft by rememberSaveable { mutableStateOf("") }
    var initialItemName by rememberSaveable { mutableStateOf("") }
    var initialBaseKcal by rememberSaveable { mutableIntStateOf(0) }
    var initialBaseGrams by rememberSaveable { mutableIntStateOf(100) }
    var lastCalculatedName by rememberSaveable { mutableStateOf("") }
    var lastCalculatedBaseKcal by rememberSaveable { mutableIntStateOf(0) }
    var lastCalculatedBaseGrams by rememberSaveable { mutableIntStateOf(100) }
    var isCalculatingCalories by rememberSaveable { mutableStateOf(false) }
    var calculationNotice by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(draft.items) {
        val normalizedItems = draft.items.mapIndexed { index, item ->
            val (normalizedName, extractedWeight) = extractNameAndWeight(item.name)
            val resolvedName = normalizedName.ifBlank { "Item ${index + 1}" }
            val resolvedWeight = extractedWeight ?: item.grams
            if (resolvedName != item.name || resolvedWeight != item.grams) {
                item.copy(name = resolvedName, grams = resolvedWeight)
            } else {
                item
            }
        }
        if (normalizedItems != draft.items) {
            onUpdateDraft(
                draft.copy(
                    items = normalizedItems,
                    totalKcal = normalizedItems.sumOf { it.kcal }.coerceAtLeast(0),
                ),
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        TopAppBar(
            title = { Text("AI Analysis", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Bg,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
            ),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                CardBlock(background = Color(0x224CAF50), borderColor = Color(0x334CAF50)) {
                    Text("Draft - please review before saving", color = Color(0xFF81C784), fontSize = 13.sp)
                }
            }
            item {
                CardBlock {
                    Text(draft.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(draft.description, color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${draft.totalKcal} kcal", color = Accent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Protein ${draft.proteinG}g | Carbs ${draft.carbsG}g | Fat ${draft.fatG}g", color = TextSecondary, fontSize = 12.sp)
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Recognized items", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            isAddingItem = true
                            editingItemIndex = null
                            itemNameDraft = ""
                            itemGramsDraft = "100"
                            initialItemName = ""
                            initialBaseKcal = 0
                            initialBaseGrams = 100
                            lastCalculatedName = ""
                            lastCalculatedBaseKcal = 0
                            lastCalculatedBaseGrams = 100
                            isCalculatingCalories = false
                            calculationNotice = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x224CAF50), contentColor = Accent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Accent),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Accent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            itemsIndexed(draft.items) { index, item ->
                val parsedDisplay = extractNameAndWeight(item.name)
                val normalizedName = parsedDisplay.first
                val displayWeight = parsedDisplay.second ?: item.grams
                CardBlock(modifier = Modifier.clickable {
                    isAddingItem = false
                    val parsed = extractNameAndWeight(item.name)
                    val parsedWeight = parsed.second
                    val initialName = parsed.first.trim()
                    editingItemIndex = index
                    itemNameDraft = initialName
                    val resolvedWeight = parsedWeight ?: item.grams
                    itemGramsDraft = resolvedWeight.toString()
                    initialItemName = initialName
                    initialBaseKcal = item.kcal.coerceAtLeast(0)
                    initialBaseGrams = resolvedWeight.coerceAtLeast(1)
                    lastCalculatedName = initialName
                    lastCalculatedBaseKcal = item.kcal.coerceAtLeast(0)
                    lastCalculatedBaseGrams = resolvedWeight.coerceAtLeast(1)
                    isCalculatingCalories = false
                    calculationNotice = null
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(normalizedName.ifBlank { "Item ${index + 1}" }, color = Color.White, fontWeight = FontWeight.Medium)
                            Text("${displayWeight}g", color = TextMuted, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${item.kcal} kcal", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Border, RoundedCornerShape(14.dp)),
            ) {
                Text("Back", color = Color.White)
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(2f),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm")
            }
        }
    }

    val currentEditingIndex = editingItemIndex
    val editingItem = currentEditingIndex?.let { draft.items.getOrNull(it) }
    val isDialogVisible = isAddingItem || editingItem != null
    if (isDialogVisible) {
        val fallbackBaseGrams = if (isAddingItem) 100 else (editingItem?.grams ?: 100).coerceAtLeast(1)
        val parsedName = extractNameAndWeight(itemNameDraft)
        val parsedGrams = itemGramsDraft.toIntOrNull()?.coerceAtLeast(0)
        val resolvedName = parsedName.first.trim()
        val resolvedGrams = parsedGrams ?: parsedName.second ?: fallbackBaseGrams
        val usesInitialCalculation = resolvedName.isNotBlank() && resolvedName == initialItemName
        val usesLastCalculated = resolvedName.isNotBlank() && resolvedName == lastCalculatedName
        val hasCalculatedForCurrentName = usesInitialCalculation || usesLastCalculated
        val isNameBlank = resolvedName.isBlank()
        val isNameChanged = resolvedName != initialItemName
        val shouldShowCalculateButton = resolvedName.isNotBlank() && isNameChanged && !hasCalculatedForCurrentName
        val activeBaseKcal = when {
            usesInitialCalculation -> initialBaseKcal
            usesLastCalculated -> lastCalculatedBaseKcal
            else -> 0
        }
        val activeBaseGrams = when {
            usesInitialCalculation -> initialBaseGrams.coerceAtLeast(1)
            usesLastCalculated -> lastCalculatedBaseGrams.coerceAtLeast(1)
            else -> 1
        }
        val recalculatedKcal = if (hasCalculatedForCurrentName) {
            recalculateItemCalories(
                baseKcal = activeBaseKcal,
                baseGrams = activeBaseGrams,
                targetGrams = resolvedGrams,
            )
        } else {
            0
        }
        val showZeroCalories = isNameBlank || !hasCalculatedForCurrentName
        val canSave = resolvedName.isNotBlank() && parsedGrams != null && !shouldShowCalculateButton && !isCalculatingCalories

        Dialog(
            onDismissRequest = {
                editingItemIndex = null
                isAddingItem = false
                isCalculatingCalories = false
                calculationNotice = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBg)
                    .border(1.dp, Border, RoundedCornerShape(24.dp))
                    .padding(18.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isAddingItem) "Add component" else "Edit component",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                editingItemIndex = null
                                isAddingItem = false
                                isCalculatingCalories = false
                                calculationNotice = null
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2D2D2D)),
                        ) {
                            Text("×", color = TextSecondary, fontSize = 20.sp, textAlign = TextAlign.Center)
                        }
                    }

                    DarkTextField("Name", itemNameDraft) {
                        val parsed = extractNameAndWeight(it)
                        itemNameDraft = parsed.first
                        parsed.second?.let { grams -> itemGramsDraft = grams.toString() }
                    }
                    DarkTextField("Weight (g)", itemGramsDraft, KeyboardType.Number) { itemGramsDraft = it }
                    Text("Calories (kcal)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (shouldShowCalculateButton) {
                        Button(
                            onClick = {
                                if (resolvedName.isBlank()) {
                                    calculationNotice = "Please type a name of something edible"
                                } else {
                                    val gramsForRequest = resolvedGrams.coerceAtLeast(0)
                                    isCalculatingCalories = true
                                    calculationNotice = null
                                    scope.launch {
                                        val result = onCalculateComponentCalories(resolvedName, gramsForRequest)
                                        result
                                            .onSuccess { estimate ->
                                                if (!estimate.isEdible) {
                                                    calculationNotice = "Please type a name of something edible"
                                                } else {
                                                    lastCalculatedName = resolvedName
                                                    lastCalculatedBaseKcal = estimate.kcal.coerceAtLeast(0)
                                                    lastCalculatedBaseGrams = gramsForRequest.coerceAtLeast(1)
                                                    calculationNotice = null
                                                }
                                            }
                                            .onFailure { error ->
                                                calculationNotice = error.message?.takeIf { it.isNotBlank() }
                                                    ?: "Failed to calculate calories. Try again."
                                            }
                                        isCalculatingCalories = false
                                    }
                                }
                            },
                            enabled = parsedGrams != null && !isCalculatingCalories,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        ) {
                            Text(
                                if (isCalculatingCalories) "Calculating..." else "✨Calculate Calories",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else {
                        Text(
                            if (showZeroCalories) "0 Calories" else "${recalculatedKcal} kcal",
                            color = if (showZeroCalories) TextSecondary else Accent,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    calculationNotice?.let { message ->
                        CardBlock(background = Color(0x33FF5252), borderColor = Danger) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    message,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { calculationNotice = null },
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x66B71C1C)),
                                ) {
                                    Text("×", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val updatedItems = draft.items.toMutableList()
                            if (isAddingItem) {
                                updatedItems.add(
                                    MealItem(
                                        name = resolvedName,
                                        grams = resolvedGrams,
                                        kcal = recalculatedKcal,
                                        confidence = "High",
                                    ),
                                )
                            } else if (editingItem != null) {
                                val indexToUpdate = currentEditingIndex ?: return@Button
                                updatedItems[indexToUpdate] = editingItem.copy(
                                    name = resolvedName,
                                    grams = resolvedGrams,
                                    kcal = recalculatedKcal,
                                )
                            }
                            val recalculatedTotal = updatedItems.sumOf { it.kcal }.coerceAtLeast(0)
                            val recalculatedMacros = scaleMacrosByCalories(
                                baseCalories = draft.totalKcal,
                                baseProtein = draft.proteinG,
                                baseCarbs = draft.carbsG,
                                baseFat = draft.fatG,
                                targetCalories = recalculatedTotal,
                            )
                            onUpdateDraft(
                                draft.copy(
                                    items = updatedItems,
                                    totalKcal = recalculatedTotal,
                                    proteinG = recalculatedMacros.protein,
                                    carbsG = recalculatedMacros.carbs,
                                    fatG = recalculatedMacros.fat,
                                ),
                            )
                            editingItemIndex = null
                            isAddingItem = false
                            calculationNotice = null
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    ) {
                        Text(if (isAddingItem) "Add" else "Save changes")
                    }
                }
            }
        }
    }
}

@Composable
private fun CardBlock(
    modifier: Modifier = Modifier,
    background: Color = CardBg,
    borderColor: Color = Border,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun SelectableCard(
    selected: Boolean,
    title: String,
    description: String,
    suffix: String? = null,
    onClick: () -> Unit,
) {
    CardBlock(
        background = if (selected) Color(0x1F4CAF50) else CardBg,
        borderColor = if (selected) Accent else Border,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(description, color = TextMuted, fontSize = 13.sp)
            }
            if (suffix != null) Text(suffix, color = Accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NumberInput(label: String, value: String, unit: String, onValueChange: (String) -> Unit) {
    Text(label.uppercase(), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        trailingIcon = { Text(unit, color = Accent, fontWeight = FontWeight.SemiBold) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun FilterButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0x224CAF50) else CardBg,
            contentColor = if (selected) Accent else TextSecondary,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) Accent else Border,
        ),
    ) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
private fun CalorieRing(consumed: Int, target: Int) {
    val progress = (consumed.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val remaining = (target - consumed).coerceAtLeast(0)
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val stroke = 16.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            drawArc(
                color = Border,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = Size(diameter, diameter),
            )
            drawArc(
                color = Accent,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = Size(diameter, diameter),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$consumed", color = Accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("consumed", color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("$remaining", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("remaining", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MacroCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    CardBlock(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold)
            Text(label, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun IntakeBars(meals: List<MealEntry>, target: Int) {
    val hours = listOf(6, 8, 10, 12, 14, 16, 18, 20)
    val values = hours.map { hour ->
        meals.filter {
            Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).hour in hour until (hour + 2)
        }.sumOf { it.totalKcal }
    }
    val maxValue = maxOf(values.maxOrNull() ?: 1, target / 4, 1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        hours.forEachIndexed { index, hour ->
            val value = values[index]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .height(56.dp * (value.toFloat() / maxValue.toFloat()).coerceAtLeast(0.08f))
                        .width(12.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (value > 0) Accent else Border),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(hour.toString(), color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun LinearProgress(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Border),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxSize()
                .background(if (progress > 1f) Danger else Accent),
        )
    }
}

@Composable
private fun BarGraph(
    data: List<Pair<String, Int>>,
    target: Int,
    barColor: (Int) -> Color = { value -> if (value > target) Danger else Accent },
) {
    val maxValue = maxOf(data.maxOfOrNull { it.second } ?: 1, target, 1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        data.forEach { (label, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(34.dp)) {
                Box(
                    modifier = Modifier
                        .height((80.dp * (value.toFloat() / maxValue.toFloat()).coerceAtLeast(0.05f)))
                        .width(18.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(barColor(value)),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(label.take(3), color = TextMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun StatMiniCard(title: String, value: String, modifier: Modifier = Modifier) {
    CardBlock(modifier = modifier) {
        Text(title, color = TextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DarkTextField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    readOnly: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        minLines = minLines,
        readOnly = readOnly,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Border,
            focusedBorderColor = Accent,
            unfocusedContainerColor = CardBg,
            focusedContainerColor = CardBg,
            unfocusedTextColor = Color.White,
            focusedTextColor = Color.White,
            unfocusedLabelColor = TextMuted,
            focusedLabelColor = Accent,
        ),
    )
}

@Composable
private fun ProfileDialogField(
    label: String,
    value: String,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
) {
    Text(
        label.uppercase(),
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Border,
            focusedBorderColor = Accent,
            unfocusedContainerColor = Color(0xFF242424),
            focusedContainerColor = Color(0xFF242424),
            unfocusedTextColor = Color.White,
            focusedTextColor = Color.White,
            unfocusedLabelColor = TextMuted,
            focusedLabelColor = Accent,
        ),
        shape = RoundedCornerShape(14.dp),
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun ProfileDialogFieldCompact(
    label: String,
    value: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            label.uppercase(),
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Border,
                focusedBorderColor = Accent,
                unfocusedContainerColor = Color(0xFF242424),
                focusedContainerColor = Color(0xFF242424),
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedLabelColor = TextMuted,
                focusedLabelColor = Accent,
            ),
            shape = RoundedCornerShape(12.dp),
        )
    }
}

@Composable
private fun ModeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    CardBlock(
        modifier = Modifier.clickable(onClick = onClick),
        background = Color(0xFF1F1F1F),
        borderColor = Color(0xFF2A2A2A),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0x224CAF50))
                    .border(1.dp, Color(0x334CAF50), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Accent)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 23.sp * 0.72f)
                Text(description, color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

private fun mealBucket(timestamp: Long): String {
    val hour = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).hour
    return when (hour) {
        in 5..10 -> "Breakfast"
        in 11..15 -> "Lunch"
        in 16..21 -> "Dinner"
        else -> "Snacks"
    }
}

private fun formatTime(timestamp: Long): String {
    val dt = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalTime()
    return dt.format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun formatHourLabel(hour24: Int): String {
    val suffix = if (hour24 < 12) "am" else "pm"
    val hour12 = when (val normalized = hour24 % 12) {
        0 -> 12
        else -> normalized
    }
    return "$hour12$suffix"
}

private fun timeMinutesFromTimestamp(timestamp: Long): Int {
    val localDateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return localDateTime.hour * 60 + localDateTime.minute
}

private fun combineDateAndMinutes(timestamp: Long, minutes: Int): Long {
    val safeMinutes = minutes.coerceIn(0, 23 * 60 + 59)
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val hour = safeMinutes / 60
    val minute = safeMinutes % 60
    return date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun formatDate(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
}

private fun formatDateShort(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("MM/dd"))
}

private fun formatDateMonthDay(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
}

private fun formatMonthName(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH))
}

private fun formatWeightAxisValue(value: Float): String {
    val rounded = value.roundToInt().toFloat()
    return if (abs(value - rounded) < 0.05f) rounded.toInt().toString() else "%.1f".format(value)
}

private fun formatWeightDateInput(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ISO_LOCAL_DATE)
}

private fun parseWeightDateInputToTimestamp(value: String): Long? {
    return runCatching {
        LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}

private fun mealEmoji(meal: MealEntry): String = when (mealBucket(meal.timestamp)) {
    "Breakfast" -> "🥣"
    "Lunch" -> "🥗"
    "Dinner" -> "🍽️"
    else -> "🍎"
}

private fun uriToDataUrl(context: android.content.Context, uri: Uri): String? {
    val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)

    var sampleSize = 1
    val maxSide = 1280
    while (
        (bounds.outWidth / sampleSize) > maxSide ||
        (bounds.outHeight / sampleSize) > maxSide
    ) {
        sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize.coerceAtLeast(1)
    }
    val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
    if (bitmap != null) {
        return bitmapToDataUrl(bitmap)
    }

    val mime = context.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
    val encoded = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
    return "data:$mime;base64,$encoded"
}

private fun bitmapToDataUrl(bitmap: Bitmap): String? {
    val stream = ByteArrayOutputStream()
    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)) return null
    val encoded = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    return "data:image/jpeg;base64,$encoded"
}

private fun resolveMealPhotoModel(photoData: String?): Any? {
    val raw = photoData?.trim().orEmpty()
    if (raw.isBlank()) return null

    if (raw.startsWith("data:", ignoreCase = true)) {
        val commaIndex = raw.indexOf(',')
        if (commaIndex <= 0 || commaIndex >= raw.lastIndex) return null
        val base64 = raw.substring(commaIndex + 1)
        val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull() ?: return null
        return if (bytes.isNotEmpty()) bytes else null
    }

    return if (
        raw.startsWith("content://", ignoreCase = true) ||
        raw.startsWith("file://", ignoreCase = true)
    ) {
        Uri.parse(raw)
    } else {
        raw
    }
}

private fun onboardingGoalDefaults(goal: GoalType): Pair<Double, Double> {
    return when (goal) {
        GoalType.MAINTAIN -> 70.0 to 70.0
        GoalType.GAIN -> 60.0 to 70.0
        GoalType.LOSE -> 80.0 to 70.0
    }
}

private fun isWeightOutOfBounds(
    weightKg: Double,
    startKg: Double,
    goalKg: Double,
    goalType: GoalType,
): Boolean {
    if (goalType == GoalType.MAINTAIN) {
        val maintainToleranceKg = 4.0
        return abs(weightKg - startKg) > maintainToleranceKg + 0.0001
    }
    val minBound = minOf(startKg, goalKg)
    val maxBound = maxOf(startKg, goalKg)
    return weightKg < minBound - 0.0001 || weightKg > maxBound + 0.0001
}









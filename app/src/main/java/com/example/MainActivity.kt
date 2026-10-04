package com.example

import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import com.example.ui.settings.*
import com.example.ui.voice.VoiceNavigationDialog
import com.example.data.export.XlsxExporter
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CosmicAnimatedBackground
import com.example.ui.components.CosmicTopBar
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.GabbaiScreen
import com.example.ui.viewmodel.GabbaiViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        I18n.init(applicationContext)
        setContent {
            val context = LocalContext.current
            val store = remember { SettingsStore(context) }
            var settings by remember { mutableStateOf(store.load().also { I18n.lang = it.language; I18n.defaultCurrency = it.currency }) }
            GabbaiCosmosTheme(settings) {
                // Re-create the screen tree when the language changes so every text is refreshed.
                key(settings.language) {
                    GabbaiAppRoot(settings = settings, onSettingsChange = {
                        I18n.lang = it.language; I18n.defaultCurrency = it.currency
                        settings = it; store.save(it)
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GabbaiAppRoot(
    viewModel: GabbaiViewModel = viewModel(),
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    var showAssistantDialog by remember { mutableStateOf(false) }
    var wipeStep by remember { mutableStateOf(0) }
    var pendingRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    val isKa = LocalAppSettings.current.language == AppLanguage.KA
    fun say(ka: String, ru: String) { val msg = when (I18n.lang) { AppLanguage.KA -> ka; AppLanguage.RU -> ru; else -> L(ka) }; scope.launch { snackbarHostState.showSnackbar(msg) } }
    val backupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.backupTo(uri) { r ->
        r.onSuccess { say(Lf("სარეზერვო ასლი შეინახა ({0} ჩანაწერი)", it), "Резервная копия сохранена ($it записей)") }
         .onFailure { say(L("ასლის შენახვა ვერ მოხერხდა"), "Не удалось сохранить копию") } } }
    val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingRestore = uri }
    var showVoiceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Display status messages
    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Android back handler: pop to Dashboard if in sub-screen
    BackHandler(enabled = state.currentScreen != GabbaiScreen.DASHBOARD || drawerState.isOpen) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.setScreen(GabbaiScreen.DASHBOARD)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = LocalGabbaiPalette.current.surface,
                drawerContentColor = LocalGabbaiPalette.current.text,
                modifier = Modifier.width(320.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Drawer Header
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CosmicCelestialGold.copy(alpha = 0.2f))
                                .border(1.dp, CosmicCelestialGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = CosmicCelestialGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gabbai Cosmos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CosmicTextPrimary
                            )
                             Text(
                                 text = tr("სინაგოგისა და ქოლელის მართვა", "Управление синагогой и коллелем"),
                                fontSize = 11.sp,
                                color = CosmicCelestialGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Items
                GabbaiScreen.values().forEach { screen ->
                    val isSelected = state.currentScreen == screen
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    GabbaiScreen.DASHBOARD -> Icons.Default.Dashboard
                                    GabbaiScreen.FINANCES -> Icons.Default.Payments
                                    GabbaiScreen.ALIYOT -> Icons.Default.MenuBook
                                    GabbaiScreen.MEMBERS -> Icons.Default.People
                                    GabbaiScreen.KOLLEL -> Icons.Default.School
                                    GabbaiScreen.TZEDAKAH -> Icons.Default.VolunteerActivism
                                    GabbaiScreen.EVENTS -> Icons.Default.Celebration
                                    GabbaiScreen.SECURITY -> Icons.Default.Security
                                    GabbaiScreen.TECH_DOCS -> Icons.Default.Description
                                    GabbaiScreen.SETTINGS -> Icons.Default.Settings
                                },
                                    contentDescription = screen.title,
                                tint = if (isSelected) CosmicStardustCyan else CosmicTextSecondary
                            )
                        },
                        label = {
                            Column {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                     color = if (isSelected) MaterialTheme.colorScheme.primary else LocalGabbaiPalette.current.text,
                                     maxLines = 1,
                                     softWrap = false,
                                     overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = nb(if (settings.language == AppLanguage.HE) screen.titleRu else screen.titleHe),
                                    fontSize = 10.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary else LocalGabbaiPalette.current.mutedText,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        selected = isSelected,
                        onClick = {
                            viewModel.setScreen(screen)
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = CosmicStardustCyan.copy(alpha = 0.15f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        CosmicAnimatedBackground {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    CosmicTopBar(
                        currentTitle = state.currentScreen.title,
                        subtitle = tr("სინაგოგა და ქოლელი", "Синагога и коллель"),
                        isVaultUnlocked = state.isVaultUnlocked,
                        onToggleVault = { viewModel.toggleVaultLock() },
                        onOpenDocs = { viewModel.setScreen(GabbaiScreen.TECH_DOCS) },
                        onOpenAssistant = { showAssistantDialog = true },
                        onOpenVoice = { showVoiceDialog = true },
                        onToggleTheme = { onSettingsChange(settings.copy(lighting = if (settings.lighting == LightingMode.NIGHT) LightingMode.DAY else LightingMode.NIGHT)) },
                        onToggleLanguage = { onSettingsChange(settings.copy(language = AppLanguage.values()[(settings.language.ordinal + 1) % AppLanguage.values().size])) },
                        onOpenSettings = { viewModel.setScreen(GabbaiScreen.SETTINGS) },
                        isNight = settings.lighting == LightingMode.NIGHT,
                        languageCode = settings.language.name,
                        onBackup = { backupLauncher.launch("gabbai-backup-" + java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US).format(java.util.Date()) + ".json") },
                        onRestore = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        onWipe = { wipeStep = 1 },
                        modifier = Modifier.statusBarsPadding()
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = LocalGabbaiPalette.current.glass,
                        contentColor = LocalGabbaiPalette.current.text,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = tr("მთავარი", "Главная")) },
                            label = { Text(tr("მთავარი", "Главная"), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = state.currentScreen == GabbaiScreen.DASHBOARD,
                            onClick = { viewModel.setScreen(GabbaiScreen.DASHBOARD) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CosmicStardustCyan,
                                indicatorColor = CosmicStardustCyan.copy(alpha = 0.2f),
                                unselectedIconColor = CosmicTextSecondary
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Payments, contentDescription = tr("ფინანსები", "Финансы")) },
                            label = { Text(tr("ფინანსები", "Финансы"), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = state.currentScreen == GabbaiScreen.FINANCES,
                            onClick = { viewModel.setScreen(GabbaiScreen.FINANCES) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CosmicEmeraldSuccess,
                                indicatorColor = CosmicEmeraldSuccess.copy(alpha = 0.2f),
                                unselectedIconColor = CosmicTextSecondary
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = tr("ალიები", "Алиёт")) },
                            label = { Text(tr("ალიები", "Алиёт"), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = state.currentScreen == GabbaiScreen.ALIYOT,
                            onClick = { viewModel.setScreen(GabbaiScreen.ALIYOT) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CosmicCelestialGold,
                                indicatorColor = CosmicCelestialGold.copy(alpha = 0.2f),
                                unselectedIconColor = CosmicTextSecondary
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.School, contentDescription = tr("ქოლელი", "Коллель")) },
                            label = { Text(tr("ქოლელი", "Коллель"), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = state.currentScreen == GabbaiScreen.KOLLEL,
                            onClick = { viewModel.setScreen(GabbaiScreen.KOLLEL) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CosmicNebulaPurple,
                                indicatorColor = CosmicNebulaPurple.copy(alpha = 0.2f),
                                unselectedIconColor = CosmicTextSecondary
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Apps, contentDescription = tr("მენიუ", "Меню")) },
                            label = { Text(tr("მოდულები", "Модули"), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = state.currentScreen !in listOf(
                                GabbaiScreen.DASHBOARD,
                                GabbaiScreen.FINANCES,
                                GabbaiScreen.ALIYOT,
                                GabbaiScreen.KOLLEL
                            ),
                            onClick = { scope.launch { drawerState.open() } },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CosmicAuroraBlue,
                                indicatorColor = CosmicAuroraBlue.copy(alpha = 0.2f),
                                unselectedIconColor = CosmicTextSecondary
                            )
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = Modifier.fillMaxSize()
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    AnimatedContent(
                        targetState = state.currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "screen_transition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            GabbaiScreen.DASHBOARD -> DashboardScreen(
                                state = state,
                                onNavigate = { viewModel.setScreen(it) },
                                onOpenVoice = { showVoiceDialog = true },
                                onExport = { XlsxExporter.share(context, state) }
                            )
                            GabbaiScreen.FINANCES -> FinancesScreen(viewModel = viewModel)
                            GabbaiScreen.ALIYOT -> AliyotScreen(viewModel = viewModel)
                            GabbaiScreen.MEMBERS -> MembersScreen(viewModel = viewModel)
                            GabbaiScreen.KOLLEL -> KollelScreen(viewModel = viewModel)
                            GabbaiScreen.TZEDAKAH -> TzedakahScreen(viewModel = viewModel)
                            GabbaiScreen.EVENTS -> EventsScreen(viewModel = viewModel)
                            GabbaiScreen.SECURITY -> SecurityScreen(viewModel = viewModel)
                            GabbaiScreen.TECH_DOCS -> TechDocsScreen()
                            GabbaiScreen.SETTINGS -> SettingsScreen(
                                settings = settings,
                                onSettingsChange = onSettingsChange,
                                onBackup = { backupLauncher.launch("gabbai-backup-" + java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US).format(java.util.Date()) + ".json") },
                                onRestore = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                                onWipe = { wipeStep = 1 },
                                onExport = { XlsxExporter.share(context, state) }
                            )
                        }
                    }
                }
            }

            pendingRestore?.let { uri ->
                AlertDialog(
                    onDismissRequest = { pendingRestore = null },
                    title = { Text(tr("აღვადგინო ასლიდან?", "Восстановить из копии?")) },
                    text = { Text(tr("ახლანდელი მონაცემები შეიცვლება ფაილში შენახულით.", "Текущие данные будут заменены данными из файла.")) },
                    confirmButton = { Button(onClick = {
                        pendingRestore = null
                        viewModel.restoreFrom(uri) { r ->
                            r.onSuccess { say(Lf("აღდგენილია: {0} ჩანაწერი", it), "Восстановлено записей: $it") }
                             .onFailure { say(L("ფაილი არ არის სწორი სარეზერვო ასლი"), "Файл не является корректной копией") } }
                    }) { Text(tr("აღდგენა", "Восстановить")) } },
                    dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(tr("გაუქმება", "Отмена")) } }
                )
            }
            if (wipeStep > 0) {
                AlertDialog(
                    onDismissRequest = { wipeStep = 0 },
                    icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text(if (wipeStep == 1) tr("ბაზის სრული გასუფთავება", "Полная очистка базы") else tr("ნამდვილად წავშალო ყველაფერი?", "Точно удалить всё?")) },
                    text = { Text(if (wipeStep == 1) tr("წაიშლება ყველა წევრი, ფინანსი, ალია, ქოლელი, ცედაკა და ღონისძიება. რეკომენდებულია ჯერ სარეზერვო ასლის შექმნა.", "Будут удалены все прихожане, финансы, алиёт, коллель, цдака и события. Сначала рекомендуется создать резервную копию.") else tr("ამ მოქმედების გაუქმება შეუძლებელია.", "Это действие нельзя отменить.")) },
                    confirmButton = { Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            if (wipeStep == 1) wipeStep = 2 else {
                                wipeStep = 0
                                viewModel.wipeDatabase { r -> r.onSuccess { say(L("ბაზა გასუფთავდა — იწყებთ ცარიელიდან"), "База очищена — начинаете с нуля") } }
                            }
                        }) { Text(if (wipeStep == 1) tr("გაგრძელება", "Продолжить") else tr("ყველაფრის წაშლა", "Удалить всё")) } },
                    dismissButton = { TextButton(onClick = { wipeStep = 0 }) { Text(tr("გაუქმება", "Отмена")) } }
                )
            }
            if (showAssistantDialog) {
                com.example.ui.components.SynagogueAssistantDialog(
                    viewModel = viewModel,
                    onDismiss = { showAssistantDialog = false }
                )
            }
            if (showVoiceDialog) {
                VoiceNavigationDialog(
                    onCommand = { text, command ->
                        command.screen?.let(viewModel::setScreen)
                        command.search?.let(viewModel::setSearchQuery)
                    },
                    onDismiss = { showVoiceDialog = false }
                )
            }
        }
    }
}

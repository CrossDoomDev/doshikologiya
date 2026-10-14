package ru.doshikologiya.nativepreview

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.yandex.mobile.ads.common.YandexAds
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var adsReady by mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15+ draws apps beneath system bars by default. Let Compose
        // manage the safe viewport in one place instead of adding hard-coded
        // dp offsets to individual screens.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        YandexAds.setUserConsent(false)
        YandexAds.setLocationTracking(false)
        if (BuildConfig.DEBUG) {
            YandexAds.initialize(this) { runOnUiThread { adsReady = true } }
        }
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Amber, onPrimary = Night, background = Night,
                    onBackground = Cream, surface = Surface, onSurface = Cream
                )
            ) { DoshikNativeApp(adsReady) }
        }
    }
}

internal sealed interface Screen {
    data object Home : Screen
    data object Library : Screen
    data object Oracle : Screen
    data object Wall : Screen
    data object More : Screen
    data class Detail(val recipe: Recipe, val origin: Screen) : Screen
    data class Cooking(val recipe: Recipe, val origin: Screen) : Screen
    data class Success(val recipe: Recipe, val origin: Screen) : Screen
}

@Composable
private fun DoshikNativeApp(adsReady: Boolean) {
    val context = LocalContext.current
    // Catalog loading never depends on SDK/network or UI initialization.
    var recipes by remember(context) { mutableStateOf(
        runCatching { RecipeCatalog.load(context) }.getOrElse {
            Log.e("DoshikNative", "Offline catalog could not be loaded", it)
            emptyList()
        }
    ) }
    val content = remember(context) { ExtraCatalog.load(context) }
    val prefs = remember(context) { context.getSharedPreferences("native_favorites", 0) }
    val favorites = remember {
        mutableStateListOf<String>().apply { addAll(prefs.getStringSet("ids", emptySet()).orEmpty()) }
    }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var query by remember { mutableStateOf("") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Все") }
    var layout by remember {
        mutableStateOf(RecipeLayout.entries.find {
            it.name.equals(prefs.getString("recipe_layout", "LARGE"), ignoreCase = true)
        } ?: RecipeLayout.LARGE)
    }
    var step by remember { mutableIntStateOf(0) }
    var confirmExit by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1150)
        showSplash = false
    }
    fun toggleFavorite(id: String) {
        if (id in favorites) favorites.remove(id) else favorites.add(id)
        prefs.edit().putStringSet("ids", favorites.toSet()).apply()
    }
    fun openDetail(recipe: Recipe, from: Screen) {
        screen = Screen.Detail(recipe, from)
    }
    fun goBack() {
        screen = when (val current = screen) {
            is Screen.Cooking -> Screen.Detail(current.recipe, current.origin)
            is Screen.Success -> Screen.Detail(current.recipe, current.origin)
            is Screen.Detail -> current.origin
            else -> Screen.Home
        }
    }
    BackHandler(enabled = !showSplash) {
        if (screen == Screen.Home) confirmExit = true else goBack()
    }
    val tab = when (val current = screen) {
        is Screen.Detail -> current.origin
        is Screen.Cooking -> current.origin
        is Screen.Success -> current.origin
        else -> current
    }
    val homeScreen = screen == Screen.Home
    // This is the SINGLE safe-area boundary for all native screens, tabs
    // and the splash. Draw the background beneath system bars, but keep
    // interactive content below status bar and above Android navigation.
    Box(
        Modifier.fillMaxSize().background(Night)
            .safeDrawingPadding()
            .imePadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            if (screen is Screen.Cooking) {
                CookingHeader((screen as Screen.Cooking).recipe.title, ::goBack)
            } else if (screen is Screen.Detail || screen is Screen.Success) {
                NativeHeader("Лабораторный протокол", ::goBack)
            }
            Box(Modifier.weight(1f)) {
                when (val page = screen) {
                    Screen.Home -> HomePage(recipes, content,
                        open = { openDetail(it, Screen.Home) },
                        all = { screen = Screen.Library },
                        oracle = { screen = Screen.Oracle },
                        wall = { screen = Screen.Wall })
                    Screen.Library -> LibraryPage(
                        recipes = recipes, query = query, onQuery = { query = it },
                        onlyFavorites = onlyFavorites, onFavorites = { onlyFavorites = !onlyFavorites },
                        category = category, onCategory = { category = it },
                        categoryOrder = content.categoryOrder,
                        layout = layout, onLayout = {
                            layout = it
                            prefs.edit().putString("recipe_layout", it.name).apply()
                        },
                        favorites = favorites.toSet(),
                        toggleFavorite = ::toggleFavorite,
                        open = { openDetail(it, Screen.Library) }
                    )
                    Screen.Oracle -> OraclePage(recipes) { openDetail(it, Screen.Oracle) }
                    Screen.Wall -> WallPage(content, onSupport = { openExternal(context, content.donateUrl) })
                    Screen.More -> MorePage(
                        content = content, recipes = recipes,
                        onCatalogUpdated = { recipes = it },
                        onSupport = {
                        if (content.supportEmail.isNotBlank()) {
                            openExternal(context, "mailto:" + content.supportEmail)
                        }
                    })
                    is Screen.Detail -> DetailPage(
                        recipe = page.recipe, selected = page.recipe.id in favorites,
                        onFavorite = { toggleFavorite(page.recipe.id) },
                        onCook = {
                            step = 0
                            screen = Screen.Cooking(page.recipe, page.origin)
                        },
                        onSupport = { openExternal(context, content.donateUrl) }
                    )
                    is Screen.Cooking -> CookingPage(
                        recipe = page.recipe, step = step, adsReady = adsReady,
                        previous = { if (step > 0) step-- },
                        next = {
                            if (step + 1 < page.recipe.steps.size) step++
                            else screen = Screen.Success(page.recipe, page.origin)
                        },
                        onExit = ::goBack
                    )
                    is Screen.Success -> SuccessPage(
                        recipe = page.recipe,
                        selected = page.recipe.id in favorites,
                        onFavorite = { toggleFavorite(page.recipe.id) },
                        onReturn = { screen = Screen.Detail(page.recipe, page.origin) }
                    )
                }
            }
            if (screen !is Screen.Detail && screen !is Screen.Cooking && screen !is Screen.Success) {
                NativeTabs(selected = tab) { screen = it }
            }
        }
        if (confirmExit) {
            AlertDialog(
                onDismissRequest = { confirmExit = false },
                title = { Text("Закрыть Дошикологию?") },
                text = { Text("Лаборатория будет ждать твоего возвращения.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmExit = false
                        (context as? android.app.Activity)?.finish()
                    }) { Text("Да, выйти") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmExit = false }) { Text("Остаться") }
                }
            )
        }
        AnimatedVisibility(
            visible = showSplash, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) { BrandSplash() }
    }
}

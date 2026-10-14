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
import android.content.Intent
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.Saver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.tween
import kotlinx.coroutines.CancellationException
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var incomingLink by mutableStateOf<Uri?>(null)
    private var linkRequest by mutableLongStateOf(0)
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
        incomingLink = intent?.data
        linkRequest = savedInstanceState?.getLong("linkRequest") ?: 0
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Amber, onPrimary = Night, background = Night,
                    onBackground = Cream, surface = Surface, onSurface = Cream
                )
            ) { DoshikNativeApp(incomingLink, linkRequest) }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingLink = intent.data
        linkRequest++
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("linkRequest", linkRequest)
        super.onSaveInstanceState(outState)
    }

}

internal sealed interface Screen {
    data object Home : Screen
    data object Library : Screen
    data object Oracle : Screen
    data object Wall : Screen
    data object More : Screen
    data object LegalPrivacy : Screen
    data object LegalTerms : Screen
    data class Detail(val recipe: Recipe, val origin: Screen) : Screen
    data class Cooking(val recipe: Recipe, val origin: Screen) : Screen
    data class Success(val recipe: Recipe, val origin: Screen) : Screen
}

@Composable
internal fun DoshikNativeApp(incomingLink: Uri? = null, linkRequest: Long = 0) {
    val context = LocalContext.current
    // Catalog loading never depends on SDK/network or UI initialization.
    val bundledRecipes by remember(context) { mutableStateOf(
        runCatching { RecipeCatalog.load(context) }.getOrElse {
            Log.e("DoshikNative", "Offline catalog could not be loaded", it)
            emptyList()
        }
    ) }
    val session: AppSession = viewModel()
    val recipes = session.installedRecipes ?: bundledRecipes
    var content by remember(context) { mutableStateOf(ServiceContacts.cached(context, ExtraCatalog.load(context))) }
    val pageStates = rememberSaveableStateHolder()
    val prefs = remember(context) { context.getSharedPreferences("native_favorites", 0) }
    val favorites = remember {
        mutableStateListOf<String>().apply { addAll(prefs.getStringSet("ids", emptySet()).orEmpty()) }
    }
    val screenSaver = remember(recipes) {
        Saver<Screen, String>(
            save = { it.encode() },
            restore = { ScreenCodec.decode(it, recipes) }
        )
    }
    var screen by rememberSaveable(stateSaver = screenSaver) { mutableStateOf<Screen>(Screen.Home) }
    var openedLinkRequest by rememberSaveable { mutableLongStateOf(-1) }
    LaunchedEffect(incomingLink, linkRequest, recipes) {
        val id = incomingLink?.takeIf {
            it.scheme == "https" && it.host == "crossdoomdev.github.io" &&
                it.path in listOf("/doshikologiya/", "/doshikologiya")
        }?.getQueryParameter("recipe")
        if (id != null && openedLinkRequest != linkRequest) {
            recipes.find { it.id == id }?.let {
                pageStates.removeState(Screen.Detail(it, Screen.Library).encode())
                screen = Screen.Detail(it, Screen.Library)
                openedLinkRequest = linkRequest
            }
        }
    }
    LaunchedEffect(Unit) {
        try { content = ServiceContacts.refresh(context, content) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { /* Cached or bundled contacts remain available offline. */ }
    }
    LaunchedEffect(screen) {
        if (screen == Screen.More) {
            try { content = ServiceContacts.refresh(context, content) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { }
        }
    }
    var query by rememberSaveable { mutableStateOf("") }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf("Все") }
    var layout by remember {
        mutableStateOf(RecipeLayout.entries.find {
            it.name.equals(prefs.getString("recipe_layout", "LARGE"), ignoreCase = true)
        } ?: RecipeLayout.LARGE)
    }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    var showSplash by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(760)
        showSplash = false
    }
    fun toggleFavorite(id: String) {
        if (id in favorites) favorites.remove(id) else favorites.add(id)
        prefs.edit().putStringSet("ids", favorites.toSet()).apply()
    }
    fun openDetail(recipe: Recipe, from: Screen) {
        pageStates.removeState(Screen.Detail(recipe, from).encode())
        screen = Screen.Detail(recipe, from)
    }
    fun goBack() {
        screen = when (val current = screen) {
            is Screen.Cooking -> Screen.Detail(current.recipe, current.origin)
            is Screen.Success -> Screen.Detail(current.recipe, current.origin)
            is Screen.Detail -> current.origin
            Screen.LegalPrivacy, Screen.LegalTerms -> Screen.More
            else -> { pageStates.removeState("home"); Screen.Home }
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
    // This is the SINGLE safe-area boundary for all native screens, tabs
    // and the splash. Draw the background beneath system bars, but keep
    // interactive content below status bar and above Android navigation.
    CompositionLocalProvider(LocalTextStyle provides TextStyle(
        fontFamily = FontFamily.SansSerif, fontSize = 16.sp, letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )) {
    Box(
        Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF120E0A), Color(0xFF0F0C09), Color(0xFF0D0A08))))
            drawRect(Brush.radialGradient(listOf(Amber.copy(alpha = .12f), Color.Transparent),
                center = Offset(size.width * .18f, -size.height * .05f), radius = size.height * .55f))
            drawRect(Brush.radialGradient(listOf(Orange.copy(alpha = .08f), Color.Transparent),
                center = Offset(size.width * .95f, size.height * .08f), radius = size.height * .4f))
        }.safeDrawingPadding()
            .imePadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            if (screen is Screen.Cooking) {
                CookingHeader(
                    (screen as Screen.Cooking).recipe.title,
                    (screen as Screen.Cooking).recipe.category,
                    (screen as Screen.Cooking).recipe.steps.size,
                    ::goBack
                )
            } else if (screen == Screen.LegalPrivacy || screen == Screen.LegalTerms) {
                NativeHeader("Документы института", ::goBack)
            }
            Box(Modifier.weight(1f)) {
              pageStates.SaveableStateProvider(screen.encode()) {
                when (val page = screen) {
                    Screen.Home -> HomePage(recipes, content,
                        open = { openDetail(it, Screen.Home) },
                        all = { screen = Screen.Library },
                        oracle = { screen = Screen.Oracle },
                        donate = { openExternal(context, content.donateUrl) })
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
                    Screen.Oracle -> OraclePage(recipes, session) { openDetail(it, Screen.Oracle) }
                    Screen.Wall -> WallPage(content, onSupport = { openExternal(context, content.donateUrl) })
                    Screen.LegalPrivacy -> LegalPage("privacy", { screen = Screen.LegalTerms }, ::goBack)
                    Screen.LegalTerms -> LegalPage("terms", { screen = Screen.LegalPrivacy }, ::goBack)
                    Screen.More -> MorePage(
                        content = content, recipes = recipes,
                        session = session,
                        onPrivacy = { screen = Screen.LegalPrivacy },
                        onTerms = { screen = Screen.LegalTerms },
                        onSupport = {
                        if (content.supportEmail.isNotBlank()) {
                            openExternal(context, "mailto:" + content.supportEmail)
                        }
                    })
                    is Screen.Detail -> DetailPage(
                        recipe = page.recipe, selected = page.recipe.id in favorites,
                        onClose = ::goBack,
                        onFavorite = { toggleFavorite(page.recipe.id) },
                        onCook = {
                            step = 0
                            screen = Screen.Cooking(page.recipe, page.origin)
                        },
                        onSupport = { openExternal(context, content.donateUrl) }
                    )
                    is Screen.Cooking -> CookingPage(
                        recipe = page.recipe, step = step,
                        previous = { if (step > 0) step-- },
                        next = {
                            if (step + 1 < page.recipe.steps.size) step++
                            else screen = Screen.Success(page.recipe, page.origin)
                        },
                        onExit = ::goBack
                    )
                    is Screen.Success -> {
                        LaunchedEffect(page.recipe.id) { MagicChime.play() }
                        SuccessPage(
                        recipe = page.recipe,
                        selected = page.recipe.id in favorites,
                        onFavorite = { toggleFavorite(page.recipe.id) },
                        onReturn = { screen = Screen.Detail(page.recipe, page.origin) },
                        onDone = { screen = page.origin }
                        )
                    }
                }
              }
            }
            if (screen !is Screen.Detail && screen !is Screen.Cooking &&
                screen !is Screen.Success && screen != Screen.LegalPrivacy &&
                screen != Screen.LegalTerms) {
                NativeTabs(selected = tab) {
                    if (screen != it) pageStates.removeState(it.encode())
                    screen = it
                }
            }
        }
        if (confirmExit) {
            ExitDialog(onStay = { confirmExit = false }, onExit = {
                confirmExit = false
                (context as? android.app.Activity)?.finish()
            })
        }
        AnimatedVisibility(
            visible = showSplash, enter = fadeIn(), exit = fadeOut(tween(280)),
            modifier = Modifier.fillMaxSize()
        ) { BrandSplash() }
    }
    }
}

internal fun Screen.encode(): String = when (this) {
    Screen.Home -> "home"
    Screen.Library -> "recipes"
    Screen.Oracle -> "oracle"
    Screen.Wall -> "wall"
    Screen.More -> "more"
    Screen.LegalPrivacy -> "privacy"
    Screen.LegalTerms -> "terms"
    is Screen.Detail -> "detail|${recipe.id}|${origin.encode()}"
    is Screen.Cooking -> "cook|${recipe.id}|${origin.encode()}"
    is Screen.Success -> "success|${recipe.id}|${origin.encode()}"
}

internal object ScreenCodec {
    fun decode(value: String, recipes: List<Recipe>): Screen {
        val parts = value.split('|')
        return when (parts[0]) {
            "recipes" -> Screen.Library
            "oracle" -> Screen.Oracle
            "wall" -> Screen.Wall
            "more" -> Screen.More
            "privacy" -> Screen.LegalPrivacy
            "terms" -> Screen.LegalTerms
            "detail", "cook", "success" -> {
                val recipe = recipes.find { it.id == parts.getOrNull(1) } ?: return Screen.Home
                val origin = when (parts.getOrNull(2)) {
                    "recipes" -> Screen.Library
                    "oracle" -> Screen.Oracle
                    else -> Screen.Home
                }
                when (parts[0]) {
                    "cook" -> Screen.Cooking(recipe, origin)
                    "success" -> Screen.Success(recipe, origin)
                    else -> Screen.Detail(recipe, origin)
                }
            }
            else -> Screen.Home
        }
    }
}

package ru.doshikologiya.nativepreview

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.YandexAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Background = Color(0xFF120E0A)
private val Surface = Color(0xFF281B12)
private val Highlight = Color(0xFFFFB229)
private val Subdued = Color(0xFFD1B99B)
private val Border = Color(0xFF63462C)

class MainActivity : ComponentActivity() {
    private var adsReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Consent and location tracking are never inferred from app installation.
        YandexAds.setUserConsent(false)
        YandexAds.setLocationTracking(false)
        if (BuildConfig.DEBUG) {
            YandexAds.initialize(this) { runOnUiThread { adsReady = true } }
        }
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Highlight,
                    onPrimary = Background,
                    background = Background,
                    onBackground = Color.White,
                    surface = Surface,
                    onSurface = Color.White
                )
            ) { DoshikApp(adsReady) }
        }
    }
}

private sealed interface Screen {
    data object Home : Screen
    data object Library : Screen
    data object Oracle : Screen
    data object More : Screen
    data class Detail(val recipe: Recipe) : Screen
    data class Cooking(val recipe: Recipe) : Screen
}

@Composable
private fun DoshikApp(adsReady: Boolean) {
    val context = LocalContext.current
    // The packaged catalog loads first. Network and SDK availability cannot
    // interrupt browsing offline recipes.
    val recipes = remember(context) {
        runCatching { RecipeCatalog.load(context) }.getOrElse {
            Log.e("DoshikNative", "Local recipe archive could not be read", it)
            emptyList()
        }
    }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var step by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val prefs = remember(context) { context.getSharedPreferences("native_favorites", 0) }
    val favorites = remember {
        mutableStateListOf<String>().apply { addAll(prefs.getStringSet("ids", emptySet()).orEmpty()) }
    }
    fun toggleFavorite(id: String) {
        if (id in favorites) favorites.remove(id) else favorites.add(id)
        prefs.edit().putStringSet("ids", favorites.toSet()).apply()
    }
    fun goBack() {
        screen = when (val current = screen) {
            is Screen.Cooking -> Screen.Detail(current.recipe)
            is Screen.Detail -> Screen.Library
            else -> Screen.Home
        }
    }
    BackHandler(enabled = screen !is Screen.Home) { goBack() }

    Column(Modifier.fillMaxSize().background(Background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (screen is Screen.Cooking || screen is Screen.Detail) {
                Text("←", color = Highlight, fontSize = 26.sp,
                    modifier = Modifier.clickable { goBack() }.padding(end = 16.dp))
            }
            Column {
                Text("ДОШИКОЛОГИЯ", color = Highlight,
                    fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("KOTLIN • НАТИВНЫЙ ПРОТОТИП", color = Subdued, fontSize = 10.sp)
            }
        }
        HorizontalDivider(color = Border)
        Box(Modifier.weight(1f)) {
            when (val page = screen) {
                Screen.Home -> HomePage(recipes,
                    open = { screen = Screen.Detail(it) },
                    all = { screen = Screen.Library })
                Screen.Library -> LibraryPage(recipes, query, onQuery = { query = it },
                    open = { screen = Screen.Detail(it) })
                Screen.Oracle -> OraclePage(recipes, open = { screen = Screen.Detail(it) })
                Screen.More -> InfoPage()
                is Screen.Detail -> DetailPage(page.recipe, page.recipe.id in favorites,
                    favorite = { toggleFavorite(page.recipe.id) },
                    cook = { step = 0; screen = Screen.Cooking(page.recipe) })
                is Screen.Cooking -> CookingPage(page.recipe, step, adsReady,
                    previous = { if (step > 0) step-- },
                    next = {
                        if (step + 1 < page.recipe.steps.size) step++
                        else screen = Screen.Detail(page.recipe)
                    })
            }
        }
        if (screen == Screen.Home || screen == Screen.Library ||
            screen == Screen.Oracle || screen == Screen.More) {
            NavigationBar(containerColor = Surface) {
                listOf(
                    Triple("⌂", "Главная", Screen.Home),
                    Triple("☷", "Рецепты", Screen.Library),
                    Triple("✦", "Оракул", Screen.Oracle),
                    Triple("⋯", "Ещё", Screen.More)
                ).forEach { (symbol, label, destination) ->
                    NavigationBarItem(
                        selected = screen == destination,
                        onClick = { screen = destination },
                        icon = { Text(symbol, fontSize = 22.sp) },
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomePage(recipes: List<Recipe>, open: (Recipe) -> Unit, all: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RoundedPanel {
            Text("ИНСТИТУТ ЛАПШИ", color = Highlight, fontSize = 14.sp,
                fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("Кулинарные эксперименты из того, что найдётся под рукой.",
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("${recipes.size} рецептов внутри приложения · без интернета",
                color = Subdued, fontSize = 13.sp)
        }
        if (recipes.isEmpty()) {
            Text("Встроенный каталог недоступен — проверь содержимое APK.",
                color = Color(0xFFFF7777))
        } else {
            Text("Протокол дня", color = Highlight, fontSize = 21.sp,
                fontWeight = FontWeight.Bold)
            RecipeTile(recipes.first(), open)
            Text("Недавние протоколы", color = Highlight, fontSize = 20.sp,
                fontWeight = FontWeight.Bold)
            recipes.drop(1).take(4).forEach { RecipeTile(it, open) }
            Button(onClick = all, modifier = Modifier.fillMaxWidth()) {
                Text("Все рецепты →")
            }
        }
    }
}

@Composable
private fun LibraryPage(recipes: List<Recipe>, query: String,
    onQuery: (String) -> Unit, open: (Recipe) -> Unit
) {
    val filtered = remember(recipes, query) {
        recipes.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = query, onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true, label = { Text("Поиск рецепта") })
        Text("Рецептов: ${filtered.size}", color = Subdued, fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(11.dp),
            contentPadding = PaddingValues(bottom = 18.dp)) {
            items(filtered, key = { it.id }) { RecipeTile(it, open) }
        }
    }
}

@Composable
private fun RecipeTile(recipe: Recipe, open: (Recipe) -> Unit) {
    Row(Modifier.fillMaxWidth().border(1.dp, Border, RoundedCornerShape(18.dp))
        .background(Surface, RoundedCornerShape(18.dp))
        .clickable { open(recipe) }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RecipeImage(recipe.imagePath, Modifier.size(94.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(recipe.category.uppercase(), color = Highlight, fontSize = 10.sp)
            Text(recipe.title, fontWeight = FontWeight.Bold, color = Color.White,
                fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(recipe.description, color = Subdued, fontSize = 12.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(recipe.time, color = Highlight, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DetailPage(recipe: Recipe, selected: Boolean,
    favorite: () -> Unit, cook: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RecipeImage(recipe.imagePath, Modifier.fillMaxWidth().height(220.dp))
        Text(recipe.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text(recipe.description, color = Subdued, fontSize = 15.sp)
        SuggestionChip(onClick = favorite,
            label = { Text(if (selected) "♥ В избранном" else "♡ В избранное") })
        Button(onClick = cook, modifier = Modifier.fillMaxWidth()) {
            Text("🍜 Начать готовку")
        }
        Text("Ингредиенты", color = Highlight, fontSize = 21.sp,
            fontWeight = FontWeight.Bold)
        recipe.ingredients.forEach { Text("• $it", color = Color.White) }
        Text("История", color = Highlight, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(recipe.story, color = Subdued)
    }
}

@Composable
private fun CookingPage(recipe: Recipe, step: Int, adsReady: Boolean,
    previous: () -> Unit, next: () -> Unit
) {
    // True native scrolling. The image, text, SDK view and controls are
    // all children of the SAME Compose scroll container.
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RecipeImage(recipe.imagePath, Modifier.fillMaxWidth().height(175.dp))
        Text(recipe.title, color = Color.White, fontSize = 20.sp,
            fontWeight = FontWeight.Bold)
        LinearProgressIndicator(progress = { (step + 1).toFloat() / recipe.steps.size },
            modifier = Modifier.fillMaxWidth(), color = Highlight)
        RoundedPanel {
            Text("ШАГ ${step + 1} ИЗ ${recipe.steps.size}", color = Highlight,
                fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Text(recipe.steps[step], color = Color.White,
                fontSize = 19.sp, lineHeight = 28.sp)
        }
        if (BuildConfig.DEBUG) {
            Text("ТЕСТОВЫЙ INLINE-БАННЕР ЯНДЕКСА", color = Subdued, fontSize = 10.sp)
            if (adsReady) DemoInlineBanner()
            else Text("Инициализируем тестовую рекламу…", color = Subdued)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = previous, enabled = step > 0,
                modifier = Modifier.weight(1f)) { Text("← Назад") }
            Button(onClick = next, modifier = Modifier.weight(1f)) {
                Text(if (step + 1 == recipe.steps.size) "Готово ✓" else "Далее →")
            }
        }
        Spacer(Modifier.height(38.dp))
    }
}

@Composable
private fun DemoInlineBanner() {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val widthDp = (config.screenWidthDp - 32).coerceAtLeast(200)
    val view = remember(context, widthDp) {
        BannerAdView(context).apply {
            setAdSize(BannerAdSize.inline(context, widthDp, 220))
            setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdLoaded() {
                    Log.d("DoshikNativeAds", "Inline demo loaded")
                }
                override fun onAdFailedToLoad(error: AdRequestError) {
                    Log.e("DoshikNativeAds", "Inline demo failed: $error")
                }
                override fun onAdClicked() {}
                override fun onImpression(data: ImpressionData?) {}
            })
        }
    }
    DisposableEffect(view) {
        view.loadAd(AdRequest.Builder("demo-banner-yandex").build())
        onDispose { view.destroy() }
    }
    AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun OraclePage(recipes: List<Recipe>, open: (Recipe) -> Unit) {
    val pick = remember(recipes) { recipes.randomOrNull() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("🔮 Оракул лапши", color = Highlight,
            fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Пока первый нативный прототип. Полную магию Оракула добавим позже.",
            color = Subdued)
        if (pick != null) RecipeTile(pick, open)
    }
}

@Composable
private fun InfoPage() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("О нативной версии", color = Highlight,
            fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Text("Независимое Android-приложение на Kotlin и Jetpack Compose. " +
            "50 рецептов доступны автономно. Демореклама отображается " +
            "не поверх WebView, а в самом прокручиваемом экране готовки.",
            color = Color.White, lineHeight = 25.sp)
    }
}

@Composable
private fun RoundedPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()
        .background(Brush.verticalGradient(listOf(Color(0xFF342111), Surface)),
            RoundedCornerShape(19.dp))
        .border(1.dp, Border, RoundedCornerShape(19.dp))
        .padding(17.dp), content = content)
}

@Composable
private fun RecipeImage(path: String, modifier: Modifier) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) { RecipeCatalog.image(context, path) }
    }
    Box(modifier.background(
        Brush.linearGradient(listOf(Color(0xFF6A3717), Color(0xFF301B13))),
        RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        if (image == null) {
            Text("🍜", fontSize = 36.sp)
        } else {
            Image(bitmap = image!!, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

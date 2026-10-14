package ru.doshikologiya.nativepreview

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.time.LocalDate

internal enum class RecipeLayout { LARGE, LIST, GRID }

@Composable
internal fun HomePage(recipes: List<Recipe>, content: ExtraCatalog,
    open: (Recipe) -> Unit, all: () -> Unit, oracle: () -> Unit, wall: () -> Unit
) {
    val featured = remember(recipes) {
        if (recipes.isEmpty()) null else recipes[LocalDate.now().dayOfYear % recipes.size]
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (featured != null) HeroCard(featured) { open(featured) }
        else RoundedPanel { Text("Архив рецептов недоступен", color = Cream) }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF302113), Surface)))
                .border(1.dp, Hairline, RoundedCornerShape(18.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssetPicture("sticker-home-chef.webp", Modifier.size(63.dp),
                ContentScale.Fit, fallback = false)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(content.noteTitle, color = Cream, fontWeight = FontWeight.Black,
                    fontSize = 14.sp)
                Text(content.noteText, color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
                    maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Overline("Архив института")
                Text("Последние пять рецептов", fontWeight = FontWeight.Black,
                    color = Cream, fontSize = 21.sp)
            }
            Text("Все →", color = Amber, fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = all).padding(4.dp))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            recipes.takeLast(5).reversed().forEach { recipe ->
                Column(Modifier.width(175.dp).clip(RoundedCornerShape(18.dp))
                    .background(Surface).border(1.dp, Hairline, RoundedCornerShape(18.dp))
                    .clickable { open(recipe) }) {
                    AssetPicture(recipe.imagePath, Modifier.fillMaxWidth().height(125.dp))
                    Text(recipe.title, color = Cream, fontWeight = FontWeight.Bold,
                        fontSize = 14.sp, maxLines = 2, lineHeight = 17.sp,
                        modifier = Modifier.padding(12.dp))
                }
            }
        }
        RoundedPanel {
            Overline("Лента института")
            Text("Новости лаборатории", fontSize = 21.sp, fontWeight = FontWeight.Black,
                color = Cream)
            content.news.take(3).forEach {
                Spacer(Modifier.height(9.dp))
                HorizontalDivider(color = Hairline)
                Text(it.title, color = Cream, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp))
                Text(it.text, color = Muted, fontSize = 12.sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        RoundedPanel {
            Text("🔮", fontSize = 35.sp)
            Overline("Отдел вероятностей")
            Text("Оракул лапши", color = Cream, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text("Не можешь выбрать? Доверь решение Оракулу.", color = Muted)
            Spacer(Modifier.height(12.dp))
            GradientButton("🔮 Спросить Оракула", oracle, Modifier.fillMaxWidth())
        }
        RoundedPanel {
            Text("♥ Стена меценатов", color = Cream, fontWeight = FontWeight.Black,
                fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            OutlineButton("Перейти на Стену →", wall, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HeroCard(recipe: Recipe, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(482.dp)
        .clip(RoundedCornerShape(25.dp)).background(Surface)
        .border(1.dp, Hairline, RoundedCornerShape(25.dp))) {
        AssetPicture(recipe.imagePath, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color(0x20080604), Color(0x90080604), Color(0xF9080604)))))
        AssetPicture("sticker-wave.webp",
            Modifier.align(Alignment.BottomEnd).padding(end = 9.dp, bottom = 13.dp)
                .size(76.dp), ContentScale.Fit, fallback = false)
        Column(Modifier.align(Alignment.BottomStart).padding(19.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LabBadge("Эксперимент дня")
            Text(recipe.title, color = Cream, fontSize = 31.sp, lineHeight = 33.sp,
                fontWeight = FontWeight.Black, maxLines = 4)
            Text(recipe.description, color = Color(0xFFE9DCC9), fontSize = 14.sp,
                lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TinyTag("⏱ " + recipe.time)
                TinyTag("🔥 " + recipe.difficulty)
            }
            Spacer(Modifier.height(7.dp))
            GradientButton("📖 Открыть протокол", onClick, Modifier.fillMaxWidth(.88f))
        }
    }
}

@Composable
internal fun LibraryPage(
    recipes: List<Recipe>, query: String, onQuery: (String) -> Unit,
    onlyFavorites: Boolean, onFavorites: () -> Unit,
    category: String, onCategory: (String) -> Unit,
    layout: RecipeLayout, onLayout: (RecipeLayout) -> Unit,
    favorites: Set<String>, toggleFavorite: (String) -> Unit,
    open: (Recipe) -> Unit
) {
    val categories = remember(recipes) {
        listOf("Все") + recipes.map { it.category }.distinct().sorted()
    }
    val filtered = remember(recipes, query, category, onlyFavorites, favorites) {
        recipes.filter { recipe ->
            (category == "Все" || recipe.category == category) &&
            (!onlyFavorites || recipe.id in favorites) &&
            (query.isBlank() ||
                listOf(recipe.title,recipe.description,recipe.story,recipe.category)
                    .any { it.contains(query, ignoreCase = true) })
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PageTitle("Архив института", "Лабораторные протоколы",
                "Ищи по названию или выбирай кафедру.")
            OutlinedTextField(value = query, onValueChange = onQuery,
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                placeholder = { Text("🔎 Найти протокол…", color = Muted) })
            OutlineButton("♡ Только избранное", onFavorites,
                modifier = Modifier.fillMaxWidth(), selected = onlyFavorites)
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                categories.forEach { name ->
                    Box(Modifier.clip(CircleShape)
                        .background(if (category == name) Amber else Surface)
                        .border(1.dp, if (category == name) Amber else Hairline, CircleShape)
                        .clickable { onCategory(name) }
                        .padding(horizontal = 14.dp, vertical = 9.dp)) {
                        Text(name, color = if (category == name) Night else Muted,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Вид рецептов", color = Muted, fontSize = 11.sp,
                    modifier = Modifier.weight(1f))
                listOf(
                    RecipeLayout.LARGE to "▣ Крупно",
                    RecipeLayout.LIST to "☷ Список",
                    RecipeLayout.GRID to "▦ Сетка"
                ).forEach { (mode, text) ->
                    Box(Modifier.clip(RoundedCornerShape(9.dp))
                        .background(if (layout == mode) Amber.copy(alpha = .17f) else Surface2)
                        .border(1.dp, if (layout == mode) Amber else Hairline,
                            RoundedCornerShape(9.dp))
                        .clickable { onLayout(mode) }
                        .padding(horizontal = 7.dp, vertical = 8.dp)) {
                        Text(text, color = if (layout == mode) AmberLight else Muted,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (layout == RecipeLayout.GRID) {
            LazyVerticalGrid(columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)) {
                gridItems(filtered, key = { it.id }) { recipe ->
                    GridCard(recipe, recipe.id in favorites,
                        { toggleFavorite(recipe.id) }, { open(recipe) })
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 11.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)) {
                items(filtered, key = { it.id }) { recipe ->
                    if (layout == RecipeLayout.LARGE) LargeCard(recipe,
                        recipe.id in favorites, { toggleFavorite(recipe.id) }, { open(recipe) })
                    else ListCard(recipe, recipe.id in favorites,
                        { toggleFavorite(recipe.id) }, { open(recipe) })
                }
            }
        }
    }
}

@Composable
private fun Heart(selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(41.dp).clip(CircleShape).background(Color(0xDC140F0B))
        .border(1.dp, if (selected) Amber else Color(0x44FFFFFF), CircleShape)
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(if (selected) "♥" else "♡", color = if (selected) Amber else Cream,
            fontSize = 26.sp)
    }
}

@Composable
private fun LargeCard(r: Recipe, fav: Boolean, toggle: () -> Unit, open: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
        .background(Surface).border(1.dp, Hairline, RoundedCornerShape(22.dp))) {
        Box(Modifier.fillMaxWidth().height(216.dp)) {
            AssetPicture(r.imagePath, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color.Transparent, Color.Transparent, Color(0xD00B0805)))))
            Box(Modifier.align(Alignment.TopEnd).padding(9.dp)) { Heart(fav, toggle) }
            Box(Modifier.align(Alignment.BottomStart).padding(10.dp)) { TinyTag(r.category) }
        }
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(r.title, color = Cream, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(r.description, color = Muted, fontSize = 13.sp, maxLines = 3,
                lineHeight = 19.sp, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TinyTag("⏱ " + r.time); TinyTag("🔥 " + r.difficulty)
            }
            GradientButton("📖 Открыть протокол", open, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ListCard(r: Recipe, fav: Boolean, toggle: () -> Unit, open: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
        .background(Surface).border(1.dp, Hairline, RoundedCornerShape(17.dp))
        .clickable(onClick = open).padding(9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AssetPicture(r.imagePath, Modifier.size(98.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Overline(r.category)
            Text(r.title, color = Cream, fontWeight = FontWeight.Bold,
                fontSize = 15.sp, maxLines = 2)
            Text(r.time, color = Muted, fontSize = 11.sp)
        }
        Heart(fav, toggle)
    }
}

@Composable
private fun GridCard(r: Recipe, fav: Boolean, toggle: () -> Unit, open: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
        .background(Surface).border(1.dp, Hairline, RoundedCornerShape(17.dp))
        .clickable(onClick = open)) {
        Box(Modifier.fillMaxWidth().height(130.dp)) {
            AssetPicture(r.imagePath, Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.TopEnd).padding(5.dp)) { Heart(fav, toggle) }
        }
        Text(r.title, color = Cream, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            maxLines = 2, lineHeight = 17.sp, modifier = Modifier.padding(10.dp))
    }
}

@Composable
internal fun DetailPage(recipe: Recipe, selected: Boolean, onFavorite: () -> Unit,
    onCook: () -> Unit, onSupport: () -> Unit
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(310.dp)) {
            AssetPicture(recipe.imagePath, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color(0x10080604),Color(0x68080604), Color(0xF4080604)))))
            Column(Modifier.align(Alignment.BottomStart).padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                LabBadge("Лабораторный протокол")
                Text(recipe.title, color = Cream, fontSize = 30.sp, lineHeight = 32.sp,
                    fontWeight = FontWeight.Black)
                TinyTag("⏱ " + recipe.time + "   🔥 " + recipe.difficulty)
            }
        }
        Column(Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text(recipe.description, color = Muted, fontSize = 15.sp, lineHeight = 22.sp)
            GradientButton("▶ Режим готовки", onCook, Modifier.fillMaxWidth())
            GradientButton(if (selected) "♥ В избранном" else "♡ Добавить в избранное",
                onFavorite, Modifier.fillMaxWidth())
            OutlineButton("📨 Поделиться протоколом", {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT,
                        recipe.title + "\nhttps://crossdoomdev.github.io/doshikologiya/")
                }
                context.startActivity(Intent.createChooser(intent, "Поделиться рецептом"))
            }, Modifier.fillMaxWidth())
            RoundedPanel {
                Text("🧪 Материалы эксперимента", color = Cream, fontWeight = FontWeight.Black,
                    fontSize = 19.sp)
                Spacer(Modifier.height(12.dp))
                recipe.ingredients.forEach {
                    Text("•  " + it, color = Cream, fontSize = 14.sp, lineHeight = 21.sp,
                        modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            RoundedPanel {
                Overline("Архив института")
                Text(recipe.story, color = Cream, fontSize = 14.sp, lineHeight = 21.sp)
            }
            RoundedPanel {
                Text("♥ Поддержать Институт дошикологии",
                    color = AmberLight, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text("Помоги лаборатории выпускать новые протоколы.", color = Muted)
                Spacer(Modifier.height(10.dp))
                GradientButton("Поддержать проект", onSupport, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun CookingPage(recipe: Recipe, step: Int, adsReady: Boolean,
    previous: () -> Unit, next: () -> Unit, onExit: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Surface).border(1.dp, Amber.copy(alpha = .17f),
                RoundedCornerShape(24.dp)).padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape)
                .background(Color(0x30FFFFFF))) {
                Box(Modifier.fillMaxWidth((step + 1).toFloat() / recipe.steps.size)
                    .fillMaxHeight().clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Amber, Orange))))
            }
            Box(Modifier.fillMaxWidth().height(191.dp)
                .clip(RoundedCornerShape(20.dp))) {
                AssetPicture(recipe.imagePath, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xAA080604)))))
                Text(recipe.ingredients.size.toString() + " ингредиентов · " +
                    recipe.steps.size + " шагов", color = Cream, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(13.dp))
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Color(0x12FFFFFF))
                .border(1.dp, Hairline, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 23.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Overline("Шаг " + (step + 1) + " из " + recipe.steps.size)
                Spacer(Modifier.height(14.dp))
                Text(recipe.steps[step], color = Cream, fontWeight = FontWeight.Black,
                    fontSize = 24.sp, lineHeight = 30.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    recipe.steps.indices.forEach { idx ->
                        Box(Modifier.size(if (idx == step) 11.dp else 9.dp)
                            .clip(CircleShape)
                            .background(if (idx == step) Amber else Color(0x40FFFFFF)))
                    }
                }
            }
            if (BuildConfig.DEBUG) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Overline("Тестовый inline-баннер Яндекса")
                    if (adsReady) NarrowDemoBanner()
                    else Text("Инициализируем тестовую рекламу…", color = Muted, fontSize = 11.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineButton("← Назад", previous, Modifier.weight(1f),
                    enabled = step > 0)
                GradientButton(if (step + 1 == recipe.steps.size) "Готово ✓" else "Далее →",
                    next, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(25.dp))
    }
}

@Composable
private fun NarrowDemoBanner() {
    val context = LocalContext.current
    val widthDp = (LocalConfiguration.current.screenWidthDp - 76).coerceIn(180, 280)
    var error by remember { mutableStateOf("") }
    val view = remember(context, widthDp) {
        BannerAdView(context).apply {
            // Request a narrower inline creative, do not scale an existing ad.
            setAdSize(BannerAdSize.inline(context, widthDp, 115))
            setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdLoaded() { error = "" }
                override fun onAdFailedToLoad(e: AdRequestError) {
                    error = e.toString().take(120)
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
    AndroidView(factory = { view }, modifier = Modifier.width(widthDp.dp))
    if (error.isNotBlank()) {
        Text("Демобаннер временно недоступен: " + error,
            color = Muted, fontSize = 11.sp)
    }
}

@Composable
internal fun SuccessPage(recipe: Recipe, selected: Boolean,
    onFavorite: () -> Unit, onReturn: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        RoundedPanel(modifier = Modifier.fillMaxWidth()) {
            Text("🎉", fontSize = 70.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            Overline("Протокол выполнен")
            Text("Эксперимент удался!", color = Cream, fontSize = 28.sp,
                fontWeight = FontWeight.Black)
            Text("Ты приготовил: " + recipe.title, color = Muted)
            Spacer(Modifier.height(13.dp))
            GradientButton(if (selected) "♥ В избранном" else "♡ Добавить в избранное",
                onFavorite, Modifier.fillMaxWidth())
            Spacer(Modifier.height(9.dp))
            OutlineButton("Вернуться к рецепту", onReturn, Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun OraclePage(recipes: List<Recipe>, open: (Recipe) -> Unit) {
    var chosen by remember { mutableStateOf<Recipe?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)) {
        PageTitle("Отдел вероятностей", "Оракул лапши",
            "Не знаешь, что приготовить? Доверь свою кулинарную судьбу Оракулу.")
        RoundedPanel {
            LabBadge("Центральный оракул")
            AssetPicture("oracle/mage-idle.webp",
                Modifier.fillMaxWidth().height(229.dp), ContentScale.Fit)
            RoundedPanel(padding = 13) {
                Text(if (chosen == null)
                    "Если холодильник смотрит на тебя растерянно — нажимай кнопку. Судьба сама выберет протокол."
                    else "Судьба выбрала: «" + chosen!!.title + "».",
                    color = Cream, fontSize = 15.sp, lineHeight = 22.sp)
            }
            Spacer(Modifier.height(13.dp))
            GradientButton("🔮 Узнать, что мне приготовить",
                { chosen = recipes.randomOrNull() }, Modifier.fillMaxWidth())
        }
        chosen?.let { recipe ->
            RoundedPanel {
                Overline("Пророчество")
                Text(recipe.title, color = Cream, fontSize = 22.sp,
                    fontWeight = FontWeight.Black)
                Spacer(Modifier.height(9.dp))
                AssetPicture(recipe.imagePath,
                    Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(15.dp)))
                Spacer(Modifier.height(9.dp))
                GradientButton("📖 Открыть протокол", { open(recipe) },
                    Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun WallPage(content: ExtraCatalog, onSupport: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageTitle("Фонд исследований", "Стена меценатов",
            "Люди, благодаря которым в лаборатории не заканчиваются идеи и кипяток.")
        RoundedPanel {
            Text("♥", color = Amber, fontSize = 50.sp)
            Text("Поддержать НИИ лапши", color = Cream,
                fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Иногда достаточно помочь сделать следующий рецепт чуть лучше.",
                color = Muted, lineHeight = 20.sp)
            Spacer(Modifier.height(12.dp))
            GradientButton("Поддержать и войти в историю", onSupport, Modifier.fillMaxWidth())
        }
        content.patrons.forEach { patron ->
            RoundedPanel {
                Row(horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("🍜 " + patron.name, color = Cream, fontWeight = FontWeight.Black,
                        fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(patron.date, color = Muted, fontSize = 10.sp)
                }
                Spacer(Modifier.height(9.dp))
                Text(patron.text, color = Muted, lineHeight = 20.sp, fontSize = 14.sp)
            }
        }
    }
}

@Composable
internal fun MorePage(content: ExtraCatalog, onSupport: () -> Unit) {
    val context = LocalContext.current
    var faqExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        PageTitle("Служба института", "Ещё",
            "Обновления, поддержка и документы — всё важное в одном месте.")
        RoundedPanel {
            Overline("Архив института")
            Text("📚 Обновление рецептов", color = Cream, fontSize = 18.sp,
                fontWeight = FontWeight.Black)
            Text("Все 50 рецептов уже находятся внутри APK. Загрузка новых рецептов появится в следующих сборках.",
                color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(10.dp))
            OutlineButton("Версия Kotlin 0.2.0 · тестовая",
                {}, Modifier.fillMaxWidth(), enabled = false)
        }
        RoundedPanel {
            Overline("Связь с лабораторией")
            Text("✉ Помощь и поддержка", color = Cream, fontSize = 18.sp,
                fontWeight = FontWeight.Black)
            Spacer(Modifier.height(9.dp))
            if (content.supportEmail.isNotBlank()) {
                OutlineButton("Написать разработчику →", onSupport, Modifier.fillMaxWidth())
            } else {
                Text("Почта поддержки пока не настроена.", color = Muted)
            }
            Spacer(Modifier.height(9.dp))
            OutlineButton(if (faqExpanded) "Частые вопросы  ⌃" else "Частые вопросы  ⌄",
                { faqExpanded = !faqExpanded }, Modifier.fillMaxWidth())
            if (faqExpanded) {
                Spacer(Modifier.height(9.dp))
                Text("Рецепты работают без интернета. Избранное хранится на устройстве. Перед публикацией перенос функций будет завершён.",
                    color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
        RoundedPanel {
            Overline("Документы")
            Text("§ Информация", color = Cream, fontSize = 18.sp,
                fontWeight = FontWeight.Black)
            Spacer(Modifier.height(9.dp))
            OutlineButton("Политика конфиденциальности  ›",
                { openExternal(context, "https://crossdoomdev.github.io/doshikologiya/privacy.html") },
                Modifier.fillMaxWidth())
            Spacer(Modifier.height(9.dp))
            OutlineButton("Пользовательское соглашение  ›",
                { openExternal(context, "https://crossdoomdev.github.io/doshikologiya/terms.html") },
                Modifier.fillMaxWidth())
        }
    }
}

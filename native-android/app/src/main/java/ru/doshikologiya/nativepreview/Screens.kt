package ru.doshikologiya.nativepreview

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.platform.LocalInspectionMode

internal enum class RecipeLayout { LARGE, LIST, GRID }

@Composable
internal fun HomePage(recipes: List<Recipe>, content: ExtraCatalog,
    open: (Recipe) -> Unit, all: () -> Unit, oracle: () -> Unit, donate: () -> Unit
) {
    // Match js/features/home.js: favorite editorial recipe every third UTC
    // epoch day; otherwise rotate by epoch-day index.
    val featured = remember(recipes, content.featuredRecipeId) {
        if (recipes.isEmpty()) null else {
            val days = System.currentTimeMillis() / 86_400_000L
            val editorial = recipes.find { it.id == content.featuredRecipeId }
            if (days % 3L == 0L && editorial != null) editorial
            else recipes[(days % recipes.size).toInt()]
        }
    }
    val newest = remember(recipes, content.homeRecipeLimit) {
        NativeChronology.latestRecipes(recipes, content.homeRecipeLimit)
    }
    val recentNews = remember(content.news) {
        NativeChronology.latestNews(content.news, 3)
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (featured != null) HeroCard(featured) { open(featured) }
        else RoundedPanel {
            Text("Архив рецептов временно недоступен", color = Cream)
            Text("Лаборатория не смогла получить каталог. Попробуй обновить страницу чуть позже.", color = Muted)
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF302113), Surface)))
                .border(1.dp, Hairline, RoundedCornerShape(18.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssetPicture("sticker-home-chef.webp", Modifier.size(58.dp),
                ContentScale.Fit, fallback = false)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(content.noteTitle, color = Cream, fontWeight = FontWeight.Black,
                    fontSize = 16.sp)
                Text(content.noteText, color = Muted, fontSize = 13.sp, lineHeight = 19.24.sp)
            }
        }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageTitle("Архив института", "Последние пять рецептов",
                "Пять последних рецептов лаборатории. Листай ленту в сторону.", topSpace = false)
            OutlineButton("Все протоколы →", all)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            newest.forEach { recipe ->
                Column(Modifier.width((LocalConfiguration.current.screenWidthDp * .6f).coerceAtLeast(162f).dp).clip(RoundedCornerShape(18.dp))
                    .background(Surface).border(1.dp, Hairline, RoundedCornerShape(18.dp))
                    .clickable { open(recipe) }) {
                    AssetPicture(recipe.imagePath, Modifier.fillMaxWidth().height(112.dp))
                    Text(recipe.title, color = Cream, fontWeight = FontWeight.Bold,
                        fontSize = 13.sp, maxLines = 2, lineHeight = 16.25.sp,
                        modifier = Modifier.heightIn(min = 56.dp).padding(start = 11.dp, end = 11.dp, top = 10.dp, bottom = 12.dp))
                }
            }
        }
        RoundedPanel(padding = 15) {
            Overline("Лента института")
            Text("Новости лаборатории", fontSize = 21.sp, fontWeight = FontWeight.Black, color = Cream)
            Text("Объявления и события", color = Muted, fontSize = 10.sp)
            Spacer(Modifier.height(11.dp))
            if (recentNews.isEmpty()) Text("Здесь скоро появятся новости института. Пока мы готовим первую сводку. 🍜",
                color = Muted, fontSize = 12.sp, lineHeight = 18.6.sp)
            recentNews.forEach {
                Column(Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(13.dp))
                    .border(1.dp, Hairline, RoundedCornerShape(13.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(NativeChronology.formatDate(it.date), color = Amber, fontSize = 11.sp)
                    Text(it.title, color = Cream, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 19.5.sp, modifier = Modifier.padding(top = 4.dp, bottom = 5.dp))
                    Text(it.text, color = Muted, fontSize = 13.sp, lineHeight = 19.5.sp)
                }
                Spacer(Modifier.height(9.dp))
            }
        }
        RoundedPanel {
            Text("🔮", fontSize = 38.sp)
            Spacer(Modifier.height(8.dp))
            Overline("Отдел вероятностей")
            Text("Оракул лапши", color = Cream, fontWeight = FontWeight.Black,
                fontSize = 21.sp)
            Text("Не знаешь, что съесть? Передай решение в Отдел вероятностей — Оракул выберет один протокол из всего архива.",
                color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(17.dp))
            GradientButton("🔮 Довериться Оракулу", oracle, Modifier.fillMaxWidth())
        }
        RoundedPanel {
            // The web version has a second feature card with a direct donation
            // action, not a navigation shortcut to the patrons tab.
            Text("❤️", fontSize = 38.sp)
            Spacer(Modifier.height(8.dp))
            Overline("Фонд исследований")
            Text("Поддержать лабораторию", color = Cream, fontWeight = FontWeight.Black,
                fontSize = 21.sp)
            Text("Донаты идут на развитие приложения, новые рецепты и моральную компенсацию испытуемым. А удачное сообщение после модерации может поселиться на Стене меценатов.",
                color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(17.dp))
            GradientButton("♥ Поддержать проект", donate, Modifier.fillMaxWidth())
        }
        AppFooter()
    }
}

@Composable
private fun HeroCard(recipe: Recipe, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = 500.dp)
        .clip(RoundedCornerShape(24.dp)).background(Surface)
        .border(1.dp, Hairline, RoundedCornerShape(24.dp))) {
        AssetPicture(recipe.imagePath, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color(0x33080604), .46f to Color(0xB8080604), 1f to Color(0xF5080604))))
        Column(Modifier.fillMaxWidth().heightIn(min = 500.dp).padding(22.dp),
            verticalArrangement = Arrangement.Bottom) {
            LabBadge("Эксперимент дня")
            Spacer(Modifier.height(15.dp))
            Text(recipe.title, color = Cream, fontSize = 38.sp, lineHeight = 35.72.sp,
                letterSpacing = (-1.33).sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text(recipe.description, color = Color(0xFFE9DCC9), fontSize = 16.sp, lineHeight = 25.92.sp)
            Spacer(Modifier.height(18.dp))
            RecipeMeta(recipe, cost = true)
            Spacer(Modifier.height(20.dp))
            GradientButton("📖 Открыть протокол", onClick, Modifier.widthIn(max = 250.dp).fillMaxWidth())
        }
        AssetPicture("sticker-wave.webp", Modifier.align(Alignment.BottomEnd)
            .padding(end = 14.dp, bottom = 16.dp).size(72.dp), ContentScale.Fit, fallback = false)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeMeta(recipe: Recipe, cost: Boolean = false) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TinyTag("⏱ " + recipe.time)
        TinyTag("🔥 " + recipe.difficulty)
        if (cost) TinyTag("💸 " + recipe.cost)
    }
}

@Composable
internal fun LibraryPage(
    recipes: List<Recipe>, query: String, onQuery: (String) -> Unit,
    onlyFavorites: Boolean, onFavorites: () -> Unit,
    category: String, onCategory: (String) -> Unit,
    categoryOrder: List<String>,
    layout: RecipeLayout, onLayout: (RecipeLayout) -> Unit,
    favorites: Set<String>, toggleFavorite: (String) -> Unit,
    open: (Recipe) -> Unit
) {
    val categories = remember(recipes, categoryOrder) {
        (listOf("Все") + categoryOrder.filter { it != "Все" } +
            recipes.flatMap { it.categories }).distinct()
    }
    var searchQuery by remember { mutableStateOf(query.trim()) }
    LaunchedEffect(query) { delay(160); searchQuery = query.trim() }
    val filtered = remember(recipes, searchQuery, category, onlyFavorites, favorites) {
        recipes.filter { recipe ->
            (category == "Все" || category in recipe.categories) &&
            (!onlyFavorites || recipe.id in favorites) &&
            (searchQuery.isBlank() ||
                (listOf(recipe.title, recipe.description, recipe.story) +
                    recipe.categories + recipe.ingredients).joinToString(" ")
                    .contains(searchQuery, ignoreCase = true))
        }
    }
    val listState = rememberLazyGridState()
    var previousFilter by rememberSaveable { mutableStateOf(listOf(query, category, onlyFavorites.toString())) }
    LaunchedEffect(query, category, onlyFavorites) {
        val filter = listOf(query, category, onlyFavorites.toString())
        if (previousFilter != filter) listState.scrollToItem(0)
        previousFilter = filter
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (layout == RecipeLayout.GRID) 2 else 1),
        state = listState,
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 13.dp, bottom = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalArrangement = Arrangement.spacedBy(if (layout == RecipeLayout.LIST) 9.dp else 16.dp)
    ) {
        item(key = "tools", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(end = 86.dp)) {
                        PageTitle("Архив института", "Лабораторные протоколы",
                            "Ищи по названию или выбирай кафедру. Избранные рецепты сохраняются прямо на устройстве.")
                    }
                    AssetPicture("sticker-chopsticks.webp", Modifier.align(Alignment.TopEnd)
                        .size(82.dp), ContentScale.Fit, fallback = false)
                }
                Row(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(15.dp))
                    .border(1.dp, Hairline, RoundedCornerShape(15.dp)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Outlined.Search,
                        contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
                    BasicTextField(value = query, onValueChange = onQuery, singleLine = true,
                        textStyle = TextStyle(color = Cream, fontSize = 16.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Amber),
                        modifier = Modifier.weight(1f).padding(vertical = 13.dp)
                            .semantics { contentDescription = "Найти протокол" },
                        decorationBox = { input ->
                            Box {
                                if (query.isEmpty()) Text("Найти протокол…", color = Color(0xFF746858), fontSize = 16.sp)
                                input()
                            }
                        })
                }
                OutlineButton(if (onlyFavorites) "♥ Показываем избранное" else "♡ Только избранное",
                    onFavorites, modifier = Modifier.fillMaxWidth(), selected = onlyFavorites)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { name ->
                        Box(Modifier.clip(CircleShape)
                            .background(if (category == name) Amber else Surface)
                            .border(1.dp, Hairline, CircleShape)
                            .clickable { onCategory(name) }
                            .semantics { selected = category == name }
                            .padding(horizontal = 12.dp, vertical = 9.dp)) {
                            Text(name, color = if (category == name) Night else Muted,
                                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Вид рецептов", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Row(Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(15.dp))
                        .border(1.dp, Hairline, RoundedCornerShape(15.dp)).padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(RecipeLayout.LARGE to "▣ Крупно", RecipeLayout.LIST to "☷ Список",
                            RecipeLayout.GRID to "▦ Сетка").forEach { (mode, label) ->
                            Box(Modifier.weight(1f).heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (layout == mode) Amber else Color.Transparent)
                                .clickable { onLayout(mode) }.semantics { selected = layout == mode }
                                .padding(horizontal = 5.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center) {
                                Text(label, color = if (layout == mode) Color(0xFF241607) else Muted,
                                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
        if (filtered.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Лаборатория ничего не нашла. Возможно, рецепт засекречен.",
                color = Muted, fontSize = 16.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().border(1.dp, Hairline, RoundedCornerShape(18.dp))
                    .padding(36.dp))
        }
        gridItems(filtered, key = { it.id }) { recipe ->
            when (layout) {
                RecipeLayout.LARGE -> LargeCard(recipe, recipe.id in favorites,
                    { toggleFavorite(recipe.id) }, { open(recipe) })
                RecipeLayout.LIST -> ListCard(recipe, recipe.id in favorites,
                    { toggleFavorite(recipe.id) }, { open(recipe) })
                RecipeLayout.GRID -> GridCard(recipe, recipe.id in favorites,
                    { toggleFavorite(recipe.id) }, { open(recipe) })
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { AppFooter() }
    }
}

@Composable
private fun Heart(selected: Boolean, onClick: () -> Unit, size: Int = 40) {
    Box(Modifier.size(size.dp).clip(CircleShape)
        .background(if (selected) Color(0xD124180A) else Color(0xB30F0B08))
        .border(1.dp, if (selected) Amber.copy(alpha = .4f) else Color(0x29FFFFFF), CircleShape)
        .clickable(onClick = onClick).semantics {
            contentDescription = if (selected) "Убрать из избранного" else "Добавить в избранное"
            this.selected = selected
        }, contentAlignment = Alignment.Center) {
        HeartGlyph(selected, if (selected) Amber else Color.White,
            Modifier.size(if (size < 40) 20.dp else 23.dp))
    }
}

@Composable
private fun LargeCard(r: Recipe, fav: Boolean, toggle: () -> Unit, open: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
        .background(Surface).border(1.dp, Hairline, RoundedCornerShape(22.dp))) {
        Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
            AssetPicture(r.imagePath, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color.Transparent, Color.Transparent, Color(0xD00B0805)))))
            Box(Modifier.align(Alignment.TopEnd).padding(9.dp)) { Heart(fav, toggle) }
            Box(Modifier.align(Alignment.BottomStart).padding(10.dp)) { TinyTag(r.category) }
        }
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(r.title, color = Cream, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text(r.description, color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            RecipeMeta(r)
            GradientButton("📖 Открыть протокол", open, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ListCard(r: Recipe, fav: Boolean, toggle: () -> Unit, open: () -> Unit) {
    val narrow = LocalConfiguration.current.screenWidthDp <= 355
    Row(Modifier.fillMaxWidth().testTag("compact-card").height(IntrinsicSize.Min).clip(RoundedCornerShape(16.dp))
        .background(Surface).border(1.dp, Hairline, RoundedCornerShape(16.dp))) {
        RecipeMedia(r, fav, toggle, Modifier.width(if (narrow) 82.dp else 92.dp)
            .fillMaxHeight().heightIn(min = 108.dp), small = true)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(r.title, color = Cream, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                lineHeight = 17.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!narrow) Text(r.description, color = Muted, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            TinyTag("⏱ " + r.time, compact = true)
            GradientButton("Открыть протокол", open,
                Modifier.align(Alignment.CenterHorizontally).widthIn(max = 164.dp)
                    .fillMaxWidth().heightIn(min = 38.dp), compact = true, fontSize = 12)
        }
    }
}

@Composable
private fun RecipeMedia(r: Recipe, fav: Boolean, toggle: () -> Unit,
    modifier: Modifier, small: Boolean = false) {
    Box(modifier) {
        AssetPicture(r.imagePath, Modifier.matchParentSize())
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.46f)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xC70C0907)))))
        Box(Modifier.align(Alignment.TopEnd).padding(if (small) 6.dp else 10.dp)) {
            Heart(fav, toggle, if (small) 34 else 40)
        }
        Text(r.category, color = Color(0xFFF0DFC7), fontSize = if (small) 10.sp else 11.sp,
            fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                .clip(CircleShape).background(Color(0xB8120D09))
                .border(1.dp, Color(0x1FFFFFFF), CircleShape).padding(horizontal = 7.dp, vertical = 4.dp))
    }
}

@Composable
private fun GridCard(r: Recipe, fav: Boolean, toggle: () -> Unit, open: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
        .background(Surface).border(1.dp, Hairline, RoundedCornerShape(22.dp))) {
        RecipeMedia(r, fav, toggle, Modifier.fillMaxWidth().height(112.dp), small = true)
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
            Text(r.title, color = Cream, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                maxLines = 2, lineHeight = 16.25.sp, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 32.dp))
            Spacer(Modifier.height(6.dp))
            Text(r.description, color = Muted, fontSize = 11.sp, lineHeight = 14.85.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            TinyTag("⏱ " + r.time, compact = true)
            Spacer(Modifier.height(9.dp))
            GradientButton("Открыть протокол", open, Modifier.fillMaxWidth(),
                compact = true, fontSize = 10)
        }
    }
}

@Composable
internal fun DetailPage(recipe: Recipe, selected: Boolean, onFavorite: () -> Unit,
    onClose: () -> Unit, onCook: () -> Unit, onSupport: () -> Unit
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(330.dp)) {
            AssetPicture(recipe.imagePath, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color(0x10080604),Color(0x68080604), Color(0xF4080604)))))
            Column(Modifier.align(Alignment.BottomStart).padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                LabBadge("Лабораторный протокол")
                Text(recipe.title, color = Cream, fontSize = 30.sp, lineHeight = 32.sp,
                    fontWeight = FontWeight.Black)
                RecipeMeta(recipe, cost = true)
            }
            Box(Modifier.align(Alignment.TopEnd).padding(14.dp).size(44.dp)
                .clip(CircleShape).background(Color(0xD0160C08))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Text("×", color = Color.White, fontSize = 26.sp)
            }
        }
        Column(Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text(recipe.description, color = Muted, fontSize = 15.sp, lineHeight = 22.sp)
            PrimaryButton("▶ Режим готовки", onCook, Modifier.fillMaxWidth())
            GradientButton(if (selected) "♥ В избранном" else "♡ Добавить в избранное",
                onFavorite, Modifier.fillMaxWidth())
            OutlineButton("📨 Поделиться протоколом", {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Дошикология: " + recipe.title)
                    putExtra(Intent.EXTRA_TEXT,
                        "🍜 Рассекречен лабораторный протокол «" + recipe.title +
                        "»! Проверь, выдержит ли твоя кухня этот эксперимент.\n" +
                        "https://crossdoomdev.github.io/doshikologiya/?recipe=" +
                        recipe.id + "#recipes")
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
                Row(Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    AssetPicture("sticker-heart.webp", Modifier.size(64.dp),
                        ContentScale.Fit, fallback = false)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Поддержать Институт дошикологии",
                            color = AmberLight, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("Если эксперимент удался — можно помочь лаборатории выпускать новые протоколы.",
                            color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Поддержать проект", onSupport, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun CookingPage(recipe: Recipe, step: Int,
    previous: () -> Unit, next: () -> Unit, onExit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var stepLocked by remember { mutableStateOf(false) }
    fun moveStep(action: () -> Unit) {
        if (stepLocked) return
        stepLocked = true
        action()
        scope.launch {
            delay(420)
            stepLocked = false
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Surface).border(1.dp, Amber.copy(alpha = .17f),
                RoundedCornerShape(28.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                .background(Color(0x30FFFFFF))) {
                Box(Modifier.fillMaxWidth((step + 1).toFloat() / recipe.steps.size)
                    .fillMaxHeight().clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Amber, Orange))))
            }
            Box(Modifier.fillMaxWidth().height(190.dp)
                .clip(RoundedCornerShape(20.dp))) {
                AssetPicture(recipe.imagePath, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xAA080604)))))
                Text(recipe.ingredients.size.toString() + " ингредиентов · " +
                    recipe.steps.size + " шагов", color = Cream, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(13.dp))
            }
            // JS cook-main animates out 170ms and in 240ms. Animate only
            // this instruction card: the SDK's inline view stays in its
            // regular scroll position instead of being recreated/moved.
            AnimatedContent(
                targetState = step,
                label = "cookInstruction",
                transitionSpec = {
                    val forward = targetState > initialState
                    val exitOffset: (Int) -> Int = { width ->
                        ((if (forward) -0.18f else 0.18f) * width).roundToInt()
                    }
                    val enterOffset: (Int) -> Int = { width ->
                        ((if (forward) 0.18f else -0.18f) * width).roundToInt()
                    }
                    val exit = slideOutHorizontally(tween(170), targetOffsetX = exitOffset) +
                        fadeOut(tween(170)) + scaleOut(tween(170), targetScale = .985f)
                    val enter = slideInHorizontally(
                        animationSpec = tween(240, delayMillis = 170),
                        initialOffsetX = enterOffset
                    ) + fadeIn(tween(240, delayMillis = 170)) +
                        scaleIn(tween(240, delayMillis = 170), initialScale = .985f)
                    enter togetherWith exit
                }
            ) { shownStep ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Color(0x08FFFFFF))
                    .border(1.dp, Hairline, RoundedCornerShape(20.dp))
                    .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Overline("Шаг " + (shownStep + 1) + " из " + recipe.steps.size)
                    Spacer(Modifier.height(14.dp))
                    Text(recipe.steps[shownStep], color = Cream,
                        fontWeight = FontWeight.Normal, letterSpacing = (-.786).sp,
                        fontSize = (LocalConfiguration.current.screenWidthDp * .08f).coerceIn(24f, 36f).sp,
                        lineHeight = (LocalConfiguration.current.screenWidthDp * .0896f).coerceIn(26.88f, 40.32f).sp,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        recipe.steps.indices.forEach { idx ->
                            Box(Modifier.size(if (idx == shownStep) 11.dp else 9.dp)
                                .clip(CircleShape)
                                .background(if (idx == shownStep) Amber else Color(0x40FFFFFF)))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineButton("← Назад", { moveStep(previous) }, Modifier.weight(1f),
                    enabled = step > 0 && !stepLocked)
                PrimaryButton(if (step + 1 == recipe.steps.size) "Готово ✓" else "Дальше →",
                    { moveStep(next) }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(25.dp))
    }
}

@Composable
internal fun SuccessPage(
    recipe: Recipe, selected: Boolean,
    onFavorite: () -> Unit, onReturn: () -> Unit, onDone: () -> Unit
) {
    val falling = rememberInfiniteTransition(label = "confetti")
    val progress by falling.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fallingConfetti"
    )
    Box(
        Modifier.fillMaxSize()
            .background(Color(0xFF0F0B08)).verticalScroll(rememberScrollState()).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(
                    listOf(Color(0xFF281C12), Color(0xFF140E0A))))
                .border(1.dp, Amber.copy(alpha = .28f), RoundedCornerShape(28.dp))
        ) {
            // The original splash has 28 deterministic falling particles.
            // Keep the confetti behind the buttons so touch targets stay clear.
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                val colors = listOf(Amber, Orange, AmberLight, Color.White, Color(0xFFD85B10))
                repeat(28) { index ->
                    val x = ((3 + (index * 37) % 94) / 100f) * size.width
                    val t = (progress + (index % 9) * .035f) % 1f
                    val y = (t * 1.3f - .12f) * size.height
                    val drift = ((index * 53) % 140 - 70).toFloat() * t
                    drawRoundRect(
                        colors[index % colors.size].copy(alpha = (1f - t).coerceIn(0f,1f)),
                        topLeft = androidx.compose.ui.geometry.Offset(x + drift, y),
                        size = androidx.compose.ui.geometry.Size(7.dp.toPx(), 15.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(88.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(
                            AmberLight, Amber, Orange, Color(0xFF9B3E08))))
                        .border(5.dp, Color(0xC8FFE8B5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AssetPicture("sticker-party.webp", Modifier.size(76.dp),
                        ContentScale.Fit, fallback = false)
                }
                LabBadge("Протокол выполнен")
                Text("Эксперимент удался!", color = Cream, fontSize = 31.sp,
                    fontWeight = FontWeight.Black, lineHeight = 33.sp,
                    textAlign = TextAlign.Center)
                Text(recipe.title, color = AmberLight, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(
                    "Лаборатория подтверждает: ты прошёл весь протокол до конца. " +
                        "Можно сервировать результат и сделать вид, что всё именно так и планировалось.",
                    color = Muted, fontSize = 14.sp, lineHeight = 21.sp,
                    textAlign = TextAlign.Center
                )
                RoundedPanel(modifier = Modifier.fillMaxWidth(), padding = 13) {
                    Text("✓ Статус: съедобно и официально завершено",
                        color = Cream, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(recipe.steps.size.toString() + " из " + recipe.steps.size +
                        " этапов завершены. Протокол закрыт.",
                        color = Muted, fontSize = 12.sp)
                }
                GradientButton(
                    if (selected) "♥ В избранном" else "♡ Добавить в избранное",
                    onFavorite, Modifier.fillMaxWidth()
                )
                OutlineButton("Вернуться к рецепту", onReturn, Modifier.fillMaxWidth())
                PrimaryButton("Отлично! ✨", onDone, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun OraclePage(recipes: List<Recipe>, session: AppSession, open: (Recipe) -> Unit) {
    val chosen = session.oracleRecipe
    val stage = session.oracleStage
    val message = session.oracleMessage
    val reason = session.oracleReason
    val reasons = remember {
        listOf(
            "Звёзды, кипяток и остатки здравого смысла сошлись именно на этом варианте.",
            "Оракул проверил все ветви будущего и решил, что эта хотя бы вкусная.",
            "Вселенная намекает: сейчас лучше довериться лапше, чем продолжать переговоры с холодильником.",
            "По расчётам Отдела вероятностей, этот выбор снижает риск ещё двадцать минут решать, что приготовить.",
            "Кармический баланс требует именно этого протокола. Мы тоже не поняли почему, но печать уже поставлена.",
            "Сегодня твоя внутренняя энергия подозрительно хорошо резонирует с этим блюдом. Звучит научно — значит, работает.",
            "Судьба выбрала это первой. Оракул категорически отрицает использование виртуального кубика.",
            "Вероятность того, что ты будешь доволен этим решением, значительно выше вероятности придумать что-то лучше самостоятельно.",
            "Твой кулинарный маршрут на сегодня рассчитан. Спорить можно, но Оракул уже ушёл на обед.",
            "Именно этот протокол сейчас находится в идеальной точке между «хочу поесть» и «не хочу больше думать»."
        )
    }
    val transition = rememberInfiniteTransition(label = "oracleFloat")
    val floatY by transition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "oracleBob"
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)) {
        PageTitle("Отдел вероятностей", "Оракул лапши",
            "Не знаешь, что покушать? Доверься Оракулу. Он заглянет в твою кулинарную судьбу и выберет один рецепт из всего архива института.")
        RoundedPanel(padding = 22, surface = Color(0xFF19120C)) {
            LabBadge("Центральный оракул")
            val art = when (stage) {
                "thinking" -> "oracle/mage-thinking.webp"
                "success" -> "oracle/mage-success.webp"
                else -> "oracle/mage-idle.webp"
            }
            AssetPicture(
                art, Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp, bottom = 24.dp).widthIn(max = 280.dp).fillMaxWidth().height(252.dp)
                    .graphicsLayer { translationY = if (stage == "thinking") floatY else 0f },
                ContentScale.Fit, fallback = false
            )
            RoundedPanel(padding = 17, surface = Color(0xFF120D09)) {
                if (chosen == null) {
                    Text(message, color = Color(0xFFF1E0C8), fontSize = 17.sp, lineHeight = 26.35.sp)
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssetPicture(chosen!!.imagePath,
                            Modifier.size(92.dp).clip(RoundedCornerShape(15.dp)))
                        Column(Modifier.weight(1f)) {
                            Overline("Оракул постановил")
                            Text(chosen!!.title, color = Cream, fontSize = 21.sp,
                                fontWeight = FontWeight.Black)
                            Text(reason, color = Color(0xFFD8C6AF), fontSize = 14.sp, lineHeight = 21.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    GradientButton("📖 Открыть протокол →",
                        { chosen?.let(open) }, Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                if (stage == "thinking") "📜 Оракул изучает хроники…"
                else if (stage == "success") "🔮 Спросить Оракула ещё раз"
                else "🔮 Узнать, что мне есть",
                { session.consult(recipes, reasons) },
                Modifier.fillMaxWidth(), enabled = stage != "thinking" && recipes.isNotEmpty()
            )
        }
        NativeDailyIndex()
        AppFooter()
    }
}

@Composable
private fun NativeDailyIndex() {
    val today = remember { java.time.LocalDate.now() }
    val forecasts = listOf(
        "Сегодня допустимы импровизации, но третий пакетик приправы лаборатория официально не одобряет.",
        "Вероятность удачного эксперимента повышается после добавления яйца.",
        "День благоприятен для дешёвых решений, которые выглядят неожиданно дорого.",
        "Лаборатория рекомендует не спорить с человеком, у которого в руках кипяток."
    )
    val index = DailyOracle.index(today)
    RoundedPanel(modifier = Modifier.fillMaxWidth()) {
        Overline("Показатель дня")
        Text("Индекс дошикологии", color = Cream, fontWeight = FontWeight.Black,
            fontSize = 19.sp)
        Text(index.toString(), color = Amber, fontWeight = FontWeight.Black,
            fontSize = 74.sp, lineHeight = 67.sp, modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))
        Text(forecasts[DailyOracle.forecast(today)],
            color = Cream, fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(8.dp))
        Text("Примечание: индекс не является медицинским, финансовым или вообще каким-либо советом. Это число, которое красиво смотрится крупным шрифтом.",
            color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
    }
}

@Composable
internal fun WallPage(content: ExtraCatalog, onSupport: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageTitle("Фонд исследований", "Стена меценатов",
            "Люди, благодаря которым в лаборатории не заканчиваются идеи, кипяток и домен.")
        RoundedPanel(padding = 26, radius = 30, borderColor = Amber.copy(alpha = .34f)) {
            AssetPicture("sticker-heart.webp", Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp).size(96.dp),
                ContentScale.Fit, fallback = false)
            Text("Поддержать НИИ лапши", color = Cream,
                fontSize = 26.sp, lineHeight = 26.52.sp, letterSpacing = (-.91).sp, fontWeight = FontWeight.Bold)
            Text("Не обязательно спасать мир. Иногда достаточно помочь сделать следующий рецепт, экран или эксперимент чуть лучше.",
                color = Muted, lineHeight = 25.92.sp, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            RoundedPanel(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), padding = 16, radius = 17,
                surface = Color(0xFF372A17), borderColor = Amber.copy(alpha = .28f)) {
                Text("🏛️ Оставь след в истории Дошикологии",
                    color = AmberLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Text("Поддержи институт — и твоё сообщение после модерации может попасть на Стену меценатов. Через годы потомки спросят: «Кто этот человек?» — и мы с гордостью ответим: «Он верил в лапшу раньше всех».",
                    color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                Text("Публикация не гарантируется: сообщения проходят модерацию. Но шанс на цифровое бессмертие официально существует.",
                    color = Muted, fontSize = 11.sp, lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 7.dp))
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Поддержать и войти в историю", onSupport, Modifier.fillMaxWidth())
        }
        if (content.patrons.isEmpty()) {
            RoundedPanel {
                Text("Стена пока пуста. Первый меценат получает почётное право сказать: «Я был здесь до хайпа».",
                    color = Muted, lineHeight = 21.sp)
            }
        }
        NativeChronology.newestPatrons(content.patrons).forEachIndexed { index, patron ->
            RoundedPanel {
                Row(horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(listOf("🍜","🥚","🔥","🥢","🧄")[index % 5] +
                        " " + patron.name, color = Cream, fontWeight = FontWeight.Black,
                        fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(patron.amount, color = AmberLight, fontSize = 12.sp,
                        fontWeight = FontWeight.Black)
                }
                Text(if (patron.date.isBlank()) "" else NativeChronology.formatDate(patron.date),
                    color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(9.dp))
                Text(patron.text, color = Muted, lineHeight = 20.sp, fontSize = 14.sp)
            }
        }
        AppFooter()
    }
}

@Composable
internal fun MorePage(content: ExtraCatalog, recipes: List<Recipe>,
    session: AppSession,
    onPrivacy: () -> Unit, onTerms: () -> Unit, onSupport: () -> Unit
) {
    val context = LocalContext.current
    val pending = session.pending
    val updateBusy = session.updateAction.isNotEmpty()
    var faqExpanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageTitle("Служба института", "Ещё",
            "Обновления, поддержка и документы — всё важное в одном месте.")
        RoundedPanel(padding = 20, surface = Surface) {
          RoundedPanel(padding = 18, surface = Surface2, radius = 16) {
            Text("📚 Обновление рецептов", color = Cream, fontSize = 15.sp,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(session.updateStatus, color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(10.dp))
            OutlineButton(if (session.updateAction == "checking") "Проверяем обновления…" else "↻ Проверить обновления",
                { session.check(content, recipes) }, Modifier.fillMaxWidth(), enabled = !updateBusy)
            pending?.let { preview ->
                Spacer(Modifier.height(9.dp))
                PrimaryButton(if (session.updateAction == "downloading") "Получаем обновление…"
                    else if (session.retryDownload) "↻ Повторить загрузку"
                    else "↓ Получить обновление (${preview.added + preview.updated})",
                    { session.install(context, content, recipes) }, Modifier.fillMaxWidth(), enabled = !updateBusy)
            }
            session.updateProgress?.let { progress ->
                Spacer(Modifier.height(9.dp))
                Text(progress.label, color = Cream, fontSize = 12.sp,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (progress.total > 0) {
                    val fraction = progress.done.toFloat() / progress.total
                    Text(if (!updateBusy) "Готово" else "${progress.done} из ${progress.total} · ${(fraction * 100).roundToInt()}%",
                        color = Muted, fontSize = 12.sp)
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth(), color = Amber)
                } else {
                    Text("Подождите…", color = Muted, fontSize = 12.sp)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Amber)
                }
            }
          }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().background(Color(0x06FFFFFF), RoundedCornerShape(14.dp))
                .border(1.dp, Hairline, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Версия приложения", color = Cream,
                        fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text("Сейчас: " + BuildConfig.VERSION_NAME, color = Muted,
                        fontSize = 12.sp)
                }
                Text("📱", fontSize = 29.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Новые рецепты загружаются здесь. После выпуска Android-версии обновления самого приложения будут доступны через RuStore.",
                color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
        }
        RoundedPanel(padding = 20, surface = Surface) {
            MoreHeading("✉", "Связь с лабораторией", "Помощь и поддержка")
            Spacer(Modifier.height(16.dp))
            if (content.supportEmail.isNotBlank()) {
                MoreLink("✉", "Написать разработчику", "Личное обращение по электронной почте", onSupport)
            } else {
                Text("Электронная почта поддержки временно недоступна.", color = Muted)
            }
            Spacer(Modifier.height(9.dp))
            OutlineButton(if (faqExpanded) "Частые вопросы  ⌃" else "Частые вопросы  ⌄",
                { faqExpanded = !faqExpanded }, Modifier.fillMaxWidth())
            if (faqExpanded) {
                Spacer(Modifier.height(9.dp))
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Работают ли рецепты без интернета?",
                        color = Cream, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("В Android — да, встроенный и сохранённый каталог. Для первой загрузки веб-сайта интернет нужен.",
                        color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                    Text("Как получить новые рецепты?",
                        color = Cream, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Нажми «Проверить обновления» выше, затем подтверди загрузку.",
                        color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                    Text("Где хранится избранное?", color = Cream,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("На устройстве; удаление данных приложения может очистить избранное.",
                        color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }
        RoundedPanel(padding = 20, surface = Surface) {
            MoreHeading("§", "Документы", "Информация")
            Spacer(Modifier.height(16.dp))
            MoreLink("♢", "Политика конфиденциальности", "Локальные данные, CloudTips и обращения", onPrivacy)
            Spacer(Modifier.height(9.dp))
            MoreLink("▤", "Пользовательское соглашение", "Правила использования Дошикологии", onTerms)
            Spacer(Modifier.height(16.dp))
            Text("О Дошикологии", color = Cream, fontWeight = FontWeight.Black,
                fontSize = 15.sp)
            Text("Институт лапши быстрого приготовления: рецепты, немного магии и кухонные эксперименты. Приложение бесплатно и не требует регистрации.",
                color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
        }
        AppFooter()
    }
}

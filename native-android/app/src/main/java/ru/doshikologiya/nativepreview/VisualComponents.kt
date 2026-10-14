package ru.doshikologiya.nativepreview

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val Night = Color(0xFF100C09)
internal val Surface = Color(0xFF1C1510)
internal val Surface2 = Color(0xFF241B14)
internal val Cream = Color(0xFFF7EAD7)
internal val Muted = Color(0xFFAB9B86)
internal val Amber = Color(0xFFFFB229)
internal val AmberLight = Color(0xFFFFD46E)
internal val Orange = Color(0xFFFF6A2A)
internal val Hairline = Color(0x1AFFF4DF)
private val GoldGradient = Brush.horizontalGradient(listOf(Amber, Color(0xFFFF8A1F), Orange))
private val PanelGradient = Brush.verticalGradient(listOf(Surface2, Surface))

internal fun openExternal(context: Context, link: String) {
    if (link.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) { }
}

@Composable
internal fun AssetPicture(
    path: String, modifier: Modifier = Modifier,
    scale: ContentScale = ContentScale.Crop,
    fallback: Boolean = true
) {
    val context = LocalContext.current
    val revision = RecipeCatalog.revision
    val image by produceState<ImageBitmap?>(initialValue = null,
        key1 = path, key2 = revision) {
        value = withContext(Dispatchers.IO) {
            RecipeCatalog.image(context, path) ?: if (fallback) RecipeCatalog.image(context, "brand-logo.webp") else null
        }
    }
    Box(modifier.testTag(if (image == null) "asset-loading" else "asset-ready"), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(bitmap = image!!, contentDescription = null,
                contentScale = scale, filterQuality = androidx.compose.ui.graphics.FilterQuality.High, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
internal fun RoundedPanel(
    modifier: Modifier = Modifier, padding: Int = 18, surface: Color? = null,
    radius: Int = 22, borderColor: Color = Hairline,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(radius.dp))
            .background(if (surface == null) PanelGradient else Brush.verticalGradient(listOf(surface, surface)))
            .border(1.dp, borderColor, RoundedCornerShape(radius.dp))
            .padding(padding.dp),
        content = content
    )
}

@Composable
internal fun LabBadge(label: String) {
    Row(
        Modifier.clip(CircleShape)
            .background(Amber.copy(alpha = 0.14f))
            .border(1.dp, Amber.copy(alpha = 0.24f), CircleShape)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(Modifier.size(6.dp).background(Amber, CircleShape))
        Text(label.uppercase(), fontSize = 10.sp, color = AmberLight,
            letterSpacing = 1.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun Overline(text: String) {
    Text(text.uppercase(), color = Amber, fontSize = 10.sp, letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Black)
}

@Composable
internal fun PageTitle(overline: String, title: String, description: String = "", topSpace: Boolean = true) {
    Column(Modifier.padding(top = if (topSpace) 28.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Overline(overline)
        Text(title, color = Cream, fontSize = 26.sp, lineHeight = 26.5.sp, letterSpacing = (-.91).sp,
            fontWeight = FontWeight.Bold)
        if (description.isNotEmpty()) {
            Text(description, color = Muted, fontSize = 16.sp, lineHeight = 25.92.sp)
        }
    }
}

@Composable
internal fun GradientButton(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, compact: Boolean = false, fontSize: Int = 16
) {
    Box(
        modifier
            .shadow(11.dp, RoundedCornerShape(50), ambientColor = Orange.copy(alpha = .30f))
            .clip(if (compact) RoundedCornerShape(12.dp) else CircleShape)
            .background(if (enabled) GoldGradient else Brush.horizontalGradient(listOf(Surface2,Surface2)))
            .clickable(enabled = enabled, onClick = onClick)
            .border(1.dp, AmberLight.copy(alpha = if (enabled) .55f else .14f), if (compact) RoundedCornerShape(12.dp) else CircleShape)
            .padding(horizontal = if (compact) 5.dp else 22.dp, vertical = if (compact) 8.dp else 13.dp),
        contentAlignment = Alignment.Center
    ) {
        ActionLabel(label, color = if (enabled) Color(0xFF231407) else Muted,
            fontSize = fontSize, weight = FontWeight.Black)
    }
}

@Composable
internal fun PrimaryButton(label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true
) {
    Box(modifier.clip(RoundedCornerShape(14.dp))
        .background(if (enabled) Amber else Surface2)
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center) {
        Text(label, color = if (enabled) Color(0xFF241607) else Muted,
            fontWeight = FontWeight.Black, fontSize = 16.sp,
            textAlign = TextAlign.Center)
    }
}

@Composable
internal fun OutlineButton(
    label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, selected: Boolean = false, enabled: Boolean = true, fontSize: Int = 16
) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(if (selected) Amber.copy(alpha = .19f) else Color(0x16FFFFFF))
            .border(1.dp, if (selected) Amber.copy(alpha = .5f) else Hairline,
                RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        ActionLabel(label, color = if (enabled) (if (selected) AmberLight else Cream) else Muted,
            fontSize = fontSize, weight = FontWeight.Bold)
    }
}

@Composable
internal fun TinyTag(text: String, compact: Boolean = false) {
    Text(text, color = Color(0xFFE6D7C2), fontSize = if (compact) 10.sp else 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(CircleShape)
            .background(Color(0xB819120D))
            .border(1.dp, Color(0x1FFFFFFF), CircleShape)
            .padding(horizontal = if (compact) 6.dp else 9.dp, vertical = if (compact) 4.dp else 6.dp))
}

@Composable
internal fun BrandSplash() {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF33200F), Night), radius = 820f)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AssetPicture("brand-logo.webp",
            Modifier.fillMaxWidth(.93f).height(320.dp), ContentScale.Fit, fallback = false)
        Spacer(Modifier.height(10.dp))
        Text("ЛАБОРАТОРИЯ ЗАПУСКАЕТСЯ  • • •", color = Muted,
            fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
    }
}

@Composable
internal fun NativeTabs(selected: Screen, onSelect: (Screen) -> Unit) {
    val tabs = listOf(
        Screen.Home to "Главная",
        Screen.Library to "Рецепты",
        Screen.Oracle to "Оракул",
        Screen.Wall to "Стена",
        Screen.More to "Ещё"
    )
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color(0xF7140F0B))
            .border(1.dp, Hairline, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .padding(start = 7.dp, end = 7.dp, top = 7.dp, bottom = 14.dp)
    ) {
        tabs.forEach { (target, label) ->
            val active = selected == target
            Column(
                Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(target) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Trace the original HTML SVG paths instead of substituting
                // similar-but-different Material icon silhouettes.
                NativeTabGlyph(target, if (active) Amber else Muted)
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (active) Amber else Muted)
                Box(Modifier.width(23.dp).height(3.dp).clip(CircleShape)
                    .background(if (active) Amber else Color.Transparent))
            }
        }
    }
}

@Composable
internal fun NativeHeader(label: String, back: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Surface)
            .border(1.dp, Hairline).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlineButton("←", back)
        Column(Modifier.weight(1f)) {
            Overline("Институт дошикологии")
            Text(label, color = Cream, fontSize = 18.sp, fontWeight = FontWeight.Black,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun CookingHeader(title: String, category: String,
    steps: Int, back: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF18110C))
            .border(1.dp, Hairline).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LabBadge("Режим готовки")
            Text(title, color = Cream, fontSize = 24.sp, fontWeight = FontWeight.Normal, letterSpacing = (-.72).sp, textAlign = TextAlign.Center,
                lineHeight = 25.92.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                CookChip("🍜 " + category)
                CookChip("Шагов: " + steps)
            }
        }
        OutlineButton("✕ Выйти", back, fontSize = 13)
    }
}

@Composable
internal fun AppFooter() {
    Column(Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("© Институт дошикологии · экспериментальная лаборатория лапши", color = Muted, fontSize = 12.sp)
        Text("Версия " + BuildConfig.VERSION_NAME, color = Muted, fontSize = 12.sp)
    }
}

@Composable
internal fun ExitDialog(onStay: () -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = onStay) {
        RoundedPanel(Modifier.fillMaxWidth(), padding = 24) {
            Text("🍜", fontSize = 34.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(14.dp))
            Text("Вы действительно хотите выйти?", color = Cream, fontSize = 26.sp,
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Лаборатория подождёт тебя. Продолжить изучать рецепты или закрыть Дошикологию?",
                color = Muted, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Остаться", onStay, Modifier.weight(1f))
                OutlineButton("Да, выйти", onExit, Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun MoreLink(symbol: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(35.dp).background(Amber.copy(alpha = .08f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) { Text(symbol, color = Amber, fontSize = 21.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Cream, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.2.sp)
            Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 16.8.sp)
        }
        Text("›", color = Muted, fontSize = 24.sp)
    }
}

@Composable
private fun CookChip(label: String) {
    Text(label, color = Muted, fontSize = 12.sp,
        modifier = Modifier.background(Color(0x0AFFFFFF), CircleShape)
            .border(1.dp, Color(0x14FFFFFF), CircleShape).padding(horizontal = 12.dp, vertical = 8.dp))
}

@Composable
internal fun MoreHeading(symbol: String, overline: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(46.dp).background(Amber.copy(alpha = .12f), RoundedCornerShape(15.dp))
            .border(1.dp, Amber.copy(alpha = .19f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
            Text(symbol, color = Amber, fontSize = 24.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Overline(overline)
            Text(title, color = Cream, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionLabel(label: String, color: Color, fontSize: Int, weight: FontWeight) {
    val heart = label.startsWith("♥ ") || label.startsWith("♡ ")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (heart) HeartGlyph(label.startsWith("♥"), color, Modifier.size(23.dp))
        Text(if (heart) label.drop(2) else label, color = color, fontSize = fontSize.sp,
            fontWeight = weight, textAlign = TextAlign.Center, lineHeight = (fontSize * 1.2f).sp)
    }
}

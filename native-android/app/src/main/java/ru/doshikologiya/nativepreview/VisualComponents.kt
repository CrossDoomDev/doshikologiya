package ru.doshikologiya.nativepreview

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
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
internal val Hairline = Color(0x2AFFF4DF)
private val GoldGradient = Brush.horizontalGradient(listOf(Amber, Color(0xFFFF8A1F), Orange))
private val PanelGradient = Brush.verticalGradient(listOf(Color(0xFF2B2018), Surface))

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
    val image by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) { RecipeCatalog.image(context, path) }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(bitmap = image!!, contentDescription = null,
                contentScale = scale, modifier = Modifier.fillMaxSize())
        } else if (fallback) {
            Text("🍜", fontSize = 42.sp)
        }
    }
}

@Composable
internal fun RoundedPanel(
    modifier: Modifier = Modifier, padding: Int = 18,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(PanelGradient)
            .border(1.dp, Hairline, RoundedCornerShape(22.dp))
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
internal fun PageTitle(overline: String, title: String, description: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Overline(overline)
        Text(title, color = Cream, fontSize = 28.sp, lineHeight = 31.sp,
            fontWeight = FontWeight.Black)
        if (description.isNotEmpty()) {
            Text(description, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
internal fun GradientButton(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier
            .shadow(11.dp, RoundedCornerShape(50), ambientColor = Orange.copy(alpha = .30f))
            .clip(CircleShape)
            .background(if (enabled) GoldGradient else Brush.horizontalGradient(listOf(Surface2,Surface2)))
            .clickable(enabled = enabled, onClick = onClick)
            .border(1.dp, AmberLight.copy(alpha = if (enabled) .42f else .14f), CircleShape)
            .padding(horizontal = 17.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (enabled) Color(0xFF231407) else Muted,
            fontSize = 14.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Black,
            maxLines = 2, lineHeight = 18.sp)
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
            fontWeight = FontWeight.Black, fontSize = 14.sp,
            textAlign = TextAlign.Center)
    }
}

@Composable
internal fun OutlineButton(
    label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, selected: Boolean = false, enabled: Boolean = true
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
        Text(label, color = if (enabled) (if (selected) AmberLight else Cream) else Muted,
            fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun TinyTag(text: String) {
    Text(text, color = Cream, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(CircleShape)
            .background(Color(0xB51A120C))
            .border(1.dp, Color(0x35FFFFFF), CircleShape)
            .padding(horizontal = 10.dp, vertical = 7.dp))
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
        Modifier.fillMaxWidth()
            .background(Color(0xFF160F0B))
            .border(1.dp, Color(0x1BFFFFFF))
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 5.dp)
    ) {
        tabs.forEach { (target, label) ->
            val active = selected == target
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(target) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Trace the original HTML SVG paths instead of substituting
                // similar-but-different Material icon silhouettes.
                NativeTabGlyph(target, if (active) Amber else Muted)
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
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
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LabBadge("Режим готовки")
            Text(title, color = Cream, fontSize = 19.sp, fontWeight = FontWeight.Black,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 21.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                TinyTag("🍜 " + category)
                TinyTag("Шагов: " + steps)
            }
        }
        OutlineButton("✕ Выйти", back)
    }
}

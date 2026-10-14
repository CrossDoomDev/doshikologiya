package ru.doshikologiya.nativepreview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withLink

internal data class NativeLegalSection(
    val title: String,
    val paragraphs: List<String>
)
internal data class NativeLegalDocument(
    val title: String,
    val edition: String,
    val sections: List<NativeLegalSection>
)

@Composable
internal fun LegalPage(kind: String, onOther: () -> Unit, onReturn: () -> Unit) {
    val context = LocalContext.current
    val document = remember(kind) {
        runCatching {
            val data = context.assets.open("legal-" + kind + ".json")
                .bufferedReader().use { it.readText() }
            val root = JSONObject(data)
            val sections = root.getJSONArray("sections")
            NativeLegalDocument(root.getString("title"), root.getString("edition"),
                (0 until sections.length()).map { i ->
                    val section = sections.getJSONObject(i)
                    val paragraphs = section.getJSONArray("paragraphs")
                    NativeLegalSection(section.getString("heading"),
                        (0 until paragraphs.length()).map { p -> paragraphs.getString(p) })
                })
        }.getOrNull()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 17.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)) {
        PageTitle("Институт дошикологии · Документы",
            document?.title ?: "Документ недоступен",
            if (kind == "privacy") "Приложение «Дошикология» и его веб-версия"
            else "Правила использования приложения «Дошикология»")
        Text(document?.edition ?: "", color = Muted, fontSize = 13.sp)
        document?.sections?.forEach { section ->
            RoundedPanel(modifier = Modifier.fillMaxWidth()) {
                Text(section.title, color = Cream, fontWeight = FontWeight.Black,
                    fontSize = 18.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(10.dp))
                section.paragraphs.forEach { paragraph ->
                    Text(buildAnnotatedString {
                        var position = 0
                        val links = Regex("romantatabaev@gmail\\.com|crossdoomdev\\.github\\.io/doshikologiya|Политике конфиденциальности")
                        links.findAll(paragraph).forEach { match ->
                            append(paragraph.substring(position, match.range.first))
                            val annotation = if (match.value == "Политике конфиденциальности")
                                LinkAnnotation.Clickable("privacy", TextLinkStyles(SpanStyle(color = Amber))) { onOther() }
                            else LinkAnnotation.Url(if ('@' in match.value) "mailto:" + match.value else "https://" + match.value + "/",
                                TextLinkStyles(SpanStyle(color = Amber)))
                            withLink(annotation) { append(match.value) }
                            position = match.range.last + 1
                        }
                        append(paragraph.substring(position))
                    }, color = Muted, fontSize = 14.sp, lineHeight = 22.sp,
                        modifier = Modifier.padding(bottom = 11.dp))
                }
            }
        }
        OutlineButton(if (kind == "privacy") "Пользовательское соглашение" else "Политика конфиденциальности", onOther)
        OutlineButton("Вернуться в приложение", onReturn)
        Spacer(Modifier.height(14.dp))
    }
}

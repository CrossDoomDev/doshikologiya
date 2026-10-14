package ru.doshikologiya.nativepreview

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class NewsItem(val title: String, val text: String, val date: String)
internal data class PatronItem(val name: String, val text: String, val date: String,
    val amount: String)
internal data class ExtraCatalog(
    val noteTitle: String = "Институт дошикологии докладывает",
    val noteText: String = "Архив лаборатории открыт для исследований.",
    val donateUrl: String = "",
    val supportEmail: String = "",
    val featuredRecipeId: String = "",
    val categoryOrder: List<String> = listOf("Все"),
    val homeRecipeLimit: Int = 5,
    val news: List<NewsItem> = emptyList(),
    val patrons: List<PatronItem> = emptyList()
) {
    companion object {
        fun load(context: Context): ExtraCatalog {
            fun json(name: String) = runCatching {
                context.assets.open(name).bufferedReader().use { it.readText() }
            }.getOrDefault("")
            val config = runCatching { JSONObject(json("config.json")) }.getOrDefault(JSONObject())
            val banner = config.optJSONObject("banner") ?: JSONObject()
            val news = runCatching {
                val a = JSONArray(json("news.json"))
                (0 until a.length()).map { i -> a.getJSONObject(i) }.map {
                    NewsItem(it.optString("title"), it.optString("text"),
                        it.optString("publishedAt"))
                }
            }.getOrDefault(emptyList())
            val patrons = runCatching {
                val a = JSONArray(json("patrons.json"))
                (0 until a.length()).map { i -> a.getJSONObject(i) }.map {
                    PatronItem(it.optString("name"), it.optString("text"),
                        it.optString("date"), it.optString("amount"))
                }
            }.getOrDefault(emptyList())
            return ExtraCatalog(
                noteTitle = banner.optString("title", "Институт дошикологии докладывает"),
                noteText = banner.optString("text", "Научные новости лаборатории."),
                donateUrl = config.optString("donateUrl"),
                supportEmail = config.optString("supportEmail"),
                featuredRecipeId = config.optString("featuredRecipeId"),
                homeRecipeLimit = config.optInt("homeRecipeLimit", 5).coerceIn(1,5),
                categoryOrder = config.optJSONArray("categoryOrder")?.let { a ->
                    (0 until a.length()).map { a.optString(it) }
                } ?: listOf("Все"),
                news = news, patrons = patrons
            )
        }
    }
}

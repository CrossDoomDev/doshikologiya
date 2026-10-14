package ru.doshikologiya.nativepreview

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONArray
import android.util.AtomicFile
import java.io.File

data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val imagePath: String,
    val category: String,
    val time: String,
    val difficulty: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val story: String,
    val cost: String = "",
    val categories: List<String> = emptyList(),
    val publishedAt: String = "",
    val imageSource: String = ""
)

object RecipeCatalog {
    fun load(context: Context): List<Recipe> {
        val bundled = parse(context.assets.open("recipes.json")
            .bufferedReader().use { it.readText() })
        val cached = runCatching {
            val file = File(context.filesDir, "recipes-cache.json")
            if (file.exists()) parse(file.readText()) else emptyList()
        }.getOrDefault(emptyList())
        // Same merge semantics as js/core/offline-catalog.js: new recipes
        // override bundled records, never make bundled content unavailable.
        val merged = LinkedHashMap(bundled.associateBy { it.id })
        cached.forEach { merged[it.id] = it }
        return merged.values.toList()
    }

    fun parse(json: String): List<Recipe> {
        val input = JSONArray(json)
        return (0 until input.length()).mapNotNull { index ->
            val item = input.getJSONObject(index)
            val steps = item.optJSONArray("steps").asStrings()
            val id = item.optString("id")
            if (!id.matches(Regex("^[a-z0-9-]{1,100}$")) || steps.isEmpty()) return@mapNotNull null
            val originalImage = item.optString("image")
            Recipe(
                id = id,
                title = item.optString("title"),
                description = item.optString("description"),
                imagePath = originalImage.substringBefore("?").removePrefix("images/"),
                category = item.optJSONArray("categories").asStrings().firstOrNull() ?: "Все",
                time = item.optString("time"),
                difficulty = item.optString("difficulty"),
                ingredients = item.optJSONArray("ingredients").asStrings(),
                steps = steps,
                story = item.optString("story"),
                cost = item.optString("cost"),
                categories = item.optJSONArray("categories").asStrings(),
                publishedAt = item.optString("publishedAt"),
                imageSource = originalImage
            )
        }
    }

    fun install(context: Context, json: String) {
        val parsed = parse(json)
        require(parsed.size >= 50 && parsed.size <= 500 && parsed.distinctBy { it.id }.size == parsed.size) {
            "Неверный формат нового архива"
        }
        val atomic = AtomicFile(File(context.filesDir, "recipes-cache.json"))
        val stream = atomic.startWrite()
        try {
            stream.write(json.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (e: Exception) {
            atomic.failWrite(stream)
            throw e
        }
    }

    private fun JSONArray?.asStrings(): List<String> =
        if (this == null) emptyList()
        else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }

    fun image(context: Context, path: String): ImageBitmap? {
        if (path.isBlank()) return null
        return try {
            val cached = File(context.filesDir, "recipe-images/" + path)
            val safe = path.matches(Regex("^recipes/[a-z0-9-]+\\.webp$"))
            fun openImage() = if (safe && cached.isFile) cached.inputStream()
                else context.assets.open(path)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openImage().use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1100 || bounds.outHeight / sample > 1100)
                sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            openImage().use { BitmapFactory.decodeStream(it, null, options) }
                ?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

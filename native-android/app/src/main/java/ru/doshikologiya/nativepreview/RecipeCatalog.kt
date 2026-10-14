package ru.doshikologiya.nativepreview

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONArray
import android.util.AtomicFile
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

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
    val imageSource: String = "",
    val sourceJson: String = ""
)

object RecipeCatalog {
    // Increment only after a complete catalog install. Compose uses this
    // revision as its image decoding key, so updated image bytes become visible
    // immediately even when the relative filename did not change.
    var revision by mutableIntStateOf(0)
        private set

    fun load(context: Context): List<Recipe> {
        val bundled = parse(context.assets.open("recipes.json")
            .bufferedReader().use { it.readText() })
        val cached = runCatching {
            val file = activeDirectory(context)?.let { File(it, "recipes.json") }
                ?: File(context.filesDir, "recipes-cache.json")
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
        val seen = mutableSetOf<String>()
        return (0 until input.length()).mapNotNull { index ->
            val item = input.optJSONObject(index) ?: return@mapNotNull null
            val steps = item.optJSONArray("steps").asStrings()
            val id = item.optString("id")
            if (!id.matches(Regex("^[a-z0-9-]+$")) || steps.isEmpty() ||
                item.optString("title").isBlank() || item.optJSONArray("ingredients").asStrings().isEmpty() ||
                !seen.add(id)) return@mapNotNull null
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
                imageSource = originalImage,
                sourceJson = RecipeUpdates.stringify(item)
            )
        }
    }

    internal fun activeDirectory(context: Context): File? = runCatching {
        val pointer = AtomicFile(File(context.filesDir, "recipes-active"))
        val name = pointer.openRead().bufferedReader().use { it.readText() }
        require(name.matches(Regex("catalog-[0-9]+")))
        File(context.filesDir, name).takeIf { File(it, "recipes.json").isFile }
    }.getOrNull()

    internal fun activate(context: Context, directory: File) {
        val pointer = AtomicFile(File(context.filesDir, "recipes-active"))
        val stream = pointer.startWrite()
        try {
            stream.write(directory.name.toByteArray(Charsets.UTF_8))
            pointer.finishWrite(stream)
        } catch (e: Exception) {
            pointer.failWrite(stream)
            throw e
        }
    }

    internal fun refreshImages() { revision++ }

    private fun JSONArray?.asStrings(): List<String> =
        if (this == null) emptyList()
        else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }

    fun image(context: Context, path: String): ImageBitmap? {
        if (path.isBlank()) return null
        return try {
            val cached = File(activeDirectory(context) ?: context.filesDir, "recipe-images/" + path)
            val safe = path.matches(Regex("^recipes/[a-z0-9-]+\\.webp$"))
            fun decode(openStream: () -> java.io.InputStream): ImageBitmap? {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                openStream().use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
                var sample = 1
                while (bounds.outWidth / sample > 1100 || bounds.outHeight / sample > 1100)
                    sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                return openStream().use { BitmapFactory.decodeStream(it, null, options) }
                    ?.asImageBitmap()
            }
            val bundled: () -> java.io.InputStream = { context.assets.open(path) }
            if (safe && cached.isFile) {
                runCatching { decode { cached.inputStream() } }.getOrNull()
                    ?: decode(bundled)
            } else decode(bundled)
        } catch (_: Exception) {
            null
        }
    }
}

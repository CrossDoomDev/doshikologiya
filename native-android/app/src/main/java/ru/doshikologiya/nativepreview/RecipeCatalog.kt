package ru.doshikologiya.nativepreview

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONArray

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
    val story: String
)

object RecipeCatalog {
    fun load(context: Context): List<Recipe> {
        val json = context.assets.open("recipes.json").bufferedReader().use { it.readText() }
        val input = JSONArray(json)
        return (0 until input.length()).mapNotNull { index ->
            val item = input.getJSONObject(index)
            val steps = item.optJSONArray("steps").asStrings()
            val id = item.optString("id")
            if (id.isBlank() || steps.isEmpty()) return@mapNotNull null
            Recipe(
                id = id,
                title = item.optString("title"),
                description = item.optString("description"),
                imagePath = item.optString("image").substringBefore("?").removePrefix("images/"),
                category = item.optJSONArray("categories").asStrings().firstOrNull() ?: "Все",
                time = item.optString("time"),
                difficulty = item.optString("difficulty"),
                ingredients = item.optJSONArray("ingredients").asStrings(),
                steps = steps,
                story = item.optString("story")
            )
        }
    }

    private fun JSONArray?.asStrings(): List<String> =
        if (this == null) emptyList()
        else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }

    fun image(context: Context, path: String): ImageBitmap? {
        if (path.isBlank()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1100 || bounds.outHeight / sample > 1100)
                sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
                ?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

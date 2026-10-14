package ru.doshikologiya.nativepreview

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

internal data class RemoteRecipePreview(
    val json: String,
    val items: List<Recipe>,
    val added: Int,
    val updated: Int,
    val changed: List<Recipe>
)

internal object RemoteRecipeUpdater {
    // A fixed trusted HTTPS source; never accept arbitrary URL values from
    // recipes or user input. This is the same repository as GitHub Pages.
    private const val ORIGIN = "https://crossdoomdev.github.io/doshikologiya/"
    private const val CATALOG = ORIGIN + "data/recipes.json"
    private val validAssetPath = Regex("^recipes/[a-z0-9-]+\\.webp$")
    private const val MAX_CATALOG_BYTES = 2_000_000
    private const val MAX_IMAGE_BYTES = 5_000_000

    suspend fun check(current: List<Recipe>): RemoteRecipePreview =
        withContext(Dispatchers.IO) {
            val json = fetchBytes(CATALOG, MAX_CATALOG_BYTES).toString(Charsets.UTF_8)
            val items = RecipeCatalog.parse(json)
            require(items.size >= 50 && items.size <= 500)
            require(items.distinctBy { it.id }.size == items.size)
            val before = current.associateBy { it.id }
            val changed = items.filter { before[it.id] != it }
            RemoteRecipePreview(
                json = json,
                items = items,
                added = changed.count { it.id !in before },
                updated = changed.count { it.id in before },
                changed = changed
            )
        }

    suspend fun install(
        context: Context, preview: RemoteRecipePreview,
        onProgress: (Int, Int) -> Unit
    ): List<Recipe> = withContext(Dispatchers.IO) {
        // Never replace the cached catalog before downloaded photographs finish.
        // If connection fails, the previous 50+ recipes remain fully usable.
        val total = preview.changed.size.coerceAtLeast(1)
        preview.changed.forEachIndexed { i, recipe ->
            val path = recipe.imagePath
            if (validAssetPath.matches(path)) {
                val src = ORIGIN + "images/" + path
                val bytes = fetchBytes(src, MAX_IMAGE_BYTES)
                val target = File(context.filesDir, "recipe-images/" + path)
                target.parentFile?.mkdirs()
                val temp = File(target.parentFile, target.name + ".part")
                temp.writeBytes(bytes)
                if (!temp.renameTo(target)) {
                    temp.delete()
                    error("Не удалось сохранить изображение")
                }
            }
            // The callback is invoked on the main thread to update Compose.
            withContext(Dispatchers.Main) { onProgress(i + 1, total) }
        }
        RecipeCatalog.install(context, preview.json)
        RecipeCatalog.load(context)
    }

    private fun fetchBytes(url: String, maxBytes: Int): ByteArray {
        require(url.startsWith(ORIGIN))
        val request = URL(url).openConnection() as HttpURLConnection
        request.connectTimeout = 8_000
        request.readTimeout = 15_000
        request.instanceFollowRedirects = false
        request.setRequestProperty("Accept", "application/json,image/webp")
        try {
            require(request.responseCode == 200) { "HTTP " + request.responseCode }
            require(request.contentLengthLong < 0 || request.contentLengthLong <= maxBytes)
            val buffer = ByteArray(8192)
            val out = ByteArrayOutputStream()
            request.inputStream.use { input ->
                while (true) {
                    val amount = input.read(buffer)
                    if (amount < 0) break
                    require(out.size() + amount <= maxBytes) { "Превышен размер файла" }
                    out.write(buffer, 0, amount)
                }
            }
            return out.toByteArray()
        } finally {
            request.disconnect()
        }
    }
}

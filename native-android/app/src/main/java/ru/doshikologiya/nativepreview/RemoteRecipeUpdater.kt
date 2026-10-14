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
        // Stage all images first, just like the browser's cacheRemoteImages
        // does before it installs the catalog. A failed download never
        // replaces the live photos or changes the installed JSON.
        val staging = File(context.filesDir, "recipe-images-staging-" + System.nanoTime())
        val pendingFiles = mutableListOf<Pair<File, File>>()
        try {
            preview.changed.forEachIndexed { i, recipe ->
                val path = recipe.imagePath
                if (validAssetPath.matches(path)) {
                    val src = ORIGIN + "images/" + path
                    val bytes = fetchBytes(src, MAX_IMAGE_BYTES)
                    val candidate = File(staging, path)
                    candidate.parentFile?.mkdirs()
                    candidate.writeBytes(bytes)
                    val bounds = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    android.graphics.BitmapFactory.decodeFile(candidate.absolutePath, bounds)
                    require(bounds.outWidth > 0 && bounds.outHeight > 0) {
                        "Неверное изображение рецепта"
                    }
                    val target = File(context.filesDir, "recipe-images/" + path)
                    pendingFiles += candidate to target
                }
                withContext(Dispatchers.Main) { onProgress(i + 1, total) }
            }
            pendingFiles.forEach { (candidate, target) ->
                target.parentFile?.mkdirs()
                if (!candidate.renameTo(target)) {
                    // Atomic replacement is supported on Android when both
                    // files are on the same app-data volume; do not silently
                    // declare an update successful otherwise.
                    error("Не удалось переместить скачанное изображение")
                }
            }
        } finally {
            staging.deleteRecursively()
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

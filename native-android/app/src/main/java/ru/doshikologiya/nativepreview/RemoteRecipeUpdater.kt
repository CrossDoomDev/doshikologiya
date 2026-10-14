package ru.doshikologiya.nativepreview

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.net.URI
import kotlin.coroutines.coroutineContext

internal data class UpdateProgress(val label: String, val done: Int = 0, val total: Int = 0)

internal object RemoteRecipeUpdater {
    private val validAssetPath = Regex("^recipes/[a-z0-9-]+\\.webp$")

    suspend fun check(content: ExtraCatalog, current: List<Recipe>,
        fetch: (String, Int) -> ByteArray = { url, limit -> PublicHttp.bytes(url, limit) }
    ): RemoteRecipePreview =
        withContext(Dispatchers.IO) {
            if (content.remoteRecipeIndexUrl.isNotBlank()) {
                RecipeUpdates.diff(current, fetch(content.remoteRecipeIndexUrl, 2_000_000)
                    .toString(Charsets.UTF_8))
            } else {
                val items = RecipeCatalog.parse(fetch(content.remoteRecipesUrl, 2_000_000)
                    .toString(Charsets.UTF_8))
                require(items.isNotEmpty())
                val before = current.associateBy { it.id }
                val changes = items.filter { before[it.id]?.sourceJson != it.sourceJson }
                RemoteRecipePreview(changes.count { it.id !in before },
                    changes.count { it.id in before }, changed = changes)
            }
        }

    suspend fun install(context: Context, content: ExtraCatalog, current: List<Recipe>,
        preview: RemoteRecipePreview,
        fetch: (String, Int) -> ByteArray = { url, limit -> PublicHttp.bytes(url, limit) },
        onProgress: (UpdateProgress) -> Unit
    ): List<Recipe> = withContext(Dispatchers.IO) {
        suspend fun progress(label: String, done: Int = 0, total: Int = 0) =
            withContext(Dispatchers.Main) { onProgress(UpdateProgress(label, done, total)) }
        progress("Загружаем рецепты", 0, preview.entries.size)
        val changed = if (preview.entries.isEmpty()) preview.changed else
            preview.entries.mapIndexed { i, entry ->
                coroutineContext.ensureActive()
                val url = URI(content.remoteRecipeIndexUrl).resolve(entry.path).toString()
                val recipe = RecipeUpdates.verify(entry,
                    fetch(url, 200_000).toString(Charsets.UTF_8))
                progress("Загружаем рецепты", i + 1, preview.entries.size)
                recipe
            }
        require(changed.isNotEmpty())
        val merged = RecipeUpdates.merge(current, changed)
        // JSON and photographs belong to a single generation. Only the atomic
        // pointer is published, so download, disk or process failure cannot leave
        // the old catalog paired with partially replaced photographs.
        val staging = File(context.filesDir, "recipe-images-staging-" + System.nanoTime())
        val installed = File(context.filesDir, "catalog-" + System.nanoTime())
        var activated = false
        try {
            check(staging.mkdirs())
            val oldImages = File(RecipeCatalog.activeDirectory(context) ?: context.filesDir, "recipe-images")
            if (oldImages.exists()) check(oldImages.copyRecursively(File(staging, "recipe-images")))
            progress("Загружаем фотографии", 0, changed.size)
            changed.forEachIndexed { i, recipe ->
                coroutineContext.ensureActive()
                val path = recipe.imagePath
                require(validAssetPath.matches(path)) { "Неверный адрес фотографии" }
                val bytes = fetch(PublicHttp.ORIGIN + "images/" + path +
                    recipe.imageSource.substringAfter('?', "").let { if (it.isEmpty()) "" else "?$it" }, 5_000_000)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Неверное изображение рецепта" }
                val target = File(staging, "recipe-images/$path")
                check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
                target.writeBytes(bytes)
                progress("Загружаем фотографии", i + 1, changed.size)
            }
            progress("Сохраняем рецепты на устройстве")
            File(staging, "recipes.json").writeText(merged.joinToString(",", "[", "]") { it.sourceJson })
            check(staging.renameTo(installed))
            coroutineContext.ensureActive()
            val previous = RecipeCatalog.activeDirectory(context)
            RecipeCatalog.activate(context, installed)
            activated = true
            // Keep the preceding generation for image readers already in flight;
            // bound storage usage across repeated successful updates.
            context.filesDir.listFiles().orEmpty().filter {
                it.isDirectory && it.name.matches(Regex("catalog-[0-9]+")) &&
                    it != installed && it != previous
            }.forEach { it.deleteRecursively() }
            withContext(Dispatchers.Main) { RecipeCatalog.refreshImages() }
            merged
        } finally {
            staging.deleteRecursively()
            if (!activated) installed.deleteRecursively()
        }
    }
}

package ru.doshikologiya.nativepreview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CatalogParityTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun asset(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }
    private fun recipes() = RecipeCatalog.parse(asset("recipes.json"))
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun everyBundledFingerprintMatchesThePublishedJavascriptIndex() {
        val recipes = recipes()
        val entries = JSONObject(asset("recipes-index.json")).getJSONArray("recipes")
        assertEquals(50, recipes.size)
        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            assertEquals(entry.getString("id"), entry.getString("hash"),
                RecipeUpdates.fingerprint(recipes.single { it.id == entry.getString("id") }.sourceJson))
        }
        val diff = RecipeUpdates.diff(recipes, asset("recipes-index.json"))
        assertEquals(0, diff.added + diff.updated)
    }

    @Test fun checksFetchOnlyTheIndexAndPreserveExistingCatalog() = runTest {
        val config = ExtraCatalog.load(context)
        val requested = mutableListOf<String>()
        val result = RemoteRecipeUpdater.check(config, recipes()) { url, _ ->
            requested += url
            asset("recipes-index.json").toByteArray()
        }
        assertEquals(listOf(config.remoteRecipeIndexUrl), requested)
        assertEquals(0, result.added + result.updated)
        assertNull(RecipeCatalog.activeDirectory(context))
    }

    @Test fun deltaDownloadVerifiesIntegrityAndCommitsJsonWithImages() = runTest {
        val original = recipes()
        val edited = JSONObject(original.first().sourceJson).put("title", "Обновлённый протокол 🍜")
        val json = RecipeUpdates.stringify(edited)
        val entry = RecipeIndexEntry(original.first().id, "recipe-entries/${original.first().id}.json",
            RecipeUpdates.fingerprint(json))
        val preview = RemoteRecipePreview(0, 1, entries = listOf(entry))
        val photo = context.assets.open(original.first().imagePath).use { it.readBytes() }
        val requested = mutableListOf<String>()
        val result = RemoteRecipeUpdater.install(context, ExtraCatalog.load(context), original, preview,
            fetch = { url, _ ->
                requested += url
                if (url.contains("recipe-entries")) json.toByteArray() else photo
            }, onProgress = {})
        assertEquals(2, requested.size)
        assertEquals("Обновлённый протокол 🍜", result.first().title)
        assertEquals(50, RecipeCatalog.load(context).size)
        assertEquals(result.first().title, RecipeCatalog.load(context).first().title)
        assertNotNull(RecipeCatalog.image(context, result.first().imagePath))
    }

    @Test fun interruptedDownloadLeavesOldCatalogAndPhotosIntact() = runTest {
        val original = recipes()
        val changes = original.take(2).map { it.copy(title = "Не установлен") }
        val preview = RemoteRecipePreview(0, 2, changed = changes)
        var count = 0
        var failed = false
        try {
            RemoteRecipeUpdater.install(context, ExtraCatalog.load(context), original, preview,
                fetch = { _, _ ->
                    if (++count == 2) error("Simulated disconnected network")
                    context.assets.open(original.first().imagePath).use { it.readBytes() }
                }, onProgress = {})
        } catch (_: IllegalStateException) { failed = true }
        assertTrue(failed)
        assertNull(RecipeCatalog.activeDirectory(context))
        assertEquals(original, RecipeCatalog.load(context))
        assertFalse(context.filesDir.listFiles().orEmpty().any { it.name.startsWith("recipe-images-staging-") })
    }

    @Test fun corruptedRecipeAndDuplicateIndexAreRejected() {
        val r = recipes().first()
        val entry = RecipeIndexEntry(r.id, "recipe-entries/${r.id}.json", "00000000")
        assertThrows(IllegalArgumentException::class.java) { RecipeUpdates.verify(entry, r.sourceJson) }
        val index = JSONObject(asset("recipes-index.json"))
        val records = index.getJSONArray("recipes")
        records.put(records.getJSONObject(0))
        assertThrows(IllegalArgumentException::class.java) { RecipeUpdates.diff(recipes(), index.toString()) }
    }

    @Test fun missingRemoteRecordsDoNotDeletePreviouslyDownloadedRecipes() {
        val original = recipes()
        val added = original.first().copy(id = "new-recipe")
        val merged = RecipeUpdates.merge(original + added, listOf(original.first().copy(title = "Updated")))
        assertEquals(51, merged.size)
        assertEquals(added, merged.last())
        assertEquals("Updated", merged.first().title)
    }

    @Test fun contactsAllowClearingAndDoNotOverrideOtherConfiguration() {
        val config = ExtraCatalog.load(context)
        val next = ServiceContacts.parse("""{"supportEmail":" test@example.com ","donateUrl":"","featuredRecipeId":"evil"}""")
            .applyTo(config)
        assertEquals("test@example.com", next.supportEmail)
        assertEquals("", next.donateUrl)
        assertEquals(config.featuredRecipeId, next.featuredRecipeId)
        assertThrows(IllegalArgumentException::class.java) {
            ServiceContacts.parse("""{"donateUrl":"https://user:password@example.com/"}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ServiceContacts.parse("""{"supportEmail":"not-an-email"}""")
        }
    }

    @Test fun dailyIndexUsesUnpaddedJavascriptDate() {
        assertEquals(202611L, DailyOracle.seed(LocalDate.of(2026, 1, 1)))
        assertEquals(20261014L, DailyOracle.seed(LocalDate.of(2026, 10, 14)))
        assertEquals(55 + (202611L * 17 % 41).toInt(), DailyOracle.index(LocalDate.of(2026, 1, 1)))
    }

    @Test fun screenRestorationKeepsRecipeAndOrigin() {
        val r = recipes().first()
        for (screen in listOf(Screen.Detail(r, Screen.Oracle), Screen.Cooking(r, Screen.Library),
            Screen.Success(r, Screen.Home))) assertEquals(screen, ScreenCodec.decode(screen.encode(), recipes()))
        assertEquals(Screen.Home, ScreenCodec.decode("detail|missing|recipes", recipes()))
    }
}

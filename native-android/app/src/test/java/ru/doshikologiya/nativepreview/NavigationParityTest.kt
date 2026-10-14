package ru.doshikologiya.nativepreview

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h851dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NavigationParityTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun start() {
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Night, surface = Surface,
                onSurface = Cream, primary = Amber)) { DoshikNativeApp() }
        }
        compose.mainClock.advanceTimeBy(1200)
        compose.waitForIdle()
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag("asset-loading", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/parity-screenshots/$name.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun tabsLayoutsSearchAndBackFollowTheCapacitorFlow() {
        start()
        screenshot("home")
        compose.onNodeWithText("Рецепты", useUnmergedTree = true).performClick()
        screenshot("recipes-large")
        compose.onNodeWithText("☷ Список").performScrollTo().performClick()
        screenshot("recipes-list")
        val compactBounds = compose.onAllNodesWithTag("compact-card").onFirst().getUnclippedBoundsInRoot()
        assertTrue("Compact photos must not expand cards after decoding", compactBounds.bottom - compactBounds.top < 200.dp)
        compose.onNodeWithText("▦ Сетка").performScrollTo().performClick()
        screenshot("recipes-grid")
        compose.onNodeWithContentDescription("Найти протокол").performScrollTo()
            .performTextInput("  карбонара  ")
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithText("Открыть протокол").performScrollTo().performClick()
        compose.onNodeWithText("▶ Режим готовки").assertExists()
        screenshot("detail")
        compose.onNodeWithText("Добавить в избранное").performScrollTo().performClick()
        compose.onNodeWithText("В избранном").assertExists()
        assertTrue(compose.activity.getSharedPreferences("native_favorites", 0)
            .getStringSet("ids", emptySet())!!.contains("carbonara-2049"))
        compose.onNodeWithText("▶ Режим готовки").performScrollTo().performClick()
        screenshot("cooking")
        back()
        compose.onNodeWithText("▶ Режим готовки").assertExists()
        back()
        compose.onNodeWithContentDescription("Найти протокол").assertTextContains("  карбонара  ")
        back()
        back()
        compose.onNodeWithText("Вы действительно хотите выйти?").assertExists()
        screenshot("exit")
        compose.onNodeWithText("Остаться").performClick()
        compose.onNodeWithText("Вы действительно хотите выйти?").assertDoesNotExist()
    }

    @Test fun oracleResultSurvivesOpeningAndClosingARecipe() {
        start()
        compose.onNodeWithText("Оракул", useUnmergedTree = true).performClick()
        screenshot("oracle")
        compose.onNodeWithText("🔮 Узнать, что мне есть").performScrollTo().performClick()
        // ViewModel timers use the Android main dispatcher; let Robolectric advance it.
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
            .idleFor(java.time.Duration.ofSeconds(4))
        compose.waitForIdle()
        compose.onNodeWithText("📖 Открыть протокол →").performScrollTo().performClick()
        back()
        compose.onNodeWithText("🔮 Спросить Оракула ещё раз").assertExists()
        screenshot("oracle-result")
        compose.onNodeWithText("Стена", useUnmergedTree = true).performClick()
        screenshot("wall")
        compose.onNodeWithText("Ещё", useUnmergedTree = true).performClick()
        screenshot("more")
        compose.onNodeWithText("Частые вопросы  ⌄").performScrollTo().performClick()
        compose.onNodeWithText("Где хранится избранное?").assertExists()
        compose.onNodeWithText("Политика конфиденциальности").performScrollTo().performClick()
        screenshot("privacy")
        back()
        compose.onNodeWithText("Где хранится избранное?").assertExists()
    }
    @Test
    fun theSameIncomingRecipeLinkCanBeOpenedAgainAfterBack() {
        val request = androidx.compose.runtime.mutableLongStateOf(0)
        val link = android.net.Uri.parse("https://crossdoomdev.github.io/doshikologiya/?recipe=carbonara-2049#recipes")
        compose.setContent { MaterialTheme { DoshikNativeApp(link, request.longValue) } }
        compose.mainClock.advanceTimeBy(1200)
        compose.onNodeWithText("▶ Режим готовки").assertExists()
        back()
        compose.onNodeWithContentDescription("Найти протокол").assertExists()
        compose.runOnIdle { request.longValue++ }
        compose.onNodeWithText("▶ Режим готовки").assertExists()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-mdpi")
    fun narrowPhoneKeepsSearchAndAllLayoutsReachable() {
        start()
        screenshot("home-320")
        compose.onNodeWithText("Рецепты", useUnmergedTree = true).performClick()
        compose.onNodeWithText("☷ Список").performScrollTo().performClick()
        screenshot("list-320")
        compose.onNodeWithText("▦ Сетка").performScrollTo().performClick()
        screenshot("grid-320")
        compose.onNodeWithContentDescription("Найти протокол").performScrollTo()
            .performTextInput("Несуществующий рецепт")
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithText("Лаборатория ничего не нашла. Возможно, рецепт засекречен.")
            .performScrollTo().assertIsDisplayed()
    }

}

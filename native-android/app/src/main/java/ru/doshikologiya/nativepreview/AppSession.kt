package ru.doshikologiya.nativepreview

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Work belongs to the app session, not to a tab's composition. */
internal class AppSession : ViewModel() {
    var installedRecipes by mutableStateOf<List<Recipe>?>(null)
        private set
    var pending by mutableStateOf<RemoteRecipePreview?>(null)
        private set
    var updateAction by mutableStateOf("")
        private set
    var updateStatus by mutableStateOf("Рецепты доступны без интернета. Проверка новых протоколов — только по твоему запросу.")
        private set
    var updateProgress by mutableStateOf<UpdateProgress?>(null)
        private set
    var retryDownload by mutableStateOf(false)
        private set
    var oracleRecipe by mutableStateOf<Recipe?>(null)
        private set
    var oracleStage by mutableStateOf("idle")
        private set
    var oracleMessage by mutableStateOf("Если холодильник смотрит на тебя так же растерянно, как ты на него — нажимай кнопку. Судьба сама выберет протокол.")
        private set
    var oracleReason by mutableStateOf("")
        private set
    private var lastRecipeId: String? = null

    fun consult(recipes: List<Recipe>, reasons: List<String>) {
        if (oracleStage == "thinking" || recipes.isEmpty()) return
        oracleStage = "thinking"
        oracleRecipe = null
        oracleMessage = "📜 Раскрываем запечатанные хроники Института…"
        viewModelScope.launch {
            delay(1000)
            oracleMessage = "Сопоставляем древние пророчества с запасами лапши…"
            delay(1000)
            oracleMessage = "Последняя проверка знаков судьбы…"
            delay(1000)
            val recipe = recipes.filter { it.id != lastRecipeId }.ifEmpty { recipes }.random()
            oracleRecipe = recipe
            lastRecipeId = recipe.id
            oracleReason = reasons.random()
            oracleStage = "success"
            MagicChime.play()
        }
    }

    fun check(content: ExtraCatalog, recipes: List<Recipe>) {
        if (updateAction.isNotEmpty()) return
        pending = null
        retryDownload = false
        updateAction = "checking"
        updateStatus = "🔎 Проверяем, появились ли новые или изменённые рецепты…"
        updateProgress = UpdateProgress("Связываемся с архивом рецептов")
        viewModelScope.launch {
            try {
                val result = RemoteRecipeUpdater.check(content, recipes)
                pending = result.takeIf { it.added + it.updated > 0 }
                updateStatus = if (pending == null)
                    "✓ Новых и изменённых рецептов нет. Всё уже на устройстве."
                else "Найдены изменения (" + listOfNotNull(
                    result.added.takeIf { it > 0 }?.let { "новых: $it" },
                    result.updated.takeIf { it > 0 }?.let { "обновлённых: $it" }
                ).joinToString(", ") + "). Получить обновление?"
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                updateStatus = "Не удалось проверить архив. Проверь интернет и попробуй снова."
            } finally { updateAction = ""; updateProgress = null }
        }
    }

    fun install(context: Context, content: ExtraCatalog, recipes: List<Recipe>) {
        val preview = pending ?: return
        if (updateAction.isNotEmpty()) return
        updateAction = "downloading"
        updateStatus = "📥 Получаем обновления. Не закрывай приложение до завершения."
        updateProgress = UpdateProgress("Подготовка загрузки")
        viewModelScope.launch {
            try {
                installedRecipes = RemoteRecipeUpdater.install(context.applicationContext,
                    content, recipes, preview) { updateProgress = it }
                pending = null
                retryDownload = false
                updateProgress = UpdateProgress("Обновление установлено", 1, 1)
                updateStatus = "✓ Архив обновлён. Загруженные рецепты доступны без интернета."
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                retryDownload = true
                updateProgress = null
                updateStatus = "Загрузка не завершена. Проверь интернет и свободное место, затем попробуй снова."
            } finally { updateAction = "" }
        }
    }
}

import { checkOnlineRecipes, installOnlineRecipes } from "../core/api.js?v=20261011-progress1";
import { applyCatalog, state } from "../core/state.js";
import { renderFeatured } from "./home.js";
import { renderChips, renderRecipes } from "./recipes.js?v=20261012-hidden-recipe-dates1";

export function initRecipeUpdates(catalog) {
  const section = document.querySelector(".catalog-update");
  const checkButton = document.getElementById("recipeUpdateCheckBtn");
  const downloadButton = document.getElementById("recipeUpdateDownloadBtn");
  const status = document.getElementById("recipeUpdateStatus");
  const progressBox = document.getElementById("recipeUpdateProgress");
  const progressBar = document.getElementById("recipeUpdateProgressBar");
  const progressFill = document.getElementById("recipeUpdateProgressFill");
  const progressStage = document.getElementById("recipeUpdateProgressStage");
  const progressCount = document.getElementById("recipeUpdateProgressCount");
  if (!checkButton || !downloadButton || !status || !progressBox
    || !progressBar || !progressFill || !progressStage || !progressCount) return;

  let knownRecipes = catalog.knownRecipes;
  let pending = null;
  let busy = false;

  const setStatus = message => { status.textContent = message; };
  const setBusy = (value, action = "") => {
    busy = value;
    checkButton.disabled = value;
    downloadButton.disabled = value;
    checkButton.dataset.busy = String(value && action === "checking");
    downloadButton.dataset.busy = String(value && action === "downloading");
    section?.classList.toggle("is-busy", value);
  };
  const hideProgress = () => {
    progressBox.classList.add("hidden");
    progressBox.classList.remove("is-indeterminate");
    progressBar.removeAttribute("aria-valuenow");
  };
  const showProgress = (label, completed, total) => {
    progressBox.classList.remove("hidden");
    progressStage.textContent = label;
    const measurable = Number.isFinite(completed) && Number.isFinite(total) && total > 0;
    progressBox.classList.toggle("is-indeterminate", !measurable);
    if (measurable) {
      const done = Math.max(0, Math.min(total, completed));
      const percent = Math.round(done * 100 / total);
      progressFill.style.width = percent + "%";
      progressCount.textContent = done + " из " + total + " · " + percent + "%";
      progressBar.setAttribute("aria-valuenow", String(percent));
      progressBar.setAttribute("aria-valuetext", label + ", " + done + " из " + total);
    } else {
      progressFill.style.width = "";
      progressCount.textContent = "Подождите…";
      progressBar.removeAttribute("aria-valuenow");
      progressBar.setAttribute("aria-valuetext", label);
    }
  };
  const resetDownload = () => {
    pending = null;
    downloadButton.classList.add("hidden");
    downloadButton.textContent = "↓ Получить обновление";
  };

  hideProgress();
  checkButton.disabled = false;
  checkButton.addEventListener("click", async () => {
    if (busy) return;
    resetDownload();
    setBusy(true, "checking");
    checkButton.textContent = "Проверяем обновления…";
    showProgress("Связываемся с архивом рецептов", null, null);
    setStatus("🔎 Проверяем, появились ли новые или изменённые рецепты…");
    try {
      const result = await checkOnlineRecipes(state.config, knownRecipes);
      if (!result.hasUpdates) {
        hideProgress();
        setStatus("✓ Новых и изменённых рецептов нет. Всё уже на устройстве.");
      } else {
        pending = result;
        const changes = [
          result.added ? "новых: " + result.added : "",
          result.updated ? "обновлённых: " + result.updated : ""
        ].filter(Boolean).join(", ");
        hideProgress();
        setStatus("Найдены изменения (" + changes + "). Получить обновление?");
        downloadButton.textContent = "↓ Получить обновление (" + (result.added + result.updated) + ")";
        downloadButton.classList.remove("hidden");
      }
    } catch (error) {
      console.warn("Ручная проверка архива не удалась:", error);
      hideProgress();
      setStatus("Не удалось проверить архив. Проверь интернет и попробуй снова.");
    } finally {
      checkButton.textContent = "↻ Проверить обновления";
      setBusy(false);
    }
  });

  downloadButton.addEventListener("click", async () => {
    if (busy || !pending) return;
    setBusy(true, "downloading");
    downloadButton.textContent = "Получаем обновление…";
    showProgress("Подготовка загрузки", null, null);
    setStatus("📥 Получаем обновления. Не закрывай приложение до завершения.");
    try {
      const result = await installOnlineRecipes(
        state.config, catalog.bundledRecipes, knownRecipes, pending,
        { onProgress: ({ phase, completed, total }) => {
          if (phase === "recipes") {
            showProgress("Загружаем рецепты", completed, total);
          } else if (phase === "images") {
            showProgress("Загружаем фотографии", completed, total);
          } else if (phase === "saving") {
            showProgress("Сохраняем рецепты на устройстве", null, null);
          }
        } }
      );
      knownRecipes = result.knownRecipes;
      applyCatalog({ config: state.config, recipes: result.recipes, patrons: state.patrons, news: state.news });
      renderFeatured();
      renderChips();
      renderRecipes();
      resetDownload();
      showProgress("Обновление установлено", 1, 1);
      progressCount.textContent = "Готово";
      setStatus("✓ Архив обновлён. Загруженные рецепты доступны без интернета.");
    } catch (error) {
      console.warn("Не удалось сохранить обновления рецептов:", error);
      hideProgress();
      setStatus("Загрузка не завершена. Проверь интернет и свободное место, затем попробуй снова.");
      if (pending) {
        downloadButton.textContent = "↻ Повторить загрузку";
      }
    } finally {
      setBusy(false);
    }
  });
}

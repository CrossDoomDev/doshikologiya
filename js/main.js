import { loadCatalog } from "./core/api.js";
import { applyCatalog, state } from "./core/state.js";
import { goTo } from "./core/router.js";
import { renderBanner, renderFeatured } from "./features/home.js";
import { renderChips, renderRecipes, selectCategory, setSearch, toggleFavoritesOnly } from "./features/recipes.js";
import { renderPatrons } from "./features/support.js";
import { openRecipe, closeRecipe, toggleFavorite } from "./features/recipe-modal.js";
import { openDonation } from "./features/donations.js";
import { initCookingLayout, openCookMode, closeCookMode, navigateCookStep, closeCookSuccess } from "./features/cooking.js";
import { renderDailyIndex, runOracle } from "./features/oracle.js";

function renderAll() {
  renderBanner();
  renderFeatured();
  renderChips();
  renderRecipes();
  renderPatrons();
  renderDailyIndex();
}

function bindEvents() {
  document.addEventListener("click", event => {
    const route = event.target.closest("[data-route]");
    if (route) {
      event.preventDefault();
      goTo(route.dataset.route);
      return;
    }

    const category = event.target.closest("[data-category]");
    if (category) {
      selectCategory(category.dataset.category, { fromHome: Boolean(category.closest("#homeCategoryChips")) });
      return;
    }

    const favorite = event.target.closest("[data-favorite]");
    if (favorite) {
      event.preventDefault();
      event.stopPropagation();
      toggleFavorite(favorite.dataset.favorite);
      return;
    }

    const recipe = event.target.closest("[data-open-recipe]");
    if (recipe) {
      event.preventDefault();
      openRecipe(recipe.dataset.openRecipe);
      return;
    }

    if (event.target.closest("[data-donate]")) {
      event.preventDefault();
      openDonation();
    }
  });

  document.getElementById("recipeSearch").addEventListener("input", event => setSearch(event.target.value));
  document.getElementById("favoritesToggle").addEventListener("click", toggleFavoritesOnly);
  document.getElementById("oracleBtn").addEventListener("click", runOracle);
  document.getElementById("closeModal").addEventListener("click", closeRecipe);
  document.getElementById("recipeModal").addEventListener("click", event => {
    if (event.target.id === "recipeModal") closeRecipe();
  });
  document.getElementById("modalFavoriteBtn").addEventListener("click", () => {
    if (state.currentRecipe) toggleFavorite(state.currentRecipe.id);
  });
  document.getElementById("cookStartBtn").addEventListener("click", openCookMode);
  document.getElementById("cookCloseBtn").addEventListener("click", closeCookMode);
  document.getElementById("cookPrevBtn").addEventListener("click", () => navigateCookStep(-1));
  document.getElementById("cookNextBtn").addEventListener("click", () => navigateCookStep(1));
  document.getElementById("successBackBtn").addEventListener("click", () => closeCookSuccess({ returnToRecipe: true }));
  document.getElementById("successDoneBtn").addEventListener("click", () => closeCookSuccess({ returnToRecipe: false }));
  document.getElementById("cookSuccess").addEventListener("click", event => {
    if (event.target.id === "cookSuccess") closeCookSuccess({ returnToRecipe: true });
  });

  document.addEventListener("keydown", event => {
    if (event.key !== "Escape") return;
    const success = document.getElementById("cookSuccess");
    if (success.classList.contains("open")) {
      closeCookSuccess({ returnToRecipe: true });
      return;
    }
    closeCookMode();
    closeRecipe();
  });

  document.addEventListener("error", event => {
    const image = event.target;
    if (image instanceof HTMLImageElement) image.style.opacity = .25;
  }, true);
}

async function boot() {
  bindEvents();
  initCookingLayout();
  goTo(location.hash.replace("#", "") || "home");

  const catalog = await loadCatalog();
  applyCatalog(catalog);
  renderAll();
}

boot().catch(error => {
  console.error("Не удалось запустить Дошикологию:", error);
  renderAll();
});

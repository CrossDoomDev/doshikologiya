import { loadCatalog } from "./core/api.js?v=20261011-progress1";
import { readCachedServiceContacts, applyServiceContacts, fetchRemoteServiceContacts, validSupportEmail } from "./core/remote-service-contacts.js?v=20261012-remote-settings1";
import { initBackNavigation } from "./core/android-back.js?v=20261012-exitconfirm1";
import { initRecipeUpdates } from "./features/recipe-updates.js?v=20261012-hidden-recipe-dates1";
import { applyCatalog, state } from "./core/state.js";
import { shareRecipe, incomingRecipeId } from "./features/sharing.js?v=20261010-offline-share1";
import { goTo } from "./core/router.js?v=20261011-more1";
import { renderBanner, renderFeatured } from "./features/home.js";
import { renderChips, renderRecipes, selectCategory, setSearch, toggleFavoritesOnly, setRecipeLayout, resumeRecipePagination } from "./features/recipes.js?v=20261013-layout-fast2";
import { renderPatrons } from "./features/support.js?v=20261011-home-feed1";
import { renderNews } from "./features/news.js?v=20261012-dateonly1";
import { openRecipe, closeRecipe, toggleFavorite } from "./features/recipe-modal.js?v=20261012-hidden-recipe-dates1";
import { openDonation } from "./features/donations.js";
import { initCookingLayout, openCookMode, closeCookMode, navigateCookStep, closeCookSuccess, updateCookSuccessFavorite } from "./features/cooking.js?v=20261014-rsya1";
import { renderDailyIndex, runOracle } from "./features/oracle.js?v=20261010-oracle-art2";
import { initSplash, hideSplash } from "./features/splash.js?v=20261009-motion2";
import { initPressFeedback } from "./features/press-feedback.js?v=20261009-motion2";

function renderServiceInfo() {
  const version = state.config.version || "—";
  const versionElement = document.getElementById("moreAppVersion");
  if (versionElement) versionElement.textContent = version;

  const contact = String(state.config.supportEmail || "").trim();
  const supportLink = document.getElementById("supportEmailLink");
  const pending = document.getElementById("supportEmailPending");
  if (supportLink && pending) {
    const valid = validSupportEmail(contact);
    supportLink.classList.toggle("hidden", !valid);
    pending.classList.toggle("hidden", valid);
    const subject = encodeURIComponent("Дошикология — обращение в поддержку (версия " + version + ")");
    supportLink.href = valid ? "mailto:" + encodeURIComponent(contact) + "?subject=" + subject : "mailto:";
  }
}

function nativeAndroid() {
  return globalThis.Capacitor?.isNativePlatform?.() === true;
}

// Online contact refresh runs in the background. The offline recipe catalog is unchanged.
let serviceContactsRequest = null;
function refreshServiceContacts() {
  if (!nativeAndroid() || !state.config.remoteConfigUrl) return Promise.resolve();
  if (serviceContactsRequest) return serviceContactsRequest;
  serviceContactsRequest = fetchRemoteServiceContacts(state.config.remoteConfigUrl)
    .then(contacts => {
      state.config = applyServiceContacts(state.config, contacts);
      renderServiceInfo();
    })
    .catch(error => console.warn("Контактные данные не обновлены; используем сохранённые:", error))
    .finally(() => { serviceContactsRequest = null; });
  return serviceContactsRequest;
}

function renderAll() {
  renderBanner();
  renderFeatured();
  renderChips();
  renderRecipes();
  renderPatrons();
  renderNews();
  renderDailyIndex();
  renderServiceInfo();
}

function bindEvents() {
  document.addEventListener("click", event => {
    const route = event.target.closest("[data-route]");
    if (route) {
      event.preventDefault();
      goTo(route.dataset.route);
      if (route.dataset.route === "recipes") resumeRecipePagination();
      if (route.dataset.route === "more") void refreshServiceContacts();
      return;
    }

    const layout = event.target.closest("[data-recipe-layout]");
    if (layout) {
      setRecipeLayout(layout.dataset.recipeLayout);
      return;
    }

    const category = event.target.closest("[data-category]");
    if (category) {
      selectCategory(category.dataset.category);
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
  document.getElementById("modalShareBtn").addEventListener("click", event => shareRecipe(state.currentRecipe, event.currentTarget));
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
  document.getElementById("successFavoriteBtn").addEventListener("click", () => {
    if (!state.currentRecipe) return;
    toggleFavorite(state.currentRecipe.id);
    updateCookSuccessFavorite();
  });
  document.getElementById("cookSuccess").addEventListener("click", event => {
    if (event.target.id === "cookSuccess") closeCookSuccess({ returnToRecipe: true });
  });

  // A previously saved recipe may point to a deleted legacy image.
  // Use one known local asset instead of leaving a broken picture on screen.
  document.addEventListener("error", event => {
    const image = event.target;
    if (!(image instanceof HTMLImageElement)) return;
    const fallback = new URL("images/ui/doshikologiya-mark.svg", document.baseURI).href;
    if (image.src === fallback) {
      // The fallback itself failed: stop here, do not create a retry loop.
      image.removeAttribute("src");
      image.style.opacity = "0.25";
      return;
    }
    image.src = fallback;
    image.style.objectFit = "contain";
    image.style.opacity = "0.9";
    if (image.id === "cookImage") {
      const backdrop = document.getElementById("cookBackdrop");
      if (backdrop) backdrop.style.backgroundImage = `url("${fallback}")`;
    }
  }, true);
}

async function boot() {
  initSplash();
  initPressFeedback();
  bindEvents();
  initBackNavigation({ closeCookSuccess, closeCookMode, closeRecipe, goTo });
  initCookingLayout();
  goTo(location.hash.replace("#", "") || "home");

  const catalog = await loadCatalog();
  applyCatalog(catalog);
  if (nativeAndroid()) {
    const savedContacts = readCachedServiceContacts();
    if (Object.keys(savedContacts).length) state.config = applyServiceContacts(state.config, savedContacts);
  }
  renderAll();
  if (!document.getElementById("view-recipes")?.classList.contains("hidden")) resumeRecipePagination();
  initRecipeUpdates(catalog);
  hideSplash();
  // Sync the support address and donation link independently of recipe updates.
  void refreshServiceContacts();

  // Shared links open a specific recipe, including a newly downloaded recipe.
  const sharedId = incomingRecipeId();
  let sharedOpened = false;
  function openSharedRecipe() {
    if (sharedOpened || !sharedId || !state.recipes.some(recipe => recipe.id === sharedId)) return;
    goTo("recipes");
    openRecipe(sharedId);
    sharedOpened = true;
  }
  openSharedRecipe();


}

boot().catch(error => {
  console.error("Не удалось запустить Дошикологию:", error);
  renderAll();
  hideSplash();
});

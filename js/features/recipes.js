import { state, getAllCategories } from "../core/state.js";
import { escapeHtml, getCategories } from "../core/utils.js";
import { isFavorite } from "../core/favorites.js";
import { latestRecipes } from "../core/chronology.js?v=20261012-dateonly1";
import { RECIPE_LAYOUTS, saveRecipeLayout } from "../core/recipe-layout.js?v=20261012-recipe-layout1";

function recipeCard(recipe) {
  const category = getCategories(recipe)[0];
  const favorite = isFavorite(recipe.id);

  return `<article class="recipe-card">
    <div class="recipe-media">
      <img class="recipe-image" src="${escapeHtml(recipe.image || "")}" alt="${escapeHtml(recipe.title)}" loading="lazy" decoding="async">
      <button class="favorite-btn ${favorite ? "on" : ""}" data-favorite="${escapeHtml(recipe.id)}" aria-label="${favorite ? "Убрать из избранного" : "Добавить в избранное"}" aria-pressed="${favorite ? "true" : "false"}">
        <svg class="favorite-heart" viewBox="0 0 24 24" aria-hidden="true"><use href="images/ui/heart.svg#heart"></use></svg>
      </button>
      <span class="card-category">${escapeHtml(category)}</span>
    </div>
    <div class="recipe-body">
      <h3>${escapeHtml(recipe.title)}</h3>
      <p>${escapeHtml(recipe.description)}</p>
      <div class="meta"><span class="tag">⏱ ${escapeHtml(recipe.time)}</span><span class="tag">🔥 ${escapeHtml(recipe.difficulty)}</span></div>
      <button class="btn dosh-action-btn card-open" data-open-recipe="${escapeHtml(recipe.id)}"><span class="btn-emoji" aria-hidden="true">📖</span> Открыть протокол</button>
    </div>
  </article>`;
}

function recentRecipeCard(recipe) {
  return `<button class="recent-recipe" type="button" data-open-recipe="${escapeHtml(recipe.id)}" aria-label="Открыть рецепт: ${escapeHtml(recipe.title)}">
    <img class="recent-recipe-image" src="${escapeHtml(recipe.image || "")}" alt="" loading="lazy">
    <span class="recent-recipe-info">
      <strong>${escapeHtml(recipe.title)}</strong>
    </span>
  </button>`;
}
// Keep the DOM small: Android WebView only paints the first page of the archive.
const PAGE_SIZE = 18;
let visibleRecipes = [];
let renderedCount = 0;
let observer = null;
let searchTimer = null;
let cachedSearchIndex = new WeakMap();

function searchText(recipe) {
  let text = cachedSearchIndex.get(recipe);
  if (text === undefined) {
    text = [recipe.title, recipe.description, recipe.story, ...getCategories(recipe),
      ...(Array.isArray(recipe.ingredients) ? recipe.ingredients : [])].join(" ").toLowerCase();
    cachedSearchIndex.set(recipe, text);
  }
  return text;
}

function filteredRecipes() {
  const query = state.currentSearch.trim().toLowerCase();
  return state.recipes.filter(recipe =>
    (state.currentCategory === "Все" || getCategories(recipe).includes(state.currentCategory)) &&
    (!state.showFavoritesOnly || isFavorite(recipe.id)) &&
    (!query || searchText(recipe).includes(query))
  );
}

function appendRecipePage() {
  const box = document.getElementById("recipesGrid");
  if (!box || renderedCount >= visibleRecipes.length) return;
  const next = visibleRecipes.slice(renderedCount, renderedCount + PAGE_SIZE);
  box.insertAdjacentHTML("beforeend", next.map(recipeCard).join(""));
  renderedCount += next.length;
  if (renderedCount >= visibleRecipes.length) {
    observer?.disconnect();
    observer = null;
    document.getElementById("recipesLoadMore")?.remove();
  }
}

function attachPagination() {
  const box = document.getElementById("recipesGrid");
  if (renderedCount >= visibleRecipes.length) return;
  const button = document.createElement("button");
  button.id = "recipesLoadMore";
  button.type = "button";
  button.className = "btn secondary recipes-load-more";
  button.textContent = "Показать ещё рецепты";
  button.addEventListener("click", appendRecipePage);
  box.after(button);
  if ("IntersectionObserver" in window) {
    observer = new IntersectionObserver(entries => {
      if (entries.some(entry => entry.isIntersecting)) appendRecipePage();
    }, { rootMargin: "350px 0px" });
    // A hidden tab should not eagerly load the entire archive.
    if (!document.getElementById("view-recipes")?.classList.contains("hidden")) observer.observe(button);
  }
}

export function renderRecipes() {
  const homeLimit = Math.min(5, state.config.homeRecipeLimit || 5);
  const home = latestRecipes(state.recipes, homeLimit);
  const homeBox = document.getElementById("homeRecipes");
  const recipesBox = document.getElementById("recipesGrid");
  if (!homeBox || !recipesBox) return;
  observer?.disconnect();
  observer = null;
  document.getElementById("recipesLoadMore")?.remove();
  recipesBox.dataset.layout = state.recipeLayout;
  document.querySelectorAll("[data-recipe-layout]").forEach(button => {
    button.setAttribute("aria-pressed", String(button.dataset.recipeLayout === state.recipeLayout));
  });
  homeBox.innerHTML = home.length
    ? home.map(recentRecipeCard).join("")
    : '<div class="empty-state">Архив рецептов пока недоступен.</div>';

  visibleRecipes = filteredRecipes();
  renderedCount = 0;
  recipesBox.innerHTML = visibleRecipes.length
    ? ""
    : '<div class="empty-state">Лаборатория ничего не нашла. Возможно, рецепт засекречен.</div>';
  appendRecipePage();
  attachPagination();
}

export function setRecipeLayout(layout) {
  if (!RECIPE_LAYOUTS.includes(layout) || layout === state.recipeLayout) return;
  state.recipeLayout = saveRecipeLayout(layout);
  renderRecipes();
}

export function renderChips() {
  const categories = getAllCategories();
  const markup = categories.map(category =>
    `<button class="chip ${category === state.currentCategory ? "active" : ""}" data-category="${escapeHtml(category)}">${escapeHtml(category)}</button>`
  ).join("");

  document.getElementById("recipeCategoryChips").innerHTML = markup;
}

export function selectCategory(category) {
  state.currentCategory = getAllCategories().includes(category) ? category : "Все";
  renderChips();
  renderRecipes();
}

export function setSearch(value) {
  state.currentSearch = String(value || "");
  clearTimeout(searchTimer);
  searchTimer = setTimeout(renderRecipes, 160);
}

export function toggleFavoritesOnly() {
  state.showFavoritesOnly = !state.showFavoritesOnly;
  const button = document.getElementById("favoritesToggle");
  button.querySelector(".favorite-filter-label").textContent = state.showFavoritesOnly ? "Показываем избранное" : "Только избранное";
  button.setAttribute("aria-pressed", String(state.showFavoritesOnly));
  renderRecipes();
}

export function resumeRecipePagination() {
  const button = document.getElementById("recipesLoadMore");
  if (button && observer) observer.observe(button);
}

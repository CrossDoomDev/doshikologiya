import { state } from "../core/state.js";
import { escapeHtml } from "../core/utils.js";

function dailyRecipe() {
  if (!state.recipes.length) return null;
  const preferred = state.recipes.find(recipe => recipe.id === state.config.featuredRecipeId);
  const days = Math.floor(Date.now() / 86400000);
  return days % 3 === 0 && preferred ? preferred : state.recipes[days % state.recipes.length];
}

export function renderBanner() {
  document.getElementById("bannerTitle").textContent = state.config.banner?.title || "";
  document.getElementById("bannerText").textContent = state.config.banner?.text || "";
  document.getElementById("appVersion").textContent = state.config.version || "—";
}

export function renderFeatured() {
  const recipe = dailyRecipe();
  const title = document.getElementById("featuredTitle");
  const description = document.getElementById("featuredDescription");
  const image = document.getElementById("featuredImage");
  const meta = document.getElementById("featuredMeta");
  const button = document.getElementById("featuredOpenBtn");

  if (!recipe) {
    title.textContent = "Архив рецептов временно недоступен";
    description.textContent = "Лаборатория не смогла получить каталог. Попробуй обновить страницу чуть позже.";
    image.removeAttribute("src");
    image.alt = "";
    meta.innerHTML = "";
    button.disabled = true;
    delete button.dataset.openRecipe;
    return;
  }

  title.textContent = recipe.title;
  description.textContent = recipe.description;
  image.src = recipe.image || "";
  image.alt = recipe.title;
  meta.innerHTML = `<span class="tag">⏱ ${escapeHtml(recipe.time)}</span><span class="tag">🔥 ${escapeHtml(recipe.difficulty)}</span><span class="tag">💸 ${escapeHtml(recipe.cost)}</span>`;
  button.disabled = false;
  button.dataset.openRecipe = recipe.id;
}

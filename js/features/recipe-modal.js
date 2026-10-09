import { state } from "../core/state.js";
import { escapeHtml } from "../core/utils.js";
import { isFavorite, toggleFavoriteValue } from "../core/favorites.js";
import { renderRecipes } from "./recipes.js";

export function openRecipe(id) {
  const recipe = state.recipes.find(item => item.id === id);
  if (!recipe) return;

  state.currentRecipe = recipe;
  document.getElementById("modalPhoto").src = recipe.image || "";
  document.getElementById("modalPhoto").alt = recipe.title;
  document.getElementById("modalTitle").textContent = recipe.title;
  document.getElementById("modalDescription").textContent = recipe.description;
  document.getElementById("modalMeta").innerHTML =
    `<span class="tag">⏱ ${escapeHtml(recipe.time)}</span><span class="tag">🔥 ${escapeHtml(recipe.difficulty)}</span><span class="tag">💸 ${escapeHtml(recipe.cost)}</span>`;
  document.getElementById("modalIngredients").innerHTML =
    recipe.ingredients.map(item => `<li>${escapeHtml(item)}</li>`).join("");
  document.getElementById("modalStory").textContent = recipe.story || "";
  updateModalFavorite();

  const modal = document.getElementById("recipeModal");
  modal.classList.add("open");
  modal.setAttribute("aria-hidden", "false");
  document.body.style.overflow = "hidden";
}

const MODAL_HEART_ICON = '<svg class="favorite-heart modal-favorite-heart" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 20.6C10.65 19.55 3.55 15.05 2.55 9.65C1.95 6.4 4.05 3.55 7.25 3.55C9.35 3.55 11.05 4.7 12 6.25C12.95 4.7 14.65 3.55 16.75 3.55C19.95 3.55 22.05 6.4 21.45 9.65C20.45 15.05 13.35 19.55 12 20.6Z"/></svg>';

export function updateModalFavorite() {
  const button = document.getElementById("modalFavoriteBtn");
  if (!state.currentRecipe) return;

  const active = isFavorite(state.currentRecipe.id);
  button.innerHTML = `${MODAL_HEART_ICON} ${active ? "В избранном" : "Добавить в избранное"}`;
  button.classList.toggle("is-favorite", active);
  button.setAttribute("aria-pressed", String(active));
}

export function toggleFavorite(id) {
  if (!id) return;
  toggleFavoriteValue(id);
  renderRecipes();
  if (state.currentRecipe?.id === id) updateModalFavorite();
}

export function closeRecipe() {
  const modal = document.getElementById("recipeModal");
  modal.classList.remove("open");
  modal.setAttribute("aria-hidden", "true");
  document.body.style.overflow = "";
}

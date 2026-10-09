export function escapeHtml(value = "") {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

export function getCategories(recipe) {
  return Array.isArray(recipe?.categories) && recipe.categories.length
    ? recipe.categories
    : ["Эксперимент"];
}

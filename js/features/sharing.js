export const PUBLIC_RECIPE_URL = "https://crossdoomdev.github.io/doshikologiya/";

export function recipeShareUrl(id) {
  const url = new URL(PUBLIC_RECIPE_URL);
  url.searchParams.set("recipe", id);
  url.hash = "recipes";
  return url.href;
}

export function incomingRecipeId() {
  return new URLSearchParams(window.location.search).get("recipe");
}

export async function shareRecipe(recipe, button) {
  if (!recipe) return;
  const url = recipeShareUrl(recipe.id);
  const message = "🍜 Рассекречен лабораторный протокол «" + recipe.title + "»! Проверь, выдержит ли твоя кухня этот эксперимент.";

  if (typeof navigator.share === "function") {
    try {
      await navigator.share({
        title: "Дошикология: " + recipe.title,
        text: message,
        url
      });
      return;
    } catch (error) {
      if (error && error.name === "AbortError") return;
    }
  }
  try {
    await navigator.clipboard.writeText(message + "\n" + url);
    const originalLabel = button.textContent;
    button.textContent = "✓ Протокол скопирован";
    window.setTimeout(() => {
      if (button.isConnected) button.textContent = originalLabel;
    }, 2200);
  } catch {
    window.prompt("Скопируй ссылку на секретный протокол:", url);
  }
}

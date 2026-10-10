import { DATA_URLS, DEFAULT_CONFIG } from "./config.js";
import { loadCachedRecipes, saveCachedRecipes, mergeRecipes, hydrateRecipeImages, cacheRemoteImages } from "./offline-catalog.js";

async function fetchJson(url) {
  const response = await fetch(url, { cache: "no-store" });
  if (!response.ok) throw new Error(`HTTP ${response.status}: ${url}`);
  return response.json();
}

function normalizeConfig(value) {
  const banner = value?.banner && typeof value.banner === "object"
    ? { ...DEFAULT_CONFIG.banner, ...value.banner }
    : { ...DEFAULT_CONFIG.banner };

  return {
    ...DEFAULT_CONFIG,
    ...(value && typeof value === "object" ? value : {}),
    banner,
    categoryOrder: Array.isArray(value?.categoryOrder) && value.categoryOrder.length
      ? value.categoryOrder
      : [...DEFAULT_CONFIG.categoryOrder],
    homeRecipeLimit: Number.isInteger(value?.homeRecipeLimit) && value.homeRecipeLimit > 0
      ? value.homeRecipeLimit
      : DEFAULT_CONFIG.homeRecipeLimit
  };
}

function normalizeRecipes(value) {
  if (!Array.isArray(value)) return [];
  const seen = new Set();

  return value.filter((recipe, index) => {
    const valid = recipe
      && typeof recipe.id === "string"
      && recipe.id.trim()
      && typeof recipe.title === "string"
      && recipe.title.trim()
      && Array.isArray(recipe.ingredients)
      && recipe.ingredients.length
      && Array.isArray(recipe.steps)
      && recipe.steps.length;

    if (!valid) {
      console.warn("Пропущен некорректный рецепт", index, recipe);
      return false;
    }
    if (seen.has(recipe.id)) {
      console.warn("Пропущен рецепт с повторяющимся id:", recipe.id);
      return false;
    }
    seen.add(recipe.id);
    return true;
  });
}

function normalizePatrons(value) {
  return Array.isArray(value)
    ? value.filter(item => item && typeof item.name === "string" && typeof item.text === "string")
    : [];
}

export async function loadCatalog() {
  // Bundled JSON and images work without internet inside an Android APK.
  const results = await Promise.allSettled([
    fetchJson(DATA_URLS.config),
    fetchJson(DATA_URLS.recipes),
    fetchJson(DATA_URLS.patrons),
    loadCachedRecipes()
  ]);

  const bundledRecipes = normalizeRecipes(results[1].status === "fulfilled" ? results[1].value : []);
  const cachedRecipes = normalizeRecipes(results[3].status === "fulfilled" ? results[3].value : []);
  const config = normalizeConfig(results[0].status === "fulfilled" ? results[0].value : null);
  const knownRecipes = mergeRecipes(bundledRecipes, cachedRecipes);
  const recipes = await hydrateRecipeImages(knownRecipes, bundledRecipes, config.remoteRecipesUrl);

  if (results[0].status === "rejected") console.warn("Не удалось загрузить конфигурацию:", results[0].reason);
  if (results[1].status === "rejected") console.warn("Не удалось загрузить базовые рецепты:", results[1].reason);
  if (results[2].status === "rejected") console.warn("Не удалось загрузить стену меценатов:", results[2].reason);

  return {
    config,
    recipes,
    patrons: normalizePatrons(results[2].status === "fulfilled" ? results[2].value : []),
    bundledRecipes,
    knownRecipes
  };
}

export function diffOnlineRecipes(knownRecipes, downloaded) {
  const known = new Map(knownRecipes.map(recipe => [recipe.id, recipe]));
  let added = 0;
  let updated = 0;
  for (const recipe of downloaded) {
    const previous = known.get(recipe.id);
    if (!previous) added++;
    else if (JSON.stringify(previous) !== JSON.stringify(recipe)) updated++;
  }
  return { added, updated, hasUpdates: added + updated > 0 };
}

export async function checkOnlineRecipes(config, knownRecipes) {
  if (!config.remoteRecipesUrl) throw new Error("Адрес удалённого архива не настроен.");
  const downloaded = normalizeRecipes(await fetchJson(config.remoteRecipesUrl));
  if (!downloaded.length) throw new Error("Удалённый архив пуст или недоступен.");
  return { ...diffOnlineRecipes(knownRecipes, downloaded), downloaded };
}

export async function installOnlineRecipes(config, bundledRecipes, knownRecipes, downloaded) {
  if (!Array.isArray(downloaded) || !downloaded.length) throw new Error("Нет рецептов для загрузки.");
  await cacheRemoteImages(downloaded, config.remoteRecipesUrl);
  const nextKnownRecipes = mergeRecipes(knownRecipes, downloaded);
  const saved = await saveCachedRecipes(nextKnownRecipes);
  if (!saved) throw new Error("Не удалось сохранить рецепты на устройстве.");
  return {
    recipes: await hydrateRecipeImages(nextKnownRecipes, bundledRecipes, config.remoteRecipesUrl),
    knownRecipes: nextKnownRecipes
  };
}

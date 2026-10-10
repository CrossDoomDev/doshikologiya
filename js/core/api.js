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
  const recipes = await hydrateRecipeImages(mergeRecipes(bundledRecipes, cachedRecipes), bundledRecipes, config.remoteRecipesUrl);

  if (results[0].status === "rejected") console.warn("Не удалось загрузить конфигурацию:", results[0].reason);
  if (results[1].status === "rejected") console.warn("Не удалось загрузить базовые рецепты:", results[1].reason);
  if (results[2].status === "rejected") console.warn("Не удалось загрузить стену меценатов:", results[2].reason);

  return {
    config,
    recipes,
    patrons: normalizePatrons(results[2].status === "fulfilled" ? results[2].value : []),
    bundledRecipes
  };
}

export async function refreshOnlineRecipes(config, bundledRecipes) {
  if (!config.remoteRecipesUrl) return null;
  try {
    const downloaded = normalizeRecipes(await fetchJson(config.remoteRecipesUrl));
    if (!downloaded.length) return null;
    await saveCachedRecipes(downloaded);
    await cacheRemoteImages(downloaded, config.remoteRecipesUrl);
    return hydrateRecipeImages(mergeRecipes(bundledRecipes, downloaded), bundledRecipes, config.remoteRecipesUrl);
  } catch (error) {
    console.info("Офлайн-режим: используем сохранённые рецепты.", error);
    return null;
  }
}

import { STORAGE_KEYS } from "./config.js";

export const RECIPE_LAYOUTS = Object.freeze(["large", "compact", "grid"]);

export function normalizeRecipeLayout(layout) {
  return RECIPE_LAYOUTS.includes(layout) ? layout : "large";
}

export function readRecipeLayout(storage) {
  try {
    const store = storage === undefined ? globalThis.localStorage : storage;
    return normalizeRecipeLayout(store?.getItem(STORAGE_KEYS.recipeLayout));
  } catch {
    return "large";
  }
}

export function saveRecipeLayout(layout, storage) {
  const value = normalizeRecipeLayout(layout);
  try {
    const store = storage === undefined ? globalThis.localStorage : storage;
    store?.setItem(STORAGE_KEYS.recipeLayout, value);
  } catch {
    // Private browsing or a restricted WebView can refuse storage.
  }
  return value;
}

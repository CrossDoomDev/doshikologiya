import { STORAGE_KEYS } from "./config.js";
import { state } from "./state.js";

export function isFavorite(id) {
  return state.favorites.has(id);
}

export function toggleFavoriteValue(id) {
  if (isFavorite(id)) state.favorites.delete(id);
  else state.favorites.add(id);

  localStorage.setItem(STORAGE_KEYS.favorites, JSON.stringify([...state.favorites]));
  return isFavorite(id);
}

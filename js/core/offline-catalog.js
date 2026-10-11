// IndexedDB stores a downloaded catalog and its images for future Android packaging.
const DB_NAME = "doshikologiya-offline";
const DB_VERSION = 1;
let databasePromise;
const objectUrls = new Map();

function database() {
  if (!("indexedDB" in globalThis)) return Promise.resolve(null);
  if (databasePromise) return databasePromise;
  databasePromise = new Promise(resolve => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      if (!request.result.objectStoreNames.contains("catalog")) request.result.createObjectStore("catalog");
      if (!request.result.objectStoreNames.contains("images")) request.result.createObjectStore("images");
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => resolve(null);
    request.onblocked = () => resolve(null);
  });
  return databasePromise;
}

async function read(store, key) {
  const db = await database();
  if (!db) return null;
  return new Promise(resolve => {
    try {
      const request = db.transaction(store, "readonly").objectStore(store).get(key);
      request.onsuccess = () => resolve(request.result ?? null);
      request.onerror = () => resolve(null);
    } catch { resolve(null); }
  });
}

async function write(store, key, value) {
  const db = await database();
  if (!db) return false;
  return new Promise(resolve => {
    try {
      const tx = db.transaction(store, "readwrite");
      tx.objectStore(store).put(value, key);
      tx.oncomplete = () => resolve(true);
      tx.onerror = () => resolve(false);
      tx.onabort = () => resolve(false);
    } catch { resolve(false); }
  });
}

export async function loadCachedRecipes() {
  const value = await read("catalog", "recipes");
  return Array.isArray(value) ? value : [];
}

export function saveCachedRecipes(recipes) {
  return write("catalog", "recipes", recipes);
}

export function mergeRecipes(bundled, downloaded) {
  const merged = new Map(bundled.map(recipe => [recipe.id, recipe]));
  for (const recipe of downloaded) merged.set(recipe.id, recipe);
  return [...merged.values()];
}

function remoteImageUrl(image, catalogUrl) {
  if (!image || !catalogUrl) return null;
  try {
    return new URL(image, new URL("../", new URL(catalogUrl, location.href))).href;
  } catch { return null; }
}

export async function hydrateRecipeImages(recipes, bundledRecipes, catalogUrl) {
  // Browser: use current same-origin images directly. Android: prefer saved offline blobs.
  if (globalThis.Capacitor?.isNativePlatform?.() !== true) return recipes;
  const bundledById = new Map(bundledRecipes.map(recipe => [recipe.id, recipe]));
  return Promise.all(recipes.map(async recipe => {
    // Old downloaded SVG art must not override the newer bundled WebP photo.
    const bundled = bundledById.get(recipe.id);
    if (bundled?.image?.split("?")[0]?.endsWith(".webp")
      && recipe.image?.split("?")[0]?.endsWith(".svg")) {
      return { ...recipe, image: bundled.image };
    }
    const remoteUrl = remoteImageUrl(recipe.image, catalogUrl);
    const blob = remoteUrl ? await read("images", remoteUrl) : null;
    if (blob instanceof Blob) {
      if (!objectUrls.has(remoteUrl)) objectUrls.set(remoteUrl, URL.createObjectURL(blob));
      return { ...recipe, image: objectUrls.get(remoteUrl) };
    }
    // An older downloaded catalog can still reference deleted SVGs.
    // Prefer the current packaged photograph for known IDs, not that stale URL.
    if (bundled?.image) return { ...recipe, image: bundled.image };
    // A new recipe whose image was not saved yet remains usable offline.
    return { ...recipe, image: "images/ui/doshikologiya-mark.svg" };
  }));
}

export async function cacheRemoteImages(recipes, catalogUrl) {
  const urls = [...new Set(recipes.map(recipe => remoteImageUrl(recipe.image, catalogUrl)).filter(Boolean))];
  let position = 0;
  let failed = 0;
  async function worker() {
    while (position < urls.length) {
      const url = urls[position++];
      if (await read("images", url)) continue;
      try {
        const response = await fetch(url);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const blob = await response.blob();
        if (!blob.type.startsWith("image/") || !(await write("images", url, blob))) {
          throw new Error("Не удалось сохранить изображение");
        }
      } catch {
        failed++;
        // Successful images remain cached; next attempt retrieves only missing ones.
      }
    }
  }
  await Promise.all(Array.from({ length: Math.min(3, urls.length) }, worker));
  if (failed) throw new Error(`Не удалось скачать изображения: ${failed}`);
}

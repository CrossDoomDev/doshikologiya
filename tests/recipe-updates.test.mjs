import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import { diffOnlineRecipes, recipeFingerprint, diffOnlineRecipeIndex } from "../js/core/api.js";

const local = [{ id: "a", title: "Локальный", ingredients: ["лапша"], steps: ["готовить"] }];

test("одинаковый каталог не считается обновлением", () => {
  assert.deepEqual(diffOnlineRecipes(local, structuredClone(local)), {
    added: 0, updated: 0, hasUpdates: false
  });
});

test("новые и изменённые протоколы определяются отдельно", () => {
  const remote = [
    { ...local[0], title: "Новая редакция" },
    { id: "b", title: "Новый рецепт", ingredients: ["лапша"], steps: ["готовить"] }
  ];
  assert.deepEqual(diffOnlineRecipes(local, remote), {
    added: 1, updated: 1, hasUpdates: true
  });
});

test("загрузка и проверка выполняются только по кнопкам", () => {
  const main = fs.readFileSync(new URL("../js/main.js", import.meta.url), "utf8");
  assert.doesNotMatch(main, /refreshOnlineRecipes|checkOnlineRecipes\s*\(/);
  assert.match(main, /initRecipeUpdates\(catalog\)/);
  const ui = fs.readFileSync(new URL("../js/features/recipe-updates.js", import.meta.url), "utf8");
  assert.match(ui, /checkButton\.addEventListener\("click"/);
  assert.match(ui, /downloadButton\.addEventListener\("click"/);
});

test("незагруженные удалённые изображения не запрашиваются на старте", () => {
  const source = fs.readFileSync(new URL("../js/core/offline-catalog.js", import.meta.url), "utf8");
  assert.match(source, /image: "images\/ui\/doshikologiya-mark\.svg"/);
  assert.doesNotMatch(source, /image: remoteUrl/);
});

test("индекс не скачивает повторно неизменённые рецепты", () => {
  const recipe = { id: "a", title: "Локальный", ingredients: ["лапша"], steps: ["готовить"] };
  const index = { schemaVersion: 1, recipes: [
    { id: "a", hash: recipeFingerprint(recipe), path: "recipe-entries/a.json" },
    { id: "b", hash: "12345678", path: "recipe-entries/b.json" }
  ] };
  assert.deepEqual(diffOnlineRecipeIndex([recipe], index), {
    added: 1, updated: 0, hasUpdates: true, changes: [index.recipes[1]]
  });
  const updated = { ...recipe, title: "Новая версия" };
  assert.equal(diffOnlineRecipeIndex([updated], index).updated, 1);
  assert.equal(diffOnlineRecipeIndex([recipe, { ...recipe, id: "b" }], index).added, 0);
});

test("скачивание использует список изменений и сохраняет старый адрес", () => {
  const api = fs.readFileSync(new URL("../js/core/api.js", import.meta.url), "utf8");
  assert.match(api, /downloadRecipeChanges\(pending\.changes/);
  assert.match(api, /cacheRemoteImages\(changedRecipes/);
  assert.match(api, /config\.remoteRecipeIndexUrl/);
  assert.match(api, /config\.remoteRecipesUrl/);
});

test("checking and downloading expose a visible progress bar, spinner and accessible status", () => {
  const html = fs.readFileSync(new URL("../index.html", import.meta.url), "utf8");
  const ui = fs.readFileSync(new URL("../js/features/recipe-updates.js", import.meta.url), "utf8");
  const styles = fs.readFileSync(new URL("../css/styles.css", import.meta.url), "utf8");
  assert.match(html, /id="recipeUpdateProgressBar" role="progressbar"/);
  assert.match(html, /id="recipeUpdateProgressStage"/);
  assert.match(html, /id="recipeUpdateProgressCount"/);
  assert.match(ui, /setBusy\(true, "checking"\)/);
  assert.match(ui, /setBusy\(true, "downloading"\)/);
  assert.match(ui, /phase === "recipes"/);
  assert.match(ui, /phase === "images"/);
  assert.match(ui, /phase === "saving"/);
  assert.match(ui, /progressBar\.setAttribute\("aria-valuenow"/);
  assert.match(styles, /recipe-update-spin/);
  assert.match(styles, /prefers-reduced-motion:reduce/);
});

test("download progress counts actual recipe and image completion, not simulated timers", () => {
  const api = fs.readFileSync(new URL("../js/core/api.js", import.meta.url), "utf8");
  const cache = fs.readFileSync(new URL("../js/core/offline-catalog.js", import.meta.url), "utf8");
  assert.match(api, /onProgress\?\.\(\{ phase: "recipes", completed, total: entries\.length \}\)/);
  assert.match(api, /phase: "saving"/);
  assert.match(cache, /onProgress\?\.\(\{ completed, total: urls\.length \}\)/);
  assert.match(cache, /finally \{\s*completed\+\+/);
});

test("network calls have timeouts so progress does not spin indefinitely", () => {
  const api = fs.readFileSync(new URL("../js/core/api.js", import.meta.url), "utf8");
  const offline = fs.readFileSync(new URL("../js/core/offline-catalog.js", import.meta.url), "utf8");
  assert.match(api, /AbortSignal\.timeout\(30000\)/);
  assert.match(offline, /AbortSignal\.timeout\(60000\)/);
});

import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import { diffOnlineRecipes } from "../js/core/api.js";

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

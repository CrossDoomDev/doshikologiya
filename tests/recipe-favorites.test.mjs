import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";

test("favorites remain functional in the cooking success screen", () => {
  const get = path => fs.readFileSync(new URL("../" + path, import.meta.url), "utf8");
  const html = get("index.html"), main = get("js/main.js"), cooking = get("js/features/cooking.js"), css = get("css/styles.css");
  assert.match(html, /id="successFavoriteBtn" aria-pressed="false"/);
  assert.match(main, /getElementById\("successFavoriteBtn"\)\.addEventListener\("click"/);
  assert.match(main, /toggleFavorite\(state\.currentRecipe\.id\);\s*updateCookSuccessFavorite\(\)/);
  assert.match(cooking, /isFavorite\(state\.currentRecipe\.id\)/);
  assert.match(css, /\.success-favorite-btn\{grid-column:1\/-1/);
});

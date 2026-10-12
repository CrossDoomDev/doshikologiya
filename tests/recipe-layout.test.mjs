import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import { readRecipeLayout, saveRecipeLayout, normalizeRecipeLayout, RECIPE_LAYOUTS } from "../js/core/recipe-layout.js";
import { state } from "../js/core/state.js";
import { renderRecipes, setRecipeLayout } from "../js/features/recipes.js";

test("catalog layouts persist, validate saved preferences and handle blocked storage", () => {
  const values = new Map();
  const storage = {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value)
  };
  assert.deepEqual(RECIPE_LAYOUTS, ["large", "compact", "grid"]);
  assert.equal(readRecipeLayout(storage), "large");
  for (const mode of RECIPE_LAYOUTS) {
    assert.equal(saveRecipeLayout(mode, storage), mode);
    assert.equal(readRecipeLayout(storage), mode);
  }
  values.set("doshikologiya:recipe-layout", "invalid");
  assert.equal(readRecipeLayout(storage), "large");
  assert.equal(normalizeRecipeLayout("__proto__"), "large");
  const blocked = { getItem() { throw new Error("blocked"); }, setItem() { throw new Error("blocked"); } };
  assert.equal(readRecipeLayout(blocked), "large");
  assert.equal(saveRecipeLayout("compact", blocked), "compact");
});

test("all three layouts keep favorite buttons, recipe opening, search and newest five", () => {
  const oldDoc = globalThis.document;
  const oldStorage = globalThis.localStorage;
  const old = {
    recipes: state.recipes,
    recipeLayout: state.recipeLayout,
    currentCategory: state.currentCategory,
    currentSearch: state.currentSearch,
    showFavoritesOnly: state.showFavoritesOnly
  };
  const buttons = ["large", "compact", "grid"].map(recipeLayout => ({
    dataset: { recipeLayout },
    attributes: {},
    setAttribute(name, value) { this.attributes[name] = value; }
  }));
  const nodes = {
    recipesGrid: { dataset: {}, innerHTML: "" },
    homeRecipes: { innerHTML: "" }
  };
  const storage = new Map();
  try {
    globalThis.document = {
      getElementById: name => nodes[name],
      querySelectorAll: () => buttons
    };
    globalThis.localStorage = {
      setItem: (key,value) => storage.set(key,value),
      getItem: key => storage.get(key) ?? null
    };
    state.recipes = [
      { id: "one", title: "Один", description: "Первый рецепт", time: "7 минут", difficulty: "Легко", categories: ["Лапша"], publishedAt: "2026-10-11T08:00:00Z", image: "" },
      { id: "two", title: "Два", description: "Второй рецепт", time: "9 минут", difficulty: "Средне", categories: ["Лапша"], publishedAt: "2026-10-12T08:00:00Z", image: "" }
    ];
    state.currentCategory = "Все";
    state.currentSearch = "";
    state.showFavoritesOnly = false;
    state.recipeLayout = "large";
    for (const mode of RECIPE_LAYOUTS) {
      setRecipeLayout(mode);
      renderRecipes();
      assert.equal(nodes.recipesGrid.dataset.layout, mode);
      assert.match(nodes.recipesGrid.innerHTML, /data-open-recipe="one"/);
      assert.match(nodes.recipesGrid.innerHTML, /data-favorite="two"/);
      assert.match(nodes.homeRecipes.innerHTML, /data-open-recipe="two"/);
      for (const button of buttons) {
        assert.equal(button.attributes["aria-pressed"], String(button.dataset.recipeLayout === mode));
      }
    }
    assert.equal(storage.get("doshikologiya:recipe-layout"), "grid");
    state.currentSearch = "Первый";
    renderRecipes();
    assert.match(nodes.recipesGrid.innerHTML, /data-open-recipe="one"/);
    assert.doesNotMatch(nodes.recipesGrid.innerHTML, /data-open-recipe="two"/);
    setRecipeLayout("strange");
    assert.equal(nodes.recipesGrid.dataset.layout, "grid");
  } finally {
    globalThis.document = oldDoc;
    globalThis.localStorage = oldStorage;
    Object.assign(state, old);
  }
});

test("recipe layout switch exists only in the catalog, responsive variants preserve homepage feed", () => {
  const index = fs.readFileSync(new URL("../index.html", import.meta.url), "utf8");
  const css = fs.readFileSync(new URL("../css/styles.css", import.meta.url), "utf8");
  const main = fs.readFileSync(new URL("../js/main.js", import.meta.url), "utf8");
  for (const mode of RECIPE_LAYOUTS) {
    assert.equal(index.split('data-recipe-layout="' + mode + '"').length - 1, 1);
  }
  assert.ok(index.includes('id="recipesGrid" data-layout="large"'));
  assert.ok(index.includes('class="recipe-layout-toolbar"'));
  assert.ok(!index.includes('id="moreAppearanceTitle"'));
  assert.ok(!css.includes("#view-more .recipe-layout-options"));
  assert.ok(css.includes('#recipesGrid[data-layout="compact"]'));
  // The global .dosh-action-btn has min-width:250px. In compact mode it
  // becomes a centered text button BELOW the cooking-time metadata.
  assert.ok(css.includes('#recipesGrid[data-layout="compact"] .recipe-body{display:flex;flex-direction:column;'));
  assert.ok(css.includes('#recipesGrid[data-layout="compact"] .card-open{position:static;align-self:center;'));
  assert.ok(css.includes('min-width:0;max-width:164px;width:min(100%,164px)'));
  assert.ok(css.includes('#recipesGrid[data-layout="compact"] .card-open .btn-emoji{display:none}'));
  assert.ok(!css.includes('#recipesGrid[data-layout="compact"] .card-open{position:absolute;'));
  assert.ok(!css.includes('grid-row:1 / span 3'));
  assert.ok(fs.readFileSync(new URL("../js/features/recipes.js", import.meta.url), "utf8").includes("Открыть протокол</button>"));
  assert.ok(css.includes('#recipesGrid[data-layout="grid"]'));
  assert.ok(css.includes('grid-template-columns:repeat(2,minmax(0,1fr))'));
  assert.ok(main.includes('setRecipeLayout(layout.dataset.recipeLayout)'));
});

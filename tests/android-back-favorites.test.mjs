import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import { closeTopOverlay, initBackNavigation } from "../js/core/android-back.js";

test("back closes exactly the topmost recipe layer", () => {
  const calls = [];
  const actions = {
    closeSuccess: () => calls.push("success"),
    closeCooking: () => calls.push("cook"),
    closeRecipe: () => calls.push("recipe")
  };
  assert.equal(closeTopOverlay({ successOpen: true, cookOpen: true, recipeOpen: true }, actions), true);
  assert.deepEqual(calls, ["success"]);
  assert.equal(closeTopOverlay({ successOpen: false, cookOpen: true, recipeOpen: true }, actions), true);
  assert.deepEqual(calls, ["success", "cook"]);
  assert.equal(closeTopOverlay({ successOpen: false, cookOpen: false, recipeOpen: true }, actions), true);
  assert.deepEqual(calls, ["success", "cook", "recipe"]);
  assert.equal(closeTopOverlay({ successOpen: false, cookOpen: false, recipeOpen: false }, actions), false);
});

test("Android Back event returns to recipe, then list, then home", () => {
  const original = {
    document: globalThis.document,
    window: globalThis.window,
    location: globalThis.location
  };
  const visible = { cookSuccess: true, cookOverlay: true, recipeModal: true };
  const calls = [];
  let onBack;
  let onEscape;
  const app = {
    addListener(event, cb) { assert.equal(event, "backButton"); onBack = cb; return Promise.resolve(); },
    exitApp() { calls.push("exit"); }
  };
  globalThis.document = {
    getElementById(id) { return { classList: { contains: cls => cls === "open" && visible[id] } }; },
    addEventListener(event, cb) { if (event === "keydown") onEscape = cb; }
  };
  globalThis.window = { Capacitor: { Plugins: { App: app } } };
  globalThis.location = { hash: "#recipes" };
  try {
    initBackNavigation({
      closeCookSuccess: ({ returnToRecipe }) => {
        assert.equal(returnToRecipe, true);
        visible.cookSuccess = false;
        visible.cookOverlay = false;
        calls.push("success");
      },
      closeCookMode: () => { visible.cookOverlay = false; calls.push("cook"); },
      closeRecipe: () => { visible.recipeModal = false; calls.push("recipe"); },
      goTo: route => { calls.push(route); globalThis.location.hash = "#" + route; }
    });
    assert.equal(typeof onBack, "function");
    onBack(); // success -> recipe
    assert.deepEqual(calls, ["success"]);
    visible.cookOverlay = true;
    onBack(); // cook -> recipe
    assert.deepEqual(calls, ["success", "cook"]);
    onBack(); // recipe -> catalog
    assert.deepEqual(calls, ["success", "cook", "recipe"]);
    onBack(); // catalog -> home
    assert.deepEqual(calls, ["success", "cook", "recipe", "home"]);
    onBack(); // home -> exit
    assert.deepEqual(calls, ["success", "cook", "recipe", "home", "exit"]);
    visible.recipeModal = true;
    let prevented = false;
    onEscape({ key: "Escape", preventDefault() { prevented = true; } });
    assert.equal(prevented, true);
    assert.equal(visible.recipeModal, false);
  } finally {
    for (const key of Object.keys(original)) {
      if (original[key] === undefined) delete globalThis[key];
      else globalThis[key] = original[key];
    }
  }
});

test("success favorite action uses existing saved favorites and matches modal button state", () => {
  const read = path => fs.readFileSync(new URL("../" + path, import.meta.url), "utf8");
  const html = read("index.html");
  const main = read("js/main.js");
  const cooking = read("js/features/cooking.js");
  const css = read("css/styles.css");
  const pkg = JSON.parse(read("package.json"));
  assert.match(html, /id="successFavoriteBtn" aria-pressed="false"/);
  assert.match(main, /getElementById\("successFavoriteBtn"\)\.addEventListener\("click"/);
  assert.match(main, /toggleFavorite\(state\.currentRecipe\.id\);\s*updateCookSuccessFavorite\(\)/);
  assert.match(cooking, /isFavorite\(state\.currentRecipe\.id\)/);
  assert.match(cooking, /updateCookSuccessFavorite\(\);/);
  assert.match(css, /\.success-favorite-btn\{grid-column:1\/-1/);
  assert.equal(pkg.dependencies["@capacitor/app"], "8.0.0");
});

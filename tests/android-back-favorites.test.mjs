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

test("Android Back at home requires confirmation; cancel never exits", () => {
  const original = {
    document: globalThis.document,
    window: globalThis.window,
    location: globalThis.location
  };
  const calls = [];
  const createNode = () => {
    const classes = new Set();
    const handlers = {};
    return {
      classList: {
        add(key) { classes.add(key); },
        remove(key) { classes.delete(key); },
        contains(key) { return classes.has(key); }
      },
      attributes: {},
      setAttribute(key, value) { this.attributes[key] = value; },
      addEventListener(key, cb) { handlers[key] = cb; },
      click() { handlers.click?.({ target: this }); },
      focus() { globalThis.document.activeElement = this; }
    };
  };
  const nodes = Object.fromEntries(
    ["cookSuccess", "cookOverlay", "recipeModal", "exitConfirm", "exitStayBtn", "exitYesBtn"]
      .map(id => [id, createNode()])
  );
  for (const name of ["cookSuccess", "cookOverlay", "recipeModal"]) nodes[name].classList.add("open");
  let onBack;
  let onKeydown;
  const app = {
    addListener(event, cb) { assert.equal(event, "backButton"); onBack = cb; return Promise.resolve(); },
    exitApp() { calls.push("exit"); }
  };
  const startingFocus = createNode();
  globalThis.document = {
    activeElement: startingFocus,
    getElementById(id) { return nodes[id] || null; },
    addEventListener(event, cb) { if (event === "keydown") onKeydown = cb; }
  };
  globalThis.window = { Capacitor: { Plugins: { App: app } } };
  globalThis.location = { hash: "#recipes" };

  try {
    initBackNavigation({
      closeCookSuccess: ({ returnToRecipe }) => {
        assert.equal(returnToRecipe, true);
        nodes.cookSuccess.classList.remove("open");
        nodes.cookOverlay.classList.remove("open");
        calls.push("success");
      },
      closeCookMode: () => { nodes.cookOverlay.classList.remove("open"); calls.push("cook"); },
      closeRecipe: () => { nodes.recipeModal.classList.remove("open"); calls.push("recipe"); },
      goTo: route => { calls.push(route); globalThis.location.hash = "#" + route; }
    });
    assert.equal(typeof onBack, "function");
    onBack(); // success -> recipe
    nodes.cookOverlay.classList.add("open");
    onBack(); // cook -> recipe
    onBack(); // recipe -> catalog
    onBack(); // catalog -> home
    assert.deepEqual(calls, ["success", "cook", "recipe", "home"]);

    onBack(); // home -> confirmation, NOT exit
    assert.deepEqual(calls, ["success", "cook", "recipe", "home"]);
    assert.equal(nodes.exitConfirm.classList.contains("open"), true);
    assert.equal(nodes.exitConfirm.attributes["aria-hidden"], "false");
    assert.equal(globalThis.document.activeElement, nodes.exitStayBtn);

    onBack(); // pressing native Back again just dismisses the prompt
    assert.equal(nodes.exitConfirm.classList.contains("open"), false);
    assert.equal(nodes.exitConfirm.attributes["aria-hidden"], "true");
    assert.equal(globalThis.document.activeElement, startingFocus);

    onBack();
    nodes.exitStayBtn.click(); // "Stay" keeps the app open
    assert.equal(nodes.exitConfirm.classList.contains("open"), false);
    assert.equal(calls.includes("exit"), false);

    onBack();
    let prevented = false;
    onKeydown({ key: "Escape", preventDefault() { prevented = true; } });
    assert.equal(prevented, true);
    assert.equal(nodes.exitConfirm.classList.contains("open"), false);

    onBack();
    nodes.exitYesBtn.click(); // Exit only on explicit confirmation
    assert.deepEqual(calls, ["success", "cook", "recipe", "home", "exit"]);
    assert.equal(nodes.exitConfirm.classList.contains("open"), false);

    // The exit dialog cannot consume back presses intended for recipe details.
    nodes.recipeModal.classList.add("open");
    prevented = false;
    onKeydown({ key: "Escape", preventDefault() { prevented = true; } });
    assert.equal(prevented, true);
    assert.equal(nodes.recipeModal.classList.contains("open"), false);
  } finally {
    for (const key of Object.keys(original)) {
      if (original[key] === undefined) delete globalThis[key];
      else globalThis[key] = original[key];
    }
  }
});

test("exit confirmation has accessible question and explicit stay/exit buttons", () => {
  const read = path => fs.readFileSync(new URL("../" + path, import.meta.url), "utf8");
  const html = read("index.html");
  const styles = read("css/styles.css");
  const main = read("js/main.js");
  assert.match(html, /id="exitConfirm" aria-hidden="true"/);
  assert.match(html, /role="alertdialog" aria-modal="true"/);
  assert.match(html, /Вы действительно хотите выйти\?/);
  assert.match(html, /id="exitStayBtn"/);
  assert.match(html, /id="exitYesBtn"/);
  assert.match(styles, /\.exit-confirm\.open\{display:flex\}/);
  assert.match(main, /android-back\.js\?v=20261012-exitconfirm1/);
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

import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";

test("web branch has no native packaging", () => {
  const root = new URL("../", import.meta.url);
  for (const path of ["capacitor.config.json","package.json","ANDROID-BUILD.md","js/core/android-back.js"]) {
    assert.equal(fs.existsSync(new URL(path, root)), false, path);
  }
  for (const path of ["index.html","js/main.js","js/core/api.js","js/core/offline-catalog.js"]) {
    assert.doesNotMatch(fs.readFileSync(new URL(path, root), "utf8"), /Capacitor|@capacitor\/|AndroidManifest/);
  }
  const html = fs.readFileSync(new URL("index.html", root), "utf8");
  assert.match(html, /js\/main\.js/);
  assert.doesNotMatch(html, /exitConfirm/);
  for (const path of ["data/recipes.json","privacy.html","data-rights.html","terms.html"]) {
    assert.equal(fs.existsSync(new URL(path, root)), true);
  }
});

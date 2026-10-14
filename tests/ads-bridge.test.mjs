import assert from "node:assert/strict";
import { test } from "node:test";
import { resolveAdsBridge } from "../js/features/ads.js";

test("Android native bridge works without a JavaScript plugin proxy", async () => {
  const requests = [];
  const listeners = [];
  const cap = {
    isNativePlatform: () => true,
    Plugins: {},
    nativePromise: (name, method, options) => {
      requests.push({ name, method, options });
      return Promise.resolve({ ok: true });
    },
    addListener: (name, event, callback) => {
      listeners.push({ name, event, callback });
      return { remove() {} };
    }
  };
  const ad = resolveAdsBridge(cap);
  assert.ok(ad, "Native Capacitor methods must be sufficient");
  assert.deepEqual(await ad.load({ width: 320 }), { ok: true });
  await ad.position({ visible: false });
  await ad.getStatus();
  await ad.checkDemo();
  await ad.addListener("loaded", () => {});
  assert.deepEqual(requests.map(r => [r.name, r.method]), [
    ["DoshikAds", "load"],
    ["DoshikAds", "position"],
    ["DoshikAds", "getStatus"],
    ["DoshikAds", "checkDemo"]
  ]);
  assert.equal(listeners[0].event, "loaded");
  assert.equal(listeners[0].name, "DoshikAds");
});

test("Preferred registered Capacitor JS plugin remains supported", () => {
  const existing = { load() {} };
  const ad = resolveAdsBridge({
    isNativePlatform: () => true,
    Plugins: { DoshikAds: existing }
  });
  assert.equal(ad, existing);
});

test("Web browser does not request native ads", () => {
  assert.equal(resolveAdsBridge({ isNativePlatform: () => false }), null);
  assert.equal(resolveAdsBridge(null), null);
});

import { isSlotFullyVisible } from "../js/features/ads.js";

test("native inline ad is visible only wholly inside the safe viewport", () => {
  const rect = { left: 12, right: 332, top: 280, bottom: 380, width: 320, height: 100 };
  assert.equal(isSlotFullyVisible(rect, 360, 720, 180), true);
  assert.equal(isSlotFullyVisible({ ...rect, top: 160 }, 360, 720, 180), false);
  assert.equal(isSlotFullyVisible({ ...rect, bottom: 721 }, 360, 720, 180), false);
  assert.equal(isSlotFullyVisible({ ...rect, right: 368 }, 360, 720, 180), false);
  assert.equal(isSlotFullyVisible({ ...rect, height: 0 }, 360, 720, 180), false);
});

import { didCookingScroll } from "../js/features/ads.js";
import { readFileSync } from "node:fs";

test("tap without actual scroll does not hide the native banner", () => {
  assert.equal(didCookingScroll(200, 200), false);
  assert.equal(didCookingScroll(200, 200.4), false);
  assert.equal(didCookingScroll(200, 201), true);
  assert.equal(didCookingScroll(200, 170), true);
});

test("HTML body and legal pages disable selection of noneditable text", () => {
  for (const stylesheet of ["css/styles.css", "css/legal.css"]) {
    const css = readFileSync(new URL("../" + stylesheet, import.meta.url), "utf8");
    assert.match(css, /body \*::after/);
    assert.match(css, /-webkit-user-select:none!important/);
    assert.match(css, /-webkit-touch-callout:none!important/);
  }
  const adJS = readFileSync(new URL("../js/features/ads.js", import.meta.url), "utf8");
  assert.doesNotMatch(adJS, /addEventListener\(["']touchstart["'],\s*hideWhileMoving/);
  assert.doesNotMatch(adJS, /addEventListener\(["']wheel["'],\s*hideWhileMoving/);
});

test("Banner width request leaves room for rounding, using true CSS-to-dp dimensions", () => {
  const js = readFileSync(new URL("../js/features/ads.js", import.meta.url), "utf8");
  const native = readFileSync(new URL("../native/android/DoshikAdsPlugin.java", import.meta.url), "utf8");
  assert.match(js, /innerWidth - 10/);
  assert.match(js, /viewportWidth: window\.innerWidth/);
  assert.match(native, /pxPerCss \/ metrics\.density/);
  assert.match(native, /result\.put\("hideReason", hideReason\)/);
  assert.match(native, /result\.put\("width", size\.getWidth\(\)\)/);
});

import { visibleAdIntersection } from "../js/features/ads.js";

test("ad scroll clipping exposes partial top and bottom, not only fully visible ads", () => {
  const base = { left: 20, right: 320, width: 300, height: 100, top: 180, bottom: 280 };
  assert.deepEqual(visibleAdIntersection(base, 360, 640, 200),
    { left: 20, right: 320, top: 200, bottom: 280, width: 300, height: 80 });
  const bottom = { ...base, top: 590, bottom: 690 };
  assert.deepEqual(visibleAdIntersection(bottom, 360, 640, 200),
    { left: 20, right: 320, top: 590, bottom: 640, width: 300, height: 50 });
  assert.equal(visibleAdIntersection({ ...base, top: 650, bottom: 750 }, 360, 640, 200), null);
  assert.equal(visibleAdIntersection({ ...base, top: 100, bottom: 199 }, 360, 640, 200), null);
});

test("Android banner host clips the creative instead of hiding it on partial scroll", () => {
  const native = readFileSync(new URL("../native/android/DoshikAdsPlugin.java", import.meta.url), "utf8");
  const js = readFileSync(new URL("../js/features/ads.js", import.meta.url), "utf8");
  assert.match(native, /bannerHost\.setClipChildren\(true\)/);
  assert.match(native, /banner\.setY\(\(float\) \(adY - clipTop\)\)/);
  assert.match(native, /lastVisibleHeightPx = Math\.max\(0, clipBottom - clipTop\)/);
  assert.doesNotMatch(js, /SCROLL_SETTLE_MS|hideWhileMoving|sendPosition\(false\).*scroll/);
});

test("optional ad layout observers do not block offline recipe startup", () => {
  const main = readFileSync(new URL("../js/main.js", import.meta.url), "utf8");
  const js = readFileSync(new URL("../js/features/ads.js", import.meta.url), "utf8");
  const dataStart = main.indexOf("const catalog = await loadCatalog();");
  const renderStart = main.indexOf("renderAll();", dataStart);
  const adsStart = main.indexOf("initCookingAds();", dataStart);
  assert.ok(dataStart > 0 && renderStart > dataStart && adsStart > renderStart,
    "Recipe data must render before optional ad integration starts");
  assert.match(main, /try \{\s+initCookingAds\(\);\s+\} catch/);
  assert.match(js, /if \(typeof ResizeObserver === "function"\)/);
  assert.doesNotMatch(js, /new ResizeObserver\(schedulePosition\)\.observe/);
});

test("native cooking ad tracks DOM position every frame without hiding on scroll", () => {
  const ad = readFileSync(new URL("../js/features/ads.js", import.meta.url), "utf8");
  const android = readFileSync(new URL("../native/android/DoshikAdsPlugin.java", import.meta.url), "utf8");
  assert.match(ad, /const shouldTrack = status === "loaded" && active\(\)/);
  assert.match(ad, /syncPosition\(\);\s+requestAnimationFrame\(frame\)/);
  assert.match(ad, /positionSamples\+\+/);
  assert.match(ad, /document\.addEventListener\("scroll"/);
  assert.doesNotMatch(ad, /hideWhileMoving|followScrollFrames|SCROLL_SETTLE_MS/);
  assert.match(android, /bannerHost\.setX\(lastHostX\)/);
  assert.match(android, /bannerHost\.setY\(lastHostY\)/);
  assert.match(android, /banner\.setY\(\(float\) \(adY - clipTop\)\)/);
});

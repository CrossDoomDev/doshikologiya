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

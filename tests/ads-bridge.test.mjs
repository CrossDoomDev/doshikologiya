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
  await ad.addListener("loaded", () => {});
  assert.deepEqual(requests.map(r => [r.name, r.method]), [
    ["DoshikAds", "load"],
    ["DoshikAds", "position"],
    ["DoshikAds", "getStatus"]
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

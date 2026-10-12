import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import {
  validSupportEmail, parseServiceContacts, readCachedServiceContacts,
  applyServiceContacts, fetchRemoteServiceContacts
} from "../js/core/remote-service-contacts.js";

function makeStorage() {
  const map = new Map();
  return {
    getItem: key => map.get(key) ?? null,
    setItem: (key, value) => map.set(key, value),
    map
  };
}

test("contact changes replace only public contact settings", () => {
  const original = { version: "2.7.0", featuredRecipeId: "x", supportEmail: "", donateUrl: "https://old.example/donate" };
  const remote = { supportEmail: "help@example.com", donateUrl: "https://new.example/donate", version: "999", featuredRecipeId: "changed" };
  const updated = applyServiceContacts(original, remote);
  assert.equal(updated.supportEmail, "help@example.com");
  assert.equal(updated.donateUrl, "https://new.example/donate");
  assert.equal(updated.version, "2.7.0");
  assert.equal(updated.featuredRecipeId, "x");
  assert.equal(original.supportEmail, "");
  assert.equal(validSupportEmail("support+doshi@gmail.com"), true);
  assert.equal(validSupportEmail("a@example.com?subject=hijack"), false);
  assert.throws(() => parseServiceContacts({ supportEmail: "not-an-email" }));
  assert.throws(() => parseServiceContacts({ donateUrl: "javascript:alert(1)" }));
  assert.throws(() => parseServiceContacts({ supportEmail: null }));
  assert.deepEqual(parseServiceContacts({ supportEmail: "" }), { supportEmail: "" });
});

test("remote contacts load with no-store, persist and remain available offline", async () => {
  const storage = makeStorage();
  let fetched = 0;
  const updated = await fetchRemoteServiceContacts(
    "https://crossdoomdev.github.io/doshikologiya/data/config.json",
    { storage, fetchImpl: async (url, options) => {
      fetched++;
      assert.equal(options.cache, "no-store");
      assert.ok(options.signal);
      assert.match(url, /^https:\/\//);
      return { ok: true, json: async () => ({
        supportEmail: "hello@example.com", donateUrl: "https://example.com/support", version: "ignored"
      }) };
    } }
  );
  assert.equal(fetched, 1);
  assert.deepEqual(updated, { supportEmail: "hello@example.com", donateUrl: "https://example.com/support" });
  assert.deepEqual(readCachedServiceContacts(storage), updated);
  const offlineConfig = applyServiceContacts({ supportEmail: "bundled@example.com", donateUrl: "" }, readCachedServiceContacts(storage));
  assert.equal(offlineConfig.supportEmail, "hello@example.com");
  await assert.rejects(fetchRemoteServiceContacts("https://example.com/config.json", {
    storage, fetchImpl: async () => { throw new Error("offline"); }
  }), /offline/);
  assert.deepEqual(readCachedServiceContacts(storage), updated);
});

test("invalid remote data and restricted storage do not corrupt cached contacts", async () => {
  const storage = makeStorage();
  storage.setItem("doshikologiya:service-contacts", JSON.stringify({ supportEmail: "old@example.com" }));
  await assert.rejects(fetchRemoteServiceContacts("https://example.com/config.json", {
    storage, fetchImpl: async () => ({ ok: true, json: async () => ({ supportEmail: "bad?header@example.com" }) })
  }), /Некорректная почта/);
  assert.equal(readCachedServiceContacts(storage).supportEmail, "old@example.com");
  const blocked = { getItem() { throw Error("blocked"); }, setItem() { throw Error("blocked"); } };
  assert.deepEqual(readCachedServiceContacts(blocked), {});
  const valid = await fetchRemoteServiceContacts("https://example.com/config.json", {
    storage: blocked, fetchImpl: async () => ({ ok: true, json: async () => ({ supportEmail: "valid@example.com" }) })
  });
  assert.equal(valid.supportEmail, "valid@example.com");
  await assert.rejects(fetchRemoteServiceContacts("http://example.com/config.json"), /безопасный адрес/);
});

test("Android starts from cached contact details and refreshes without updating recipes", () => {
  const main = fs.readFileSync(new URL("../js/main.js", import.meta.url), "utf8");
  const bundled = JSON.parse(fs.readFileSync(new URL("../data/config.json", import.meta.url), "utf8"));
  const index = fs.readFileSync(new URL("../index.html", import.meta.url), "utf8");
  assert.equal(bundled.remoteConfigUrl, "https://crossdoomdev.github.io/doshikologiya/data/config.json");
  assert.match(main, /readCachedServiceContacts\(\)/);
  assert.match(main, /fetchRemoteServiceContacts\(state\.config\.remoteConfigUrl\)/);
  assert.match(main, /refreshServiceContacts\(\)/);
  assert.match(main, /renderServiceInfo\(\)/);
  assert.match(index, /id="supportEmailLink"/);
});

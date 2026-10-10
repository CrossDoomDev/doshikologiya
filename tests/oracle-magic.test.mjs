import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";

const root = new URL("../", import.meta.url);
const file = relative => fs.readFileSync(new URL(relative, root), "utf8");

test("Оракул имеет три отдельные офлайн-иллюстрации", () => {
  for (const state of ["idle", "thinking", "success"]) {
    const art = file(`images/oracle/mage-${state}.svg`);
    assert.match(art, /^<svg[^>]+viewBox="0 0 320 320"/);
    assert.match(art, /<path /);
  }
  assert.notEqual(file("images/oracle/mage-idle.svg"), file("images/oracle/mage-success.svg"));
});

test("Переходы меняют рисунок и включают общий звук только после ответа", () => {
  const oracle = file("js/features/oracle.js");
  const cooking = file("js/features/cooking.js");
  assert.match(oracle, /setOracleLook\(machine, "thinking"\)/);
  assert.match(oracle, /setOracleLook\(machine, "success"\)/);
  assert.match(oracle, /playSuccessSound\(\)/);
  assert.match(cooking, /playSuccessSound\(\)/);
  assert.match(cooking, /unlockSuccessSound\(\)/);
  assert.match(oracle, /oracleBusy = false;\s*playSuccessSound\(\)/);
  assert.match(cooking, /success\.setAttribute\("aria-hidden", "false"\);\s*playSuccessSound\(\)/);
});

test("Звуковой сигнал генерируется без сети и использования медиафайлов", async () => {
  const { playSuccessSound } = await import("../js/features/success-sound.js");
  const events = [];
  const param = { setValueAtTime: () => {}, exponentialRampToValueAtTime: () => {} };
  const audio = {
    state: "running", currentTime: 0, destination: {},
    createGain() { return { gain: param, connect() {} }; },
    createOscillator() {
      return {
        frequency: param, connect() {},
        start(t) { events.push(["start", t]); },
        stop(t) { events.push(["stop", t]); }
      };
    }
  };
  globalThis.window = { AudioContext: class { constructor() { return audio; } } };
  try {
    playSuccessSound();
    assert.equal(events.filter(x => x[0] === "start").length, 8);
    assert.equal(events.filter(x => x[0] === "stop").length, 8);
  } finally { delete globalThis.window; }
});

test("Разметка и сборка Android подключают новые ресурсы", () => {
  const html = file("index.html");
  const main = file("js/main.js");
  const prep = file("scripts/prepare-android.mjs");
  const css = file("css/styles.css");
  assert.match(html, /id="oracleMascot" src="images\/oracle\/mage-idle\.svg"/);
  assert.match(html, /id="oraclePhaseLabel"/);
  assert.match(main, /oracle-mage1/);
  assert.match(prep, /"images"/);
  assert.match(prep, /"js"/);
  assert.match(css, /prefers-reduced-motion:reduce/);
});

import test from "node:test";
import assert from "node:assert/strict";
import { createCookingAdsController } from "../js/features/cooking-ads.js";

function fixture({ native = true, prepare } = {}) {
  const calls = { prepare: [], position: [], release: [] };
  const listeners = new Set();
  const frames = new Map();
  const timers = new Map();
  let next = 0;
  const events = new Map();
  const rect = { left: 36, top: 400, width: 288, height: 100, right: 324, bottom: 500 };
  const overlay = { classList: { contains: () => true },
    addEventListener: (name, fn) => events.set(name, fn), removeEventListener: name => events.delete(name) };
  const wrapper = { hidden: true };
  const slot = { style: {}, clientWidth: 288, getBoundingClientRect: () => rect };
  const nodes = { cookAd: wrapper, cookAdSlot: slot, cookOverlay: overlay,
    cookTop: { getBoundingClientRect: () => ({ bottom: 100 }) },
    cookSuccess: { classList: { contains: () => false } } };
  const plugin = {
    addListener: async (_, fn) => { listeners.add(fn); return { remove: async () => listeners.delete(fn) }; },
    prepare: async args => { calls.prepare.push(args); return prepare ? prepare(args) : { height: 100 }; },
    position: async args => { calls.position.push(args); },
    release: async args => { calls.release.push(args); },
  };
  const win = {
    Capacitor: { getPlatform: () => native ? "android" : "web", isPluginAvailable: () => native,
      Plugins: { CookingAds: plugin } },
    innerWidth: 360, innerHeight: 640, performance: { now: () => 1000 },
    requestAnimationFrame: fn => { frames.set(++next, fn); return next; },
    cancelAnimationFrame: id => frames.delete(id),
    setTimeout: fn => { timers.set(++next, fn); return next; }, clearTimeout: id => timers.delete(id),
  };
  const doc = { getElementById: id => nodes[id], hidden: false,
    addEventListener: (name, fn) => events.set(name, fn), removeEventListener: name => events.delete(name) };
  const controller = createCookingAdsController(win, doc);
  const frame = () => { const pending = [...frames.values()]; frames.clear(); pending.forEach(fn => fn()); };
  const emit = (state, session = calls.prepare.at(-1)?.session) => listeners.forEach(fn => fn({ state, session }));
  return { calls, rect, nodes, wrapper, win, doc, controller, frame, emit, timers, events, frames };
}

test("website does not initialize the mobile SDK or reserve an ad gap", async () => {
  const f = fixture({ native: false });
  await f.controller.open();
  assert.equal(f.calls.prepare.length, 0);
  assert.equal(f.wrapper.hidden, true);
});

test("ad follows the slot, stays below the header, and does not reload for a step", async () => {
  const f = fixture();
  await f.controller.open();
  assert.equal(f.calls.position.at(-1).visible, false);
  f.emit("loaded"); f.frame();
  assert.equal(f.calls.position.at(-1).visible, true);
  f.rect.top = 450; f.rect.bottom = 550; f.frame();
  assert.equal(f.calls.position.at(-1).y, 450);
  assert.equal(f.calls.prepare.length, 1);
  f.rect.top = 70; f.rect.bottom = 170; f.frame();
  assert.equal(f.calls.position.at(-1).visible, false);
  f.rect.top = 580; f.rect.bottom = 680; f.frame();
  assert.equal(f.calls.position.at(-1).visible, false);
  f.controller.close();
  assert.equal(f.wrapper.hidden, true);
  assert.equal(f.frames.size, 0);
  assert.equal(f.calls.release.length, 1);
});

test("scrolling and backgrounding hide the native view", async () => {
  const f = fixture(); await f.controller.open(); f.emit("loaded"); f.frame();
  f.events.get("scroll")(); f.frame();
  assert.equal(f.calls.position.at(-1).visible, false);
  f.win.performance.now = () => 1200; f.frame();
  assert.equal(f.calls.position.at(-1).visible, true);
  f.doc.hidden = true; f.events.get("visibilitychange")();
  assert.equal(f.calls.position.at(-1).visible, false);
  f.controller.close();
});

test("opening scale animation does not request a second ad", async () => {
  const f = fixture(); f.rect.width = 280;
  await f.controller.open(); f.emit("loaded"); f.frame();
  assert.equal(f.calls.prepare[0].width, 288);
  assert.equal(f.calls.position.at(-1).visible, false);
  f.rect.width = 288; f.frame();
  assert.equal(f.calls.position.at(-1).visible, true);
  assert.equal(f.calls.prepare.length, 1);
  assert.equal(f.timers.size, 0);
  f.controller.close();
});

test("no fill and timeout collapse the gap without retries", async () => {
  for (const failure of ["failed", "timeout"]) {
    const f = fixture(); await f.controller.open();
    if (failure === "failed") f.emit("failed");
    else [...f.timers.values()][0]();
    assert.equal(f.wrapper.hidden, true);
    assert.equal(f.calls.prepare.length, 1);
    assert.equal(f.frames.size, 0);
  }
});

test("closing before prepare resolves cannot resurrect the banner", async () => {
  let resolve;
  const f = fixture({ prepare: () => new Promise(done => { resolve = done; }) });
  const pending = f.controller.open(); await Promise.resolve();
  const oldSession = f.calls.prepare[0].session;
  f.controller.close();
  resolve({ height: 100 }); await pending;
  f.emit("loaded", oldSession);
  assert.equal(f.wrapper.hidden, true);
  assert.equal(f.calls.position.length, 0);
  assert.equal(f.frames.size, 0);
});

test("completion closes the ad; rotation requests a new size once", async () => {
  const f = fixture(); await f.controller.open(); f.emit("loaded"); f.frame();
  f.win.innerWidth = 640; f.frame(); f.frame();
  assert.equal(f.calls.position.at(-1).visible, false);
  assert.equal(f.timers.size, 1);
  [...f.timers.values()][0](); await Promise.resolve(); await Promise.resolve();
  assert.equal(f.calls.prepare.length, 2);
  f.emit("loaded"); f.frame();
  f.nodes.cookSuccess.classList.contains = () => true; f.frame();
  assert.equal(f.wrapper.hidden, true);
});

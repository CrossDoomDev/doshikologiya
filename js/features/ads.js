// Native-only ad in the existing hidden cooking placeholder.
// Browser/PWA remain ad-free; no HTML ad script is embedded.
const SLOT_ID = "cookingAdSlot";
let status = "idle";
let bannerHeight = 0;
let syncQueued = false;
let sentAt = 0;
let lastPosition = "";
// Capacitor injects nativePromise/addListener into Android WebView even when
// the unbundled JS page has not registered @capacitor/core's JS plugin proxy.
// This app has no JS bundler; do not assume Capacitor.Plugins.DoshikAds exists.
let bridgeOwner = null;
let bridgeProxy = null;
export function resolveAdsBridge(cap = globalThis.Capacitor) {
  if (cap?.isNativePlatform?.() !== true) return null;
  if (cap.Plugins?.DoshikAds) return cap.Plugins.DoshikAds;
  if (typeof cap.nativePromise !== "function" || typeof cap.addListener !== "function") {
    return null;
  }
  if (bridgeOwner !== cap || !bridgeProxy) {
    bridgeOwner = cap;
    bridgeProxy = {
      load: options => cap.nativePromise("DoshikAds", "load", options),
      position: options => cap.nativePromise("DoshikAds", "position", options),
      addListener: (event, callback) =>
        Promise.resolve(cap.addListener("DoshikAds", event, callback))
    };
  }
  return bridgeProxy;
}
function nativePlugin() { return resolveAdsBridge(); }
function active() {
  return document.getElementById("cookOverlay")?.classList.contains("open")
    && !document.getElementById("cookSuccess")?.classList.contains("open")
    && document.visibilityState !== "hidden";
}
function syncPosition(force = false) {
  const slot = document.getElementById(SLOT_ID);
  const plugin = nativePlugin();
  if (!slot || !plugin) return;
  const visible = status === "loaded" && active();
  if (!visible) {
    lastPosition = "";
    void plugin.position({ visible: false }).catch(() => {});
    return;
  }
  const rect = slot.getBoundingClientRect();
  const within = rect.top >= 0 && rect.bottom <= window.innerHeight
    && rect.left >= 0 && rect.right <= window.innerWidth;
  const details = {
    visible: within,
    left: rect.left, top: rect.top, viewportWidth: window.innerWidth
  };
  const hash = `${within}|${Math.round(rect.left)}|${Math.round(rect.top)}|${window.innerWidth}`;
  const now = Date.now();
  if (!force && (hash === lastPosition || now - sentAt < 90)) return;
  sentAt = now;
  lastPosition = hash;
  void plugin.position(details).catch(() => {});
}
function schedulePosition(force = false) {
  if (syncQueued) return;
  syncQueued = true;
  requestAnimationFrame(() => { syncQueued = false; syncPosition(force); });
}
function refresh() {
  const slot = document.getElementById(SLOT_ID);
  const plugin = nativePlugin();
  if (!slot || !plugin) return;
  if (active() && status === "idle") {
    status = "loading";
    const rect = slot.parentElement.getBoundingClientRect();
    void plugin.load({ width: Math.round(rect.width) }).catch(error => {
      console.warn("Не удалось инициализировать рекламный блок:", error);
      status = "failed";
      slot.hidden = true;
    });
  }
  const show = active() && status === "loaded";
  slot.hidden = !show;
  if (show) slot.style.height = `${bannerHeight}px`;
  schedulePosition(true);
}
export function initCookingAds() {
  const plugin = nativePlugin();
  const slot = document.querySelector(".cook-ad");
  if (!plugin || !slot) return;
  slot.id = SLOT_ID;
  slot.classList.add("android-ad-slot");
  slot.hidden = true;
  // Subscribe before requesting any banners, so a quick response isn't lost.
  void plugin.addListener("loaded", event => {
    bannerHeight = Math.max(50, Number(event.height) || 50);
    status = "loaded";
    refresh();
  });
  void plugin.addListener("failed", event => {
    console.warn("РСЯ не заполнила рекламное место:", event.message);
    status = "failed";
    slot.hidden = true;
    schedulePosition(true);
  });
  const overlay = document.getElementById("cookOverlay");
  const success = document.getElementById("cookSuccess");
  new MutationObserver(refresh).observe(overlay, { attributes: true, attributeFilter: ["class"] });
  new MutationObserver(refresh).observe(success, { attributes: true, attributeFilter: ["class"] });
  overlay.addEventListener("scroll", () => schedulePosition(), { passive: true, capture: true });
  window.addEventListener("resize", () => schedulePosition(true), { passive: true });
  document.addEventListener("visibilitychange", refresh);
  window.addEventListener("pagehide", () => {
    slot.hidden = true;
    void plugin.position({ visible: false }).catch(() => {});
  });
  refresh();
}

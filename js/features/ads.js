// Native-only ad in the existing hidden cooking placeholder.
// Browser/PWA remain ad-free; no HTML ad script is embedded.
const SLOT_ID = "cookingAdSlot";
let status = "idle";
let bannerHeight = 0;
let syncQueued = false;
let lastPosition = "";
let lastNativeVisible = false;
let positionSerial = 0;
let scrolling = false;
let scrollSettledTimer = null;
const SCROLL_SETTLE_MS = 300;
let diagnosticError = "";
let bridgeDiagnostics = "Не проверялся";
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
        Promise.resolve(cap.addListener("DoshikAds", event, callback)),
      getStatus: () => cap.nativePromise("DoshikAds", "getStatus", {}),
      checkDemo: () => cap.nativePromise("DoshikAds", "checkDemo", {})
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
// AdView cannot be inserted into HTML itself. While a user scrolls we hide the
// Android layer, and only restore it once the HTML slot has stopped moving.
// This prevents a delayed native position update from floating over content.
export function isSlotFullyVisible(rect, viewportWidth, viewportHeight, headerBottom = 0) {
  return !!rect && rect.width > 0 && rect.height > 0
    && rect.left >= 0 && rect.right <= viewportWidth
    && rect.top >= headerBottom + 8 && rect.bottom <= viewportHeight - 6;
}
function sendPosition(visible, rect = null, force = false) {
  const plugin = nativePlugin();
  if (!plugin) return;
  const key = visible && rect
    ? [Math.round(rect.left), Math.round(rect.top), Math.round(rect.width), Math.round(rect.height), window.innerWidth].join("|")
    : "hidden";
  if (!force && key === lastPosition && visible === lastNativeVisible) return;
  lastPosition = key;
  lastNativeVisible = visible;
  // Sequence is checked in Android, so a delayed "show" cannot override a hide.
  const params = visible && rect
    ? { visible: true, left: rect.left, top: rect.top,
        width: rect.width, height: rect.height,
        viewportWidth: window.innerWidth, sequence: ++positionSerial }
    : { visible: false, sequence: ++positionSerial };
  void plugin.position(params).catch(error => {
    diagnosticError = "Ошибка позиционирования: " + String(error?.message || error);
  });
}
function syncPosition() {
  const slot = document.getElementById(SLOT_ID);
  if (!slot || !nativePlugin()) return;
  if (status !== "loaded" || !active() || scrolling) {
    sendPosition(false);
    return;
  }
  const rect = slot.getBoundingClientRect();
  const headerBottom = document.getElementById("cookTop")?.getBoundingClientRect().bottom || 0;
  const fullyVisible = isSlotFullyVisible(rect, window.innerWidth, window.innerHeight, headerBottom);
  sendPosition(fullyVisible, fullyVisible ? rect : null);
}
function schedulePosition() {
  if (syncQueued) return;
  syncQueued = true;
  requestAnimationFrame(() => {
    syncQueued = false;
    syncPosition();
  });
}
function hideWhileMoving() {
  scrolling = true;
  // Hide *before* the DOM moves, not by following it frame-by-frame.
  sendPosition(false);
  if (scrollSettledTimer !== null) clearTimeout(scrollSettledTimer);
  scrollSettledTimer = setTimeout(() => {
    scrollSettledTimer = null;
    scrolling = false;
    schedulePosition();
  }, SCROLL_SETTLE_MS);
}
function refresh() {
  const slot = document.getElementById(SLOT_ID);
  const plugin = nativePlugin();
  if (!slot || !plugin) return;
  if (active() && status === "idle") {
    status = "loading";
    const parent = slot.parentElement;
    const style = getComputedStyle(parent);
    const parentWidth = parent.getBoundingClientRect().width;
    // Width of .cook-box includes padding; ad must fit the actual HTML slot.
    const innerWidth = parentWidth
      - parseFloat(style.paddingLeft || "0") - parseFloat(style.paddingRight || "0")
      - parseFloat(style.borderLeftWidth || "0") - parseFloat(style.borderRightWidth || "0");
    void plugin.load({ width: Math.max(120, Math.floor(innerWidth)) }).catch(error => {
      console.warn("Не удалось инициализировать рекламный блок:", error);
      status = "failed";
      diagnosticError = String(error?.message || error);
      slot.hidden = true;
    });
  }
  const show = active() && status === "loaded";
  slot.hidden = !show;
  if (show) slot.style.height = `${bannerHeight}px`;
  schedulePosition();
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
    diagnosticError = "";
    bannerHeight = Math.max(50, Number(event.height) || 50);
    status = "loaded";
    refresh();
  });
  void plugin.addListener("failed", event => {
    console.warn("РСЯ не заполнила рекламное место:", event.message);
    status = "failed";
    diagnosticError = String(event?.message || "SDK вернул ошибку без описания");
    slot.hidden = true;
    schedulePosition();
  });
  const overlay = document.getElementById("cookOverlay");
  const success = document.getElementById("cookSuccess");
  new MutationObserver(refresh).observe(overlay, { attributes: true, attributeFilter: ["class"] });
  new MutationObserver(refresh).observe(success, { attributes: true, attributeFilter: ["class"] });
  overlay.addEventListener("scroll", hideWhileMoving, { passive: true, capture: true });
  overlay.addEventListener("touchstart", hideWhileMoving, { passive: true, capture: true });
  overlay.addEventListener("wheel", hideWhileMoving, { passive: true, capture: true });
  window.addEventListener("resize", hideWhileMoving, { passive: true });
  document.addEventListener("visibilitychange", refresh);
  window.addEventListener("pagehide", () => {
    slot.hidden = true;
    sendPosition(false, null, true);
  });
  refresh();
}

export async function getAdsDiagnostics() {
  const native = globalThis.Capacitor?.isNativePlatform?.() === true;
  const plugin = nativePlugin();
  const overlay = document.getElementById("cookOverlay");
  const slot = document.getElementById(SLOT_ID);
  let nativeStatus = null;
  let nativeError = "";
  if (plugin?.getStatus) {
    try { nativeStatus = await plugin.getStatus(); }
    catch(error) { nativeError = String(error?.message || error); }
  } else if (native && plugin) {
    nativeError = "Нативный метод getStatus недоступен";
  }
  const rect = slot?.getBoundingClientRect();
  return [
    "Дошикология — проверка рекламы РСЯ",
    "Версия: 1.1.4",
    "Рабочий рекламный блок: R-M-20184115-1",
    "Активный ID SDK: " + (nativeStatus?.adUnit || "нет"),
    "Android: " + (native ? "да" : "нет"),
    "Capacitor nativePromise: " + (typeof globalThis.Capacitor?.nativePromise === "function" ? "есть" : "нет"),
    "Мост DoshikAds: " + (plugin ? "есть" : "НЕТ"),
    "Мост (при запуске): " + bridgeDiagnostics,
    "JS-состояние: " + status,
    "JS-ошибка: " + (diagnosticError || "нет"),
    "Режим готовки открыт: " + (overlay?.classList.contains("open") ? "да" : "нет"),
    "Место рекламы: " + (slot ? (slot.hidden ? "скрыто" : "показано") : "не найдено"),
    "Место на экране: " + (rect ? "Y=" + Math.round(rect.top) + ".." + Math.round(rect.bottom) : "—"),
    "Высота баннера (dp): " + bannerHeight,
    "Прокрутка в процессе: " + (scrolling ? "да" : "нет"),
    "Native layer: " + (lastNativeVisible ? "привязан к месту" : "скрыт"),
    "Нативный статус: " + (nativeStatus ? JSON.stringify(nativeStatus, null, 2) : "нет ответа"),
    "Ошибка вызова статуса: " + (nativeError || "нет")
  ].join("\n");
}

export function initAdsDiagnostics() {
  const panel = document.getElementById("adsDiagnosticsSection");
  if (!panel || globalThis.Capacitor?.isNativePlatform?.() !== true) return;
  panel.classList.remove("hidden");
  const output = document.getElementById("adsDiagnosticsReport");
  const refreshButton = document.getElementById("adsDiagnosticsRefreshBtn");
  const copyButton = document.getElementById("adsDiagnosticsCopyBtn");
  const demoButton = document.getElementById("adsDiagnosticsDemoBtn");
  refreshButton?.addEventListener("click", async () => {
    refreshButton.disabled = true;
    output.value = "Проверяем…";
    try { output.value = await getAdsDiagnostics(); }
    catch(error) { output.value = "Ошибка диагностики: " + String(error?.message || error); }
    finally { refreshButton.disabled = false; }
  });
  demoButton?.addEventListener("click", async () => {
    const plugin = nativePlugin();
    if (!plugin?.checkDemo) { output.value = "Метод проверки тестового баннера недоступен"; return; }
    demoButton.disabled = true;
    status = "loading";
    diagnosticError = "";
    output.value = "Отправляем тестовый запрос. Подожди несколько секунд, затем нажми «Проверить состояние».";
    try { await plugin.checkDemo(); }
    catch(error) { status = "failed"; diagnosticError = String(error?.message || error); }
    finally { demoButton.disabled = false; }
  });
  copyButton?.addEventListener("click", async () => {
    output.focus();
    output.select();
    try {
      if (navigator.clipboard?.writeText) await navigator.clipboard.writeText(output.value);
      else if (!document.execCommand("copy")) throw Error("Копирование недоступно");
      copyButton.textContent = "Скопировано ✓";
    } catch (_) {
      copyButton.textContent = "Выдели текст и скопируй";
    }
  });
}

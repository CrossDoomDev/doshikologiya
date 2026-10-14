// Native-only ad in the existing hidden cooking placeholder.
// Browser/PWA remain ad-free; no HTML ad script is embedded.
const SLOT_ID = "cookingAdSlot";
let status = "idle";
let bannerHeight = 0;
let bannerWidth = 0;
let syncQueued = false;
let lastPosition = "";
let lastNativeVisible = false;
let positionSerial = 0;
let scrolling = false;
let frameTrackingUntil = 0;
let lastLayoutNote = "not measured";
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
// Capacitor displays a separate Android BannerAdView over the HTML WebView.
// The HTML slot controls its coordinates. Partial visibility is handled with
// native viewport clipping, never by toggling the whole banner during scroll.
export function isSlotFullyVisible(rect, viewportWidth, viewportHeight, headerBottom = 0) {
  return !!rect && rect.width > 0 && rect.height > 0
    && rect.left >= 0 && rect.right <= viewportWidth
    && rect.top >= headerBottom + 8 && rect.bottom <= viewportHeight - 6;
}
export function visibleAdIntersection(rect, viewportWidth, viewportHeight, headerBottom = 0) {
  if (!rect || rect.width <= 0 || rect.height <= 0) return null;
  const left = Math.max(0, rect.left);
  const right = Math.min(viewportWidth, rect.right);
  const top = Math.max(0, headerBottom, rect.top);
  const bottom = Math.min(viewportHeight, rect.bottom);
  if (right <= left || bottom <= top) return null;
  return { left, right, top, bottom, width: right - left, height: bottom - top };
}
function sendPosition(visible, rect = null, clipTop = 0, force = false) {
  const plugin = nativePlugin();
  if (!plugin) return;
  const key = visible && rect
    ? [Math.round(rect.left), Math.round(rect.top), Math.round(rect.width),
       Math.round(rect.height), Math.round(clipTop), window.innerWidth, window.innerHeight].join("|")
    : "hidden";
  if (!force && key === lastPosition && visible === lastNativeVisible) return;
  lastPosition = key;
  lastNativeVisible = visible;
  const params = visible && rect
    ? { visible: true, left: rect.left, top: rect.top, width: rect.width,
        height: rect.height, clipTop, viewportWidth: window.innerWidth,
        viewportHeight: window.innerHeight, sequence: ++positionSerial }
    : { visible: false, sequence: ++positionSerial };
  void plugin.position(params).catch(error => {
    diagnosticError = "Ошибка позиционирования: " + String(error?.message || error);
  });
}
function syncPosition() {
  const slot = document.getElementById(SLOT_ID);
  if (!slot || !nativePlugin()) return;
  if (status !== "loaded" || !active()) {
    sendPosition(false);
    return;
  }
  const rect = slot.getBoundingClientRect();
  const headerBottom = document.getElementById("cookTop")?.getBoundingClientRect().bottom || 0;
  const visible = visibleAdIntersection(rect, window.innerWidth, window.innerHeight, headerBottom);
  lastLayoutNote = [
    "slot=" + Math.round(rect.width) + "x" + Math.round(rect.height),
    "banner=" + Math.round(bannerWidth) + "x" + Math.round(bannerHeight),
    "slotY=" + Math.round(rect.top) + ".." + Math.round(rect.bottom),
    "headerBottom=" + Math.round(headerBottom),
    "viewport=" + window.innerWidth + "x" + window.innerHeight,
    "visibleHeight=" + Math.round(visible?.height || 0)
  ].join(" ");
  sendPosition(Boolean(visible), visible ? rect : null, headerBottom);
}
function schedulePosition() {
  if (syncQueued) return;
  syncQueued = true;
  requestAnimationFrame(() => {
    syncQueued = false;
    syncPosition();
  });
}
export function didCookingScroll(before, after) {
  return Number.isFinite(before) && Number.isFinite(after) && Math.abs(after - before) >= 1;
}
// During kinetic scrolling, run visual updates on animation frames as well as
// scroll events, allowing the Android view to track intermediate coordinates.
function followScrollFrames() {
  frameTrackingUntil = performance.now() + 180;
  if (scrolling) return;
  scrolling = true;
  const tick = () => {
    schedulePosition();
    if (performance.now() < frameTrackingUntil && active()) {
      requestAnimationFrame(tick);
    } else {
      scrolling = false;
      schedulePosition();
    }
  };
  requestAnimationFrame(tick);
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
    // Keep a small sizing margin to avoid dp/CSS rounding making a banner
    // 1-2px wider than its slot. Android converts CSS pixels to dp explicitly.
    const requestedWidth = Math.max(120, Math.floor(innerWidth - 10));
    void plugin.load({ width: requestedWidth, viewportWidth: window.innerWidth }).catch(error => {
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
    bannerWidth = Math.max(0, Number(event.width) || 0);
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
  // touchstart fires for taps and navigation buttons: do not hide the ad.
  // wheel also fires without movement; the scroll event is authoritative.
  lastOverlayScrollTop = overlay.scrollTop;
  overlay.addEventListener("scroll", () => {
    const nextScrollTop = overlay.scrollTop;
    if (didCookingScroll(lastOverlayScrollTop, nextScrollTop)) {
      schedulePosition();
      followScrollFrames();
    }
    lastOverlayScrollTop = nextScrollTop;
  }, { passive: true });
  window.addEventListener("resize", schedulePosition, { passive: true });
  window.visualViewport?.addEventListener("resize", schedulePosition, { passive: true });
  new ResizeObserver(schedulePosition).observe(slot);
  new ResizeObserver(schedulePosition).observe(document.getElementById("cookTop"));
  document.addEventListener("visibilitychange", refresh);
  window.addEventListener("pagehide", () => {
    slot.hidden = true;
    sendPosition(false, null, 0, true);
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
    "Версия: 1.1.7",
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
    "Размер баннера (dp): " + bannerWidth + "x" + bannerHeight,
    "Проверка геометрии: " + lastLayoutNote,
    "Отслеживание прокрутки: " + (scrolling ? "да" : "нет"),
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

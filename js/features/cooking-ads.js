// The Android view is anchored to this DOM slot; no advertising script runs in the website.
export function createCookingAdsController(win, doc) {
  let active = null;
  let serial = 0;

  function close() {
    const current = active;
    active = null;
    if (!current) return;
    win.cancelAnimationFrame(current.frame);
    win.clearTimeout(current.timeout);
    win.clearTimeout(current.resizeTimer);
    current.overlay.removeEventListener("scroll", current.onScroll);
    doc.removeEventListener("visibilitychange", current.onVisibility);
    current.listener?.remove().catch(() => {});
    current.wrapper.hidden = true;
    current.wrapper.classList.remove("is-loading");
    current.plugin.release({ session: current.id }).catch(() => {});
  }

  async function open() {
    close();
    const capacitor = win.Capacitor;
    if (capacitor?.getPlatform?.() !== "android" || !capacitor.isPluginAvailable?.("CookingAds")) return;
    const wrapper = doc.getElementById("cookAd");
    const slot = doc.getElementById("cookAdSlot");
    const overlay = doc.getElementById("cookOverlay");
    if (!wrapper || !slot || !overlay) return;
    const plugin = capacitor.Plugins?.CookingAds || capacitor.registerPlugin("CookingAds");
    const current = {
      id: `${Date.now()}-${++serial}`, plugin, wrapper, slot, overlay,
      frame: 0, timeout: 0, resizeTimer: 0, listener: null, loaded: false,
      lastPosition: "", scrollingUntil: 0, width: 0, viewportWidth: 0,
    };
    active = current;
    current.onScroll = () => { current.scrollingUntil = win.performance.now() + 120; };
    current.onVisibility = () => { current.lastPosition = ""; sendPosition(current, false); };
    overlay.addEventListener("scroll", current.onScroll, { passive: true });
    doc.addEventListener("visibilitychange", current.onVisibility);
    wrapper.classList.add("is-loading");
    wrapper.hidden = false;
    slot.style.height = "100px";
    current.timeout = win.setTimeout(() => { if (active === current) close(); }, 15000);
    try {
      const listener = await plugin.addListener("state", event => {
        if (active !== current || event.session !== current.id) return;
        if (event.state === "failed") { close(); return; }
        if (event.state === "loaded") {
          current.loaded = true;
          current.wrapper.classList.remove("is-loading");
          win.clearTimeout(current.timeout);
        }
        current.lastPosition = "";
      });
      if (active !== current) { await listener.remove(); return; }
      current.listener = listener;
      // clientWidth excludes the opening scale animation of .cook-box.
      current.width = slot.clientWidth;
      current.viewportWidth = win.innerWidth;
      const result = await plugin.prepare({
        session: current.id, width: current.width, viewportWidth: current.viewportWidth,
      });
      if (active !== current) { await plugin.release({ session: current.id }); return; }
      if (!Number.isFinite(result.height) || result.height <= 0 || result.height > 300) throw new Error("Invalid ad height");
      slot.style.height = `${result.height}px`;
      tick(current);
    } catch {
      if (active === current) close();
    }
  }

  function sendPosition(current, visible) {
    const rect = current.slot.getBoundingClientRect();
    const position = {
      session: current.id, x: rect.left, y: rect.top,
      width: rect.width, height: rect.height, viewportWidth: win.innerWidth, visible,
    };
    const key = JSON.stringify(position);
    if (key === current.lastPosition) return;
    current.lastPosition = key;
    current.plugin.position(position).catch(() => { if (active === current) close(); });
  }

  function tick(current) {
    if (active !== current) return;
    if (!current.overlay.classList.contains("open") || doc.getElementById("cookSuccess")?.classList.contains("open")) {
      close();
      return;
    }
    const rect = current.slot.getBoundingClientRect();
    const widthChanged = Math.abs(current.slot.clientWidth - current.width) > 1 || win.innerWidth !== current.viewportWidth;
    if (widthChanged && !current.resizeTimer) {
      // Recalculate the SDK size after rotation, but never reload just for changing a step.
      current.resizeTimer = win.setTimeout(() => { if (active === current) void open(); }, 200);
    }
    const top = doc.getElementById("cookTop")?.getBoundingClientRect().bottom || 0;
    const viewport = win.visualViewport;
    const viewportTop = viewport?.offsetTop || 0;
    const viewportBottom = viewportTop + (viewport?.height || win.innerHeight);
    const visible = current.loaded && !doc.hidden && !widthChanged &&
      Math.abs(rect.width - current.slot.clientWidth) < 1 &&
      win.performance.now() >= current.scrollingUntil && (!viewport || viewport.scale === 1) &&
      rect.left >= 0 && rect.right <= win.innerWidth &&
      rect.top >= Math.max(top, viewportTop) && rect.bottom <= viewportBottom;
    sendPosition(current, visible);
    current.frame = win.requestAnimationFrame(() => tick(current));
  }

  return { open, close };
}

let controller;
export function openCookingAd() {
  controller ||= createCookingAdsController(window, document);
  return controller.open();
}
export function closeCookingAd() { controller?.close(); }

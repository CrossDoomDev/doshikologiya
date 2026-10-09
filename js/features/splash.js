const MIN_VISIBLE_MS = 760;
const MAX_VISIBLE_MS = 1250;

let startedAt = performance.now();
let hidden = false;
let fallbackTimer = null;

export function initSplash() {
  startedAt = performance.now();
  fallbackTimer = window.setTimeout(() => hideSplash({ force: true }), MAX_VISIBLE_MS);
}

export function hideSplash({ force = false } = {}) {
  if (hidden) return;

  const splash = document.getElementById("appSplash");
  if (!splash) {
    hidden = true;
    return;
  }

  const elapsed = performance.now() - startedAt;
  const delay = force ? 0 : Math.max(0, MIN_VISIBLE_MS - elapsed);

  window.setTimeout(() => {
    if (hidden) return;
    hidden = true;
    if (fallbackTimer) window.clearTimeout(fallbackTimer);

    splash.classList.add("is-hiding");
    splash.setAttribute("aria-hidden", "true");

    window.setTimeout(() => {
      splash.hidden = true;
    }, 320);
  }, delay);
}

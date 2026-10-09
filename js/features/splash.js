const MIN_VISIBLE_MS = 760;
// На быстром соединении заставка короткая; при медленном ждём данные чуть дольше.
const MAX_VISIBLE_MS = 4000;

let startedAt = null;
let hidden = false;
let fallbackTimer = null;
let hideTimer = null;

export function initSplash() {
  if (startedAt !== null) return;
  startedAt = performance.now();
  fallbackTimer = window.setTimeout(() => hideSplash({ force: true }), MAX_VISIBLE_MS);
}

export function hideSplash({ force = false } = {}) {
  if (hidden || hideTimer !== null) return;

  const splash = document.getElementById("appSplash");
  if (!splash) {
    hidden = true;
    return;
  }

  const elapsed = startedAt === null ? 0 : performance.now() - startedAt;
  const delay = force ? 0 : Math.max(0, MIN_VISIBLE_MS - elapsed);

  hideTimer = window.setTimeout(() => {
    hideTimer = null;
    if (hidden) return;
    hidden = true;
    if (fallbackTimer !== null) {
      window.clearTimeout(fallbackTimer);
      fallbackTimer = null;
    }

    splash.classList.add("is-hiding");
    splash.setAttribute("aria-hidden", "true");

    window.setTimeout(() => {
      splash.hidden = true;
    }, 320);
  }, delay);
}

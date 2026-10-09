const PRESSABLE_SELECTOR = [
  ".btn",
  ".nav-btn",
  ".chip",
  ".favorite-btn",
  ".sheet-close"
].join(",");

// Даже очень быстрое касание должно быть визуально заметно.
const MIN_PRESS_MS = 140;
let activePressable = null;
let activeInput = null;
let pressedAt = 0;
let releaseTimer = null;
let releaseTarget = null;

function clearPendingRelease() {
  if (releaseTimer !== null) {
    window.clearTimeout(releaseTimer);
    releaseTimer = null;
  }
  releaseTarget?.classList.remove("is-pressing");
  releaseTarget = null;
}

function release({ immediate = false } = {}) {
  if (!activePressable) return;
  const target = activePressable;
  activePressable = null;
  activeInput = null;

  clearPendingRelease();
  const elapsed = performance.now() - pressedAt;
  const remaining = immediate ? 0 : Math.max(0, MIN_PRESS_MS - elapsed);

  if (!remaining) {
    target.classList.remove("is-pressing");
    return;
  }

  releaseTarget = target;
  releaseTimer = window.setTimeout(() => {
    target.classList.remove("is-pressing");
    releaseTarget = null;
    releaseTimer = null;
  }, remaining);
}

function press(target, input) {
  const element = target?.closest?.(PRESSABLE_SELECTOR);
  if (!element || element.disabled || element.getAttribute("aria-disabled") === "true") return;

  release({ immediate: true });
  clearPendingRelease();
  activePressable = element;
  activeInput = input;
  pressedAt = performance.now();
  element.classList.add("is-pressing");
}

export function initPressFeedback() {
  document.addEventListener("pointerdown", event => {
    if (event.button !== undefined && event.button !== 0) return;
    press(event.target, event.pointerId);
  }, { passive: true });

  document.addEventListener("pointerup", event => {
    if (activeInput === event.pointerId) release();
  }, { passive: true });

  document.addEventListener("pointercancel", event => {
    if (activeInput === event.pointerId) release({ immediate: true });
  }, { passive: true });

  document.addEventListener("pointerleave", event => {
    if (event.pointerType === "mouse" && activeInput === event.pointerId) {
      release({ immediate: true });
    }
  }, { passive: true });

  // Клавиатура тоже получает визуальный отклик, а не только сенсорный экран.
  document.addEventListener("keydown", event => {
    if (event.repeat || !["Enter", " "].includes(event.key) || activePressable) return;
    press(event.target, "keyboard");
  });
  document.addEventListener("keyup", event => {
    if (["Enter", " "].includes(event.key) && activeInput === "keyboard") release();
  });

  document.addEventListener("scroll", () => release({ immediate: true }), { passive: true, capture: true });
  window.addEventListener("blur", () => {
    release({ immediate: true });
    clearPendingRelease();
  });
}

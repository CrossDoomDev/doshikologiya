const PRESSABLE_SELECTOR = [
  ".btn",
  ".nav-btn",
  ".chip",
  ".favorite-btn",
  ".sheet-close"
].join(",");

let activePressable = null;

function release() {
  if (!activePressable) return;
  activePressable.classList.remove("is-pressing");
  activePressable = null;
}

export function initPressFeedback() {
  document.addEventListener("pointerdown", event => {
    if (event.button !== undefined && event.button !== 0) return;
    const target = event.target.closest(PRESSABLE_SELECTOR);
    if (!target || target.disabled) return;

    release();
    activePressable = target;
    target.classList.add("is-pressing");
  }, { passive: true });

  document.addEventListener("pointerup", release, { passive: true });
  document.addEventListener("pointercancel", release, { passive: true });
  document.addEventListener("pointerleave", event => {
    if (activePressable && event.pointerType === "mouse") release();
  }, { passive: true });
  window.addEventListener("blur", release);
}

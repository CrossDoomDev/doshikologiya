import { state } from "../core/state.js";
import { closeRecipe } from "./recipe-modal.js";

let cookStepAnimating = false;
let resizeObserver = null;

export function initCookingLayout() {
  const top = document.getElementById("cookTop");
  if ("ResizeObserver" in window && top && !resizeObserver) {
    resizeObserver = new ResizeObserver(syncCookTopHeight);
    resizeObserver.observe(top);
  }
  window.addEventListener("resize", syncCookTopHeight, { passive: true });
}

export function openCookMode() {
  const recipe = state.currentRecipe;
  if (!recipe) return;

  state.cookStepIndex = 0;
  document.getElementById("cookTitle").textContent = recipe.title;
  document.getElementById("cookMetaSteps").textContent = `Шагов: ${recipe.steps.length}`;
  document.getElementById("cookMetaServing").textContent = `🍜 ${recipe.categories?.[0] || "Лабораторный протокол"}`;
  document.getElementById("cookPhotoLabel").textContent = `${recipe.ingredients.length} ингредиентов · ${recipe.steps.length} шагов`;

  const image = document.getElementById("cookImage");
  const backdrop = document.getElementById("cookBackdrop");

  if (recipe.image) {
    image.src = recipe.image;
    image.alt = recipe.title;
    backdrop.style.backgroundImage = `url('${recipe.image}')`;
  } else {
    image.removeAttribute("src");
    image.alt = "";
    backdrop.style.backgroundImage = "none";
  }

  const overlay = document.getElementById("cookOverlay");
  overlay.classList.add("open");
  overlay.setAttribute("aria-hidden", "false");
  renderCookStep();
  requestAnimationFrame(syncCookTopHeight);
}

export function syncCookTopHeight() {
  const overlay = document.getElementById("cookOverlay");
  const top = document.getElementById("cookTop");
  if (!overlay || !top) return;
  overlay.style.setProperty("--cook-top-height", `${Math.ceil(top.getBoundingClientRect().height)}px`);
}

export function closeCookMode() {
  const overlay = document.getElementById("cookOverlay");
  overlay.classList.remove("open");
  overlay.setAttribute("aria-hidden", "true");
}

function renderCookStep() {
  const recipe = state.currentRecipe;
  if (!recipe) return;

  const total = recipe.steps.length;
  document.getElementById("cookStepNumber").textContent = `Шаг ${state.cookStepIndex + 1} из ${total}`;
  document.getElementById("cookStepText").textContent = recipe.steps[state.cookStepIndex];
  document.getElementById("cookProgress").style.width = `${((state.cookStepIndex + 1) / total) * 100}%`;
  document.getElementById("cookPrevBtn").disabled = state.cookStepIndex === 0;
  document.getElementById("cookNextBtn").textContent = state.cookStepIndex === total - 1 ? "Готово ✓" : "Дальше →";
  document.getElementById("cookDots").innerHTML = recipe.steps
    .map((_, index) => `<span class="${index === state.cookStepIndex ? "active" : ""}" aria-hidden="true"></span>`)
    .join("");
}

export function navigateCookStep(direction) {
  const recipe = state.currentRecipe;
  if (!recipe || cookStepAnimating) return;

  const target = state.cookStepIndex + direction;
  if (target < 0) return;
  if (target >= recipe.steps.length) {
    if (direction > 0) showCookSuccess();
    return;
  }

  const panel = document.querySelector(".cook-main");
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  if (!panel || reduceMotion) {
    state.cookStepIndex = target;
    renderCookStep();
    return;
  }

  cookStepAnimating = true;
  const goingNext = direction > 0;
  const outFrames = goingNext
    ? [{ transform: "translateX(0) rotate(0deg) scale(1)", opacity: 1 }, { transform: "translateX(-18%) rotate(-2.2deg) scale(.985)", opacity: 0 }]
    : [{ transform: "translateX(0) rotate(0deg) scale(1)", opacity: 1 }, { transform: "translateX(18%) rotate(2.2deg) scale(.985)", opacity: 0 }];
  const inFrames = goingNext
    ? [{ transform: "translateX(18%) rotate(2.2deg) scale(.985)", opacity: 0 }, { transform: "translateX(0) rotate(0deg) scale(1)", opacity: 1 }]
    : [{ transform: "translateX(-18%) rotate(-2.2deg) scale(.985)", opacity: 0 }, { transform: "translateX(0) rotate(0deg) scale(1)", opacity: 1 }];

  const outAnim = panel.animate(outFrames, { duration: 170, easing: "cubic-bezier(.4,0,.7,.2)", fill: "forwards" });
  outAnim.onfinish = () => {
    state.cookStepIndex = target;
    renderCookStep();
    const inAnim = panel.animate(inFrames, { duration: 240, easing: "cubic-bezier(.16,.84,.32,1)", fill: "forwards" });
    inAnim.onfinish = () => {
      panel.style.transform = "";
      panel.style.opacity = "";
      cookStepAnimating = false;
    };
    inAnim.oncancel = () => { cookStepAnimating = false; };
  };
  outAnim.oncancel = () => { cookStepAnimating = false; };
}

function buildSuccessConfetti() {
  const box = document.getElementById("successConfetti");
  const palette = ["#ffb229", "#ff7a18", "#ffe086", "#ffffff", "#d85b10"];
  box.innerHTML = Array.from({ length: 28 }, (_, index) => {
    const x = 3 + ((index * 37) % 94);
    const color = palette[index % palette.length];
    const rotation = ((index * 47) % 180) - 90;
    const duration = (1.7 + (index % 7) * .12).toFixed(2) + "s";
    const delay = ((index % 9) * .035).toFixed(3) + "s";
    const drift = (((index * 53) % 140) - 70) + "px";
    return `<i style="--x:${x}%;--c:${color};--r:${rotation}deg;--d:${duration};--delay:${delay};--drift:${drift}"></i>`;
  }).join("");
}

function showCookSuccess() {
  const recipe = state.currentRecipe;
  if (!recipe) return;

  document.getElementById("successRecipeTitle").textContent = recipe.title;
  document.getElementById("successBadgeText").textContent = `${recipe.steps.length} из ${recipe.steps.length} этапов завершены. Протокол закрыт.`;
  buildSuccessConfetti();

  const success = document.getElementById("cookSuccess");
  success.classList.remove("open");
  void success.offsetWidth;
  success.classList.add("open");
  success.setAttribute("aria-hidden", "false");
}

export function closeCookSuccess({ returnToRecipe = false } = {}) {
  const success = document.getElementById("cookSuccess");
  success.classList.remove("open");
  success.setAttribute("aria-hidden", "true");
  closeCookMode();
  if (!returnToRecipe) closeRecipe();
}

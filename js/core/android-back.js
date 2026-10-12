// Handle Android's system Back action in the Capacitor shell. No browser bundler needed.
export function closeTopOverlay({ successOpen, cookOpen, recipeOpen }, { closeSuccess, closeCooking, closeRecipe }) {
  if (successOpen) {
    closeSuccess();
    return true;
  }
  if (cookOpen) {
    closeCooking();
    return true;
  }
  if (recipeOpen) {
    closeRecipe();
    return true;
  }
  return false;
}

export function initBackNavigation({ closeCookSuccess, closeCookMode, closeRecipe, goTo }) {
  const exitDialog = document.getElementById("exitConfirm");
  const stayButton = document.getElementById("exitStayBtn");
  const yesButton = document.getElementById("exitYesBtn");
  const app = window.Capacitor?.Plugins?.App;
  let priorFocus = null;

  const isExitPromptOpen = () => !!exitDialog?.classList.contains("open");

  const showExitPrompt = () => {
    if (!exitDialog || isExitPromptOpen()) return;
    priorFocus = document.activeElement;
    exitDialog.classList.add("open");
    exitDialog.setAttribute("aria-hidden", "false");
    stayButton?.focus();
  };

  const hideExitPrompt = () => {
    if (!isExitPromptOpen()) return;
    exitDialog.classList.remove("open");
    exitDialog.setAttribute("aria-hidden", "true");
    priorFocus?.focus?.();
    priorFocus = null;
  };

  stayButton?.addEventListener("click", hideExitPrompt);
  yesButton?.addEventListener("click", () => {
    if (!isExitPromptOpen()) return;
    hideExitPrompt();
    app?.exitApp?.(); // Only explicit confirmation may exit Android.
  });
  exitDialog?.addEventListener("click", event => {
    if (event.target === exitDialog) hideExitPrompt();
  });

  const closeVisibleOverlay = () => closeTopOverlay({
    successOpen: document.getElementById("cookSuccess")?.classList.contains("open"),
    cookOpen: document.getElementById("cookOverlay")?.classList.contains("open"),
    recipeOpen: document.getElementById("recipeModal")?.classList.contains("open")
  }, {
    closeSuccess: () => closeCookSuccess({ returnToRecipe: true }),
    closeCooking: closeCookMode,
    closeRecipe
  });

  // Escape dismisses the highest dialog first, without closing the app.
  document.addEventListener("keydown", event => {
    if (isExitPromptOpen()) {
      if (event.key === "Escape") {
        event.preventDefault();
        hideExitPrompt();
      } else if (event.key === "Tab") {
        // Keep keyboard focus inside the modal dialog.
        const buttons = [stayButton, yesButton].filter(Boolean);
        const first = buttons[0], last = buttons[buttons.length - 1];
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }
      return;
    }
    if (event.key === "Escape" && closeVisibleOverlay()) event.preventDefault();
  });

  // The native Capacitor App plugin supplies Android's system Back event.
  if (!app?.addListener) return;

  Promise.resolve(app.addListener("backButton", () => {
    if (isExitPromptOpen()) {
      hideExitPrompt();
      return;
    }
    if (closeVisibleOverlay()) return;
    if (location.hash.replace("#", "") !== "home") {
      goTo("home");
    } else {
      showExitPrompt();
    }
  })).catch(error => console.warn("Android Back listener unavailable:", error));
}

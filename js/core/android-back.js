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
  const closeVisibleOverlay = () => closeTopOverlay({
    successOpen: document.getElementById("cookSuccess")?.classList.contains("open"),
    cookOpen: document.getElementById("cookOverlay")?.classList.contains("open"),
    recipeOpen: document.getElementById("recipeModal")?.classList.contains("open")
  }, {
    closeSuccess: () => closeCookSuccess({ returnToRecipe: true }),
    closeCooking: closeCookMode,
    closeRecipe
  });

  // Keyboard access and local browser testing follow the same navigation order.
  document.addEventListener("keydown", event => {
    if (event.key === "Escape" && closeVisibleOverlay()) event.preventDefault();
  });

  // Native bridge exposes installed plugins to vanilla JS inside Capacitor WebView.
  const app = window.Capacitor?.Plugins?.App;
  if (!app?.addListener) return;

  Promise.resolve(app.addListener("backButton", () => {
    if (closeVisibleOverlay()) return;
    if (location.hash.replace("#", "") !== "home") {
      goTo("home");
    } else {
      // At the root screen, retain standard Android Back-to-exit behavior.
      app.exitApp?.();
    }
  })).catch(error => console.warn("Android Back listener unavailable:", error));
}

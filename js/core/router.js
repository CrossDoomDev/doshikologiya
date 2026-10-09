const ROUTES = Object.freeze(["home", "recipes", "oracle", "wall"]);

export function goTo(route) {
  const target = ROUTES.includes(route) ? route : "home";

  ROUTES.forEach(name => {
    document.getElementById(`view-${name}`)?.classList.toggle("hidden", name !== target);
  });

  document.querySelector(".topbar")?.classList.toggle("logo-hidden", target !== "home");
  document.querySelectorAll("[data-route]").forEach(element => {
    element.classList.toggle("active", element.dataset.route === target && element.classList.contains("nav-btn"));
  });

  history.replaceState(null, "", `#${target}`);
  window.scrollTo({ top: 0, behavior: "smooth" });
}

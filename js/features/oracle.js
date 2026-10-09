import { ORACLE_REASONS } from "../core/config.js";
import { state } from "../core/state.js";
import { escapeHtml } from "../core/utils.js";

let lastOracleRecipeId = null;
let oracleBusy = false;

export function renderDailyIndex() {
  const date = new Date();
  const seed = Number(`${date.getFullYear()}${date.getMonth() + 1}${date.getDate()}`);
  const index = 55 + (seed * 17) % 41;
  const forecasts = [
    "Сегодня допустимы импровизации, но третий пакетик приправы лаборатория официально не одобряет.",
    "Вероятность удачного эксперимента повышается после добавления яйца.",
    "День благоприятен для дешёвых решений, которые выглядят неожиданно дорого.",
    "Лаборатория рекомендует не спорить с человеком, у которого в руках кипяток."
  ];

  document.getElementById("dailyIndex").textContent = index;
  document.getElementById("dailyForecast").textContent = forecasts[seed % forecasts.length];
}

export function runOracle() {
  if (oracleBusy || !state.recipes.length) return;

  const machine = document.getElementById("oracleMachine");
  const output = document.getElementById("oracleResult");
  const button = document.getElementById("oracleBtn");

  oracleBusy = true;
  machine.classList.add("consulting");
  button.textContent = "Оракул заглядывает в судьбу…";
  output.textContent = "Секунду… Сверяем положение звёзд, уровень кипятка и содержимое архива.";

  window.setTimeout(() => {
    const pool = state.recipes.length > 1
      ? state.recipes.filter(recipe => recipe.id !== lastOracleRecipeId)
      : state.recipes;
    const recipe = pool[Math.floor(Math.random() * pool.length)];
    const reason = ORACLE_REASONS[Math.floor(Math.random() * ORACLE_REASONS.length)];

    lastOracleRecipeId = recipe.id;
    output.innerHTML = `<div class="oracle-choice">
      <img class="oracle-choice-image" src="${escapeHtml(recipe.image || "")}" alt="${escapeHtml(recipe.title)}">
      <div class="oracle-choice-copy">
        <div class="oracle-choice-label">Оракул постановил</div>
        <h3 class="oracle-choice-title">${escapeHtml(recipe.title)}</h3>
        <p class="oracle-choice-reason">${escapeHtml(reason)}</p>
        <button class="btn dosh-action-btn" data-open-recipe="${escapeHtml(recipe.id)}"><span class="btn-emoji" aria-hidden="true">📖</span> Открыть протокол →</button>
      </div>
    </div>`;

    machine.classList.remove("consulting");
    button.textContent = "🔮 Спросить Оракула ещё раз";
    oracleBusy = false;
  }, 1050);
}

import { ORACLE_REASONS } from "../core/config.js";
import { state } from "../core/state.js";
import { escapeHtml } from "../core/utils.js";
import { unlockSuccessSound, playSuccessSound } from "./success-sound.js?v=20261010-oracle-mage1";

const ORACLE_ART = Object.freeze({
  idle: "images/oracle/mage-idle.svg",
  thinking: "images/oracle/mage-thinking.svg",
  success: "images/oracle/mage-success.svg"
});

function setOracleLook(machine, stage) {
  const mascot = document.getElementById("oracleMascot");
  const caption = document.getElementById("oraclePhaseLabel");
  if (mascot) mascot.src = ORACLE_ART[stage];
  if (caption) caption.textContent = {
    idle: "Магия лапши ждёт своего часа",
    thinking: "Лапша-маг творит предсказание…",
    success: "✨ Протокол найден!"
  }[stage];
  machine.classList.toggle("consulting", stage === "thinking");
  machine.classList.toggle("oracle-solved", stage === "success");
}

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

  // Разблокируем звук прямо во время касания, до 3-секундного ожидания.
  unlockSuccessSound();
  oracleBusy = true;
  setOracleLook(machine, "thinking");
  button.disabled = true;
  button.textContent = "📜 Оракул изучает хроники…";
  output.classList.remove("oracle-revealed");
  output.setAttribute("aria-busy", "true");
  output.textContent = "📜 Раскрываем запечатанные хроники Института…";

  window.setTimeout(() => {
    if (oracleBusy) output.textContent = "Сопоставляем древние пророчества с запасами лапши…";
  }, 1000);
  window.setTimeout(() => {
    if (oracleBusy) output.textContent = "Последняя проверка знаков судьбы…";
  }, 2000);

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

    setOracleLook(machine, "success");
    output.removeAttribute("aria-busy");
    output.classList.add("oracle-revealed");
    button.disabled = false;
    button.textContent = "🔮 Спросить Оракула ещё раз";
    oracleBusy = false;
    playSuccessSound();
  }, 3000);
}

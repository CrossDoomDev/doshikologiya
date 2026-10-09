function deterministicDaily(){
      const d=new Date(); const seed=Number(`${d.getFullYear()}${d.getMonth()+1}${d.getDate()}`);
      const idx=55+(seed*17)%41; document.getElementById("dailyIndex").textContent=idx;
      const forecasts=["Сегодня допустимы импровизации, но третий пакетик приправы лаборатория официально не одобряет.","Вероятность удачного эксперимента повышается после добавления яйца.","День благоприятен для дешёвых решений, которые выглядят неожиданно дорого.","Лаборатория рекомендует не спорить с человеком, у которого в руках кипяток."];
      document.getElementById("dailyForecast").textContent=forecasts[seed%forecasts.length];
    }

    let lastOracleRecipeId=null;
    let oracleBusy=false;
    function oracle(){
      if(oracleBusy || !appData.recipes.length)return;
      const machine=document.getElementById("oracleMachine");
      const out=document.getElementById("oracleResult");
      const button=document.getElementById("oracleBtn");
      oracleBusy=true;
      machine.classList.add("consulting");
      button.textContent="Оракул заглядывает в судьбу…";
      out.textContent="Секунду… Сверяем положение звёзд, уровень кипятка и содержимое архива.";

      window.setTimeout(()=>{
        const pool=appData.recipes.length>1
          ?appData.recipes.filter(recipe=>recipe.id!==lastOracleRecipeId)
          :appData.recipes;
        const recipe=pool[Math.floor(Math.random()*pool.length)];
        const reason=oracleReasons[Math.floor(Math.random()*oracleReasons.length)];
        lastOracleRecipeId=recipe.id;
        out.innerHTML=`<div class="oracle-choice">
          <img class="oracle-choice-image" src="${escapeHtml(recipe.image||"")}" alt="${escapeHtml(recipe.title)}">
          <div class="oracle-choice-copy">
            <div class="oracle-choice-label">Оракул постановил</div>
            <h3 class="oracle-choice-title">${escapeHtml(recipe.title)}</h3>
            <p class="oracle-choice-reason">${escapeHtml(reason)}</p>
            <button class="btn dosh-action-btn" data-open-recipe="${escapeHtml(recipe.id)}"><span class="btn-emoji" aria-hidden="true">📖</span> Открыть протокол →</button>
          </div>
        </div>`;
        bindDynamicButtons();
        machine.classList.remove("consulting");
        button.textContent="🔮 Спросить Оракула ещё раз";
        oracleBusy=false;
      },1050);
    }

    const routes=["home","recipes","oracle","wall"];

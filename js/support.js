function formatPatronDate(value){
      const raw=String(value||"").trim();
      const relative={"сегодня":"07.10.2026","вчера":"06.10.2026","2 дня назад":"05.10.2026","3 дня назад":"04.10.2026","неделю назад":"30.09.2026"};
      return relative[raw.toLowerCase()]||raw;
    }

    function renderComments(){
      const box=document.getElementById("commentsList");
      if(!appData.comments?.length){box.innerHTML='<div class="empty-state">Стена пока пуста. Первый меценат получает почётное право сказать: «Я был здесь до хайпа».</div>';return;}
      const icons=["🍜","🥚","🔥","🥢","🧄"];
      box.innerHTML=appData.comments.map((c,i)=>`<article class="comment"><div class="comment-top"><div class="donor"><div class="avatar">${icons[i%icons.length]}</div><div><div>${escapeHtml(c.name)}</div><small style="color:var(--muted)">${escapeHtml(formatPatronDate(c.date))}</small></div></div><div class="amount">${escapeHtml(c.amount||"")}</div></div><p>${escapeHtml(c.text)}</p></article>`).join("");
    }

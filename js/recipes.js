function recipeCard(recipe){
      const cat=getCategories(recipe)[0];
      return `<article class="recipe-card">
        <div class="recipe-media">
          <img class="recipe-image" src="${escapeHtml(recipe.image||"")}" alt="${escapeHtml(recipe.title)}" loading="lazy">
          <button class="favorite-btn ${isFavorite(recipe.id)?"on":""}" data-favorite="${escapeHtml(recipe.id)}" aria-label="${isFavorite(recipe.id)?"Убрать из избранного":"Добавить в избранное"}" aria-pressed="${isFavorite(recipe.id)?"true":"false"}"><svg class="favorite-heart" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 20.6C10.65 19.55 3.55 15.05 2.55 9.65C1.95 6.4 4.05 3.55 7.25 3.55C9.35 3.55 11.05 4.7 12 6.25C12.95 4.7 14.65 3.55 16.75 3.55C19.95 3.55 22.05 6.4 21.45 9.65C20.45 15.05 13.35 19.55 12 20.6Z"/></svg></button>
          <span class="card-category">${escapeHtml(cat)}</span>
        </div>
        <div class="recipe-body">
          <h3>${escapeHtml(recipe.title)}</h3>
          <p>${escapeHtml(recipe.description)}</p>
          <div class="meta"><span class="tag">⏱ ${escapeHtml(recipe.time)}</span><span class="tag">🔥 ${escapeHtml(recipe.difficulty)}</span></div>
          <button class="btn dosh-action-btn card-open" data-open-recipe="${escapeHtml(recipe.id)}"><span class="btn-emoji" aria-hidden="true">📖</span> Открыть протокол</button>
        </div>
      </article>`;
    }

    function filteredRecipes(){
      const q=currentSearch.trim().toLowerCase();
      return appData.recipes.filter(recipe=>{
        const byCategory=currentCategory==="Все"||getCategories(recipe).includes(currentCategory);
        const hay=(recipe.title+" "+recipe.description+" "+getCategories(recipe).join(" ")).toLowerCase();
        const bySearch=!q||hay.includes(q);
        const byFavorite=!showFavoritesOnly||isFavorite(recipe.id);
        return byCategory&&bySearch&&byFavorite;
      });
    }

    function renderRecipes(){
      const home=appData.recipes.slice(0,6);
      document.getElementById("homeRecipes").innerHTML=home.map(recipeCard).join("");
      const filtered=filteredRecipes();
      document.getElementById("recipesGrid").innerHTML=filtered.length?filtered.map(recipeCard).join(""):'<div class="empty-state">Лаборатория ничего не нашла. Возможно, рецепт засекречен.</div>';
      bindDynamicButtons();
    }

    function renderChips(){
      const make=(target)=>{
        target.innerHTML=categoryOrder.map(cat=>`<button class="chip ${cat===currentCategory?"active":""}" data-category="${escapeHtml(cat)}">${escapeHtml(cat)}</button>`).join("");
      };
      make(document.getElementById("homeCategoryChips"));
      make(document.getElementById("recipeCategoryChips"));
      document.querySelectorAll("[data-category]").forEach(btn=>btn.onclick=()=>{
        currentCategory=btn.dataset.category;
        renderChips();renderRecipes();
        if(btn.closest("#homeCategoryChips")){goTo("recipes");}
      });
    }

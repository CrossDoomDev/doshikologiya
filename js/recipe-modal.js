function bindDynamicButtons(){
      document.querySelectorAll("[data-open-recipe]").forEach(btn=>btn.onclick=()=>openRecipe(btn.dataset.openRecipe));
      document.querySelectorAll("[data-favorite]").forEach(btn=>btn.onclick=(e)=>{e.stopPropagation();toggleFavorite(btn.dataset.favorite);});
    }

    function toggleFavorite(id){
      if(isFavorite(id)) favorites.delete(id); else favorites.add(id);
      saveFavorites();renderRecipes();
      if(currentRecipe?.id===id) updateModalFavorite();
    }

    function openRecipe(id){
      const r=appData.recipes.find(x=>x.id===id); if(!r) return;
      currentRecipe=r;
      document.getElementById("modalPhoto").src=r.image||"";
      document.getElementById("modalPhoto").alt=r.title;
      document.getElementById("modalTitle").textContent=r.title;
      document.getElementById("modalDescription").textContent=r.description;
      document.getElementById("modalMeta").innerHTML=`<span class="tag">⏱ ${escapeHtml(r.time)}</span><span class="tag">🔥 ${escapeHtml(r.difficulty)}</span><span class="tag">💸 ${escapeHtml(r.cost)}</span>`;
      document.getElementById("modalIngredients").innerHTML=r.ingredients.map(x=>`<li>${escapeHtml(x)}</li>`).join("");
      document.getElementById("modalStory").textContent=r.story;
      updateModalFavorite();
      const modal=document.getElementById("recipeModal");modal.classList.add("open");modal.setAttribute("aria-hidden","false");document.body.style.overflow="hidden";
    }
    function updateModalFavorite(){
      const b=document.getElementById("modalFavoriteBtn");
      if(!currentRecipe)return;
      const active=isFavorite(currentRecipe.id);
      b.innerHTML=active?'<span class="btn-emoji" aria-hidden="true">♥</span> В избранном':'<span class="btn-emoji" aria-hidden="true">♡</span> Добавить в избранное';
      b.classList.toggle("is-favorite",active);
    }
    function closeRecipe(){const modal=document.getElementById("recipeModal");modal.classList.remove("open");modal.setAttribute("aria-hidden","true");document.body.style.overflow="";}

    function openDonate(){const url=appData.donateUrl||"";if(url)window.open(url,"_blank","noopener,noreferrer");}

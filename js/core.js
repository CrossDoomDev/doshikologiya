function escapeHtml(value=""){
      return String(value).replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll('"',"&quot;").replaceAll("'","&#039;");
    }
    function saveFavorites(){localStorage.setItem("doshikologiya:favorites",JSON.stringify([...favorites]));}
    function getCategories(recipe){return Array.isArray(recipe.categories)&&recipe.categories.length?recipe.categories:["Эксперимент"];}
    function isFavorite(id){return favorites.has(id);}

    async function loadRemoteData(){
      try{
        const response=await fetch(REMOTE_DATA_URL,{cache:"no-store"});
        if(!response.ok) throw new Error("HTTP "+response.status);
        const remote=await response.json();
        appData={...fallbackData,...remote,banner:{...fallbackData.banner,...(remote.banner||{})},recipes:Array.isArray(remote.recipes)&&remote.recipes.length?remote.recipes:fallbackData.recipes,comments:Array.isArray(remote.comments)?remote.comments:fallbackData.comments};
        renderAll();
      }catch(error){console.warn("Удалённые данные недоступны, используется локальный архив:",error);renderAll();}
    }

    function dailyRecipe(){
      if(!appData.recipes.length) return null;
      const preferred=appData.recipes.find(r=>r.id===appData.featuredRecipeId);
      const days=Math.floor(Date.now()/86400000);
      return days%3===0 && preferred ? preferred : appData.recipes[days%appData.recipes.length];
    }

    function renderFeatured(){
      const recipe=dailyRecipe(); if(!recipe) return;
      document.getElementById("featuredTitle").textContent=recipe.title;
      document.getElementById("featuredDescription").textContent=recipe.description;
      const img=document.getElementById("featuredImage"); img.src=recipe.image||""; img.alt=recipe.title;
      document.getElementById("featuredMeta").innerHTML=`<span class="tag">⏱ ${escapeHtml(recipe.time)}</span><span class="tag">🔥 ${escapeHtml(recipe.difficulty)}</span><span class="tag">💸 ${escapeHtml(recipe.cost)}</span>`;
      document.getElementById("featuredOpenBtn").onclick=()=>openRecipe(recipe.id);
    }

    function renderBanner(){
      document.getElementById("bannerTitle").textContent=appData.banner?.title||fallbackData.banner.title;
      document.getElementById("bannerText").textContent=appData.banner?.text||fallbackData.banner.text;
      document.getElementById("appVersion").textContent=appData.version||"2.0";
    }

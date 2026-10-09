function renderAll(){renderBanner();renderFeatured();renderChips();renderRecipes();renderComments();deterministicDaily();}

    document.addEventListener("click",e=>{const route=e.target.closest("[data-route]");if(route){e.preventDefault();goTo(route.dataset.route);}});
    document.getElementById("recipeSearch").addEventListener("input",e=>{currentSearch=e.target.value;renderRecipes();});
    document.getElementById("favoritesToggle").addEventListener("click",()=>{showFavoritesOnly=!showFavoritesOnly;document.getElementById("favoritesToggle").textContent=showFavoritesOnly?"♥ Показываем избранное":"♡ Только избранное";renderRecipes();});
    document.getElementById("featuredOpenBtn").addEventListener("click",()=>{});
    document.getElementById("oracleBtn").addEventListener("click",oracle);
    ["noteDonateBtn","homeDonateBtn","wallDonateBtn","modalDonateBtn","modalDonateBottomBtn"].forEach(id=>document.getElementById(id).addEventListener("click",openDonate));
    document.getElementById("closeModal").addEventListener("click",closeRecipe);
    document.getElementById("recipeModal").addEventListener("click",e=>{if(e.target.id==="recipeModal")closeRecipe();});
    document.getElementById("modalFavoriteBtn").addEventListener("click",()=>currentRecipe&&toggleFavorite(currentRecipe.id));
    document.getElementById("cookStartBtn").addEventListener("click",openCookMode);
    document.getElementById("cookCloseBtn").addEventListener("click",closeCookMode);
    document.getElementById("cookPrevBtn").addEventListener("click",()=>navigateCookStep(-1));
    document.getElementById("cookNextBtn").addEventListener("click",()=>navigateCookStep(1));

    document.getElementById("successBackBtn").addEventListener("click",()=>closeCookSuccess({returnToRecipe:true}));
    document.getElementById("successDoneBtn").addEventListener("click",()=>closeCookSuccess({returnToRecipe:false}));
    document.getElementById("cookSuccess").addEventListener("click",e=>{if(e.target.id==="cookSuccess")closeCookSuccess({returnToRecipe:true});});
    document.addEventListener("keydown",e=>{if(e.key==="Escape"){const success=document.getElementById("cookSuccess");if(success.classList.contains("open")){closeCookSuccess({returnToRecipe:true});return;}closeCookMode();closeRecipe();}});
    document.addEventListener("error",e=>{const img=e.target;if(img instanceof HTMLImageElement){img.style.opacity=.25;}},true);

    renderAll();
    goTo(location.hash.replace("#","")||"home");
    loadRemoteData();

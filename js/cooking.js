function openCookMode(){
      if(!currentRecipe)return;
      cookStepIndex=0;
      document.getElementById("cookTitle").textContent=currentRecipe.title;
      document.getElementById("cookMetaSteps").textContent=`Шагов: ${currentRecipe.steps.length}`;
      document.getElementById("cookMetaServing").textContent=`🍜 ${currentRecipe.categories?.[0] || "Лабораторный протокол"}`;
      document.getElementById("cookPhotoLabel").textContent=`${currentRecipe.ingredients.length} ингредиентов · ${currentRecipe.steps.length} шагов`;
      const img=document.getElementById("cookImage");
      const backdrop=document.getElementById("cookBackdrop");
      if(currentRecipe.image){
        img.src=currentRecipe.image;
        img.alt=currentRecipe.title;
        backdrop.style.backgroundImage=`url('${currentRecipe.image}')`;
      }else{
        img.removeAttribute("src");
        img.alt="";
        backdrop.style.backgroundImage="none";
      }
      const cookOverlay=document.getElementById("cookOverlay");
      cookOverlay.classList.add("open");
      cookOverlay.setAttribute("aria-hidden","false");
      renderCookStep();
      requestAnimationFrame(syncCookTopHeight);
    }
    function syncCookTopHeight(){
      const overlay=document.getElementById("cookOverlay");
      const top=document.getElementById("cookTop");
      if(!overlay || !top)return;
      overlay.style.setProperty("--cook-top-height",`${Math.ceil(top.getBoundingClientRect().height)}px`);
    }
    if("ResizeObserver" in window){
      new ResizeObserver(syncCookTopHeight).observe(document.getElementById("cookTop"));
    }
    window.addEventListener("resize",syncCookTopHeight,{passive:true});
    function closeCookMode(){document.getElementById("cookOverlay").classList.remove("open");document.getElementById("cookOverlay").setAttribute("aria-hidden","true");}
    function renderCookStep(){
      if(!currentRecipe)return;
      const steps=currentRecipe.steps;
      const total=steps.length;
      document.getElementById("cookStepNumber").textContent=`Шаг ${cookStepIndex+1} из ${total}`;
      document.getElementById("cookStepText").textContent=steps[cookStepIndex];
      document.getElementById("cookProgress").style.width=`${((cookStepIndex+1)/total)*100}%`;
      document.getElementById("cookPrevBtn").disabled=cookStepIndex===0;
      document.getElementById("cookNextBtn").textContent=cookStepIndex===total-1?"Готово ✓":"Дальше →";
      const dots=document.getElementById("cookDots");
      dots.innerHTML=steps.map((_,i)=>`<span class="${i===cookStepIndex?"active":""}" aria-hidden="true"></span>`).join("");
    }

    let cookStepAnimating=false;
    function navigateCookStep(direction){
      if(!currentRecipe || cookStepAnimating)return;
      const total=currentRecipe.steps.length;
      const target=cookStepIndex+direction;
      if(target<0)return;
      if(target>=total){
        if(direction>0)showCookSuccess();
        return;
      }

      const panel=document.querySelector(".cook-main");
      const reduceMotion=window.matchMedia("(prefers-reduced-motion: reduce)").matches;
      if(!panel || reduceMotion){
        cookStepIndex=target;
        renderCookStep();
        return;
      }

      cookStepAnimating=true;
      const goingNext=direction>0;
      const outFrames=goingNext
        ?[
          {transform:"translateX(0) rotate(0deg) scale(1)",opacity:1},
          {transform:"translateX(-18%) rotate(-2.2deg) scale(.985)",opacity:0}
        ]
        :[
          {transform:"translateX(0) rotate(0deg) scale(1)",opacity:1},
          {transform:"translateX(18%) rotate(2.2deg) scale(.985)",opacity:0}
        ];
      const inFrames=goingNext
        ?[
          {transform:"translateX(18%) rotate(2.2deg) scale(.985)",opacity:0},
          {transform:"translateX(0) rotate(0deg) scale(1)",opacity:1}
        ]
        :[
          {transform:"translateX(-18%) rotate(-2.2deg) scale(.985)",opacity:0},
          {transform:"translateX(0) rotate(0deg) scale(1)",opacity:1}
        ];

      const outAnim=panel.animate(outFrames,{duration:170,easing:"cubic-bezier(.4,0,.7,.2)",fill:"forwards"});
      outAnim.onfinish=()=>{
        cookStepIndex=target;
        renderCookStep();
        const inAnim=panel.animate(inFrames,{duration:240,easing:"cubic-bezier(.16,.84,.32,1)",fill:"forwards"});
        inAnim.onfinish=()=>{
          panel.style.transform="";
          panel.style.opacity="";
          cookStepAnimating=false;
        };
        inAnim.oncancel=()=>{cookStepAnimating=false;};
      };
      outAnim.oncancel=()=>{cookStepAnimating=false;};
    }

    function buildSuccessConfetti(){
      const box=document.getElementById("successConfetti");
      const palette=["#ffb229","#ff7a18","#ffe086","#ffffff","#d85b10"];
      box.innerHTML=Array.from({length:28},(_,i)=>{
        const x=3+((i*37)%94);
        const c=palette[i%palette.length];
        const r=((i*47)%180)-90;
        const d=(1.7+(i%7)*.12).toFixed(2)+"s";
        const delay=((i%9)*.035).toFixed(3)+"s";
        const drift=(((i*53)%140)-70)+"px";
        return `<i style="--x:${x}%;--c:${c};--r:${r}deg;--d:${d};--delay:${delay};--drift:${drift}"></i>`;
      }).join("");
    }
    function showCookSuccess(){
      if(!currentRecipe)return;
      document.getElementById("successRecipeTitle").textContent=currentRecipe.title;
      document.getElementById("successBadgeText").textContent=`${currentRecipe.steps.length} из ${currentRecipe.steps.length} этапов завершены. Протокол закрыт.`;
      buildSuccessConfetti();
      const success=document.getElementById("cookSuccess");
      success.classList.remove("open");
      void success.offsetWidth;
      success.classList.add("open");
      success.setAttribute("aria-hidden","false");
    }
    function closeCookSuccess({returnToRecipe=false}={}){
      const success=document.getElementById("cookSuccess");
      success.classList.remove("open");
      success.setAttribute("aria-hidden","true");
      closeCookMode();
      if(!returnToRecipe) closeRecipe();
    }

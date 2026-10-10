import fs from 'node:fs';
import path from 'node:path';

// Illustrated recipe plates. Every garnish is derived exclusively from the recipe's ingredients.
// Deterministic SVG art; unlike the old template it never adds random unrelated food.
const root = process.cwd();
const source = path.join(root, 'data/recipes.json');
const recipes = JSON.parse(fs.readFileSync(source,'utf8'));
const descriptions = {
'garlic-butter':'Лапша, чеснок и сливочное масло ведут переговоры о капитуляции голода.',
'creamy-mushrooms':'Шампиньоны в нежных сливках успешно прошли собеседование с твоим аппетитом.',
'tomato-express':'Томатная лапша с луком прибывает на ужин раньше любой доставки.',
'cheese-volcano':'Сырная лава покрывает лапшу. Вулканологи рекомендуют взять вилку.',
'egg-broth':'Лёгкий бульон с яичными ленточками и зелёным луком: судьба в миске.',
'cold-cucumber':'Охлаждённая лапша с огурцом, укропом и йогуртом. Жара получила выговор.',
'cabbage-crunch':'Хрустящая капуста с морковью захватывают тарелку без единого выстрела.',
'carrot-garlic':'Морковь и чеснок проводят секретную операцию под прикрытием кунжута.',
'corn-landing':'Золотая кукуруза высадилась в сметанно-сырной лапше без разрешения штаба.',
'tomato-basil':'Помидор, свежий базилик и капля масла: бюджетная командировка в Италию.',
'zucchini-pan':'Обжаренный кабачок и чеснок делают лапшу подозрительно цивилизованной.',
'sour-mushrooms':'Грибы, лук и сметана составили отчёт о неприлично вкусном ужине.',
'pea-egg':'Зелёный горошек и нежное яйцо сдают экзамен на звание завтрака.',
'broccoli-cheese':'Брокколи встречает плавленый сыр. Вечеринка объявлена съедобной.',
'ham-creamy':'Ветчина, сыр и сметана повышают статус лапши до служебного банкета.',
'sausage-rave':'Сосиски под кетчупом и луком устраивают дискотеку на сковороде.',
'chicken-honey-soy':'Курица в медово-соевом соусе вернулась с командировки с отличными новостями.',
'mince-tomato':'Мясной фарш с томатом и луком превращает лапшу в бюджетное болоньезе.',
'bacon-alarm':'Хрустящий бекон и яйцо объявляют утреннюю тревогу для сонного желудка.',
'smoked-sausage':'Охотничьи колбаски с луком и паприкой берут голод в окружение.',
'cutlet-noodles':'Котлета и солёный огурец дают вчерашнему ужину вторую карьеру.',
'morning-cheese':'Два яйца, сыр и масло: деловое утро может начинаться без дел.',
'fried-egg-soy':'Глазунья с зелёным луком смотрит на лапшу поверх соевого соуса.',
'cheese-nuggets':'Золотистые лапшичные кометы с сыром: космос съедобен и хрустит.',
'cheese-bake':'Запеканка под сырной корочкой. План Б внезапно лучше плана А.',
'wrap-egg':'Горячий рулет из лаваша скрывает лапшу, яйцо и растаявший сыр.',
'spicy-tomato':'Томат, чеснок и хлопья чили посылают с тарелки острое сообщение.',
'spicy-garlic':'Чеснок, соевый и острый соусы превращают лапшу в маленький огнемёт.',
'spicy-cheese':'Острый соус прорывается сквозь сырную лаву. Футболка предупреждена.',
'fiery-mushroom':'Шампиньоны с луком и острым соусом объявляют красный уровень.',
'pepper-fire':'Сладкий перец притворился мирным, пока чили не включил сирену.',
'kimchi-soup':'Горячий суп с кимчи и яйцом. Соседний отдел передаёт привет.',
'tuna-lemon':'Тунец с лимоном и перцем подключился к лапше по защищённому каналу.',
'crab-salad':'Прохладный салат из лапши, огурца и крабовых палочек: всё по протоколу.',
'shrimp-garlic':'Креветки в чесночном масле официально объявили себя премиальным ужином.',
'lemon-butter':'Лимон, сливочное масло и сыр оправдывают даже самый поздний перекус.',
'peanut-sauce':'Арахисовая паста с соевым соусом завернула лапшу в секретный план.',
'coconut-curry':'Кокосовое молоко, морковь и карри оформляют командировку без чемодана.',
'honey-chili':'Мёд и чили заключили хрупкое перемирие на территории соевой лапши.',
'apple-dessert':'Яблоко, корица и мёд превращают лапшу в сладкий секрет института.'
};
const hashRecipe = recipe => {
  let value=2166136261;
  for (const character of JSON.stringify(recipe)) {
    const code=character.codePointAt(0);
    if (code > 65535) {
      const x=code-65536;
      value=Math.imul(value^(55296+(x>>10)),16777619);
      value=Math.imul(value^(56320+(x&1023)),16777619);
    } else value=Math.imul(value^code,16777619);
  }
  return (value>>>0).toString(16).padStart(8,'0');
};
const seedFor=id=>[...id].reduce((a,c)=>Math.imul(a^c.charCodeAt(0),16777619)>>>0,2166136261);
const rndFrom=id=>{let s=seedFor(id);return()=>{s=(Math.imul(s,1664525)+1013904223)>>>0;return s/4294967296;};};
const fmt=n=>Number(n).toFixed(1);
const oval=(cx,cy,rx,ry,color,stroke='none',width=1)=>`<ellipse cx="${fmt(cx)}" cy="${fmt(cy)}" rx="${rx}" ry="${ry}" fill="${color}" stroke="${stroke}" stroke-width="${width}"/>`;
const curve=(d,fill,stroke='none',width=1)=>`<path d="${d}" fill="${fill}" stroke="${stroke}" stroke-width="${width}" stroke-linejoin="round" stroke-linecap="round"/>`;
function motif(type,x,y,scale,rand){
const f=(n)=>fmt(n), rotate=Math.round((rand()-.5)*90), wrap=s=>`<g transform="translate(${f(x)} ${f(y)}) rotate(${rotate}) scale(${f(scale)})">${s}</g>`;
switch(type){
case 'egg':return wrap(`${curve('M-35 0Q-40-20-20-29Q-1-42 18-27Q38-20 37 5Q28 30 5 31Q-22 33-35 0Z','#fff5d2','#ead4ab',3)}${oval(4,0,17,17,'#f7a82a','#ffdd7c',3)}${oval(-2,-6,5,5,'#fff5c6')}`);
case 'egg-ribbon':return wrap(curve('M-29-15Q-9-28 6-6T30-8M-24 15Q-3-1 26 22','none','#fff3b5',9));
case 'mushroom':return wrap(`${curve('M-31 1Q-25-31 0-28Q25-28 31 1Z','#c7925c','#f6d7a7',3)}${curve('M-11 0Q-13 17 -9 23H10Q12 13 9 0Z','#efd9b3','#a97956',3)}${curve('M-20-1Q-3-17 20-2','none','#79523d',3)}`);
case 'garlic':return wrap(`${curve('M-19 12Q-31-3 -13-15Q-7-26 0-12Q8-30 17-13Q33 7 8 18Q-9 26-19 12Z','#fff0c6','#caad7a',3)}${curve('M0-10Q-8 7 0 18','none','#dbca9c',2)}`);
case 'cheese':return wrap(`${curve('M-29-19L26-11L10 20L-33 16Z','#ffcd54','#f9e79a',3)}${oval(2,-1,5,3,'#f4ad28')}${oval(-12,7,3,5,'#f3a72d')}`);
case 'onion':return wrap(`${curve('M-23-13Q10-37 27-6Q13 22-20 19Q6 8-23-13Z','#f9e5bb','#d5a77f',3)}`);
case 'cucumber':return wrap(`${oval(0,0,28,21,'#3d8a55','#b1ce6c',4)}${oval(0,0,22,15,'#c7dc96')} ${oval(-6,2,2,3,'#fbefc3')}${oval(6,2,2,3,'#fbefc3')}`);
case 'cabbage':return wrap(`${curve('M-28-10Q0-28 27-10Q9-3 30 12Q1 27-30 11Q-11-4-28-10Z','#b9d58b','#f3f4b9',3)}${curve('M-23 7Q0-5 22 8','none','#7da767',3)}`);
case 'carrot':return wrap(curve('M-27-9Q-11-15 28 6M-22 2Q0-3 23 17','none','#f3973f',9));
case 'corn':return wrap(`${oval(0,0,11,16,'#f2c74f','#ffe98a',3)}${oval(-4,-3,4,5,'#ffe17f')}`);
case 'tomato':return wrap(`${curve('M-27-9Q0-31 28-10L14 17Q-1 27-27-9Z','#e95c3b','#ffb372',3)}${curve('M-19-9Q0-14 16-8','none','#ffc59d',3)}`);
case 'basil':case 'dill':case 'green-onion':return wrap(`${curve('M-28 4Q-12-22 18-18Q28 9-14 19Z',type==='dill'?'#6b9845':'#418e55','#aade83',3)}${curve('M-19 10L19-13','none','#d2e89a',2)}`);
case 'zucchini':return wrap(`${oval(0,0,26,19,'#4e9565','#b8cf79',4)}${oval(0,0,18,12,'#d6db99')}`);
case 'peas':return wrap(`${oval(0,0,13,13,'#529c4e','#a4d375',3)}${oval(-4,-5,4,3,'#d2eea4')}`);
case 'broccoli':return wrap(`${curve('M-6 3L-9 24H8L5 3Z','#a4bf6b','#83a86f',3)}${oval(-20,-8,18,17,'#4c934c')}${oval(0,-17,22,21,'#60ad54')}${oval(22,-6,17,17,'#428b4b')}`);
case 'ham':case 'chicken':case 'cutlet':case 'mince':return wrap(`${curve('M-27-12Q-6-28 29-8L23 21Q-8 27-28 11Z',type==='chicken'?'#d79c6b':type==='ham'?'#dd8e83':'#9c5f3e','#e6bc83',3)}${curve('M-14-4Q-1-11 15-1','none','#f9d5ae',3)}`);
case 'sausage':case 'bacon':return wrap(`${oval(0,0,29,type==='bacon'?14:20,type==='bacon'?'#9e5336':'#b85e53','#f2b086',3)}${curve('M-17-5Q0-9 18 5','none','#fac9a1',3)}`);
case 'pepper':return wrap(`${curve('M-26-14Q5-30 25-6Q4 1 22 16Q-12 20-26-14Z','#db5341','#f2a16e',3)}`);
case 'chili':return wrap(curve('M-21-18L10-12L19 21Z','#dc3b32','#f3aa70',3));
case 'kimchi':return wrap(`${curve('M-26-16Q8-27 27-6Q16 17-26 19Z','#e56d40','#f8b065',3)}${curve('M-18-7Q-3-15 13 0','none','#ffbd82',4)}`);
case 'tuna':return wrap(`${curve('M-21-11Q5-19 23 0Q-10 26-26 12Z','#9d9b8c','#d8d0b9',3)}${curve('M-12-7L7 8M-8 12L14-6','none','#dfd7bf',3)}`);
case 'crab':return wrap(`${curve('M-30-9Q-12-18 30-5L24 14Q-17 14-30-9Z','#f5ecda','#dc5e5a',5)}`);
case 'shrimp':return wrap(`${curve('M-29-12Q21-38 29 4Q20 26-9 12','none','#f8a077',15)}${curve('M-26-11Q12-28 21-2','none','#ffdcad',4)}`);
case 'apple':return wrap(`${curve('M-26-8Q-15-25 1-18Q15-30 27-7L14 18Q-8 28-26-8Z','#f7c56d','#f8e5a9',3)}${curve('M0-19L3 18','none','#cb7e40',2)}`);
case 'sesame':return wrap(oval(0,0,5,3,'#fff3c9','#cfa77c',1));
case 'paprika':case 'pepper-black':case 'cinnamon':return wrap(oval(0,0,3,3,type==='cinnamon'?'#ab654a':type==='paprika'?'#d9613a':'#493126'));
default:return '';
}}
function classify(ingredients){
const text=ingredients.join(' ').toLowerCase();
const includes=r=>r.test(text), out=[];
const check=(name,re,count)=>{if(includes(re))out.push({name,count});};
check('egg',/яйц/,3);check('mushroom',/шампиньон/,6);check('garlic',/чеснок/,3);check('cheese',/сыр/,5);
check('onion',/луковиц/,4);check('cucumber',/огурец|огурц/,5);check('cabbage',/капуст(?!.*кимчи)/,5);
check('carrot',/морков/,5);check('corn',/кукуруз/,11);check('tomato',/помидор/,5);check('basil',/базилик/,4);
check('dill',/укроп/,5);check('green-onion',/зелёного лука/,5);check('zucchini',/кабачк/,5);
check('peas',/горошк/,11);check('broccoli',/брокколи/,5);check('ham',/ветчин/,6);check('chicken',/куриного филе/,6);
check('cutlet',/котлет/,2);check('mince',/фарш/,8);check('sausage',/сосиск|колбаск/,8);check('bacon',/бекон/,5);
check('pepper',/красного перца/,6);check('chili',/чили|острого соуса/,7);check('kimchi',/кимчи/,7);
check('tuna',/тунц/,7);check('crab',/крабовы/,5);check('shrimp',/кревет/,6);check('apple',/яблок/,6);
check('sesame',/кунжут/,14);check('paprika',/паприк/,9);check('pepper-black',/чёрный перец/,8);check('cinnamon',/корицы/,7);
return {out,text};
}
function imageFor(recipe){
 const rand=rndFrom(recipe.id), type=recipe.id;
 const {out,text}=classify(recipe.ingredients);
 const soup=/broth|soup|kimchi/.test(type),wrap=/wrap-egg/.test(type),bake=/cheese-bake|cheese-nuggets/.test(type),salad=/cold-cucumber|crab-salad/.test(type),dessert=/apple-dessert/.test(type),pan=/zucchini|bacon|sausage|fiery|tomato-express|cabbage-crunch|fried-egg/.test(type);
 const shape=wrap?'wrap':bake?'bake':pan?'pan':soup?'soup':salad?'salad':dessert?'dessert':['plate','bowl'][seedFor(type)%2];
 const creamy=/сливок|сметан|йогурт|молока|сыра|сыр|масла/.test(text);
 const tomato=/томат|кетчуп|чили|острого соуса|кимчи/.test(text);
 const green=/огурц|брокколи|зелёного лука|базилик|укроп|капуст|горошк/.test(text);
 const ground=tomato?'#a24d2e':creamy?'#dbab65':dessert?'#ba8c52':green?'#bd8f4e':'#b57b43';
 const shade=['#281710','#1b1d1e','#291d25','#1e2220'][seedFor(type)%4];
 const groundTop=['#523025','#333039','#3d2931','#243832'][seedFor(type)%4];
 const cx=360,cy=278,rx=shape==='plate'?239:shape==='wrap'?222:shape==='bake'?226:shape==='pan'?247:217,ry=shape==='plate'?151:shape==='pan'?147:shape==='wrap'?127:137;
 let s=`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 720 540" role="img" aria-label="${recipe.title.replaceAll('&','&amp;').replaceAll('"','&quot;')}"><defs><linearGradient id="wood" x2=".95" y2="1"><stop stop-color="${groundTop}"/><stop offset="1" stop-color="${shade}"/></linearGradient><linearGradient id="edge" x2=".7" y2="1"><stop stop-color="#f7d9a8"/><stop offset=".48" stop-color="#8a5e4a"/><stop offset="1" stop-color="#372522"/></linearGradient><radialGradient id="dish"><stop stop-color="${tomato?'#d08043':creamy?'#f6d08e':'#f5b859'}"/><stop offset="1" stop-color="${ground}"/></radialGradient><clipPath id="crop">${oval(cx,cy,rx,ry,'#fff')}</clipPath></defs>`;
 s+=`<rect width="720" height="540" fill="url(#wood)"/>`;
 for(let i=0;i<9;i++){const y=58+i*55+rand()*25;s+=curve(`M0 ${fmt(y)}Q340 ${fmt(y+rand()*18)} 720 ${fmt(y+rand()*24)}`,'none','#f5c68b',.8).replace('stroke-width="0.8"','stroke-width="0.8" opacity=".09"')}
 s+=oval(360,457,rx+52,36,'#090909');
 if(shape==='pan'){s+=`${oval(cx,cy,rx+24,ry+20,'#33353d','#77757a',13)}${curve('M592 270L707 300L697 326L573 301Z','#2a2d31','#77757a',6)}`;}
 else if(shape==='wrap'){s+=oval(cx,cy,rx+35,ry+25,'#473026','#bc9c7a',11);}
 else if(shape==='bake'){s+=`${oval(cx,cy,rx+32,ry+23,'#ae6c4f','#f8d8a2',11)}${oval(cx,cy,rx+17,ry+8,'#8b4837')}`;}
 else s+=oval(cx,cy,rx+23,ry+21,'url(#edge)','#f6d8a6',7);
 s+=oval(cx,cy,rx+2,ry+3,'#2f1b13','#e8ba8b',7)+oval(cx,cy,rx,ry,'url(#dish)');
 if(soup){s+=oval(cx,cy,rx-13,ry-12,tomato?'#ac4e30':'#b1844b');}
 if(salad){s+=oval(cx,cy,rx-14,ry-13,'#dfcd9e');}
 if(dessert){s+=oval(cx,cy,rx-12,ry-10,'#d9b578');}
 s+='<g clip-path="url(#crop)">';
 // Curled noodles are visible as the base, not as rows of repeated horizontal lines.
 for(let i=0;i<39;i++){
  const x=178+rand()*360,y=152+rand()*225,dx=10+rand()*23,dy=5+rand()*13;
  const d=`M${fmt(x-dx)} ${fmt(y+dy)}Q${fmt(x-dx*1.5)} ${fmt(y-dy)} ${fmt(x)} ${fmt(y-dy)}T${fmt(x+dx)} ${fmt(y+dy)}T${fmt(x+dx*1.75)} ${fmt(y)}`;
  s+=curve(d,'none','#6f3a22',13)+curve(d,'none',tomato?'#e79a47':creamy?'#ffe0a0':'#fbc062',8);
 }
 const toppings=out.flatMap(item=>Array.from({length:item.count},()=>item.name));
 // Dynamic deterministic spacing; show actual ingredients only.
 toppings.forEach((name,i)=>{
   const a=(i/toppings.length)*Math.PI*2+rand()*.42;
   const ring=38+Math.sqrt(rand())*(shape==='plate'?140:123);
   const x=cx+Math.cos(a)*ring*1.38,y=cy+Math.sin(a)*ring*.91;
   s+=motif(name,x,y,.65+rand()*.41,rand);
 });
 // Sauces show as a glaze rather than invented toppings.
 if(/сметан|сливок|йогурт/.test(text))for(let i=0;i<5;i++){const x=267+rand()*185,y=226+rand()*105;s+=curve(`M${fmt(x)} ${fmt(y)}q${fmt(12+rand()*12)} -11 ${fmt(27+rand()*12)} 4`,'none','#f7e8c4',4);}
 if(/томатн|кетчуп|острого соуса/.test(text))for(let i=0;i<4;i++){const x=253+rand()*200,y=222+rand()*119;s+=curve(`M${fmt(x)} ${fmt(y)}q14 -13 29 3`,'none','#bc4933',5);}
 if(/мёда|мёд/.test(text))for(let i=0;i<4;i++){const x=270+rand()*190,y=243+rand()*110;s+=curve(`M${fmt(x)} ${fmt(y)}q9 11 22 4`,'none','#edb24c',5);}
 s+='</g>';
 if(shape==='wrap'){s+=curve('M135 333Q227 140 338 237Q446 142 587 321Q486 416 345 397Q230 421 135 333Z','none','#e9bb78',9);}
 if(shape==='bake'){s+=curve('M158 374Q349 447 561 372','none','#f1c785',9);}
 else if(shape==='plate'||shape==='bowl'||shape==='soup'||shape==='dessert'){s+=oval(cx,cy,rx+6,ry+6,'none','#ffecbd',4);}
 s+=`<path d="M112 444Q356 494 604 443" fill="none" stroke="#fff2d2" stroke-opacity=".10" stroke-width="3"/></svg>`;
 return s;
}

for(const recipe of recipes.slice(10)){
 if(!descriptions[recipe.id]) throw new Error(`Missing description: ${recipe.id}`);
 recipe.description=descriptions[recipe.id];
 recipe.image=`images/recipes/${recipe.id}.svg?v=20261011-ingredient-art2`;
 const svg=imageFor(recipe);
 fs.writeFileSync(path.join(root,'images/recipes',recipe.id+'.svg'),svg);
 fs.writeFileSync(path.join(root,'data/recipe-entries',recipe.id+'.json'),JSON.stringify(recipe,null,2)+'\n');
}
fs.writeFileSync(source,JSON.stringify(recipes,null,2)+'\n');
const index={schemaVersion:1,recipes:recipes.map(recipe=>({id:recipe.id,hash:hashRecipe(recipe),path:`recipe-entries/${recipe.id}.json`}))};
fs.writeFileSync(path.join(root,'data/recipes-index.json'),JSON.stringify(index,null,2)+'\n');
console.log('Rebuilt',recipes.length-10,'ingredient-specific illustrations and descriptions; manifest entries:',index.recipes.length);
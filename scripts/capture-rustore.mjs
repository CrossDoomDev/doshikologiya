#!/usr/bin/env node
// Real Android screenshots via page-level WebView CDP and ADB. No generated UI.
import fs from 'node:fs/promises';
import path from 'node:path';
import { execFileSync } from 'node:child_process';

const out=path.resolve('store/rustore/screenshots');
await fs.mkdir(out,{recursive:true});
const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms));
const response=await fetch('http://127.0.0.1:9222/json/list');
if(!response.ok) throw new Error('CDP endpoint HTTP '+response.status);
const pages=await response.json();
const target=pages.find(x=>x.type==='page' && x.url.includes('localhost'));
if(!target?.webSocketDebuggerUrl)throw new Error('Android WebView page not found');
console.log('[WEBVIEW]',target.title,target.url);

const ws=new WebSocket(target.webSocketDebuggerUrl);
const pending=new Map();
let nextId=0;
ws.addEventListener('message',event=>{
  const msg=JSON.parse(event.data);
  if(!msg.id)return;
  const item=pending.get(msg.id);
  if(!item)return;
  pending.delete(msg.id);
  if(msg.error)item.reject(new Error(JSON.stringify(msg.error)));
  else item.resolve(msg.result);
});
ws.addEventListener('close',()=>{
  for(const item of pending.values())item.reject(new Error('CDP closed'));
  pending.clear();
});
await new Promise((resolve,reject)=>{
  ws.addEventListener('open',resolve,{once:true});
  ws.addEventListener('error',reject,{once:true});
});
function send(method,params={}){
  return new Promise((resolve,reject)=>{
    const id=++nextId;
    pending.set(id,{resolve,reject});
    ws.send(JSON.stringify({id,method,params}));
  });
}
async function evalJS(expression){
  const r=await send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});
  if(r.exceptionDetails)throw new Error('WebView JS error: '+JSON.stringify(r.exceptionDetails));
  return r.result?.value;
}
async function waitFor(expr,label,timeout=35000){
  const end=Date.now()+timeout;
  while(Date.now()<end){
    if(await evalJS(expr))return;
    await sleep(400);
  }
  throw new Error('Timed out waiting for '+label);
}
async function navigate(route){
  const clicked=await evalJS("(function(){const n=document.querySelector('.mobile-tabs [data-route=\""+route+"\"]');if(!n)return false;n.click();return true;})()");
  if(!clicked)throw new Error('Missing nav '+route);
  await waitFor("!!document.querySelector('#view-"+route+":not(.hidden)')",'route '+route);
  await sleep(650);
  await evalJS("window.scrollTo({top:0,behavior:'instant'});true");
}
async function capture(name,selector){
  await waitFor("!!document.querySelector("+JSON.stringify(selector)+")",name);
  await sleep(750);
  const broken=await evalJS("(function(){return Array.from(document.querySelectorAll('img')).filter(img=>{const b=img.getBoundingClientRect();return b.width>15&&b.height>15&&b.top<window.innerHeight&&b.bottom>0&&(!img.complete||!img.naturalWidth)}).map(img=>img.getAttribute('src')?.slice(0,120));})()");
  if(broken.length)throw new Error('Visible assets missing for '+name+': '+JSON.stringify(broken));
  const png=execFileSync('adb',['exec-out','screencap','-p'],{maxBuffer:25*1024*1024});
  if(png.subarray(0,8).toString('hex')!=='89504e470d0a1a0a')throw new Error('Not a PNG: '+name);
  const w=png.readUInt32BE(16),h=png.readUInt32BE(20);
  if(w!==1080||h!==1920)throw new Error('Wrong size for '+name+': '+w+'x'+h);
  if(png.length<180000)throw new Error('Screenshot unexpectedly small / not fully rendered: '+name+' ('+png.length+' bytes)');
  await fs.writeFile(path.join(out,name+'.png'),png);
  console.log('[CAPTURE]',name,w+'x'+h,png.length,'bytes');
}
try{
  await send('Runtime.enable');
  await waitFor("!!document.querySelector('#featuredImage')?.naturalWidth && !!document.querySelector('#featuredOpenBtn')?.dataset?.openRecipe",'app startup',45000);
  await navigate('home');
  await capture('01_home','#view-home:not(.hidden) #featuredImage');
  await navigate('recipes');
  await capture('02_recipes','#recipesGrid .recipe-card');
  await evalJS("(function(){const n=document.querySelector('#recipeSearch');n.value='Карбонара';n.dispatchEvent(new Event('input',{bubbles:true}));return true;})()");
  await waitFor("!!document.querySelector('#recipesGrid [data-open-recipe=\"carbonara-2049\"]')",'Carbonara search');
  await evalJS("document.querySelector('#recipesGrid [data-open-recipe=\"carbonara-2049\"]').click();true");
  await capture('03_recipe_details','#recipeModal.open #modalPhoto');
  await evalJS("document.querySelector('#cookStartBtn').click();true");
  await waitFor("!!document.querySelector('#cookOverlay.open #cookImage')?.naturalWidth",'cooking photo loaded');
  // Android WebView needs extra frames to compose the full-screen cooking overlay.
  await sleep(2600);
  await capture('04_cooking_mode','#cookOverlay.open #cookStepText');
  await evalJS("document.querySelector('#cookCloseBtn').click();document.querySelector('#closeModal').click();true");
  await navigate('oracle');
  await evalJS("document.querySelector('#oracleBtn').click();true");
  await waitFor("!!document.querySelector('#oracleResult .oracle-choice') && !!document.querySelector('#oracleMascot')?.naturalWidth",'oracle result',15000);
  await capture('05_oracle','#oracleResult .oracle-choice');
  await navigate('wall');
  await capture('06_patron_wall','#view-wall:not(.hidden) .comment');
  console.log('[DONE] 6 screenshots captured from actual Android app');
}finally{ws.close()}

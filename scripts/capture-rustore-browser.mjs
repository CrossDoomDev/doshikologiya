import { chromium } from 'playwright';
import fs from 'node:fs/promises';
import path from 'node:path';
const out=path.resolve('store/rustore-browser/screenshots');
await fs.mkdir(out,{recursive:true});
const browser=await chromium.launch({headless:true});
try{
 const ctx=await browser.newContext({viewport:{width:360,height:640},deviceScaleFactor:3,isMobile:true,hasTouch:true,locale:'ru-RU'});
 const page=await ctx.newPage();
 page.on('pageerror',e=>console.warn('[APP ERROR]',e.message));
 await page.goto('http://127.0.0.1:8765/',{waitUntil:'domcontentloaded'});
 await page.waitForFunction(()=>document.querySelector('#featuredImage')?.naturalWidth > 0 && Boolean(document.querySelector('#featuredOpenBtn')?.dataset?.openRecipe),null,{timeout:30000});
 await page.waitForTimeout(1100);
 const shots=[];
 async function save(name){
  await page.waitForTimeout(700);
  await page.screenshot({path:path.join(out,name+'.png'),animations:'disabled'});
  shots.push(name);
  console.log('[CAPTURE]',name,await page.locator('h1,h2').first().textContent());
 }
 async function nav(route){
  await page.locator('.mobile-tabs [data-route="'+route+'"]').click();
  await page.locator('#view-'+route+':not(.hidden)').waitFor({state:'visible'});
  await page.waitForTimeout(500);
 }
 await nav('home');await save('01_home');
 await nav('recipes');await save('02_recipes');
 await page.locator('#recipesGrid [data-open-recipe]').first().click();await save('03_recipe_details');
 await page.locator('#cookStartBtn').click();await save('04_cooking_mode');
 await page.locator('#cookCloseBtn').click();await page.locator('#closeModal').click();
 await nav('oracle');await page.locator('#oracleBtn').click();
 await page.locator('#oracleResult .oracle-choice').waitFor({timeout:12000});await save('05_oracle');
 await nav('wall');await save('06_patron_wall');
 console.log('[DONE]',shots.join(','));
}finally{await browser.close();}

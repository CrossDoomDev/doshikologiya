#!/usr/bin/env node
// RuStore capture from the actual debug Android WebView using Chromium DevTools Protocol.
// Screenshots are recorded via adb screencap, so they contain the real Android UI.
import { chromium } from 'playwright-core';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs/promises';
import path from 'node:path';

const out = path.resolve('store/rustore/screenshots');
await fs.mkdir(out, { recursive: true });
const browser = await chromium.connectOverCDP('http://127.0.0.1:9222', { timeout: 30000 });
try {
  const context = browser.contexts()[0];
  let page;
  for (let i = 0; i < 45; i++) {
    page = context.pages().find(p => p.url().includes('localhost') || p.url().includes('https://localhost'));
    if (page) break;
    await new Promise(r => setTimeout(r, 700));
  }
  if (!page) throw new Error('Android WebView page not available through CDP');
  page.on('pageerror', e => console.warn('[WEBVIEW JS ERROR]', e.message));
  await page.waitForFunction(() => {
    const img = document.querySelector('#featuredImage');
    const open = document.querySelector('#featuredOpenBtn');
    return Boolean(img?.complete && img?.naturalWidth && open?.dataset?.openRecipe &&
      !document.querySelector('#appSplash')?.classList.contains('active'));
  }, { timeout: 45000 });
  await page.waitForTimeout(1300);
  async function snap(name, checkSelector) {
    if (checkSelector) await page.locator(checkSelector).waitFor({state:'visible',timeout:12000});
    await page.waitForTimeout(500);
    const png = execFileSync('adb', ['exec-out','screencap','-p'], {maxBuffer: 25*1024*1024});
    if (png.subarray(0,8).toString('hex') !== '89504e470d0a1a0a') throw new Error('Invalid PNG capture '+name);
    const w=png.readUInt32BE(16),h=png.readUInt32BE(20);
    if (w!==1080 || h!==1920) throw new Error('Incorrect dimensions '+name+': '+w+'x'+h);
    const errors = await page.evaluate(()=>{
      return [...document.querySelectorAll('img')].filter(x=>x.getBoundingClientRect().width>20 &&
        x.getBoundingClientRect().top<window.innerHeight &&
        x.getBoundingClientRect().bottom>0 && (!x.complete || !x.naturalWidth))
        .map(x=>x.getAttribute('src')?.slice(0,100));
    });
    if (errors.length) throw new Error('Visible image(s) failed on '+name+': '+JSON.stringify(errors));
    const filename=path.join(out,name+'.png');
    await fs.writeFile(filename,png);
    console.log('[CAPTURE]',name,w+'x'+h,(png.length/1024).toFixed(1)+'KB',
      'URL',page.url(), 'title',await page.title());
  }
  async function nav(route) {
    await page.locator('.mobile-tabs [data-route="'+route+'"]').click({timeout:10000});
    await page.locator('#view-'+route+':not(.hidden)').waitFor({state:'visible',timeout:10000});
    await page.waitForTimeout(650);
  }

  await nav('home');
  await snap('01_home','#view-home:not(.hidden)');
  await nav('recipes');
  await snap('02_recipes','#recipesGrid .recipe-card');
  // Pick a visually rich recipe already present in the real bundled catalog.
  await page.locator('#recipeSearch').fill('Карбонара');
  await page.locator('#recipesGrid [data-open-recipe="carbonara-2049"]').waitFor({state:'visible',timeout:10000});
  await page.locator('#recipesGrid [data-open-recipe="carbonara-2049"]').click();
  await snap('03_recipe_details','#recipeModal.open #modalTitle');
  await page.locator('#cookStartBtn').click();
  await snap('04_cooking_mode','#cookOverlay.open #cookStepText');
  await page.locator('#cookCloseBtn').click();
  await page.locator('#closeModal').click();
  await nav('oracle');
  await page.locator('#oracleBtn').click();
  await page.waitForFunction(() => document.querySelector('#oracleMachine')?.classList.contains('success') ||
    document.querySelector('#oracleResult .oracle-choice'), {timeout:12000});
  await snap('05_oracle','#oracleResult .oracle-choice');
  await nav('wall');
  await snap('06_patron_wall','#view-wall:not(.hidden)');

  console.log('[DONE] 6 actual Android screenshots created.');
} finally {
  await browser.close();
}

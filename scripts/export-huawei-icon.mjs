// Export the existing Doshikologiya artwork for Huawei AppGallery.
// The original SVG intentionally hides the lower wordmark, which also leaves
// the instant noodle cup abruptly cut off. Extend the cup below the fade.
// This script does not modify Android launcher icons or the original SVG.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import sharp from "sharp";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const source = fs.readFileSync(path.join(root, "images/ui/doshikologiya-app-icon.svg"), "utf8");
const match = source.match(/href="data:image\/webp;base64,([^"]+)"/);
if (!match) throw new Error("Missing original embedded WebP in SVG icon.");

const SIZE = 768;
const original = Buffer.from(match[1], "base64");
const cropped = await sharp(original).resize(900, 900)
  .extract({ left: 66, top: 0, width: 768, height: 754 }).png().toBuffer();
const { data, info } = await sharp({
  create: { width: SIZE, height: SIZE, channels: 4,
    background: { r: 0, g: 0, b: 0, alpha: 0 } }
}).composite([{ input: cropped, left: 0, top: 14 }])
  .ensureAlpha().raw().toBuffer({ resolveWithObject: true });

// Cut the bottom wordmark, and feather over the redrawn lower edge.
const fadeFrom = 574, fadeTo = 629;
for (let y = fadeFrom; y < SIZE; y++) {
  const k = Math.max(0, Math.min(1, (fadeTo - y) / (fadeTo - fadeFrom)));
  for (let x = 0; x < SIZE; x++) {
    const i = (y * SIZE + x) * info.channels + 3;
    data[i] = Math.round(data[i] * k);
  }
}
const upper = await sharp(data, { raw: {
  width: SIZE, height: SIZE, channels: info.channels
}}).png().toBuffer();

// The cup's original top, rim, noodle illustration, laboratory symbol
// and chopsticks are preserved pixel-for-pixel. Only its hidden base is
// continued in the original ink/amber style.
const cupExtension = Buffer.from(String.raw`<svg xmlns="http://www.w3.org/2000/svg" width="768" height="768" viewBox="0 0 768 768">
  <defs>
    <linearGradient id="cup" x1="0" y1="0" x2="1" y2=".32">
      <stop offset="0" stop-color="#130d0b"/>
      <stop offset=".24" stop-color="#35221b"/>
      <stop offset=".55" stop-color="#161012"/>
      <stop offset=".82" stop-color="#2e1b15"/>
      <stop offset="1" stop-color="#110c0d"/>
    </linearGradient>
    <linearGradient id="gold" x1="0" x2="0" y1="0" y2="1">
      <stop offset="0" stop-color="#fff5b2"/>
      <stop offset=".35" stop-color="#ffd644"/>
      <stop offset="1" stop-color="#e18a00"/>
    </linearGradient>
  </defs>
  <!-- Rounded, slightly tapered continuation of the hidden ramen cup. -->
  <path d="M180 457 C186 541 202 633 235 691
           C269 721 493 724 533 690
           C564 629 583 542 591 456
           Z" fill="url(#cup)" stroke="#08070a" stroke-width="12" stroke-linejoin="round"/>
  <path d="M198 519 C205 577 222 648 246 684"
        stroke="#85643a" stroke-width="8" opacity=".5" fill="none" stroke-linecap="round"/>
  <path d="M573 522 C563 602 550 655 527 682"
        stroke="#734822" stroke-width="7" opacity=".52" fill="none" stroke-linecap="round"/>
  <!-- Finish the base with a narrow curved amber highlight and dark sole. -->
  <path d="M244 682 C297 713 461 718 525 681
           L520 696 C479 721 286 725 247 698 Z"
        fill="url(#gold)" stroke="#0c090b" stroke-width="6" stroke-linejoin="round"/>
  <path d="M255 704 C318 724 454 726 514 700"
        stroke="#130c10" stroke-width="10" stroke-linecap="round" fill="none"/>
</svg>`);

const result = await sharp({
  create: { width: SIZE, height: SIZE, channels: 4,
    background: { r: 0, g: 0, b: 0, alpha: 0 } }
}).composite([{ input: cupExtension, left: 0, top: 0 },
              { input: upper, left: 0, top: 0 }])
  .resize(512, 512, { kernel: sharp.kernel.lanczos3 })
  .png({ compressionLevel: 9, palette: true, quality: 100 })
  .toBuffer();

const outputPath = path.join(root, "images/ui/doshikologiya-app-icon-huawei.png");
fs.writeFileSync(outputPath, result);
if (result.length > 2_000_000) throw new Error("AppGallery PNG must be at most 2 MB.");
console.log("AppGallery icon: 512x512 PNG,", result.length, "bytes at", outputPath);

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import sharp from "sharp";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const res = path.join(root, "native-android", "app", "src", "main", "res");
const source = fs.readFileSync(path.join(root, "images", "ui", "doshikologiya-app-icon.svg"), "utf8");
const embedded = source.match(/href="data:image\/webp;base64,([^"]+)"/);
if (!embedded) throw new Error("В SVG-иконке не найден исходный WebP-рисунок.");
if (!fs.existsSync(res)) throw new Error("Не найден native-android/app/src/main/res.");

// Work directly with the embedded raster, rather than asking sharp/librsvg to resolve
// a WebP data URI inside an SVG. This reliably removes the entire lower wordmark.
// Match the coordinates and fade in doshikologiya-app-icon.svg exactly.
const ART_SIZE = 768;
const ART_SCALED_SIZE = 900;
const ART_LEFT_CROP = 66;
const ART_TOP_MARGIN = 14;
const FADE_START = 590;
const FADE_END = 634;

const sourceWebp = Buffer.from(embedded[1], "base64");
const scaled = await sharp(sourceWebp).resize(ART_SCALED_SIZE, ART_SCALED_SIZE).png().toBuffer();
const cropped = await sharp(scaled)
  .extract({ left: ART_LEFT_CROP, top: 0, width: ART_SIZE, height: ART_SIZE - ART_TOP_MARGIN })
  .png().toBuffer();
const { data: pixels, info } = await sharp({
  create: {
    width: ART_SIZE, height: ART_SIZE, channels: 4,
    background: { r: 0, g: 0, b: 0, alpha: 0 }
  }
})
  .composite([{ input: cropped, left: 0, top: ART_TOP_MARGIN }])
  .ensureAlpha()
  .raw()
  .toBuffer({ resolveWithObject: true });

for (let y = FADE_START; y < ART_SIZE; y++) {
  const opacity = Math.max(0, Math.min(1, (FADE_END - y) / (FADE_END - FADE_START)));
  for (let x = 0; x < ART_SIZE; x++) {
    const alpha = (y * ART_SIZE + x) * info.channels + 3;
    pixels[alpha] = Math.round(pixels[alpha] * opacity);
  }
}
const logo = await sharp(pixels, { raw: { width: ART_SIZE, height: ART_SIZE, channels: info.channels } })
  .png().toBuffer();

const bgColor = "#160f0a";
const densities = new Map([
  ["mdpi", 48],
  ["hdpi", 72],
  ["xhdpi", 96],
  ["xxhdpi", 144],
  ["xxxhdpi", 192]
]);

function writeXml(relativePath, content) {
  const dest = path.join(res, relativePath);
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.writeFileSync(dest, content.trim() + "\n", "utf8");
}

async function compositeLauncher(size, foregroundFraction, { round = false, transparent = false } = {}) {
  const markSize = Math.round(size * foregroundFraction);
  const left = Math.round((size - markSize) / 2);
  const mark = await sharp(logo).resize(markSize, markSize, { fit: "contain" }).png().toBuffer();
  let result = await sharp({
    create: { width: size, height: size, channels: 4, background: transparent ? "#00000000" : bgColor }
  }).composite([{ input: mark, left, top: left }]).png().toBuffer();
  if (round) {
    const mask = Buffer.from(`<svg width="${size}" height="${size}" xmlns="http://www.w3.org/2000/svg"><circle cx="${size / 2}" cy="${size / 2}" r="${size / 2}" fill="#fff"/></svg>`);
    result = await sharp(result).composite([{ input: mask, blend: "dest-in" }]).png().toBuffer();
  }
  return result;
}

for (const [density, size] of densities) {
  const folder = path.join(res, `mipmap-${density}`);
  fs.mkdirSync(folder, { recursive: true });
  fs.writeFileSync(path.join(folder, "ic_launcher.png"), await compositeLauncher(size, 0.84));
  fs.writeFileSync(path.join(folder, "ic_launcher_round.png"), await compositeLauncher(size, 0.84, { round: true }));
  // Adaptive icon artwork stays within the safe zone of the 108dp launcher foreground.
  const adaptiveSize = Math.round(size * 2.25);
  fs.writeFileSync(path.join(folder, "ic_launcher_foreground.png"),
    await compositeLauncher(adaptiveSize, 0.62, { transparent: true }));
}

writeXml("values/doshik_launcher_colors.xml", `
<resources>
    <color name="doshik_launcher_bg">${bgColor}</color>
</resources>`);

const adaptive = `
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/doshik_launcher_bg"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>`;
writeXml("mipmap-anydpi-v26/ic_launcher.xml", adaptive);
writeXml("mipmap-anydpi-v26/ic_launcher_round.xml", adaptive);

console.log("Иконка Дошикологии без нижней надписи: обычная, круглая и адаптивная, все плотности.");

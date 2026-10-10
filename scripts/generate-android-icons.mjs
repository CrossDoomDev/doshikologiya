import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import sharp from "sharp";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const res = path.join(root, "android", "app", "src", "main", "res");
const source = fs.readFileSync(path.join(root, "images", "ui", "doshikologiya-mark.svg"), "utf8");
const embedded = source.match(/href="data:image\/webp;base64,([^"]+)"/);
if (!embedded) throw new Error("В фирменном SVG не найден встроенный WebP-логотип.");
if (!fs.existsSync(res)) throw new Error("Сначала создайте Android-проект командой cap add android.");

const logo = Buffer.from(embedded[1], "base64");
const dimensions = await sharp(logo).metadata();
if (dimensions.width !== dimensions.height || dimensions.width < 256) {
  throw new Error("Исходный логотип должен быть квадратным и достаточно крупным.");
}

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
  // Adaptive launcher: 108dp viewport, artwork inside the guaranteed 72dp safe zone.
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

console.log("Готовы фирменные Android-иконки: 5 плотностей, обычные, круглые и адаптивные.");

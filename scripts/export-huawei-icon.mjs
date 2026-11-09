// Faithful PNG conversion of images/ui/doshikologiya-app-icon.svg.
// This SVG uses a base64-embedded WebP and a bottom fade-out mask.
// Reproduce that geometry and alpha mask exactly; do not invent or redraw pixels.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import sharp from "sharp";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const svgPath = path.join(root, "images", "ui", "doshikologiya-app-icon.svg");
const svg = fs.readFileSync(svgPath, "utf8");
const match = svg.match(/href="data:image\/webp;base64,([^"]+)"/);
if (!match) throw new Error("Original SVG does not contain the embedded WebP.");

const SIZE = 768;
const picture = Buffer.from(match[1], "base64");
// SVG: <image x="-66" y="14" width="900" height="900" ...>
const enlarged = await sharp(picture).resize(900, 900).png().toBuffer();
const visible = await sharp(enlarged)
  .extract({ left: 66, top: 0, width: SIZE, height: SIZE - 14 })
  .png().toBuffer();
const { data, info } = await sharp({
  create: { width: SIZE, height: SIZE, channels: 4,
    background: { r: 0, g: 0, b: 0, alpha: 0 } }
}).composite([{ input: visible, left: 0, top: 14 }])
  .ensureAlpha().raw().toBuffer({ resolveWithObject: true });

// SVG mask: fully visible through y=590, linear fade to y=634, then invisible.
// This preserves the original cut-off as requested, without "fixing" the artwork.
for (let y = 590; y < SIZE; y++) {
  const alpha = Math.max(0, Math.min(1, (634 - y) / 44));
  for (let x = 0; x < SIZE; x++) {
    const index = (y * SIZE + x) * info.channels + 3;
    data[index] = Math.round(data[index] * alpha);
  }
}
const exactRaster = await sharp(data, {
  raw: { width: SIZE, height: SIZE, channels: info.channels }
}).png().toBuffer();
const output = await sharp(exactRaster)
  .resize(512, 512, { kernel: sharp.kernel.lanczos3 })
  .png({ compressionLevel: 9 })
  .toBuffer();
if (output.length >= 2_000_000) throw new Error("PNG exceeds 2 MB AppGallery limit.");
const target = path.join(root, "images", "ui", "doshikologiya-app-icon.png");
fs.writeFileSync(target, output);
console.log("Faithful conversion:", target, "512x512 PNG,", output.length, "bytes");

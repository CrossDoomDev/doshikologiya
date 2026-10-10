import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const output = path.join(root, "www");
const sources = ["index.html", "css", "js", "data", "images", "manifest.webmanifest"];
fs.rmSync(output, { recursive: true, force: true });
fs.mkdirSync(output, { recursive: true });

for (const relativePath of sources) {
  const source = path.join(root, relativePath);
  if (!fs.existsSync(source)) throw new Error("Missing Android resource: " + relativePath);
  fs.cpSync(source, path.join(output, relativePath), { recursive: true });
}
const recipeImages = fs.readdirSync(path.join(output, "images", "recipes")).length;
console.log("Prepared offline Android assets, recipe images: " + recipeImages);

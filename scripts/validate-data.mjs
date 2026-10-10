import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const readJson = file => JSON.parse(fs.readFileSync(path.join(root, file), "utf8"));

const config = readJson("data/config.json");
const recipes = readJson("data/recipes.json");
const patrons = readJson("data/patrons.json");

const errors = [];
const ids = new Set();

if (!Array.isArray(recipes)) errors.push("data/recipes.json должен содержать массив.");
if (!Array.isArray(patrons)) errors.push("data/patrons.json должен содержать массив.");

for (const [index, recipe] of (Array.isArray(recipes) ? recipes : []).entries()) {
  const label = `Рецепт #${index + 1}`;
  for (const field of ["id", "title", "description", "time", "difficulty", "cost", "story", "image"]) {
    if (typeof recipe[field] !== "string" || !recipe[field].trim()) errors.push(`${label}: отсутствует строковое поле "${field}".`);
  }
  if (!Array.isArray(recipe.ingredients) || !recipe.ingredients.length) errors.push(`${label}: ingredients должен быть непустым массивом.`);
  if (!Array.isArray(recipe.steps) || !recipe.steps.length) errors.push(`${label}: steps должен быть непустым массивом.`);
  if (!Array.isArray(recipe.categories) || !recipe.categories.length) errors.push(`${label}: categories должен быть непустым массивом.`);

  if (recipe.id) {
    if (ids.has(recipe.id)) errors.push(`Повторяющийся id: ${recipe.id}`);
    ids.add(recipe.id);
  }

  if (typeof recipe.image === "string" && recipe.image && !/^https?:\/\//.test(recipe.image)) {
    const imagePath = recipe.image.split("?")[0];
    if (!fs.existsSync(path.join(root, imagePath))) errors.push(`${label}: не найдено изображение ${imagePath}`);
  }
}

if (config.featuredRecipeId && !ids.has(config.featuredRecipeId)) {
  errors.push(`featuredRecipeId "${config.featuredRecipeId}" не найден среди рецептов.`);
}
if (!Array.isArray(config.categoryOrder) || !config.categoryOrder.includes("Все")) {
  errors.push('config.categoryOrder должен быть массивом и содержать "Все".');
}
if (config.donateUrl && !/^https:\/\//.test(config.donateUrl)) {
  errors.push("donateUrl должен быть HTTPS-ссылкой.");
}


/* Защита оригинального названия во всех пользовательских текстах. */
const forbiddenBrandPatterns = [
  new RegExp("доши" + "рак", "iu"),
  new RegExp("doshi" + "rak", "iu")
];
const inspectedExtensions = new Set([
  ".html", ".css", ".js", ".mjs", ".json", ".md", ".svg", ".webmanifest", ".txt", ".yml", ".yaml"
]);
const ignoredDirectories = new Set([".git", "node_modules", ".cache"]);

function inspectPublicTexts(folder) {
  for (const entry of fs.readdirSync(folder, { withFileTypes: true })) {
    const entryPath = path.join(folder, entry.name);
    if (entry.isDirectory()) {
      if (!ignoredDirectories.has(entry.name)) inspectPublicTexts(entryPath);
      continue;
    }
    if (!entry.isFile() || !inspectedExtensions.has(path.extname(entry.name).toLowerCase())) continue;

    const content = fs.readFileSync(entryPath, "utf8");
    if (forbiddenBrandPatterns.some(pattern => pattern.test(content))) {
      const relativePath = path.relative(root, entryPath).split(path.sep).join("/");
      errors.push(`${relativePath}: обнаружено чужое торговое название. Используй «Дошик».`);
    }
  }
}

inspectPublicTexts(root);


const index = readJson("data/recipes-index.json");
function fingerprint(recipe) {
  let h = 2166136261;
  for (let i = 0, str = JSON.stringify(recipe); i < str.length; i++) {
    h = Math.imul(h ^ str.charCodeAt(i), 16777619);
  }
  return (h >>> 0).toString(16).padStart(8, "0");
}
if (recipes.length !== 50) errors.push("Ожидалось ровно 50 рецептов.");
if (index.schemaVersion !== 1 || !Array.isArray(index.recipes) || index.recipes.length !== recipes.length) {
  errors.push("Индекс рецептов отсутствует или содержит неверное количество.");
} else {
  const byId = new Map(recipes.map(recipe => [recipe.id, recipe]));
  const manifestIds = new Set();
  for (const entry of index.recipes) {
    const recipe = byId.get(entry.id);
    if (manifestIds.has(entry.id)) errors.push("Повтор в индексе: " + entry.id);
    manifestIds.add(entry.id);
    if (!recipe || entry.path !== `recipe-entries/${entry.id}.json` || entry.hash !== fingerprint(recipe)) {
      errors.push("Неверный индекс рецепта: " + entry.id); continue;
    }
    const resource = path.join(root, "data", entry.path);
    if (!fs.existsSync(resource) || JSON.stringify(readJson(path.join("data", entry.path))) !== JSON.stringify(recipe)) {
      errors.push("Не совпадает отдельный файл рецепта: " + entry.id);
    }
  }
}

if (errors.length) {
  console.error("Проверка данных не пройдена:\n- " + errors.join("\n- "));
  process.exit(1);
}

console.log(`Данные корректны: ${recipes.length} рецептов, ${patrons.length} записей на стене, schemaVersion ${config.schemaVersion}.`);

// Public, non-secret contact settings. The Android build has its own fallback config,
// while GitHub Pages can update these fields without publishing another APK.
const CACHE_KEY = "doshikologiya:service-contacts";

export function validSupportEmail(email) {
  return typeof email === "string" && email.length <= 254
    && /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email);
}

function validDonateUrl(value) {
  if (value === "") return true;
  if (typeof value !== "string" || value.length > 2048) return false;
  try {
    const url = new URL(value);
    return url.protocol === "https:" && url.hostname !== "" && !url.username && !url.password;
  } catch {
    return false;
  }
}

export function parseServiceContacts(value) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error("Некорректный формат публичной конфигурации.");
  }
  const contacts = {};
  if (Object.hasOwn(value, "supportEmail")) {
    if (typeof value.supportEmail !== "string") throw new Error("Некорректная почта поддержки.");
    const email = value.supportEmail.trim();
    if (email && !validSupportEmail(email)) throw new Error("Некорректная почта поддержки.");
    contacts.supportEmail = email;
  }
  if (Object.hasOwn(value, "donateUrl")) {
    if (!validDonateUrl(value.donateUrl)) throw new Error("Некорректная ссылка на поддержку проекта.");
    contacts.donateUrl = value.donateUrl;
  }
  if (!Object.keys(contacts).length) throw new Error("В конфигурации нет настроек для обновления.");
  return contacts;
}

export function readCachedServiceContacts(storage = globalThis.localStorage) {
  try {
    const raw = storage?.getItem(CACHE_KEY);
    return raw ? parseServiceContacts(JSON.parse(raw)) : {};
  } catch {
    return {};
  }
}

export function applyServiceContacts(config, contacts) {
  return { ...config, ...parseServiceContacts(contacts) };
}

export async function fetchRemoteServiceContacts(url, {
  fetchImpl = globalThis.fetch,
  storage = globalThis.localStorage,
  timeoutMs = 6000
} = {}) {
  if (typeof url !== "string" || !url.startsWith("https://")) {
    throw new Error("Не указан безопасный адрес публичной конфигурации.");
  }
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetchImpl(url, { cache: "no-store", signal: controller.signal });
    if (!response.ok) throw new Error("HTTP " + response.status);
    const contacts = parseServiceContacts(await response.json());
    try { storage?.setItem(CACHE_KEY, JSON.stringify(contacts)); } catch { /* storage may be blocked */ }
    return contacts;
  } finally {
    clearTimeout(timeout);
  }
}

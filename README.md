# Дошикология — разработка сайта

**Веб-версия:** https://crossdoomdev.github.io/doshikologiya/

## Разделение веток

- **`main`** — самостоятельная веб-версия на HTML, CSS и JavaScript. Это основная среда для разработки интерфейса и контента; её публикует GitHub Pages.
- **[`release/kotlin-rustore-ads`](https://github.com/CrossDoomDev/doshikologiya/tree/release/kotlin-rustore-ads)** — нативная версия Android на Kotlin / Jetpack Compose с рекламой Яндекса. В ней находятся `native-android/`, Gradle и GitHub Actions для сборки подписанного APK.

В `main` намеренно **нет Capacitor, Android SDK, Gradle, сборки APK/AAB и файлов магазинов**. Не пытайтесь собирать Android из этой ветки.

Изменения интерфейса сначала проектируем и проверяем в `main`, затем воспроизводим их **на Kotlin** в `release/kotlin-rustore-ads`. Обычный `merge` HTML и JavaScript в Kotlin не переносит: это разные реализации, и синхронизация требует отдельной работы.

## Структура сайта

- `index.html` — разметка страниц и модальных окон;
- `css/` — стили сайта и документов;
- `js/core/`, `js/features/`, `js/main.js` — загрузка данных, маршрутизация, избранное, рецепты, режим готовки, Оракул и вспомогательные функции;
- `data/config.json` — публичные настройки, почта поддержки и ссылка CloudTips;
- `data/recipes.json`, `data/recipes-index.json`, `data/recipe-entries/` — каталог и индекс изменений;
- `data/patrons.json`, `data/news.json` — Стена меценатов и новости;
- `images/` — графика;
- `privacy.html`, `data-rights.html`, `terms.html` — документы, доступные на GitHub Pages и по ссылкам из карточек приложений;
- `scripts/validate-data.mjs`, `tests/` — проверки данных и функциональности.

HTML, CSS, JS и JSON публикуются как статический сайт без npm-сборщика. Для локального запуска используйте HTTP-сервер (не `file://`):

```bash
python -m http.server 8000
```

Откройте `http://localhost:8000/`. Для тестов нужен Node.js:

```bash
node scripts/validate-data.mjs
node --test tests/*.test.mjs
```

На `main` оставлен GitHub Actions `validate-content.yml`: он проверяет каталог и тесты, но не создаёт APK.

## Разработка контента и интерфейса

В каталоге `data/recipes.json` хранятся текущие рецепты для сайта. При изменении рецепта обновляйте его отдельную запись `data/recipe-entries/<id>.json` и контрольный отпечаток в `data/recipes-index.json`. Подробности в [data/README.md](data/README.md).

Сайт содержит поиск, фильтры, избранное, три режима раскладки, режим готовки, Оракула, Стену меценатов и раздел документов. Избранное и настройки интерфейса сохраняются в браузере. Приоритет на загрузке — у опубликованного JSON-каталога; в разделе «Ещё» можно вручную проверить обновления и обновить текущую страницу. Для первоначального запуска сайта нужен интернет: service worker для полного офлайн-запуска не настроен.

Общие данные, новые рецепты и изображения могут затем использоваться при отдельной разработке Kotlin-версии; перенос интерфейса и проверка поведения выполняются в нативной ветке.

## Публичные документы

- https://crossdoomdev.github.io/doshikologiya/privacy.html
- https://crossdoomdev.github.io/doshikologiya/data-rights.html
- https://crossdoomdev.github.io/doshikologiya/terms.html

Эти страницы остаются общедоступными; удалять их при очистке Android-артефактов нельзя. Документы должны соответствовать фактическим функциям опубликованной Kotlin-сборки, включая рекламный SDK и передачу данных. Веб-ветка сама по себе не содержит рекламных SDK.

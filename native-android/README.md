# Дошикология на Kotlin (отдельный Android-проект)

Проект расположен только в `native-android/`. Он **не использует** Capacitor,
WebView, HTML/CSS/JS или старые скрипты Android-сборки. Веб-прототип в корне
репозитория остаётся самостоятельным проектом и продолжает жить отдельно.

## Пробная сборка
- Отдельный тестовый пакет: `ru.doshikologiya.nativepreview` (сохраняет
  установленную оригинальную `ru.doshikologiya.app`).
- Kotlin + Jetpack Compose, независимые Gradle и GitHub Actions.
- Главная, каталог, поиск, карточка рецепта, режим готовки и первая
  нативная интеграция **демо** Yandex SDK (inline).
- Встроенные 50 рецептов работают без интернета, фото — локально.
- Оракул и «Ещё» пока прототипы. Функциональный паритет с HTML впереди.

## Разделение двух проектов
| | Веб | Kotlin |
|---|---|---|
| Исходники | `index.html`, `css/`, `js/` | `native-android/app/src/main/` |
| Сборка | GitHub Pages / текущий Capacitor workflow | Новый Gradle / отдельный workflow |
| Пакет | `ru.doshikologiya.app` | `ru.doshikologiya.nativepreview` |
| Монетизация | без изменений | только демобаннер в debug |

Нативный проект **читает только общие данные** из корневых `data/` и
`images/` с помощью Gradle assets source sets. Это не объединение кода
двух приложений. Сборка Kotlin никогда не выполняет веб-скрипты.

## Сборка
GitHub Actions: `.github/workflows/native-kotlin.yml`.
Артефакт: `doshikologiya-kotlin-preview-debug`.

Локально при наличии JDK 17, Android SDK и Gradle 8.11.1:

```bash
cd native-android
gradle :app:assembleDebug
```

Полученный APK: `app/build/outputs/apk/debug/app-debug.apk`.

**Не отправлять debug APK в RuStore.** Перенос избранного, полнота
функциональности, подпись прежним ключом и прежний app ID — отдельный этап.

// Install custom native ad bridge into the ephemeral Capacitor Android project.
// Runs after 'npx cap add android' + 'npx cap sync android', on every CI build.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const app = path.join(root, "android", "app");
const gradlePath = path.join(app, "build.gradle");
const activityPath = path.join(app, "src", "main", "java", "ru", "doshikologiya", "app", "MainActivity.java");
const manifestPath = path.join(app, "src", "main", "AndroidManifest.xml");
const pluginTarget = path.join(path.dirname(activityPath), "DoshikAdsPlugin.java");
if (![gradlePath, activityPath, manifestPath].every(fs.existsSync)) {
  throw new Error("Capacitor Android project not created. Run npx cap add android first.");
}
let gradle = fs.readFileSync(gradlePath, "utf8");
if (!gradle.includes("com.yandex.android:mobileads:")) {
  gradle = gradle.replace(/dependencies\s*\{/, "dependencies {\n    implementation 'com.yandex.android:mobileads:8.5.0'");
}
if (!gradle.includes("com.yandex.android:mobileads:8.5.0")) throw new Error("Could not inject ads Gradle dependency.");
// Preserve installed-app upgrade compatibility: same package/signature, increased versionCode.
gradle = gradle.replace(/versionCode\s+\d+/, "versionCode 8").replace(/versionName\s+['\"][^'\"]+['\"]/, 'versionName "1.1.6"');
// Install the QA build side-by-side with the signed RuStore app (do not overwrite user data).
if (!gradle.includes('applicationIdSuffix ".demo"')) {
  gradle = gradle.replace(/buildTypes\s*\{/, 'buildTypes {\n        debug { applicationIdSuffix ".demo"; versionNameSuffix "-demo" }');
}
fs.writeFileSync(gradlePath, gradle);
let activity = fs.readFileSync(activityPath, "utf8");
if (!activity.includes("registerPlugin(DoshikAdsPlugin.class)")) {
  const needle = "public class MainActivity extends BridgeActivity {";
  if (!activity.includes(needle)) throw new Error("Unexpected Capacitor MainActivity template.");
  activity = activity.replace(needle, needle + `
    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        registerPlugin(DoshikAdsPlugin.class);
        super.onCreate(savedInstanceState);
    }
`);
}
fs.writeFileSync(activityPath, activity);
fs.copyFileSync(path.join(root, "native", "android", "DoshikAdsPlugin.java"), pluginTarget);
let manifest = fs.readFileSync(manifestPath, "utf8");
if (!manifest.includes("com.yandex.mobile.ads.AUTOMATIC_SDK_INITIALIZATION")) {
  manifest = manifest.replace(/(<application\b[^>]*>)/, '$1\n        <meta-data android:name="com.yandex.mobile.ads.AUTOMATIC_SDK_INITIALIZATION" android:value="false" />');
}
fs.writeFileSync(manifestPath, manifest);
console.log("Installed Yandex Ads SDK and native cooking-slot bridge. Debug=demo unit; release=R-M-20184115-1");

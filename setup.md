# Social Kids — Setup Guide

Local-first, parent-curated media player for kids (replaces YouTube/TikTok browsing). 100% Room/DataStore, no account, no ads, no analytics. Child sees only parent-added content; YouTube/TikTok/Facebook play via official embed (`youtube-nocookie.com` + `android-youtube-player`), direct `mp4/m3u8` via ExoPlayer, images via Coil.

**Package:** `com.example.socialbaby` → change to `com.socialkids.app` before Play publish (`app/build.gradle.kts:10` namespace + `12` applicationId)  
**Version:** `versionCode 1` `versionName 1.0` `app/build.gradle.kts:15` | `targetSdk 37` `minSdk 24` | **AGP 9.3.2 / Kotlin 2.2.10 / Gradle 9.5 / JDK 17/25**

---

## 1) Prerequisites

* **Android Studio Ladybug+** (AGP 9.3.2)
* **JDK 17** (Gradle 9.5 bundles JDK 25, fallback to 24 is automatic; `kotlin.jvmTarget = 17` `app/build.gradle.kts:9`)
* **Android SDK:** `compileSdk 37`, `Build Tools 37`, `Platform Tools`
* **Device/Emulator:** Android 7.0+ (API 24), hardwareAccelerated WebView, internet for oEmbed/YouTube embed (local videos work offline)
* **Git**

```bash
java -version  # 17+
./gradlew --version # Gradle 9.5, Kotlin 2.3.20 embedded
```

---

## 2) Clone & Open

```bash
git clone <your-repo> SocialBaby2
cd SocialBaby2
# open folder in Android Studio → Sync (Gradle sync auto)
```

`settings.gradle.kts:14` `rootProject.name = "Social Baby"` → `Social Kids` (`res/values/strings.xml:2`)

---

## 3) Build

### Debug (dev, no signing)
```bash
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk (25 MB)
# or :app:bundleDebug
```

### Release (Play requires .aab signed)
1. Create keystore once:
```bash
keytool -genkey -v -keystore socialkids.jks -keyalg RSA -keysize 2048 -validity 10000 -alias socialkids
```
2. `app/build.gradle.kts:30` signing:
```kotlin
signingConfigs {
  create("release") {
    storeFile = file("../socialkids.jks")
    storePassword = System.getenv("STORE_PASS")
    keyAlias = "socialkids"
    keyPassword = System.getenv("KEY_PASS")
  }
}
buildTypes { release { isMinifyEnabled=true; isShrinkResources=true; signingConfig=signingConfigs.getByName("release") } }
```
3. Build:
```bash
STORE_PASS=xxx KEY_PASS=xxx ./gradlew :app:bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

If `android.builtInKotlin=false` warning → already set `gradle.properties:19` for AGP 9.3.2 compat.

---

## 4) Run

* **Emulator:** AVD Pixel 7 API 34, cold boot, `Run → app`
* **Device:** Enable `Developer options → USB debugging`, `adb devices`, `Run`
* First launch seeds DB `MainActivity.kt:56`:
  * 4 shelves: `Long Videos`, `Shorts`, `Photos`, `Bedtime` (`ShelfEntity` `SHELF/SHORTS_FEED/PHOTOS/PLAYLIST`)
  * 5 samples: `android-screens-10s.mp4` (ExoPlayer test, always 200), `Big Buck Bunny` (test-videos.co.uk), `picsum 800/600` (Coil), YouTube `YE7VzlLtp-4` + `dQw4w9WgXcQ` (youtube-player)

Uninstall/reinstall to re-seed (old `hashCode` IDs from early builds cause 150/153).

---

## 5) App Flow

**Child (default `KidTubeNavHost` `MainActivity.kt:123`):**
* **Home `ui/child/home/HomeScreen.kt:1`** — banner `Social Kids` gradient, `Shorts`/`Photos` `BigNavCard` (`secondary/tertiary`), shelf `FilterChip` (`primary/secondaryContainer`), grid 2-col `MediaCard` (thumb via `AsyncImage` or `MediaMetadataRetriever` + play dot, `VIDEO/PHOTO/WEB/YT` badge). Filtering fixed: `selectedShelf.flatMapLatest { pagingAll/byShelf }`.
* **Shorts `ShortsFeedScreen.kt:1`** — `VerticalPager` `beyondViewport 1`, `isCurrentPage` → `LocalVideoPlayer(autoPlay, looping, showControls=false, isMuted)` for `LOCAL/ONLINE_VIDEO`, `YoutubePlayer` (muted autoplay) for YT, else `EmbeddedLinkPlayer` / `AsyncImage`. Swipe auto-plays like Reels.
* **Photos `PhotoGalleryScreen.kt:1`** — `HorizontalPager` `AsyncImage Fit`.
* **Player `PlayerScreen.kt:1`** — `LocalVideoPlayer` (ExoPlayer) for `LOCAL/ONLINE_VIDEO`, `AsyncImage` for images, `YoutubePlayer` (dedicated `pierfrancescosoffritti:androidyoutubeplayer:12.1.0`) for YT, `EmbeddedLinkPlayer` (WebView) for TikTok/FB/`GENERIC` (your site). Hold back 2s to exit, `BackHandler`.

**Parent (PIN `ParentAuthStore.kt:1` SHA-256):**
* `ParentUnlockScreen.kt:1` → `ParentDashboardScreen.kt:1` (stats `primaryContainer/secondaryContainer/tertiaryContainer`, FAB `Add`, `Edit ✏️` dialog `EditMediaDialog` with title + shelf dropdown + `copyWith` for all 8 types, `Record` shortcut)
* `AddContentScreen.kt:1` — `OpenDocument` (persistable, fixes `configuration error 153` from `GetContent`), **Record** `MediaStore.ACTION_VIDEO_CAPTURE/IMAGE_CAPTURE` (`StartActivityForResult` + bitmap fallback to `cacheDir/capture_*.jpg`), `Pick/Browse`, **Link**: `OutlinedTextField` with `✓ Detected: PLATFORM` (`UrlParser.kt:38` handles `youtu.be?si=`, `m.youtube`, `shorts?feature=share`, `fb.watch`, `share/v` → `GENERIC`), chips `Try sample MP4`.
* `ManageShelfScreen.kt`, `ParentSettingsScreen.kt` (daily limit 0-180, bedtime `20:00→07:00`, session, shorts/online toggles), `WatchHistoryScreen.kt`.

**Data:** `AppDatabase.kt:1` Room `media_items/type/sourceUri/externalUrl/platformVideoId/thumbnailPath/shelfId/lastWatchedPositionMs/isFavorite`, `shelves`, `watch_logs`. `DataStore` encrypted PIN + `SettingsStore.kt:1`.

---

## 6) Adding Content (Parent)

1. `Parents` (top bar, PIN 4-6 digits) → `Add Content`
2. **Record:** `Record Video` / `Take Photo` → system camera → auto thumbnail (`MediaMetadataRetriever` 1s frame → `cache/thumb_local_*.jpg`)
3. **Pick:** `Pick Video/Image` / `Browse` (`OpenDocument` `arrayOf("video/*")`)
4. **Link:** paste `https://.../video.mp4` (→ `ONLINE_VIDEO` ExoPlayer), `https://picsum…` (→ `ONLINE_IMAGE` Coil), `https://www.youtube.com/watch?v=ID` / `https://youtu.be/ID?si=` / `https://www.youtube.com/shorts/ID`, `https://www.tiktok.com/@user/video/ID`, `https://fb.watch/…` / `https://your-site.com/watch/123` (→ `YOUTUBE/TIKTOK/FACEBOOK/GENERIC` WebView/youtube-player). Chip shows `✓ Detected`.
5. Choose shelf → `Add Link (in-app playback)`.

**Tip:** YouTube `Share → Copy link` full `https://` (not `youtu.be` short with `?si=` still works). Facebook share `r` tokens → treated as `GENERIC` WebView (needs login, better use numeric `video.php?v=ID`).

---

## 7) Theming

`ui/theme/Color.kt:19` light `FFFFFBF0/FFFFFF` vs dark `121417/1C1F22` + `Theme.kt:13` `lightColorScheme/darkColorScheme` (`primary KidYellowDark/primaryContainer KidYellow`, etc.). `SocialBabyTheme(darkTheme=isSystemInDarkTheme())` auto. All screens use `MaterialTheme.colorScheme` (no hardcoded `Color(0xFFFFFBF0)`).

**Logo:** `res/drawable/social_kids_logo_red.xml:1` VectorDrawable `512` (gradient `FF7E7E→FF2E4D→A30E26`, folder, play `66r`, face+blush, sparkles). Source SVG `assets/social_kids_logo_red.svg:1`. Launcher `AndroidManifest.xml:23` `mipmap-anydpi-v26/ic_launcher.xml:3` → `@drawable/social_kids_logo_red`.

---

## 8) Embedded Video Notes

* **YouTube** now via `YoutubePlayer.kt:1` `YouTubePlayerView` (`enableAutomaticInitialization=false`, `lifecycle.addObserver`, `loadVideo/mute`, `ENDED loop`, `onError UNKNOWN/150/101/153` → fallback `EmbeddedLinkPlayer(GENERIC)` loading original watch page). Previous `WebView iframe_api / youtube-nocookie` was fragile (autoplay blocked, `hashCode` IDs).
* **TikTok/Facebook/Generic:** `EmbeddedLinkPlayer.kt:34` `loadUrl(embedUrl)` direct (`youtube-nocookie/embed/...?mute=1&origin`, `tiktok.com/embed/v2`, `facebook.com/plugins/video.php?href=enc`), `WebSettings` `javaScript/domStorage/allowUniversalAccess/mediaPlaybackRequiresUserGesture=false/mixedContent ALWAYS_ALLOW` + `CookieManager.setAcceptThirdPartyCookies(WebView)` (was `WebSettings` leak), `hardwareAccelerated`.
* **Online `mp4`:** `LocalVideoPlayer.kt:57` `DefaultHttpDataSource.Factory` `Mozilla/5.0 SocialKids` `15000ms`, `MimeTypes` inference, error `2004 BAD_HTTP 403` (HTML page pasted as `ONLINE_VIDEO`) → `Open as Web Page` button (`fallbackToWebView` → `EmbeddedLinkPlayer(GENERIC)`), `152-154` → `GENERIC` fallback. `usesCleartextTraffic=true` `AndroidManifest.xml:20` for `http`.

---

## 9) Publish (Play Console) — you have account

See also: Play Console `Create app` → `Social Kids`, `Designed for Families` `5 & under`.

* **Store listing:** icon `512`, feature `1024x500`, screenshots `1080x1920`, short/full desc (mention local-only, no ads).
* **Content rating:** `Family` `5+`.
* **Data safety:** `No collection` (local only).
* **Target audience:** `5 & under` + `Designed for Families`.
* **Privacy:** `https://your-site.com/privacy` (no collection).
* **Upload:** `Internal testing → Production` `.aab`.
* **Update:** bump `versionCode+1` `versionName` `app/build.gradle.kts:15`, `bundleRelease`, `Production → Create new release`.

---

## 10) Troubleshooting

* **`packageDebugResources: file name must end with .xml`** → `.svg` in `res/drawable` invalid; use `.xml` VectorDrawable (kept `.svg` in `assets/`).
* **`153 / 2004`** → HTML page as `ONLINE_VIDEO` → re-add as `YouTube/Facebook/Generic` (now auto `GENERIC` WebView). `403` → signed Facebook/TikTok link expired → re-copy fresh link or use direct `.mp4`.
* **`Permission lost`** → `GetContent` before fix → re-add via new `OpenDocument` build, reinstall.
* **Back not working** → `BackHandler` added `Shorts/Photo/Player`.
* **Shorts not auto-playing** → `isCurrentPage` + `loop/mute` fixed.

---

## 11) Project Structure

`com.example.socialbaby` `KidTubeApp.kt` `MainActivity.kt` `di/` `data/local/entity|dao|datastore` `data/remote/OEmbedApi` `data/mapper` `data/repository` `domain/model|repository|usecase` `ui/theme|navigation|child|parent` `util/UrlParser|SafUriPersister` `worker/`

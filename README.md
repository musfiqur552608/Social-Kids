# Social Kids — Local-First, Parent-Curated Media Player

> **A safe, offline-first alternative to YouTube / TikTok for children.** Parents curate 100% of content (local videos/photos + approved links). No algorithm, no search, no comments, no ads, no account. Child gets a big-button, swipeable player with resume, favorites, and time-limit guards.

* **App Name:** `Social Kids` (`app/src/main/res/values/strings.xml:2`)
* **Package:** `com.example.socialbaby` → change to `com.socialkids.app` before Play publish (`app/build.gradle.kts:10`)
* **Version:** `1.0` (`versionCode 1` `app/build.gradle.kts:15`) | `targetSdk 37` `minSdk 24`
* **Stack:** Kotlin 2.2.10, Jetpack Compose + Material 3, Media3 ExoPlayer, Coil, Room + Paging3, DataStore, Hilt, Navigation-Compose, WorkManager, Biometric, Retrofit/OkHttp, `android-youtube-player:12.1.0`

---

## 1. Purpose

Replace open browsing with **Curate, Don't Crawl**:
* Parent pastes a link → app stores link + metadata locally, plays via **official embed** (YouTube `youtube-nocookie` + `android-youtube-player`, TikTok `embed/v2`, Facebook `plugins/video.php`) inside a `WebView` (`EmbeddedLinkPlayer.kt:1`). Child never sees host chrome/comments/up-next.
* Local videos/photos (`content://` via SAF `OpenDocument`) play natively via **ExoPlayer/Coil** — 100% offline, persistent `Uri` permission (`SafUriPersister.kt:1`).

Explicit non-goals: no downloading/re-hosting, no open search in Child, no social/comments/upload, no ad/tracking SDK, no AI recommendations.

## 2. Two Modes

### Child Mode (default, `KidTubeNavHost` `MainActivity.kt:123`)
* **Home:** 2-col grid of big thumbnails, shelf `FilterChip`s (`All`, `Long Videos`, `Shorts`…), banner gradient, `Shorts`/`Photos` cards. Empty → `Go to Parent Mode`.
* **Shorts:** `VerticalPager` `ShortsFeedScreen.kt:1` — **same-page autoplay** like Reels: `Local/Online Video → LocalVideoPlayer(autoPlay, looping, muted)` when `isCurrentPage`, YouTube via `YoutubePlayer` (muted), images via `AsyncImage`. Right `like`/`mute`, bottom `VIDEO/WEB` badge, `Playing • swipe` hint.
* **Photos:** `HorizontalPager` `PhotoGalleryScreen.kt:1` pinch-zoom, swipe.
* **Player:** `PlayerScreen.kt:1` routes `LocalVideoPlayer` (`LOCAL/ONLINE_VIDEO` + `GENERIC` fallback WebView) / `YoutubePlayer` / `EmbeddedLinkPlayer` / `AsyncImage`. Resume from `lastWatchedPositionMs`, `Hold 2s` exit (BackHandler + `BackHandler` in all child screens), `Favorite`, no share/comments.
* **Guards:** `EnforceTimeLimitUseCase.kt:1` (`dailyLimit`, `bedtime window`), `ScreenPinningManager.kt:1` `startLockTask` optional, `Back` trapped to Home.
* Accessibility: `48–96dp` targets, high contrast, `TalkBack`.

### Parent Mode (PIN/Biometric gated `ParentAuthStore.kt:1` SHA-256)
* **Unlock:** `ParentUnlockScreen.kt:1` 4-6 digit PIN pad, `BiometricPrompt` optional.
* **Dashboard:** `ParentDashboardScreen.kt:1` stats (`primaryContainer/secondaryContainer/tertiaryContainer`), `Add/Record/Shelves/Settings/History` actions, 2-col `ParentMediaCard` with preview (`AsyncImage` or `MediaMetadataRetriever` thumb), badge `LOCAL/PHOTO/ONLINE/WEB/YT`, `Edit ✏️` dialog (`EditMediaDialog` title + shelf `ExposedDropdown` → `copyWith` for all 8 types), `▶` preview, `★`/`🗑`.
* **Add Content:** `AddContentScreen.kt:1` — **Record** `ACTION_VIDEO_CAPTURE`/`IMAGE_CAPTURE` (`StartActivityForResult` + bitmap fallback to `cache/capture_*.jpg`), **Pick** `OpenDocument(arrayOf("video/*"))` (persistable, fixes `153` from `GetContent`), **Browse**, **Link** `OutlinedTextField` with `✓ Detected: PLATFORM` (`UrlParser.kt:38` handles `youtu.be?si`, `m.youtube`, `shorts?feature`, `fb.watch`, `share/r` → `GENERIC`), chips `Try sample MP4`. Link types: direct `mp4/m3u8/webm` (`ONLINE_VIDEO` ExoPlayer), `jpg/png` (`ONLINE_IMAGE`), `youtube-nocookie`/`tiktok`/`facebook` embed, any `https` → `GenericLink` WebView (fixes `configuration error 153` where HTML was parsed as video).
* **Manage Shelf:** `ManageShelfScreen.kt:1` create/delete `ShelfEntity` (`SHELF/PLAYLIST/SHORTS_FEED/PHOTOS`).
* **Settings:** `ParentSettingsScreen.kt:1` daily limit `0-180` slider, bedtime `20:00→07:00` switch, session, `shorts/online` toggles. Safety note local-only.
* **History:** `WatchHistoryScreen.kt:1` `watch_logs` sum since midnight, `formatDuration`, clear.

## 3. Data (100% local `AppDatabase.kt:1` Room)
```
MediaItemEntity(id, type[LOCAL_VIDEO/LOCAL_IMAGE/YOUTUBE/TIKTOK/FACEBOOK/ONLINE_VIDEO/ONLINE_IMAGE/GENERIC_LINK],
  sourceUri, externalUrl, platformVideoId, title, thumbnailPath, shelfId FK, sortOrder,
  dateAdded, lastWatchedPositionMs, lastWatchedAt, totalWatchTimeMs, isFavorite)
ShelfEntity(id, name, kind[SHELF/PLAYLIST/SHORTS_FEED/PHOTOS], sortOrder)
WatchLogEntity(id, mediaItemId FK, watchedAt, durationMs)
```
`typeName()` `domain/model/MediaItem.kt:122` sealed `LocalVideo/LocalImage/Youtube/TikTok/Facebook/OnlineVideo/OnlineImage/GenericLink`. Mapper `data/mapper/MediaItemMapper.kt:1`.

**Storage:** `Room` + `Scoped Storage` (no copy unless SD card), `DataStore` encrypted PIN (`ParentAuthStore`) + settings (`SettingsStore.kt:1`), `Coil` disk, `WorkManager` thumbnail cleanup `ThumbnailCacheCleanupWorker.kt:1`, daily reset `DailyWatchTimeResetWorker.kt:1`.

**Network:** one-time `OEmbedApi.kt:1` (`Retrofit` `converter-gson`) + thumbnail cache, plus `WebView` only while watching that link. `onlineLinksEnabled=false` → 100% offline.

## 4. Architecture
```
com.example.socialbaby
├── KidTubeApp.kt (@HiltAndroidApp)
├── MainActivity.kt (NavHost, seed 4 shelves + 5 samples)
├── di/ DatabaseModule.kt, DataStoreModule.kt, NetworkModule.kt, RepositoryModule.kt
├── data/local/entity|dao|datastore, remote/dto, mapper, repository/*Impl.kt
├── domain/model, repository (interfaces), usecase (AddLocalMedia, AddOnlineLink, GetShelfContent, SaveWatchProgress, EnforceTimeLimit, VerifyParentAuth)
├── ui/theme (Color.kt light #FFFFFBF0 vs dark #121417, Theme.kt isSystemInDarkTheme auto, Shape.kt, Type.kt)
├── ui/navigation (Root/Child/Parent NavGraph, ChildRoute/Home/Shorts/Photos/Player, ParentRoute/Unlock/Dashboard/Add/Manage/Settings/History)
├── ui/child, ui/parent, worker, util (UrlParser, SafUriPersister, ScreenPinningManager)
```

## 5. Video Pipeline
* **Local:** SAF `OpenDocument` → `SafUriPersister.persist` → `AddLocalMediaUseCase.kt:1` `MediaMetadataRetriever.getFrameAtTime(1s)` → `cache/thumb_local_*.jpg` → `MediaRepositoryImpl.insert`.
* **Online:** `UrlParser.parse` → `AddOnlineLinkUseCase.kt:21` `OEmbedApi` once → cache thumb `cache/thumb_*` → insert `YOUTUBE/TIKTOK/FACEBOOK/ONLINE_VIDEO/ONLINE_IMAGE/GENERIC`.
* **Playback:** `PlayerScreen` picks `LocalVideoPlayer` (ExoPlayer `DefaultDataSource.Factory` `Mozilla/5.0 SocialKids` `15000ms`, `MimeTypes` inference, `ERROR_CODE_IO_BAD_HTTP_STATUS 2004/403` → `Open as Web Page` fallback) or `YoutubePlayer` (`android-youtube-player` `12.1.0` with `VIDEO_NOT_PLAYABLE 150/101/153` fallback to `EmbeddedLinkPlayer(GENERIC)`) or `EmbeddedLinkPlayer` (`WebView` `youtube-nocookie/embed/...?mute=1&origin`, `tiktok/embed/v2`, `facebook/plugins/video.php?href=enc`, `GENERIC` `loadUrl(externalUrl)`), `CookieManager.setAcceptThirdPartyCookies(WebView)` (was `WebSettings` leak), `hardwareAccelerated`, `onReceivedError` fallback to watch page.
* **Shorts:** `LocalVideoPlayer(muted, looping, isCurrentPage)` auto-plays only current `VerticalPager` page.

## 6. Tech Stack (fixed)
Kotlin 2.2.10, Compose BOM 2024.09.03, M3 (KidShapes 12-32dp, KidTypography), Media3 1.2.1, Coil 2.7.0, Room 2.8.1 + KSP 2.2.10-2.0.2, DataStore 1.1.1, Hilt 2.56.2, Navigation 2.8.4, Paging 3.2.1, Work 2.9.0, Biometric 1.1.0, Retrofit 2.11.0/OkHttp 4.12.0, youtube-player 12.1.0, Security Crypto.

## 7. Setup — see `setup.md:1` for full steps
```bash
# JDK 17+, Android Studio Ladybug+ (AGP 9.3.2), SDK 37
git clone <repo> && cd SocialBaby2
./gradlew :app:assembleDebug # → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:bundleRelease # → app/build/outputs/bundle/release/app-release.aab (Play)
# Run on device API 24+ (USB debugging) or emulator Pixel 7 API34
```
First launch: Home → `Parents` → set PIN → `Add Content` → `Record`/`Pick`/`Link` (watch chip `✓ Detected`).

## 8. Theming
`ui/theme/Theme.kt:13` `SocialKidsLightScheme` `KidYellowDark/primaryContainer KidYellow` vs `DarkScheme` `KidYellow/primaryContainer KidYellowDark`, `surfaceContainer #F8F0DA/#252A2E`. All screens `MaterialTheme.colorScheme` (no hardcoded `0xFFFFFBF0`). `SocialBabyTheme(darkTheme=isSystemInDarkTheme())` auto. Logo `res/drawable/social_kids_logo_red.xml:1` VectorDrawable `512` (gradient `FF7E7E→A30E26`, folder, play `66r`, face+blush, sparkles) source `assets/social_kids_logo_red.svg:1`, launcher `AndroidManifest.xml:23` `mipmap-anydpi-v26/ic_launcher.xml:3`.

## 9. Permissions (`AndroidManifest.xml:5`)
`INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA`, `RECORD_AUDIO` (record), `READ_MEDIA_VIDEO/IMAGES` (+ `READ_EXTERNAL_STORAGE` max 32), `USE_BIOMETRIC`, `hardwareAccelerated`, `usesCleartextTraffic=true`. No `REQUEST_INSTALL_PACKAGES`/`MANAGE_EXTERNAL_STORAGE`.

## 10. Publish (Play Console — you have account)
*Change `com.example.*` (blocked) → `com.socialkids.app`.*
1. Keystore: `keytool -genkey -keystore socialkids.jks -alias socialkids` + `signingConfigs release` `app/build.gradle.kts:30`
2. Console `Create app` → `Social Kids` → `Designed for Families 5 & under` → `Free`
3. Checklist: **Store listing** (icon 512, feature 1024x500, screenshots Home/Shorts autoplay/Photos/Parent), **Content rating** Family 5+, **Data safety** `No collection`, **Target audience** 5 & under, **Privacy** `https://.../privacy` (local-only), **Ads** `No`
4. `Testing → Internal → Production` upload `app-release.aab` → `Review` (1-7d families)
5. **Update:** bump `versionCode+1` `versionName` `app/build.gradle.kts:15` → `bundleRelease` → `Production → Create new release`

## 11. Troubleshooting
* `file name must end with .xml` → `.svg` in `res/drawable` invalid → use `social_kids_logo_red.xml` + keep `.svg` in `assets/`
* `153/2004` → HTML page as `ONLINE_VIDEO` → now `GENERIC` WebView; `403` signed FB link expired → re-copy fresh numeric `video.php?v=ID`; `GetContent` permission lost → re-add via new `OpenDocument` build, reinstall clears old `hashCode` IDs
* Back not working → `BackHandler` added `Shorts/Photo/Player`
* Shorts not autoplay → `isCurrentPage` + `loop/mute` fixed; YouTube `YoutubePlayer` muted autoplay

## 12. License & Contributing
No ad/tracking SDK. Keep `Curate, Don't Crawl` (no scraping). PRs welcome; run `./gradlew :app:assembleDebug` before push.

---

*Built as runnable Android Studio (Compose) project per `KidTube — Architecture` doc.*

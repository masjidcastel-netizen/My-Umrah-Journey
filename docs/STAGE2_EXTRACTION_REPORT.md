# Stage 2 Extraction Report

**Date:** 2026-11-03
**Repository:** `masjidcastel-netizen/My-Umrah-Journey`, branch `native-android-development`
**Audit commit read:** `806b1e333bdeac82bbffcebbe793e6d0e6516cae` (`docs/STAGE2_ARCHIVE_AUDIT.md`)
**Scope of this report:** read-only inspection. Nothing was imported, replaced or deleted. `main` was not changed.

## How to read this report

Every finding is tagged:

- **[VERIFIED]** I observed it directly: by reading the extracted files, running a command, or compiling and running code here.
- **[ASSUMPTION]** It is plausible or stated by someone else (for example the Stage 2 README) but I did not confirm it.
- **[NOT VERIFIED]** It could not be checked in this environment.

**Headline: the Stage 2 project has never been built by Gradle, by me or (per its own README) by anyone. It must not be described as compiling.** The environment used for this inspection has no Android SDK and cannot reach Google Maven, Maven Central or Gradle services.

---

## 1. Summary

| Question | Answer |
|---|---|
| Is the ZIP in GitHub? | **Yes** [VERIFIED]. `MyUmrahJourney-stage2 (1).zip` is on `main` (commit `f7a4743`) and on this branch. Nothing needs uploading. |
| Is it the same file you attached in chat earlier? | **Yes** [VERIFIED]. SHA-256 `60a173081391c8d6b7963f4cbbc748d36d85be9875701defd60cf50a49b612c5` for both. |
| Is it a genuine native Android + Kotlin + Compose + libGDX project? | **Yes, by inspection** [VERIFIED]. No WebView, HTML, CSS or JavaScript anywhere in code or config. The game runs in `GameLauncherActivity : AndroidApplication` with `ModelBatch`, `Environment` and `DirectionalShadowLight` (libGDX 3D on OpenGL ES 2.0). |
| Does it compile as an Android app? | **Unknown** [NOT VERIFIED]. |
| Does the game logic work? | The simulation, collision and world-generation code and its **28 unit tests compile and pass** under Kotlin 2.0.21 + JUnit 4.13.2 [VERIFIED, run here, see section 8]. This does not exercise libGDX or Android. |
| Is the repository ready to build it? | **No.** The branch holds only a README, the ZIP and the audit. There is no `codemagic.yaml` and the source is not extracted [VERIFIED]. |

---

## 2. Archive facts [VERIFIED]

| Item | Value |
|---|---|
| File | `MyUmrahJourney-stage2 (1).zip`, 139,522 bytes |
| SHA-256 | `60a173081391c8d6b7963f4cbbc748d36d85be9875701defd60cf50a49b612c5` |
| Entries | 135 = 65 files + 70 directory entries |
| Uncompressed size of files | 250,826 bytes |
| Top-level folder | `MyUmrahJourney/` (single root; nothing outside it) |
| Extracted to | a temporary directory outside the repository; the repository was not modified by extraction |
| Binary assets | **None** except `gradle/wrapper/gradle-wrapper.jar` (no images, audio, 3D models or fonts) |
| Source | 31 `.kt` files, 3,963 lines: `app` 9 main, `core` 5 main + 2 test, `game` 13 main + 2 test |

The audit's description of the archive (modules `app`, `core`, `game`, Gradle wrapper, documentation, the listed source and test paths, localisation resources) is **confirmed**. Its "NOT VERIFIED" items are addressed below.

---

## 3. Verified technology versions

| Component | Version | Where [all VERIFIED by reading the files] |
|---|---|---|
| Gradle (wrapper distribution) | **8.10.2** | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | **8.7.3** | `gradle/libs.versions.toml` |
| Kotlin | **2.0.21** (Compose compiler via `org.jetbrains.kotlin.plugin.compose` 2.0.21) | `libs.versions.toml` |
| libGDX | **1.12.1** (`gdx`, `gdx-backend-android`, natives `gdx-platform:1.12.1` for armeabi-v7a, arm64-v8a, x86, x86_64) | `libs.versions.toml`, `game/build.gradle.kts` |
| Compose BOM | 2024.12.01 (Material 3, UI) | `libs.versions.toml` |
| Navigation Compose | 2.8.5 | `libs.versions.toml` |
| Lifecycle | 2.8.7 | `libs.versions.toml` |
| Activity Compose | 1.9.3 | `libs.versions.toml` |
| DataStore Preferences | 1.1.1 | `libs.versions.toml` |
| Coroutines | 1.9.0 | `libs.versions.toml` |
| JUnit | 4.13.2 | `libs.versions.toml` |
| compileSdk / targetSdk / minSdk | 35 / 35 / **26** (Android 8.0, meets the spec's "Android 8 and later") | module `build.gradle.kts` files |
| Java/Kotlin JVM target | 17 | module `build.gradle.kts` files |
| App version | `0.2.0-stage2`, versionCode 1 | `app/build.gradle.kts` |

**Compatibility reasoning [ASSUMPTION, from published version requirements, not from a build]:**

- AGP 8.7.3 needs Gradle 8.9 or newer and JDK 17: satisfied by Gradle 8.10.2 and a JDK 17 toolchain.
- AGP 8.7.x supports compileSdk 35. The Compose BOM 2024.12.01 libraries require compileSdk 35: satisfied.
- Kotlin 2.0.21 with the Compose compiler Gradle plugin matches the Kotlin version: satisfied.
- libGDX 1.12.1 `gdx-backend-android` supports minSdk 26 and needs no AndroidX.

**Application ID and namespaces [VERIFIED]**

| Item | Value |
|---|---|
| `applicationId` | `com.masjidcastel.myumrahjourney` (only in `app/build.gradle.kts`). **Already correct; no change needed.** |
| Namespaces | `...myumrahjourney.app`, `.core`, `.game` (unchanged Kotlin packages) |
| Launcher activity | `...myumrahjourney.app.MainActivity` (`ComponentActivity`, Compose) |
| Game activity | `...myumrahjourney.game.GameLauncherActivity` (declared in the `game` manifest, merged into the app; `exported=false`, `sensorLandscape`, fullscreen theme) |
| Application class | `.MyUmrahJourneyApplication` (defined in `App.kt`; the manifest reference resolves) |
| Manifest feature | `uses-feature glEsVersion=0x00020000 required=true` (OpenGL ES 2.0) |

**Wrapper jar and checksum [VERIFIED facts, NOT VERIFIED provenance]**

- `gradle-wrapper.jar`: 43,764 bytes, SHA-256 `7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`.
- Its manifest has no `Implementation-Version` and contains `Enable-Native-Access: ALL-UNNAMED`.
- `gradle-wrapper.properties` has **no `distributionSha256Sum`**.
- I could not confirm the jar belongs to Gradle 8.10.2 (the official checksum list is not reachable from here). A wrapper jar from a different Gradle release normally still launches the 8.10.2 distribution, but this is an **assumption**.

---

## 4. Module structure [VERIFIED]

`settings.gradle.kts` includes `:app`, `:core`, `:game`. Repositories: Google (restricted to `com.android*`, `com.google*`, `androidx*`), Maven Central, Gradle Plugin Portal; `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.

```
:app  (com.android.application, Compose + Material 3)
 |-- implementation :core
 |-- implementation :game
:game (com.android.library, libGDX)
 |-- implementation :core
 |-- api gdx 1.12.1, gdx-backend-android 1.12.1
 `-- natives configuration -> copyGdxNatives task -> build/generated/gdxNatives (registered as jniLibs)
:core (com.android.library, no UI)
 |-- api datastore-preferences 1.1.1, kotlinx-coroutines-android 1.9.0
 `-- implementation core-ktx
```

The `game` -> `core` and `app` -> `game` links go through a small Intent contract (`GameLaunchContract`): player name, language code, sound/music flags and graphics quality. No circular dependencies.

---

## 5. Important source files [VERIFIED by reading]

| File | Lines | Role |
|---|---:|---|
| `game/.../DrivingGame.kt` | 274 | libGDX `ApplicationAdapter`: fixed 120 Hz simulation step, camera, shadow pass, main 3D pass, HUD; pause on app background |
| `game/.../GameLauncherActivity.kt` | 61 | `AndroidApplication`; RGBA8888, 24-bit depth, optional 2x MSAA; reads settings from the Intent; saves quality changes to DataStore |
| `game/.../sim/Vehicle.kt` | 129 | Arcade bicycle-model driving: throttle, brake, reverse, speed-dependent steering, rolling resistance and drag |
| `game/.../sim/Collision.kt` | 276 | Oriented-rectangle car vs box and circle colliders (separating-axis test), grid broad-phase, push-out, bounce, wall-sliding |
| `game/.../sim/VehicleSpec.kt` | 21 | Shared car dimensions for collision and model |
| `game/.../world/WorldLayout.kt` | 415 | Deterministic generated town (pure Kotlin, no libGDX) |
| `game/.../render/WorldScene.kt` | 261 | Builds batched, frustum-culled 80 m mesh chunks with libGDX `ModelBuilder` |
| `game/.../render/CarModel.kt` | 153 | Car body plus four separate wheel nodes: spin with distance, front pair steers |
| `game/.../render/CameraRig.kt` | 115 | Chase, Far and Driver cameras, lagged follow yaw, FOV by speed, impact shake |
| `game/.../render/QualityProfile.kt` | 26 | Low/Medium/High: draw distance 200/320/480 m, shadows off/1024/2048, MSAA 0/0/2 |
| `game/.../render/ProceduralTextures.kt` | 80 | Grass, asphalt, facade textures generated in code (managed, so they survive GL context loss) |
| `game/.../ui/GameUi.kt` | 314 | libGDX-drawn touch controls, HUD, pause menu; multi-touch polling |
| `core/.../data/DataStoreRepository.kt` | 91 | Preferences DataStore for name, language, sound, music, graphics quality, last-saved time |
| `core/.../language/LanguageManager.kt` | 79 | English, French, Mauritian Creole (`mfe`), Arabic with RTL flag; in-process locale switching |
| `app/.../App.kt`, `navigation/Navigation.kt` | 100, 154 | Compose root, language/layout-direction provider, bottom navigation, "Start journey" launches the game Activity |
| `app/.../ui/screens/*` | | Home, Settings (saves via ViewModel), About |

---

## 6. Existing Stage 2 functionality

**Present in code [VERIFIED by reading; behaviour on a device NOT VERIFIED]**

- 3D scene: 480 m x 480 m playable area, 3x3 road grid with lane markings, buildings (houses, shops, towers), palm and round trees, lamp posts, barriers, boulders, boundary walls, backdrop hills.
- Drivable car: throttle, brake, reverse (hold brake at standstill), speed-sensitive steering, four rotating wheels, front-wheel steering visuals.
- Collisions with buildings, trees, lamps, barriers and walls, with impact flash, camera shake, impact speed and a hit counter.
- Three cameras (Chase, Far, Driver) with a camera button.
- On-screen controls: left/right arrow buttons, accelerator and brake pedals, pause button; keyboard fallback for emulators.
- Pause menu: Resume, Graphics (Low/Medium/High, saved to DataStore), Exit to app. Pauses automatically when the app goes to the background.
- Speed display, FPS counter, hit counter.
- Compose app: Home, Settings, About; four languages with RTL support for Arabic; saved settings.

**Not present (needed by the master specification)**

| Specification item | Stage 2 status |
|---|---|
| Walking/running character, entering and exiting vehicles, NPCs, shops, missions, wallet | Absent. Only the car exists. |
| Virtual steering wheel, tilt steering (accelerometer is off: `useAccelerometer = false`) | Absent (planned Stage 3) |
| Traffic and road interactions, vehicle sounds, any audio | Absent |
| Mauritius, Saudi Arabia, other countries | Absent. The level is a generic prototype town, not Mauritian or Saudi. |
| Umrah, Hajj, Madinah, prayer, Qur'an, daily-life, quiz, level/achievement, progression systems | Absent (Compose screens are Home/Settings/About only) |
| Day/night lighting, weather | Absent |
| In-game localisation | Absent. HUD and pause text are hard-coded English; the built-in libGDX font has no Arabic glyphs. The 4-language support is in the Compose app only (44 strings, plus the one toast string in `game`). |
| Gender selection, clothing options | Absent |
| Full navigation list (Explore, Umrah, Hajj, Prayer, Qur'an, Learning, Activities, Profile) | Absent (3 destinations only) |
| Models/assets | All geometry and textures are procedural primitives; there are no GLB/glTF or external assets. |

---

## 7. Gaps between the Stage 2 README and what I found

| README claim | Finding |
|---|---|
| "5 + 28 JVM tests" | 28 game tests [VERIFIED: 18 + 10]. The `core` module has **10** tests (5 DataStore + 5 language), not 5. So 38 in total [VERIFIED by counting `@Test`]. Minor documentation error. |
| 28 game tests pass | **Confirmed**, I ran them (section 8). |
| Strings present in all 4 languages with matching placeholders; XML well-formed | **Confirmed** with my own script: 44 keys in each of the four `app` locales, 1 key in each of the four `game` locales, zero missing, zero extra, zero placeholder mismatches; every XML file parses. |
| libGDX natives are not 16 KB page-aligned | Stated in the README [ASSUMPTION]. Plausible and relevant for Google Play (targetSdk 35). Not a debug-build blocker. |

---

## 8. What I ran (all inside a temporary directory; nothing written to the repository)

| Check | Result |
|---|---|
| Compiled `game/sim/*.kt` and `game/world/*.kt` (no libGDX dependency) with Kotlin 2.0.21 | **Success, no errors** |
| Compiled the two game test files with JUnit 4.13.2 | **Success** |
| Ran `DrivingSimulationTest` + `WorldLayoutTest` with JUnit | **`OK (28 tests)`** |
| String and XML check across `app` and `game` resources | **Pass** (see section 7) |
| Search for `WebView`, `webkit`, `javascript`, `loadUrl`, `.html`, `.js`, `.css` in Kotlin, Gradle, XML and TOML files | **None found.** No HTML/JS/CSS files exist in the archive. |
| Check that `Icons.Filled.*` used by the app exist in `material-icons-core` | `Home`, `Info`, `Settings`, `PlayArrow` are all in the core icon set [ASSUMPTION from my knowledge of that library, not compiled]. |

**Not run:** the 10 `core` tests (they need the DataStore libraries, which are not available offline), any Android/libGDX/Compose code, Gradle, lint, or an APK build.

---

## 9. Build blockers and dependency problems

### Blockers (the debug APK cannot be produced until these are resolved)

1. **No build has ever succeeded** [VERIFIED: README and audit both say so; I could not build either]. Treat every Android-facing file as unbuilt.
2. **No Android SDK and no network access to Google/Gradle/Maven in this environment** [VERIFIED]. The real build must run on Codemagic (or a machine with Android Studio).
3. **No CI configuration in the repository** [VERIFIED]. `codemagic.yaml` does not exist yet. (A draft exists from earlier work, see section 11.)
4. **No `local.properties` / SDK path** [VERIFIED]. Codemagic needs `sdk.dir` written from `$ANDROID_SDK_ROOT`, or Android Studio generates it. `.gitignore` correctly excludes it.

### Likeliest first-build failures [ASSUMPTION: reasoning from code, not from a build]

| # | Spot | Why it may fail |
|---|---|---|
| a | `game/build.gradle.kts`, `copyGdxNatives` | Custom task. It uses `project.copy {}` inside `doLast` (deprecated in Gradle 8.x, removed in Gradle 9, so fine on 8.10.2) and assumes `libgdx.so` sits at the root of each natives jar (true for libGDX's standard layout, unconfirmed here). If the jars have another layout the APK would lack `libgdx.so` and crash at start. The build itself might still succeed, so check the APK: `unzip -l app-debug.apk | grep libgdx`. |
| b | libGDX API usage in `DrivingGame.kt`, `WorldScene.kt`, `MeshKit.kt`, `CarModel.kt`, `GameUi.kt`, `ProceduralTextures.kt` | Not compiled against the real 1.12.1 jars. Constructors and signatures I could check from memory (`DirectionalShadowLight`, `ModelBuilder.part`, `PixmapTextureData`, `MeshPartBuilder.box/cylinder/sphere/setUVRange`, `Usage.ColorUnpacked`) look right, but a compile error is plausible. |
| c | Runtime rendering | Vertex-colour meshes with fog and a shadow map on `DefaultShader` / `DepthShaderProvider` under OpenGL ES 2.0 are untested on any GPU. Shadow creation already has a fallback if the depth framebuffer fails. |
| d | Wrapper jar provenance | Unknown (section 3). If the wrapper fails to start the distribution, regenerate it. |

### Dependency notes

- No dependency conflict is evident from the declared versions [ASSUMPTION].
- `kotlinOptions {}` is used (fine on Kotlin 2.0.21; deprecated in later Kotlin versions).
- All four ABIs are packaged (no `abiFilters`), so the APK will be larger; acceptable for now.
- `isMinifyEnabled = false` for release; no ProGuard rules for libGDX needed while that stays false.
- libGDX 1.12.1 natives are reported as not 16 KB-aligned [ASSUMPTION]: plan to move to a newer libGDX before a Play Store submission. This conflicts with the spec's pinned "libGDX 1.12.1", so **it needs your decision later**, not now.
- Spec says OpenGL ES 3D rendering; the project requires GLES 2.0, which is a valid subset and works on all supported phones.

---

## 10. Import risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Path mismatch (ZIP root is `MyUmrahJourney/`; repository root has only README, ZIP, docs) | Certain, but trivial | Decide the target layout before extracting (section 11 asks you to choose) |
| Accidentally modifying or deleting the ZIP | Low | Keep the ZIP untouched on both branches; import from a fresh extraction and compare byte for byte afterwards (`diff -r`) |
| Committing build output or secrets | Low | The project's `.gitignore` excludes `build/`, `.gradle/`, `local.properties`, `*.apk`, `*.aab`. Keystores are not covered, so add `*.jks` and `*.keystore` at the repository root before any signing work. |
| Wrapper jar unknown provenance | Medium | Verify or regenerate; add `distributionSha256Sum` |
| Stage 2 import "fixes" hide real faults | Medium | Commit the unmodified import first; make every fix a separate, documented commit |
| Overwriting the existing `README.md` | Low | Keep the repo `README.md`; the project README stays inside the project folder |
| Claiming success too early | Medium | Only a green Codemagic build plus an APK counts; record the device test results from the Stage 2 README (9 acceptance tests) |
| Earlier work on a different repository (`muhammadnasseersumun-alt/My-Umrah-Journey`) is unpushed there | Informational | That repository contains an older WebView/HTML app. It is not used here. My earlier Stage 1 files (Codemagic config and signing change) exist only as a zip and a git bundle I sent you; they can be re-applied here after approval. |

---

## 11. Recommended safe import plan (awaiting your approval)

**Decision needed from you: where should the project live?**

- **Option A (recommended): `MyUmrahJourney/` subfolder** at the repository root. Matches the ZIP's own top-level path, needs no path rewriting, keeps the audit's "do not flatten" rule, and leaves `docs/`, the ZIP and the README alone. Codemagic then uses `working_directory: MyUmrahJourney`.
- Option B: project contents at the repository root. Simpler for Android Studio, but moves every file and makes the repo root busy.

**Steps (each its own small commit on `native-android-development`):**

1. Re-extract the ZIP to a clean temporary directory; confirm SHA-256 `60a17308...49b612c5` and the 135-entry inventory below.
2. Copy `MyUmrahJourney/` into the repository unchanged. Run `diff -r` against the fresh extraction (must be identical). Commit: "Import Stage 2 project unchanged."
3. Add root `.gitignore` entries for `*.jks`, `*.keystore`, `*.p12`, `local.properties`, `.idea/`, `build/`. Commit.
4. Add `codemagic.yaml` with an `android-debug` workflow only (`:app:assembleDebug`, artifact `MyUmrahJourney/app/build/outputs/apk/debug/*.apk`), plus JDK 17 and `sdk.dir` setup. Commit. Validate syntax locally; do **not** claim it works.
5. Regenerate or verify the Gradle wrapper (`gradle wrapper --gradle-version 8.10.2` on a connected machine, and record its checksum). Commit separately, only if verification fails or you want to harden it.
6. You run the Codemagic build and send me the first error. I fix errors one per commit, each explained. No feature work.
7. When the APK builds: install on your phone, run the 9 Stage 2 acceptance tests from the project README, record device and results in `docs/`.
8. Only then start Stage 3 (driving upgrade). Release signing (keystore from Codemagic's secure store, never in Git) comes after a successful debug build.

**What will not happen:** no rewrite of Stage 2, no removal of working code, no change to `main`, no web code, no claim of a successful build before Codemagic reports one.

---

## 12. Complete archive inventory [VERIFIED, generated from the ZIP]

### Files (65, 250,826 bytes uncompressed)

| Path (inside ZIP) | Bytes |
|---|---:|
| `MyUmrahJourney/.gitignore` | 106 |
| `MyUmrahJourney/BUILD_ON_ANDROID.md` | 4,654 |
| `MyUmrahJourney/README.md` | 6,949 |
| `MyUmrahJourney/app/build.gradle.kts` | 1,714 |
| `MyUmrahJourney/app/proguard-rules.pro` | 167 |
| `MyUmrahJourney/app/src/main/AndroidManifest.xml` | 959 |
| `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/App.kt` | 4,002 |
| `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/MainActivity.kt` | 719 |
| `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/navigation/Navigation.kt` | 6,456 |
| `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/ui/components/ResponsiveColumn.kt` | 1,751 |
| `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/ui/screens/AboutScreen.kt` | 3,415 |
| `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/ui/screens/HomeScreen.kt` | 6,683 |
| `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/ui/screens/SettingsScreen.kt` | 12,074 |
| `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/ui/theme/Theme.kt` | 3,311 |
| `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/viewmodel/JourneyViewModel.kt` | 2,200 |
| `MyUmrahJourney/app/src/main/res/drawable/ic_launcher_foreground.xml` | 611 |
| `MyUmrahJourney/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | 341 |
| `MyUmrahJourney/app/src/main/res/values-ar/strings.xml` | 3,663 |
| `MyUmrahJourney/app/src/main/res/values-b+mfe/strings.xml` | 2,977 |
| `MyUmrahJourney/app/src/main/res/values-fr/strings.xml` | 3,202 |
| `MyUmrahJourney/app/src/main/res/values/colors.xml` | 173 |
| `MyUmrahJourney/app/src/main/res/values/strings.xml` | 2,961 |
| `MyUmrahJourney/app/src/main/res/values/themes.xml` | 506 |
| `MyUmrahJourney/app/src/main/res/xml/locales_config.xml` | 264 |
| `MyUmrahJourney/build.gradle.kts` | 298 |
| `MyUmrahJourney/core/build.gradle.kts` | 594 |
| `MyUmrahJourney/core/src/main/AndroidManifest.xml` | 111 |
| `MyUmrahJourney/core/src/main/kotlin/com/masjidcastel/myumrahjourney/core/data/DataStoreRepository.kt` | 4,186 |
| `MyUmrahJourney/core/src/main/kotlin/com/masjidcastel/myumrahjourney/core/language/LanguageManager.kt` | 3,024 |
| `MyUmrahJourney/core/src/main/kotlin/com/masjidcastel/myumrahjourney/core/launch/GameLaunchContract.kt` | 1,989 |
| `MyUmrahJourney/core/src/main/kotlin/com/masjidcastel/myumrahjourney/core/player/GraphicsQuality.kt` | 512 |
| `MyUmrahJourney/core/src/main/kotlin/com/masjidcastel/myumrahjourney/core/player/PlayerState.kt` | 885 |
| `MyUmrahJourney/core/src/test/kotlin/com/masjidcastel/myumrahjourney/core/data/DataStoreRepositoryTest.kt` | 4,000 |
| `MyUmrahJourney/core/src/test/kotlin/com/masjidcastel/myumrahjourney/core/language/LanguageManagerTest.kt` | 1,775 |
| `MyUmrahJourney/game/build.gradle.kts` | 2,129 |
| `MyUmrahJourney/game/src/main/AndroidManifest.xml` | 612 |
| `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/DrivingGame.kt` | 10,154 |
| `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/GameLauncherActivity.kt` | 2,576 |
| `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/render/CameraRig.kt` | 3,871 |
| `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/render/CarModel.kt` | 6,262 |
| `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/render/MeshKit.kt` | 2,996 |
| `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/render/ProceduralTextures.kt` | 3,090 |
| `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/render/QualityProfile.kt` | 1,181 |
| `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/render/WorldScene.kt` | 12,908 |
| `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/sim/Collision.kt` | 10,059 |
| `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/sim/Vehicle.kt` | 4,701 |
| `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/sim/VehicleSpec.kt` | 696 |
| `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/ui/GameUi.kt` | 12,739 |
| `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/world/WorldLayout.kt` | 17,319 |
| `MyUmrahJourney/game/src/main/res/values-ar/strings.xml` | 335 |
| `MyUmrahJourney/game/src/main/res/values-b+mfe/strings.xml` | 225 |
| `MyUmrahJourney/game/src/main/res/values-fr/strings.xml` | 243 |
| `MyUmrahJourney/game/src/main/res/values/colors.xml` | 121 |
| `MyUmrahJourney/game/src/main/res/values/strings.xml` | 214 |
| `MyUmrahJourney/game/src/main/res/values/themes.xml` | 263 |
| `MyUmrahJourney/game/src/test/kotlin/com/masjidcastel/myumrahjourney/game/sim/DrivingSimulationTest.kt` | 7,094 |
| `MyUmrahJourney/game/src/test/kotlin/com/masjidcastel/myumrahjourney/game/world/WorldLayoutTest.kt` | 4,849 |
| `MyUmrahJourney/gradle.properties` | 185 |
| `MyUmrahJourney/gradle/libs.versions.toml` | 2,252 |
| `MyUmrahJourney/gradle/wrapper/gradle-wrapper.jar` | 43,764 |
| `MyUmrahJourney/gradle/wrapper/gradle-wrapper.properties` | 224 |
| `MyUmrahJourney/gradlew` | 8,733 |
| `MyUmrahJourney/gradlew.bat` | 2,937 |
| `MyUmrahJourney/local.properties.example` | 288 |
| `MyUmrahJourney/settings.gradle.kts` | 574 |

### Directory entries (70)

- `MyUmrahJourney/`
- `MyUmrahJourney/app/`
- `MyUmrahJourney/app/src/`
- `MyUmrahJourney/app/src/main/`
- `MyUmrahJourney/app/src/main/kotlin/`
- `MyUmrahJourney/app/src/main/kotlin/com/`
- `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/`
- `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/`
- `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/`
- `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/navigation/`
- `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/ui/`
- `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/ui/components/`
- `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/ui/screens/`
- `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/ui/theme/`
- `MyUmrahJourney/app/src/main/kotlin/com/masjidcastel/myumrahjourney/app/viewmodel/`
- `MyUmrahJourney/app/src/main/res/`
- `MyUmrahJourney/app/src/main/res/drawable/`
- `MyUmrahJourney/app/src/main/res/mipmap-anydpi-v26/`
- `MyUmrahJourney/app/src/main/res/values-ar/`
- `MyUmrahJourney/app/src/main/res/values-b+mfe/`
- `MyUmrahJourney/app/src/main/res/values-fr/`
- `MyUmrahJourney/app/src/main/res/values/`
- `MyUmrahJourney/app/src/main/res/xml/`
- `MyUmrahJourney/core/`
- `MyUmrahJourney/core/src/`
- `MyUmrahJourney/core/src/main/`
- `MyUmrahJourney/core/src/main/kotlin/`
- `MyUmrahJourney/core/src/main/kotlin/com/`
- `MyUmrahJourney/core/src/main/kotlin/com/masjidcastel/`
- `MyUmrahJourney/core/src/main/kotlin/com/masjidcastel/myumrahjourney/`
- `MyUmrahJourney/core/src/main/kotlin/com/masjidcastel/myumrahjourney/core/`
- `MyUmrahJourney/core/src/main/kotlin/com/masjidcastel/myumrahjourney/core/data/`
- `MyUmrahJourney/core/src/main/kotlin/com/masjidcastel/myumrahjourney/core/language/`
- `MyUmrahJourney/core/src/main/kotlin/com/masjidcastel/myumrahjourney/core/launch/`
- `MyUmrahJourney/core/src/main/kotlin/com/masjidcastel/myumrahjourney/core/player/`
- `MyUmrahJourney/core/src/test/`
- `MyUmrahJourney/core/src/test/kotlin/`
- `MyUmrahJourney/core/src/test/kotlin/com/`
- `MyUmrahJourney/core/src/test/kotlin/com/masjidcastel/`
- `MyUmrahJourney/core/src/test/kotlin/com/masjidcastel/myumrahjourney/`
- `MyUmrahJourney/core/src/test/kotlin/com/masjidcastel/myumrahjourney/core/`
- `MyUmrahJourney/core/src/test/kotlin/com/masjidcastel/myumrahjourney/core/data/`
- `MyUmrahJourney/core/src/test/kotlin/com/masjidcastel/myumrahjourney/core/language/`
- `MyUmrahJourney/game/`
- `MyUmrahJourney/game/src/`
- `MyUmrahJourney/game/src/main/`
- `MyUmrahJourney/game/src/main/kotlin/`
- `MyUmrahJourney/game/src/main/kotlin/com/`
- `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/`
- `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/`
- `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/`
- `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/render/`
- `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/sim/`
- `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/ui/`
- `MyUmrahJourney/game/src/main/kotlin/com/masjidcastel/myumrahjourney/game/world/`
- `MyUmrahJourney/game/src/main/res/`
- `MyUmrahJourney/game/src/main/res/values-ar/`
- `MyUmrahJourney/game/src/main/res/values-b+mfe/`
- `MyUmrahJourney/game/src/main/res/values-fr/`
- `MyUmrahJourney/game/src/main/res/values/`
- `MyUmrahJourney/game/src/test/`
- `MyUmrahJourney/game/src/test/kotlin/`
- `MyUmrahJourney/game/src/test/kotlin/com/`
- `MyUmrahJourney/game/src/test/kotlin/com/masjidcastel/`
- `MyUmrahJourney/game/src/test/kotlin/com/masjidcastel/myumrahjourney/`
- `MyUmrahJourney/game/src/test/kotlin/com/masjidcastel/myumrahjourney/game/`
- `MyUmrahJourney/game/src/test/kotlin/com/masjidcastel/myumrahjourney/game/sim/`
- `MyUmrahJourney/game/src/test/kotlin/com/masjidcastel/myumrahjourney/game/world/`
- `MyUmrahJourney/gradle/`
- `MyUmrahJourney/gradle/wrapper/`

# Building Stage 2 on an Android phone with Code on the Go

STATUS: this project has NOT been built or run on any device. Treat every step below as untested.
The exact menu wording in Code on the Go (CoGo) may differ from what is written here; I could not
confirm it from the documentation. Use the CoGo docs (docs.androidide.com) where they differ.

## 1. Before you start
- Phone: 64-bit (arm64), at least 6 GB RAM and 8 GB free storage is realistic. Close other apps.
- Internet (Wi-Fi) for the FIRST build. Gradle 8.10.2 (about 130 MB) and all libraries (Compose, libGDX
  and its natives, DataStore) are downloaded then.
- Install Code on the Go from its official site or GitHub releases. Follow its first-run setup, which
  installs the JDK and Android SDK components it needs.

## 2. Get the project onto the phone
1. Download MyUmrahJourney-stage2.zip to the phone.
2. Extract it with a file manager that can unzip. The result must be a folder `MyUmrahJourney`
   containing `settings.gradle.kts`, `gradlew`, `app/`, `core/`, `game/` directly inside it
   (not one extra folder level deeper).
3. Put it in a place CoGo can open (its own projects folder, or shared storage if CoGo has access).

## 3. Open and build
1. In CoGo choose to open an existing project and select the `MyUmrahJourney` folder.
2. Let Gradle sync finish. The first sync can take many minutes.
3. Build the debug APK: Gradle task `:app:assembleDebug`
   (in CoGo's build / quick-run action, or the Gradle tasks list under `app` -> build -> assembleDebug).
4. Expected APK: `app/build/outputs/apk/debug/app-debug.apk`
5. Install it (CoGo's run/install action, or open the APK from your file manager and allow installs
   from that source). Then open "My Umrah Journey", tap Start journey, rotate to landscape.

If CoGo has a terminal: `chmod +x gradlew && ./gradlew :app:assembleDebug --stacktrace 2>&1 | tee build-log.txt`

## 4. Things that may need extra configuration
| Item | What to do |
|---|---|
| Android SDK Platform 35 and Build-Tools | Required (compileSdk/targetSdk 35). Install via CoGo's SDK manager if sync says the platform is missing. If CoGo cannot provide 35, tell me; I can lower to the highest it supports. |
| SDK location | If Gradle says "SDK location not found", copy `local.properties.example` to `local.properties` and set `sdk.dir=` to CoGo's SDK path. |
| Memory | `gradle.properties` has `-Xmx3072m`. If the build dies with out-of-memory or the app is killed, change it to `-Xmx2048m` (or 1536m). |
| Gradle wrapper | Downloads Gradle 8.10.2 on first run. If `./gradlew` says permission denied, run `chmod +x gradlew`. CoGo may instead use its own Gradle; Gradle/AGP version support in CoGo is unconfirmed. |
| AGP 8.7.3 + aapt2 on ARM | Building with AGP on-device is a known risk (aapt2 binary for ARM). If the error mentions aapt2, send it to me. |
| libGDX natives | Custom task `copyGdxNatives` in `game/build.gradle.kts` downloads `gdx-platform` native jars. Needs internet, and is the most likely first failure. |
| Storage | Gradle caches grow to several GB. |
| Unsigned/debug | The debug APK is signed with the debug key automatically. No release signing at this stage. |

## 5. What to send back
- "Build succeeded" and whether the app installs and opens, OR
- The exact error text (the first `FAILURE:` / `What went wrong:` block, plus the lines above it), or
  `build-log.txt`. A photo of the error screen is fine.
- If it builds: results of the 9 acceptance tests in README.md (pass / fail / what you saw), and
  the phone model, Android version, and roughly what FPS shows on the HUD.

Nothing new (Part 1 controls/audio, Part 3 content) will be started until the build succeeds.

## Application ID
The installed app's ID is `com.masjidcastel.myumrahjourney` (set in `app/build.gradle.kts`).
The Kotlin code namespace stays `com.masjidcastel.myumrahjourney.app` / `.core` / `.game`; this is normal and
does not change the installed ID. The launch activity is
`com.masjidcastel.myumrahjourney/com.masjidcastel.myumrahjourney.app.MainActivity`.

## libGDX natives (checked by reading, not by running)
`copyGdxNatives` takes each `gdx-platform-1.12.1-natives-<abi>.jar`, reads `<abi>` from the file name
(armeabi-v7a, arm64-v8a, x86, x86_64) and copies the `*.so` files at the jar root into
`game/build/generated/gdxNatives/<abi>/`, which is registered as a jniLibs folder. The assumption that
`libgdx.so` sits at the jar root is from libGDX's standard layout; I could not download the jars to confirm.
After a build, check the APK has `lib/arm64-v8a/libgdx.so`
(`unzip -l app/build/outputs/apk/debug/app-debug.apk | grep libgdx`).

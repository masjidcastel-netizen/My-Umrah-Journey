# My Umrah Journey — Stage 2: 3D driving prototype

Package `com.masjidcastel.myumrahjourney` · Kotlin · Jetpack Compose · Material 3 · Navigation · DataStore · libGDX 1.12.1 (OpenGL ES 2.0)
Android 8.0+ (minSdk 26) · English, Français, Kreol Morisien, العربية (RTL)

Stage 1 (native app foundation) is unchanged. Stage 2 adds a real-time 3D driving game in the `game` module,
launched from the **Start journey** button.

## Modules

| Module | Contents |
|---|---|
| `app` | `MainActivity`, `App`, `Theme`, `Navigation`, `JourneyViewModel`, Home / Settings / About screens, 4-language resources |
| `core` | `LanguageManager`, `PlayerState`, `GraphicsQuality`, `DataStoreRepository`, `GameLaunchContract`, unit tests |
| `game` | `GameLauncherActivity`, `DrivingGame`, `sim/` (vehicle physics, collision), `world/` (level layout), `render/` (3D meshes, car, camera, textures), `ui/` (touch controls, HUD, pause menu) |

## Build and run

Needs JDK 17 or 21, Android SDK Platform 35, and internet on first sync.

```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties        # not needed in Android Studio
./gradlew :core:testDebugUnitTest :game:testDebugUnitTest   # 5 + 28 JVM tests, no device needed
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.masjidcastel.myumrahjourney/com.masjidcastel.myumrahjourney.app.MainActivity
```
Windows: `gradlew.bat`. Logs from the game: `adb logcat -s MyUmrahDriving`.

## Controls (this stage)

| Control | Touch | Keyboard (emulator) |
|---|---|---|
| Steer left / right | arrow buttons, bottom-left | A / D or arrows |
| Accelerate | green pedal, bottom-right | W / Up |
| Brake, then reverse | red pedal (hold at standstill to reverse) | S / Down |
| Change camera | camera button, top-right (Chase → Far → Driver) | C |
| Pause menu | pause button, or Back | P / Esc |

Pause menu: Resume, Graphics (Low / Medium / High, saved), Exit to app. Leaving the app also pauses the game.
Graphics changes draw distance and shadows immediately; antialiasing (High only) applies at the next launch.

## Stage 2 acceptance tests (run on a real phone)

| # | Test | Expected |
|---|---|---|
| 1 | Launch from the app | Start journey opens a landscape 3D scene: a town with roads, buildings, palm trees, lamp posts, hills on the horizon |
| 2 | Drive | Hold the green pedal: the turquoise car accelerates down the road; wheels spin; speed shows in km/h |
| 3 | Steer | Hold left/right arrows while moving: the car turns and the front wheels visibly steer; no steering while stopped |
| 4 | Accelerate and brake | Release the green pedal: car coasts down. Hold red pedal: it brakes, stops, then reverses |
| 5 | Camera views | Camera button cycles Chase, Far, Driver (car body hidden in driver view) |
| 6 | Collisions | Drive into a barrier, building, tree or lamp post: car stops or bounces, screen flashes red, camera shakes, "IMPACT n km/h" and the HITS counter update. Sliding along a wall at a shallow angle keeps moving |
| 7 | Pause / resume | Pause button freezes the car; Resume continues. Pressing Home then returning shows the pause menu |
| 8 | Graphics settings | Pause menu Graphics cycles Low/Medium/High (shadows appear on Medium/High). Value persists after restart and matches Settings in the app |
| 9 | Return to the app | Pause, Exit to app (or Back twice): Home screen is back |

## What was verified, and what was not

Verified by compiling and running here (Kotlin 2.0.21 compiler, JUnit 4.13.2):
- Vehicle physics, collision and level generation: 28 unit tests pass (acceleration, top speed, braking, reverse, steering direction, wall/tree/barrier collisions, sliding along walls, spawn clearance, buildings never on roads, boundary cannot be escaped, deterministic layout).
- All libGDX-facing Kotlin code type-checks against a hand-written model of the libGDX API (catches typos and wrong names; a deliberate typo was caught). This does NOT prove the real libGDX 1.12.1 API matches that model.
- XML well-formed; every string present in all 4 languages with matching placeholders.

NOT verified (no Android SDK and no access to Google Maven / Maven Central from the build sandbox):
- Gradle sync, `assembleDebug`, installation, and every on-device behaviour above. Treat Stage 2 as unbuilt until you have run the commands and tests.
- Most likely trouble spots if the first build fails: `game/build.gradle.kts` (libGDX natives task); vertex-colour rendering (`Usage.ColorUnpacked`) and shadow-map setup in `DrivingGame.kt`/`WorldScene.kt`; a libGDX API signature differing from what is used. Send me the exact error and I will fix it.

## Asset inventory

No external assets, no audio. Everything is generated in code and original:

| Asset | Source |
|---|---|
| Car (body, glass, lights, bumpers, 4 separately rotating wheels) | `render/CarModel.kt`, built from primitives |
| Level: 93 road pieces, 900 paint markings, 33 buildings (7 towers, 11 shops, 15 houses, up to 43 m), 385 trees (188 palms), 72 lamp posts, 4 barriers, 4 boulders, 100 wall segments, 30 hills, 502 colliders | `world/WorldLayout.kt` (fixed seed, identical every launch) |
| Textures: grass, asphalt, building facade (64x64, tileable) | `render/ProceduralTextures.kt` |
| Meshes are batched into ~80 m chunks, culled by camera frustum and quality draw distance | `render/WorldScene.kt` |

## Known limits (honest list)

- Models are procedural primitives, not GLB/glTF art. Loading real GLB assets needs the gdx-gltf library and artist-made files; planned once you have models.
- Terrain is flat; hills are scenery behind the boundary wall. The playable area is 480 m x 480 m. This is a prototype level, not an open world.
- The camera can pass through buildings when you reverse into them.
- HUD and pause text use libGDX's built-in font: English only, no Arabic glyphs yet. The Compose app is fully 4-language; in-game text localisation (with a proper font) is scheduled with the language/audio stage.
- No sound yet. Accelerometer is switched off (`useAccelerometer = false`).
- Performance has not been measured on a device. Use `FPS` on the HUD and the Graphics setting to tune.
- libGDX 1.12.1 natives are not 16 KB page-aligned; move to a newer libGDX before Play Store submission.

## Roadmap (updated with your latest request)

- **Stage 2b — Driving controls** (next): tilt steering (accelerometer, calibration, sensitivity, dead zone, inversion), button steering, virtual steering wheel, handbrake, automatic/manual gears, left/right-handed layout, vibration, a Driving Settings screen saved in DataStore. The steering maths will be unit-tested here like the physics.
- **Stage 3 — Open world**, **Stage 4 — Islamic education and life simulation**.
- **Stage 5 — Languages and audio**, expanded to your audio spec. Audio files must be supplied by you (see below).
- **Stage 6 — Integration and release**.

# First debug APK: verification

**Date:** 2026-11-03
**Input:** `app-debug.apk` supplied by the project owner (built on Codemagic from branch `native-android-development`; the Codemagic build log itself was not seen by me).
**What I did:** static inspection of the APK file. I did **not** run it. Device behaviour below is as reported by the owner.

## Verified by inspecting the file

| Check | Result |
|---|---|
| APK SHA-256 | `c9cf76b6152529b824ee77c300fff7d5d4455516344938242267b5008c0eba24` |
| Size | 12,433,746 bytes (148 entries, 30,016,002 bytes uncompressed) |
| Package ID | `com.masjidcastel.myumrahjourney` |
| Activities | `com.masjidcastel.myumrahjourney.app.MainActivity`, `com.masjidcastel.myumrahjourney.game.GameLauncherActivity`; application class `...app.MyUmrahJourneyApplication` |
| libGDX natives | `lib/<abi>/libgdx.so` present for all four ABIs: arm64-v8a, armeabi-v7a, x86, x86_64. The `copyGdxNatives` task works. |
| Other native libs | `libandroidx.graphics.path.so`, `libdatastore_shared_counter.so` (all ABIs) |
| Compose and libGDX in the dex | Compose UI classes present; `com.badlogic.gdx.backends.android.AndroidApplication` present |
| Web content | `assets/` folder absent: no HTML, JS, CSS, images or audio bundled. No class in `com.masjidcastel.*` mentions WebView or web. One library dex contains the type name `Landroid/webkit/WebView;` (and `MimeTypeMap`) alongside libGDX backend classes; which library class refers to it was not identified, and nothing in our code uses a WebView. |

## 16 KB page-size measurement (the open decision from the import report)

Measured with `tools/check_elf_alignment.py`:

| Library | LOAD alignment | 16 KB-ready |
|---|---|---|
| `libgdx.so` (libGDX 1.12.1), arm64-v8a | 0x1000 (4 KB) | **No** |
| `libgdx.so` (libGDX 1.12.1), x86_64 | 0x1000 (4 KB) | **No** |
| `libgdx.so`, armeabi-v7a and x86 (32-bit) | 0x1000 | No (Play's rule targets 64-bit) |
| `libandroidx.graphics.path.so`, `libdatastore_shared_counter.so` (all ABIs) | 0x4000 | Yes |

So the Stage 2 README's statement is now **confirmed**: the libGDX 1.12.1 native library is 4 KB-aligned. The APK stores libraries uncompressed at 16 KB-aligned offsets (AGP did that), so the only blocker is libGDX's own `libgdx.so`.
Consequence: fine for the current debug builds on ordinary phones. A Google Play release (targetSdk 35) and phones running 16 KB pages need a libGDX build with 16 KB-aligned natives. Not changed yet; decision pending.

## Owner feedback on the running app (first device test)

Reported: it renders real-time 3D. Missing, in the owner's words: audio, characters, interaction, more realistic car and buildings, scenes. These are the planned later stages of the master specification (Stage 3 driving upgrade including audio; Stage 4 Mauritius world with NPCs; Stage 5 international worlds).

Not yet recorded: phone model, Android version, FPS, and results of the 9 acceptance tests in `MyUmrahJourney/README.md`.

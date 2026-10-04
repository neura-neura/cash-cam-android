# CashCam Android

Create product cards from photos, save them to your gallery, and share one or several through Android's share sheet. CashCam is a native Android app built with Kotlin, Jetpack Compose, Material 3 Expressive, and CameraX.

Requires **Android 10 or later**. The app interface is currently in Spanish. It works offline and does not use external services.

## Download

Get the APK from [GitHub Releases](https://github.com/neura-neura/cash-cam-android/releases). Version 1.2 is an optimized preview build signed with a debug key. Production signing is not configured.

## Features

- Capture photos in 16:9, 1:1, or 9:16, switch cameras, and adjust available zoom.
- Edit the title, price, condition, subtitle, and product details with a live preview that stays visible while you fill in the form.
- Customize the text in the card's upper-right corner. This text and the subtitle persist across app restarts and become defaults for new cards; either can be left blank.
- Center condition and price text inside their badges.
- Adapt card height to the photo format, keeping landscape cards compact and giving portrait photos more space.
- Browse a gallery grid, open individual cards, or long-press to select several.
- Save one or several PNG cards to `Pictures/CashCam`. Saving again does not duplicate an export that still exists in the gallery.
- Share individual images or a selection using Android's share sheet. The receiving app controls how many images it accepts and how it publishes them.
- Follow the system light/dark theme, with dynamic colors on Android 12 and later.

Selected cards are shared as separate images. Existing cards keep their original rendered content when you change the defaults.

## Use

1. Open **Crear**, grant camera access, choose a format, and take a photo.
2. Fill in the product fields while watching the live preview.
3. Tap **Crear ficha** to save the card inside the app.
4. Open **Mis fichas** and tap a card to view it, save it, or share it. Long-press a card to enter multiple selection.

| Photo format | Exported card size | Photo area |
| --- | --- | --- |
| 16:9 | 1040 × 1044 px | 960 × 540 px |
| 1:1 | 1040 × 1240 px | 736 × 736 px |
| 9:16 | 1040 × 2211 px | 960 × 1707 px |

## Local storage and efficiency

Only camera permission is requested; broad photo-library access and Internet permission are not required. Cards are stored in the app's private storage. Uninstalling removes those internal cards, while gallery exports remain.

The gallery uses lazy loading and 260 px thumbnails with the card's aspect ratio. Preview and PNG export share the same drawing code. The preview decodes each captured photo once at a bounded size; typing does not compress PNGs. Export rendering runs off the main thread, limits photo decoding to 2048 px, and releases bitmaps. Batch gallery exports process one file at a time. Sharing uses FileProvider URIs with temporary read grants. The preview build enables R8 and resource shrinking.

These are implementation measures; battery consumption, latency, and memory use have not been benchmarked on physical hardware.

## Build

Open the repository root in Android Studio, or use the included Gradle wrapper. Requirements: JDK 21, Android SDK Platform 36, and the required SDK build tools. Configure your SDK location with Android Studio or an untracked `local.properties` file.

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assemblePreview
```

On macOS or Linux:

```sh
./gradlew :app:assembleDebug
./gradlew :app:assemblePreview
```

The optimized, debug-key-signed preview APK is generated at `app/build/outputs/apk/preview/app-preview.apk`. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. The release variant enables shrinking but remains unsigned until you configure your own signing key outside the repository.

Material 3 is pinned to `1.5.0-alpha18` for the experimental Expressive APIs used by the app. CameraX is pinned to `1.5.3`.

## Validation

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:lintDebug
```

Instrumented tests require a connected Android device or emulator with API 29 or later. Version 1.2 passed 3 unit tests and 6 instrumented tests, and Android Lint completed with no errors. See [VERIFICATION.md](VERIFICATION.md) for coverage and limits.

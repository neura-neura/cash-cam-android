# CashCam Android 1.2 Preview

First release of the standalone Android repository. Create product cards with Material 3 Expressive, save them to your gallery, and share one or several images through Android's share sheet.

## Included

- Camera capture with 16:9, 1:1, and 9:16 formats, camera switching, and available zoom.
- Live card preview while filling in product details.
- Customizable corner text and subtitle, remembered for new cards across app restarts.
- Centered condition and price badges.
- Adaptive card height: compact landscape cards, the existing square layout, and taller portrait photos.
- Gallery grid with multiple selection, batch saving, and single or multiple image sharing.
- Offline local storage, thumbnail loading, and optimized code and resources.

## Installation

Download `CashCam-Android-1.2-preview.apk` from the assets below and install it on Android 10 or later. The interface is currently in Spanish.

This is an optimized preview APK signed with a debug key. Production signing is not configured. Package: `com.neura.cashcam`; versionName: `1.2`; versionCode: `3`.

## Validation

3 unit tests and 6 instrumented tests passed on an Android 17 / API 37 emulator. Android Lint completed with no errors. The preview APK was installed and launched successfully.

Physical-device performance and publication inside social apps have not been validated. Receiving apps determine the number of accepted images and their publication format.

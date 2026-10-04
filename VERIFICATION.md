# Version 1.2 validation

Validated on October 3, 2026 using an Android 17 / API 37 emulator.

- **3 unit tests and 6 instrumented tests passed**, with no failures or skipped tests.
- Android Lint completed with **0 errors**. Warnings remain, including dependency-update suggestions, version-catalog organization, KTX suggestions, and template resources.
- The optimized preview APK was built, installed, and launched on the emulator.

## Coverage

- Centered photo crop geometry and bounds for landscape, square, and portrait formats.
- Adaptive card sizes: 1040 × 1044, 1040 × 1240, and 1040 × 2211 px.
- Actual PNG rendering, thumbnail proportions, persisted dimensions, and compatibility with older card metadata.
- MediaStore export without duplicate saves, readable FileProvider URIs, and temporary read grants for single and multiple image sharing.
- Gallery selection and detail actions, including visible controls when viewing a tall portrait card.
- Condition and price text centering, checked against rendered text pixels with a 1.5 px tolerance.
- Restored corner-text and subtitle defaults, including blank values.
- Live preview updates while editing, before generating a card.

The verification tasks were `:app:testDebugUnitTest`, `:app:connectedDebugAndroidTest`, `:app:assemblePreview`, and `:app:lintDebug`.

## Limits

Physical-device camera behavior, rotation, extreme font scaling, and TalkBack have not been fully validated. The emulator did not have social apps installed: tests covered Android's share contract and chooser, rather than publication inside receiving apps. Battery use, latency, and memory consumption have not been measured on physical hardware.

The downloadable APK uses versionName `1.2`, versionCode `3`, and the preview build type. It is optimized with R8 and resource shrinking, and signed with a local debug key. Production signing is not configured.

# Lumina Puzzle (Android)

Lumina Puzzle is a sleek, neon-themed `Lights Out` style puzzle game built with Kotlin and Jetpack Compose.

[![Download APK](https://img.shields.io/badge/Download-APK-brightgreen.svg)](PASTE_YOUR_GITHUB_RELEASE_LINK_HERE)

## Features

- Tap a tile to toggle it and its four neighbors.
- Turn all lights off to solve the level.
- Each solved level unlocks the next one with higher shuffle difficulty.
- Best score (fewest moves) is stored on-device with DataStore.
- Includes local achievements and a daily challenge mode.
- Includes haptic + tone feedback and adaptive tablet-friendly board sizing.

## Tech Stack

- Kotlin
- Jetpack Compose (Material 3)
- Android ViewModel + StateFlow
- AndroidX DataStore Preferences
- Google Mobile Ads SDK (AdMob banner)

## Localization

- English: `app/src/main/res/values/strings.xml`
- Spanish: `app/src/main/res/values-es/strings.xml`

## Build Requirements

- Android Studio Koala or newer
- JDK 17
- Android SDK 35

## Run

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on an emulator or Android device.

## Play Store Release Checklist

1. Create a release keystore and keep it safe.
2. Update `applicationId` if needed in `app/build.gradle.kts`.
3. Increase `versionCode` and `versionName` for each release.
4. Build an Android App Bundle (`Build > Generate Signed Bundle / APK`).
5. Create Play Console listing assets (icon, screenshots, feature graphic).
6. Add privacy policy URL and content rating information in Play Console.
7. Complete app content declarations (ads, data safety, target audience).
8. Upload `.aab`, run pre-launch report, then submit to production.
9. Replace AdMob test IDs in `AndroidManifest.xml` and `MainActivity.kt` with your production ad IDs before publishing.

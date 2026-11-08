# FlappyBro — Skyline Run

An original, fully native Android tap-to-fly arcade game. Java + Android Canvas, no WebView, no network permissions, no ads and no external runtime dependencies. All art is drawn with native vector primitives; sounds are original synthesized PCM effects.

Tap to fly through mint gates over a twilight skyline. Progressively faster flight, collision effects, pause/resume, sound toggle, instant retry and persistent local high scores. Android 8.0 or newer.

## Build

Install JDK 17 or 21, Android SDK platform 35 and build tools 35.0.0. Set `ANDROID_HOME` or create `local.properties` with `sdk.dir`.

```sh
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. The distributed copy is named `FlappyBro.apk`. This is a signed, sideloadable debug build.

## Android runtime smoke test

```sh
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.flappybro.skyline.test/com.flappybro.skyline.SmokeTestRunner
```

The native instrumentation exercises launch, touch flight, pause, real time obstacle passing, collision, restart and preference persistence. No physical device is implied by an emulator run.

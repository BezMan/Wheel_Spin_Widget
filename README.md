# Spin Wheel Home Screen Widget — Android SDK

A Kotlin Android library (`spinwheel-sdk`) that renders a spin wheel as a home-screen AppWidget and a full-screen Compose UI. A thin demo app (`:app`) exercises the SDK.

---

## Project Structure

```
WheelSpinWidget/
├── app/                        # Demo consumer app
└── spinwheel-sdk/              # Deliverable library (Maven lib)
    └── src/main/java/com/bez/spinwheel_sdk/
        ├── SpinWheelSdk.kt             # Public singleton entry point
        ├── SpinWheelState.kt           # Public observable state
        ├── domain/model/               # WheelConfig, WheelResult (sealed)
        ├── domain/repository/          # ConfigRepository interface
        ├── data/mock/                  # MockConfigRepository — parses local config.json
        ├── data/remote/                # RemoteConfigRepository — OkHttp stub (not wired)
        ├── data/prefs/                 # ConfigPrefs — SharedPreferences JSON storage
        ├── data/work/                  # ConfigSyncWorker — simulates FCM push
        ├── di/                         # Hilt module
        └── presentation/
            ├── SpinActivity.kt / SpinViewModel.kt / WheelComposable.kt
            └── widget/  SpinWheelWidgetProvider, SpinAnimationWorker, WidgetState
```

---

## Architecture

```
domain/        → Pure Kotlin. Models + repository interface.
data/          → ConfigRepository impl. Persistence, networking, WorkManager.
presentation/  → Compose UI, ViewModel, AppWidget, animation workers.
```

**`AppWidgetProvider` + `RemoteViews` over Glance** — Glance's async composable→RemoteViews pipeline fails silently during frame-by-frame animation. Direct `AppWidgetManager.updateAppWidget()` is a synchronous IPC call; `partiallyUpdateAppWidget` pushes only the rotating wheel layer per frame, avoiding a full layout re-send.

**`SpinAnimationWorker` (WorkManager)** — `BroadcastReceiver.onReceive()` has a 10-second limit. WorkManager gives the animation a proper long-running coroutine; `try/finally` ensures the spinning flag always clears.

**`Canvas.rotate` over `Matrix.postRotate`** — `Matrix.postRotate` expands the bounding box at diagonal angles, causing visible pulsing. `Canvas.rotate` keeps output dimensions fixed.

**`SpinWheelSdk` singleton** — Hilt is an internal detail. Consumers call `SpinWheelSdk.init(context)` and collect `spinState: StateFlow<SpinWheelState>`. Bridges widget and in-app spin so all observers see a unified angle and spin source.

**`WheelResult<T>` over `Result<T>`** — avoids name collision with `ListenableWorker.Result` in WorkManager workers.

---

## Configuration (Mock)

`spinwheel-sdk/src/main/assets/config.json` — three variants, random-picked on each `fetchConfig()` call to simulate an FCM push:

| ID              | `duration` | `minimumSpins` | `maximumSpins` |
|-----------------|------------|----------------|----------------|
| `wheel_fast`    | 1000 ms    | 3              | 5              |
| `wheel_minimal` | 2500 ms    | 2              | 4              |
| `wheel_slow`    | 5000 ms    | 5              | 8              |

Duration is clamped to `[1000, 2500, 5000]` ms and shown in the UI as "Spin duration: X ms".

---

## Persistence (SharedPreferences)

| File                     | Class         | Purpose                                      |
|--------------------------|---------------|----------------------------------------------|
| `spinwheel_config`       | `ConfigPrefs` | Business data — serialised `WheelConfig`, written on FCM push only |
| `spinwheel_widget_state` | `WidgetState` | Runtime UI state — angle + spin flags, written every animation frame, read cross-process by the launcher |

Two files rather than one: `WidgetState` is written at ~30 fps and accessed from both the app process and the launcher process. Merging it with `ConfigPrefs` would couple unrelated concerns and complicate the cross-process boundary.

---

## Assets

| File              | Role                                                        |
|-------------------|-------------------------------------------------------------|
| `bg.png`          | Background — full bleed, `centerCrop`                       |
| `wheel.png`       | Spinning wheel — rotated each frame via `Canvas.rotate`     |
| `wheel_frame.png` | Static overlay centred on the wheel                         |
| `wheel_spin.png`  | Tap-to-spin button — dimmed at alpha 90 while spinning      |

Stored as local drawables. `WheelConfig` models the remote asset URLs (`network.assets.host + wheel.assets.*`) ready for remote loading.

---

## Demo App

| Button | Action |
|--------|--------|
| **Open Spin Wheel** | Launches `SpinActivity` — full-screen Compose UI with SDK state debug card |
| **Simulate Config Push (FCM)** | Enqueues `ConfigSyncWorker` — picks a new random config, updates widget |

Widget and in-app wheel stay in sync: the app snaps to the widget's final angle on resume via `LifecycleEventEffect(ON_RESUME)`. Opening the app mid-widget-spin disables the in-app spin button until the widget finishes.

---

## What Is Not Yet Implemented

**1. Remote config fetch** — `RemoteConfigRepository` not implemented.

**2. Remote image loading** — Assets are local drawables.

**3. React Native wrapper** — Simulated by app wrapper around the Maven lib file

---

## Build & Run

```bash
./gradlew :app:installDebug
# Add widget: long-press home screen → Widgets → "Wheel Spin Widget"
```

**Requirements:** Android Studio Meerkat | AGP 9.0.1 | Kotlin 2.0.21 | minSdk 31

### SDK distribution

`:app` depends on the SDK via Maven coordinates — not a Gradle module reference. `maven-local/` is a local Maven repository.

```kotlin
implementation("com.bez:spinwheel-sdk:1.0.0")  // app/build.gradle.kts
maven { url = uri("maven-local") }              // settings.gradle.kts
```

To re-publish after SDK changes:
```bash
# 1. Uncomment include(":spinwheel-sdk") in settings.gradle.kts
./gradlew :spinwheel-sdk:publishReleasePublicationToProjectLocalRepository
# 2. Comment it out again
```

# Spin Wheel Home Screen Widget — Android SDK

## Overview

A Kotlin Android library (`spinwheel-sdk`) that renders a spin wheel as a home-screen
AppWidget and as an in-app full-screen Compose UI. A thin demo app (`:app`) exercises the SDK.

The original task called for a full React Native wrapper, remote config/asset fetching, and a
downloadable demo RN app. This submission covers the Android/Kotlin side in full and documents
clearly what remains for the React Native integration phase.

---

## Project Structure

```
WheelSpinWidget/
├── app/                        # Demo consumer app
│   └── src/main/java/com/example/wheelspinwidget/
│       ├── MainActivity.kt             # Launcher with 3 action buttons
│       └── WheelSpinWidgetApplication.kt   # Hilt + SDK init
│
└── spinwheel-sdk/              # Deliverable library (AAR)
    └── src/main/java/com/bez/spinwheel_sdk/
        ├── SpinWheelSdk.kt             # Public SDK entry point (singleton)
        ├── SpinWheelState.kt           # Public observable state model
        ├── domain/
        │   ├── model/
        │   │   ├── WheelConfig.kt      # Full JSON schema models
        │   │   └── WheelResult.kt      # Sealed Loading / Success / Error
        │   └── repository/
        │       └── ConfigRepository.kt # Interface: observeConfig() + fetchConfig()
        ├── data/
        │   ├── mock/
        │   │   └── MockConfigRepository.kt  # Parses local config.json (3 variants)
        │   ├── remote/
        │   │   └── RemoteConfigRepository.kt # OkHttp stub — not yet wired
        │   ├── prefs/
        │   │   └── ConfigPrefs.kt      # SharedPreferences JSON storage
        │   └── work/
        │       └── ConfigSyncWorker.kt # WorkManager worker — simulates FCM push
        ├── di/
        │   └── SpinWheelModule.kt      # Hilt module: binds MockConfigRepository → ConfigRepository
        └── presentation/
            ├── SpinActivity.kt         # SDK-provided transparent Compose activity
            ├── SpinViewModel.kt        # ViewModel: collects config StateFlow
            ├── WheelComposable.kt      # Full-screen Compose spin UI
            └── widget/
                ├── SpinWheelWidgetProvider.kt  # AppWidgetProvider + RemoteViews helpers
                ├── SpinAnimationWorker.kt      # WorkManager frame-by-frame animation
                └── WidgetState.kt              # SharedPreferences: rotation angle + spin flags
```

---

## Architecture

### Clean Architecture layers

```
domain/        → Pure Kotlin. Models and repository interface. No Android dependencies.
data/          → Implements ConfigRepository. Handles persistence, networking, background work.
presentation/  → Compose UI, ViewModel, AppWidget, WorkManager workers.
```

### Key design decisions

**`WheelResult<T>` instead of `Result<T>`**
WorkManager workers return `ListenableWorker.Result`. Using Kotlin's `Result<T>` in the same
file causes a name collision. A custom sealed class avoids the ambiguity cleanly.

**`AppWidgetProvider` + `RemoteViews` instead of Jetpack Glance**
Glance was prototyped first but removed. Glance's `GlanceAppWidget.update()` is an async
composable-to-RemoteViews pipeline that fails silently — making frame-by-frame animation
unreliable. Direct `AppWidgetManager.updateAppWidget(ids, remoteViews)` is a synchronous IPC
call: if it returns, the frame reached the launcher. Glance also has no `graphicsLayer` support,
so wheel rotation required pre-rendered bitmaps either way.

**`SpinAnimationWorker` (WorkManager `CoroutineWorker`) for widget animation**
A widget tap fires `BroadcastReceiver.onReceive()`, which has a hard 10-second wall-clock
limit even with `goAsync()`. The spin animation runs up to 5 seconds plus per-frame IPC cost.
WorkManager gives the animation a proper long-running coroutine that the system respects, and
automatically handles process restarts mid-animation. The `try/finally` block ensures
`setWidgetSpinning(false)` runs even on `CancellationException`.

**Bitmap rotation via `Canvas.rotate` instead of `Matrix.postRotate`**
`Matrix.postRotate` expands the bitmap's bounding box at diagonal angles, causing the image to
visually pulse larger and smaller as it spins. `Canvas.rotate` paints onto a fixed-size canvas,
keeping the output dimensions constant across all angles.

**`partiallyUpdateAppWidget` for animation frames**
During the spin loop only the wheel layer changes. `partiallyUpdateAppWidget` pushes a single
`RemoteViews` containing only the updated `ImageView`, avoiding the cost of re-sending the full
widget layout on every frame.

**`SpinWheelSdk` singleton as the public API**
Hilt is an internal implementation detail. Consumers call `SpinWheelSdk.init(context)` and
collect `SpinWheelSdk.spinState: StateFlow<SpinWheelState>`. The SDK bridges widget and in-app
spin events so all observers see a unified, continuously updated angle and spin source.

**`WidgetState` — SharedPreferences as cross-process state**
Widget updates run in the launcher's process; the app runs in its own. SharedPreferences
(with `apply()`) is the standard low-overhead mechanism for sharing small state across Android
processes. Three values are persisted: `rotation` (float, degrees), `widget_spinning` (bool),
`app_spinning` (bool).

**Hilt + `EntryPointAccessors` in `ConfigSyncWorker`**
`ConfigSyncWorker` is a plain `CoroutineWorker` (no `@HiltWorker`), avoiding the need for
`HiltWorkerFactory` and `Configuration.Provider` in the Application class. It still accesses
the Hilt-managed `ConfigRepository` singleton via `EntryPointAccessors.fromApplication()`,
ensuring the ViewModel and the worker share the same instance and the same `StateFlow`.

---

## Configuration (Mock)

The SDK ships with `spinwheel-sdk/src/main/assets/config.json` containing three wheel variants:

| ID             | `rotation.duration` | `minimumSpins` | `maximumSpins` |
|----------------|---------------------|----------------|----------------|
| `wheel_fast`   | 1000 ms             | 3              | 5              |
| `wheel_minimal`| 2500 ms             | 2              | 4              |
| `wheel_slow`   | 5000 ms             | 5              | 8              |

`MockConfigRepository` parses this file lazily from assets and picks a random variant on each
`fetchConfig()` call, simulating an FCM-triggered remote config push.

**`rotation.duration` is clamped** to `[1000, 5000]` ms in both the Compose animation
(`WheelComposable`) and the widget worker (`SpinAnimationWorker`) before use.

The duration value is surfaced in the in-app UI as "Spin duration: X ms" so the effect of
pressing "Simulate Config Push" is immediately visible.

---

## Persistence (SharedPreferences)

Two separate SharedPreferences files are used, intentionally:

| File                     | Class           | Keys stored                               |
|--------------------------|-----------------|-------------------------------------------|
| `spinwheel_config`       | `ConfigPrefs`   | `config_json` (serialised `WheelConfig`), `last_fetch_ms` |
| `spinwheel_widget_state` | `WidgetState`   | `rotation` (Float), `widget_spinning`, `app_spinning` |

**Why not one file?** The two stores have fundamentally different roles and access patterns:

- **`ConfigPrefs`** is app-process only, written infrequently (on FCM push), and can be `null`
  (config not yet fetched). It holds business data — what the wheel looks like and how it behaves.
- **`WidgetState`** is written on every animation frame (~20 fps) and read from both the app
  process and the launcher's process (which renders the AppWidget). It holds runtime UI state —
  the current angle and spinning flags. Merging the two would couple unrelated concerns, blur the
  null-handling semantics, and make the cross-process access boundary harder to reason about.

`ConfigPrefs` uses `kotlinx-serialization-json` to serialise `WheelConfig` to a JSON string
before storing it. `lenientJson { ignoreUnknownKeys = true }` is used on read so future schema
additions don't break existing persisted data.

`WidgetState` is the single source of truth for the wheel's resting angle, shared between the
widget process and the app process. `SpinWheelSdk.init()` seeds `SpinWheelState.currentAngle`
from it on cold start, so the SDK state always reflects the physically persisted position.

---

## Assets

The four provided images are shipped as local drawable resources in the SDK:

| File              | Role                                      |
|-------------------|-------------------------------------------|
| `bg.png`          | Widget and activity background (full bleed, `centerCrop`) |
| `wheel.png`       | The spinning wheel — rotated each frame via `Canvas.rotate` |
| `wheel_frame.png` | Static frame overlay, centred on the wheel |
| `wheel_spin.png`  | Tap-to-spin button, centred; dimmed at alpha 90 while spinning |

The JSON schema (`network.assets.host` + `wheel.assets.*`) is fully modelled in `WheelConfig`
and `NetworkConfig`, ready to drive remote asset loading — the local drawables are a stand-in
for the Google Drive URLs from the task specification.

---

## Demo App

`MainActivity` exposes three actions:

| Button | What it does |
|--------|-------------|
| **Open Spin Wheel** | Launches `SpinActivity` (full-screen Compose UI with live SDK state debug card) |
| **Simulate Config Push (FCM)** | Enqueues `ConfigSyncWorker` — picks a new random duration, persists it, updates the widget |

The in-app spin wheel and the home-screen widget stay in sync: finishing a widget spin snaps
the in-app wheel to the same final angle on next resume via `LifecycleEventEffect(ON_RESUME)`.

---

## What Is Not Yet Implemented

The following items are specified in the task but are pending:

### 1. Remote config fetch
`RemoteConfigRepository` exists with an OkHttp client constructor but returns
`WheelResult.Error(UnsupportedOperationException(...))`. To complete:
- Wire a real base URL into `NetworkConfig.assets.host`
- Implement `fetchConfig()` with an OkHttp `GET` call
- Deserialise the response with `kotlinx-serialization-json`
- Wire `RemoteConfigRepository` into `SpinWheelModule` replacing `MockConfigRepository`

### 2. Remote image loading (Google Drive URLs)
Assets are currently local drawables. The `WheelAssets` model already holds the relative paths
from the JSON (`bg`, `wheelFrame`, `wheelSpin`, `wheel`). To complete:
- Construct absolute URLs: `network.assets.host + wheel.assets.*`
- Download each asset over OkHttp
- Cache to the app's internal storage (or `Context.cacheDir`)
- Load into the Compose `Image` composable and into the widget `RemoteViews` via `Bitmap`

### 3. Asset caching
No disk or memory cache exists for downloaded images. A simple file-based cache keyed on URL
hash, with a TTL driven by `network.attributes.cacheExpiration`, would satisfy the spec.

### 4. `kotlinx-serialization-cbor`
The task specification lists CBOR as a serialisation option. It is declared in the dependency
set but not used; the current implementation uses JSON throughout. CBOR would be relevant if
the remote endpoint serves binary-encoded config.

### 5. React Native wrapper

No React Native work has been started. A full RN bridge remains future work; the Android side is
ready for it — `SpinWheelSdk.init()`, `SpinWheelSdk.spinState`, and `SpinActivity` map cleanly
to a native module API surface.

---

## Build & Run

```bash
# Install demo app
./gradlew :app:installDebug

# Add the widget: long-press home screen → Widgets → "Wheel Spin Widget"
```

**Requirements:** Android Studio Meerkat, AGP 9.0.1, Kotlin 2.0.21, minSdk 31 (Android 12).

### SDK distribution

`:app` consumes the SDK as a published Maven artifact from `maven-local/` — not as a live Gradle
module dependency. This demonstrates the real SDK–consumer boundary: transitive dependencies
are resolved automatically via the published POM, exactly as they would be from Maven Central.

```kotlin
// app/build.gradle.kts
implementation("com.bez:spinwheel-sdk:1.0.0")

// settings.gradle.kts (dependencyResolutionManagement)
maven { url = uri("repo") }
```

The `:spinwheel-sdk` module is commented out of `settings.gradle.kts` — `:app` has no
Gradle-level access to the SDK source at all.

To re-publish after SDK changes:
```bash
# 1. Uncomment include(":spinwheel-sdk") in settings.gradle.kts
# 2. Publish
./gradlew :spinwheel-sdk:publishReleasePublicationToProjectLocalRepository
# 3. Comment include(":spinwheel-sdk") out again
```

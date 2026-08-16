# iBoost X

A premium, glassmorphism-styled Android game booster built with C++/NDK,
JNI, CMake, and an OpenGL ES 3.0 animated background — by **SRT X CHEATS**.

Every reading on the dashboard is real:
- **CPU usage** — parsed live from `/proc/stat` in native C++ (`CPUMonitor.cpp`)
- **CPU temperature** — scanned from real Linux thermal zones (`ThermalManager.cpp`), returns "N/A" honestly on devices that don't expose one
- **RAM** — `ActivityManager.MemoryInfo`
- **Battery %/voltage/temp/charging** — `BatteryManager` via the sticky `ACTION_BATTERY_CHANGED` intent
- **Thermal status** (NORMAL/WARM/HOT/CRITICAL) — Android's real Thermal API (`PowerManager.getCurrentThermalStatus`, API 29+)
- **GPU usage** — best-effort read from known vendor sysfs nodes; shows "N/A" where Android doesn't expose it (most non-rooted devices)

No fake numbers, ever — see `SETTINGS → What this app can and can't do`
in-app for the exact list of what's real vs. not possible without root.

## Features

- Glass dashboard with live CPU/temp graphs
- BOOST GAME mode (real Do Not Disturb + `GameManager.setGameState()`, session/thermal watchdog)
- Game library — scans installed games via `ApplicationInfo` category, plus manual add
- **Suzuki Setup** — 4-step wizard (choose game → sensitivity mode → sliders/control layout → save) that writes a per-game `settings.json` profile. This is a config/reference profile, not a hardware sensitivity modifier — Android doesn't expose that to third-party apps.
- Floating gaming HUD overlay (draggable, shows CPU/RAM/battery/temp; FPS honestly marked N/A — no public API for reading another app's frame rate without root)

## Tech stack

Android Studio · C++ NDK · JNI · CMake · OpenGL ES 3.0 · Android SDK
(min SDK 29 / Android 10, target SDK 35 / Android 15)

## Build it

See [`BUILD_INSTRUCTIONS.md`](BUILD_INSTRUCTIONS.md) — includes a
GitHub Actions workflow that builds the APK automatically on every push,
no Android Studio required.

## Project structure

```
iBoostX/
├── .github/workflows/build-apk.yml   # CI: builds APK on every push
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── cpp/                      # native engine
│       │   ├── CMakeLists.txt
│       │   ├── JNI.cpp
│       │   ├── CPUMonitor.{h,cpp}
│       │   ├── ThermalManager.{h,cpp}
│       │   ├── BatteryNative.{h,cpp}
│       │   └── Booster.{h,cpp}
│       ├── java/com/srtxcheats/iboostx/
│       │   ├── core/                 # NativeBridge, DeviceMonitor, BoosterController, SensitivityProfile
│       │   ├── games/                # GameManager, GameAdapter, GamesFragment
│       │   ├── overlay/              # OverlayService (floating HUD)
│       │   ├── settings/             # SettingsFragment
│       │   └── ui/                   # MainActivity, HomeFragment, PerformanceFragment, SuzukiSetupActivity, NebulaBackgroundView, LineGraphView
│       ├── res/                      # glass drawables, dark neon theme, layouts
│       └── AndroidManifest.xml
├── build.gradle
├── settings.gradle
└── gradle.properties
```

# ThorTools

ThorTools is a Thor-first Android utility for the AYN Thor dual-screen handheld. It provides device diagnostics, safe system settings, Magisk-backed boot property changes, and an EZ Root workflow for Thor boot partitions.

## Supported hardware

ThorTools recognizes AYN Thor Lite, Base, Pro, and Max variants. The upper display is 1920 × 1080 at 120 Hz and the lower display is 1240 × 1080 at 60 Hz. The upper panel is a read-only status dashboard; the lower panel owns navigation, actions, and confirmations.

The app enters diagnostics-only mode on other devices or when the Thor privileged root service is unavailable. Every image operation rechecks the current active slot, partition nodes, Magisk state, battery level, and image size before it runs.

## Features

- Thor hardware, firmware, battery, slot, root, Magisk, and partition diagnostics
- EZ Root backup, Magisk patch, active-slot flash, stock restore, and cache management
- DPI and animation speed controls
- Rooted volume-step control
- Optional boot-animation disable through a ThorTools Magisk module
- SHA-256 hashes for cached stock and patched images
- Dual-screen AYN Thor AVD and patched emulator compositor

Thor-specific audio changes are intentionally deferred until they have been measured and validated on physical Thor hardware.

## Build and run

```sh
./scripts/setup-android.sh
./scripts/create-ayn-thor-avd.sh
AEMU_SOURCE_ROOT=/path/to/aemu ./scripts/build-ayn-thor-emulator-overlay.sh
./scripts/run-ayn-thor-avd.sh
ANDROID_HOME=/path/to/android-sdk ./gradlew assembleDebug
ANDROID_HOME=/path/to/android-sdk ./gradlew assembleAlpha
```

The debug APK uses the deterministic fake backend for emulator validation. The alpha APK keeps the real Thor backend while using the automatically generated debug signing key, so it is the build to install on physical hardware. The emulator validates the dual-screen layout, touch mapping, and UI state. It cannot validate real root services or partition writes. Keep `AYN_THOR_ALLOW_STOCK_EMULATOR=1` limited to UI diagnostics when the patched overlay is unavailable.

## Release

Release tags use `vMAJOR.MINOR.PATCH-alpha.N`, `vMAJOR.MINOR.PATCH-beta.N`, or `vMAJOR.MINOR.PATCH`. Alpha releases use an automatically debug-signed alpha build with the real Thor backend and do not require repository signing secrets. Beta and stable releases use the production keystore and are uploaded as `thortools-<tag>.apk`.

## Credits and support

ThorTools preserves the upstream history and GPLv2 attribution from [FeralAI/o2ptweaks.app](https://github.com/FeralAI/o2ptweaks.app).

Support the project through [GitHub Sponsors](https://github.com/sponsors/castdrian) or [Ko-fi](https://ko-fi.com/castdrian).

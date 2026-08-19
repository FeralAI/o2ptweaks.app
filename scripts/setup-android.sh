#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$repo_root/scripts/android-sdk-root.sh"
sdk_root="$(android_sdk_root)"
sdkmanager=""

case "$(uname -m)" in
    arm64|aarch64)
        system_image_abi="arm64-v8a"
        ;;
    x86_64|amd64)
        system_image_abi="x86_64"
        ;;
    *)
        printf '%s\n' "Unsupported host architecture: $(uname -m)"
        exit 1
        ;;
esac

system_image="system-images;android-34;google_apis;$system_image_abi"

if command -v sdkmanager >/dev/null 2>&1; then
    sdkmanager="$(command -v sdkmanager)"
fi

for candidate in \
    "$sdk_root/cmdline-tools/latest/bin/sdkmanager" \
    "$sdk_root/cmdline-tools/bin/sdkmanager" \
    "$sdk_root/tools/bin/sdkmanager"; do
    if [[ -z "$sdkmanager" && -x "$candidate" ]]; then
        sdkmanager="$candidate"
        break
    fi
done

if [[ -z "$sdkmanager" ]]; then
    printf '%s\n' "Android SDK command-line tools were not found under $sdk_root. Install them with Android Studio, then rerun this script."
    exit 1
fi

yes | "$sdkmanager" --sdk_root="$sdk_root" --licenses >/dev/null || true
"$sdkmanager" --sdk_root="$sdk_root" \
    "platform-tools" \
    "emulator" \
    "platforms;android-34" \
    "build-tools;35.0.0" \
    "cmake;3.22.1" \
    "ndk;27.3.13750724" \
    "$system_image"

if [[ ! -f "$repo_root/local.properties" ]]; then
    printf 'sdk.dir=%s\n' "$sdk_root" > "$repo_root/local.properties"
fi

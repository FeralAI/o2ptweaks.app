#!/usr/bin/env bash

android_sdk_root() {
    if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
        printf '%s\n' "$ANDROID_SDK_ROOT"
        return
    fi

    if [[ -n "${ANDROID_HOME:-}" ]]; then
        printf '%s\n' "$ANDROID_HOME"
        return
    fi

    case "$(uname -s)" in
        Darwin)
            printf '%s\n' "$HOME/Library/Android/sdk"
            ;;
        Linux)
            printf '%s\n' "$HOME/Android/Sdk"
            ;;
        MINGW*|MSYS*)
            printf '%s\n' "${LOCALAPPDATA:-$HOME/AppData/Local}/Android/Sdk"
            ;;
        *)
            printf '%s\n' "$HOME/Android/Sdk"
            ;;
    esac
}

#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$repo_root/scripts/android-sdk-root.sh"
sdk_root="$(android_sdk_root)"
avd_home="$repo_root/.android/avd"
emulator="$sdk_root/emulator/emulator"
avd_name="AYN_Thor_API_34"
overlay_emulator="$repo_root/.emulator-overlay/emulator"
overlay_patch_digest_file="$repo_root/.emulator-overlay/ayn-thor-single-window.patch.sha256"
overlay_patch_file="$repo_root/tools/android-emulator/ayn-thor-single-window.patch"
audio_args=()
vsync_rate="${AYN_THOR_VSYNC_RATE:-120}"
gpu_mode="${AYN_THOR_GPU_MODE:-auto}"
window_scale="${AYN_THOR_WINDOW_SCALE:-auto}"
thor_preview_width_millimetres="132.83"
boot_animation_args=()
snapshot_args=(-no-snapshot)
renderer_feature_args=(-feature Vulkan)
multidisplay_args=(-feature MultiDisplay -multidisplay "1,1240,1080,420,1347")
secondary_display_attempts=60

verify_thor_layout_patch() {
    local upper_y_count
    local lower_y_count
    local lower_x_count
    local lower_width_count
    local lower_height_count
    local lower_input_x_scale_count
    local lower_input_y_scale_count
    local lower_input_y_origin_count
    local lower_preview_width_count
    local lower_preview_height_count
    local renderer_preview_width_count
    local renderer_preview_height_count
    local gl_renderer_preview_width_count
    local gl_renderer_preview_height_count
    upper_y_count="$(rg -c 'primary->second\.pos_y = thorPreviewHeight \+ thorPreviewGap;' "$overlay_patch_file" 2>/dev/null || true)"
    lower_y_count="$(rg -c 'thorDisplay->second\.pos_y = 0;' "$overlay_patch_file" 2>/dev/null || true)"
    lower_x_count="$(rg -c '\(primary->second\.originalWidth - thorPreviewWidth\) / 2;' "$overlay_patch_file" 2>/dev/null || true)"
    lower_width_count="$(rg -c 'thorDisplay->second\.width = thorPreviewWidth;' "$overlay_patch_file" 2>/dev/null || true)"
    lower_height_count="$(rg -c 'thorDisplay->second\.height = thorPreviewHeight;' "$overlay_patch_file" 2>/dev/null || true)"
    lower_input_x_scale_count="$(rg -c '\*x \* \(iter\.second\.originalWidth - 1\)' "$overlay_patch_file" 2>/dev/null || true)"
    lower_input_y_scale_count="$(rg -c '\*y \* \(iter\.second\.originalHeight - 1\)' "$overlay_patch_file" 2>/dev/null || true)"
    lower_input_y_origin_count="$(rg -c 'pos_y = totalH - iter\.second\.height - iter\.second\.pos_y;' "$overlay_patch_file" 2>/dev/null || true)"
    lower_preview_width_count="$(rg -c 'constexpr uint32_t thorPreviewWidth = 1085;' "$overlay_patch_file" 2>/dev/null || true)"
    lower_preview_height_count="$(rg -c 'constexpr uint32_t thorPreviewHeight = 945;' "$overlay_patch_file" 2>/dev/null || true)"
    renderer_preview_width_count="$(rg -c 'currentDisplayW = 1085;' "$overlay_patch_file" 2>/dev/null || true)"
    renderer_preview_height_count="$(rg -c 'currentDisplayH = 945;' "$overlay_patch_file" 2>/dev/null || true)"
    gl_renderer_preview_width_count="$(rg -c 'currentDisplayW = 1085;' "$overlay_patch_file" 2>/dev/null || true)"
    gl_renderer_preview_height_count="$(rg -c 'currentDisplayH = 945;' "$overlay_patch_file" 2>/dev/null || true)"
    if [[ "$upper_y_count" != "3" || "$lower_y_count" != "3" || "$lower_x_count" != "3" || "$lower_width_count" != "3" || "$lower_height_count" != "3" || "$lower_input_x_scale_count" != "1" || "$lower_input_y_scale_count" != "1" || "$lower_input_y_origin_count" != "1" || "$lower_preview_width_count" != "3" || "$lower_preview_height_count" != "3" || "$renderer_preview_width_count" != "2" || "$renderer_preview_height_count" != "2" || "$gl_renderer_preview_width_count" != "2" || "$gl_renderer_preview_height_count" != "2" ]]; then
        printf '%s\n' "The AYN Thor compositor patch does not describe an upright upper-over-lower centered display layout."
        exit 1
    fi
}

verify_thor_layout_patch

if [[ "$window_scale" != "auto" && ! "$window_scale" =~ ^0\.[1-9][0-9]*$|^1(\.0*)?$ ]]; then
    printf '%s\n' "AYN_THOR_WINDOW_SCALE must be auto or a value between 0.1 and 1.0."
    exit 1
fi

case "$vsync_rate" in
    60|90|120) ;;
    *)
        printf '%s\n' "AYN_THOR_VSYNC_RATE must be 60, 90, or 120."
        exit 1
        ;;
esac

case "$gpu_mode" in
    auto|host|software|swiftshader|swangle) ;;
    *)
        printf '%s\n' "AYN_THOR_GPU_MODE must be auto, host, software, swiftshader, or swangle."
        exit 1
        ;;
esac

if [[ "$(uname -s)" == "Darwin" && -z "${AYN_THOR_AUDIO_BACKEND:-}" ]]; then
    audio_args=(-audio coreaudio)
elif [[ -n "${AYN_THOR_AUDIO_BACKEND:-}" ]]; then
    audio_args=(-audio "$AYN_THOR_AUDIO_BACKEND")
fi

if [[ "${AYN_THOR_BOOT_ANIMATION:-0}" != "1" ]]; then
    boot_animation_args=(-no-boot-anim)
fi

if [[ ! -x "$emulator" ]]; then
    printf '%s\n' "The Android emulator was not found at $emulator. Run ./scripts/setup-android.sh first."
    exit 1
fi

if [[ -x "$overlay_emulator" ]]; then
    if [[ ! -f "$overlay_patch_digest_file" || "$(cat "$overlay_patch_digest_file")" != "$(shasum -a 256 "$overlay_patch_file" | awk '{print $1}')" ]]; then
        if [[ "${AYN_THOR_ALLOW_STOCK_EMULATOR:-0}" == "1" ]]; then
            printf '%s\n' "Warning: the AYN Thor emulator overlay is stale; using the stock emulator for diagnostics."
            emulator="$sdk_root/emulator/emulator"
        else
            printf '%s\n' "The AYN Thor emulator overlay is stale for the checked-in display layout patch. Reinstall it with ./scripts/install-ayn-thor-emulator-overlay.sh."
            exit 1
        fi
    else
        emulator="$overlay_emulator"
    fi
elif [[ "${AYN_THOR_ALLOW_STOCK_EMULATOR:-0}" == "1" ]]; then
    printf '%s\n' "Warning: using the stock emulator; the Thor screen order is not guaranteed."
else
    printf '%s\n' "The patched AYN Thor emulator overlay is missing at $overlay_emulator."
    printf '%s\n' "Build it with AEMU_SOURCE_ROOT=/path/to/aemu ./scripts/build-ayn-thor-emulator-overlay.sh"
    printf '%s\n' "Set AYN_THOR_ALLOW_STOCK_EMULATOR=1 only for diagnostics."
    exit 1
fi

if [[ "$(uname -s)" == "Darwin" && "$emulator" == "$overlay_emulator" ]]; then
    overlay_library_path="$repo_root/.emulator-overlay/lib64"
    if [[ -d "$overlay_library_path" ]]; then
        export DYLD_LIBRARY_PATH="$overlay_library_path:$overlay_library_path/qt/lib${DYLD_LIBRARY_PATH:+:$DYLD_LIBRARY_PATH}"
    fi
fi

if [[ ! -f "$avd_home/$avd_name.ini" ]]; then
    "$repo_root/scripts/create-ayn-thor-avd.sh"
fi

avd_config_file="$avd_home/$avd_name.avd/config.ini"
set_avd_config() {
    local config_key="$1"
    local config_value="$2"
    if rg -q "^${config_key}[[:space:]]*=" "$avd_config_file"; then
        perl -0pi -e "s|^\\Q$config_key\\E[[:space:]]*=.*$|$config_key = $config_value|m" "$avd_config_file"
    else
        printf '%s = %s\n' "$config_key" "$config_value" >> "$avd_config_file"
    fi
}

set_avd_config "hw.display1.xOffset" "340"
set_avd_config "hw.display1.yOffset" "0"
set_avd_config "hw.multi_display_window" "no"
set_avd_config "hw.hotplug_multi_display" "no"
set_avd_config "hw.initialOrientation" "landscape"

export ANDROID_AVD_HOME="$avd_home"
export ANDROID_SDK_ROOT="$sdk_root"
adb_binary="$sdk_root/platform-tools/adb"
device_serial="${ANDROID_SERIAL:-emulator-5554}"

thor_qemu_pids() {
    ps -axo pid=,command= | awk -v avd="$avd_name" '$0 ~ /qemu-system-(aarch64|x86_64)/ {for (i = 1; i < NF; i += 1) if ($i == "-avd" && $(i + 1) == avd) print $1}'
}

stop_existing_thor_emulator() {
    local existing_pids
    local attempt=0
    existing_pids="$(thor_qemu_pids)"
    if [[ -z "$existing_pids" ]]; then
        return 0
    fi
    printf '%s\n' "Stopping the existing AYN Thor emulator before applying the current display compositor."
    kill $existing_pids 2>/dev/null || true
    while (( attempt < secondary_display_attempts )); do
        existing_pids="$(thor_qemu_pids)"
        [[ -z "$existing_pids" ]] && return 0
        (( attempt += 1 ))
        sleep 0.25
    done
    kill -KILL $existing_pids 2>/dev/null || true
    attempt=0
    while (( attempt < secondary_display_attempts )); do
        existing_pids="$(thor_qemu_pids)"
        [[ -z "$existing_pids" ]] && return 0
        (( attempt += 1 ))
        sleep 0.25
    done
    printf '%s\n' "Unable to stop the existing AYN Thor emulator before starting a new compositor."
    exit 1
}

stop_existing_thor_emulator

wait_for_android_boot() {
    local attempt=0
    local boot_completed
    local device_state
    while (( attempt < 120 )); do
        (( attempt += 1 ))
        device_state="$($adb_binary -s "$device_serial" get-state 2>/dev/null || true)"
        if [[ "$device_state" == "device" ]]; then
            boot_completed="$($adb_binary -s "$device_serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
            if [[ "$boot_completed" == "1" ]]; then
                return 0
            fi
        fi
        if ! kill -0 "$emulator_pid" 2>/dev/null; then
            return 1
        fi
        sleep 1
    done
    return 1
}

activate_secondary_display() {
    local attempt=0
    local display_info
    while (( attempt < 20 )); do
        (( attempt += 1 ))
        if "$adb_binary" -s "$device_serial" shell am broadcast \
            -a com.android.emulator.multidisplay.START \
            -n com.android.emulator.multidisplay/.MultiDisplayServiceReceiver \
            --user 0 >/dev/null 2>&1; then
            display_info="$($adb_binary -s "$device_serial" shell dumpsys display 2>/dev/null || true)"
            if [[ "$display_info" == *"virtual:com.android.emulator.multidisplay"* ]]; then
                return 0
            fi
        fi
        sleep 1
    done
    return 1
}

verify_thor_displays() {
    local attempt=0
    local display_info
    while (( attempt < 20 )); do
        (( attempt += 1 ))
        display_info="$($adb_binary -s "$device_serial" shell dumpsys display 2>/dev/null || true)"
        if printf '%s\n' "$display_info" | grep -Eq 'DisplayViewport\{type=INTERNAL,.*displayId=0,.*orientation=0,.*logicalFrame=Rect\(0, 0 - 1920, 1080\)' &&
            printf '%s\n' "$display_info" | grep -Eq 'DisplayViewport\{type=VIRTUAL,.*displayId=[1-9][0-9]*,.*orientation=0,.*logicalFrame=Rect\(0, 0 - 1240, 1080\)'; then
            return 0
        fi
        sleep 1
    done
    printf '%s\n' "The AYN Thor displays did not come up as upright 1920x1080 and 1240x1080 panels."
    return 1
}

scale_macos_preview() {
    if [[ "$(uname -s)" != "Darwin" || "$window_scale" == "1" ]]; then
        return 0
    fi
    if ! command -v osascript >/dev/null 2>&1; then
        printf '%s\n' "osascript is required to scale the macOS Thor preview window."
        return 0
    fi
    if ! osascript - "$window_scale" "$thor_preview_width_millimetres" <<'APPLESCRIPT'
use framework "CoreGraphics"

on run argv
    set requestedScale to item 1 of argv
    set targetWidthMillimetres to (item 2 of argv) as real
    set previewScale to 1
    if requestedScale is "auto" then
        set displayId to current application's CGMainDisplayID()
        set displaySize to current application's CGDisplayScreenSize(displayId)
        set screenWidthMillimetres to (displaySize's width) as real
        tell application "Finder" to set desktopBounds to bounds of window of desktop
        set desktopWidth to item 3 of desktopBounds
        set targetWidth to (desktopWidth * targetWidthMillimetres / screenWidthMillimetres) as integer
    else
        set previewScale to requestedScale as real
    end if
    tell application "System Events"
        set emulatorProcesses to every process whose name begins with "qemu-system-"
        repeat with emulatorProcess in emulatorProcesses
            tell emulatorProcess
                set windowItems to windows
                set windowCount to count of windowItems
                repeat with windowIndex from 1 to windowCount
                    set windowItem to item windowIndex of windowItems
                    if (name of windowItem as text) contains "Android Emulator" then
                        set currentSize to size of windowItem
                        if requestedScale is "auto" then
                            set previewScale to targetWidth / (item 1 of currentSize)
                        end if
                        set size of windowItem to {((item 1 of currentSize) * previewScale) as integer, ((item 2 of currentSize) * previewScale) as integer}
                        return
                    end if
                end repeat
            end tell
        end repeat
    end tell
end run
APPLESCRIPT
    then
        printf '%s\n' "Unable to scale the macOS Thor preview window; continuing with the native guest displays."
    fi
}

stop_emulator() {
    if kill -0 "$emulator_pid" 2>/dev/null; then
        kill "$emulator_pid" 2>/dev/null || true
    fi
}

"$emulator" \
    -avd "$avd_name" \
    -gpu "$gpu_mode" \
    -vsync-rate "$vsync_rate" \
    "${boot_animation_args[@]}" \
    "${audio_args[@]}" \
    "${renderer_feature_args[@]}" \
    "${multidisplay_args[@]}" \
    "$@" \
    "${snapshot_args[@]}" &
emulator_pid=$!
trap stop_emulator EXIT INT TERM

if ! wait_for_android_boot; then
    printf '%s\n' "The AYN Thor emulator exited before Android finished booting."
    exit 1
fi

if ! activate_secondary_display; then
    printf '%s\n' "The AYN Thor secondary display did not become available."
    exit 1
fi

if ! verify_thor_displays; then
    exit 1
fi

scale_macos_preview

wait "$emulator_pid"

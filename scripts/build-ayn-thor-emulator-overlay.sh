#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source_root="${AEMU_SOURCE_ROOT:?Set AEMU_SOURCE_ROOT to a synced Android Emulator source checkout}"
qemu_root="$source_root/external/qemu"
patch_file="$repo_root/tools/android-emulator/ayn-thor-single-window.patch"
multidisplay_source="$qemu_root/android/android-emu/android/emulation/MultiDisplay.cpp"
post_worker_source="$source_root/hardware/google/gfxstream/host/vulkan/post_worker_vk.cpp"
post_worker_gl_source="$source_root/hardware/google/gfxstream/host/post_worker_gl.cpp"
build_root="$source_root/objs-thortools"
ccache_mode="${AEMU_THOR_CCACHE:-auto}"

if [[ "$ccache_mode" == "auto" ]]; then
    case "$(uname -s)" in
        Darwin)
            sccache_host="darwin"
            ;;
        Linux)
            sccache_host="linux"
            ;;
        MINGW*|MSYS*)
            sccache_host="windows"
            ;;
        *)
            sccache_host=""
            ;;
    esac
    if [[ -n "$sccache_host" && ! -x "$source_root/prebuilts/android-emulator-build/common/sccache/${sccache_host}-x86_64/sccache" ]]; then
        ccache_mode="none"
    fi
fi

if ! git -C "$qemu_root" rev-parse --git-dir >/dev/null 2>&1; then
    printf '%s\n' "The Android Emulator source checkout was not found at $qemu_root"
    exit 1
fi

if git -C "$qemu_root" apply --unidiff-zero --check "$patch_file"; then
    git -C "$qemu_root" apply --unidiff-zero "$patch_file"
elif ! git -C "$qemu_root" apply --unidiff-zero --reverse --check "$patch_file"; then
    printf '%s\n' "The AYN Thor patch does not apply to this emulator source revision."
    exit 1
fi

verify_patched_source() {
    local lower_input_y_origin_count
    local renderer_preview_width_count
    local renderer_preview_height_count
    local gl_renderer_preview_width_count
    local gl_renderer_preview_height_count
    local upper_y_count
    local lower_y_count
    lower_input_y_origin_count="$(rg -c 'pos_y = totalH - iter\.second\.height - iter\.second\.pos_y;' "$multidisplay_source" 2>/dev/null || true)"
    upper_y_count="$(rg -c 'primary->second\.pos_y = thorPreviewHeight \+ thorPreviewGap;' "$multidisplay_source" 2>/dev/null || true)"
    lower_y_count="$(rg -c 'thorDisplay->second\.pos_y = 0;' "$multidisplay_source" 2>/dev/null || true)"
    renderer_preview_width_count="$(rg -c 'currentDisplayW = 1085;' "$post_worker_source" 2>/dev/null || true)"
    renderer_preview_height_count="$(rg -c 'currentDisplayH = 945;' "$post_worker_source" 2>/dev/null || true)"
    gl_renderer_preview_width_count="$(rg -c 'currentDisplayW = 1085;' "$post_worker_gl_source" 2>/dev/null || true)"
    gl_renderer_preview_height_count="$(rg -c 'currentDisplayH = 945;' "$post_worker_gl_source" 2>/dev/null || true)"
    if [[ ! -f "$multidisplay_source" || ! -f "$post_worker_source" || ! -f "$post_worker_gl_source" || "$lower_input_y_origin_count" -lt "1" || "$upper_y_count" != "3" || "$lower_y_count" != "3" || "$renderer_preview_width_count" != "1" || "$renderer_preview_height_count" != "1" || "$gl_renderer_preview_width_count" != "1" || "$gl_renderer_preview_height_count" != "1" ]]; then
        printf '%s\n' "The checked-out AEMU source does not contain the complete AYN Thor patch."
        exit 1
    fi
}

verify_patched_source
"$qemu_root/android/rebuild.sh" --ccache "$ccache_mode" --out "$build_root"

case "$(uname -s)-$(uname -m)" in
    Darwin-arm64|Darwin-aarch64)
        qemu_name="qemu-system-aarch64"
        qemu_headless_name="qemu-system-aarch64-headless"
        ;;
    Darwin-x86_64|Linux-x86_64|Linux-amd64)
        qemu_name="qemu-system-x86_64"
        qemu_headless_name="qemu-system-x86_64-headless"
        ;;
    MINGW64_NT-*-x86_64|MSYS_NT-*-x86_64)
        qemu_name="qemu-system-x86_64.exe"
        qemu_headless_name="qemu-system-x86_64-headless.exe"
        ;;
    *)
        printf '%s\n' "Unsupported host: $(uname -s)-$(uname -m)"
        exit 1
        ;;
esac

qemu_binary="$(find "$build_root" -type f -name "$qemu_name" -print -quit)"
if [[ -z "$qemu_binary" ]]; then
    printf '%s\n' "The built QEMU binary was not found under $build_root"
    exit 1
fi

case "$(uname -s)-$(uname -m)" in
    Darwin-arm64|Darwin-aarch64)
        ninja_host="darwin-x86"
        ;;
    Darwin-x86_64)
        ninja_host="darwin-x86"
        ;;
    Linux-x86_64|Linux-amd64)
        ninja_host="linux-x86"
        ;;
    *)
        ninja_host=""
        ;;
esac

if [[ -n "$ninja_host" && -f "$build_root/build.ninja" ]]; then
    ninja_binary="$source_root/prebuilts/ninja/$ninja_host/ninja"
    if [[ ! -x "$ninja_binary" ]]; then
        printf '%s\n' "The Ninja binary was not found at $ninja_binary"
        exit 1
    fi
    "$ninja_binary" -C "$build_root" "$qemu_name" "$qemu_headless_name" gfxstream_backend
fi

qemu_headless_binary="$(find "$build_root" -type f -name "$qemu_headless_name" -print -quit)"
if [[ -z "$qemu_headless_binary" ]]; then
    printf '%s\n' "The built headless QEMU binary was not found under $build_root"
    exit 1
fi

patch_digest="$(shasum -a 256 "$patch_file" | awk '{print $1}')"
build_patch_digest_file="$build_root/ayn-thor-single-window.patch.sha256"
printf '%s\n' "$patch_digest" > "$build_patch_digest_file"

"$repo_root/scripts/install-ayn-thor-emulator-overlay.sh" "$qemu_binary" "$qemu_headless_binary" "$build_root"

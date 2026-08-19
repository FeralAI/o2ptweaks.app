#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$repo_root/scripts/android-sdk-root.sh"
sdk_root="$(android_sdk_root)"
sdk_emulator_root="$sdk_root/emulator"
overlay_root="$repo_root/.emulator-overlay"
patch_file="$repo_root/tools/android-emulator/ayn-thor-single-window.patch"
qemu_binary="${1:?Usage: ./scripts/install-ayn-thor-emulator-overlay.sh /path/to/qemu-system-binary /path/to/qemu-system-headless-binary [build-output-root]}"
qemu_headless_binary="${2:?Usage: ./scripts/install-ayn-thor-emulator-overlay.sh /path/to/qemu-system-binary /path/to/qemu-system-headless-binary [build-output-root]}"
build_output_root="${3:-}"

case "$(uname -s)-$(uname -m)" in
    Darwin-arm64|Darwin-aarch64)
        host_directory="darwin-aarch64"
        qemu_name="qemu-system-aarch64"
        ;;
    Darwin-x86_64)
        host_directory="darwin-x86_64"
        qemu_name="qemu-system-x86_64"
        ;;
    Linux-x86_64|Linux-amd64)
        host_directory="linux-x86_64"
        qemu_name="qemu-system-x86_64"
        ;;
    MINGW64_NT-*-x86_64|MSYS_NT-*-x86_64)
        host_directory="windows-x86_64"
        qemu_name="qemu-system-x86_64.exe"
        ;;
    *)
        printf '%s\n' "Unsupported host: $(uname -s)-$(uname -m)"
        exit 1
        ;;
esac

case "$qemu_name" in
    *.exe)
        qemu_headless_name="${qemu_name%.exe}-headless.exe"
        ;;
    *)
        qemu_headless_name="${qemu_name}-headless"
        ;;
esac

if [[ ! -x "$sdk_emulator_root/emulator" ]]; then
    printf '%s\n' "The Android emulator was not found at $sdk_emulator_root. Run ./scripts/setup-android.sh first."
    exit 1
fi

if [[ ! -f "$qemu_binary" ]]; then
    printf '%s\n' "The patched QEMU binary was not found: $qemu_binary"
    exit 1
fi

if [[ ! -f "$qemu_headless_binary" ]]; then
    printf '%s\n' "The patched headless QEMU binary was not found: $qemu_headless_binary"
    exit 1
fi

if [[ -z "$build_output_root" ]]; then
    candidate_build_root="$(cd "$(dirname "$qemu_binary")" 2>/dev/null && pwd || true)"
    if [[ -d "$candidate_build_root/lib64" ]]; then
        build_output_root="$candidate_build_root"
    fi
fi

patch_digest="$(shasum -a 256 "$patch_file" | awk '{print $1}')"
if [[ -n "$build_output_root" ]]; then
    build_patch_digest_file="$build_output_root/ayn-thor-single-window.patch.sha256"
    if [[ ! -f "$build_patch_digest_file" || "$(cat "$build_patch_digest_file")" != "$patch_digest" ]]; then
        printf '%s\n' "The QEMU binary was not built from the checked-in Thor layout patch."
        exit 1
    fi
fi

if [[ -e "$overlay_root" && ! -d "$overlay_root" ]]; then
    printf '%s\n' "The overlay path is not a directory: $overlay_root"
    exit 1
fi

mkdir -p "$overlay_root/qemu/$host_directory"
if [[ ! -e "$overlay_root/qemu/$host_directory/lib64" && ! -L "$overlay_root/qemu/$host_directory/lib64" ]]; then
    ln -s "../../lib64/qt/lib" "$overlay_root/qemu/$host_directory/lib64"
fi
for sdk_entry in "$sdk_emulator_root"/*; do
    entry_name="$(basename "$sdk_entry")"
    if [[ "$entry_name" != "emulator" && "$entry_name" != "qemu" && "$entry_name" != "lib64" && ! -e "$overlay_root/$entry_name" && ! -L "$overlay_root/$entry_name" ]]; then
        ln -s "$sdk_entry" "$overlay_root/$entry_name"
    fi
done
cp "$sdk_emulator_root/emulator" "$overlay_root/emulator"
if [[ -L "$overlay_root/lib64" ]]; then
    unlink "$overlay_root/lib64"
fi
mkdir -p "$overlay_root/lib64"
find "$overlay_root/lib64" -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +
cp -R "$sdk_emulator_root/lib64/." "$overlay_root/lib64/"
if [[ -d "$build_output_root/lib64" ]]; then
    find "$build_output_root/lib64" -maxdepth 1 -type f \( -name '*.dylib' -o -name '*.so' -o -name '*.dll' \) -exec cp {} "$overlay_root/lib64/" \;
fi
qemu_overlay_binary="$overlay_root/qemu/$host_directory/$qemu_name"
qemu_headless_overlay_binary="$overlay_root/qemu/$host_directory/$qemu_headless_name"
if [[ ! -e "$qemu_overlay_binary" || ! "$qemu_binary" -ef "$qemu_overlay_binary" ]]; then
    cp "$qemu_binary" "$qemu_overlay_binary"
fi
if [[ ! -e "$qemu_headless_overlay_binary" || ! "$qemu_headless_binary" -ef "$qemu_headless_overlay_binary" ]]; then
    cp "$qemu_headless_binary" "$qemu_headless_overlay_binary"
fi
chmod +x "$overlay_root/emulator"
chmod +x "$qemu_overlay_binary"
chmod +x "$qemu_headless_overlay_binary"

printf '%s\n' "$patch_digest" > "$overlay_root/ayn-thor-single-window.patch.sha256"

printf '%s\n' "Installed the AYN Thor emulator overlay in $overlay_root"

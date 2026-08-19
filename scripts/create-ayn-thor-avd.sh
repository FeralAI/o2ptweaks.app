#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$repo_root/scripts/android-sdk-root.sh"
sdk_root="$(android_sdk_root)"
avd_home="$repo_root/.android/avd"
base_config="$repo_root/config/avd/ayn-thor-base.ini"

case "$(uname -m)" in
    arm64|aarch64)
        system_image_abi="arm64-v8a"
        cpu_arch="arm64"
        ;;
    x86_64|amd64)
        system_image_abi="x86_64"
        cpu_arch="x86_64"
        ;;
    *)
        printf '%s\n' "Unsupported host architecture: $(uname -m)"
        exit 1
        ;;
esac

avd_name="AYN_Thor_API_34"
system_image="system-images;android-34;google_apis;$system_image_abi"
system_image_dir="$sdk_root/system-images/android-34/google_apis/$system_image_abi"
image_sysdir="system-images/android-34/google_apis/$system_image_abi/"
profile="$repo_root/config/avd/ayn-thor.ini"
target="android-34"
tag_display="Google APIs"
tag_id="google_apis"

if [[ ! -f "$base_config" ]]; then
    printf '%s\n' "The base AVD configuration is missing: $base_config"
    exit 1
fi

if [[ ! -d "$system_image_dir" ]]; then
    printf '%s\n' "The $system_image package is missing. Run ./scripts/setup-android.sh first."
    exit 1
fi

export ANDROID_SDK_ROOT="$sdk_root"
export ANDROID_AVD_HOME="$avd_home"

mkdir -p "$avd_home"
mkdir -p "$avd_home/$avd_name.avd"
cp "$base_config" "$avd_home/$avd_name.avd/config.ini"
printf 'avd.ini.encoding=UTF-8\npath=%s\npath.rel=avd/%s.avd\ntarget=%s\n' \
    "$avd_home/$avd_name.avd" "$avd_name" "$target" > "$avd_home/$avd_name.ini"
config="$avd_home/$avd_name.avd/config.ini"

set_config() {
    local key="$1"
    local value="$2"
    if rg -q "^${key}[[:space:]]*=" "$config"; then
        perl -0pi -e "s|^\\Q$key\\E[[:space:]]*=.*$|$key = $value|m" "$config"
    else
        printf '%s = %s\n' "$key" "$value" >> "$config"
    fi
}

while IFS='=' read -r key value; do
    [[ -z "$key" ]] && continue
    set_config "$key" "$value"
done < "$profile"

set_config "AvdId" "$avd_name"
set_config "abi.type" "$system_image_abi"
set_config "avd.ini.displayname" "AYN Thor API 34"
set_config "hw.cpu.arch" "$cpu_arch"
set_config "image.sysdir.1" "$image_sysdir"
set_config "tag.display" "$tag_display"
set_config "tag.id" "$tag_id"

perl -0pi -e 's/^skin\.name[[:space:]]*=.*\n//m; s/^skin\.path[[:space:]]*=.*\n//m' "$config"

printf '%s\n' "Created $avd_name in $avd_home"

#!/system/bin/sh

resolve_partition() {
    partition_name="$1"
    for base_path in /dev/block/by-name /dev/block/bootdevice/by-name /dev/block/platform/*/by-name /dev/block/platform/*/*/by-name /dev/block/platform/*/*/*/by-name; do
        partition_path="$base_path/$partition_name"
        if [ -e "$partition_path" ]; then
            printf '%s' "$partition_path"
            return 0
        fi
    done
    return 1
}

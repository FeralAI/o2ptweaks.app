#!/system/bin/sh

WORKING_PATH="${THORTOOLS_WORKING_PATH:-/storage/emulated/0/Android/data/dev.adrian.thortools/files}"
LOG_FILE="$WORKING_PATH/init_boot.flash.log"

echo "Flash rooted init_boot.img starting..." > $LOG_FILE

ACTIVE_SLOT=$(getprop ro.boot.slot_suffix)
BOOT_IMG="$WORKING_PATH/init_boot_patched$ACTIVE_SLOT.img"
# BOOT_DEVICE=$(ls -la /dev/block/bootdevice/by-name | grep " init_boot$ACTIVE_SLOT " | sed -En 's/^.*(\/dev\/block\/.*)$/\1/p')
BOOT_DEVICE="/dev/block/by-name/init_boot$ACTIVE_SLOT"

if [ -s "$BOOT_IMG" ] && dd if="$BOOT_IMG" of="$BOOT_DEVICE" >> "$LOG_FILE" 2>&1; then
    echo "Flash rooted init_boot.img complete!" >> "$LOG_FILE"
    exit 0
fi

echo "Flash rooted init_boot.img failed!" >> "$LOG_FILE"
exit 1

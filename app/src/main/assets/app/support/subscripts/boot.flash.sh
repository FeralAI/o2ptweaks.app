#!/system/bin/sh

WORKING_PATH="${THORTOOLS_WORKING_PATH:-/storage/emulated/0/Android/data/dev.adrian.thortools/files}"
LOG_FILE="$WORKING_PATH/boot.flash.log"

echo "Flash rooted boot.img starting..." > $LOG_FILE

ACTIVE_SLOT=$(getprop ro.boot.slot_suffix)
case "$ACTIVE_SLOT" in
    a) ACTIVE_SLOT="_a" ;;
    b) ACTIVE_SLOT="_b" ;;
    _a|_b) ;;
    *) exit 1 ;;
esac
BOOT_IMG="$WORKING_PATH/boot_patched$ACTIVE_SLOT.img"
BOOT_DEVICE="/dev/block/by-name/boot$ACTIVE_SLOT"

if [ -s "$BOOT_IMG" ] && dd if="$BOOT_IMG" of="$BOOT_DEVICE" >> "$LOG_FILE" 2>&1 && sync; then
    echo "Flash rooted boot.img complete!" >> "$LOG_FILE"
    exit 0
fi

echo "Flash rooted boot.img failed!" >> "$LOG_FILE"
exit 1

#!/system/bin/sh

WORKING_PATH="${THORTOOLS_WORKING_PATH:-/storage/emulated/0/Android/data/dev.adrian.thortools/files}"
DOWNLOAD_PATH="/storage/emulated/0/Download"
LOG_FILE="$WORKING_PATH/init_boot.restore.log"

echo "Restore init_boot.img starting..." > $LOG_FILE

ACTIVE_SLOT=$(getprop ro.boot.slot_suffix)
# BOOT_DEVICE=$(ls -la /dev/block/bootdevice/by-name | grep " init_boot$ACTIVE_SLOT " | sed -En 's/^.*(\/dev\/block\/.*)$/\1/p')
BOOT_DEVICE="/dev/block/by-name/init_boot$ACTIVE_SLOT"
BOOT_IMG="$WORKING_PATH/init_boot$ACTIVE_SLOT.img"

# if [ ! -e "$BOOT_IMG" ]; then
#     echo "$BOOT_IMG not found"
#     BOOT_IMG="$DOWNLOAD_PATH/init_boot$ACTIVE_SLOT.img"
# fi

if [ -s "$BOOT_IMG" ]; then
    echo "Restoring $BOOT_IMG..." >> $LOG_FILE
    if dd if="$BOOT_IMG" of="$BOOT_DEVICE" >> "$LOG_FILE" 2>&1; then
        echo "Restore init_boot.img complete!" >> "$LOG_FILE"
        exit 0
    fi
    echo "Restore init_boot.img failed!" >> "$LOG_FILE"
else
    echo "$BOOT_IMG not found"
    echo "Could not find restore file"
fi
exit 1

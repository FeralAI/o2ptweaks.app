#!/system/bin/sh

WORKING_PATH="/storage/emulated/0/Android/data/dev.adrian.thortools/files"
DOWNLOAD_PATH="/storage/emulated/0/Download"
LOG_FILE="$WORKING_PATH/boot.restore.log"

echo "Restore boot.img starting..." > $LOG_FILE

ACTIVE_SLOT=$(getprop ro.boot.slot_suffix)
# BOOT_DEVICE=$(ls -la /dev/block/bootdevice/by-name | grep " boot$ACTIVE_SLOT " | sed -En 's/^.*(\/dev\/block\/.*)$/\1/p')
BOOT_DEVICE="/dev/block/by-name/boot$ACTIVE_SLOT"
BOOT_IMG="$WORKING_PATH/boot$ACTIVE_SLOT.img"

# if [ ! -e "$BOOT_IMG" ]; then
#     echo "$BOOT_IMG not found"
#     BOOT_IMG="$DOWNLOAD_PATH/boot$ACTIVE_SLOT.img"
# fi

if [ -e "$BOOT_IMG" ]; then
    echo "Restoring $BOOT_IMG..." >> $LOG_FILE
    dd if="$BOOT_IMG" of="$BOOT_DEVICE" >> $LOG_FILE
    echo "Restore boot.img complete!" >> $LOG_FILE
else
    echo "$BOOT_IMG not found"
    echo "Could not find restore file"
fi

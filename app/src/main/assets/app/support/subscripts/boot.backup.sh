#!/system/bin/sh

WORKING_PATH="/storage/emulated/0/Android/data/com.feralai.o2ptweaks/files"
DOWNLOAD_PATH="/storage/emulated/0/Download"
LOG_FILE="$WORKING_PATH/backup.boot.log"

echo "Backup boot started..." > $LOG_FILE

ACTIVE_SLOT=$(getprop ro.boot.slot_suffix)
# BOOT_DEVICE=$(ls -la /dev/block/bootdevice/by-name | grep " boot$ACTIVE_SLOT " | sed -En 's/^.*(\/dev\/block\/.*)$/\1/p')
BOOT_DEVICE="/dev/block/by-name/boot$ACTIVE_SLOT"

echo "$ACTIVE_SLOT at $BOOT_DEVICE" >> $LOG_FILE
dd if="$BOOT_DEVICE" of="$WORKING_PATH/boot$ACTIVE_SLOT.img" >> $LOG_FILE

echo "Backing up backup file(s)" >> $LOG_FILE
cp -afv "$WORKING_PATH/boot$ACTIVE_SLOT.img" "$DOWNLOAD_PATH/" >> $LOG_FILE

echo "Backup boot complete!" >> $LOG_FILE

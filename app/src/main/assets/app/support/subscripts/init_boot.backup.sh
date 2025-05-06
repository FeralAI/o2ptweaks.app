#!/system/bin/sh

WORKING_PATH="/storage/emulated/0/Android/data/com.feralai.o2ptweaks/files"
DOWNLOAD_PATH="/storage/emulated/0/Download"
LOG_FILE="$WORKING_PATH/backup.init_boot.log"

echo "Backup init_boot started..." > $LOG_FILE

ACTIVE_SLOT=$(getprop ro.boot.slot_suffix)
# BOOT_DEVICE=$(ls -la /dev/block/bootdevice/by-name | grep " init_boot$ACTIVE_SLOT " | sed -En 's/^.*(\/dev\/block\/.*)$/\1/p')
BOOT_DEVICE="/dev/block/by-name/init_boot$ACTIVE_SLOT"

echo "$ACTIVE_SLOT at $BOOT_DEVICE" >> $LOG_FILE
dd if="$BOOT_DEVICE" of="$WORKING_PATH/init_boot$ACTIVE_SLOT.img" >> $LOG_FILE

echo "Backing up backup file(s)"
cp -afv "$WORKING_PATH/init_boot$ACTIVE_SLOT.img" "$DOWNLOAD_PATH/"

echo "Backup init_boot complete!" >> $LOG_FILE

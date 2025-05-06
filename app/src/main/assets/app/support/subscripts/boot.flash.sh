#!/system/bin/sh

WORKING_PATH="/storage/emulated/0/Android/data/com.feralai.o2ptweaks/files"
LOG_FILE="$WORKING_PATH/boot.flash.log"

echo "Flash rooted boot.img starting..." > $LOG_FILE

ACTIVE_SLOT=$(getprop ro.boot.slot_suffix)
BOOT_IMG="$WORKING_PATH/boot_patched$ACTIVE_SLOT.img"
BOOT_DEVICE="/dev/block/by-name/boot$ACTIVE_SLOT"

dd if="$BOOT_IMG" of="$BOOT_DEVICE" >> $LOG_FILE

echo "Flash rooted boot.img complete!" >> $LOG_FILE

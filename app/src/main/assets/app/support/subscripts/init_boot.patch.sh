#!/system/bin/sh

WORKING_PATH="${THORTOOLS_WORKING_PATH:-/storage/emulated/0/Android/data/dev.adrian.thortools/files}"
DOWNLOAD_PATH="/storage/emulated/0/Download"
LOG_FILE="$WORKING_PATH/init_boot.patch.log"

echo "Magisk patch init_boot.img starting..." > $LOG_FILE

# MAGISK_PATH="/data/adb/magisk"
MAGISK_PATH="$1"
MAGISK_PATCH="$MAGISK_PATH/boot_patch.sh"
MAGISK_NEWBOOT="$MAGISK_PATH/new-boot.img"
ACTIVE_SLOT=$(getprop ro.boot.slot_suffix)
BOOT_IMG="$WORKING_PATH/init_boot$ACTIVE_SLOT.img"

echo "Cleaning temp files"
rm -f "$MAGISK_NEWBOOT"

echo "Patching $BOOT_IMG using $MAGISK_PATCH..." >> $LOG_FILE
KEEPVERITY=true KEEPFORCEENCRYPT=true sh "$MAGISK_PATCH" "$BOOT_IMG" >> $LOG_FILE

#MAGISK_OLDBOOT="$MAGISK_PATH/stock-boot.img"
if [ -s "$MAGISK_NEWBOOT" ]
then
    cp -afv "$MAGISK_NEWBOOT" "$WORKING_PATH/init_boot_patched$ACTIVE_SLOT.img" >> $LOG_FILE
    cp -afv "$MAGISK_NEWBOOT" "$DOWNLOAD_PATH/init_boot_patched$ACTIVE_SLOT.img" >> $LOG_FILE
    rm -f "$MAGISK_NEWBOOT" >> $LOG_FILE
fi

echo "Magisk patch init_boot.img complete!" >> $LOG_FILE

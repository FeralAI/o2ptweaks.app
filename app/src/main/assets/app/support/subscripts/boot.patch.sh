#!/system/bin/sh

SCRIPT_DIR="${0%/*}"
[ "$SCRIPT_DIR" = "$0" ] && SCRIPT_DIR=.
. "$SCRIPT_DIR/partition_path.sh"

WORKING_PATH="${THORTOOLS_WORKING_PATH:-/storage/emulated/0/Android/data/dev.adrian.thortools/files}"
DOWNLOAD_PATH="/storage/emulated/0/Download"
LOG_FILE="$WORKING_PATH/boot.patch.log"

echo "Magisk patch boot.img starting..." > $LOG_FILE

# MAGISK_PATH="/data/adb/magisk"
MAGISK_PATH="$1"
MAGISK_PATCH="$MAGISK_PATH/boot_patch.sh"
MAGISK_NEWBOOT="$MAGISK_PATH/new-boot.img"
ACTIVE_SLOT=$(getprop ro.boot.slot_suffix)
case "$ACTIVE_SLOT" in
    a) ACTIVE_SLOT="_a" ;;
    b) ACTIVE_SLOT="_b" ;;
    _a|_b) ;;
    *) exit 1 ;;
esac
BOOT_IMG="$WORKING_PATH/boot$ACTIVE_SLOT.img"
PATCHED_BOOT="$WORKING_PATH/boot_patched$ACTIVE_SLOT.img"
TEMP_PATCHED_BOOT="$PATCHED_BOOT.tmp"

echo "Cleaning temp files"
rm -f "$MAGISK_NEWBOOT" >> $LOG_FILE

echo "Patching $BOOT_IMG using $MAGISK_PATCH..." >> $LOG_FILE
KEEPVERITY=true KEEPFORCEENCRYPT=true sh "$MAGISK_PATCH" "$BOOT_IMG" >> $LOG_FILE

#MAGISK_OLDBOOT="$MAGISK_PATH/stock-boot.img"
if [ -s "$MAGISK_NEWBOOT" ]
then
    rm -f "$TEMP_PATCHED_BOOT"
    if cp -afv "$MAGISK_NEWBOOT" "$TEMP_PATCHED_BOOT" >> "$LOG_FILE" 2>&1 &&
        [ -s "$TEMP_PATCHED_BOOT" ] &&
        mv -f "$TEMP_PATCHED_BOOT" "$PATCHED_BOOT" &&
        cp -afv "$MAGISK_NEWBOOT" "$DOWNLOAD_PATH/boot_patched$ACTIVE_SLOT.img" >> "$LOG_FILE" 2>&1; then
        rm -f "$MAGISK_NEWBOOT"
        echo "Magisk patch boot.img complete!" >> "$LOG_FILE"
        exit 0
    fi
fi

rm -f "$TEMP_PATCHED_BOOT" "$MAGISK_NEWBOOT"
echo "Magisk patch boot.img failed!" >> "$LOG_FILE"
exit 1

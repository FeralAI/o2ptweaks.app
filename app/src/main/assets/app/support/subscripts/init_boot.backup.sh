#!/system/bin/sh

WORKING_PATH="/storage/emulated/0/Android/data/dev.adrian.thortools/files"
DOWNLOAD_PATH="/storage/emulated/0/Download"
LOG_FILE="$WORKING_PATH/backup.init_boot.log"

mkdir -p "$WORKING_PATH" "$DOWNLOAD_PATH"
echo "ThorTools init_boot backup started" > "$LOG_FILE"

for ACTIVE_SLOT in _a _b; do
    BOOT_DEVICE="/dev/block/by-name/init_boot$ACTIVE_SLOT"
    OUTPUT_FILE="$WORKING_PATH/init_boot$ACTIVE_SLOT.img"
    if [ -e "$BOOT_DEVICE" ]; then
        dd if="$BOOT_DEVICE" of="$OUTPUT_FILE" >> "$LOG_FILE" 2>&1
        if [ -s "$OUTPUT_FILE" ]; then
            cp -f "$OUTPUT_FILE" "$DOWNLOAD_PATH/" >> "$LOG_FILE" 2>&1
        fi
    fi
done

echo "ThorTools init_boot backup complete" >> "$LOG_FILE"

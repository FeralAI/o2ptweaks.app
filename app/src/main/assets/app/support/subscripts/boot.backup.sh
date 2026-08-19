#!/system/bin/sh

WORKING_PATH="/storage/emulated/0/Android/data/dev.adrian.thortools/files"
DOWNLOAD_PATH="/storage/emulated/0/Download"
LOG_FILE="$WORKING_PATH/backup.boot.log"

mkdir -p "$WORKING_PATH" "$DOWNLOAD_PATH"
echo "ThorTools boot backup started" > "$LOG_FILE"

for ACTIVE_SLOT in _a _b; do
    BOOT_DEVICE="/dev/block/by-name/boot$ACTIVE_SLOT"
    OUTPUT_FILE="$WORKING_PATH/boot$ACTIVE_SLOT.img"
    if [ -e "$BOOT_DEVICE" ]; then
        dd if="$BOOT_DEVICE" of="$OUTPUT_FILE" >> "$LOG_FILE" 2>&1
        if [ -s "$OUTPUT_FILE" ]; then
            cp -f "$OUTPUT_FILE" "$DOWNLOAD_PATH/" >> "$LOG_FILE" 2>&1
        fi
    fi
done

echo "ThorTools boot backup complete" >> "$LOG_FILE"

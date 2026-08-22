#!/system/bin/sh

SCRIPT_DIR="${0%/*}"
[ "$SCRIPT_DIR" = "$0" ] && SCRIPT_DIR=.
. "$SCRIPT_DIR/partition_path.sh"

WORKING_PATH="${THORTOOLS_WORKING_PATH:-/storage/emulated/0/Android/data/dev.adrian.thortools/files}"
DOWNLOAD_PATH="/storage/emulated/0/Download"
LOG_FILE="$WORKING_PATH/backup.init_boot.log"

mkdir -p "$WORKING_PATH" "$DOWNLOAD_PATH"
echo "ThorTools init_boot backup started" > "$LOG_FILE"
copied=0
expected=0
failed=0

for ACTIVE_SLOT in _a _b; do
    BOOT_DEVICE="$(resolve_partition "init_boot$ACTIVE_SLOT")"
    [ -n "$BOOT_DEVICE" ] || continue
    expected=$((expected + 1))
    OUTPUT_FILE="$WORKING_PATH/init_boot$ACTIVE_SLOT.img"
    TEMP_FILE="$OUTPUT_FILE.tmp"
    if [ -e "$BOOT_DEVICE" ]; then
        rm -f "$TEMP_FILE"
        if dd if="$BOOT_DEVICE" of="$TEMP_FILE" >> "$LOG_FILE" 2>&1 &&
            [ -s "$TEMP_FILE" ] &&
            mv -f "$TEMP_FILE" "$OUTPUT_FILE"; then
            DOWNLOAD_FILE="$DOWNLOAD_PATH/init_boot$ACTIVE_SLOT.img"
            if cp -f "$OUTPUT_FILE" "$DOWNLOAD_FILE" >> "$LOG_FILE" 2>&1 && [ -s "$DOWNLOAD_FILE" ]; then
                copied=$((copied + 1))
            else
                failed=1
            fi
        else
            failed=1
        fi
        rm -f "$TEMP_FILE"
    fi
done

if [ "$expected" -gt 0 ] && [ "$copied" -eq "$expected" ] && [ "$failed" -eq 0 ]; then
    echo "ThorTools init_boot backup complete" >> "$LOG_FILE"
    exit 0
fi
echo "ThorTools init_boot backup failed" >> "$LOG_FILE"
exit 1

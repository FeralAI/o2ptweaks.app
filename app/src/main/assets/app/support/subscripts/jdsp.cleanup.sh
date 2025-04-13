#!/system/bin/sh

SDIR="$1"
TMPFS="$SDIR/support/o2ptweaks_tmpfs"
TMPFS64="$SDIR/support/o2ptweaks_tmpfs64"
SOUNDFX_DIR=/vendor/lib/soundfx
SOUNDFX_DIR64=/vendor/lib64/soundfx

#pm disable james.dsp

# Cleanup
for m in $(mount |grep tmpfs | grep $(basename $TMPFS)| awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
	umount -l "$m"
done

for m in $(mount |grep tmpfs | grep $(basename $TMPFS64)| awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
	umount -l "$m"
done

for m in $(mount |grep tmpfs | grep "$SOUNDFX_DIR"| awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
	umount -l "$m"
done

for m in $(mount | grep tmpfs | grep "$SOUNDFX_DIR64" | awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
  umount -l "$m"
done

umount /system/etc/audio_effects.conf
umount /system/vendor/etc/audio/sku_kalama/audio_effects.conf
umount /system/vendor/etc/audio/sku_kalama/audio_effects.xml
umount /vendor/etc/default_volume_tables.xml

# Restart audio system
killall -q audioserver
killall -q mediaserver

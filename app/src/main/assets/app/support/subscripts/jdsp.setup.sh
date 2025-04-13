#!/system/bin/sh

SDIR="$1"
TMPFS="$SDIR/support/o2ptweaks_tmpfs"
TMPFS64="$SDIR/support/o2ptweaks_tmpfs64"
SOUNDFX_DIR=/vendor/lib/soundfx
SOUNDFX_DIR64=/vendor/lib64/soundfx

echo "Start cleanup"

for m in $(mount | grep tmpfs | grep $(basename $TMPFS) | awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
  umount -l "$m"
done

for m in $(mount | grep tmpfs | grep $(basename $TMPFS64) | awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
  umount -l "$m"
done

for m in $(mount | grep tmpfs | grep "$SOUNDFX_DIR" | awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
  umount -l "$m"
done

for m in $(mount | grep tmpfs | grep "$SOUNDFX_DIR64" | awk -F' on ' '{print $2}' | awk -F' type ' '{print $1}') ; do
  umount -l "$m"
done

umount /system/etc/audio_effects.conf
umount /system/vendor/etc/audio/sku_kalama/audio_effects.conf
umount /system/vendor/etc/audio/sku_kalama/audio_effects.xml
umount /vendor/etc/default_volume_tables.xml

echo "End cleanup"


# Registers JamesDSP library in the Android Audio effect chain
echo "Mounting audio_effects.conf"
mount -o bind $SDIR/support/conf_files/audio_effects-jdsp.conf /system/etc/audio_effects.conf
chown root:root /system/etc/audio_effects.conf
chmod 0644      /system/etc/audio_effects.conf
chcon u:object_r:vendor_configs_file:s0 /system/etc/audio_effects.conf

echo "Mounting sku_kalama/audio_effects.conf"
mount -o bind $SDIR/support/conf_files/sku_kalama/audio_effects-jdsp.conf /system/vendor/etc/audio/sku_kalama/audio_effects.conf
chown root:root /system/vendor/etc/audio/sku_kalama/audio_effects.conf
chmod 0644      /system/vendor/etc/audio/sku_kalama/audio_effects.conf
chcon u:object_r:vendor_configs_file:s0 /system/vendor/etc/audio/sku_kalama/audio_effects.conf

echo "Mounting sku_kalama/audio_effects.xml"
mount -o bind $SDIR/support/conf_files/sku_kalama/audio_effects-jdsp.xml /system/vendor/etc/audio/sku_kalama/audio_effects.xml
chown root:root /system/vendor/etc/audio/sku_kalama/audio_effects.xml
chmod 0644      /system/vendor/etc/audio/sku_kalama/audio_effects.xml
chcon u:object_r:vendor_configs_file:s0 /system/vendor/etc/audio/sku_kalama/audio_effects.xml


# Create TMPFS partitions for audio effects
if [ ! -d "$TMPFS" ]; then
	echo "Creating mountpoint $TMPFS"
	mkdir "$TMPFS"
fi
mount -t tmpfs tmpfs $TMPFS

if [ ! -d "$TMPFS64" ]; then
	echo "Creating mountpoint $TMPFS64"
	mkdir "$TMPFS64"
fi
mount -t tmpfs tmpfs $TMPFS64

# Copy effects libraries to TMPFS folders
echo "Copying sfx libs to TMPFS"
VDIR="$SDIR/support/libs"
cp $VDIR/libjamesdsp.so $TMPFS/
cp -av $SOUNDFX_DIR/* $TMPFS/
cp $VDIR/libjamesdsp.so $TMPFS64/
cp -av $SOUNDFX_DIR64/* $TMPFS64/
	
# bind mount the cooked TMPFS over the system soundfx dir
echo "Mount TMPFS"
mount -o bind $TMPFS $SOUNDFX_DIR
mount -o bind $TMPFS64 $SOUNDFX_DIR64

# Set permissions and SELinux context
echo "Set TMPFS permissions"
chown root:root $SOUNDFX_DIR/*
chmod 0644      $SOUNDFX_DIR/*
chcon u:object_r:vendor_configs_file:s0 $SOUNDFX_DIR/*
chown root:root $SOUNDFX_DIR64/*
chmod 0644      $SOUNDFX_DIR64/*
chcon u:object_r:vendor_configs_file:s0 $SOUNDFX_DIR64/*


#O2P speaker volume curve starts too high, patch it with updated default_volume_tables.xml
echo "Mounting default_volume_tables.xml"
mount -o bind  $SDIR/support/conf_files/default_volume_tables.xml /vendor/etc/default_volume_tables.xml
chown root:root /vendor/etc/default_volume_tables.xml
chmod 0644      /vendor/etc/default_volume_tables.xml
chcon u:object_r:vendor_configs_file:s0 /vendor/etc/default_volume_tables.xml

# Copy JamesDSP backup to storage root
cp -afv $SDIR/support/conf_files/jamesdsp_backup_o2ptweaks.backup /storage/emulated/0/Download/jamesdsp_backup_o2ptweaks.tar.gz

#Enable hidden API
settings put global hidden_api_policy 1

# Restart audio system
echo "Restarting audio systems"
killall -q audioserver
killall -q mediaserver
	
# Enabled JamesDSP
pm enable james.dsp

# Start JamesDSP app to trigger plugin
am start james.dsp/me.timschneeberger.rootlessjamesdsp.activity.EngineLauncherActivity

#!/system/bin/sh

SDIR="$1"

echo "Start cleanup"
umount /vendor/etc/default_volume_tables.xml
echo "End cleanup"

#O2P speaker volume curve starts too high, patch it with updated default_volume_tables.xml
echo "Mounting default_volume_tables.xml"
mount -o bind  $SDIR/support/conf_files/default_volume_tables.xml /vendor/etc/default_volume_tables.xml
chown root:root /vendor/etc/default_volume_tables.xml
chmod 0644      /vendor/etc/default_volume_tables.xml
chcon u:object_r:vendor_configs_file:s0 /vendor/etc/default_volume_tables.xml

# Restart audio system
echo "Restarting audio systems"
killall -q audioserver
killall -q mediaserver

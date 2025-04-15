#!/system/bin/sh

umount /vendor/etc/default_volume_tables.xml

# Restart audio system
killall -q audioserver
killall -q mediaserver

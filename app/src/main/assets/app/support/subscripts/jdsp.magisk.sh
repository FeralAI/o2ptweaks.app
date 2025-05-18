#!/system/bin/sh

WORKING_PATH="/storage/emulated/0/Android/data/com.feralai.o2ptweaks/files"
DOWNLOAD_PATH="/storage/emulated/0/Download"
APPFILES_PATH="/data/data/com.feralai.o2ptweaks/files"
LOG_FILE="$WORKING_PATH/jdsp.magisk.log"

echo "Install JamesDSP Magisk Module started" > $LOG_FILE

cp -fv $APPFILES_PATH/app/support/magisk/jdsp_v6.4-trimmed.zip $DOWNLOAD_PATH/ >> $LOG_FILE
magisk --install-module $DOWNLOAD_PATH/jdsp_v6.4-trimmed.zip >> $LOG_FILE

echo "Install JamesDSP Magisk Module finished" >> $LOG_FILE

#!/system/bin/sh

WORKING_PATH="/storage/emulated/0/Android/data/com.feralai.o2ptweaks/files"
DOWNLOAD_PATH="/storage/emulated/0/Download"
APPFILES_PATH="/data/data/com.feralai.o2ptweaks/files"
LOG_FILE="$WORKING_PATH/jdsp.magisk.log"

echo "Install JamesDSP Magisk Module started" > $LOG_FILE

cp -fv $APPFILES_PATH/app/support/magisk/ainur_jamesdsp-v6.1-trimmed.zip $DOWNLOAD_PATH/ >> $LOG_FILE
magisk --install-module $DOWNLOAD_PATH/ainur_jamesdsp-v6.1-trimmed.zip >> $LOG_FILE

echo "Install JamesDSP Magisk Module finished" >> $LOG_FILE

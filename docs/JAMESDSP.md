# JamesDSP Setup

O2P Tweaks has 2 options for enabling JamesDSP for handling system-wide EQ.

The temp root method is meant to be used on the Odin2 Portal only, but may work on other devices with the SD8Gen2 processor.

The root method can be used on any rooted device.

The included JamesDSP backup file is only intended for use with the Odin 2 Portal.

## Setup

### Enabling JamesDSP (Temp Root)

1. Open O2P Tweaks and navigate to the `Tweaks` page.
2. Go the the `Sound > JamesDSP` section and tap `Install JamesDSP Manager`
3. When prompted, allow JamesDSP Manager to send you notifications.
4. Go the the `Sound > JamesDSP` section and tap the toggle switch to enable. This will load the JamesDSP library immediately and should open the JamesDSP manager app.

### Enabling JamesDSP (Magisk Root)

1. Open O2P Tweaks and navigate to the `Tweaks` page.
2. Go the the `Sound > JamesDSP` section and tap `Install JamesDSP Manager`
3. When prompted, allow JamesDSP Manager to send you notifications.
4. Go the the `Sound > JamesDSP` section and tap `Install JamesDSP Module`
5. Reboot your device after you see the install complete message for the module

### Notes

A few other steps may be required:

* Using the Temp Root method, in the JamesDSP manager you may need to toggle the `Settings > Audio processing > Legacy mode` option off and on again to engage the JamesDSP library (should be on when finished) if it wasn't loaded on startup
* If needed, turn on JamesDSP by tapping the "Power on" icon in the center/lower part of the screen.

## Odin 2 Portal EQ

Using the provided JamesDSP backup file (Odin2 Portal only):

### Benefit/what to expect:
* An improvement in sound quality and volume control from the speakers.
* A fairly linear frequency response from 150Hz to 10kHz

### What NOT to expect:
* You won't hear anything lower than 150Hz

### Drawbacks:
* On some devices, the audio output latency may increase by up to 70ms when using JamesDSP.

Anecdotally, there is a negligible impact on latency for the Odin2 Portal.

### Configuring JamesDSP for the Odin 2 Portal

1. Open JamesDSP Manager
2. Tap the cog icon in the lower/left side of the screen to enter the Settings page
3. Select "Backup and restore > Restore backup"
4. Browse to your internal storage "Downloads" folder (/storage/emulated/0/Download) and select the file named jamesdsp_backup_o2ptweaks.tar.gz, which was copied during the JamesDSP installation.
5. When prompted, choose to do a Clean restore

This EQ backup only targets the internal speakers. You can create profiles for other output devices in the JamesDSP manager when they are connected.

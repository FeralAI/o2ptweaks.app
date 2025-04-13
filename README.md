# 🛠️ O2P Tweaks App

O2P Tweaks is an Android application that leverages the temporary root functionality of the Odin2 Portal to apply fixes and enhancements to your device.

Features:

* Adjusted volume curve for speaker output for lower minimum volume
* Installs JamesDSP for system-wide audio equalization 

> This app is only for the Odin2 Portal. Running this on any other device is unsupported.

### Benefit/what to expect:
* An improvement in sound quality and volume control from the speakers.
* A fairly linear frequency response from 150Hz to 10kHz

### What NOT to expect:
* You won't hear anything lower than 150Hz

### Drawbacks:
* The audio output latency may increase by up to 70ms. (needs verification)
* The output volume will be slightly lower
* CPU usage may be slightly higher (<1%)

-----------------------------
### **ELI5, OVER-DETAILED HOW TO:**
-----------------------------
![O2P Tweaks Main Screen](repo_images/shot1.png?raw=true)

* Download and install O2P_Tweak.apk from the release/assets page
* Open the app
* Allow O2P Tweaks to send you notifications when prompted.
* Tap on "Enable JamesDSP"
* Tap on "Install bundled JamesDSP..."

	When asked:
	* Confirm to "Open with package installer"
	* Install unknown apps: -> allow from this source.
	* Finally confirm JamesDSP installation	

* Open the newly installed application
	* Allow JamesDSP to send you notifications

* Now let's configure JamesDSP for Retroid pocket 5 speakers:
	* Tap the cog icon in the lower/left side of the screen to enter the Settings page
	* Select "Backup and restore > Restore backup"
	* Browse to your internal storage "Downloads" folder (/storage/emulated/0/Downloads) and select the file named `jamesdsp_backup_o2ptweaks.tar.gz`, which was copied during the JamesDSP installation.
	* You may need to toggle the "Settings > Audio processing > Legacy mode" option off and on again to engage the JamesDSP library (should be on when finished)
	* If needed, turn on JamesDSP by tapping the "Power on" icon in the center/lower part of the screen.

## You're done!

Now go back to the O2P Tweaks app select if you want or not JamesDSP to start at every boot.

Reboot to verify everything works correctly (give the app a few secs to setup everything).

## Credits/License:

This app is based on the [jdsp4rp5.app](https://github.com/kokoko3k/jdsp4rp5.app) created by kokoko3k, and inherits the GNU General Public License v2.0.

Original credits from the jdsp4rp5.app:

* Provided libjamesdsp.so by James Fung 
* Provided JamesDSPManagerThePBone.apk by James Fung
* Thanks to ShadoV from JamesDSP [Community] Telegram channel
for insights and support over audio_effects.xml
* Thanks to Sayrune from RP5's discord channel
for support on audio_policy_configuration.xml
* Audio analysis done via: Room EQ Wizard https://www.roomeqwizard.com

# <img src="docs/images/logo.webp" style="width: 24px;"> O2P Tweaks App

O2P Tweaks is an application that leverages the temporary root functionality of some devices to apply fixes and enhancements to Android handhelds.

This app was originally designed for the Odin2 Portal. Most features work on other Android handhelds that meet requirements, but YMMV.

## Features

* Install JamesDSP for system-wide audio equalization (O2P only)
  * Includes JamesDSP backup with with tuned EQ curve for the Odin 2 Portal speaker 
* Adjusted volume curve for speaker output for lower minimum volume (O2P only)
* Adjust system DPI
* Adjust system animation speed
* System info page with software and hardware details for firmware, battery, etc
* EZ Root feature lets you root your device, on your device, without a PC!

If running a rooted device, O2P Tweaks will apply tweaks via an auto-generated Magisk module, and some new options become available:

* Install JamesDSP via Magisk module (compatible with more devices)
* Adjust system DPI at boot instead of runtime (fixes some UI scaling issues)
* Set the number of volume steps for more granular volume control

## Usage

Download the latest release to your Android device and install.

When prompted, allow notifications. This allows O2P Tweaks to run the tweaks on system startup.

Check out these pages for more details:

* [User Guide](docs/USERGUIDE.md)
* [JamesDSP Setup](docs/JAMESDSP.md)
* [EZ Root Guide](docs/EZROOT.md)

## Credits/License:

The initial release of this app is based on the [jdsp4rp5.app](https://github.com/kokoko3k/jdsp4rp5.app) created by kokoko3k, and inherits the GNU General Public License v2.0.

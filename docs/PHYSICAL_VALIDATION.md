# Physical Thor Validation

Complete this checklist before publishing a beta or stable build. Alpha builds are suitable for emulator validation and cautious hardware diagnostics, but they are not evidence that partition writes are safe on a physical Thor.

## Test matrix

Record the complete property dump for at least these devices:

| Device | SoC | Build and firmware | Result | Evidence |
| --- | --- | --- | --- | --- |
| Thor Lite | Snapdragon 865 |  |  |  |
| Thor Base, Pro, or Max | Snapdragon 8 Gen 2 |  |  |  |

## Preflight

- Confirm the bootloader is unlocked and the device can obtain the AYN privileged root service.
- Record manufacturer, brand, model, device, product, board, hardware, SoC, firmware, build, serial, and active slot.
- Record both display geometries, refresh rates, orientation, and touch behavior.
- Confirm battery is at least 35% and the recovery folder is writable.
- Confirm the app remains diagnostics-only on a non-Thor or when the privileged service is unavailable.

## Recovery images

- Back up every available `boot` and `init_boot` slot.
- Confirm each non-empty image appears in the app recovery folder and `Download`.
- Compare the SHA-256 values shown by ThorTools with independently calculated hashes.
- Restart the app and confirm stock images and hashes remain visible.
- Remove or hide the app-local stock image, verify the Download fallback is detected, and restore from it.

## Root flow

- Install Magisk and prepare a patch from the active-slot stock image.
- Confirm the selected image type is the partition actually present on the device.
- Flash only the active slot after the lower-screen confirmation.
- Reboot, verify Magisk and root state, and confirm both displays remain usable.
- Exercise DPI, animation speed, volume steps, and boot-animation settings, including reboot persistence.
- Restore stock, reboot, and verify the device returns to its unrooted state.
- Repeat the restore and reboot flow after an interrupted app operation.

## OTA and rollback

- With stock images restored, install an OTA and record the resulting active slot and firmware.
- Re-run diagnostics and verify that stale patched images cannot be selected for flashing.
- Repeat backup, patch, flash, reboot, and restore on the second tested device.

## Release evidence

Attach the property dumps, image hashes, operation logs, screenshots of both panels, OTA result, and the tested APK version to the release record. A beta or stable tag should not be created until every required row has a result and the stock restore path has been verified on both SoC classes.

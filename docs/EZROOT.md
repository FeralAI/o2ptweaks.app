# EZ Root for AYN Thor

EZ Root is an on-device workflow for backing up, Magisk-patching, flashing, and restoring Thor boot partitions. It is not a universal Android flashing utility.

## Before starting

- Confirm the bootloader is unlocked and the Thor can run its privileged root script service.
- Charge the Thor to at least 35%.
- Install Magisk and keep an independent copy of the stock images.
- Do not begin a firmware update until stock images have been restored.

## Workflow

1. Open the EZ Root section on the lower display and review the upper dashboard.
2. Download and install Magisk if it is not already present.
3. Choose **Back up available slots**. ThorTools stores every discovered slot image in its app folder, copies each one to `Download`, and shows the exact app-local paths and SHA-256 hashes on both displays.
4. Choose **Prepare root patch**. ThorTools uses the active slot's `init_boot` image when that partition exists and uses `boot` only when `init_boot` is unavailable; it blocks if the selected target lacks a verified stock image.
5. Review the displayed image hashes and confirm **Flash active-slot patch**.
6. After reboot, complete Magisk's additional setup.

Before an OTA or service visit, use **Restore stock image**, confirm the active-slot prompt, and wait for the reboot to finish. If the app-local copy is unavailable, the restore script falls back to the matching `Download` copy.

ThorTools records the build fingerprint, slot, partition, file size, and SHA-256 for every stock and patched image. An OTA, image replacement, or tampering invalidates the record, so repeat the stock-backup step after every firmware change before preparing another patch.

If any capability check is unavailable, ThorTools leaves mutating actions disabled and keeps the device in diagnostics-only mode.

Before a beta or stable release, complete the [physical Thor validation checklist](PHYSICAL_VALIDATION.md) on both required SoC classes.

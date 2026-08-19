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
3. Choose **Back up both slots**. ThorTools stores non-empty images in its app folder and copies them to `Download`.
4. Choose **Prepare root patch**. ThorTools tries the active slot's `init_boot` image first and falls back to `boot`.
5. Review the displayed image hashes and confirm **Flash active-slot patch**.
6. After reboot, complete Magisk's additional setup.

Before an OTA or service visit, use **Restore stock image**, confirm the active-slot prompt, and wait for the reboot to finish.

If any capability check is unavailable, ThorTools leaves mutating actions disabled and keeps the device in diagnostics-only mode.

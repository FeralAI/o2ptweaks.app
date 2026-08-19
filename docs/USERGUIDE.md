# ThorTools User Guide

ThorTools is designed around the AYN Thor's two displays. The upper display shows device state, capability checks, operation progress, results, and recovery information. The lower display contains the controls and confirmations.

## Status

The Status section reports the detected Thor variant, raw model information, firmware, build, active slot, battery, kernel, root state, Magisk state, and available boot partitions. Refresh before starting a sensitive operation.

## Tweaks

Display density and animation speed are applied through Android system settings. On a rooted Thor, volume steps and boot-animation behavior are persisted through the ThorTools Magisk module and require a reboot when the dashboard says so.

The controls are disabled when Thor identity, the privileged root service, or root access is missing.

## EZ Root

The root flow checks the active slot and partition layout before every backup, patch, flash, and restore. It requires a 35% battery level, a non-empty image, and an explicit confirmation on the lower display for writes. Cached stock and patched images are hashed with SHA-256 and shown on the dashboard.

## Recovery

Restore stock before installing an OTA. If the app is interrupted, the next launch shows the interrupted operation in the dashboard without repeating it. Keep the Download-folder copies of the stock images until the Thor has rebooted successfully.

## Scope

ThorTools v1 does not include the Odin 2 Portal speaker curve or JamesDSP assets. Those features require Thor-specific audio measurement and physical validation.

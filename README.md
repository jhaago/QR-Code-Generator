# QR Code Generator

A simple native Android QR code generator built with Kotlin and Jetpack Compose.

## Current scope

The initial build provides:

- URL/text entry
- Offline QR generation
- Live QR preview
- PNG saving to the device Pictures folder
- Android share sheet support
- Clean architecture ready for QR style presets and colour controls

## Planned next steps

- Multiple QR module styles (classic, rounded, dots)
- Foreground/background colour selection
- More export sizes
- Optional embedded logo with safe error-correction rules
- Lightweight local history/favourites

## Technology

- Kotlin
- Jetpack Compose + Material 3
- ZXing Core for QR encoding
- Android MediaStore for saving images

No server or account is required; QR codes are generated locally on the device.

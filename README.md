# Warehouse Location Scanner

> Modernized, native Android application designed for high speed warehouse floor operations. Replaces legacy system interfaces with a streamlined, error-proof scanning experience.

## Hardware Integration

This application is optimized and designed specifically for the **Zebra TC22** enterprise handheld scanner. Bypasses standard camera-based scanning by directly utilizing the Zebra **DataWedge** intent broadcast service for zero-latency barcode processing.

## Tech Stack

* **Language:** Kotlin
* **Minimum SDK:** Android 8.0 (API 26)
* **Target SDK:** Android 14 (API 34)
* **Networking:** OkHttp3 (RESTful communication)
* **UI/UX:** Material Design (Dark Industrial Theme)
* **Architecture:** MVVM (`MainActivity`, `ScannerViewModel`, `ScannerUiState`)

## Key Features (Version 1.0.0)

* **MVVM Architecture:** Robust state preservation using `ScannerViewModel` and `ScannerUiState`, preventing data loss across device configuration and orientation changes.
* **Dark Industrial UI:** High-contrast, card-based Material Design interface tailored for warehouse floor environments.
* **Worker ID Gatekeeper (`LoginActivity`):** Client-side worker ID sanitization (max 20 chars, alphanumeric + `_-`) that passes `USER_SIGNATURE` via Intent extra to the scanning workflow.
* **Zero-Latency Zebra DataWedge Integration (`ZebraScanReceiver`):** Listens for broadcast intent `com.frontera.scanner.ACTION` and decodes scan data via `contentResolver` on Android 11+ for instant hardware barcode processing.
* **Two-Step Scan State Machine:** Strict enforcement of scanning sequence (1st scan: Source/Item, 2nd scan: Destination/Location) to prevent unintended data entry errors.
* **Audit Logging & Hyper-V Server Sync:** Transmits full audit payloads including `device_id` (`<Build.MODEL>-<ANDROID_ID>`) and `user_id` (`USER_SIGNATURE`) to the backend endpoint (`http://${BuildConfig.SERVER_IP}:5000/scanner/location_update`).
* **Secure Environment Variables:** Internal server IPs are injected at compile time via `BuildConfig.SERVER_IP`, keeping sensitive infrastructure out of version control.
* **Adaptive Network Security:** Custom network security configuration allowing local HTTP communication for internal server routing while maintaining secure standards.

---

## Installation & Setup

To clone and compile this project locally, you must specify your own internal server IP address. Sensitive infrastructure configuration is managed outside version control via `BuildConfig.SERVER_IP`.

### Build Commands
* **Compile Debug APK:** `./gradlew assembleDebug`
* **Run Unit Tests:** `./gradlew testDebugUnitTest`

---

## Roadmap

### Version 1.0.0 (Completed)
- [x] **State Management:** Implement `ScannerViewModel` and `ScannerUiState` to preserve active scan data during device orientation changes.
- [x] **UI Overhaul:** Dark Industrial, card-based Material Design interface overhaul.
- [x] **Worker ID Authentication:** `LoginActivity` gatekeeper with worker ID sanitization and signature passing.
- [x] **Hardware Scan Receiver:** Native `ZebraScanReceiver` DataWedge intent processing.
- [x] **Audit Logging:** Attach `device_id` and `user_id` signatures to HTTP POST requests to track all scan actions on the Hyper-V server.

### Version 1.1.0 Roadmap (Completed)
- [x] **Custom Adaptive App Icon:** Modern adaptive launcher icons for Android devices.
- [x] **Audio & Haptic Scan Feedback:** Audio tones (`ToneGenerator`) and vibration cues (`Vibrator`) for scan confirmation and error alerts.
- [x] **Recent Scans Session History:** On-screen log/list of recently completed item-location pairings during the active session.

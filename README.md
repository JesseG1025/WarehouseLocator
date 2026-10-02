# Warehouse Location Scanner

> Modernized, native Android application designed for high-speed warehouse floor operations. Features a dual-server architecture and encrypted token authentication to securely interface with legacy systems via a streamlined, error-proof scanning experience.

## Hardware Integration

This application is optimized and designed specifically for the **Zebra TC22** enterprise handheld scanner. Bypasses standard camera-based scanning by directly utilizing the Zebra **DataWedge** intent broadcast service for zero-latency barcode processing.

## Tech Stack

* **Language:** Kotlin
* **Minimum SDK:** Android 8.0 (API 26)
* **Target SDK:** Android 14 (API 34)
* **Networking:** Retrofit2 & OkHttp3 (RESTful communication, Interceptors)
* **Security:** AndroidX Security Crypto (`EncryptedSharedPreferences`)
* **UI/UX:** Material Design (Dark Industrial Theme)
* **Architecture:** MVVM (`MainActivity`, `ScannerViewModel`, `ScannerUiState`)

## System Architecture: Dual-Server Flow

The application bridges modern mobile security with legacy database infrastructure by routing traffic between two distinct internal endpoints:

1. **Authentication Server:** A Flask-based REST API that validates user credentials against a hashed SQLite database and issues a secure `X-API-Key` session token upon successful login.
2. **Operations Gateway:** A Windows Hyper-V server running legacy PyODBC drivers that receives the injected token, validates the session, and executes `UPDATE` commands on the Advantage Database Server (ADS).

## Key Features (Version 1.2.0)

* **Secure Authentication & Token Management:** Replaced client-side sanitization with a full server-side login flow. Tokens are stored securely on the hardware level using `EncryptedSharedPreferences` (AES256_GCM).
* **Automated Request Authorization (`AuthInterceptor`):** Automatically injects the stored `X-API-Key` into all operational HTTP headers. Rejects `401 Unauthorized` responses and instantly routes the user back to the login gate.
* **MVVM Architecture:** Robust state preservation using `ScannerViewModel` and `ScannerUiState`, preventing data loss across device configuration and orientation changes.
* **Dark Industrial UI:** High-contrast, card-based Material Design interface tailored for warehouse floor environments.
* **Zero-Latency Zebra DataWedge Integration (`ZebraScanReceiver`):** Listens for broadcast intent `com.frontera.scanner.ACTION` and decodes scan data via `contentResolver` on Android 11+ for instant hardware barcode processing. 
* **Custom Network Security Manifest Overrides:** Bypasses aggressive third-party DataWedge SDK cleartext blocking using explicit `tools:replace` manifest directives, allowing seamless communication with local `192.168.1.X` subnets.
* **Two-Step Scan State Machine:** Strict enforcement of scanning sequence (1st scan: Source/Item, 2nd scan: Destination/Location) to prevent unintended data entry errors.

---

## Installation & Setup

To clone and compile this project locally, you must specify your own internal server IP address. Sensitive infrastructure configuration is managed outside version control via `BuildConfig.SERVER_IP`.

### Build Commands
* **Compile Debug APK:** `./gradlew assembleDebug`
* **Run Unit Tests:** `./gradlew testDebugUnitTest`

---

## Roadmap

### Version 1.2.0 (Completed)
- [x] **API Migration:** Replaced raw HTTP connections with a Retrofit2 networking client.
- [x] **Secure Auth Flow:** Implemented username/password login endpoint routing.
- [x] **Token Persistence:** Upgraded storage to `EncryptedSharedPreferences` to prevent device-level token extraction.
- [x] **Network Security Overrides:** Patched Android 9+ cleartext traffic restrictions conflicting with DataWedge SDKs.

### Version 1.1.0 (Completed)
- [x] **Custom Adaptive App Icon:** Modern adaptive launcher icons for Android devices.
- [x] **Audio & Haptic Scan Feedback:** Audio tones (`ToneGenerator`) and vibration cues (`Vibrator`) for scan confirmation and error alerts.
- [x] **Recent Scans Session History:** On-screen log/list of recently completed item-location pairings during the active session.

### Version 1.0.0 (Completed)
- [x] **State Management:** Implement `ScannerViewModel` and `ScannerUiState`.
- [x] **UI Overhaul:** Dark Industrial, card-based Material Design interface.
- [x] **Hardware Scan Receiver:** Native `ZebraScanReceiver` DataWedge intent processing.
- [x] **Audit Logging:** Attach `device_id` and `user_id` signatures to HTTP POST requests to track all scan actions on the Hyper-V server.

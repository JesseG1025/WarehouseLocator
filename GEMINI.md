# 1. Project Overview & Stack

- **Purpose** This android application's purpose is to place items with their corresponding location via a Zebra TC22.
- **Stack:** Kotlin, AndroidX, Material Design, MVVM (`MainActivity`, `ScannerViewModel`, `ScannerUiState`), `OkHttpClient`.
## 2. Architecture & Quirks
- **Login Gatekeeper (`LoginActivity.kt`):** Launcher activity. Sanitizes worker IDs client-side (max 20 chars, alphanumeric + `_-`) and passes `"USER_SIGNATURE"` via Intent extra to `MainActivity`.
- **Zebra DataWedge (`ZebraScanReceiver.kt`):** Listens for Broadcast Intent `com.frontera.scanner.ACTION`. Uses `contentResolver` on Android 11+ to decode URI data. Do NOT replace with keyboard/keystroke listeners.
- **Two-Step Scan State Machine:** 1st scan sets `sourceBarcode` (Item). 2nd scan sets `destinationBarcode` (Location) and triggers the POST request.
- **API Payload (`http://${BuildConfig.SERVER_IP}:5000/scanner/location_update`):**
  ```json
  {
    "item": "<sourceBarcode>",
    "location": "<destinationBarcode>",
    "device_id": "<Build.MODEL>-<ANDROID_ID>",
    "user_id": "<USER_SIGNATURE>"
  }

## 3. Build & Run Commands
- Compile Debug APK: ./gradlew assembleDebug
- Run Unit Tests: ./gradlew testDebugUnitTest

## 4. Strict Developer Guardrails
- Preserve Comments: NEVER delete or strip existing developer comments when editing or refactoring code.
- Component Checklist: Before starting code for any new feature or project, ALWAYS list all hardware, software, and file components needed first.
- Full Code vs. Guidance: When asked to be guided on code, guide step-by-step without dumping full code. When asked for code updates, provide complete files rather than fragmented snippets.
- Security: Never hardcode IP addresses or credentials; always use BuildConfig.SERVER_IP
- Comments: Whenever adding new code ensure that you comment what the code accomplishes and why it was used.
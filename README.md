Warehouse Location Scanner

>Modernized, native Android application designed for high speed warehouse floor operations. Replaces 
> legacy system interfaces with a streamlined, error proof scanning experience.

Hardware Integration

This application is optimized and designed specifically for the **Zebra TC22** enterprise handheld scanner.
Bypasses standard camera based scanning by directly utilizing the Zebra **Datawedge** intent broadcast
service for zero latency barcode processing.

Tech Stack

* **Language:** Kotlin
* **Minimum SDK:** Android 8.0 (API 26)
* **Target SDK:** Android 14 (API 34)
* **Networking:** OkHTTP3 (RESTful communication)
* **UI/UX:** Material Design *(In Progress)*
* **Architecture:** MVVM *(In Progress)*

Key Features
* **Two Step Sequence:** Strict enforcement of scan sequence to prevent any unintended data entry errors.
* **Zero Latency Scanning:** Utilization of native intent listeners such as 'com.symbol.datawedge.decode_data') to process barcodes in the background instantly.
* **Secure Environment Variables:** Internal server IPs are injected only at compile time via 'BuildConfig', keeping sensitive infrastructure out of version control.
* **Adaptive Network Security:** Custom XML network profiles allow for internal cleartext (HTTP) traffic during local debugging, but defaults to secure connections for production.

---

Installation & Setup

To clone and compile this project locally, you must use your own internal server IP address. This repository is configured to block sensitive infrastructure data via '.gitignore'.


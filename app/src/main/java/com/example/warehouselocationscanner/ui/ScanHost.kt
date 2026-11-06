package com.example.warehouselocationscanner.ui

/**
 * Implemented by any tab (Fragment) that wants to receive DataWedge scans.
 * MainActivity owns the single BroadcastReceiver and forwards each scan only to the
 * currently visible tab, so a scan on the Lookup tab never advances the put-away flow.
 */
interface ScanConsumer {
    fun onScan(barcode: String)
}

/**
 * Implemented by MainActivity so tabs can trigger the shared beep/vibrate feedback
 * without each Fragment allocating its own ToneGenerator (a limited system resource).
 */
interface ScanFeedback {
    fun playSuccessFeedback()
    fun playErrorFeedback()
}

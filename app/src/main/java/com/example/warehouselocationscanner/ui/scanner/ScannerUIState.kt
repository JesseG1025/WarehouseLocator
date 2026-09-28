package com.example.warehouselocationscanner.ui.scanner
data class ScannerUiState(
    val sourceBarcode: String? = null,
    val destinationBarcode: String? = null,
    val statusMessage: String = "Status: Waiting for item scan",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    // List to keep track of recently completed item-location pairings
    val recentScans: List<String> = emptyList()
) {
    val isReadyToMatch: Boolean
        get() = sourceBarcode != null && destinationBarcode != null
}
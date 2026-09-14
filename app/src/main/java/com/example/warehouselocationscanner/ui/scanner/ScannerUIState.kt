package com.example.warehouselocationscanner.ui.scanner
data class ScannerUiState(
    val sourceBarcode: String? = null,
    val destinationBarcode: String? = null,
    val statusMessage: String = "Status: Waiting for item scan",
    val isLoading: Boolean = false,
    val isError: Boolean = false
) {
    val isReadyToMatch: Boolean
        get() = sourceBarcode != null && destinationBarcode != null
}
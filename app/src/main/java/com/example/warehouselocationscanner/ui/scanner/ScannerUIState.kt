package com.example.warehouselocationscanner.ui.scanner
data class ScannerUiState(
    val sourceBarcode: String? = null,
    val destinationBarcode: String? = null,
    val statusMessage: String = "Scan Source Item",
    val isLoading: Boolean = false,
    val isError: Boolean = false
) {
    val isReadyToMatch: Boolean
        get() = sourceBarcode != null && destinationBarcode != null
}
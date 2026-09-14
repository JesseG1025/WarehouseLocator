package com.example.warehouselocationscanner.ui.scanner

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScannerViewModel : ViewModel() {

    // Private mutable state (only the ViewModel can change this)
    private val _uiState = MutableStateFlow(ScannerUiState())

    // Public read-only state (MainActivity will observe this)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /**
     * Called by MainActivity whenever DataWedge captures a barcode.
     */
    fun processScan(barcode: String) {
        _uiState.update { currentState ->

            // STEP 1: Source is empty. Assign this barcode to the Source field.
            if (currentState.sourceBarcode == null) {
                currentState.copy(
                    sourceBarcode = barcode,
                    statusMessage = "Item scanned. Now scan location.",
                    isError = false
                )
            }
            // STEP 2: Source is full, but Destination is empty. Assign to Destination.
            else if (currentState.destinationBarcode == null) {
                currentState.copy(
                    destinationBarcode = barcode,
                    statusMessage = "Match ready to process.",
                    isError = false
                )
            }
            // STEP 3: Both are already full. User accidentally scanned a third time.
            else {
                currentState.copy(
                    statusMessage = "Transaction is already full. Please submit or clear.",
                    isError = true
                )
            }
        }
    }

    /**
     * Resets the entire screen back to the default empty state.
     */
    fun resetScan() {
        _uiState.update {
            ScannerUiState() // Calling the data class with no arguments uses the default nulls
        }
    }
}
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
     * Resets the entire screen back to the default empty state, but preserves the recent scans.
     */
    fun resetScan() {
        _uiState.update { currentState ->
            ScannerUiState(recentScans = currentState.recentScans)
        }
    }

    /**
     * Adds a newly completed item-location pair to the history.
     */
    fun addRecentScan(item: String, location: String) {
        _uiState.update { currentState ->
            val updatedList = currentState.recentScans.toMutableList()
            updatedList.add("Item: $item -> Loc: $location")
            currentState.copy(recentScans = updatedList)
        }
    }
}
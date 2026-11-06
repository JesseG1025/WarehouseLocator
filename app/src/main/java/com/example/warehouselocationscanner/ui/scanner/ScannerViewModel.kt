package com.example.warehouselocationscanner.ui.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouselocationscanner.network.LocationUpdateRequest
import com.example.warehouselocationscanner.network.RetrofitClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// AndroidViewModel (instead of ViewModel) gives us the Application context needed by
// RetrofitClient, without leaking an Activity/Fragment context.
class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    // Private mutable state (only the ViewModel can change this)
    private val _uiState = MutableStateFlow(ScannerUiState())

    // Public read-only state (MainActivity will observe this)
    // (Now observed by ScanFragment, which hosts the put-away screen inside the Scan tab.)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    // One-shot success/failure events for beep + vibrate. A SharedFlow (not state) is used
    // so feedback plays exactly once per submission and doesn't replay after rotation.
    private val _feedback = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val feedback: SharedFlow<Boolean> = _feedback.asSharedFlow()

    // Shared Retrofit service (one OkHttp client/connection pool for the whole app).
    private val api = RetrofitClient.getApiService(application)

    /**
     * Called by MainActivity whenever DataWedge captures a barcode.
     * (MainActivity forwards it to ScanFragment, which calls this.)
     * deviceId/userId are passed through so the audit signature travels with the request.
     */
    fun processScan(barcode: String, deviceId: String, userId: String) {
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

        // Submitting from the ViewModel (rather than from the UI's state collector) guarantees
        // the POST fires exactly once per pair, even if the UI re-collects state after rotation.
        val state = _uiState.value
        if (state.isReadyToMatch) {
            submitMatch(state.sourceBarcode!!, state.destinationBarcode!!, deviceId, userId)
        }
    }

    /**
     * Sends the item/location pair to the server on a background coroutine.
     * v1 behavior preserved: fail loudly, no retry queue, and the scan fields reset
     * immediately so the next pair can begin regardless of the request's outcome.
     */
    private fun submitMatch(item: String, location: String, deviceId: String, userId: String) {
        _uiState.update {
            ScannerUiState(
                recentScans = it.recentScans,
                statusMessage = "Status: Sending to server...",
                isLoading = true
            )
        }

        // viewModelScope auto-cancels if the ViewModel is cleared, so no leaked callbacks.
        viewModelScope.launch {
            val result = runCatching {
                api.updateLocation(LocationUpdateRequest(item, location, deviceId, userId))
            }
            val response = result.getOrNull()
            when {
                response == null -> {
                    _uiState.update {
                        it.copy(
                            statusMessage = "Status: Network Error - ${result.exceptionOrNull()?.message}",
                            isLoading = false,
                            isError = true
                        )
                    }
                    _feedback.tryEmit(false)
                }
                !response.isSuccessful -> {
                    _uiState.update {
                        it.copy(
                            statusMessage = "Status: Server Error (${response.code()})",
                            isLoading = false,
                            isError = true
                        )
                    }
                    _feedback.tryEmit(false)
                }
                else -> {
                    // Record the successful scan to our history
                    addRecentScan(item, location)
                    _uiState.update {
                        it.copy(
                            statusMessage = "Status: Success! DB Updated.",
                            isLoading = false,
                            isError = false
                        )
                    }
                    _feedback.tryEmit(true)
                }
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

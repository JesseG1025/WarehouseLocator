package com.example.warehouselocationscanner.ui.lookup

/**
 * Immutable UI state for the Item Lookup tab.
 * A sealed hierarchy makes every screen state explicit, so the Fragment can't end up
 * showing a spinner and an error at the same time.
 */
sealed interface ItemLookupUiState {
    /** Nothing scanned yet; show the prompt. */
    data object Idle : ItemLookupUiState

    /** GET request in flight for [itemCode]. */
    data class Loading(val itemCode: String) : ItemLookupUiState

    /** Server answered 2xx. [details] is already formatted for display. */
    data class Success(val itemCode: String, val details: String) : ItemLookupUiState

    /** Server answered 404 for [itemCode]. */
    data class NotFound(val itemCode: String) : ItemLookupUiState

    /** Any other failure. Exactly one of [httpCode] / [networkMessage] is set. */
    data class Error(
        val itemCode: String,
        val httpCode: Int? = null,
        val networkMessage: String? = null
    ) : ItemLookupUiState
}

package com.example.warehouselocationscanner.ui.lookup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouselocationscanner.network.RetrofitClient
import com.google.gson.JsonElement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the Item Lookup tab. Calls GET /api/wms/item_lookup/<item_code>
 * and exposes the result as [ItemLookupUiState].
 */
class ItemLookupViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ItemLookupUiState>(ItemLookupUiState.Idle)
    val uiState: StateFlow<ItemLookupUiState> = _uiState.asStateFlow()

    // One-shot success/failure events for beep + vibrate (doesn't replay on rotation).
    private val _feedback = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val feedback: SharedFlow<Boolean> = _feedback.asSharedFlow()

    // Shared Retrofit service; AuthInterceptor adds the X-API-Key header automatically.
    private val api = RetrofitClient.getApiService(application)

    // Tracks the in-flight lookup so a rapid second scan cancels the first request.
    // Prevents a slow, stale response from overwriting the newer item's result.
    private var lookupJob: Job? = null

    /**
     * Called by ItemLookupFragment for every DataWedge scan while this tab is visible.
     */
    fun lookup(itemCode: String) {
        val code = itemCode.trim()
        if (code.isEmpty()) return

        lookupJob?.cancel()
        _uiState.value = ItemLookupUiState.Loading(code)

        lookupJob = viewModelScope.launch {
            try {
                val response = api.lookupItem(code)
                _uiState.value = when {
                    response.isSuccessful -> {
                        // Format off the main thread; large replies won't stutter the UI.
                        val text = withContext(Dispatchers.Default) { format(response.body()) }
                        ItemLookupUiState.Success(code, text)
                    }
                    response.code() == 404 -> ItemLookupUiState.NotFound(code)
                    else -> ItemLookupUiState.Error(code, httpCode = response.code())
                }
            } catch (e: CancellationException) {
                // A newer scan replaced this one; rethrow so coroutine cancellation works.
                throw e
            } catch (e: Exception) {
                _uiState.value = ItemLookupUiState.Error(code, networkMessage = e.message ?: e.javaClass.simpleName)
            }
            _feedback.tryEmit(_uiState.value is ItemLookupUiState.Success)
        }
    }

    /**
     * Turns the server's JSON into readable "key: value" lines.
     * Done generically because the reply schema isn't fixed in the app: objects become
     * labelled lines, arrays (e.g. an item stored in several bins) become numbered blocks.
     */
    private fun format(element: JsonElement?, indent: String = ""): String {
        if (element == null || element.isJsonNull) return ""
        return when {
            element.isJsonObject -> element.asJsonObject.entrySet().joinToString("\n") { (key, value) ->
                val label = key.replace('_', ' ').replaceFirstChar { it.uppercase() }
                if (value.isJsonPrimitive || value.isJsonNull) {
                    "$indent$label: ${if (value.isJsonNull) "--" else value.asString}"
                } else {
                    "$indent$label:\n${format(value, "$indent  ")}"
                }
            }
            element.isJsonArray -> element.asJsonArray.mapIndexed { i, child ->
                if (child.isJsonPrimitive) "$indent• ${child.asString}"
                else "$indent#${i + 1}\n${format(child, "$indent  ")}"
            }.joinToString("\n")
            else -> "$indent${element.asString}"
        }
    }
}

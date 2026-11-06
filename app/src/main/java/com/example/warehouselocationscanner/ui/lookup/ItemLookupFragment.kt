package com.example.warehouselocationscanner.ui.lookup

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.warehouselocationscanner.R
import com.example.warehouselocationscanner.ui.ScanConsumer
import com.example.warehouselocationscanner.ui.ScanFeedback
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/**
 * Item Lookup tab. Any DataWedge scan received while this tab is visible is treated as an
 * item code and looked up on the server; it never touches the Scan tab's put-away flow.
 */
class ItemLookupFragment : Fragment(R.layout.fragment_item_lookup), ScanConsumer {

    private val viewModel: ItemLookupViewModel by viewModels()

    private lateinit var tvItem: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvResult: TextView
    private lateinit var cardResult: MaterialCardView
    private lateinit var progress: ProgressBar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvItem = view.findViewById(R.id.tvLookupItem)
        tvStatus = view.findViewById(R.id.tvLookupStatus)
        tvResult = view.findViewById(R.id.tvLookupResult)
        cardResult = view.findViewById(R.id.cardLookupResult)
        progress = view.findViewById(R.id.progressLookup)

        // viewLifecycleOwner ties collection to this Fragment's view, so nothing runs while
        // the tab is in the background or after its view is destroyed.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.feedback.collect { ok ->
                        val host = activity as? ScanFeedback
                        if (ok) host?.playSuccessFeedback() else host?.playErrorFeedback()
                    }
                }
            }
        }
    }

    /** Forwarded from MainActivity's DataWedge receiver. */
    override fun onScan(barcode: String) {
        viewModel.lookup(barcode)
    }

    /** Maps each state to exactly one visual configuration. */
    private fun render(state: ItemLookupUiState) {
        val code = when (state) {
            ItemLookupUiState.Idle -> null
            is ItemLookupUiState.Loading -> state.itemCode
            is ItemLookupUiState.Success -> state.itemCode
            is ItemLookupUiState.NotFound -> state.itemCode
            is ItemLookupUiState.Error -> state.itemCode
        }
        tvItem.text = code?.let { getString(R.string.item_scanned, it) } ?: "Item: --"

        progress.isVisible = state is ItemLookupUiState.Loading
        cardResult.isVisible = state is ItemLookupUiState.Success

        tvStatus.text = when (state) {
            ItemLookupUiState.Idle -> getString(R.string.lookup_prompt)
            is ItemLookupUiState.Loading -> getString(R.string.lookup_searching, state.itemCode)
            is ItemLookupUiState.Success -> getString(R.string.lookup_prompt)
            is ItemLookupUiState.NotFound -> getString(R.string.lookup_not_found, state.itemCode)
            is ItemLookupUiState.Error ->
                if (state.httpCode != null) getString(R.string.lookup_server_error, state.httpCode)
                else getString(R.string.lookup_network_error, state.networkMessage)
        }

        if (state is ItemLookupUiState.Success) {
            tvResult.text = state.details.ifBlank { getString(R.string.lookup_empty_result) }
        }
    }
}

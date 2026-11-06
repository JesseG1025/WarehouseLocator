package com.example.warehouselocationscanner.ui.scanner

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.warehouselocationscanner.R
import com.example.warehouselocationscanner.ui.ScanConsumer
import com.example.warehouselocationscanner.ui.ScanFeedback
import kotlinx.coroutines.launch

/**
 * Scan tab: the original two-step put-away screen, moved out of MainActivity so it can sit
 * beside other tabs. MainActivity still owns the DataWedge receiver and forwards scans here.
 */
class ScanFragment : Fragment(R.layout.fragment_scan), ScanConsumer {

    // 1. Initialize our ViewModel to survive screen rotations
    // activityViewModels() scopes it to MainActivity, so the scan pair and recent-scan
    // history survive switching tabs as well as rotation.
    private val viewModel: ScannerViewModel by activityViewModels()

    // UI Elements for text display
    private lateinit var tvItem: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvStatus: TextView

    // --- AUDIT LOGGING VARIABLES ---
    // Read from the hosting MainActivity (set via Intent extra by LoginActivity).
    private val activeUser: String by lazy {
        requireActivity().intent.getStringExtra("USER_SIGNATURE") ?: "Unknown_User"
    }
    private val deviceSignature: String by lazy {
        val model = android.os.Build.MODEL
        val androidId = android.provider.Settings.Secure.getString(
            requireContext().contentResolver, android.provider.Settings.Secure.ANDROID_ID
        )
        "$model-$androidId"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Connect code variables to the physical XML elements
        tvItem = view.findViewById(R.id.tvItem)
        tvLocation = view.findViewById(R.id.tvLocation)
        tvStatus = view.findViewById(R.id.tvStatus)

        /**
         * Allows workers to reset any scans. Can be used to clear accidental scans.
         */
        view.findViewById<Button>(R.id.btnClear).setOnClickListener {
            viewModel.resetScan()
        }

        /**
         * Navigate to the Recent Scans screen
         */
        view.findViewById<Button>(R.id.btnRecentScans).setOnClickListener {
            val intent = Intent(requireContext(), RecentScansActivity::class.java)
            intent.putStringArrayListExtra("RECENT_SCANS", ArrayList(viewModel.uiState.value.recentScans))
            startActivity(intent)
        }

        // 2. Observe the ViewModel's UI State reactively
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::updateUI) }
                // Beep/vibrate once per server response, via the shared host feedback.
                launch {
                    viewModel.feedback.collect { ok ->
                        val host = activity as? ScanFeedback
                        if (ok) host?.playSuccessFeedback() else host?.playErrorFeedback()
                    }
                }
            }
        }
    }

    /** Forwarded from MainActivity's DataWedge receiver while this tab is visible. */
    override fun onScan(barcode: String) {
        // State machine to alternate between item scan and location scan.
        // Barcodes look similar in format, so we rely purely on scan ORDER
        // (1st = item, 2nd = location) rather than trying to detect barcode type.

        // Pass the raw data directly to the ViewModel. It handles the logic and
        // will automatically emit a new state for the UI to observe.
        // When the pair is complete the ViewModel also fires the POST and resets the fields.
        viewModel.processScan(barcode, deviceSignature, activeUser)
    }

    private fun updateUI(state: ScannerUiState) {
        // Update text views based on the current state from the ViewModel
        tvItem.text = state.sourceBarcode?.let { getString(R.string.item_scanned, it) }
            ?: "Item: " // Fallback if null

        tvLocation.text = state.destinationBarcode?.let { getString(R.string.location_scanned, it) }
            ?: "Location: " // Leave blank until scanned

        tvStatus.text = state.statusMessage
    }
}

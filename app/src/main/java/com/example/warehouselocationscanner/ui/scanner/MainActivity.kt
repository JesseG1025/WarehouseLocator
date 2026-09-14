package com.example.warehouselocationscanner.ui.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import com.example.warehouselocationscanner.BuildConfig
import com.example.warehouselocationscanner.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

class MainActivity : AppCompatActivity() {

    // 1. Initialize our ViewModel to survive screen rotations
    private val viewModel: ScannerViewModel by viewModels()

    // Intent action DataWedge is configured (in the profile) to broadcast on every scan.
    // Must match the "Intent Action" field in your DataWedge profile's Intent Output settings exactly.
    private val actionString = "com.frontera.scanner.ACTION"

    // Legacy/simple extra key some DataWedge versions use to embed the decoded string directly.
    // Kept as a first-try lookup below, but on this device DataWedge instead sends a
    // content:// URI under "com.symbol.datawedge.decode_data" that we have to query separately.
    private val dataString = "com.symbol.datawedge.data_string"

    private val client = OkHttpClient()

    // Mutable variables driving our simple two-step scan state machine:
    // 1st scan populates itemScan, 2nd scan populates locationScan, then we submit and reset.
    // (NOTE: This logic is now safely encapsulated inside ScannerViewModel, but the
    // fundamental flow remains exactly as described above).

    // UI Elements for text display
    private lateinit var tvItem: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvStatus: TextView

    // Reference to our broadcast receiver instance
    private lateinit var zebraReceiver: ZebraScanReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Connect code variables to the physical XML elements
        tvItem = findViewById(R.id.tvItem)
        tvLocation = findViewById(R.id.tvLocation)
        tvStatus = findViewById(R.id.tvStatus)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize our receiver and pass actions/callbacks to handle UI updates.
        // handleScan() runs every time a barcode is successfully decoded from DataWedge.
        zebraReceiver = ZebraScanReceiver(
            actionCheck = actionString,
            dataKey = dataString,
            onScanReceived = { scannedValue ->
                handleScan(scannedValue)
            }
        )

        // 2. Observe the ViewModel's UI State reactively
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun handleScan(barcodeData: String) {
        // State machine to alternate between item scan and location scan.
        // Barcodes look similar in format, so we rely purely on scan ORDER
        // (1st = item, 2nd = location) rather than trying to detect barcode type.

        // Pass the raw data directly to the ViewModel. It handles the logic and
        // will automatically emit a new state for the UI to observe.
        viewModel.processScan(barcodeData)
    }

    private fun updateUI(state: ScannerUiState) {
        // Update text views based on the current state from the ViewModel
        tvItem.text = state.sourceBarcode?.let { getString(R.string.item_scanned, it) }
            ?: getString(R.string.status_waiting) // Fallback if null

        tvLocation.text = state.destinationBarcode?.let { getString(R.string.location_scanned, it) }
            ?: "" // Leave blank until scanned

        tvStatus.text = state.statusMessage

        // If both barcodes have been captured, fire the network request
        if (state.isReadyToMatch) {
            tvStatus.text = getString(R.string.status_sending)
            sendDataToServer(state.sourceBarcode!!, state.destinationBarcode!!)

            // v1 behavior: fail loudly, no retry queue.
            // Reset immediately after firing the request so the next scan pair can begin,
            // regardless of whether this request ultimately succeeds or fails.
            viewModel.resetScan()
        }
    }

    private fun sendDataToServer(item: String, location: String) {
        // 1. JSON String Payload
        val jsonPayload = """{"item": "$item", "location": "$location"}"""

        // 3. Define the media type
        val mediaType = "application/json; charset=utf-8".toMediaType()

        // 4. HTTP Request
        val body = jsonPayload.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("http://${BuildConfig.SERVER_IP}:5000/scanner/location_update")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Network Error: $e")
                runOnUiThread {
                    tvStatus.text = getString(R.string.status_network_error, e.message)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val code = response.code
                    runOnUiThread {
                        if (!response.isSuccessful) {
                            println("Server rejected request: $code")
                            tvStatus.text = getString(R.string.status_server_error, code)
                        } else {
                            println("Success! Code: $code")
                            tvStatus.text = getString(R.string.status_success)
                        }
                    }
                }
            }
        })
    }

    // When the app is active on the screen, register to receive DataWedge scan broadcasts.
    // RECEIVER_EXPORTED is required because DataWedge is a separate app broadcasting to us.
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(actionString)
        ContextCompat.registerReceiver(this, zebraReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    // Whenever the app is minimized or screen turns off, unregister so we don't leak the receiver.
    // NOTE: this means scans are only captured while this Activity is in the foreground.
    override fun onPause() {
        super.onPause()
        unregisterReceiver(zebraReceiver)
    }
}

// Dedicated BroadcastReceiver class to avoid manifest compilation errors.
class ZebraScanReceiver(
    private val actionCheck: String,
    private val dataKey: String,
    private val onScanReceived: (String) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != actionCheck) return

        // --- ATTEMPT 1: legacy/simple extra ---
        // Some DataWedge versions/profiles embed the decoded string directly in this extra.
        var barcodeData = intent.getStringExtra(dataKey)

        // --- ATTEMPT 2: content-provider URI (what THIS device actually sends) ---
        // Due to stricter Binder IPC transaction size limits on newer Android versions,
        // DataWedge no longer embeds the raw string in the broadcast. Instead it sends a
        // content:// URI under "com.symbol.datawedge.decode_data" that we must query
        // against DataWedge's own ContentResolver to retrieve the actual decoded value.
        if (barcodeData == null) {
            val decodeUriString = intent.getStringExtra("com.symbol.datawedge.decode_data")

            if (decodeUriString != null) {
                val decodeUri = decodeUriString.toUri() // using KTX extension

                // Requires a <queries> declaration for authority "com.symbol.datawedge.decode"
                // in AndroidManifest.xml, or this query silently fails to find the provider
                // (Android 11+ package visibility restriction).
                context.contentResolver.query(decodeUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        // Confirmed via Logcat on this device: the column is "data_string",
                        // not "data". Cursor also exposes id, label_type, decode_data,
                        // next_data_uri, full_data_size, data_buffer_size (pagination fields
                        // for oversized payloads - not needed for our short item/location codes).
                        val dataIndex = cursor.getColumnIndex("data_string")
                        if (dataIndex != -1) {
                            barcodeData = cursor.getString(dataIndex)
                        } else {
                            // Diagnostic aid: if the column name ever changes again on a
                            // future DataWedge update, log the real column names to re-fix it.
                            Log.d("DataWedge", "No 'data_string' column. Columns were: ${cursor.columnNames.joinToString()}")
                        }
                    }
                }
            }
        }

        // Safe unwrapping replacing the !! operations
        barcodeData?.takeIf { it.isNotBlank() }?.let { validData ->
            Toast.makeText(context, "Scanned: $validData", Toast.LENGTH_LONG).show()
            onScanReceived(validData)
        } ?: run {
            Toast.makeText(context, "Scanned data was null or blank", Toast.LENGTH_SHORT).show()
        }
    }
}
package com.example.warehouselocationscanner.ui.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.warehouselocationscanner.R
import com.example.warehouselocationscanner.ui.ScanConsumer
import com.example.warehouselocationscanner.ui.ScanFeedback
import com.example.warehouselocationscanner.ui.lookup.ItemLookupFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Tab host. Owns the single DataWedge receiver plus the shared beep/vibrate feedback,
 * and forwards each scan to whichever tab is currently visible.
 * The put-away UI and its POST logic now live in ScanFragment / ScannerViewModel.
 */
class MainActivity : AppCompatActivity(), ScanFeedback {

    // Intent action DataWedge is configured (in the profile) to broadcast on every scan.
    // Must match the "Intent Action" field in your DataWedge profile's Intent Output settings exactly.
    private val actionString = "com.frontera.scanner.ACTION"

    // Legacy/simple extra key some DataWedge versions use to embed the decoded string directly.
    // Kept as a first-try lookup below, but on this device DataWedge instead sends a
    // content:// URI under "com.symbol.datawedge.decode_data" that we have to query separately.
    private val dataString = "com.symbol.datawedge.data_string"

    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    // Mutable variables driving our simple two-step scan state machine:
    // 1st scan populates itemScan, 2nd scan populates locationScan, then we submit and reset.
    // (NOTE: This logic is now safely encapsulated inside ScannerViewModel, but the
    // fundamental flow remains exactly as described above).

    // Reference to our broadcast receiver instance
    private lateinit var zebraReceiver: ZebraScanReceiver

    // The tab Fragments. Both are kept alive and toggled with show/hide (instead of replace)
    // so switching tabs is instant and each tab keeps its state without re-inflating views.
    private lateinit var scanFragment: Fragment
    private lateinit var lookupFragment: Fragment
    private lateinit var activeFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupTabs(savedInstanceState)

        // Initialize our receiver and pass actions/callbacks to handle UI updates.
        // handleScan() runs every time a barcode is successfully decoded from DataWedge.
        zebraReceiver = ZebraScanReceiver(
            actionCheck = actionString,
            dataKey = dataString,
            onScanReceived = { scannedValue ->
                handleScan(scannedValue)
            }
        )
    }

    /**
     * Adds both tab Fragments once, or re-attaches the existing ones after a configuration
     * change (the FragmentManager restores them itself, so we look them up by tag).
     */
    private fun setupTabs(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            scanFragment = ScanFragment()
            lookupFragment = ItemLookupFragment()
            supportFragmentManager.commit {
                add(R.id.fragmentContainer, lookupFragment, TAG_LOOKUP)
                hide(lookupFragment)
                add(R.id.fragmentContainer, scanFragment, TAG_SCAN)
            }
            activeFragment = scanFragment
        } else {
            scanFragment = supportFragmentManager.findFragmentByTag(TAG_SCAN)!!
            lookupFragment = supportFragmentManager.findFragmentByTag(TAG_LOOKUP)!!
            activeFragment = if (lookupFragment.isHidden) scanFragment else lookupFragment
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener { item ->
            val target = when (item.itemId) {
                R.id.nav_lookup -> lookupFragment
                else -> scanFragment
            }
            if (target !== activeFragment) {
                // setReorderingAllowed lets the FragmentManager batch the hide/show efficiently.
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    hide(activeFragment)
                    show(target)
                }
                activeFragment = target
            }
            true
        }
    }

    private fun handleScan(barcodeData: String) {
        // Route the scan to the visible tab only: on Scan it advances the item -> location
        // pair, on Lookup it triggers an item lookup.
        (activeFragment as? ScanConsumer)?.onScan(barcodeData)
    }

    override fun playSuccessFeedback() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(100)
        }
    }

    override fun playErrorFeedback() {
        toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 300)
        
        val timings = longArrayOf(0, 200, 100, 200) // Double-pulse: delay, vibrate, sleep, vibrate
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(timings, -1)
        }
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

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        toneGenerator = null
    }

    // Fragment tags used to find the existing tabs again after rotation/recreation.
    private companion object {
        const val TAG_SCAN = "tab_scan"
        const val TAG_LOOKUP = "tab_lookup"
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
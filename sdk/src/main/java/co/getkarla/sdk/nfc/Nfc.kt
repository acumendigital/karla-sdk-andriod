package co.getkarla.sdk.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.util.Log
import co.getkarla.sdk.EventBus
import co.getkarla.sdk.Events
import co.getkarla.sdk.nfc.parser.NdefMessageParser

class Nfc : Activity() {

    private var TAG = "NFC ACTIVITY"

    private var mNfcAdapter: NfcAdapter? = null
    private var mPendingIntent: PendingIntent? = null

    private var result: String = ""

    override fun onStart() {
        super.onStart()
        EventBus.register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.unregister(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (checkNFCEnable()) {
//            mPendingIntent =
//                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            mPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

            if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMessages ->
                    val messages: List<NdefMessage> = rawMessages.map { it as NdefMessage }
                    // Process the messages array.
                    parserNDEFMessage(messages)
                }
            }
        } else {
            // TODO: implement no nfc
            Log.d(TAG, "nfc not enabled")
        }
    }

    override fun onResume() {
        super.onResume()
        mNfcAdapter?.enableForegroundDispatch(this, mPendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        mNfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMessages ->
                val messages: List<NdefMessage> = rawMessages.map { it as NdefMessage }
                // Process the messages array.
                parserNDEFMessage(messages)
            }
        }
    }

    private fun parserNDEFMessage(messages: List<NdefMessage>) {
        val builder = StringBuilder()
        val records = NdefMessageParser.parse(messages[0])
        val size = records.size

        for (i in 0 until size) {
            val record = records[i]
            val str = record.str()
            print(str)
            builder.append(str).append("\n")
        }

        this.result = builder.toString()
        val result = Events.NfcReadResult(builder.toString())
        EventBus.post(result)
        finish()
    }

    private fun checkNFCEnable(): Boolean {
        return if (mNfcAdapter == null) {
            // TODO: implement no nfc
            Log.d(TAG, "nfc not enabled")
            false
        } else {
            mNfcAdapter?.isEnabled == true
        }
    }
}
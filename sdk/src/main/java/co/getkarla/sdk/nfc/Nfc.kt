package co.getkarla.sdk.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.os.Build
import android.os.Bundle
import android.util.Log
import co.getkarla.sdk.EventBus
import co.getkarla.sdk.Events
import co.getkarla.sdk.nfc.parser.NdefMessageParser

class Nfc : Activity(), NfcAdapter.ReaderCallback {

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

        EventBus.register(this)
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
        if (mNfcAdapter != null) {
            val options = Bundle();
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
            mNfcAdapter?.enableReaderMode(this, this,NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_BARCODE or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,options)
        }
    }

    override fun onPause() {
        super.onPause()
        mNfcAdapter?.disableReaderMode(this)
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

    override fun onTagDiscovered(tag: Tag?) {
        val mNdef = Ndef.get(tag)

        var finalResult = ""

        if (mNdef != null) {
            val mNdefMessage = mNdef.cachedNdefMessage

            if (mNdefMessage != null) {
                for (record in mNdefMessage.records) {
                    val payload = record.payload
                    val data = String(payload, Charsets.UTF_8)
                    val startIndex = data.indexOf("{", data.indexOf("stxen"))
                    val result = data.substring(startIndex)
                    finalResult = result

                }

                runOnUiThread {
                    val result = Events.NfcReadResult(finalResult)
                    EventBus.post(result)
                }

                finish()
            }

        }
    }
}
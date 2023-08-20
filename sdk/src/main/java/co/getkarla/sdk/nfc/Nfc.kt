package co.getkarla.sdk.nfc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import co.getkarla.sdk.nfc.parser.NdefMessageParser

class Nfc(context: Context) : AppCompatActivity() {

    private var mNfcAdapter: NfcAdapter? = null
    private var mPendingIntent: PendingIntent? = null

    init {
        Log.d("NFC ACTIVITY","i am here")
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context)
        if (checkNFCEnable()) {
            mPendingIntent = PendingIntent.getActivity(context, 0, Intent(context, context.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        } else {
            // TODO: handle no nfc
            Log.d("NFC ACTIVITY", "no nfc")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("NFC ACTIVITY", "resuming")
        mNfcAdapter?.enableForegroundDispatch(this, mPendingIntent, null, null)
    }


    override fun onPause() {
        super.onPause()
        Log.d("NFC ACTIVITY", "pausing")
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
            val record = records.get(i)
            val str = record.str()
            builder.append(str).append("\n")
        }
//        mTvView.text = builder.toString()
        print(builder.toString())
    }

    private fun checkNFCEnable(): Boolean {
        return if (mNfcAdapter == null) {
            // TODO: handle no nfc
            Log.d("NFC ACTIVITY", "no nfc")
            false
        } else {
            mNfcAdapter!!.isEnabled
        }
    }
}
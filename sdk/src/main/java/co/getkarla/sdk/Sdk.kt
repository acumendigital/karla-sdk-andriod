package co.getkarla.sdk

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import co.getkarla.sdk.cardEmulation.KHostApduService
import co.getkarla.sdk.nfc.Nfc
import com.squareup.otto.Subscribe

val EventBus = Bus().getBus()

class Sdk(apiKey: String, onTransactionInitiated: () -> Unit, onTransactionCompleted: (data: String) -> Unit ) {
    private lateinit var mNfc: Nfc

    // in this version, our contactless sdk will power transactions Phone2Phone, Phone2POS
    // the flow is this -
    /*
    * Initiate Transaction - Merchant/Pos
    * Complete Transaction - User App
    * Initiate Transaction basically starts a transaction from a merchants pos/app
    * it starts a HCE session probably carrying a transaction identifier optionally encrypted
    *
    * Complete Transaction picks up that identifier, sends the identifier back to the integrating app
    * and completes the transaction (can be initiating a bank transfer or something)
    * */

    val onTransactionInitiated: () -> Unit
    val onTransactionCompleted: (data: String) -> Unit
    private val apiKey: String
    init {
        this.onTransactionInitiated = onTransactionInitiated
        this.onTransactionCompleted = onTransactionCompleted
        this.apiKey = apiKey
        EventBus.register(this)
    }

    // Initiate Transaction
    fun startTransaction(context: Context, data: MutableMap<String, String>) {
        val intent = Intent(context, KHostApduService::class.java)
        Log.d("debug", data.entries.toString())
        intent.putExtra("ndefMessage", data.entries.toString())
        context.startService(intent)
        this.onTransactionInitiated()
    }

    @Subscribe
    fun onComplete(event: Events.NfcReadResult) {
        this.onTransactionCompleted(event.getResult())
    }

    fun completeTransaction() {
        try {
            this.mNfc = Nfc()
        } catch (e: Exception) {
            throw(e)
        }
    }
}
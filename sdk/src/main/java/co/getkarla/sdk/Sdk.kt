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

class Sdk {

    // in this version our contactles sdk will power transactions bank to bank (same bank)
    // the flow is this -
    /*
    * Initiate Transaction - Merchant Pos
    * Complete Transaction - User App
    * Initiate Transaction basically starts a transaction from a merchants pos/app
    * it starts a HCE session probably carrying a transaction identifier optionally encrypted
    *
    * Complete Transaction picks up that identifier, sends the identifier back to the integrating app
    * and completes the transaction (can be initiating a bank transfer or something)
    * */
    val onTransactionInitiated: () -> Unit
    val onTransactionCompleted: (data: MutableMap<String, String>) -> Unit
    val apiKey: String
    constructor(apiKey: String, onTransactionInitiated: () -> Unit, onTransactionCompleted: (data: MutableMap<String, String>) -> Unit ) {
        this.onTransactionInitiated = onTransactionInitiated
        this.onTransactionCompleted = onTransactionCompleted
        this.apiKey = apiKey
    }

    // Initiate Transaction
    fun startTransaction(context: Context, data: MutableMap<String, String>) {
        val intent = Intent(context, KHostApduService::class.java)
        Log.d("debug", data.entries.toString())
        intent.putExtra("ndefMessage", data.entries.toString())
        context.startService(intent)
        this.onTransactionInitiated()
    }

    fun completeTransaction() {
        Log.d("NFC ACTIVITY", "starting nfc activity")
        try {
            Nfc()
        } catch (e: Exception) {
            throw(e)
        }

    }
}
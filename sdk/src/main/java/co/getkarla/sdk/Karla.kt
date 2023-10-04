package co.getkarla.sdk

import android.app.Activity
import android.content.Intent
import android.util.Log
import co.getkarla.sdk.cardEmulation.KHostApduService
import co.getkarla.sdk.nfc.Card
import co.getkarla.sdk.nfc.Nfc
import com.squareup.otto.Subscribe
import org.json.JSONArray
import org.json.JSONObject

class Karla(apiKey: String, onTransactionInitiated: (data: Map<String, *>) -> Unit, onTransactionCompleted: (data: Map<String, *>) -> Unit, onReadEmvCard: (data: Map<String, *>) -> Unit ) {
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

    val onTransactionInitiated: (data: Map<String, *>) -> Unit
    val onTransactionCompleted: (data: Map<String, *>) -> Unit
    val onReadEmvCard: (data: Map<String, *>) -> Unit
    private val apiKey: String
    init {
        this.onTransactionInitiated = onTransactionInitiated
        this.onTransactionCompleted = onTransactionCompleted
        this.onReadEmvCard = onReadEmvCard
        this.apiKey = apiKey
        EventBus.register(this)
    }

    fun startTransaction(context: Activity, id: String, amount: Double, reference: String, extra: Map<String, *>?) {
        val intent = Intent(context, KHostApduService::class.java)
        val data = mapOf("id" to id, "amount" to amount, "reference" to reference).plus(extra as Map<String, *>)
        if (extra.isNotEmpty()) {
            data + extra
        }
        val jsonData = JSONObject(data).toString()
        Log.d("HCE ACTIVITY", jsonData)
        intent.putExtra("ndefMessage", jsonData)
        context.startService(intent)
        this.onTransactionInitiated(data)
    }

    @Subscribe
    fun onComplete(event: Events.NfcReadResult) {
        val result = JSONObject(event.getResult())
        this.onTransactionCompleted(result.toMap())
    }

    @Subscribe
    fun onComplete(event: Events.EmvReadResult) {
        val result = JSONObject(event.getResult())
        this.onReadEmvCard(result.toMap())
    }

    fun completeTransaction() {
        try {
            this.mNfc = Nfc()
        } catch (e: Exception) {
            throw(e)
        }
    }

    fun readEmvCard(context: Activity, amount: Double, authorizeTransaction: () -> Boolean) {
        try {
            val intent = Intent(context, Card::class.java)
            context.startService(intent)
            Log.d("Details", amount.toString())
            authorizeTransaction()
            // log that user started a transaction
        } catch (e: Exception) {
            throw(e)
        }
    }

    fun JSONObject.toMap(): Map<String, *> = keys().asSequence().associateWith { it ->
        when (val value = this[it])
        {
            is JSONArray ->
            {
                val map = (0 until value.length()).associate { Pair(it.toString(), value[it]) }
                JSONObject(map).toMap().values.toList()
            }
            is JSONObject -> value.toMap()
            JSONObject.NULL -> null
            else            -> value
        }
    }
}
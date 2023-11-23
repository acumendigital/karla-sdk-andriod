package co.getkarla.sdk

import android.app.Activity
import android.content.Intent
import android.util.Log
import co.getkarla.sdk.cardEmulation.KHostApduService
import co.getkarla.sdk.nfc.Card
import co.getkarla.sdk.nfc.Nfc
import com.google.gson.Gson
import com.squareup.otto.Subscribe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class Karla(apiKey: String, onTransactionInitiated: (data: Map<String, *>) -> Unit, onTransactionCompleted: (data: Map<String, *>) -> Unit, onReadEmvCard: (data: Map<String, *>) -> Unit, onCompleteEmvTransaction: (data: Map<String, Any>) -> Boolean ) {
    private lateinit var mCard: Card
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
    private lateinit var cardResult: MutableMap<String, Any>
    private var completeEmvTransaction: (data: Map<String, Any>) -> Boolean
    private var amount: Double = 0.0

    data class Transaction(
        val _id: String,
        val amount: Double,
        val success: Boolean,
        val reason: String?
    )

    data class Response(
        val error: Boolean,
        val data: Transaction,
        val msg: String?
    )

    init {
        this.onTransactionInitiated = onTransactionInitiated
        this.onTransactionCompleted = onTransactionCompleted
        this.onReadEmvCard = onReadEmvCard
        this.apiKey = apiKey
        this.completeEmvTransaction = onCompleteEmvTransaction
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
        val resultMap = result.toMap() as MutableMap<String, Any>

        if (resultMap["error"] == false)  {
            cardResult = resultMap["data"] as MutableMap<String, Any>
        }

        this.onReadEmvCard(mapOf("error" to resultMap["error"], "msg" to resultMap["msg"]))
    }

    fun completeTransaction(context: Activity) {
        try {
            val intent = Intent(context, Nfc::class.java)
            context.startActivity(intent)
        } catch (e: Exception) {
            throw(e)
        }
    }

    fun readEmvCard(context: Activity, amount: Double) {
        try {
            val intent = Intent(context, Card::class.java)
            context.startActivity(intent)
            this.amount = amount
        } catch (e: Exception) {
            throw(e)
        }
    }

    fun completeEmvTransaction(pin: String) {
        cardResult["pin"] = pin
        cardResult["amount"] = this.amount

        val context = this
        runBlocking {
            val scope = CoroutineScope(Dispatchers.IO)
            val result = scope.async{
                context.recordTransaction(context.amount)
            }

            val txn = result.await()
            if (txn != null) {
                cardResult["transaction_id"] = txn._id
            }
        }

        this.completeEmvTransaction(cardResult)
    }

    private fun recordTransaction(amount: Double): Transaction? {

        val url = "https://karla-dev.fly.dev/api/v1/partner/transaction"
        val requestData = JSONObject(mapOf("amount" to amount)).toString().trimIndent()
        val apiKey = this.apiKey
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestData.toRequestBody(mediaType)

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val resp = gson.fromJson(responseBody, Response::class.java)
                if (!resp.error) {
                    return resp.data
                }
            } else {
                println("Request failed with code: ${response.code}")
            }
        } catch (e: Exception) {
            println("ERROR: ${e.message}")
        }

        return null
    }

    fun updateTransactionStatus(id: String, status: Boolean, reason: String?): Boolean {
        val url = "https://karla-dev.fly.dev/api/v1/partner/transaction/$id"
        val requestData = JSONObject(mapOf("success" to status, "reason" to reason )).toString()
        val apiKey = this.apiKey

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestData.toRequestBody(mediaType)

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val gson = Gson()
                val resp = gson.fromJson(responseBody, Response::class.java)
                return !resp.error
            } else {
                println("Request failed with code: ${response.code}")
            }
        } catch (e: Exception) {
            println("ERROR: ${e.message}")
        }

        return false
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
package co.getkarla.sdk.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import co.getkarla.sdk.EventBus
import co.getkarla.sdk.Events
import co.getkarla.sdk.creditCardNfcReader.utils.CardNfcUtils
import org.json.JSONObject

class Card: Activity(), MyCardNfcAsyncTask.MyCardNfcInterface {

    private var TAG = "CARD ACTIVITY"

    private var mNfcAdapter: NfcAdapter? = null
    private var mCardNfcUtils: CardNfcUtils? = null
    private var mCardNfcAsyncTask: MyCardNfcAsyncTask? = null
    private var mIntentFromCreate: Boolean = false

    private lateinit var mCardType: String

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
        Log.d(TAG, "created")
        if (checkNFCEnable()) {
            mCardNfcUtils = CardNfcUtils(this)
            mIntentFromCreate = true
            readNFCInfo()
        } else {
            // TODO: return no nfc
            Log.d(TAG, "nfc not enabled")
        }
    }

    override fun onResume() {
        super.onResume()
        mIntentFromCreate = false
        if (mNfcAdapter != null) {
            mCardNfcUtils?.enableDispatch()
        }

//        mNfcAdapter?.enableForegroundDispatch(this, mPendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        if (mNfcAdapter != null) {
            mCardNfcUtils?.disableDispatch()
        }

        mNfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("CARD", "created too")
        // readNFCTextInfo(intent)
        if (mNfcAdapter != null && checkNFCEnable()) {
            mCardNfcAsyncTask = MyCardNfcAsyncTask.Builder(this, intent, mIntentFromCreate).build()
        }
    }

    override fun cardIsReadyToRead() {
        val cardNumber = mCardNfcAsyncTask?.cardNumber
        val expirationDate = mCardNfcAsyncTask?.cardExpireDate
        val holderName =
            mCardNfcAsyncTask?.cardHolderFirstName + " " + mCardNfcAsyncTask?.cardHolderLastName
        val cardType = parseCardType(mCardNfcAsyncTask?.cardType)
        val resultMap = mapOf("error" to false, "data" to mapOf("pan" to cardNumber, "exp" to expirationDate, "type" to cardType, "holder_name" to holderName))
        val result = Events.EmvReadResult(JSONObject(resultMap).toString())
        EventBus.post(result)
        finish()
    }

    override fun finishNfcReadCard() {
        mCardNfcAsyncTask = null
    }

    override fun cardWithLockedNfc() {
        val resultMap = mapOf("error" to true, "msg" to "your card has locked NFC")
        val result = Events.EmvReadResult(JSONObject(resultMap).toString())
        EventBus.post(result)
        finish()
    }

    override fun doNotMoveCardSoFast() {
        val resultMap = mapOf("error" to true, "msg" to "do not move card so fast")
        val result = Events.EmvReadResult(JSONObject(resultMap).toString())
        EventBus.post(result)
        finish()
    }

    override fun unknownEmvCard() {
        val resultMap = mapOf("error" to true, "msg" to "unknown emv card")
        val result = Events.EmvReadResult(JSONObject(resultMap).toString())
        EventBus.post(result)
        finish()
    }

    private fun readNFCInfo() {
        onNewIntent(intent)
    }

    private fun checkNFCEnable(): Boolean {
        return if (mNfcAdapter == null) {
            false
        } else {
            mNfcAdapter?.isEnabled == true
        }
    }

    private fun parseCardType(cardType: String?): String {
        when (cardType) {
            MyCardNfcAsyncTask.CARD_UNKNOWN -> return "UNKNOWN"
            MyCardNfcAsyncTask.CARD_VISA -> return "VISA"
            MyCardNfcAsyncTask.CARD_MASTER_CARD -> return "Mastercard"
            MyCardNfcAsyncTask.CARD_VERVE -> return "Verve"
        }

        return ""
    }

    override fun startNfcReadCard() {
        // return something
    }

}
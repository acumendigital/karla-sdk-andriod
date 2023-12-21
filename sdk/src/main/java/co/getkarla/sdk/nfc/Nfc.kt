package co.getkarla.sdk.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import co.getkarla.sdk.EventBus
import co.getkarla.sdk.Events
import co.getkarla.sdk.nfc.parser.NdefMessageParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.Array
import java.math.BigInteger
import java.util.Arrays
import java.util.concurrent.TimeUnit

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
        mNfcAdapter?.disableForegroundDispatch(this)
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

    private fun selectApdu(aid: ByteArray): ByteArray? {
        val commandApdu = ByteArray(6 + aid.size)
        commandApdu[0] = 0x00.toByte() // CLA
        commandApdu[1] = 0xA4.toByte() // INS
        commandApdu[2] = 0x04.toByte() // P1
        commandApdu[3] = 0x00.toByte() // P2
        commandApdu[4] = (aid.size and 0x0FF).toByte() // Lc
        System.arraycopy(aid, 0, commandApdu, 5, aid.size)
        commandApdu[commandApdu.size - 1] = 0x00.toByte() // Le
        return commandApdu
    }

//    fun hexStringToByteArray(s: String): ByteArray {
//        val len = s.length
//        val data = ByteArray(len / 2)
//        var i = 0
//        while (i < len) {
//            data[i / 2] = ((((s[i].digitToIntOrNull(16) ?: (-1 shl 4)) + s[i + 1].digitToIntOrNull(
//                16
//            )!!) ?: -1)).toByte()
//            i += 2
//        }
//        return data
//    }

    fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)

        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
        }

        return data
    }

    fun bytesToHex(bytes: ByteArray): String? {
        val result = StringBuffer()
        for (b in bytes) result.append(
            Integer.toString((b.toInt() and 0xff) + 0x100, 16).substring(1)
        )
        return result.toString()
    }

    private val SW_9000 = byteArrayOf(0x90.toByte(), 0x00.toByte())

    fun isSucceed(pByte: ByteArray): Boolean {
        if (pByte.size < 2) {
            return false
        }
        val resultValue = pByte.copyOfRange(pByte.size - 2, pByte.size)
        return resultValue.contentEquals(SW_9000)
    }


    fun convertIntToByteArray(value: Int, numberOfBytes: Int): ByteArray {
        val b = ByteArray(numberOfBytes)
        var i: Int
        var shift: Int
        i = 0
        shift = (b.size - 1) * 8
        while (i < b.size) {
            b[i] = (0xFF and (value shr shift)).toByte()
            i++
            shift -= 8
        }
        return b
    }

    fun parseTextrecordPayload(ndefPayload: ByteArray): ByteArray {
        val languageCodeLength = Array.getByte(ndefPayload, 0).toInt()
        val ndefPayloadLength = ndefPayload.size
        val languageCode = ByteArray(languageCodeLength)
        System.arraycopy(ndefPayload, 1, languageCode, 0, languageCodeLength)
        val message = ByteArray(ndefPayloadLength - 1 - languageCodeLength)
        System.arraycopy(
            ndefPayload,
            1 + languageCodeLength,
            message,
            0,
            ndefPayloadLength - 1 - languageCodeLength
        )
        return message
    }

    private fun doVibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (this.getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(
                VibrationEffect.createOneShot(150, 10)
            )
        } else {
            val v = this.getSystemService(VIBRATOR_SERVICE) as Vibrator
            v.vibrate(200)
        }
    }

    override fun onTagDiscovered(tag: Tag) {

        Log.d("DISCOVERED", tag.toString())

        val mIsoDep = IsoDep.get(tag)

        if (mIsoDep != null) {
            mIsoDep.connect()

            // now we run the select command with AID
            val nfcHceNdefAid = "D2760000850101"
            val aid: ByteArray = hexStringToByteArray(nfcHceNdefAid)

            println(aid)

            var command = selectApdu(aid)
            val responseSelect: ByteArray = mIsoDep.transceive(command)

            println("responseSelect: " + bytesToHex(responseSelect))

            if (!isSucceed(responseSelect)) {
                println("responseSelect is not 90 00 - aborted ")
                return
            }

            // sending cc select = get the capability container
            val selectCapabilityContainer = "00a4000c02e103"
            command = hexStringToByteArray(selectCapabilityContainer)
            val responseSelectCc: ByteArray = mIsoDep.transceive(command)

            println("responseSelectCc: " + bytesToHex(responseSelectCc))

            if (!isSucceed(responseSelectCc)) {
                println("responseSelectCc is not 90 00 - aborted ")
                return
            }

            // Sending ReadBinary from CC...
            val sendBinareFromCc = "00b000000f"
            command = hexStringToByteArray(sendBinareFromCc)
            val responseSendBinaryFromCc: ByteArray = mIsoDep.transceive(command)

            println(
                "sendBinaryFromCc response: " + bytesToHex(
                    responseSendBinaryFromCc
                )
            )

            if (!isSucceed(responseSendBinaryFromCc)) {
                println("responseSendBinaryFromCc is not 90 00 - aborted ")
                return
            }

            // Capability Container header:

            // Capability Container header:
            val capabilityContainerHeader =
                responseSendBinaryFromCc.copyOfRange(0, responseSendBinaryFromCc.size - 2)
            println(
                "capabilityContainerHeader: " + bytesToHex(
                    capabilityContainerHeader
                )
            )
            println("capabilityContainerHeader: $capabilityContainerHeader")

            // Sending NDEF Select...

            // Sending NDEF Select...
            val sendNdefSelect = "00a4000c02e104"
            command = hexStringToByteArray(sendNdefSelect)
            val responseSendNdefSelect: ByteArray = mIsoDep.transceive(command)

            println("sendNdefSelect response: " + bytesToHex(responseSendNdefSelect))

            if (!isSucceed(responseSendNdefSelect)) {
                println("responseSendNdefSelect is not 90 00 - aborted ")
                return
            }

            // Sending ReadBinary NLEN...
            val sendReadBinaryNlen = "00b0000002"
            command = hexStringToByteArray(sendReadBinaryNlen)
            val responseSendBinaryNlen: ByteArray = mIsoDep.transceive(command)
            println("sendBinaryNlen response: " + bytesToHex(responseSendBinaryNlen))

            if (!isSucceed(responseSendBinaryNlen)) {
                println("responseSendBinaryNlen is not 90 00 - aborted ")
                return
            }

            // Sending ReadBinary, get NDEF data...
            val ndefLen: ByteArray = Arrays.copyOfRange(responseSendBinaryNlen, 0, 2)
            val cmdLen: ByteArray = hexStringToByteArray(sendReadBinaryNlen)
            val ndefLenInt = BigInteger(ndefLen).toInt()
            val ndefLenIntRequest = ndefLenInt + 2
            //byte[] cmdLenNew = BigInteger.valueOf(ndefLenIntRequest).toByteArray();
            val cmdLenNew: ByteArray = convertIntToByteArray(ndefLenIntRequest, 2)

            val sendReadBinaryNdefData = "00b000" + bytesToHex(cmdLenNew)
            //String sendReadBinaryNdefData = "00b0000092";
            command = hexStringToByteArray(sendReadBinaryNdefData)
            val responseSendBinaryNdefData: ByteArray = mIsoDep.transceive(command)

            println(
                "sendBinaryNdefData response: " + bytesToHex(
                    responseSendBinaryNdefData
                )
            )
            println("sendBinaryNdefData response: $responseSendBinaryNdefData")

            if (!isSucceed (responseSendBinaryNdefData)) {
                println("responseSendBinaryNdefData is not 90 00 - aborted ")
                return
            }

            val ndefMessage = Arrays.copyOfRange(
                responseSendBinaryNdefData,
                0,
                responseSendBinaryNdefData.size - 2
            )

            println("ndefMessage: $ndefMessage")

            // strip off the first 2 bytes
            val ndefMessageStrip = Arrays.copyOfRange(ndefMessage, 9, ndefMessage.size)

            //String ndefMessageParsed = Utils.parseTextrecordPayload(ndefMessageStrip);
            val ndefMessageParsed = ndefMessageStrip
            println("ndefMessage parsed: $ndefMessageParsed")

            // try to get a NdefMessage from the byte array
            val ndefMessageByteArray = Arrays.copyOfRange(ndefMessage, 2, ndefMessage.size)
            try {
                val ndefMessageFromTag = NdefMessage(ndefMessageByteArray)
                val ndefRecords = ndefMessageFromTag.records
                var ndefRecord: NdefRecord
                val ndefRecordsCount = ndefRecords.size
                if (ndefRecordsCount > 0) {
                    var isoDepResult = ""
                    for (i in 0 until ndefRecordsCount) {
                        val ndefTnf = ndefRecords[i].tnf
                        val ndefType = ndefRecords[i].type
                        val ndefPayload = ndefRecords[i].payload
                        // here we are trying to parse the content
                        // Well known type - Text
                        if (ndefTnf == NdefRecord.TNF_WELL_KNOWN &&
                            Arrays.equals(ndefType, NdefRecord.RTD_TEXT)
                        ) {

                            val ndefRec = parseTextrecordPayload(ndefPayload)
                            isoDepResult = String(ndefRec, Charsets.UTF_8)

                            println("NDEF PAYLOAD ${String(ndefRec, Charsets.UTF_8)}")
                        }
                    }
                    doVibrate()
                    runOnUiThread {
                        val result = Events.NfcReadResult(isoDepResult)
                        EventBus.post(result)
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        delay(TimeUnit.SECONDS.toMillis(3))
                        withContext(Dispatchers.Main) {
                            Log.i("TAG", "this will be called after 3 seconds")
                            finish()
                        }
                    }

                }
            } catch (e: FormatException) {
                e.printStackTrace();
            }

            return
        }

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

                doVibrate()

                runOnUiThread {
                    val result = Events.NfcReadResult(finalResult)
                    EventBus.post(result)
                }

                CoroutineScope(Dispatchers.IO).launch {
                    delay(TimeUnit.SECONDS.toMillis(3))
                    withContext(Dispatchers.Main) {
                        Log.i("TAG", "this will be called after 3 seconds")
                        finish()
                    }
                }

                return
            }

        }
    }

}
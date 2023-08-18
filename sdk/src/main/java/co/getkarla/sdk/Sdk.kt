package co.getkarla.sdk

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import co.getkarla.sdk.cardEmulation.KHostApduService

class Sdk {
    fun sayHello(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun testHce(context: Context, msg: String) {
        val intent = Intent(context, KHostApduService::class.java)
        intent.putExtra("ndefMessage", msg)
        context.startService(intent)
    }
}
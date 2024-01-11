package co.getkarla.contactless

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import co.getkarla.contactless.ui.theme.KarlaTheme
import co.getkarla.sdk.Karla
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val karla = Karla("KARLA_UcPVCOMuOmeoEqHBGHuSHjKapw",::onTransactionInitiated, ::onTransactionCompleted, ::onReadEmvCard, ::onCompleteEmvTransaction)
    fun onTransactionCompleted(data: String) {
        Log.i("FINAL RESULT", data)
        // do whatever you want to do with the data received
    }
    fun onTransactionInitiated(data: Map<String, *>) {}

    fun onReadEmvCard(data: Map<String, *>) {
        // will probably tell you if there's an error from here
        if (data["error"] == false) {
            karla.completeEmvTransaction("1919")
        } else {
            // handle error
            Log.d("EMV ERROR", data["msg"] as String)
        }
    }

    fun onCompleteEmvTransaction(data: Map<String, Any>): Boolean {
        // call your endpoint here
        Log.i("FINAL RESULT", data.toString())
        runBlocking {
            val scope = CoroutineScope(Dispatchers.IO)
            val result = scope.async{
                karla.updateTransactionStatus(data["transaction_id"].toString(), true, "success")
            }
            val txn = result.await()
            Log.d("STATUS RESULT", txn.toString())
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        karla.completeTransaction(this)

//        karla.readEmvCard(this, 45000.00)
        setContent {
            KarlaTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")

                }
            }
        }
//        karla.startTransaction(this, "656558e524ad4dfc363d4b9b", 4000.00,"", mapOf("merchantName" to "Elvis Merchant", "_id" to "656558e524ad4dfc363d4b9b"))
        karla.startApplePayTransaction(this, "656558e524ad4dfc363d4b9b", "elvis@acumen.digital", "")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KarlaTheme {
        Greeting("Android")
    }
}
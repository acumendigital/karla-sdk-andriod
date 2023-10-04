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

class MainActivity : ComponentActivity() {

    fun onTransactionCompleted(data: Map<String, *>) {
        Log.i("FINAL RESULT", data.toString())
        // do whatever you want to do with the data received
    }
    fun onTransactionInitiated(data: Map<String, *>) {}

    fun onReadEmvCard(data: Map<String, *>) {
        Log.i("FINAL RESULT", data.toString())
        // do whatever you want to do with the data received
    }

    fun authorizeTransaction(): Boolean {
        return false
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val karla = Karla()
        karla.init("", ::onTransactionInitiated, ::onTransactionCompleted, ::onReadEmvCard)
//        sdk.completeTransaction()
//        sdk.startTransaction(this, "", 4000.00,"", mapOf("merchantName" to "Elvis Chuks"))
        karla.readEmvCard(this, 40000.00, ::authorizeTransaction)
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
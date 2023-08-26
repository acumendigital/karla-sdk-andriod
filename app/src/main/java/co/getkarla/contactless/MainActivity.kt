package co.getkarla.contactless

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import co.getkarla.contactless.ui.theme.KarlaTheme
import co.getkarla.sdk.Sdk

class MainActivity : ComponentActivity() {

    fun onTransactionCompleted(data: String) {
        Log.i("FINAL RESULT",data)
    }
    fun onTransactionInitiated() {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sdk = Sdk("", ::onTransactionInitiated, ::onTransactionCompleted)
//        sdk.completeTransaction()
        sdk.startTransaction(this, "{\"_id\":\"62ff8a9d062c226eceed5dcb\",\"merchantName\":\"Elvis Chuku\"}")
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
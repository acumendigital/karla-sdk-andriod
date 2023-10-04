package co.getkarla.sdk.nfc.extensions

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun longShowSnackBar(view: View, message: String) {
    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
}
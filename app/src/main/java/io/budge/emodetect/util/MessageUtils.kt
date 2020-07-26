package io.budge.emodetect.util

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

class MessageUtils {
    fun showToast(message: String, context: Context) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun showSnackBar(message: String, view: View, isShort: Boolean = false){
        val snackbar = Snackbar.make(view, message,
            if (isShort) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_INDEFINITE )
        if (snackbar.isShownOrQueued) snackbar.dismiss()
        snackbar.setAction("Dismiss") { snackbar.dismiss() }
        snackbar.show()
    }
}
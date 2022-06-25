package it.unibo.mobilesystems.utils

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class OkDialogFragment(
    private val msg : String,
    private val onOkPressed : () -> Unit
) : DialogFragment() {

    companion object {
        const val TAG = "OkDialog"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", DialogInterface.OnClickListener { _, _ ->
                    onOkPressed()
                })
            builder.create()
        } ?: throw IllegalArgumentException("activity cannot be null")
    }

}
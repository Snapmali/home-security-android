package com.snapkirin.homesecurity.ui.util

import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.snapkirin.homesecurity.R

fun AppCompatActivity.showToast(@StringRes stringId: Int) {
    if (stringId == R.string.blank)
        return
    Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show()
}

fun AppCompatActivity.showDialog(
    title: Int,
    message: Int,
    positiveCallback: DialogInterface.OnClickListener? = null,
    hasNegativeButton: Boolean = true,
    negativeCallback: DialogInterface.OnClickListener? = null
) {
    val dialogBuilder = MaterialAlertDialogBuilder(this)
    dialogBuilder.apply {
        setTitle(title)
        setMessage(message)
        setPositiveButton(R.string.ok, positiveCallback)
        if (hasNegativeButton)
            setNegativeButton(R.string.cancel, negativeCallback)
    }
    dialogBuilder.show()
}

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
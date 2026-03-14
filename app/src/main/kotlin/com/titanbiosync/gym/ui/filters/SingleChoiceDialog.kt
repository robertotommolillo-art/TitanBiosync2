package com.titanbiosync.gym.ui.filters

import android.content.Context
import androidx.appcompat.app.AlertDialog

object SingleChoiceDialog {

    fun show(
        context: Context,
        title: String,
        options: List<String>,
        current: String?,
        onSelected: (String?) -> Unit
    ) {
        val items = options.toTypedArray()
        val currentIndex = current?.let { options.indexOf(it) } ?: -1

        AlertDialog.Builder(context)
            .setTitle(title)
            .setSingleChoiceItems(items, currentIndex) { dialog, which ->
                onSelected(options[which])
                dialog.dismiss()
            }
            .setNegativeButton("Annulla", null)
            .setNeutralButton("Reset") { _, _ ->
                onSelected(null)
            }
            .show()
    }

    fun showLabeled(
        context: Context,
        title: String,
        options: List<Pair<String, String>>, // (label, value)
        currentValue: String?,
        onSelectedValue: (String?) -> Unit
    ) {
        val labels = options.map { it.first }.toTypedArray()
        val values = options.map { it.second }
        val currentIndex = currentValue?.let { values.indexOf(it) } ?: -1

        AlertDialog.Builder(context)
            .setTitle(title)
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                onSelectedValue(values[which])
                dialog.dismiss()
            }
            .setNegativeButton("Annulla", null)
            .setNeutralButton("Reset") { _, _ ->
                onSelectedValue(null)
            }
            .show()
    }
}
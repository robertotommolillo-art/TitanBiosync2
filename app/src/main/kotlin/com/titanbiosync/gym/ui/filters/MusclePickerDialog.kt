package com.titanbiosync.gym.ui.filters

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.titanbiosync.data.local.entities.gym.MuscleEntity

object MusclePickerDialog {
    fun show(
        context: Context,
        muscles: List<MuscleEntity>,
        selectedIds: Set<String>,
        onSelected: (List<String>) -> Unit
    ) {
        val names = muscles.map { it.nameIt }.toTypedArray()
        val checked = muscles.map { it.id in selectedIds }.toBooleanArray()

        AlertDialog.Builder(context)
            .setTitle("Muscoli")
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setNegativeButton("Annulla", null)
            .setNeutralButton("Reset") { _, _ ->
                onSelected(emptyList())
            }
            .setPositiveButton("OK") { _, _ ->
                val result = muscles.mapIndexedNotNull { index, m ->
                    if (checked[index]) m.id else null
                }
                onSelected(result)
            }
            .show()
    }
}
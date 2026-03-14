package com.titanbiosync.ui.common

import android.text.InputFilter
import android.text.Spanned

/**
 * Permette solo numeri decimali positivi con:
 * - solo cifre 0-9
 * - al massimo un punto '.'
 * - max N cifre decimali
 *
 * Consente anche input intermedi mentre si digita: "" e "."
 */
class DecimalDigitsInputFilter(
    private val maxDecimals: Int
) : InputFilter {

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val inserted = source.subSequence(start, end).toString()
        if (inserted.isEmpty()) return null // delete/backspace

        val newText = dest.substring(0, dstart) + inserted + dest.substring(dend)

        if (newText.isEmpty() || newText == ".") return null

        // Solo cifre e un eventuale punto
        if (!newText.matches(Regex("^\\d*(\\.\\d*)?$"))) return ""

        val dot = newText.indexOf('.')
        if (dot >= 0) {
            val decimals = newText.length - dot - 1
            if (decimals > maxDecimals) return ""
        }

        return null
    }
}
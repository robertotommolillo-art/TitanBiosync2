package com.titanbiosync.data.local.json

/**
 * Salva in DB una stringa JSON molto semplice (una stringa tra virgolette).
 */
object SimpleJsonString {

    fun encode(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    fun encodeOrNull(value: String?): String? = value?.let(::encode)

    fun decode(json: String?): String? {
        if (json.isNullOrBlank()) return null
        val trimmed = json.trim()
        if (trimmed.length >= 2 && trimmed.first() == '"' && trimmed.last() == '"') {
            val inner = trimmed.substring(1, trimmed.length - 1)
            return inner
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }
        return trimmed
    }
}
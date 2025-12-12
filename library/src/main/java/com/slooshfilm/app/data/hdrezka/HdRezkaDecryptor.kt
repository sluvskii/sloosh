package com.slooshfilm.app.data.hdrezka

import android.util.Base64
import android.util.Log

object HdRezkaDecryptor {

    private const val TRASH_SEPARATOR = "//_//"
    private val TRASH_SYMBOLS = listOf("@", "#", "!", "^", "$")
    private val CLEAR_BEFORE_SYMBOLS = listOf('/', '=')

    // Lazy initialization of the trash regex to avoid re-computing it every time
    private val trashRegex: Regex by lazy {
        generateTrashRegExp(TRASH_SYMBOLS)
    }

    fun decrypt(encrypted: String): String? {
        if (!encrypted.startsWith("#")) return encrypted

        try {
            // Remove the initial "#h" (Lumen does encrypted.slice(2))
            val input = encrypted.substring(2)
            val decryptedRecursive = decryptRecursive(input)
            
            // Final Base64 decode
            val decodedBytes = Base64.decode(decryptedRecursive, Base64.DEFAULT)
            return String(decodedBytes)
        } catch (e: Exception) {
            Log.e("HdRezkaDecryptor", "Failed to decrypt string: $encrypted", e)
            return null
        }
    }

    private fun decryptRecursive(input: String): String {
        val indexes = indexesOf(input, TRASH_SEPARATOR)

        if (indexes.isEmpty()) {
            return input
        }

        // We want to find the "shortest" resulting string after trying to clean up at each separator.
        // In Kotlin, we can map and reduce.
        return indexes.map { index ->
            val partAfterSeparator = input.substring(index + TRASH_SEPARATOR.length)
            val (before, after) = divideAtFirstOccurrenceOfSymbols(partAfterSeparator, CLEAR_BEFORE_SYMBOLS)
            
            val cleanedBefore = before.replace(trashRegex, "")
            
            val candidate = input.substring(0, index) + cleanedBefore + after
            decryptRecursive(candidate)
        }.minByOrNull { it.length } ?: input
    }

    private fun indexesOf(input: String, search: String): List<Int> {
        val indexes = mutableListOf<Int>()
        var index = 0
        while (true) {
            index = input.indexOf(search, index)
            if (index == -1) break
            indexes.add(index)
            index++ 
        }
        return indexes
    }

    private fun divideAtFirstOccurrenceOfSymbols(input: String, symbols: List<Char>): Pair<String, String> {
        val index = input.indexOfFirst { symbols.contains(it) }
        return if (index != -1) {
            // Include the symbol in the first part (index + 1) matches Lumen's logic
            // Lumen: input.slice(0, index + 1), input.slice(index + 1)
            Pair(input.substring(0, index + 1), input.substring(index + 1))
        } else {
            Pair(input, "")
        }
    }

    private fun generateTrashRegExp(symbols: List<String>): Regex {
        // [2, 3].flatMap -> cartesianProduct -> base64 -> join
        val patterns = listOf(2, 3).flatMap { n ->
            cartesianProduct(symbols, n)
        }.map { trash ->
            // Base64 encode the trash string
            val encoded = Base64.encodeToString(trash.toByteArray(), Base64.NO_WRAP)
            // Regex escape might be needed if base64 contains special regex chars, 
            // but standard base64 is A-Za-z0-9+/= which are mostly safe except + and = (sometimes).
            // Better to escape just in case, though standard Base64 usually doesn't need heavy escaping for OR logic.
            // However, Regex.escape replacement is safer.
            Regex.escape(encoded)
        }
        
        return Regex(patterns.joinToString("|"))
    }

    private fun cartesianProduct(symbols: List<String>, n: Int): List<String> {
        if (n <= 1) return symbols
        
        val smallerProduct = cartesianProduct(symbols, n - 1)
        return smallerProduct.flatMap { first ->
            symbols.map { second -> first + second }
        }
    }
}

package com.baszincir.satis.util

/**
 * "6 MM ZİNCİR", "8 MM ZİNCİR", "10 MM ZİNCİR", "13 MM ZİNCİR" gibi başında
 * sayı olan ürün adlarını, düz alfabetik sıralamanın aksine (10, 13, 6, 8
 * şeklinde değil) sayısal olarak doğru sırada (6, 8, 10, 13) sıralar.
 * Sayı ile başlamayan metinler için normal alfabetik sıralamaya döner.
 */
object NaturalSort {
    private val leadingNumberRegex = Regex("^\\s*(\\d+)")

    val comparator: Comparator<String> = Comparator { a, b ->
        val numA = leadingNumberRegex.find(a)?.groupValues?.get(1)?.toLongOrNull()
        val numB = leadingNumberRegex.find(b)?.groupValues?.get(1)?.toLongOrNull()
        when {
            numA != null && numB != null && numA != numB -> numA.compareTo(numB)
            numA != null && numB != null -> a.compareTo(b)
            numA != null -> -1
            numB != null -> 1
            else -> a.compareTo(b)
        }
    }

    fun sorted(items: List<String>): List<String> = items.sortedWith(comparator)
}

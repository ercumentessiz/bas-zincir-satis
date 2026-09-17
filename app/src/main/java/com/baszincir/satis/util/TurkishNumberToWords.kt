package com.baszincir.satis.util

/**
 * Tam sayıları Türkçe okunuşuna çevirir (proforma faturada "GENEL TOPLAM" tutarının
 * yazıyla gösterilmesi için). Kuruş kısmı olmadan, tam lira tutarını çevirir.
 * Örnek: 1392000 -> "birmilyonüçyüzdoksanikibin"
 */
object TurkishNumberToWords {

    private val birler = arrayOf("", "bir", "iki", "üç", "dört", "beş", "altı", "yedi", "sekiz", "dokuz")
    private val onlar = arrayOf("", "on", "yirmi", "otuz", "kırk", "elli", "altmış", "yetmiş", "seksen", "doksan")
    private val basamaklar = arrayOf("", "bin", "milyon", "milyar", "trilyon")

    fun convert(number: Long): String {
        if (number == 0L) return "sıfır"
        var n = number
        val gruplar = mutableListOf<Int>()
        while (n > 0) {
            gruplar.add((n % 1000).toInt())
            n /= 1000
        }

        val sb = StringBuilder()
        for (i in gruplar.indices.reversed()) {
            val grup = gruplar[i]
            if (grup == 0) continue
            val grupYazi = threeDigitToWords(grup)
            // "bir bin" değil, sadece "bin" denir.
            if (i == 1 && grup == 1) {
                sb.append(basamaklar[1])
            } else {
                sb.append(grupYazi).append(basamaklar[i])
            }
        }
        return sb.toString()
    }

    private fun threeDigitToWords(number: Int): String {
        val sb = StringBuilder()
        val yuzler = number / 100
        val kalan = number % 100
        val on = kalan / 10
        val bir = kalan % 10

        if (yuzler > 0) {
            if (yuzler > 1) sb.append(birler[yuzler])
            sb.append("yüz")
        }
        if (on > 0) sb.append(onlar[on])
        if (bir > 0) sb.append(birler[bir])
        return sb.toString()
    }

    /** "#doksanbeşbinTürklirası#" biçiminde tam metni üretir (boşluksuz, bitişik). */
    fun toLiraText(amount: Long): String {
        return "#${convert(amount)}Türklirası#"
    }
}

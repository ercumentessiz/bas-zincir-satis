package com.baszincir.satis.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

val TR_LOCALE: Locale = Locale("tr", "TR")

/** Kullanıcı adı/şehir/vergi dairesi gibi alanlar için Türkçe kurallara uygun büyük harfe çevirir. */
fun String.toTrUpperCase(): String = this.uppercase(TR_LOCALE)

fun formatDate(millis: Long): String {
    if (millis == 0L) return ""
    val sdf = SimpleDateFormat("dd.MM.yyyy", TR_LOCALE)
    return sdf.format(Date(millis))
}

fun formatDateTime(millis: Long): String {
    if (millis == 0L) return ""
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", TR_LOCALE)
    return sdf.format(Date(millis))
}

/** 1160000.0 -> "1.160.000,00" */
fun formatMoney(value: Double): String {
    val nf = NumberFormat.getNumberInstance(TR_LOCALE)
    nf.minimumFractionDigits = 2
    nf.maximumFractionDigits = 2
    return nf.format(value)
}

fun formatMoneyTl(value: Double): String = "${formatMoney(value)} ₺"

fun parseTrDouble(text: String): Double {
    if (text.isBlank()) return 0.0
    val normalized = text.trim().replace(".", "").replace(",", ".")
    return normalized.toDoubleOrNull() ?: 0.0
}

/** Proforma No: yyyy + 5 sıfır + ay(2) + gün(2). Örn: 2026000000813 */
fun buildProformaNo(dateMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy", TR_LOCALE)
    val mdSdf = SimpleDateFormat("MMdd", TR_LOCALE)
    val date = Date(dateMillis)
    return "${sdf.format(date)}00000${mdSdf.format(date)}"
}

/** Sipariş No'yu "000077" biçiminde 6 haneli gösterir. */
fun formatSiparisNo(n: Int): String = n.toString().padStart(6, '0')

/**
 * Teklif tarihine göre "Opsiyon" (geçerlilik) tarih/saatini otomatik
 * hesaplar: normalde ertesi gün saat 17:00; teklif tarihi Cuma, Cumartesi
 * veya Pazar ise bir sonraki Pazartesi saat 17:00.
 */
fun computeOpsiyonMillis(dateMillis: Long): Long {
    if (dateMillis == 0L) return 0L
    val cal = java.util.Calendar.getInstance(TR_LOCALE).apply { timeInMillis = dateMillis }
    val daysToAdd = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.FRIDAY -> 3
        java.util.Calendar.SATURDAY -> 2
        java.util.Calendar.SUNDAY -> 1
        else -> 1
    }
    cal.add(java.util.Calendar.DAY_OF_MONTH, daysToAdd)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 17)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

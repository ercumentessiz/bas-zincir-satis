package com.baszincir.satis.util

/**
 * Teklif formundaki "Teklif Veren" alanı için sabit kişi listesi. Yeni bir
 * kişi eklemek veya imza görselini güncellemek istendiğinde bu dosya ve
 * ilgili drawable kaynağı (varsa) birlikte güncellenir.
 */
data class TeklifVeren(
    val ad: String,
    val unvan: String,
    /** Bu kişi için henüz imza görseli eklenmediyse false; PDF'te imza alanı boş bırakılır. */
    val hasSignature: Boolean
)

object TeklifVerenler {
    val all = listOf(
        TeklifVeren("Muzaffer Can", "Satış ve Pazarlama Müdürü", hasSignature = true),
        TeklifVeren("Cem Demir", "Satış ve Pazarlama Müdürü", hasSignature = false),
        TeklifVeren("Nadir Baş", "Şirket Müdürü", hasSignature = false)
    )

    fun findByAd(ad: String): TeklifVeren = all.find { it.ad == ad } ?: all[0]

    fun label(v: TeklifVeren): String = "${v.ad} - ${v.unvan}"
}

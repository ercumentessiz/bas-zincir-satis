package com.baszincir.satis.data

/** Bir müşteri kaydı. Firestore koleksiyonu: customers */
data class Customer @JvmOverloads constructor(
    var id: String = "",
    var name: String = "",
    var city: String = "",
    var taxOffice: String = "",
    var taxNumber: String = "",
    var tcNumber: String = "",
    var phone: String = "",
    var nameLower: String = ""
)

/** Katalog kaydı (Zincir ürünü veya Standart). Koleksiyon: products / standards */
data class CatalogItem @JvmOverloads constructor(
    var id: String = "",
    var name: String = ""
)

/** Teklif formundaki bir ürün kalemi (bir teklifte birden fazla ürün olabilir). */
data class QuoteItem @JvmOverloads constructor(
    var product: String = "",
    var adet: String = "",
    var zincirOzellikleri: String = "",
    var standart: String = "",
    var miktar: String = "",
    var miktarBirim: String = "Mt.",
    var fiyat: String = "",
    var kdv: String = "Hariçtir"
)

/** Teklif formu. Koleksiyon: quotes */
data class Quote @JvmOverloads constructor(
    var id: String = "",
    var customerId: String = "",
    var customerName: String = "",
    var city: String = "",
    var dateMillis: Long = 0L,
    /** Opsiyonel: "Sayın .... DİKKATİNE!" satırı için isim kısmı (boş bırakılabilir). */
    var dikkatEdilecekKisi: String = "",
    /** Bir teklifte birden fazla ürün kalemi olabilir. */
    var items: List<QuoteItem> = emptyList(),
    // --- Aşağıdaki tekil alanlar ESKİ (çoklu ürün desteğinden önceki) kayıtlar
    // için geriye dönük uyumluluk amacıyla korunuyor; yeni kayıtlarda
    // kullanılmıyor (items listesi kullanılıyor).
    var product: String = "",
    var adet: String = "",
    var zincirOzellikleri: String = "",
    var standart: String = "",
    var miktar: String = "",
    var miktarBirim: String = "Mt.",
    var fiyat: String = "",
    var kdv: String = "Hariçtir",
    // --- Tekliflin tamamı için geçerli (ürün bazında değil) alanlar ---
    var odeme: String = "Peşin",
    var vadeSuresi: String = "",
    var odemeNotu: String = "",
    /** "Peşin" seçiliyken opsiyonel açıklama (örn. "Siparişte Peşin, Teslimatta Peşin").
     * Doluysa PDF'te "Peşin" yerine bu metin yazılır; boşsa "Peşin" yazılır. */
    var pesinNotu: String = "",
    var teslimatSuresi: String = "",
    /** Nakliye artık ürün bazında değil, teklifin tamamı için tek seçilir. */
    var nakliye: String = "Hariçtir",
    var opsiyonMillis: Long = 0L,
    var teklifVerenAdi: String = "Muzaffer Can",
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L
)

/** Proforma fatura kalemi */
data class ProformaItem @JvmOverloads constructor(
    var stokKodu: String = "",
    /** Eski alan: geriye dönük uyumluluk için korunuyor (artık kullanılmıyor,
     * eski kayıtları okurken bu alan doluysa PDF'te yedek olarak kullanılır). */
    var ozellikler: String = "",
    /** Ürün Yönetimi kataloğundan seçilen/eklenen ürün adı. */
    var urunAdi: String = "",
    /** Bu ürün için seçilen standart. */
    var standart: String = "",
    /** Ürüne dair alt alta eklenen opsiyonel özellik satırları. */
    var ozellikSatirlari: List<String> = emptyList(),
    var birim: String = "Mt.",
    var miktar: String = "",
    var birimFiyati: String = ""
)

/** Proforma fatura. Koleksiyon: proformas */
data class Proforma @JvmOverloads constructor(
    var id: String = "",
    var customerId: String = "",
    var customerName: String = "",
    var city: String = "",
    var taxOffice: String = "",
    var taxNumber: String = "",
    var tcNumber: String = "",
    var proformaNo: String = "",
    var dateMillis: Long = 0L,
    var siparisNo: String = "",
    /** Eski alan: geriye dönük uyumluluk için korunuyor. Standart artık her
     * ürün kaleminin kendi içinde (ProformaItem.standart) tutuluyor. */
    var standart: String = "",
    var nakliye: String = "Hariçtir",
    /** "Hariç" ise sadece Toplam Fiyat gösterilir; "Dahil" ise %20 KDV eklenip Genel Toplam hesaplanır. */
    var kdv: String = "Hariç",
    var odeme: String = "Peşin",
    var vadeSuresi: String = "",
    var odemeNotu: String = "",
    /** "Peşin" seçiliyken opsiyonel açıklama (örn. "Siparişte Peşin, Teslimatta Peşin").
     * Doluysa PDF'te "Peşin" yerine bu metin yazılır; boşsa "Peşin" yazılır. */
    var pesinNotu: String = "",
    var items: List<ProformaItem> = emptyList(),
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L
)

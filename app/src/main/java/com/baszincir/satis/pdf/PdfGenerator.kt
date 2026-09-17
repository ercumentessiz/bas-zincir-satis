package com.baszincir.satis.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.baszincir.satis.R
import com.baszincir.satis.data.Proforma
import com.baszincir.satis.data.Quote
import com.baszincir.satis.data.QuoteItem
import com.baszincir.satis.util.TurkishNumberToWords
import com.baszincir.satis.util.TeklifVerenler
import com.baszincir.satis.util.formatDate
import com.baszincir.satis.util.formatDateTime
import com.baszincir.satis.util.formatMoney
import com.baszincir.satis.util.parseTrDouble
import java.io.File

/**
 * "Ataköy Marina - Teklif.doc" ve "KURTSAN_TARIM.docx" örnek dosyalarındaki
 * düzeni birebir taklit eden PDF üretici. Android'in yerleşik PdfDocument /
 * Canvas API'si kullanılır; ekstra bir kütüphaneye ihtiyaç yoktur.
 */
object PdfGenerator {

    // A4, 72 dpi (nokta) cinsinden: 595 x 842
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 48f

    private val NAVY = Color.rgb(30, 42, 120)
    private val RED = Color.rgb(190, 20, 20)
    private val BLACK = Color.BLACK

    // ---------------------------------------------------------------
    // TEKLİF
    // ---------------------------------------------------------------

    fun generateTeklifPdf(context: Context, quote: Quote): File {
        val document = PdfDocument()
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN

        // Eski (tek ürünlü) kayıtlarla geriye dönük uyumluluk: items listesi
        // boşsa, tekil alanlardan tek bir kalem oluştur.
        val effectiveItems: List<QuoteItem> = quote.items.ifEmpty {
            if (quote.product.isNotBlank()) listOf(
                QuoteItem(
                    product = quote.product, adet = quote.adet, zincirOzellikleri = quote.zincirOzellikleri,
                    standart = quote.standart, miktar = quote.miktar, miktarBirim = quote.miktarBirim,
                    fiyat = quote.fiyat, kdv = quote.kdv
                )
            ) else emptyList()
        }

        val letterhead = loadBitmap(context, R.drawable.letterhead)
        y = drawLetterhead(canvas, letterhead, y)
        y += 12f

        val label = Paint().apply { color = BLACK; textSize = 9.5f; isFakeBoldText = true; isAntiAlias = true }
        val value = Paint().apply { color = BLACK; textSize = 9.5f; isAntiAlias = true }
        val normal = Paint().apply { color = BLACK; textSize = 10f; isAntiAlias = true }

        // Ürün sütunu (1. ürün), altındaki Ödeme/Teslimat/Nakliye vb. alan ve en
        // alttaki kırmızı banka bilgileri hepsi MARGIN'den başladığı için ':'
        // işaretlerinin tepeden tırnağa aynı hizada durmasını istiyoruz. Üçü de
        // farklı yazı tipi boyutu kullandığından, her birinin kendi etiket
        // kümesini kendi fontuyla ölçüp en genişini ortak x olarak kullanıyoruz.
        val itemRowMeasurePaint = Paint().apply { textSize = 9f }
        val itemLabelMaxWidth = listOf("Adet", "Özellikleri", "Standart", "Miktar", "Fiyat", "KDV", "Toplam Fiyat")
            .maxOf { itemRowMeasurePaint.measureText(it) }
        val sharedLabelMaxWidth = listOf("Genel Toplam", "Ödeme", "Ödeme Notu", "Teslimat Süresi", "Nakliye", "Marka", "Menşei", "Sertifika", "Opsiyon")
            .maxOf { label.measureText(it) }
        val bankMeasurePaint = Paint().apply { textSize = 10.5f; isFakeBoldText = true }
        val bankLabelMaxWidth = listOf("Hesap Adı", "Banka", "Şube", "İban").maxOf { bankMeasurePaint.measureText(it) }
        val commonValueX = MARGIN + maxOf(itemLabelMaxWidth, sharedLabelMaxWidth, bankLabelMaxWidth) + 12f

        // Tarih (sağ üst)
        val dateText = formatDate(quote.dateMillis)
        canvas.drawText(dateText, PAGE_W - MARGIN - normal.measureText(dateText), y, normal)
        y += 18f

        // Müşteri adı / şehir
        val nameBold = Paint(normal).apply { isFakeBoldText = true; textSize = 11.5f }
        canvas.drawText(quote.customerName, MARGIN, y, nameBold)
        y += 14f
        canvas.drawText(quote.city, MARGIN, y, normal)
        y += 20f

        // Konu
        canvas.drawText("Konu\t:Teklif", MARGIN, y, Paint(normal).apply { isFakeBoldText = true })
        y += 18f

        // "Sayın .... DİKKATİNE!" satırı — opsiyonel, boşsa hiç yer kaplamaz
        if (quote.dikkatEdilecekKisi.isNotBlank()) {
            val dikkatPaint = Paint(normal).apply { isFakeBoldText = true; textAlign = Paint.Align.CENTER }
            canvas.drawText("Sayın ${quote.dikkatEdilecekKisi} DİKKATİNE!", PAGE_W / 2f, y, dikkatPaint)
            y += 18f
        }

        // Gövde metni (sabit) — birden fazla ürün varsa isimleri "/" ile birleştirir
        val productNames = effectiveItems.map { it.product }.filter { it.isNotBlank() }
        val productPhrase = when {
            productNames.isEmpty() -> ""
            productNames.size == 1 -> productNames[0]
            else -> productNames.joinToString(" / ")
        }
        val body1 = "Firmamızdan talep etmiş olduğunuz $productPhrase ile alakalı fiyat teklifimiz aşağıda bilgilerinize sunulmuştur."
        y = drawWrapped(canvas, body1, MARGIN, y, PAGE_W - 2 * MARGIN, normal, 12f)
        y += 4f
        val body2 = "Teklifimizi uygun bulacağınızı ümit eder işlerinizde kolaylıklar dileriz."
        y = drawWrapped(canvas, body2, MARGIN, y, PAGE_W - 2 * MARGIN, normal, 12f)
        y += 16f

        // İmza bloğu (sağ taraf): önce "Saygılarımızla,", ardından İsim / Ünvan /
        // İmza sırasıyla üstten alta, hepsi aynı bloğun ortasına hizalı.
        val signX = PAGE_W - MARGIN - 170f
        val teklifVeren = TeklifVerenler.findByAd(quote.teklifVerenAdi)
        val blockCenterX = (signX + (PAGE_W - MARGIN)) / 2f
        val centeredPaint = Paint(normal).apply { textAlign = Paint.Align.CENTER }

        canvas.drawText("Saygılarımızla,", blockCenterX, y, centeredPaint)
        y += 20f

        canvas.drawText(teklifVeren.ad, blockCenterX, y, centeredPaint)
        y += 12f
        canvas.drawText(teklifVeren.unvan, blockCenterX, y, centeredPaint)
        y += 10f

        if (teklifVeren.hasSignature) {
            val signature = loadBitmap(context, R.drawable.signature)
            if (signature != null) {
                val sw = 90f
                val sh = sw * signature.height / signature.width
                val sx = blockCenterX - sw / 2f
                canvas.drawBitmap(signature, null, android.graphics.RectF(sx, y, sx + sw, y + sh), null)
                y += sh
            }
        }
        y += 14f

        // Ürün(ler) — birden fazla ürün varsa yan yana sütunlar halinde, her
        // sütun "Ürün" adıyla başlayacak şekilde. Tek ürün varsa tek (geniş)
        // sütun olarak çizilir.
        if (effectiveItems.isNotEmpty()) {
            val gutter = 14f
            val contentWidth = PAGE_W - 2 * MARGIN
            val colCount = effectiveItems.size
            val colWidth = (contentWidth - (colCount - 1) * gutter) / colCount
            var maxBottom = y
            for ((idx, item) in effectiveItems.withIndex()) {
                val colX = MARGIN + idx * (colWidth + gutter)
                // Sadece 1. ürün sütunu MARGIN'den başladığı için, altındaki
                // bölümlerle aynı ':' hizasını yalnızca o kullanır. Diğer
                // sütunlar (varsa) kendi içinde ayrıca hizalı olur.
                val forcedValueX = if (idx == 0) commonValueX else null
                val bottom = drawQuoteItemColumn(canvas, item, colX, y, colWidth, forcedValueX)
                if (bottom > maxBottom) maxBottom = bottom
            }
            y = maxBottom + 10f
        }

        // Tekliflin tamamı için geçerli, ürün bazında olmayan alanlar
        val sharedFields = mutableListOf<Pair<String, String>>()
        // Birden fazla ürün varsa, "Ödeme" satırının hemen üstüne tüm
        // ürünlerin toplam fiyatlarının toplamını gösteren "Genel Toplam" eklenir.
        if (effectiveItems.size > 1) {
            val genelToplam = effectiveItems.sumOf { item ->
                val adetCarpani = parseTrDouble(item.adet).let { if (it == 0.0) 1.0 else it }
                parseTrDouble(item.miktar) * parseTrDouble(item.fiyat) * adetCarpani * (if (item.kdv == "Dahildir") 1.20 else 1.0)
            }
            sharedFields += "Genel Toplam" to "${formatMoney(genelToplam)} ₺"
        }
        val odemeValue = when {
            quote.odeme == "Vadeli" && quote.vadeSuresi.isNotBlank() -> "Vadeli (${quote.vadeSuresi})"
            quote.odeme == "Peşin" && quote.pesinNotu.isNotBlank() -> quote.pesinNotu
            else -> quote.odeme
        }
        sharedFields += "Ödeme" to odemeValue
        if (quote.odeme == "Vadeli" && quote.odemeNotu.isNotBlank()) {
            sharedFields += "Ödeme Notu" to quote.odemeNotu
        }
        sharedFields += "Teslimat Süresi" to quote.teslimatSuresi
        sharedFields += "Nakliye" to quote.nakliye
        sharedFields += "Marka" to "BAŞ ZİNCİR - Kendi İmalatımız"
        sharedFields += "Menşei" to "TM"
        sharedFields += "Sertifika" to "Teslimatta EN 10204-3.1 uygunluk test sertifikası verilecektir."
        if (quote.opsiyonMillis > 0L) {
            sharedFields += "Opsiyon" to "Teklifimiz ${formatDateTime(quote.opsiyonMillis)}'a kadar geçerlidir."
        }

        for ((k, v) in sharedFields) {
            if (v.isBlank()) continue
            if (y > PAGE_H - 130f) {
                document.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, document.pages.size + 1).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
            }
            canvas.drawText(k, MARGIN, y, label)
            canvas.drawText(":", commonValueX, y, value)
            val fieldTextX = commonValueX + value.measureText(": ")
            y = drawWrapped(canvas, v, fieldTextX, y, (PAGE_W - MARGIN) - fieldTextX, value, 12f)
            y += 5f
        }

        // Alt kısım: kırmızı banka bilgileri (sabit)
        if (y > PAGE_H - 120f) {
            document.finishPage(page)
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, document.pages.size + 1).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = MARGIN
        }
        y = maxOf(y + 16f, PAGE_H - 96f)
        val redBold = Paint().apply { color = RED; textSize = 10.5f; isFakeBoldText = true; isAntiAlias = true }
        // Tab karakterleri (\t) Canvas'ta güvenilir şekilde hizalanmadığı için,
        // etiketleri sola dayalı çizip ":" işaretini yukarıdaki bölümlerle aynı
        // ortak x konumunda (commonValueX) çiziyoruz; böylece en üstten en alta
        // kadar tüm ':' işaretleri aynı hizada durur.
        val bankRows = listOf(
            "Hesap Adı" to "BAŞ ZİNCİR SAN. VE TİC. A.Ş.",
            "Banka" to "HALK BANKASI",
            "Şube" to "BEYDAĞ",
            "İban" to "TR09 0001 2001 7930 0010 1001 18"
        )
        for ((k, v) in bankRows) {
            canvas.drawText(k, MARGIN, y, redBold)
            canvas.drawText(": $v", commonValueX, y, redBold)
            y += 16f
        }

        document.finishPage(page)
        val fileName = "${quote.customerName} - Teklif.pdf"
        return PdfFileStore.save(context, document, sanitizeFileName(fileName))
    }

    /**
     * Bir ürün kalemini, sütunun en üstünde ürün adı (kalın) olacak şekilde,
     * ardından geri kalan bilgileri alt alta çizer. Etiketler (Adet,
     * Özellikleri, Standart, Miktar, Fiyat, KDV, Toplam Fiyat) sütunun sol
     * kenarına dayalı (sola hizalı) yazılır; ":" işareti ise hepsinde aynı
     * sabit x konumunda durur, böylece alt alta hizalı görünür. Uzun
     * "Özellikleri" metni birden fazla satıra sarsa bile devam satırları da
     * sola dayalı kalır. Tek ürün olduğunda tam sayfa genişliğinde, birden
     * fazla ürün olduğunda dar bir sütun genişliğinde kullanılır.
     * [forcedValueX] verilirse (1. ürün sütunu için), ':' işareti sütunun
     * kendi etiket genişliği yerine bu sabit x konumunda çizilir; böylece
     * altındaki Ödeme/Teslimat/Nakliye ve kırmızı banka bilgisi bölümleriyle
     * aynı dikey hizada durur. Sonraki serbest Y konumunu döner.
     */
    private fun drawQuoteItemColumn(canvas: Canvas, item: QuoteItem, x: Float, startY: Float, width: Float, forcedValueX: Float? = null): Float {
        var cy = startY
        val headerPaint = Paint().apply { color = BLACK; textSize = 10.5f; isFakeBoldText = true; isAntiAlias = true }
        val rowPaint = Paint().apply { color = BLACK; textSize = 9f; isAntiAlias = true }

        cy = drawWrapped(canvas, item.product.ifBlank { "-" }, x, cy + 10f, width, headerPaint, 12f)
        cy += 3f

        val rows = mutableListOf<Pair<String, String>>()
        if (item.adet.isNotBlank()) rows += "Adet" to item.adet
        if (item.zincirOzellikleri.isNotBlank()) rows += "Özellikleri" to item.zincirOzellikleri
        if (item.standart.isNotBlank()) rows += "Standart" to item.standart
        if (item.miktar.isNotBlank()) rows += "Miktar" to "${item.miktar} ${item.miktarBirim}"
        if (item.fiyat.isNotBlank()) rows += "Fiyat" to "${item.fiyat} ₺/${item.miktarBirim}"
        rows += "KDV" to item.kdv
        val adetCarpani = parseTrDouble(item.adet).let { if (it == 0.0) 1.0 else it }
        val toplam = parseTrDouble(item.miktar) * parseTrDouble(item.fiyat) * adetCarpani * (if (item.kdv == "Dahildir") 1.20 else 1.0)
        rows += "Toplam Fiyat" to "${formatMoney(toplam)} ₺"

        // Etiketler sola dayalı; ":" işaretinin (ve değerin) her satırda aynı
        // sabit x konumundan başlaması için en uzun etiketin genişliği kadar
        // bir boşluk bırakılır (forcedValueX verilmediyse).
        val maxLabelWidth = rows.maxOf { rowPaint.measureText(it.first) }
        val valueX = forcedValueX ?: (x + maxLabelWidth + 12f)

        for ((k, v) in rows) {
            canvas.drawText(k, x, cy, rowPaint)
            canvas.drawText(":", valueX, cy, rowPaint)
            val fieldTextX = valueX + rowPaint.measureText(": ")
            cy = drawWrapped(canvas, v, fieldTextX, cy, (x + width) - fieldTextX, rowPaint, 10.5f)
            cy += 2f
        }
        return cy
    }

    // ---------------------------------------------------------------
    // PROFORMA FATURA
    // ---------------------------------------------------------------

    fun generateProformaPdf(context: Context, proforma: Proforma): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var y = MARGIN

        val letterhead = loadBitmap(context, R.drawable.letterhead)
        y = drawLetterhead(canvas, letterhead, y)
        y += 20f

        val normal = Paint().apply { color = BLACK; textSize = 10.5f; isAntiAlias = true }
        val bold = Paint(normal).apply { isFakeBoldText = true }
        val title = Paint().apply { color = BLACK; textSize = 16f; isFakeBoldText = true; isAntiAlias = true; textAlign = Paint.Align.CENTER }

        // Müşteri bilgisi (sol)
        canvas.drawText(proforma.customerName, MARGIN, y, bold)
        y += 15f
        canvas.drawText(proforma.city, MARGIN, y, normal)
        y += 15f
        if (proforma.tcNumber.isNotBlank()) {
            canvas.drawText("T.C.: ${proforma.tcNumber}", MARGIN, y, normal)
        } else {
            canvas.drawText("V.D.: ${proforma.taxOffice}   V.N.: ${proforma.taxNumber}", MARGIN, y, normal)
        }
        y += 26f

        // Başlık
        canvas.drawText("PROFORMA FATURA", PAGE_W / 2f, y, title)
        y += 26f

        // Üst bilgi satırları
        val infoLeftX = MARGIN
        val infoRightX = PAGE_W / 2f + 20f
        var yLeft = y
        canvas.drawText("Proforma Fatura No: ${proforma.proformaNo}", infoLeftX, yLeft, normal); yLeft += 16f
        canvas.drawText("Tarih: ${formatDate(proforma.dateMillis)}", infoLeftX, yLeft, normal)

        var yRight = y
        canvas.drawText("Sipariş No: ${proforma.siparisNo}", infoRightX, yRight, normal); yRight += 16f
        val odemeText = when {
            proforma.odeme == "Vadeli" && proforma.vadeSuresi.isNotBlank() -> "VADELİ (${proforma.vadeSuresi})"
            proforma.odeme == "Peşin" && proforma.pesinNotu.isNotBlank() -> proforma.pesinNotu.uppercase(com.baszincir.satis.util.TR_LOCALE)
            else -> proforma.odeme.uppercase(com.baszincir.satis.util.TR_LOCALE)
        }
        canvas.drawText("Ödeme: $odemeText", infoRightX, yRight, normal); yRight += 16f
        canvas.drawText("Nakliye: ${proforma.nakliye.uppercase(com.baszincir.satis.util.TR_LOCALE)}", infoRightX, yRight, normal)

        y = maxOf(yLeft, yRight) + 24f

        // Tablo — Miktar sütunu Birim'den önce gelir (kullanıcı önce miktarı,
        // sonra birimini görmek/girmek istiyor)
        val colWidths = floatArrayOf(45f, 175f, 60f, 55f, 90f, 74f) // toplam ~499
        val headers = arrayOf("SIRA\nNO", "ÜRÜN", "MİKTAR", "BİRİM", "BİRİM\nFİYATI", "FİYAT")
        val tableX = MARGIN
        val tableW = colWidths.sum()
        val headerPaint = Paint().apply { color = Color.WHITE; textSize = 8.5f; isFakeBoldText = true; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        val headerBg = Paint().apply { color = NAVY }
        val cellPaint = Paint().apply { color = BLACK; textSize = 9.5f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        val cellPaintLeft = Paint(cellPaint).apply { textAlign = Paint.Align.LEFT }
        val linePaint = Paint().apply { color = Color.rgb(180, 180, 180); strokeWidth = 0.7f }
        val borderPaint = Paint().apply { color = Color.rgb(120, 120, 120); style = Paint.Style.STROKE; strokeWidth = 1f }

        val headerH = 28f
        val headerTopY = y
        canvas.drawRect(tableX, headerTopY, tableX + tableW, headerTopY + headerH, headerBg)
        var cx = tableX
        for (i in headers.indices) {
            val cxCenter = cx + colWidths[i] / 2f
            val lines = headers[i].split("\n")
            var hy = headerTopY + headerH / 2f - (lines.size - 1) * 5f
            for (line in lines) {
                canvas.drawText(line, cxCenter, hy + 8f, headerPaint)
                hy += 10f
            }
            cx += colWidths[i]
        }
        y = headerTopY + headerH

        val lineHeight = 12f
        var total = 0.0
        for (item in proforma.items) {
            val miktar = parseTrDouble(item.miktar)
            val birimFiyat = parseTrDouble(item.birimFiyati)
            val fiyat = miktar * birimFiyat
            total += fiyat

            // Ürün hücresi: ürün adı, ardından (varsa) standart, ardından
            // (varsa) özellik satırları — birden fazla satır tutabilir; önce
            // kaç satıra ihtiyaç olduğunu hesaplayıp satır yüksekliğini buna
            // göre ayarlıyoruz ki yazılar üst üste binmesin veya taşmasın.
            // Eski kayıtlarda (ürün seçimi/özellik satırları eklenmeden önce
            // girilmiş) tek parça "ozellikler" metni varsa o kullanılır.
            val urunAdi = item.urunAdi.ifBlank { item.ozellikler }
            val standartSatiri = if (item.standart.isNotBlank()) "Standart: ${item.standart}" else null
            val paragraphs = (listOf(urunAdi) + listOfNotNull(standartSatiri) + item.ozellikSatirlari)
                .filter { it.isNotBlank() }
            val productLines = wrapMultilineText(paragraphs.joinToString("\n"), colWidths[1] - 8f, cellPaintLeft)
            val rowH = maxOf(22f, productLines.size * lineHeight + 10f)

            cx = tableX
            for (i in 0 until colWidths.size) {
                if (i == 1) {
                    var ly = y + lineHeight
                    for (line in productLines) {
                        canvas.drawText(line, cx + 4f, ly, cellPaintLeft)
                        ly += lineHeight
                    }
                } else {
                    val txt = when (i) {
                        0 -> item.stokKodu
                        2 -> formatMoney(miktar)
                        3 -> item.birim
                        4 -> formatMoney(birimFiyat)
                        else -> formatMoney(fiyat)
                    }
                    val textY = y + rowH / 2f + 3.5f
                    canvas.drawText(txt, cx + colWidths[i] / 2f, textY, cellPaint)
                }
                cx += colWidths[i]
            }
            canvas.drawLine(tableX, y + rowH, tableX + tableW, y + rowH, linePaint)
            y += rowH
        }
        val rowsBottomY = y

        // Dış çerçeve
        canvas.drawRect(tableX, headerTopY, tableX + tableW, rowsBottomY, borderPaint)
        cx = tableX
        for (w in colWidths) {
            canvas.drawLine(cx, headerTopY, cx, rowsBottomY, linePaint)
            cx += w
        }
        canvas.drawLine(tableX + tableW, headerTopY, tableX + tableW, rowsBottomY, linePaint)

        y += 10f

        // Toplamlar — "Hariç" seçiliyse sadece Toplam Fiyat, "Dahil" seçiliyse
        // TOPLAM / KDV %20 / GENEL TOPLAM üçü birden gösterilir.
        val kdvAmount = if (proforma.kdv == "Dahil") total * 0.20 else 0.0
        val genelToplam = total + kdvAmount
        val totalsValueX = tableX + tableW

        val totalsPaint = Paint().apply { color = BLACK; textSize = 10.5f; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
        val totalsBold = Paint(totalsPaint).apply { isFakeBoldText = true }
        val totalsLabelPaint = Paint(totalsPaint).apply { textAlign = Paint.Align.RIGHT }
        val totalsLabelBoldPaint = Paint(totalsBold).apply { textAlign = Paint.Align.RIGHT }

        if (proforma.kdv == "Dahil") {
            val toplamStr = "${formatMoney(total)} ₺"
            val kdvStr = "${formatMoney(kdvAmount)} ₺"
            val genelStr = "${formatMoney(genelToplam)} ₺"
            // Etiketlerin (ve sonlarındaki ":" işaretinin) alt alta aynı hizada
            // durması için, her satırın kendi değer genişliğine göre değil, üç
            // satırın en genişine göre SABİT bir sol sınır kullanılır.
            val maxValueWidth = maxOf(
                totalsPaint.measureText(toplamStr),
                totalsPaint.measureText(kdvStr),
                totalsBold.measureText(genelStr)
            )
            val labelX = totalsValueX - maxValueWidth - 10f

            fun drawTotalRow(label: String, valueText: String, rowY: Float, labelPaint: Paint, valuePaint: Paint) {
                canvas.drawText(valueText, totalsValueX, rowY, valuePaint)
                canvas.drawText(label, labelX, rowY, labelPaint)
            }

            drawTotalRow("TOPLAM:", toplamStr, y, totalsLabelPaint, totalsPaint)
            y += 18f
            drawTotalRow("KDV %20:", kdvStr, y, totalsLabelPaint, totalsPaint)
            y += 18f
            drawTotalRow("GENEL TOPLAM:", genelStr, y, totalsLabelBoldPaint, totalsBold)
            y += 30f
        } else {
            val toplamStr = "${formatMoney(total)} ₺"
            canvas.drawText(toplamStr, totalsValueX, y, totalsBold)
            val valueWidth = totalsBold.measureText(toplamStr)
            canvas.drawText("TOPLAM FİYAT:", totalsValueX - valueWidth - 10f, y, totalsLabelBoldPaint)
            y += 30f
        }

        // Yazıyla tutar — sol hizalı, "#doksanbeşbinTürklirası#" biçiminde
        val amountWords = TurkishNumberToWords.toLiraText(genelToplam.toLong())
        val wordsPaint = Paint().apply { color = BLACK; textSize = 10f; isFakeBoldText = true; isAntiAlias = true; textAlign = Paint.Align.LEFT }
        canvas.drawText(amountWords, tableX, y, wordsPaint)

        // Alt sabit not (nakliye bilgisi zaten üstte ayrıca gösterildiği için
        // burada tekrar edilmiyor)
        y = PAGE_H - 90f
        val notePaint = Paint().apply { color = BLACK; textSize = 9f; isAntiAlias = true }
        y = drawWrapped(
            canvas,
            "Satılan mal geri alınmaz. Müşteri talebi olmadıkça Mal Nakliye Sigortası yaptırılmaz.",
            MARGIN, y, PAGE_W - 2 * MARGIN, notePaint, 13f
        )
        y += 10f
        val bankPaint = Paint().apply { color = RED; textSize = 9.5f; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText(
            "T.C. HALK BANKASI / TR09 0001 2001 7930 0010 1001 18 / BAŞ ZİNCİR SAN. VE. TİC. A.Ş.",
            MARGIN, y, bankPaint
        )

        document.finishPage(page)
        val fileName = "${proforma.customerName} - Proforma Fatura.pdf"
        return PdfFileStore.save(context, document, sanitizeFileName(fileName))
    }

    // ---------------------------------------------------------------
    // Yardımcılar
    // ---------------------------------------------------------------

    private fun drawLetterhead(canvas: Canvas, bitmap: Bitmap?, startY: Float): Float {
        if (bitmap == null) return startY
        val w = PAGE_W - 2 * MARGIN
        val h = w * bitmap.height / bitmap.width
        val rect = android.graphics.RectF(MARGIN, startY, MARGIN + w, startY + h)
        canvas.drawBitmap(bitmap, null, rect, null)
        return startY + h
    }

    private fun loadBitmap(context: Context, resId: Int): Bitmap? = try {
        BitmapFactory.decodeResource(context.resources, resId)
    } catch (_: Exception) {
        null
    }

    /** Basit kelime bazlı satır sarma. Sonraki serbest Y konumunu döner. */
    /**
     * Kelime bazlı satır sarma. Metindeki gerçek satır sonlarını (\n) da her
     * zaman yeni bir satır olarak işler — böylece formda alt alta yazılan
     * özellikler (ör. "ISIL İŞLEMLİ" / "SİYAH BOYALI") PDF'te de alt alta
     * görünür, tek satıra sıkışmaz. Sonraki serbest Y konumunu döner.
     */
    private fun drawWrapped(canvas: Canvas, text: String, x: Float, startY: Float, maxWidth: Float, paint: Paint, lineHeight: Float): Float {
        if (text.isBlank()) return startY
        var y = startY
        for (paragraph in text.split("\n")) {
            if (paragraph.isEmpty()) {
                y += lineHeight
                continue
            }
            val words = paragraph.split(" ")
            var line = StringBuilder()
            for (word in words) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(test) > maxWidth && line.isNotEmpty()) {
                    canvas.drawText(line.toString(), x, y, paint)
                    y += lineHeight
                    line = StringBuilder(word)
                } else {
                    line = StringBuilder(test)
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line.toString(), x, y, paint)
                y += lineHeight
            }
        }
        return y
    }

    /**
     * Kullanıcının girdiği metni (örn. "13 MM G-80 ZİNCİR\nSİYAH BOYALI\n50'ŞER
     * METRE...") önce satır sonlarına, sonra da sütun genişliğine göre kelime
     * kelime sarmalayarak ayrı satırlar halinde döner. Proforma tablosunda
     * "Ürün" hücresinin kaç satır tutacağını önceden hesaplamak için kullanılır.
     */
    private fun wrapMultilineText(text: String, maxWidth: Float, paint: Paint): List<String> {
        if (text.isBlank()) return listOf("")
        val result = mutableListOf<String>()
        for (paragraph in text.split("\n")) {
            if (paragraph.isBlank()) {
                result.add("")
                continue
            }
            val words = paragraph.split(" ")
            var line = StringBuilder()
            for (word in words) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(test) > maxWidth && line.isNotEmpty()) {
                    result.add(line.toString())
                    line = StringBuilder(word)
                } else {
                    line = StringBuilder(test)
                }
            }
            result.add(line.toString())
        }
        return result
    }

    fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[/\\\\:*?\"<>|]"), "-").trim()
    }
}

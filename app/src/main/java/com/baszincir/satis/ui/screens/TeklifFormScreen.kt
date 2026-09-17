package com.baszincir.satis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.baszincir.satis.data.Customer
import com.baszincir.satis.data.Quote
import com.baszincir.satis.data.QuoteItem
import com.baszincir.satis.ui.components.DateField
import com.baszincir.satis.ui.components.PlainTextField
import com.baszincir.satis.ui.components.SearchableDropdown
import com.baszincir.satis.ui.components.SimpleDropdown
import com.baszincir.satis.ui.components.UppercaseTextField
import com.baszincir.satis.util.TeklifVerenler
import com.baszincir.satis.util.TurkishCities
import com.baszincir.satis.util.computeOpsiyonMillis
import com.baszincir.satis.util.formatDateTime
import com.baszincir.satis.util.formatMoney
import com.baszincir.satis.util.parseTrDouble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeklifFormScreen(
    initial: Quote?,
    customers: List<Customer>,
    products: List<String>,
    standards: List<String>,
    onBack: () -> Unit,
    onAddProduct: (String) -> Unit,
    onAddStandard: (String) -> Unit,
    onDeleteProductOption: (String) -> Unit,
    onDeleteStandardOption: (String) -> Unit,
    onSave: (Quote) -> Unit,
    onDelete: (() -> Unit)?
) {
    var customerId by remember { mutableStateOf(initial?.customerId ?: "") }
    var customerName by remember { mutableStateOf(initial?.customerName ?: "") }
    var city by remember { mutableStateOf(initial?.city ?: "") }
    var dateMillis by remember { mutableStateOf(initial?.dateMillis ?: System.currentTimeMillis()) }
    var dikkatEdilecekKisi by remember { mutableStateOf(initial?.dikkatEdilecekKisi ?: "") }
    var odeme by remember { mutableStateOf(initial?.odeme ?: "Peşin") }
    var vadeSuresi by remember { mutableStateOf(initial?.vadeSuresi ?: "") }
    var odemeNotu by remember { mutableStateOf(initial?.odemeNotu ?: "") }
    var pesinNotu by remember { mutableStateOf(initial?.pesinNotu ?: "") }
    var teslimatSuresi by remember { mutableStateOf(initial?.teslimatSuresi ?: "") }
    var nakliye by remember { mutableStateOf(initial?.nakliye ?: "Hariçtir") }
    var teklifVerenAdi by remember { mutableStateOf(initial?.teklifVerenAdi ?: TeklifVerenler.all.first().ad) }

    // Opsiyon (geçerlilik tarihi/saati) artık elle girilmiyor: teklif
    // tarihine göre otomatik hesaplanır (ertesi gün 17:00; teklif Cuma/hafta
    // sonuysa bir sonraki Pazartesi 17:00).
    val opsiyonMillis = remember(dateMillis) { computeOpsiyonMillis(dateMillis) }

    val items = remember {
        val startItems = initial?.items?.takeIf { it.isNotEmpty() }
            ?: initial?.let {
                // Eski (tek ürünlü) kayıt: geriye dönük uyumluluk için tek kalem olarak yükle.
                if (it.product.isNotBlank()) listOf(
                    QuoteItem(
                        product = it.product, adet = it.adet, zincirOzellikleri = it.zincirOzellikleri,
                        standart = it.standart, miktar = it.miktar, miktarBirim = it.miktarBirim,
                        fiyat = it.fiyat, kdv = it.kdv
                    )
                ) else null
            }
            ?: listOf(QuoteItem())
        startItems.toMutableStateList()
    }

    val customerNames = remember(customers) { customers.map { it.name }.sorted() }

    fun buildQuote(): Quote = Quote(
        id = initial?.id ?: "",
        customerId = customerId,
        customerName = customerName,
        city = city,
        dateMillis = dateMillis,
        dikkatEdilecekKisi = dikkatEdilecekKisi,
        items = items.toList(),
        odeme = odeme,
        vadeSuresi = vadeSuresi,
        odemeNotu = odemeNotu,
        pesinNotu = pesinNotu,
        teslimatSuresi = teslimatSuresi,
        nakliye = nakliye,
        opsiyonMillis = opsiyonMillis,
        teklifVerenAdi = teklifVerenAdi,
        createdAt = initial?.createdAt ?: 0L
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "Teklif Hazırla" else "Teklifi Düzenle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SearchableDropdown(
                label = "Müşteri Adı",
                options = customerNames,
                selected = customerName,
                onSelected = { picked ->
                    customerName = picked
                    customers.find { it.name.equals(picked, ignoreCase = true) }?.let {
                        customerId = it.id
                        city = it.city
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            SearchableDropdown(
                label = "Şehir",
                options = TurkishCities.all,
                selected = city,
                onSelected = { city = it },
                allowAddNew = true,
                onAddNew = { city = it }
            )
            Spacer(modifier = Modifier.height(12.dp))

            DateField(label = "Tarih", millis = dateMillis, onPicked = { dateMillis = it })
            Spacer(modifier = Modifier.height(12.dp))

            PlainTextField(
                value = dikkatEdilecekKisi,
                onValueChange = { dikkatEdilecekKisi = it },
                label = "Dikkat Edilecek Kişi (opsiyonel)"
            )
            Text(
                if (dikkatEdilecekKisi.isBlank()) "Boş bırakılırsa PDF'te bu satır hiç görünmez."
                else "PDF'te şöyle görünecek: \"Sayın $dikkatEdilecekKisi DİKKATİNE!\"",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text("Ürünler", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            items.forEachIndexed { index, item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row {
                            Text("Ürün ${index + 1}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            if (items.size > 1) {
                                IconButton(onClick = { items.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Ürünü sil")
                                }
                            }
                        }

                        SearchableDropdown(
                            label = "Zincir",
                            options = products,
                            selected = item.product,
                            onSelected = { items[index] = item.copy(product = it) },
                            allowAddNew = true,
                            onAddNew = { onAddProduct(it); items[index] = item.copy(product = it) },
                            onDeleteOption = onDeleteProductOption
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        PlainTextField(value = item.adet, onValueChange = { items[index] = item.copy(adet = it) }, label = "Adet")
                        Spacer(modifier = Modifier.height(10.dp))

                        UppercaseTextField(
                            value = item.zincirOzellikleri,
                            onValueChange = { items[index] = item.copy(zincirOzellikleri = it) },
                            label = "Zincir Özellikleri (opsiyonel)",
                            singleLine = false
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        SearchableDropdown(
                            label = "Standart",
                            options = standards,
                            selected = item.standart,
                            onSelected = { items[index] = item.copy(standart = it) },
                            allowAddNew = true,
                            onAddNew = { onAddStandard(it); items[index] = item.copy(standart = it) },
                            onDeleteOption = onDeleteStandardOption
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row {
                            PlainTextField(
                                value = item.miktar, onValueChange = { items[index] = item.copy(miktar = it) }, label = "Miktar",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            SimpleDropdown(
                                label = "Birim", options = listOf("Mt.", "Kg.", "Bakla", "Adet"), selected = item.miktarBirim,
                                onSelected = { items[index] = item.copy(miktarBirim = it) }, modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        PlainTextField(
                            value = item.fiyat, onValueChange = { items[index] = item.copy(fiyat = it) }, label = "Fiyat (TL)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        SimpleDropdown(
                            label = "KDV", options = listOf("Hariçtir", "Dahildir"), selected = item.kdv,
                            onSelected = { items[index] = item.copy(kdv = it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val toplamFiyat = parseTrDouble(item.miktar) * parseTrDouble(item.fiyat) *
                            parseTrDouble(item.adet).let { if (it == 0.0) 1.0 else it } *
                            (if (item.kdv == "Dahildir") 1.20 else 1.0)
                        Text("Toplam Fiyat: ${formatMoney(toplamFiyat)} ₺", fontWeight = FontWeight.Bold)
                    }
                }
            }

            OutlinedButton(
                onClick = { items.add(QuoteItem()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Yeni Ürün Ekle")
            }

            Spacer(modifier = Modifier.height(20.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            if (items.size > 1) {
                val genelToplam = items.sumOf { item ->
                    val adetCarpani = parseTrDouble(item.adet).let { if (it == 0.0) 1.0 else it }
                    parseTrDouble(item.miktar) * parseTrDouble(item.fiyat) * adetCarpani * (if (item.kdv == "Dahildir") 1.20 else 1.0)
                }
                Text("Genel Toplam: ${formatMoney(genelToplam)} ₺", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
            }

            SimpleDropdown(label = "Ödeme", options = listOf("Peşin", "Vadeli"), selected = odeme, onSelected = { odeme = it })
            if (odeme == "Vadeli") {
                Spacer(modifier = Modifier.height(12.dp))
                PlainTextField(value = vadeSuresi, onValueChange = { vadeSuresi = it }, label = "Vade Süresi")
                Spacer(modifier = Modifier.height(12.dp))
                UppercaseTextField(
                    value = odemeNotu,
                    onValueChange = { odemeNotu = it },
                    label = "Ödeme Notu",
                    singleLine = false,
                    placeholder = "Örn: Siparişte %30 nakit kalan kısmı teslimat sonrası 30 günde nakit ödemeli"
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                UppercaseTextField(
                    value = pesinNotu,
                    onValueChange = { pesinNotu = it },
                    label = "Ödeme Notu (opsiyonel)",
                    placeholder = "Örn: Siparişte Peşin, Teslimatta Peşin"
                )
                Text(
                    if (pesinNotu.isBlank()) "Boş bırakılırsa PDF'te \"Peşin\" yazar."
                    else "PDF'te \"Peşin\" yerine bu yazı görünecek.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            PlainTextField(value = teslimatSuresi, onValueChange = { teslimatSuresi = it }, label = "Teslimat Süresi")
            Spacer(modifier = Modifier.height(12.dp))

            SimpleDropdown(label = "Nakliye", options = listOf("Hariçtir", "Dahildir"), selected = nakliye, onSelected = { nakliye = it })
            Spacer(modifier = Modifier.height(12.dp))

            Text("Opsiyon (geçerlilik tarihi/saati): ${formatDateTime(opsiyonMillis)}")
            Text(
                "Teklif tarihine göre otomatik hesaplanır (ertesi gün 17:00; Cuma/hafta sonu ise Pazartesi 17:00).",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))

            SimpleDropdown(
                label = "Teklif Veren",
                options = TeklifVerenler.all.map { TeklifVerenler.label(it) },
                selected = TeklifVerenler.label(TeklifVerenler.findByAd(teklifVerenAdi)),
                onSelected = { picked ->
                    TeklifVerenler.all.find { TeklifVerenler.label(it) == picked }?.let { teklifVerenAdi = it.ad }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onSave(buildQuote()) },
                enabled = customerName.isNotBlank() && items.any { it.product.isNotBlank() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Kaydet ve PDF Oluştur")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

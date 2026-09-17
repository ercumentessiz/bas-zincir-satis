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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.baszincir.satis.data.Customer
import com.baszincir.satis.data.Proforma
import com.baszincir.satis.data.ProformaItem
import com.baszincir.satis.ui.components.PlainTextField
import com.baszincir.satis.ui.components.SearchableDropdown
import com.baszincir.satis.ui.components.SimpleDropdown
import com.baszincir.satis.ui.components.UppercaseTextField
import com.baszincir.satis.util.formatMoney
import com.baszincir.satis.util.parseTrDouble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProformaFormScreen(
    initial: Proforma?,
    customers: List<Customer>,
    products: List<String>,
    standards: List<String>,
    proformaNo: String,
    dateMillis: Long,
    siparisNo: String,
    onBack: () -> Unit,
    onAddProduct: (String) -> Unit,
    onAddStandard: (String) -> Unit,
    onDeleteProductOption: (String) -> Unit,
    onDeleteStandardOption: (String) -> Unit,
    onSave: (Proforma) -> Unit,
    onDelete: (() -> Unit)?
) {
    var customerId by remember { mutableStateOf(initial?.customerId ?: "") }
    var customerName by remember { mutableStateOf(initial?.customerName ?: "") }
    var city by remember { mutableStateOf(initial?.city ?: "") }
    var taxOffice by remember { mutableStateOf(initial?.taxOffice ?: "") }
    var taxNumber by remember { mutableStateOf(initial?.taxNumber ?: "") }
    var tcNumber by remember { mutableStateOf(initial?.tcNumber ?: "") }
    var nakliye by remember { mutableStateOf(initial?.nakliye ?: "Hariçtir") }
    var kdv by remember { mutableStateOf(initial?.kdv ?: "Hariç") }
    var odeme by remember { mutableStateOf(initial?.odeme ?: "Peşin") }
    var vadeSuresi by remember { mutableStateOf(initial?.vadeSuresi ?: "") }
    var odemeNotu by remember { mutableStateOf(initial?.odemeNotu ?: "") }
    var pesinNotu by remember { mutableStateOf(initial?.pesinNotu ?: "") }

    val items = remember {
        (initial?.items?.takeIf { it.isNotEmpty() } ?: listOf(ProformaItem(stokKodu = "1"))).toMutableStateList()
    }

    val customerNames = remember(customers) { customers.map { it.name }.sorted() }

    val total = items.sumOf { parseTrDouble(it.miktar) * parseTrDouble(it.birimFiyati) }
    val kdvAmount = if (kdv == "Dahil") total * 0.20 else 0.0
    val genelToplam = total + kdvAmount

    fun buildProforma(): Proforma = Proforma(
        id = initial?.id ?: "",
        customerId = customerId,
        customerName = customerName,
        city = city,
        taxOffice = taxOffice,
        taxNumber = taxNumber,
        tcNumber = tcNumber,
        proformaNo = initial?.proformaNo ?: proformaNo,
        dateMillis = initial?.dateMillis ?: dateMillis,
        siparisNo = initial?.siparisNo ?: siparisNo,
        nakliye = nakliye,
        kdv = kdv,
        odeme = odeme,
        vadeSuresi = vadeSuresi,
        odemeNotu = odemeNotu,
        pesinNotu = pesinNotu,
        items = items.toList(),
        createdAt = initial?.createdAt ?: 0L
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "Proforma Fatura Hazırla" else "Proformayı Düzenle") },
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
                        taxOffice = it.taxOffice
                        taxNumber = it.taxNumber
                        tcNumber = it.tcNumber
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Şehir: $city   |   " + if (tcNumber.isNotBlank()) "T.C.: $tcNumber" else "V.D.: $taxOffice   V.N.: $taxNumber")
            Spacer(modifier = Modifier.height(16.dp))

            Row {
                PlainTextField(value = initial?.proformaNo ?: proformaNo, onValueChange = {}, label = "Proforma Fatura No", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            PlainTextField(value = initial?.siparisNo ?: siparisNo, onValueChange = {}, label = "Sipariş No")
            Spacer(modifier = Modifier.height(12.dp))

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
                    placeholder = "Örn: Siparişte yarısı nakit kalan kısmı teslimat sonrası 30 günde nakit ödemeli"
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
            SimpleDropdown(label = "Nakliye", options = listOf("Hariçtir", "Dahildir"), selected = nakliye, onSelected = { nakliye = it })
            Spacer(modifier = Modifier.height(12.dp))
            SimpleDropdown(label = "KDV", options = listOf("Hariç", "Dahil"), selected = kdv, onSelected = { kdv = it })

            Spacer(modifier = Modifier.height(20.dp))
            Text("Kalemler", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            items.forEachIndexed { index, item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row {
                            Text("Sıra No: ${index + 1}", modifier = Modifier.weight(1f))
                            if (items.size > 1) {
                                IconButton(onClick = { items.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Kalemi sil")
                                }
                            }
                        }

                        SearchableDropdown(
                            label = "Ürün",
                            options = products,
                            selected = item.urunAdi,
                            onSelected = { items[index] = item.copy(urunAdi = it) },
                            allowAddNew = true,
                            onAddNew = { onAddProduct(it) },
                            onDeleteOption = onDeleteProductOption
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
                        Text("Ürün Özelliği (opsiyonel)", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        item.ozellikSatirlari.forEachIndexed { satirIndex, satir ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UppercaseTextField(
                                    value = satir,
                                    onValueChange = { newValue ->
                                        val updated = item.ozellikSatirlari.toMutableList()
                                        updated[satirIndex] = newValue
                                        items[index] = item.copy(ozellikSatirlari = updated)
                                    },
                                    label = "Özellik ${satirIndex + 1}",
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    val updated = item.ozellikSatirlari.toMutableList()
                                    updated.removeAt(satirIndex)
                                    items[index] = item.copy(ozellikSatirlari = updated)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Özelliği sil")
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        OutlinedButton(
                            onClick = {
                                items[index] = item.copy(ozellikSatirlari = item.ozellikSatirlari + "")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Özellik Ekle")
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row {
                            PlainTextField(
                                value = item.miktar,
                                onValueChange = { items[index] = item.copy(miktar = it) },
                                label = "Miktar",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            SimpleDropdown(
                                label = "Birim", options = listOf("Mt.", "Kg.", "Bakla", "Adet"), selected = item.birim,
                                onSelected = { items[index] = item.copy(birim = it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        PlainTextField(
                            value = item.birimFiyati,
                            onValueChange = { items[index] = item.copy(birimFiyati = it) },
                            label = "Birim Fiyatı (TL)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        val lineTotal = parseTrDouble(item.miktar) * parseTrDouble(item.birimFiyati)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Fiyat: ${formatMoney(lineTotal)} ₺")
                    }
                }
            }

            OutlinedButton(
                onClick = { items.add(ProformaItem(stokKodu = (items.size + 1).toString())) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Yeni Ürün Ekle")
            }

            Spacer(modifier = Modifier.height(20.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            if (kdv == "Dahil") {
                Text("TOPLAM: ${formatMoney(total)} ₺")
                Text("KDV %20: ${formatMoney(kdvAmount)} ₺")
                Text("GENEL TOPLAM: ${formatMoney(genelToplam)} ₺", fontWeight = FontWeight.Bold)
            } else {
                Text("Toplam Fiyat: ${formatMoney(total)} ₺", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onSave(buildProforma()) },
                enabled = customerName.isNotBlank() && items.any { it.miktar.isNotBlank() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Kaydet ve PDF Oluştur")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

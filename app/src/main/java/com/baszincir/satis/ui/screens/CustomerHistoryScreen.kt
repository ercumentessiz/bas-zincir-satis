package com.baszincir.satis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baszincir.satis.data.Customer
import com.baszincir.satis.data.Proforma
import com.baszincir.satis.data.Quote
import com.baszincir.satis.util.formatDate

/** Bir teklifin (yeni çoklu-ürün ya da eski tek-ürün kaydı fark etmeksizin) özet metnini üretir. */
private fun quoteSummary(q: Quote): String {
    val products = q.items.map { it.product }.filter { it.isNotBlank() }
    return if (products.isNotEmpty()) products.joinToString(" / ") else q.product.ifBlank { "Ürün belirtilmemiş" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHistoryScreen(
    customer: Customer,
    loadQuotes: suspend () -> List<Quote>,
    loadProformas: suspend () -> List<Proforma>,
    onBack: () -> Unit,
    onEditCustomer: () -> Unit,
    onOpenQuote: (Quote) -> Unit,
    onOpenProforma: (Proforma) -> Unit,
    onSharePdfQuote: (Quote) -> Unit,
    onSharePdfProforma: (Proforma) -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var quotes by remember { mutableStateOf<List<Quote>?>(null) }
    var proformas by remember { mutableStateOf<List<Proforma>?>(null) }

    LaunchedEffect(customer.id) {
        quotes = loadQuotes()
        proformas = loadProformas()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Müşteri Detayı") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = onEditCustomer) {
                        Icon(Icons.Default.Edit, contentDescription = "Müşteri Bilgilerini Düzenle")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Müşteri adı, üst çubuğun sabit yüksekliğine sığmayabilecek kadar
            // uzun olabileceği için burada, serbestçe alt satıra geçebileceği
            // bir alanda gösteriliyor.
            Text(
                text = customer.name,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            )

            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Teklifler") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Proforma Faturalar") })
            }

            if (tab == 0) {
                val list = quotes
                when {
                    list == null -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                    list.isEmpty() -> Text("Bu müşteri için kayıtlı teklif yok.", modifier = Modifier.padding(16.dp))
                    else -> LazyColumn {
                        items(list) { q ->
                            ListItem(
                                headlineContent = { Text("${formatDate(q.dateMillis)} — ${quoteSummary(q)}") },
                                supportingContent = {
                                    val count = q.items.size.coerceAtLeast(if (q.product.isNotBlank()) 1 else 0)
                                    Text(if (count > 1) "$count ürün" else "1 ürün")
                                },
                                trailingContent = {
                                    IconButton(onClick = { onSharePdfQuote(q) }) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Gönder")
                                    }
                                },
                                modifier = Modifier.clickable { onOpenQuote(q) }
                            )
                            Divider()
                        }
                    }
                }
            } else {
                val list = proformas
                when {
                    list == null -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                    list.isEmpty() -> Text("Bu müşteri için kayıtlı proforma yok.", modifier = Modifier.padding(16.dp))
                    else -> LazyColumn {
                        items(list) { p ->
                            ListItem(
                                headlineContent = { Text("${formatDate(p.dateMillis)} — No: ${p.proformaNo}") },
                                supportingContent = { Text("Sipariş No: ${p.siparisNo}") },
                                trailingContent = {
                                    IconButton(onClick = { onSharePdfProforma(p) }) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Gönder")
                                    }
                                },
                                modifier = Modifier.clickable { onOpenProforma(p) }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

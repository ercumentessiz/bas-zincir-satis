package com.baszincir.satis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import com.baszincir.satis.data.Proforma
import com.baszincir.satis.data.Quote
import com.baszincir.satis.util.formatDate

/** Bir teklifin (yeni çoklu-ürün ya da eski tek-ürün kaydı fark etmeksizin) özet metnini üretir. */
private fun quoteSummary(q: Quote): String {
    val products = q.items.map { it.product }.filter { it.isNotBlank() }
    return if (products.isNotEmpty()) products.joinToString(" / ") else q.product.ifBlank { "Ürün belirtilmemiş" }
}

/**
 * Herhangi bir müşteri seçmeden, kayıtlı TÜM teklif ve proforma faturaları
 * en yeniden en eskiye doğru listeler. Bir kayda dokunmak düzenleme formunu
 * açar (formdaki çöp kutusu simgesiyle silinebilir), PDF simgesi ise
 * yeniden PDF üretip paylaşım ekranını açar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OldRecordsScreen(
    loadAllQuotes: suspend () -> List<Quote>,
    loadAllProformas: suspend () -> List<Proforma>,
    onBack: () -> Unit,
    onOpenQuote: (Quote) -> Unit,
    onOpenProforma: (Proforma) -> Unit,
    onSharePdfQuote: (Quote) -> Unit,
    onSharePdfProforma: (Proforma) -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var quotes by remember { mutableStateOf<List<Quote>?>(null) }
    var proformas by remember { mutableStateOf<List<Proforma>?>(null) }

    LaunchedEffect(Unit) {
        quotes = loadAllQuotes()
        proformas = loadAllProformas()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eski Kayıtlar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Eski Teklifler") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Eski Proforma Faturalar") })
            }

            if (tab == 0) {
                val list = quotes
                when {
                    list == null -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                    list.isEmpty() -> Text("Henüz kayıtlı teklif yok.", modifier = Modifier.padding(16.dp))
                    else -> LazyColumn {
                        items(list) { q ->
                            ListItem(
                                headlineContent = { Text(q.customerName) },
                                supportingContent = { Text("${formatDate(q.dateMillis)} — ${quoteSummary(q)}") },
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
                    list.isEmpty() -> Text("Henüz kayıtlı proforma yok.", modifier = Modifier.padding(16.dp))
                    else -> LazyColumn {
                        items(list) { p ->
                            ListItem(
                                headlineContent = { Text(p.customerName) },
                                supportingContent = { Text("${formatDate(p.dateMillis)} — No: ${p.proformaNo} · Sipariş: ${p.siparisNo}") },
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

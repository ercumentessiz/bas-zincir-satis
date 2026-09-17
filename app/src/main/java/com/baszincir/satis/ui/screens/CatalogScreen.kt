package com.baszincir.satis.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.baszincir.satis.data.CatalogItem
import com.baszincir.satis.util.toTrUpperCase

/**
 * "Ürün Yönetimi": zincir ürünlerinin listelendiği, yeni ürün eklenip
 * mevcut ürünlerin silinebildiği ekran. (Standart listesi burada YOK;
 * standartlar sadece Teklif/Proforma formlarındaki "Standart" alanının
 * kendi "+ yeni ekle" seçeneğiyle yönetiliyor.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    products: List<CatalogItem>,
    onBack: () -> Unit,
    onAddProduct: (String) -> Unit,
    onDeleteProduct: (CatalogItem) -> Unit
) {
    var newProductName by remember { mutableStateOf("") }
    var pendingDeleteProduct by remember { mutableStateOf<CatalogItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ürün Yönetimi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                OutlinedTextField(
                    value = newProductName,
                    onValueChange = { newProductName = it.toTrUpperCase() },
                    label = { Text("Yeni zincir ürünü") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (newProductName.isNotBlank()) {
                        onAddProduct(newProductName)
                        newProductName = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Ekle")
                }
            }
            if (products.isEmpty()) {
                Text("Henüz zincir ürünü eklenmedi.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn {
                    items(products) { p ->
                        ListItem(
                            headlineContent = { Text(p.name) },
                            trailingContent = {
                                IconButton(onClick = { pendingDeleteProduct = p }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Sil")
                                }
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    pendingDeleteProduct?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteProduct = null },
            title = { Text("Ürünü sil") },
            text = { Text("\"${toDelete.name}\" listeden kaldırılsın mı?") },
            confirmButton = {
                TextButton(onClick = { onDeleteProduct(toDelete); pendingDeleteProduct = null }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteProduct = null }) { Text("Vazgeç") }
            }
        )
    }
}

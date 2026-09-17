package com.baszincir.satis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.baszincir.satis.data.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    customers: List<Customer>,
    onBack: () -> Unit,
    onPick: (Customer) -> Unit,
    onAddNew: () -> Unit,
    onDelete: (Customer) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, customers) {
        if (query.isBlank()) customers
        else customers.filter { it.name.contains(query, ignoreCase = true) }
    }
    var customerPendingDelete by remember { mutableStateOf<Customer?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Müşteriler (${customers.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Müşteri")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Müşteri ara...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
            LazyColumn {
                items(filtered) { customer ->
                    ListItem(
                        headlineContent = { Text(customer.name) },
                        supportingContent = { Text(customer.city) },
                        trailingContent = {
                            IconButton(onClick = { customerPendingDelete = customer }) {
                                Icon(Icons.Default.Delete, contentDescription = "Müşteriyi Sil")
                            }
                        },
                        modifier = Modifier.clickable { onPick(customer) }
                    )
                    Divider()
                }
            }
        }
    }

    val toDelete = customerPendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { customerPendingDelete = null },
            title = { Text("Müşteriyi sil") },
            text = {
                Text(
                    "\"${toDelete.name}\" silinsin mi? Bu müşterinin geçmiş teklif ve " +
                        "proforma faturaları silinmez, sadece müşteri kaydı ve seçim " +
                        "listesinden kaldırılır."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(toDelete)
                    customerPendingDelete = null
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { customerPendingDelete = null }) { Text("Vazgeç") }
            }
        )
    }
}

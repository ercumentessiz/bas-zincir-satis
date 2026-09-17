package com.baszincir.satis.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baszincir.satis.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTeklif: () -> Unit,
    onProforma: () -> Unit,
    onCustomers: () -> Unit,
    onOldRecords: () -> Unit,
    onCatalog: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Baş Zincir - Satış") },
                actions = {
                    IconButton(onClick = onOldRecords) {
                        Icon(Icons.Default.History, contentDescription = "Eski Kayıtlar")
                    }
                    IconButton(onClick = onCustomers) {
                        Icon(Icons.Default.People, contentDescription = "Müşteriler")
                    }
                    IconButton(onClick = onCatalog) {
                        Icon(Icons.Default.Category, contentDescription = "Ürün Yönetimi")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onTeklif,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("Teklif Hazırla", fontWeight = FontWeight.Bold)
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onProforma,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text("Proforma Fatura Hazırla", fontWeight = FontWeight.Bold)
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onOldRecords,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                Text("Eski Teklifler / Proforma Faturalar")
            }
        }
    }
}

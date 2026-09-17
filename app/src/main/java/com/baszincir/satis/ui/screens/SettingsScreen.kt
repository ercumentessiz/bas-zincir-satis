package com.baszincir.satis.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userEmail: String?,
    customerCount: Int,
    productCount: Int,
    standardCount: Int,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onReplaceAllCustomers: ((Int) -> Unit) -> Unit
) {
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showReplaceCustomersConfirm by remember { mutableStateOf(false) }
    var replacingCustomers by remember { mutableStateOf(false) }
    var replaceResultMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Baş Zincir - Satış")
            Spacer(modifier = Modifier.height(8.dp))
            if (userEmail != null) {
                Text("Giriş yapan hesap: $userEmail")
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text("Kayıtlı müşteri sayısı: $customerCount")
            Text("Kayıtlı zincir ürünü sayısı: $productCount")
            Text("Kayıtlı standart sayısı: $standardCount")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tüm veriler Firebase Firestore üzerinde saklanır ve cihazlar arasında senkronizedir. Sadece izin verilen hesaplar erişebilir.")

            Spacer(modifier = Modifier.height(32.dp))
            Text("Müşteri Listesi", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Uygulamayla birlikte gelen güncel müşteri listesini yeniden yükler. " +
                    "Mevcut TÜM müşteri kayıtları silinip yerine bu liste yazılır. " +
                    "Geçmiş teklif/proforma faturalar etkilenmez.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (replacingCustomers) {
                CircularProgressIndicator()
            } else {
                OutlinedButton(
                    onClick = { showReplaceCustomersConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Müşteri Listesini Yeniden Yükle")
                }
            }
            if (replaceResultMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(replaceResultMessage!!, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { showSignOutConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Çıkış Yap")
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Çıkış yap") },
            text = { Text("Hesabınızdan çıkış yapmak istediğinize emin misiniz? Tekrar giriş yapmanız gerekecek.") },
            confirmButton = {
                TextButton(onClick = { showSignOutConfirm = false; onSignOut() }) { Text("Çıkış Yap") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Vazgeç") }
            }
        )
    }

    if (showReplaceCustomersConfirm) {
        AlertDialog(
            onDismissRequest = { showReplaceCustomersConfirm = false },
            title = { Text("Müşteri listesini yenile") },
            text = {
                Text(
                    "Bu işlem mevcut TÜM müşterileri kalıcı olarak siler ve yerine " +
                        "uygulamayla gelen güncel listeyi yükler. Devam edilsin mi?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showReplaceCustomersConfirm = false
                    replacingCustomers = true
                    replaceResultMessage = null
                    onReplaceAllCustomers { count ->
                        replacingCustomers = false
                        replaceResultMessage = "$count müşteri yüklendi."
                    }
                }) { Text("Evet, Yenile") }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceCustomersConfirm = false }) { Text("Vazgeç") }
            }
        )
    }
}

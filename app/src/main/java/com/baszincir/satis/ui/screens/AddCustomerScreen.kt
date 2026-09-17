package com.baszincir.satis.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.baszincir.satis.data.Customer
import com.baszincir.satis.ui.components.PlainTextField
import com.baszincir.satis.ui.components.SearchableDropdown
import com.baszincir.satis.ui.components.UppercaseTextField
import com.baszincir.satis.util.TurkishCities

/**
 * `initial` null ise "Yeni Müşteri" ekleme formu, dolu ise o müşterinin
 * bilgilerini düzenleme formu olarak çalışır (aynı id ile üzerine kaydedilir).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    initial: Customer? = null,
    onBack: () -> Unit,
    onSave: (Customer) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var city by remember { mutableStateOf(initial?.city ?: "") }
    var taxOffice by remember { mutableStateOf(initial?.taxOffice ?: "") }
    var taxNumber by remember { mutableStateOf(initial?.taxNumber ?: "") }
    var tcNumber by remember { mutableStateOf(initial?.tcNumber ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "Yeni Müşteri" else "Müşteriyi Düzenle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
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
            UppercaseTextField(value = name, onValueChange = { name = it }, label = "Müşteri Adı")
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
            UppercaseTextField(value = taxOffice, onValueChange = { taxOffice = it }, label = "Vergi Dairesi")
            Spacer(modifier = Modifier.height(12.dp))
            PlainTextField(
                value = taxNumber, onValueChange = { taxNumber = it }, label = "Vergi Numarası (V.N.)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(12.dp))
            PlainTextField(
                value = tcNumber, onValueChange = { tcNumber = it }, label = "T.C. Kimlik No (şahıs ise)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(12.dp))
            PlainTextField(
                value = phone, onValueChange = { phone = it }, label = "Telefon",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    onSave(
                        Customer(
                            id = initial?.id ?: "",
                            name = name.trim(),
                            city = city.trim(),
                            taxOffice = taxOffice.trim(),
                            taxNumber = taxNumber.trim(),
                            tcNumber = tcNumber.trim(),
                            phone = phone.trim()
                        )
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (initial == null) "Kaydet" else "Güncelle") }
        }
    }
}

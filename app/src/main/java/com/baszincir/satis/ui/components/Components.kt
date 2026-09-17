package com.baszincir.satis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import com.baszincir.satis.util.TR_LOCALE
import com.baszincir.satis.util.formatDate
import com.baszincir.satis.util.formatDateTime
import com.baszincir.satis.util.toTrUpperCase
import java.util.Calendar

/**
 * Otomatik olarak büyük harfe çeviren metin alanı. "Not yazılabilecek alanlarda
 * klavye otomatik büyük harfle başlasın" isteğini KeyboardCapitalization.Characters
 * ile karşılar; ayrıca girilen değeri de her zaman büyük harfe zorlar.
 */
@Composable
fun UppercaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    placeholder: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.toTrUpperCase()) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Aranabilir, alfabetik/özel sıralı açılır menü. Listede olmayan bir değer
 * girilip "Yeni ekle" seçilerek katalog listesine (müşteri, ürün, standart vb.)
 * yeni kayıt eklenebilir. Sabit (deneysel olmayan) DropdownMenu API'si üzerine
 * kurulmuştur; bu yüzden herhangi bir OptIn gerekmez.
 */
@Composable
fun SearchableDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowAddNew: Boolean = false,
    onAddNew: (String) -> Unit = {},
    onDeleteOption: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(selected) }

    // Bu alan dışarıdan değişebilir (örn. müşteri seçilince şehir otomatik
    // dolduruluyor). "selected" sadece kullanıcı bu alandan çıktığında veya
    // listeden bir öğe seçtiğinde değiştiği için (aşağıya bakın), burada her
    // harf yazıldığında değil, sadece gerçekten dışarıdan bir değişiklik
    // olduğunda tetiklenir.
    androidx.compose.runtime.LaunchedEffect(selected) {
        query = selected
    }

    val filtered = remember(query, options) {
        val matches = if (query.isBlank()) options
        else options.filter { it.contains(query, ignoreCase = true) }
        // Çok uzun listelerde (örn. 357 müşteri) tamamını birden göstermek
        // yerine ilk 8 sonucu gösteriyoruz; aramaya devam edildikçe liste
        // daralır. Bu liste, ayrı bir pencere (popup) yerine doğrudan sayfa
        // akışı içinde çizilir; bazı cihazlarda popup pencerelerin klavyeyi
        // kapatması sorununu tamamen ortadan kaldırmak için bilerek bu yol
        // seçildi.
        matches.take(8)
    }
    val hiddenCount = remember(query, options) {
        val totalMatches = if (query.isBlank()) options.size
        else options.count { it.contains(query, ignoreCase = true) }
        (totalMatches - 8).coerceAtLeast(0)
    }
    val showAddOption = allowAddNew && query.isNotBlank() &&
        options.none { it.equals(query, ignoreCase = true) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it.toTrUpperCase()
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    // Kullanıcı bu alandan çıktığında (başka bir alana dokunduğunda
                    // veya "Kaydet" butonuna bastığında), o ana kadar yazdığı metni
                    // listeden seçmese bile "seçilmiş" say.
                    if (!focusState.isFocused) {
                        onSelected(query)
                        expanded = false
                    }
                }
        )
        if (expanded && (filtered.isNotEmpty() || showAddOption)) {
            androidx.compose.material3.Surface(
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                Column(modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                    filtered.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    query = item
                                    onSelected(item)
                                    expanded = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(text = item, modifier = Modifier.weight(1f))
                            if (onDeleteOption != null) {
                                IconButton(
                                    onClick = { onDeleteOption(item) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "\"$item\" listeden sil",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    if (hiddenCount > 0) {
                        Text(
                            text = "+$hiddenCount sonuç daha... aramaya devam edin",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    if (showAddOption) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddNew(query)
                                    onSelected(query)
                                    expanded = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("\"$query\" yeni ekle")
                        }
                    }
                }
            }
        }
    }
}

/** Sabit seçenekli (örn. Peşin/Vadeli, Hariçtir/Dahildir) basit açılır menü. */
@Composable
fun SimpleDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = {
                    onSelected(opt)
                    expanded = false
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    millis: Long,
    onPicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = if (millis > 0) formatDate(millis) else "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Takvim")
            }
        },
        modifier = modifier.fillMaxWidth()
    )
    if (showDialog) {
        val state = rememberDatePickerState(initialSelectedDateMillis = if (millis > 0) millis else System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPicked(it) }
                    showDialog = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Vazgeç") } }
        ) {
            androidx.compose.material3.DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    label: String,
    millis: Long,
    onPicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf(millis) }

    OutlinedTextField(
        value = if (millis > 0) formatDateTime(millis) else "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showDate = true }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Takvim")
            }
        },
        modifier = modifier.fillMaxWidth()
    )

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = if (millis > 0) millis else System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = state.selectedDateMillis ?: System.currentTimeMillis()
                    showDate = false
                    showTime = true
                }) { Text("İleri (saat seç)") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Vazgeç") } }
        ) {
            androidx.compose.material3.DatePicker(state = state)
        }
    }

    if (showTime) {
        val cal = Calendar.getInstance(TR_LOCALE).apply { timeInMillis = if (millis > 0) millis else System.currentTimeMillis() }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        Dialog(onDismissRequest = { showTime = false }) {
            androidx.compose.material3.Surface(shape = MaterialTheme.shapes.medium) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.heightIn(min = 8.dp))
                    TimePicker(state = timeState)
                    Button(onClick = {
                        val dateCal = Calendar.getInstance(TR_LOCALE).apply {
                            timeInMillis = pendingDateMillis
                            set(Calendar.HOUR_OF_DAY, timeState.hour)
                            set(Calendar.MINUTE, timeState.minute)
                        }
                        onPicked(dateCal.timeInMillis)
                        showTime = false
                    }) { Text("Tamam") }
                }
            }
        }
    }
}

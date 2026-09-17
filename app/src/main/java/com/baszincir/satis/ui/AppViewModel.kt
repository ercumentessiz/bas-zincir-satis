package com.baszincir.satis.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baszincir.satis.data.Customer
import com.baszincir.satis.data.CatalogItem
import com.baszincir.satis.data.Proforma
import com.baszincir.satis.data.Quote
import com.baszincir.satis.data.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppUiState(
    val loading: Boolean = true,
    val signedIn: Boolean = false,
    val authError: String? = null,
    val userEmail: String? = null,
    val customers: List<Customer> = emptyList(),
    val products: List<CatalogItem> = emptyList(),
    val standards: List<CatalogItem> = emptyList(),
    val message: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        val email = Repository.currentUserEmail()
        if (email != null) {
            _state.value = _state.value.copy(signedIn = true, userEmail = email)
            loadInitialData()
        } else {
            _state.value = _state.value.copy(loading = false)
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            Repository.seedCustomersIfEmpty(getApplication())
            Repository.seedStandardsIfEmpty()
            refreshAll()
        }
    }

    /** E-posta/şifre ile giriş dener. Firebase Console'da önceden oluşturulmuş
     * (izinli) hesaplar dışında hiçbir giriş başarılı olmaz. */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, authError = null)
            try {
                Repository.signIn(email, password)
                _state.value = _state.value.copy(signedIn = true, userEmail = Repository.currentUserEmail())
                loadInitialData()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    authError = "Giriş başarısız: e-posta veya şifre hatalı."
                )
            }
        }
    }

    fun signOut() {
        Repository.signOut()
        _state.value = AppUiState(loading = false, signedIn = false)
    }

    fun refreshAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val customers = Repository.getAllCustomers()
            val products = Repository.getAllProducts()
            val standards = Repository.getAllStandards()
            _state.value = _state.value.copy(
                loading = false,
                customers = customers,
                products = products,
                standards = standards
            )
        }
    }

    fun addCustomer(customer: Customer, onDone: (Customer) -> Unit) {
        viewModelScope.launch {
            val saved = Repository.addCustomer(customer)
            refreshAll()
            onDone(saved)
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            Repository.deleteCustomer(customerId)
            refreshAll()
        }
    }

    /** Tüm müşterileri siler ve assets/customers_seed.json'daki güncel listeyi yükler. */
    fun replaceAllCustomersFromAssets(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val count = Repository.replaceAllCustomersFromAssets(getApplication())
            refreshAll()
            onDone(count)
        }
    }

    fun addProduct(name: String) {
        viewModelScope.launch {
            Repository.addProduct(name)
            refreshAll()
        }
    }

    fun addStandard(name: String) {
        viewModelScope.launch {
            Repository.addStandard(name)
            refreshAll()
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            Repository.deleteProduct(id)
            refreshAll()
        }
    }

    fun deleteStandard(id: String) {
        viewModelScope.launch {
            Repository.deleteStandard(id)
            refreshAll()
        }
    }

    fun deleteProductByName(name: String) {
        viewModelScope.launch {
            Repository.deleteProductByName(name)
            refreshAll()
        }
    }

    fun deleteStandardByName(name: String) {
        viewModelScope.launch {
            Repository.deleteStandardByName(name)
            refreshAll()
        }
    }

    suspend fun quotesForCustomer(customerId: String): List<Quote> = Repository.getQuotesForCustomer(customerId)
    suspend fun proformasForCustomer(customerId: String): List<Proforma> = Repository.getProformasForCustomer(customerId)
    suspend fun allQuotes(): List<Quote> = Repository.getAllQuotes()
    suspend fun allProformas(): List<Proforma> = Repository.getAllProformas()

    suspend fun saveQuote(quote: Quote): Quote = Repository.saveQuote(quote)
    suspend fun deleteQuote(id: String) = Repository.deleteQuote(id)

    suspend fun saveProforma(proforma: Proforma): Proforma = Repository.saveProforma(proforma)
    suspend fun deleteProforma(id: String) = Repository.deleteProforma(id)
    suspend fun nextSiparisNo(): Int = Repository.nextSiparisNo()
}

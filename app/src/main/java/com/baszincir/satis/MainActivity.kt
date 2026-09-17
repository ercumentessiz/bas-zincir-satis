package com.baszincir.satis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.baszincir.satis.data.Customer
import com.baszincir.satis.data.Proforma
import com.baszincir.satis.data.Quote
import com.baszincir.satis.pdf.PdfGenerator
import com.baszincir.satis.pdf.ShareUtil
import com.baszincir.satis.ui.AppViewModel
import com.baszincir.satis.ui.screens.AddCustomerScreen
import com.baszincir.satis.ui.screens.CatalogScreen
import com.baszincir.satis.ui.screens.CustomerHistoryScreen
import com.baszincir.satis.ui.screens.CustomerListScreen
import com.baszincir.satis.ui.screens.HomeScreen
import com.baszincir.satis.ui.screens.LoginScreen
import com.baszincir.satis.ui.screens.OldRecordsScreen
import com.baszincir.satis.ui.screens.ProformaFormScreen
import com.baszincir.satis.ui.screens.SettingsScreen
import com.baszincir.satis.ui.screens.TeklifFormScreen
import com.baszincir.satis.ui.theme.BasZincirTheme
import com.baszincir.satis.util.buildProformaNo
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BasZincirTheme {
                Surface(modifier = Modifier) {
                    AppRoot(appViewModel)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(viewModel: AppViewModel) {
    val uiState by viewModel.state.collectAsState()

    if (!uiState.signedIn) {
        LoginScreen(
            loading = uiState.loading,
            error = uiState.authError,
            onLogin = { email, password -> viewModel.signIn(email, password) }
        )
        return
    }

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var selectedCustomerForHistory by remember { mutableStateOf<Customer?>(null) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var editingQuote by remember { mutableStateOf<Quote?>(null) }
    var editingProforma by remember { mutableStateOf<Proforma?>(null) }
    var pendingProformaNo by remember { mutableStateOf("") }
    var pendingProformaDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var pendingSiparisNo by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onTeklif = {
                    editingQuote = null
                    navController.navigate("teklif_form")
                },
                onProforma = {
                    editingProforma = null
                    scope.launch {
                        pendingProformaDate = System.currentTimeMillis()
                        pendingProformaNo = buildProformaNo(pendingProformaDate)
                        val n = viewModel.nextSiparisNo()
                        pendingSiparisNo = n.toString().padStart(6, '0')
                        navController.navigate("proforma_form")
                    }
                },
                onCustomers = { navController.navigate("customers") },
                onOldRecords = { navController.navigate("old_records") },
                onCatalog = { navController.navigate("catalog") },
                onSettings = { navController.navigate("settings") }
            )
        }

        composable("catalog") {
            CatalogScreen(
                products = uiState.products,
                onBack = { navController.popBackStack() },
                onAddProduct = { viewModel.addProduct(it) },
                onDeleteProduct = { item -> viewModel.deleteProduct(item.id) }
            )
        }

        composable("old_records") {
            OldRecordsScreen(
                loadAllQuotes = { viewModel.allQuotes() },
                loadAllProformas = { viewModel.allProformas() },
                onBack = { navController.popBackStack() },
                onOpenQuote = { q ->
                    editingQuote = q
                    navController.navigate("teklif_form")
                },
                onOpenProforma = { p ->
                    editingProforma = p
                    navController.navigate("proforma_form")
                },
                onSharePdfQuote = { q ->
                    val file = PdfGenerator.generateTeklifPdf(context, q)
                    ShareUtil.sharePdf(context, file)
                },
                onSharePdfProforma = { p ->
                    val file = PdfGenerator.generateProformaPdf(context, p)
                    ShareUtil.sharePdf(context, file)
                }
            )
        }

        composable("customers") {
            CustomerListScreen(
                customers = uiState.customers,
                onBack = { navController.popBackStack() },
                onPick = { customer ->
                    selectedCustomerForHistory = customer
                    navController.navigate("history")
                },
                onAddNew = {
                    editingCustomer = null
                    navController.navigate("add_customer")
                },
                onDelete = { customer -> viewModel.deleteCustomer(customer.id) }
            )
        }

        composable("add_customer") {
            AddCustomerScreen(
                initial = editingCustomer,
                onBack = { navController.popBackStack() },
                onSave = { customer ->
                    viewModel.addCustomer(customer) { saved ->
                        if (editingCustomer != null) {
                            selectedCustomerForHistory = saved
                        }
                        editingCustomer = null
                        navController.popBackStack()
                    }
                }
            )
        }

        composable("history") {
            val customer = selectedCustomerForHistory
            if (customer != null) {
                CustomerHistoryScreen(
                    customer = customer,
                    loadQuotes = { viewModel.quotesForCustomer(customer.id) },
                    loadProformas = { viewModel.proformasForCustomer(customer.id) },
                    onBack = { navController.popBackStack() },
                    onEditCustomer = {
                        editingCustomer = customer
                        navController.navigate("add_customer")
                    },
                    onOpenQuote = { q ->
                        editingQuote = q
                        navController.navigate("teklif_form")
                    },
                    onOpenProforma = { p ->
                        editingProforma = p
                        navController.navigate("proforma_form")
                    },
                    onSharePdfQuote = { q ->
                        val file = PdfGenerator.generateTeklifPdf(context, q)
                        ShareUtil.sharePdf(context, file)
                    },
                    onSharePdfProforma = { p ->
                        val file = PdfGenerator.generateProformaPdf(context, p)
                        ShareUtil.sharePdf(context, file)
                    }
                )
            }
        }

        composable("teklif_form") {
            TeklifFormScreen(
                initial = editingQuote,
                customers = uiState.customers,
                products = uiState.products.map { it.name },
                standards = uiState.standards.map { it.name },
                onBack = { navController.popBackStack() },
                onAddProduct = { viewModel.addProduct(it) },
                onAddStandard = { viewModel.addStandard(it) },
                onDeleteProductOption = { viewModel.deleteProductByName(it) },
                onDeleteStandardOption = { viewModel.deleteStandardByName(it) },
                onSave = { quote ->
                    scope.launch {
                        val saved = viewModel.saveQuote(quote)
                        val file = PdfGenerator.generateTeklifPdf(context, saved)
                        ShareUtil.sharePdf(context, file)
                        navController.popBackStack()
                    }
                },
                onDelete = editingQuote?.let { q ->
                    {
                        scope.launch {
                            viewModel.deleteQuote(q.id)
                            navController.popBackStack()
                        }
                    }
                }
            )
        }

        composable("proforma_form") {
            ProformaFormScreen(
                initial = editingProforma,
                customers = uiState.customers,
                products = uiState.products.map { it.name },
                standards = uiState.standards.map { it.name },
                proformaNo = pendingProformaNo,
                dateMillis = pendingProformaDate,
                siparisNo = pendingSiparisNo,
                onBack = { navController.popBackStack() },
                onAddProduct = { viewModel.addProduct(it) },
                onAddStandard = { viewModel.addStandard(it) },
                onDeleteProductOption = { viewModel.deleteProductByName(it) },
                onDeleteStandardOption = { viewModel.deleteStandardByName(it) },
                onSave = { proforma ->
                    scope.launch {
                        val saved = viewModel.saveProforma(proforma)
                        val file = PdfGenerator.generateProformaPdf(context, saved)
                        ShareUtil.sharePdf(context, file)
                        navController.popBackStack()
                    }
                },
                onDelete = editingProforma?.let { p ->
                    {
                        scope.launch {
                            viewModel.deleteProforma(p.id)
                            navController.popBackStack()
                        }
                    }
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                userEmail = uiState.userEmail,
                customerCount = uiState.customers.size,
                productCount = uiState.products.size,
                standardCount = uiState.standards.size,
                onBack = { navController.popBackStack() },
                onSignOut = { viewModel.signOut() },
                onReplaceAllCustomers = { onDone -> viewModel.replaceAllCustomersFromAssets(onDone) }
            )
        }
    }
}


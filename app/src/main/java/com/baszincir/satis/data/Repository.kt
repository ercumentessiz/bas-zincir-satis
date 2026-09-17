package com.baszincir.satis.data

import android.content.Context
import com.baszincir.satis.util.NaturalSort
import com.baszincir.satis.util.TR_LOCALE
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Tüm Firebase (Firestore + Auth) erişimini tek noktadan yöneten repository.
 * Uygulama, önceden Firebase Console'dan oluşturulmuş belirli e-posta/şifre
 * hesapları ile giriş gerektirir (Anonim giriş KULLANILMAZ). Bu sayede hem
 * Firestore güvenlik kuralları hem de bu kod, sadece izin verilen kişilerin
 * veriye erişebilmesini garanti eder.
 */
object Repository {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private const val COL_CUSTOMERS = "customers"
    private const val COL_PRODUCTS = "products"
    private const val COL_STANDARDS = "standards"
    private const val COL_QUOTES = "quotes"
    private const val COL_PROFORMAS = "proformas"
    private const val COL_COUNTERS = "counters"

    /** Uygulama açıldığında daha önce giriş yapılmış mı diye kontrol etmek için. */
    fun currentUserEmail(): String? = auth.currentUser?.email

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    fun signOut() {
        auth.signOut()
    }

    // ---------------- Customers ----------------

    suspend fun getAllCustomers(): List<Customer> {
        val snap = db.collection(COL_CUSTOMERS).orderBy("nameLower", Query.Direction.ASCENDING).get().await()
        return snap.documents.mapNotNull { doc ->
            doc.toObject(Customer::class.java)?.apply { id = doc.id }
        }
    }

    suspend fun addCustomer(customer: Customer): Customer {
        val data = customer.copy(nameLower = customer.name.lowercase(TR_LOCALE))
        val ref = if (customer.id.isBlank()) db.collection(COL_CUSTOMERS).document() else db.collection(COL_CUSTOMERS).document(customer.id)
        data.id = ref.id
        ref.set(data).await()
        return data
    }

    /**
     * Müşteri kaydını siler. Bu müşteriye ait geçmiş teklif/proforma faturalar
     * silinmez (o kayıtlar müşteri adı/şehir/vergi bilgilerini kendi içinde
     * ayrıca sakladığı için silinen müşteriden bağımsız olarak görüntülenmeye
     * devam eder); sadece müşteri artık seçim listelerinde görünmez.
     */
    suspend fun deleteCustomer(id: String) {
        db.collection(COL_CUSTOMERS).document(id).delete().await()
    }

    /** İlk kurulumda müşteri listesini assets/customers_seed.json dosyasından Firestore'a yükler. */
    suspend fun seedCustomersIfEmpty(context: Context): Int {
        val existing = db.collection(COL_CUSTOMERS).limit(1).get().await()
        if (!existing.isEmpty) return 0
        return loadCustomersFromAssets(context)
    }

    /**
     * Firestore'daki TÜM müşteri kayıtlarını siler ve assets/customers_seed.json
     * dosyasındaki güncel listeyi baştan yükler. Bu, dosyada olmayan eski
     * müşterileri kalıcı olarak kaldırır. Geri alınamaz; çağıran taraf
     * (Ayarlar ekranı) kullanıcıdan onay almalıdır. Bu müşterilere ait geçmiş
     * teklif/proforma kayıtları etkilenmez (kendi bilgilerini ayrıca saklarlar).
     */
    suspend fun replaceAllCustomersFromAssets(context: Context): Int {
        // Firestore tek seferde en fazla 500 belgeyi silme/yazma işlemine izin
        // verdiği için, mevcut kayıtları sayfa sayfa (batch'ler halinde) siliyoruz.
        while (true) {
            val snap = db.collection(COL_CUSTOMERS).limit(400).get().await()
            if (snap.isEmpty) break
            val batch = db.batch()
            for (doc in snap.documents) batch.delete(doc.reference)
            batch.commit().await()
        }
        return loadCustomersFromAssets(context)
    }

    private suspend fun loadCustomersFromAssets(context: Context): Int {
        val json = context.assets.open("customers_seed.json").use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        }
        val arr = JSONArray(json)
        var batch = db.batch()
        var count = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val ref = db.collection(COL_CUSTOMERS).document()
            val name = o.optString("name")
            val map = mapOf(
                "id" to ref.id,
                "name" to name,
                "nameLower" to name.lowercase(TR_LOCALE),
                "city" to o.optString("city"),
                "taxOffice" to o.optString("taxOffice"),
                "taxNumber" to o.optString("taxNumber"),
                "tcNumber" to o.optString("tcNumber"),
                "phone" to o.optString("phone")
            )
            batch.set(ref, map)
            count++
            // Firestore batch limiti 500 yazma işlemidir.
            if (count % 450 == 0) {
                batch.commit().await()
                batch = db.batch()
            }
        }
        batch.commit().await()
        return count
    }

    // ---------------- Catalog: Products (Zincir) & Standards ----------------

    suspend fun getAllProducts(): List<CatalogItem> {
        val items = getCatalog(COL_PRODUCTS)
        // Firestore'un "name" alanına göre alfabetik sıralaması "10 MM" gibi
        // sayısal başlıklı ürünleri yanlış sıraya koyar (10, 13, 6, 8 gibi).
        // Bunun yerine sayısal olarak doğru sırayı (6, 8, 10, 13) veren
        // NaturalSort ile burada, istemci tarafında yeniden sıralıyoruz.
        val order = NaturalSort.sorted(items.map { it.name }).withIndex().associate { (i, name) -> name to i }
        return items.sortedBy { order[it.name] ?: Int.MAX_VALUE }
    }

    suspend fun getAllStandards(): List<CatalogItem> = getCatalog(COL_STANDARDS)

    private suspend fun getCatalog(collection: String): List<CatalogItem> {
        val snap = db.collection(collection).orderBy("name", Query.Direction.ASCENDING).get().await()
        return snap.documents.mapNotNull { it.toObject(CatalogItem::class.java)?.apply { id = it.id } }
    }

    suspend fun addProduct(name: String) = addCatalogItem(COL_PRODUCTS, name)
    suspend fun addStandard(name: String) = addCatalogItem(COL_STANDARDS, name)
    suspend fun deleteProduct(id: String) = db.collection(COL_PRODUCTS).document(id).delete().await()
    suspend fun deleteStandard(id: String) = db.collection(COL_STANDARDS).document(id).delete().await()
    suspend fun deleteProductByName(name: String) = deleteCatalogItemByName(COL_PRODUCTS, name)
    suspend fun deleteStandardByName(name: String) = deleteCatalogItemByName(COL_STANDARDS, name)

    private suspend fun deleteCatalogItemByName(collection: String, name: String) {
        val snap = db.collection(collection).whereEqualTo("name", name).get().await()
        for (doc in snap.documents) doc.reference.delete().await()
    }

    private suspend fun addCatalogItem(collection: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val existing = db.collection(collection).whereEqualTo("name", trimmed).limit(1).get().await()
        if (!existing.isEmpty) return
        val ref = db.collection(collection).document()
        ref.set(CatalogItem(id = ref.id, name = trimmed)).await()
    }

    /** İlk kurulumda "Standart" listesini önceden tanımlı DIN normlarıyla doldurur. */
    suspend fun seedStandardsIfEmpty() {
        val existing = db.collection(COL_STANDARDS).limit(1).get().await()
        if (!existing.isEmpty) return
        val defaults = listOf("DIN 763", "DIN 766", "DIN 5685")
        val batch = db.batch()
        for (name in defaults) {
            val ref = db.collection(COL_STANDARDS).document()
            batch.set(ref, CatalogItem(id = ref.id, name = name))
        }
        batch.commit().await()
    }

    // ---------------- Quotes (Teklifler) ----------------

    suspend fun saveQuote(quote: Quote): Quote {
        val now = System.currentTimeMillis()
        val ref = if (quote.id.isBlank()) db.collection(COL_QUOTES).document() else db.collection(COL_QUOTES).document(quote.id)
        quote.id = ref.id
        if (quote.createdAt == 0L) quote.createdAt = now
        quote.updatedAt = now
        ref.set(quote).await()
        return quote
    }

    suspend fun deleteQuote(id: String) {
        db.collection(COL_QUOTES).document(id).delete().await()
    }

    suspend fun getQuotesForCustomer(customerId: String): List<Quote> {
        // NOT: Burada bilerek orderBy kullanılmıyor. Firestore, "eşitlik filtresi +
        // farklı bir alana göre sıralama" birleşimi için Firebase Console'da elle
        // oluşturulması gereken bir "composite index" ister; bu olmadan sorgu hata
        // fırlatıp uygulamayı çökertir. Bunun yerine sıralamayı burada, istemci
        // tarafında yapıyoruz.
        val snap = db.collection(COL_QUOTES)
            .whereEqualTo("customerId", customerId)
            .get().await()
        return snap.documents.mapNotNull { it.toObject(Quote::class.java)?.apply { id = it.id } }
            .sortedByDescending { it.dateMillis }
    }

    /** Müşteri ayrımı yapmadan tüm teklifleri en yeniden en eskiye getirir ("Eski Teklifler" ekranı için). */
    suspend fun getAllQuotes(limit: Long = 300): List<Quote> {
        val snap = db.collection(COL_QUOTES)
            .orderBy("dateMillis", Query.Direction.DESCENDING)
            .limit(limit)
            .get().await()
        return snap.documents.mapNotNull { it.toObject(Quote::class.java)?.apply { id = it.id } }
    }

    // ---------------- Proformas ----------------

    /** counters/proforma dokümanındaki siparisNo alanını atomik olarak 1 artırır (77'den başlar). */
    suspend fun nextSiparisNo(): Int {
        val ref = db.collection(COL_COUNTERS).document("proforma")
        return db.runTransaction { tx ->
            val snap = tx.get(ref)
            val current = snap.getLong("siparisNo") ?: 76L
            val next = current + 1
            tx.set(ref, mapOf("siparisNo" to next))
            next
        }.await().toInt()
    }

    suspend fun saveProforma(proforma: Proforma): Proforma {
        val now = System.currentTimeMillis()
        val ref = if (proforma.id.isBlank()) db.collection(COL_PROFORMAS).document() else db.collection(COL_PROFORMAS).document(proforma.id)
        proforma.id = ref.id
        if (proforma.createdAt == 0L) proforma.createdAt = now
        proforma.updatedAt = now
        ref.set(proforma).await()
        return proforma
    }

    suspend fun deleteProforma(id: String) {
        db.collection(COL_PROFORMAS).document(id).delete().await()
    }

    suspend fun getProformasForCustomer(customerId: String): List<Proforma> {
        val snap = db.collection(COL_PROFORMAS)
            .whereEqualTo("customerId", customerId)
            .get().await()
        return snap.documents.mapNotNull { it.toObject(Proforma::class.java)?.apply { id = it.id } }
            .sortedByDescending { it.dateMillis }
    }

    /** Müşteri ayrımı yapmadan tüm proforma faturaları en yeniden en eskiye getirir ("Eski Proforma Faturalar" ekranı için). */
    suspend fun getAllProformas(limit: Long = 300): List<Proforma> {
        val snap = db.collection(COL_PROFORMAS)
            .orderBy("dateMillis", Query.Direction.DESCENDING)
            .limit(limit)
            .get().await()
        return snap.documents.mapNotNull { it.toObject(Proforma::class.java)?.apply { id = it.id } }
    }
}

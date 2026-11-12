package com.cafe.accounts.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cafe.accounts.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    private val prefs = app.getSharedPreferences("procaffe", 0)

    val products = dao.products().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val customers = dao.customersWithDebt().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val invoices = dao.recentInvoices().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _rate = MutableStateFlow(prefs.getFloat("rate", 0f).toDouble())
    val rate: StateFlow<Double> = _rate
    private val _rateInfo = MutableStateFlow(prefs.getString("rateInfo", "لم يتم تحديد السعر") ?: "")
    val rateInfo: StateFlow<String> = _rateInfo

    /** productId -> qty */
    val cart = mutableStateMapOf<Long, Double>()
    /** purchase price typed per item (in the item's own currency) */
    val priceText = mutableStateMapOf<Long, String>()

    fun clearCart() { cart.clear(); priceText.clear() }
    fun unitCost(p: Product): Double = priceText[p.id]?.toDoubleOrNull() ?: p.costPrice
    fun itemsOf(id: Long) = dao.itemsOf(id)

    private val _candidates = MutableStateFlow<List<Double>>(emptyList())
    val candidates: StateFlow<List<Double>> = _candidates
    private val _fetching = MutableStateFlow(false)
    val fetching: StateFlow<Boolean> = _fetching

    init {
        viewModelScope.launch {
            if (dao.productCount() == 0) dao.insertProducts(Seed.products())
        }
        refreshRate()
    }

    fun refreshRate() = viewModelScope.launch {
        _fetching.value = true
        val res = RateFetcher.fetch()
        _fetching.value = false
        _candidates.value = res.candidates
        if (res.rate != null) {
            setRate(res.rate, "من موقع الليرة اليوم")
        } else {
            val keep = if (_rate.value > 0) " — بقي السعر المحفوظ" else ""
            _rateInfo.value = "فشل التحديث (${res.error})$keep. أدخل السعر يدوياً"
        }
    }

    fun setRate(r: Double, source: String = "يدوي") {
        _rate.value = r
        val stamp = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.US).format(java.util.Date())
        _rateInfo.value = "$source • $stamp"
        prefs.edit().putFloat("rate", r.toFloat()).putString("rateInfo", _rateInfo.value).apply()
    }

    fun toSyp(amount: Double, currency: String) = if (currency == "USD") amount * _rate.value else amount

    // ---- cart ----
    fun add(p: Product, d: Double = 1.0) {
        val n = (cart[p.id] ?: 0.0) + d
        if (n <= 0) cart.remove(p.id) else cart[p.id] = n
    }

    fun cartTotal(sale: Boolean): Double = cart.entries.sumOf { (id, q) ->
        val p = products.value.firstOrNull { it.id == id } ?: return@sumOf 0.0
        toSyp(if (sale) p.salePrice else unitCost(p), p.currency) * q
    }

    /** customerId == null means cash sale. paid defaults to full total. */
    fun checkoutSale(customerId: Long?, paid: Double?, onDone: (Long) -> Unit = {}) = viewModelScope.launch {
        val total = cartTotal(true)
        val pd = if (customerId == null) total else (paid ?: total)
        val invId = dao.insertInvoice(Invoice(type = "SALE", customerId = customerId, total = total, paid = pd, rate = _rate.value))
        val items = cart.mapNotNull { (id, q) ->
            val p = products.value.firstOrNull { it.id == id } ?: return@mapNotNull null
            dao.adjustStock(id, -q)
            InvoiceItem(invoiceId = invId, productId = id, productName = p.name, qty = q,
                unitPrice = toSyp(p.salePrice, p.currency), unitCost = toSyp(p.costPrice, p.currency))
        }
        dao.insertItems(items)
        clearCart()
        onDone(invId)
    }

    fun checkoutPurchase(supplier: String, paid: Double?, onDone: (Long) -> Unit = {}) = viewModelScope.launch {
        val total = cartTotal(false)
        val invId = dao.insertInvoice(Invoice(type = "PURCHASE", supplierName = supplier, total = total, paid = paid ?: total, rate = _rate.value))
        val items = cart.mapNotNull { (id, q) ->
            val p = products.value.firstOrNull { it.id == id } ?: return@mapNotNull null
            dao.adjustStock(id, q)
            val c = unitCost(p)
            if (c != p.costPrice) dao.setCost(id, c)
            InvoiceItem(invoiceId = invId, productId = id, productName = p.name, qty = q,
                unitPrice = toSyp(c, p.currency), unitCost = toSyp(c, p.currency))
        }
        dao.insertItems(items)
        clearCart()
        onDone(invId)
    }

    // ---- products / customers ----
    fun saveProduct(p: Product) = viewModelScope.launch { dao.upsertProduct(p) }
    fun deleteProduct(p: Product) = viewModelScope.launch { dao.deleteProduct(p) }
    fun addCustomer(name: String, phone: String) = viewModelScope.launch { dao.insertCustomer(Customer(name = name, phone = phone)) }
    fun addPayment(customerId: Long, amount: Double, note: String = "") =
        viewModelScope.launch { dao.insertPayment(Payment(customerId = customerId, amount = amount, note = note)) }

    fun invoicesOf(id: Long) = dao.invoicesOfCustomer(id)
    fun paymentsOf(id: Long) = dao.paymentsOfCustomer(id)

    // ---- reports ----
    /** range: 0 = today, 1 = last 7 days, 2 = a calendar month (monthOffset 0 = this month, 1 = last month ...) */
    fun bounds(range: Int, monthOffset: Int = 0): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return when (range) {
            0 -> cal.timeInMillis to System.currentTimeMillis()
            1 -> { cal.add(Calendar.DAY_OF_YEAR, -6); cal.timeInMillis to System.currentTimeMillis() }
            else -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -monthOffset)
                val from = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                from to cal.timeInMillis - 1
            }
        }
    }

    suspend fun summary(range: Int, monthOffset: Int = 0): DaySummary {
        val (from, to) = bounds(range, monthOffset)
        return DaySummary(dao.salesBetween(from, to), dao.purchasesBetween(from, to), dao.profitBetween(from, to))
    }

    // ---- backup ----
    fun export(uri: Uri, done: (Boolean) -> Unit) = viewModelScope.launch {
        try {
            val o = JSONObject()
            o.put("products", JSONArray().also { a -> dao.allProducts().forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("category", it.category).put("salePrice", it.salePrice).put("costPrice", it.costPrice).put("currency", it.currency).put("stock", it.stock).put("minStock", it.minStock).put("trackStock", it.trackStock)) } })
            o.put("customers", JSONArray().also { a -> dao.allCustomers().forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("phone", it.phone)) } })
            o.put("invoices", JSONArray().also { a -> dao.allInvoices().forEach { a.put(JSONObject().put("id", it.id).put("type", it.type).put("customerId", it.customerId ?: JSONObject.NULL).put("supplierName", it.supplierName).put("date", it.date).put("total", it.total).put("paid", it.paid).put("rate", it.rate)) } })
            o.put("items", JSONArray().also { a -> dao.allItems().forEach { a.put(JSONObject().put("id", it.id).put("invoiceId", it.invoiceId).put("productId", it.productId).put("productName", it.productName).put("qty", it.qty).put("unitPrice", it.unitPrice).put("unitCost", it.unitCost)) } })
            o.put("payments", JSONArray().also { a -> dao.allPayments().forEach { a.put(JSONObject().put("id", it.id).put("customerId", it.customerId).put("amount", it.amount).put("date", it.date).put("note", it.note)) } })
            getApplication<Application>().contentResolver.openOutputStream(uri)!!.use { it.write(o.toString().toByteArray()) }
            done(true)
        } catch (e: Exception) { done(false) }
    }

    fun restore(uri: Uri, done: (Boolean) -> Unit) = viewModelScope.launch {
        try {
            val txt = getApplication<Application>().contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
            val o = JSONObject(txt)
            fun <T> arr(k: String, f: (JSONObject) -> T): List<T> { val a = o.getJSONArray(k); return List(a.length()) { f(a.getJSONObject(it)) } }
            val ps = arr("products") { Product(it.getLong("id"), it.getString("name"), it.getString("category"), it.getDouble("salePrice"), it.getDouble("costPrice"), it.getString("currency"), it.getDouble("stock"), it.getDouble("minStock"), it.getBoolean("trackStock")) }
            val cs = arr("customers") { Customer(it.getLong("id"), it.getString("name"), it.getString("phone")) }
            val inv = arr("invoices") { Invoice(it.getLong("id"), it.getString("type"), if (it.isNull("customerId")) null else it.getLong("customerId"), it.getString("supplierName"), it.getLong("date"), it.getDouble("total"), it.getDouble("paid"), it.getDouble("rate")) }
            val its = arr("items") { InvoiceItem(it.getLong("id"), it.getLong("invoiceId"), it.getLong("productId"), it.getString("productName"), it.getDouble("qty"), it.getDouble("unitPrice"), it.getDouble("unitCost")) }
            val pay = arr("payments") { Payment(it.getLong("id"), it.getLong("customerId"), it.getDouble("amount"), it.getLong("date"), it.getString("note")) }
            dao.clearItems(); dao.clearInvoices(); dao.clearPayments(); dao.clearCustomers(); dao.clearProducts()
            dao.putProducts(ps); dao.putCustomers(cs); dao.putInvoices(inv); dao.putItems(its); dao.putPayments(pay)
            done(true)
        } catch (e: Exception) { done(false) }
    }
}

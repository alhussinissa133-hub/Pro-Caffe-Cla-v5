package com.cafe.accounts.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cafe.accounts.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun money(d: Double) = String.format(Locale.US, "%,.2f", d).removeSuffix(".00")
private fun qtyStr(d: Double) = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()
private fun date(l: Long) = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(l))
private fun catName(c: String) = Seed.categories.firstOrNull { it.first == c }?.second ?: c

@Composable
fun MainScreen(vm: AppViewModel) = BeanBackground { MainScaffold(vm) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(vm: AppViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("بيع", "شراء", "زبائن", "مخزون", "تقارير", "إعدادات")
    val icons = listOf("🛒", "📦", "👥", "🏷", "📊", "⚙")
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { Column { TopAppBar(title = { Text("☕ Pro Caffe", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)); TricolorStripe() } },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i },
                        icon = { Text(icons[i]) }, label = { Text(t, style = MaterialTheme.typography.labelSmall) })
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> TradeScreen(vm, sale = true)
                1 -> TradeScreen(vm, sale = false)
                2 -> CustomersScreen(vm)
                3 -> StockScreen(vm)
                4 -> ReportsScreen(vm)
                else -> SettingsScreen(vm)
            }
        }
    }
}

// ---------------- Sell / Buy ----------------
@Composable
fun TradeScreen(vm: AppViewModel, sale: Boolean) {
    val products by vm.products.collectAsState()
    val customers by vm.customers.collectAsState()
    val rate by vm.rate.collectAsState()
    var cat by remember { mutableStateOf("all") }
    var checkout by remember { mutableStateOf(false) }
    var savedInvoice by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(sale) { vm.clearCart() }
    val shown = products.filter { cat == "all" || it.category == cat }
    val total = vm.cartTotal(sale)

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(cat == "all", { cat = "all" }, { Text("الكل") })
                    Seed.categories.take(3).forEach { (k, n) -> FilterChip(cat == k, { cat = k }, { Text(n) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Seed.categories.drop(3).forEach { (k, n) -> FilterChip(cat == k, { cat = k }, { Text(n) }) }
                }
            }
            items(shown, key = { it.id }) { p ->
                val q = vm.cart[p.id] ?: 0.0
                val price = if (sale) p.salePrice else p.costPrice
                val unit = if (p.currency == "USD") "$" else "ل.س"
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.SemiBold)
                                Text("${money(price)} $unit" +
                                    (if (p.trackStock) "  •  المخزون: ${qtyStr(p.stock)}" else ""),
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            if (q > 0) {
                                OutlinedButton({ vm.add(p, -1.0) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(38.dp)) { Text("−") }
                                Text(qtyStr(q), Modifier.padding(horizontal = 10.dp), fontWeight = FontWeight.Bold)
                            }
                            Button({ vm.add(p) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(38.dp)) { Text("+") }
                        }
                        if (!sale && q > 0) {
                            OutlinedTextField(
                                value = vm.priceText[p.id] ?: (if (p.costPrice == 0.0) "" else qtyStr(p.costPrice)),
                                onValueChange = { vm.priceText[p.id] = it },
                                label = { Text("سعر الشراء للقطعة ($unit)") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
        Surface(shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الإجمالي: ${money(total)} ل.س", fontWeight = FontWeight.Bold)
                    if (rate > 0) Text("≈ ${money(total / rate)} $", style = MaterialTheme.typography.bodySmall)
                }
                TextButton({ vm.clearCart() }, enabled = vm.cart.isNotEmpty()) { Text("تفريغ") }
                Button({ checkout = true }, enabled = vm.cart.isNotEmpty()) { Text(if (sale) "إتمام البيع" else "تسجيل الشراء") }
            }
        }
    }

    if (checkout) CheckoutDialog(vm, sale, total, customers, onSaved = { savedInvoice = it }) { checkout = false }
    savedInvoice?.let { InvoiceDialog(vm, it) { savedInvoice = null } }
}

@Composable
fun CheckoutDialog(vm: AppViewModel, sale: Boolean, total: Double, customers: List<CustomerDebt>, onSaved: (Long) -> Unit, onClose: () -> Unit) {
    var customer by remember { mutableStateOf<CustomerDebt?>(null) }
    var supplier by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf("") }
    var pick by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (sale) "إتمام البيع" else "تسجيل الشراء") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الإجمالي: ${money(total)} ل.س", fontWeight = FontWeight.Bold)
                if (sale) {
                    OutlinedButton({ pick = true }, Modifier.fillMaxWidth()) { Text(customer?.name ?: "بيع نقدي (اختر زبون للدين)") }
                    DropdownMenu(pick, { pick = false }) {
                        DropdownMenuItem({ Text("بيع نقدي") }, { customer = null; pick = false })
                        customers.forEach { c -> DropdownMenuItem({ Text(c.name) }, { customer = c; pick = false }) }
                    }
                } else {
                    OutlinedTextField(supplier, { supplier = it }, label = { Text("المورّد (اختياري)") }, singleLine = true)
                }
                if (!sale || customer != null) {
                    OutlinedTextField(paid, { paid = it }, label = { Text("المبلغ المدفوع (فارغ = كامل)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    val p = paid.toDoubleOrNull() ?: total
                    if (p < total) Text("يُسجَّل دين: ${money(total - p)} ل.س", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button({
                val p = paid.toDoubleOrNull()
                if (sale) vm.checkoutSale(customer?.id, p, onSaved) else vm.checkoutPurchase(supplier, p, onSaved)
                onClose()
            }) { Text("تأكيد") }
        },
        dismissButton = { TextButton(onClose) { Text("إلغاء") } }
    )
}

/** Saved invoice: what was bought/sold, prices, total, paid, remaining. */
@Composable
fun InvoiceDialog(vm: AppViewModel, invoiceId: Long, onClose: () -> Unit) {
    val invoices by vm.invoices.collectAsState()
    val customers by vm.customers.collectAsState()
    val items by vm.itemsOf(invoiceId).collectAsState(emptyList())
    val inv = invoices.firstOrNull { it.id == invoiceId }
    AlertDialog(onClose,
        title = { Text(if (inv?.type == "PURCHASE") "فاتورة شراء #$invoiceId" else "فاتورة بيع #$invoiceId") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (inv == null) { Text("..."); return@Column }
                Text(date(inv.date), style = MaterialTheme.typography.bodySmall)
                if (inv.type == "SALE") Text("الزبون: " + (customers.firstOrNull { it.id == inv.customerId }?.name ?: "نقدي"))
                else if (inv.supplierName.isNotBlank()) Text("المورّد: ${inv.supplierName}")
                HorizontalDivider()
                items.forEach {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(it.productName, fontWeight = FontWeight.SemiBold)
                            Text("${qtyStr(it.qty)} × ${money(it.unitPrice)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(money(it.qty * it.unitPrice))
                    }
                }
                HorizontalDivider()
                Text("الإجمالي: ${money(inv.total)} ل.س", fontWeight = FontWeight.Bold)
                Text("المدفوع: ${money(inv.paid)} ل.س")
                if (inv.total - inv.paid > 0.5) Text("المتبقي: ${money(inv.total - inv.paid)} ل.س", color = MaterialTheme.colorScheme.error)
                if (inv.rate > 0) Text("سعر الدولار وقتها: ${money(inv.rate)}", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClose) { Text("إغلاق") } })
}

// ---------------- Customers / debts ----------------
@Composable
fun CustomersScreen(vm: AppViewModel) {
    val customers by vm.customers.collectAsState()
    var add by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<CustomerDebt?>(null) }
    val totalDebt = customers.sumOf { it.debt }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)) {
            Text("إجمالي الديون: ${money(totalDebt)} ل.س", Modifier.padding(14.dp), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Button({ add = true }, Modifier.fillMaxWidth()) { Text("+ زبون جديد") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
            items(customers, key = { it.id }) { c ->
                Card(Modifier.fillMaxWidth().clickable { selected = c }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(c.name, fontWeight = FontWeight.SemiBold); if (c.phone.isNotBlank()) Text(c.phone, style = MaterialTheme.typography.bodySmall) }
                        Text("${money(c.debt)} ل.س", color = if (c.debt > 0.5) MaterialTheme.colorScheme.error else Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    if (add) {
        var n by remember { mutableStateOf("") }; var ph by remember { mutableStateOf("") }
        AlertDialog({ add = false }, title = { Text("زبون جديد") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(n, { n = it }, label = { Text("الاسم") }, singleLine = true)
                OutlinedTextField(ph, { ph = it }, label = { Text("الهاتف") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone)) } },
            confirmButton = { Button({ if (n.isNotBlank()) { vm.addCustomer(n.trim(), ph.trim()); add = false } }) { Text("حفظ") } },
            dismissButton = { TextButton({ add = false }) { Text("إلغاء") } })
    }
    selected?.let { c ->
        val current = customers.firstOrNull { it.id == c.id } ?: c
        CustomerDetail(vm, current) { selected = null }
    }
}

@Composable
fun CustomerDetail(vm: AppViewModel, c: CustomerDebt, onClose: () -> Unit) {
    val invoices by vm.invoicesOf(c.id).collectAsState(emptyList())
    val payments by vm.paymentsOf(c.id).collectAsState(emptyList())
    var amount by remember { mutableStateOf("") }
    AlertDialog(onClose, title = { Text(c.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("الدين الحالي: ${money(c.debt)} ل.س", fontWeight = FontWeight.Bold)
                OutlinedTextField(amount, { amount = it }, label = { Text("دفعة جديدة") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number))
                Button({ amount.toDoubleOrNull()?.takeIf { it > 0 }?.let { vm.addPayment(c.id, it); amount = "" } }, Modifier.fillMaxWidth()) { Text("تسجيل دفعة") }
                HorizontalDivider()
                Text("كشف الحساب", fontWeight = FontWeight.SemiBold)
                val lines = invoices.map { Triple(it.date, "فاتورة", it.total - it.paid) } + payments.map { Triple(it.date, "دفعة", -it.amount) }
                lines.sortedByDescending { it.first }.forEach { (d, t, v) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text("$t • ${date(d)}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text(if (v >= 0) "+${money(v)}" else money(v), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClose) { Text("إغلاق") } })
}

// ---------------- Stock / products ----------------
@Composable
fun StockScreen(vm: AppViewModel) {
    val products by vm.products.collectAsState()
    var edit by remember { mutableStateOf<Product?>(null) }
    val low = products.filter { it.trackStock && it.stock <= it.minStock }
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        if (low.isNotEmpty()) Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0B2))) {
            Text("⚠ قارب على النفاد: " + low.joinToString("، ") { it.name }, Modifier.padding(12.dp))
        }
        Button({ edit = Product(name = "", category = "other", salePrice = 0.0, costPrice = 0.0) }, Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("+ صنف جديد") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(products, key = { it.id }) { p ->
                Card(Modifier.fillMaxWidth().clickable { edit = p }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.SemiBold)
                            Text("${catName(p.category)} • بيع ${money(p.salePrice)} • شراء ${money(p.costPrice)} ${if (p.currency == "USD") "$" else "ل.س"}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (p.trackStock) Text(qtyStr(p.stock), fontWeight = FontWeight.Bold,
                            color = if (p.stock <= p.minStock) MaterialTheme.colorScheme.error else Color.Unspecified)
                    }
                }
            }
        }
    }
    edit?.let { p -> ProductDialog(p, onSave = { vm.saveProduct(it); edit = null }, onDelete = { vm.deleteProduct(it); edit = null }, onClose = { edit = null }) }
}

@Composable
fun ProductDialog(p: Product, onSave: (Product) -> Unit, onDelete: (Product) -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf(p.name) }
    var cat by remember { mutableStateOf(p.category) }
    var sale by remember { mutableStateOf(if (p.salePrice == 0.0) "" else qtyStr(p.salePrice)) }
    var cost by remember { mutableStateOf(if (p.costPrice == 0.0) "" else qtyStr(p.costPrice)) }
    var cur by remember { mutableStateOf(p.currency) }
    var stock by remember { mutableStateOf(qtyStr(p.stock)) }
    var min by remember { mutableStateOf(qtyStr(p.minStock)) }
    var track by remember { mutableStateOf(p.trackStock) }
    val num = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
    AlertDialog(onClose, title = { Text(if (p.id == 0L) "صنف جديد" else "تعديل الصنف") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("الاسم") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Seed.categories.take(3).forEach { (k, n) -> FilterChip(cat == k, { cat = k }, { Text(n, style = MaterialTheme.typography.labelSmall) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Seed.categories.drop(3).forEach { (k, n) -> FilterChip(cat == k, { cat = k }, { Text(n, style = MaterialTheme.typography.labelSmall) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(cur == "SYP", { cur = "SYP" }, { Text("ليرة") })
                    FilterChip(cur == "USD", { cur = "USD" }, { Text("دولار") })
                }
                OutlinedTextField(sale, { sale = it }, label = { Text("سعر البيع") }, singleLine = true, keyboardOptions = num)
                OutlinedTextField(cost, { cost = it }, label = { Text("سعر الشراء/التكلفة") }, singleLine = true, keyboardOptions = num)
                Row(verticalAlignment = Alignment.CenterVertically) { Switch(track, { track = it }); Text("  تتبّع المخزون") }
                if (track) {
                    OutlinedTextField(stock, { stock = it }, label = { Text("الكمية الحالية") }, singleLine = true, keyboardOptions = num)
                    OutlinedTextField(min, { min = it }, label = { Text("حد التنبيه") }, singleLine = true, keyboardOptions = num)
                }
            }
        },
        confirmButton = {
            Button({
                if (name.isNotBlank()) onSave(p.copy(name = name.trim(), category = cat, salePrice = sale.toDoubleOrNull() ?: 0.0,
                    costPrice = cost.toDoubleOrNull() ?: 0.0, currency = cur, stock = stock.toDoubleOrNull() ?: 0.0,
                    minStock = min.toDoubleOrNull() ?: 0.0, trackStock = track))
            }) { Text("حفظ") }
        },
        dismissButton = {
            Row { if (p.id != 0L) TextButton({ onDelete(p) }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
                TextButton(onClose) { Text("إلغاء") } }
        })
}

// ---------------- Reports ----------------
private val monthNames = listOf("كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران", "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول")

@Composable
fun ReportsScreen(vm: AppViewModel) {
    var range by remember { mutableIntStateOf(0) }
    var monthOffset by remember { mutableIntStateOf(0) }
    var kind by remember { mutableStateOf("ALL") }
    var open by remember { mutableStateOf<Long?>(null) }
    var s by remember { mutableStateOf(DaySummary(0.0, 0.0, 0.0)) }
    val invoices by vm.invoices.collectAsState()
    LaunchedEffect(range, monthOffset, invoices) { s = vm.summary(range, monthOffset) }
    val (from, to) = vm.bounds(range, monthOffset)
    val shown = invoices.filter { it.date in from..to && (kind == "ALL" || it.type == kind) }
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = from }
    val monthLabel = "${monthNames[cal.get(java.util.Calendar.MONTH)]} ${cal.get(java.util.Calendar.YEAR)}"
    LazyColumn(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("اليوم", "7 أيام", "شهر محدد").forEachIndexed { i, t -> FilterChip(range == i, { range = i }, { Text(t) }) }
            }
        }
        if (range == 2) item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton({ monthOffset++ }) { Text("▶ أقدم") }
                Text(monthLabel, fontWeight = FontWeight.Bold)
                OutlinedButton({ if (monthOffset > 0) monthOffset-- }, enabled = monthOffset > 0) { Text("أحدث ◀") }
            }
        }
        item { StatCard("المبيعات", s.sales) }
        item { StatCard("المشتريات", s.purchases) }
        item { StatCard("صافي الربح (مبيعات − تكلفة الأصناف)", s.profit) }
        item {
            Text("الفواتير (اضغط على فاتورة لعرض التفاصيل)", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(kind == "ALL", { kind = "ALL" }, { Text("الكل") })
                FilterChip(kind == "SALE", { kind = "SALE" }, { Text("بيع") })
                FilterChip(kind == "PURCHASE", { kind = "PURCHASE" }, { Text("شراء") })
            }
        }
        if (shown.isEmpty()) item { Text("لا فواتير بهذه الفترة", style = MaterialTheme.typography.bodySmall) }
        items(shown, key = { it.id }) { i ->
            Card(Modifier.fillMaxWidth().clickable { open = i.id }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${if (i.type == "SALE") "بيع" else "شراء"} #${i.id}", fontWeight = FontWeight.SemiBold)
                        Text(date(i.date), style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${money(i.total)} ل.س", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    open?.let { InvoiceDialog(vm, it) { open = null } }
}

@Composable
fun StatCard(label: String, v: Double) = Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text("${money(v)} ل.س", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

// ---------------- Settings ----------------
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val rate by vm.rate.collectAsState()
    val info by vm.rateInfo.collectAsState()
    val fetching by vm.fetching.collectAsState()
    val cands by vm.candidates.collectAsState()
    var manual by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        u?.let { vm.export(it) { ok -> msg = if (ok) "تم حفظ النسخة الاحتياطية" else "فشل الحفظ" } } }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        u?.let { vm.restore(it) { ok -> msg = if (ok) "تمت الاستعادة" else "ملف غير صالح" } } }
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("سعر الدولار", fontWeight = FontWeight.Bold)
                Text(if (rate > 0) "1$ = ${money(rate)} ل.س" else "غير محدد", style = MaterialTheme.typography.titleLarge)
                Text(info, style = MaterialTheme.typography.bodySmall)
                if (cands.isNotEmpty()) {
                    Text("أرقام وجدتها بالموقع — اختر الصحيح:", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        cands.forEach { c -> AssistChip({ vm.setRate(c, "اختيار يدوي") }, { Text(money(c)) }) }
                    }
                }
                if (rate > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ vm.setRate(rate / 100, "÷100") }) { Text("÷ 100 (ليرة جديدة)") }
                    OutlinedButton({ vm.setRate(rate * 100, "×100") }) { Text("× 100 (ليرة قديمة)") }
                }
                Button({ vm.refreshRate() }, Modifier.fillMaxWidth(), enabled = !fetching) { Text(if (fetching) "جاري التحديث..." else "تحديث من موقع الليرة اليوم") }
                OutlinedTextField(manual, { manual = it }, label = { Text("إدخال السعر يدوياً") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedButton({ manual.toDoubleOrNull()?.takeIf { it > 0 }?.let { vm.setRate(it); manual = "" } }, Modifier.fillMaxWidth()) { Text("حفظ السعر") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("النسخ الاحتياطي", fontWeight = FontWeight.Bold)
                Button({ exp.launch("procaffe-backup.json") }, Modifier.fillMaxWidth()) { Text("حفظ نسخة احتياطية") }
                OutlinedButton({ imp.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth()) { Text("استعادة نسخة (تستبدل البيانات الحالية)") }
                if (msg.isNotBlank()) Text(msg)
            }
        }
        Text("Pro Caffe • Hussin Issa", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

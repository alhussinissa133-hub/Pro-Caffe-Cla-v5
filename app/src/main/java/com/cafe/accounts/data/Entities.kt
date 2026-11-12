package com.cafe.accounts.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** category: coffee, sachet, snack, cold, tobacco, other */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val salePrice: Double,
    val costPrice: Double,
    /** "SYP" or "USD" */
    val currency: String = "SYP",
    val stock: Double = 0.0,
    val minStock: Double = 0.0,
    /** drinks made to order (espresso, cappuccino...) don't track stock */
    val trackStock: Boolean = true
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = ""
)

/** type: SALE or PURCHASE. All totals stored in SYP (using the rate at the time). */
@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val customerId: Long? = null,
    val supplierName: String = "",
    val date: Long = System.currentTimeMillis(),
    val total: Double,
    val paid: Double,
    val rate: Double
)

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long,
    val productName: String,
    val qty: Double,
    /** unit price in SYP at time of sale */
    val unitPrice: Double,
    /** unit cost in SYP at time of sale (for profit) */
    val unitCost: Double
)

/** A customer paying off debt. */
@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)

data class CustomerDebt(
    val id: Long,
    val name: String,
    val phone: String,
    val debt: Double
)

data class DaySummary(
    val sales: Double,
    val purchases: Double,
    val profit: Double
)

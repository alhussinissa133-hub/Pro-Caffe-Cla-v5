package com.cafe.accounts.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // products
    @Query("SELECT * FROM products ORDER BY category, name")
    fun products(): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun productCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(p: Product): Long

    @Insert
    suspend fun insertProducts(list: List<Product>)

    @Delete
    suspend fun deleteProduct(p: Product)

    @Query("UPDATE products SET stock = stock + :delta WHERE id = :id AND trackStock = 1")
    suspend fun adjustStock(id: Long, delta: Double)

    // customers
    @Insert
    suspend fun insertCustomer(c: Customer): Long

    @Delete
    suspend fun deleteCustomer(c: Customer)

    @Query(
        """
        SELECT c.id AS id, c.name AS name, c.phone AS phone,
          COALESCE((SELECT SUM(total - paid) FROM invoices WHERE customerId = c.id AND type = 'SALE'), 0)
          - COALESCE((SELECT SUM(amount) FROM payments WHERE customerId = c.id), 0) AS debt
        FROM customers c ORDER BY c.name
        """
    )
    fun customersWithDebt(): Flow<List<CustomerDebt>>

    // invoices
    @Insert
    suspend fun insertInvoice(i: Invoice): Long

    @Insert
    suspend fun insertItems(items: List<InvoiceItem>)

    @Insert
    suspend fun insertPayment(p: Payment)

    @Query("SELECT * FROM invoices WHERE customerId = :id ORDER BY date DESC")
    fun invoicesOfCustomer(id: Long): Flow<List<Invoice>>

    @Query("SELECT * FROM payments WHERE customerId = :id ORDER BY date DESC")
    fun paymentsOfCustomer(id: Long): Flow<List<Payment>>

    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun recentInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :id")
    fun itemsOf(id: Long): Flow<List<InvoiceItem>>

    @Query("UPDATE products SET costPrice = :c WHERE id = :id")
    suspend fun setCost(id: Long, c: Double)

    // reports
    @Query("SELECT COALESCE(SUM(total),0) FROM invoices WHERE type='SALE' AND date BETWEEN :from AND :to")
    suspend fun salesBetween(from: Long, to: Long): Double

    @Query("SELECT COALESCE(SUM(total),0) FROM invoices WHERE type='PURCHASE' AND date BETWEEN :from AND :to")
    suspend fun purchasesBetween(from: Long, to: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM((it.unitPrice - it.unitCost) * it.qty), 0)
        FROM invoice_items it JOIN invoices i ON i.id = it.invoiceId
        WHERE i.type='SALE' AND i.date BETWEEN :from AND :to
        """
    )
    suspend fun profitBetween(from: Long, to: Long): Double

    // backup
    @Query("SELECT * FROM products") suspend fun allProducts(): List<Product>
    @Query("SELECT * FROM customers") suspend fun allCustomers(): List<Customer>
    @Query("SELECT * FROM invoices") suspend fun allInvoices(): List<Invoice>
    @Query("SELECT * FROM invoice_items") suspend fun allItems(): List<InvoiceItem>
    @Query("SELECT * FROM payments") suspend fun allPayments(): List<Payment>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putProducts(l: List<Product>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCustomers(l: List<Customer>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putInvoices(l: List<Invoice>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putItems(l: List<InvoiceItem>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putPayments(l: List<Payment>)

    @Query("DELETE FROM products") suspend fun clearProducts()
    @Query("DELETE FROM customers") suspend fun clearCustomers()
    @Query("DELETE FROM invoices") suspend fun clearInvoices()
    @Query("DELETE FROM invoice_items") suspend fun clearItems()
    @Query("DELETE FROM payments") suspend fun clearPayments()
}

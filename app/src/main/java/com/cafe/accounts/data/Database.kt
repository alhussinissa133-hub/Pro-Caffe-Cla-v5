package com.cafe.accounts.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Product::class, Customer::class, Invoice::class, InvoiceItem::class, Payment::class],
    version = 1,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "procaffe.db")
                .build().also { inst = it }
        }
    }
}

/** Starter catalogue — prices are 0 so the owner fills them in. */
object Seed {
    private fun p(name: String, cat: String, track: Boolean = true) =
        Product(name = name, category = cat, salePrice = 0.0, costPrice = 0.0, trackStock = track)

    fun products(): List<Product> = listOf(
        p("قهوة إسبريسو", "coffee", false),
        p("قهوة نسكافيه", "coffee", false),
        p("كابتشينو", "coffee", false),
        p("ميلو", "coffee", false),
        p("زهورات", "coffee", false),
        p("ظرف كابتشينو", "sachet"),
        p("ظرف ماكياتو", "sachet"),
        p("دخان - نوع 1", "tobacco"),
        p("دخان - نوع 2", "tobacco"),
        p("دخان - نوع 3", "tobacco"),
        p("شيبس", "snack"),
        p("بسكوت", "snack"),
        p("كولا", "cold"),
        p("مشروب طاقة", "cold"),
        p("عصير", "cold"),
        p("قداحة", "other")
    )

    val categories = listOf(
        "coffee" to "مشروبات ساخنة",
        "sachet" to "ظروف",
        "tobacco" to "دخان",
        "snack" to "سناكات",
        "cold" to "مشروبات باردة",
        "other" to "أخرى"
    )
}

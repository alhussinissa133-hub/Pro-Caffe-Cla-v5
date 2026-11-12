package com.cafe.accounts.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads the USD/SYP rate from "الليرة اليوم" (sp-today.com).
 * The site has no documented API, so this reads the page text and looks for the
 * numbers next to "USD" / "دولار". Returns the rate or an error message.
 */
object RateFetcher {
    data class Result(val rate: Double?, val error: String = "", val candidates: List<Double> = emptyList())

    private val urls = listOf("https://sp-today.com/", "https://sp-today.com/en", "https://www.sp-today.com/", "https://sp-today.com/ar")
    private val numRe = Regex("""\d{1,3}(?:,\d{3})+(?:\.\d+)?|\d{2,6}(?:\.\d+)?""")

    suspend fun fetch(): Result = withContext(Dispatchers.IO) {
        var err = ""
        for (u in urls) {
            try {
                val c = URL(u).openConnection() as HttpURLConnection
                c.connectTimeout = 10000
                c.readTimeout = 10000
                c.instanceFollowRedirects = true
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                c.setRequestProperty("Accept-Language", "ar,en;q=0.8")
                if (c.responseCode != 200) { err = "HTTP ${c.responseCode}"; continue }
                val html = c.inputStream.bufferedReader().use { it.readText() }
                val r = parse(html)
                if (r != null) return@withContext Result(r, "", candidates(html))
                err = "لم أجد السعر داخل الصفحة"
            } catch (e: Exception) {
                err = e.javaClass.simpleName + (e.message?.let { ": $it" } ?: "")
            }
        }
        Result(null, err)
    }

    internal fun parse(html: String): Double? {
        val text = html.replace(Regex("<script[\\s\\S]*?</script>"), " ")
        for (key in listOf("USD", "دولار", "Dollar", "dollar")) {
            var from = 0
            while (true) {
                val i = text.indexOf(key, from)
                if (i < 0) break
                from = i + key.length
                val chunk = text.substring(from, minOf(text.length, from + 400)).replace(Regex("<[^>]+>"), " ")
                val nums = numRe.findAll(chunk).mapNotNull { it.value.replace(",", "").toDoubleOrNull() }
                    .filter { it in 50.0..200000.0 }.take(2).toList()
                if (nums.size == 2 && nums[1] / nums[0] in 0.9..1.1) return (nums[0] + nums[1]) / 2
                if (nums.isNotEmpty()) return nums[0]
            }
        }
        return null
    }

    /** Other numbers found near "USD" so the user can pick the right one if the guess is wrong. */
    internal fun candidates(html: String): List<Double> {
        val text = html.replace(Regex("<script[\\s\\S]*?</script>"), " ")
        val out = LinkedHashSet<Double>()
        for (key in listOf("USD", "دولار")) {
            var from = 0
            var n = 0
            while (n < 6) {
                val i = text.indexOf(key, from)
                if (i < 0) break
                from = i + key.length; n++
                val chunk = text.substring(from, minOf(text.length, from + 300)).replace(Regex("<[^>]+>"), " ")
                numRe.findAll(chunk).mapNotNull { it.value.replace(",", "").toDoubleOrNull() }
                    .filter { it in 50.0..200000.0 }.take(2).forEach { out.add(it) }
            }
        }
        return out.take(8)
    }
}

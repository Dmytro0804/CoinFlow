package com.example.coinflow.Activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coinflow.Helper.PrefsManager
import com.example.coinflow.R
import com.example.coinflow.databinding.ActivityStatisticsBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var prefs: PrefsManager
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        prefs = PrefsManager(this)

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        fetchLivePrices()
    }

    private fun fetchLivePrices() {
        val request = Request.Builder().url("https://api.binance.com/api/v3/ticker/24hr").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { refreshAllUI() } }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val jsonBody = response.body?.string() ?: return
                    if (!jsonBody.trim().startsWith("[")) return

                    val jsonArray = JSONArray(jsonBody)
                    val myCoins = listOf("BTC", "ETH", "BNB", "SOL", "XRP", "DOGE", "SHIB", "TRX")

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val symbol = obj.optString("symbol", "")

                        for (coin in myCoins) {
                            if (symbol == "${coin}USDT") {
                                val price = obj.optString("lastPrice", "0").toDoubleOrNull() ?: 0.0
                                val percentChange = obj.optString("priceChangePercent", "0").toDoubleOrNull() ?: 0.0

                                if (price > 0) {
                                    prefs.saveLivePrice(coin, price)
                                    prefs.saveLiveChangePercent(coin, percentChange)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    runOnUiThread { refreshAllUI() }
                }
            }
        })
    }

    private fun refreshAllUI() {
        loadTradingStats()
        loadFavoriteCoin()
        loadWalletPieChart()
        updateProfitUI()
    }

    private fun loadTradingStats() {
        val stats = prefs.getTradingStats()
        val boughtUsd = stats["totalBought"] ?: 0.0
        val soldUsd = stats["totalSold"] ?: 0.0
        val maxBuyUsd = stats["maxBuy"] ?: 0.0
        val maxSellUsd = stats["maxSell"] ?: 0.0
        val currentExchangeRate = prefs.getLivePrice("CURRENCY_SALE", 40.50)

        binding.totalBoughtTxt.text = String.format(Locale.US, "%,.2f $\n%,.2f ₴", boughtUsd, boughtUsd * currentExchangeRate)
        binding.totalSoldTxt.text = String.format(Locale.US, "%,.2f $\n%,.2f ₴", soldUsd, soldUsd * currentExchangeRate)
        binding.maxBuyTxt.text = String.format(Locale.US, "%,.2f $\n%,.2f ₴", maxBuyUsd, maxBuyUsd * currentExchangeRate)
        binding.maxSellTxt.text = String.format(Locale.US, "%,.2f $\n%,.2f ₴", maxSellUsd, maxSellUsd * currentExchangeRate)
    }

    private fun loadFavoriteCoin() {
        val favSymbol = prefs.getFavoriteCoin()
        if (favSymbol == "NONE") {
            binding.favoriteCoinTxt.text = "Ще немає трейдів"; binding.favoriteCoinLogo.visibility = View.GONE
        } else {
            binding.favoriteCoinLogo.visibility = View.VISIBLE
            val (name, iconRes) = when (favSymbol) {
                "BTC" -> Pair("Bitcoin", R.drawable.bitcoin); "ETH" -> Pair("Ethereum", R.drawable.etherium)
                "BNB" -> Pair("Binance Coin", R.drawable.binance); "SOL" -> Pair("Solana", R.drawable.solana)
                "XRP" -> Pair("Ripple", R.drawable.ripple); "DOGE" -> Pair("Dogecoin", R.drawable.dogecoin)
                "SHIB" -> Pair("Shiba Inu", R.drawable.shiba); "TRX" -> Pair("Tron", R.drawable.trox)
                "USDT" -> Pair("Tether", R.drawable.tether)
                "CFC" -> Pair("CoinFlow Coin", R.drawable.bitcoin) // Лого CFC
                else -> Pair(favSymbol, R.drawable.bitcoin)
            }
            binding.favoriteCoinTxt.text = name; binding.favoriteCoinLogo.setImageResource(iconRes)
        }
    }

    private fun loadWalletPieChart() {
        val entries = ArrayList<PieEntry>(); val colors = ArrayList<Int>()
        val coins = mutableListOf("BTC", "ETH", "BNB", "SOL", "XRP", "DOGE", "SHIB", "TRX", "USDT")

        if (prefs.getCurrentPlan() != "BRONZE") coins.add("CFC")

        val coinNames = mapOf("BTC" to "Bitcoin", "ETH" to "Ethereum", "BNB" to "Binance Coin", "SOL" to "Solana", "XRP" to "Ripple", "DOGE" to "Dogecoin", "SHIB" to "Shiba Inu", "TRX" to "Tron", "USDT" to "Tether", "CFC" to "CoinFlow Coin")
        val coinColors = mapOf("BTC" to Color.parseColor("#FF9800"), "ETH" to Color.parseColor("#E0E0E0"), "BNB" to Color.parseColor("#FFEB3B"), "SOL" to Color.parseColor("#00FFA3"), "XRP" to Color.parseColor("#2196F3"), "DOGE" to Color.parseColor("#FFC107"), "SHIB" to Color.parseColor("#FFCCBC"), "TRX" to Color.parseColor("#F44336"), "USDT" to Color.parseColor("#009688"), "CFC" to Color.parseColor("#9C27B0")) // Фіолетовий для CFC
        val defaultPrices = mapOf("BTC" to 0.0, "ETH" to 0.0, "BNB" to 0.0, "SOL" to 0.0, "XRP" to 0.0, "DOGE" to 0.0, "SHIB" to 0.0, "TRX" to 0.0, "USDT" to 1.0, "CFC" to 2.50)

        for (coin in coins) {
            val balance = prefs.getCryptoBalance(coin)
            if (balance > 0) {
                val price = if (coin == "CFC") 2.50 else prefs.getLivePrice(coin, defaultPrices[coin] ?: 0.0)
                entries.add(PieEntry((balance * price).toFloat(), coinNames[coin]))
                colors.add(coinColors[coin] ?: Color.GRAY)
            }
        }
        if (entries.isEmpty()) { entries.add(PieEntry(1f, "Немає крипти")); colors.add(Color.parseColor("#333333")) }

        val dataSet = PieDataSet(entries, "").apply { this.colors = colors; setDrawValues(false) }
        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.description.isEnabled = false
        binding.pieChart.legend.apply { textColor = Color.WHITE; textSize = 12f; isWordWrapEnabled = true }
        binding.pieChart.setDrawEntryLabels(false); binding.pieChart.setHoleColor(Color.TRANSPARENT)
        binding.pieChart.holeRadius = 50f; binding.pieChart.animateY(1400); binding.pieChart.invalidate()
    }

    private fun updateProfitUI() {
        if (prefs.getCurrentPlan() == "BRONZE") {
            binding.unlockedProfitBlock.visibility = View.GONE; binding.lockedProfitBlock.visibility = View.VISIBLE
        } else {
            binding.unlockedProfitBlock.visibility = View.VISIBLE; binding.lockedProfitBlock.visibility = View.GONE
            setupProfitChartAppearance(); calculateAndDrawRealProfit()
        }
    }

    private fun setupProfitChartAppearance() {
        binding.profitChart.description.isEnabled = false; binding.profitChart.legend.isEnabled = false
        binding.profitChart.setDrawGridBackground(false); binding.profitChart.setTouchEnabled(true)
        binding.profitChart.isDragEnabled = true; binding.profitChart.setScaleEnabled(false)
        binding.profitChart.xAxis.apply { position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM; setDrawGridLines(false); setDrawLabels(false) }
        binding.profitChart.axisLeft.apply { textColor = Color.parseColor("#888888"); setDrawGridLines(true); gridColor = Color.parseColor("#222222") }
        binding.profitChart.axisRight.isEnabled = false
    }

    private fun calculateAndDrawRealProfit() {
        val stats = prefs.getTradingStats()
        val totalBought = stats["totalBought"] ?: 0.0
        val totalSold = stats["totalSold"] ?: 0.0
        var currentCryptoPortfolioValue = 0.0

        val coins = mutableListOf("BTC", "ETH", "BNB", "SOL", "XRP", "DOGE", "SHIB", "TRX", "USDT")
        if (prefs.getCurrentPlan() != "BRONZE") coins.add("CFC")

        for (coin in coins) {
            val balance = prefs.getCryptoBalance(coin)
            if (balance > 0) {
                val price = if (coin == "CFC") 2.50 else prefs.getLivePrice(coin, 0.0)
                currentCryptoPortfolioValue += balance * price
            }
        }

        val actualProfit = totalSold - totalBought + currentCryptoPortfolioValue
        val entries = ArrayList<com.github.mikephil.charting.data.Entry>()

        entries.add(com.github.mikephil.charting.data.Entry(0f, 0f))
        entries.add(com.github.mikephil.charting.data.Entry(1f, actualProfit.toFloat()))

        val isPositive = actualProfit >= 0
        val color = if (isPositive) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")

        val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, "Profit").apply {
            this.color = color; lineWidth = 3f; setDrawCircles(true); setCircleColor(color); circleRadius = 5f; setDrawValues(false)
            mode = com.github.mikephil.charting.data.LineDataSet.Mode.LINEAR; setDrawFilled(true); fillColor = color; fillAlpha = 50
        }
        binding.profitChart.data = com.github.mikephil.charting.data.LineData(dataSet)
        binding.profitChart.animateX(800); binding.profitChart.invalidate()

        binding.totalProfitUsdTxt.text = String.format(Locale.US, if (isPositive) "+ %,.2f $" else "- %,.2f $", Math.abs(actualProfit))
        binding.totalProfitUsdTxt.setTextColor(color)

        val profitPercent = if (totalBought > 0) (actualProfit / totalBought) * 100 else 0.0
        binding.totalProfitPercentTxt.text = String.format(Locale.US, if (isPositive) "▲ %,.2f%%" else "▼ %,.2f%%", Math.abs(profitPercent))
        binding.totalProfitPercentTxt.setTextColor(color)

        val profitUah = actualProfit * prefs.getLivePrice("CURRENCY_SALE", 40.50)
        binding.totalProfitUahTxt.text = String.format(Locale.US, if (isPositive) "≈ + %,.2f ₴" else "≈ - %,.2f ₴", Math.abs(profitUah))
    }

    private fun showProfilePasswordDialog() {
        val prefs = PrefsManager(this)
        val savedPassword = prefs.getCurrentPassword()
        val textInputLayout = com.google.android.material.textfield.TextInputLayout(this).apply {
            endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
            setEndIconTintList(android.content.res.ColorStateList.valueOf(Color.LTGRAY))
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_NONE
            isHintEnabled = false
        }
        val editText = com.google.android.material.textfield.TextInputEditText(textInputLayout.context).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Ваш пароль"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.WHITE)
            textSize = 18f
            setPadding(0, 32, 0, 32)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
        }
        textInputLayout.addView(editText)
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(64, 32, 64, 16)
            addView(textInputLayout)
        }
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Безпека").setMessage("Введіть пароль від акаунту для доступу до Профілю.").setView(layout)
            .setPositiveButton("Увійти") { _, _ ->
                if (editText.text.toString() == savedPassword) {
                    startActivity(Intent(this, ProfileActivity::class.java))
                } else Toast.makeText(this, "Невірний пароль!", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Скасувати", null).create()

        dialog.show()
        val solidBackground = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#1C1F26")); cornerRadius = 32f
        }
        dialog.window?.setBackgroundDrawable(solidBackground)
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#4CAF50"))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#F44336"))
    }

    private fun setupBottomNavigation() {
        binding.navWalletImg.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)); finish() }
        binding.navWalletTxt.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)); finish() }
        binding.navFuturesImg.setOnClickListener { startActivity(Intent(this, GraphActivity::class.java)); finish() }
        binding.navFuturesTxt.setOnClickListener { startActivity(Intent(this, GraphActivity::class.java)); finish() }
        binding.navTradeImg.setOnClickListener { startActivity(Intent(this, AccountsActivity::class.java)); finish() }
        binding.navTradeTxt.setOnClickListener { startActivity(Intent(this, AccountsActivity::class.java)); finish() }
        binding.navProfileImg.setOnClickListener { showProfilePasswordDialog() }
        binding.navProfileTxt.setOnClickListener { showProfilePasswordDialog() }
    }
}
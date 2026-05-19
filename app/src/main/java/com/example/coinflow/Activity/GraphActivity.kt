package com.example.coinflow.Activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coinflow.Helper.PrefsManager
import com.example.coinflow.R
import com.example.coinflow.databinding.ActivityGraphBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.util.Locale
import kotlin.random.Random

class GraphActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGraphBinding
    private val client = OkHttpClient()

    private val basePrices = mapOf("BTC" to 0.0, "ETH" to 0.0, "BNB" to 0.0, "SOL" to 0.0, "XRP" to 0.0, "DOGE" to 0.0, "SHIB" to 0.0, "TRX" to 0.0, "USDT" to 1.0, "CFC" to 2.50)
    private var coinsList = mutableListOf("Bitcoin (BTC)", "Ethereum (ETH)", "Binance Coin (BNB)", "Solana (SOL)", "Ripple (XRP)", "Dogecoin (DOGE)", "Shiba Inu (SHIB)", "Tron (TRX)", "Tether (USDT)")

    private var currentSymbol = "BTC"
    private var currentBasePrice = 0.0
    private var currentPointsCount = 60
    private lateinit var timeFilters: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGraphBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        binding.backBtn.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)); finish() }

        val currentPlan = PrefsManager(this).getCurrentPlan()
        if (currentPlan != "BRONZE" && !coinsList.contains("CoinFlow Coin (CFC)")) {
            coinsList.add("CoinFlow Coin (CFC)")
        }

        setupBottomNavigation()
        setupSpinner()
        setupChartAppearance()
        setupTimeFilters()
    }

    override fun onResume() {
        super.onResume()
        fetchLivePrices()
    }

    private fun fetchLivePrices() {
        val request = Request.Builder().url("https://api.binance.com/api/v3/ticker/24hr").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { refreshUIWithCurrentData(currentPointsCount) } }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val jsonBody = response.body?.string() ?: return
                    if (!jsonBody.trim().startsWith("[")) return

                    val jsonArray = JSONArray(jsonBody)
                    val prefs = PrefsManager(this@GraphActivity)
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
                } catch (e: Exception) { e.printStackTrace() }
                finally { runOnUiThread { refreshUIWithCurrentData(currentPointsCount) } }
            }
        })
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, R.layout.spinner_item, coinsList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.cryptoSelectorSpin.adapter = adapter

        binding.cryptoSelectorSpin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSymbol = coinsList[position].substringAfter("(").substringBefore(")")
                currentBasePrice = basePrices[currentSymbol] ?: 0.0
                updateLogo(); currentPointsCount = 60
                timeFilters.forEach { it.setBackgroundResource(0) }
                binding.filter1m.setBackgroundResource(R.drawable.blue_rounded_bg)
                refreshUIWithCurrentData(currentPointsCount)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateLogo() {
        val resId = when (currentSymbol) {
            "BTC" -> R.drawable.bitcoin; "ETH" -> R.drawable.etherium; "BNB" -> R.drawable.binance; "SOL" -> R.drawable.solana
            "XRP" -> R.drawable.ripple; "DOGE" -> R.drawable.dogecoin; "SHIB" -> R.drawable.shiba; "TRX" -> R.drawable.trox; "USDT" -> R.drawable.tether
            "CFC" -> R.drawable.bitcoin // Заміни на лого CFC, якщо є
            else -> R.drawable.bitcoin
        }
        binding.selectedCryptoLogo.setImageResource(resId)
    }

    private fun setupChartAppearance() {
        binding.priceChart.description.isEnabled = false; binding.priceChart.legend.isEnabled = false
        binding.priceChart.setDrawGridBackground(false); binding.priceChart.setTouchEnabled(true)
        binding.priceChart.isDragEnabled = true; binding.priceChart.setScaleEnabled(false)
        binding.priceChart.xAxis.apply { position = XAxis.XAxisPosition.BOTTOM; setDrawGridLines(false); textColor = Color.parseColor("#888888"); setDrawLabels(false) }
        binding.priceChart.axisLeft.apply { textColor = Color.parseColor("#888888"); setDrawGridLines(true); gridColor = Color.parseColor("#222222") }
        binding.priceChart.axisRight.isEnabled = false
    }

    private fun setupTimeFilters() {
        timeFilters = listOf(binding.filter7d, binding.filter1m, binding.filter3m, binding.filter6m, binding.filter1y, binding.filterAll)
        val map = mapOf(binding.filter7d to 7, binding.filter1m to 30, binding.filter3m to 90, binding.filter6m to 180, binding.filter1y to 365, binding.filterAll to 500)
        timeFilters.forEach { filter ->
            filter.setOnClickListener { clickedView ->
                timeFilters.forEach { it.setBackgroundResource(0) }
                clickedView.setBackgroundResource(R.drawable.blue_rounded_bg)
                currentPointsCount = map[clickedView] ?: 60; generateChartData(currentPointsCount)
            }
        }
    }

    private fun refreshUIWithCurrentData(chartPoints: Int) {
        val prefs = PrefsManager(this)

        if (currentSymbol == "CFC") {
            // Для CFC жорстко ставимо 2.50 і відсоток 0.0
            binding.currentPriceTxt.text = "2.50 $"
            updateStatBlock(binding.change24hPercentTxt, binding.change24hUsdTxt, 2.50, 0.0)
            updateStatBlock(binding.change7dPercentTxt, binding.change7dUsdTxt, 2.50, 0.0)
            updateStatBlock(binding.change30dPercentTxt, binding.change30dUsdTxt, 2.50, 0.0)
            updateStatBlock(binding.change1yPercentTxt, binding.change1yUsdTxt, 2.50, 0.0)
            generateChartData(chartPoints)
            return
        }

        if (!isSessionInitialized.contains(currentSymbol)) {
            prefs.saveLiveChangePercent(currentSymbol, Random.nextDouble(-5.0, 5.0))
            statPercentCache["${currentSymbol}_7d"] = Random.nextDouble(-15.0, 15.0); statPercentCache["${currentSymbol}_30d"] = Random.nextDouble(-30.0, 30.0); statPercentCache["${currentSymbol}_1y"] = Random.nextDouble(-80.0, 80.0)
            isSessionInitialized.add(currentSymbol)
        }

        val livePrice = prefs.getLivePrice(currentSymbol, currentBasePrice)
        val percent24h = prefs.getLiveChangePercent(currentSymbol, 0.0)

        val formatAmount = if (currentSymbol == "SHIB" || currentSymbol == "DOGE" || currentSymbol == "XRP") "%.4f" else "%,.2f"
        binding.currentPriceTxt.text = "${String.format(Locale.US, formatAmount, livePrice)} $"

        updateStatBlock(binding.change24hPercentTxt, binding.change24hUsdTxt, livePrice, percent24h)
        updateStatBlock(binding.change7dPercentTxt, binding.change7dUsdTxt, livePrice, statPercentCache["${currentSymbol}_7d"] ?: 0.0)
        updateStatBlock(binding.change30dPercentTxt, binding.change30dUsdTxt, livePrice, statPercentCache["${currentSymbol}_30d"] ?: 0.0)
        updateStatBlock(binding.change1yPercentTxt, binding.change1yUsdTxt, livePrice, statPercentCache["${currentSymbol}_1y"] ?: 0.0)

        generateChartData(chartPoints)
    }

    private fun updateStatBlock(percentTxt: TextView, usdTxt: TextView, currentPrice: Double, percentChange: Double) {
        val usdChange = (currentPrice * percentChange) / 100
        val usdFormat = if (currentSymbol == "SHIB" || currentSymbol == "DOGE" || currentSymbol == "XRP") "%.4f" else "%,.2f"
        val usdString = String.format(Locale.US, usdFormat, Math.abs(usdChange))
        val percentString = String.format(Locale.US, "%.2f", Math.abs(percentChange))

        if (percentChange >= 0) { percentTxt.text = "▲ $percentString%"; percentTxt.setTextColor(Color.parseColor("#4CAF50")); usdTxt.text = "+$usdString дол. США" }
        else { percentTxt.text = "▼ $percentString%"; percentTxt.setTextColor(Color.parseColor("#F44336")); usdTxt.text = "-$usdString дол. США" }
    }

    private fun generateChartData(pointsCount: Int) {
        val cacheKey = "${currentSymbol}_$pointsCount"
        val prefs = PrefsManager(this)
        val livePrice = if (currentSymbol == "CFC") 2.50 else prefs.getLivePrice(currentSymbol, currentBasePrice)

        val historyPrices = if (chartHistoryCache.containsKey(cacheKey) && currentSymbol != "CFC") {
            val cached = chartHistoryCache[cacheKey]!!.toMutableList(); cached[cached.size - 1] = livePrice; cached
        } else {
            val newList = mutableListOf<Double>()
            var simulatedPrice = livePrice
            val volatility = if (pointsCount > 100) 0.05 else 0.02
            newList.add(simulatedPrice)
            for (i in 1..pointsCount) {
                if (currentSymbol != "CFC") simulatedPrice *= Random.nextDouble(1.0 - volatility, 1.0 + volatility)
                newList.add(simulatedPrice)
            }
            newList.reverse(); newList[newList.size - 1] = livePrice; chartHistoryCache[cacheKey] = newList; newList
        }

        val entries = ArrayList<Entry>()
        for ((index, price) in historyPrices.withIndex()) entries.add(Entry(index.toFloat(), price.toFloat()))

        val dataSet = LineDataSet(entries, "Price").apply { color = Color.parseColor("#2962FF"); lineWidth = 2f; setDrawCircles(false); setDrawValues(false); mode = LineDataSet.Mode.CUBIC_BEZIER; setDrawFilled(true); fillColor = Color.parseColor("#1A2962FF") }
        binding.priceChart.data = LineData(dataSet); binding.priceChart.animateX(800); binding.priceChart.invalidate()
        updateTopHistoryBlock(historyPrices.first(), historyPrices.last())
    }

    private fun updateTopHistoryBlock(oldPrice: Double, newPrice: Double) {
        val diffUsd = newPrice - oldPrice
        val diffPercent = if (oldPrice > 0) (diffUsd / oldPrice) * 100 else 0.0
        val format = if (currentSymbol == "SHIB" || currentSymbol == "DOGE" || currentSymbol == "XRP") "%.4f" else "%,.2f"

        val usdString = String.format(Locale.US, format, Math.abs(diffUsd))
        val percentString = String.format(Locale.US, "%,.2f", Math.abs(diffPercent))

        if (diffUsd >= 0) { binding.priceChangeUsdTxt.text = "+$usdString дол. США"; binding.priceChangePercentTxt.text = "▲ $percentString%"; binding.priceChangePercentTxt.setTextColor(Color.parseColor("#4CAF50")) }
        else { binding.priceChangeUsdTxt.text = "-$usdString дол. США"; binding.priceChangePercentTxt.text = "▼ $percentString%"; binding.priceChangePercentTxt.setTextColor(Color.parseColor("#F44336")) }
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
        binding.navTradeImg.setOnClickListener { startActivity(Intent(this, AccountsActivity::class.java)); finish() }
        binding.navTradeTxt.setOnClickListener { startActivity(Intent(this, AccountsActivity::class.java)); finish() }
        binding.navMarketImg.setOnClickListener { startActivity(Intent(this, StatisticsActivity::class.java)); finish() }
        binding.navMarketTxt.setOnClickListener { startActivity(Intent(this, StatisticsActivity::class.java)); finish() }
        binding.navProfileImg.setOnClickListener { showProfilePasswordDialog() }
        binding.navProfileTxt.setOnClickListener { showProfilePasswordDialog() }
    }

    companion object {
        val chartHistoryCache = mutableMapOf<String, List<Double>>()
        val statPercentCache = mutableMapOf<String, Double>()
        val isSessionInitialized = mutableSetOf<String>()
    }
}
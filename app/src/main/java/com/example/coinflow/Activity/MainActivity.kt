package com.example.coinflow.Activity

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coinflow.Adapter.CryptoListAdapter
import com.example.coinflow.Helper.PrefsManager
import com.example.coinflow.ViewModel.MainViewModel
import com.example.coinflow.databinding.ActivityMainBinding
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var cryptoAdapter: CryptoListAdapter
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        initRecyclerviewCrypto()
        setupSearch()

        // У MainActivity.kt в методі onCreate:
        binding.btnAssistant.setOnClickListener {
            val prefs = PrefsManager(this)
            if (prefs.getCurrentPlan() == "GOLD") {
                startActivity(Intent(this, AssistantActivity::class.java))
            } else {
                Toast.makeText(this, "Ця функція доступна лише для Золотого плану!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buyBtn.setOnClickListener { startActivity(Intent(this, DepositActivity::class.java)) }
        binding.sellBtn.setOnClickListener { startActivity(Intent(this, ConvertActivity::class.java)) }
        binding.withdrawBtn.setOnClickListener { startActivity(Intent(this, WithdrawActivity::class.java)) }

        binding.navFuturesImg.setOnClickListener { startActivity(Intent(this, GraphActivity::class.java)) }
        binding.navFuturesTxt.setOnClickListener { startActivity(Intent(this, GraphActivity::class.java)) }
        binding.navTradeImg.setOnClickListener { startActivity(Intent(this, AccountsActivity::class.java)) }
        binding.navTradeTxt.setOnClickListener { startActivity(Intent(this, AccountsActivity::class.java)) }
        binding.navMarketImg.setOnClickListener { startActivity(Intent(this, StatisticsActivity::class.java)) }
        binding.navMarketTxt.setOnClickListener { startActivity(Intent(this, StatisticsActivity::class.java)) }
        binding.navProfileImg.setOnClickListener { showProfilePasswordDialog() }
        binding.navProfileTxt.setOnClickListener { showProfilePasswordDialog() }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        fetchLivePrices()
    }

    private fun fetchLivePrices() {
        val request = Request.Builder().url("https://api.binance.com/api/v3/ticker/24hr").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { if (::cryptoAdapter.isInitialized) cryptoAdapter.updateData(getLiveCryptoList()) }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val jsonBody = response.body?.string() ?: return
                    if (!jsonBody.trim().startsWith("[")) return

                    val jsonArray = JSONArray(jsonBody)
                    val prefs = PrefsManager(this@MainActivity)
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
                    runOnUiThread { if (::cryptoAdapter.isInitialized) cryptoAdapter.updateData(getLiveCryptoList()) }
                }
            }
        })
    }

    private fun updateUI() {
        val prefs = PrefsManager(this)
        val usdBalance = prefs.getBalance()
        val uahBalance = prefs.getUahBalance()
        animateBalance(usdBalance, uahBalance)
        binding.balanceTitleTxt.text = "Баланс гаманця (${prefs.getCurrentUsername()})"
    }

    private fun getLiveCryptoList(): List<com.example.coinflow.Model.CryptoModel> {
        val prefs = PrefsManager(this)
        val baseList = mainViewModel.loadData().toMutableList()
        val currentPlan = prefs.getCurrentPlan()

        // ПЕРЕВІРКА ПЛАНУ: Видаляємо CFC зі списку, якщо план Бронзовий
        if (currentPlan == "BRONZE") {
            baseList.removeAll { it.ShortSymbol.contains("CFC") }
        }

        for (item in baseList) {
            val cleanSymbol = item.ShortSymbol.replace("/USDT", "").replace("/USD", "").trim()

            // Встановлюємо жорстку ціну для CFC, інші тягнемо з бази Binance
            if (cleanSymbol == "CFC") {
                item.Price = 2.50
                item.ChangePercent = 0.0
            } else {
                item.Price = prefs.getLivePrice(cleanSymbol, item.Price)
                item.ChangePercent = prefs.getLiveChangePercent(cleanSymbol, item.ChangePercent)
            }
        }
        return baseList
    }

    private fun initRecyclerviewCrypto() {
        binding.view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        cryptoAdapter = CryptoListAdapter(getLiveCryptoList())
        binding.view.adapter = cryptoAdapter
    }

    private fun setupSearch() {
        binding.editTextText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val liveList = getLiveCryptoList()
                val filteredList = if (query.isEmpty()) liveList else {
                    liveList.filter { it.Symbol.lowercase().contains(query) || it.ShortSymbol.lowercase().contains(query) }
                }
                cryptoAdapter.updateData(filteredList)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun animateBalance(targetUsd: Double, targetUah: Double) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 1200
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            binding.balanceUsdTxt.text = String.format(Locale.US, "%,.2f $", targetUsd * fraction)
            binding.balanceUahTxt.text = String.format(Locale.US, "%,.2f ₴", targetUah * fraction)
        }
        animator.start()
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
}
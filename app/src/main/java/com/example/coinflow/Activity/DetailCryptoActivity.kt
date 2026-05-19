package com.example.coinflow.Activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.coinflow.Helper.PrefsManager
import com.example.coinflow.Model.CryptoModel
import com.example.coinflow.R
import com.example.coinflow.databinding.ActivityDetailCryptoBinding
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class DetailCryptoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailCryptoBinding
    private lateinit var item: CryptoModel
    private lateinit var prefs: PrefsManager
    private val client = OkHttpClient()

    private var formatter: DecimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private var isBuyOrder = true
    private var currentCoinSymbol = ""
    private var selectedOrderType = "Своя ціна"
    private var targetedOrderOwner: String? = null
    private var targetedOrderIsBuy: Boolean? = null
    private var isSystemFillingText = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailCryptoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        getBundle()
        setupOrderTypeSpinner()
        setVariable()
        setupCalculation()
        updateAvailableBalanceUI()
        updateOrderBookUI()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        fetchLivePrices()
    }

    private fun fetchLivePrices() {
        if (currentCoinSymbol == "CFC") {
            item.Price = 2.50
            item.ChangePercent = 0.0
            runOnUiThread { updatePriceUI() }
            return
        }

        val request = Request.Builder().url("https://api.binance.com/api/v3/ticker/24hr").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { updatePriceUI() } }

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
                                    if (coin == currentCoinSymbol) {
                                        item.Price = price
                                        item.ChangePercent = percentChange
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
                finally { runOnUiThread { updatePriceUI() } }
            }
        })
    }

    private fun updatePriceUI() {
        val format = if (currentCoinSymbol == "SHIB") "%.6f" else "%,.2f"
        binding.priceTxt.text = String.format(Locale.US, format, item.Price)
        binding.changePercentTxt.text = String.format(Locale.US, "%.2f%%", item.ChangePercent)

        if (item.ChangePercent >= 0) {
            binding.priceTxt.setTextColor(Color.parseColor("#4CAF50"))
            binding.changePercentTxt.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            binding.priceTxt.setTextColor(Color.parseColor("#F44336"))
            binding.changePercentTxt.setTextColor(Color.parseColor("#F44336"))
        }

        if (selectedOrderType == "Ринкова ціна") {
            val editFormat = if (currentCoinSymbol == "SHIB") "%.6f" else "%.2f"
            binding.priceEdt.setText(String.format(Locale.US, editFormat, item.Price))
        }
    }

    private fun setVariable() {
        binding.buyPositionBtn.setOnClickListener { isBuyOrder = true; binding.buyPositionBtn.setBackgroundResource(R.drawable.green_bg); binding.sellPositionBtn.setBackgroundResource(R.drawable.semi_white_bg); binding.sendOrderBtn.setBackgroundResource(R.drawable.green_bg); binding.sendOrderBtn.text = "КУПИТИ $currentCoinSymbol"; updateAvailableBalanceUI(); updateOrderBookUI() }
        binding.sellPositionBtn.setOnClickListener { isBuyOrder = false; binding.buyPositionBtn.setBackgroundResource(R.drawable.semi_white_bg); binding.sellPositionBtn.setBackgroundResource(R.drawable.red_bg); binding.sendOrderBtn.setBackgroundResource(R.drawable.red_bg); binding.sendOrderBtn.text = "ПРОДАТИ $currentCoinSymbol"; updateAvailableBalanceUI(); updateOrderBookUI() }
        binding.plusAmountBtn.setOnClickListener { val currentVal = binding.amountEdt.text.toString().toDoubleOrNull() ?: 0.0; binding.amountEdt.setText((currentVal + 1).toString()) }
        binding.minusAmountBtn.setOnClickListener { val currentVal = binding.amountEdt.text.toString().toDoubleOrNull() ?: 0.0; if (currentVal > 0) binding.amountEdt.setText((currentVal - 1).toString()) }
        binding.totalEdt.isEnabled = false
        binding.sendOrderBtn.setOnClickListener { showConfirmTransactionDialog() }
        binding.orderBookScrollView.isFillViewport = true; binding.orderBookScrollView.isNestedScrollingEnabled = false
        binding.orderBookScrollView.setOnTouchListener { _, event -> when (event.action) { android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> binding.scrollView2.requestDisallowInterceptTouchEvent(true); android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> binding.scrollView2.requestDisallowInterceptTouchEvent(false) }; false }
    }

    private fun getBundle() {
        item = intent.getParcelableExtra("object")!!
        currentCoinSymbol = item.ShortSymbol.trim().replace("/USDT", "").replace("/USD", "")
        item.Price = if (currentCoinSymbol == "CFC") 2.50 else prefs.getLivePrice(currentCoinSymbol, item.Price)
        item.ChangePercent = if (currentCoinSymbol == "CFC") 0.0 else prefs.getLiveChangePercent(currentCoinSymbol, item.ChangePercent)

        binding.symbolNameTxt.text = item.ShortSymbol
        val drawable = resources.getIdentifier(item.SymbolLogo, "drawable", packageName)
        Glide.with(this).load(drawable).into(binding.logoImg)

        updatePriceUI(); binding.backBtn.setOnClickListener { finish() }
    }

    private fun fillOrderPanel(owner: String, price: Double, amount: Double, isOrderBuy: Boolean) {
        targetedOrderOwner = owner; targetedOrderIsBuy = isOrderBuy; isSystemFillingText = true
        if (!isOrderBuy) binding.buyPositionBtn.performClick() else binding.sellPositionBtn.performClick()
        binding.orderTypeSpin.setSelection(0)
        val editFormat = DecimalFormat("#.######", DecimalFormatSymbols(Locale.US))
        binding.priceEdt.setText(editFormat.format(price)); binding.amountEdt.setText(editFormat.format(amount))
        isSystemFillingText = false
    }

    private fun cancelMyOrder(price: Double, amount: Double, isBuy: Boolean) {
        val currentUser = prefs.getCurrentUsername()
        val removed = prefs.removeLimitOrder(currentCoinSymbol, isBuy, currentUser, price, amount)
        if (removed) {
            if (isBuy) prefs.addBalance(price * amount) else prefs.addCryptoBalance(currentCoinSymbol, amount)
            val formatTotal = String.format(Locale.US, "%,.2f", price * amount)
            val formatAmount = java.text.DecimalFormat("#.######", DecimalFormatSymbols(Locale.US)).format(amount)
            if (isBuy) prefs.addTransaction("Скасування (Купівля)", "гаманець", "гаманець", "+ $formatTotal $", true) else prefs.addTransaction("Скасування (Продаж)", currentCoinSymbol, currentCoinSymbol, "+ $formatAmount $currentCoinSymbol", true)
            Toast.makeText(this, "Ордер скасовано, кошти повернуто!", Toast.LENGTH_SHORT).show()
            updateOrderBookUI(); updateAvailableBalanceUI()
        }
    }

    private fun updateOrderBookUI() {
        if (prefs.getCurrentPlan() == "BRONZE") { binding.orderBookBlock.visibility = View.GONE; binding.lockedOrderBookBlock.visibility = View.VISIBLE; return }
        else { binding.orderBookBlock.visibility = View.VISIBLE; binding.lockedOrderBookBlock.visibility = View.GONE }

        binding.orderBookContainer.removeAllViews()
        val currentUser = prefs.getCurrentUsername()
        val rawOrders = prefs.getLimitOrders(currentCoinSymbol, isBuyOrder)

        if (rawOrders.isEmpty()) {
            val emptyTxt = android.widget.TextView(this).apply { text = "Наразі ордерів немає"; setTextColor(Color.parseColor("#888888")); textAlignment = View.TEXT_ALIGNMENT_CENTER; setPadding(0, 64, 0, 64); textSize = 14f }
            binding.orderBookContainer.addView(emptyTxt); return
        }

        // Отримуємо доступ до глобальних налаштувань, щоб дізнатися плани ІНШИХ користувачів
        val sharedPreferences = getSharedPreferences("CoinFlowPrefs", Context.MODE_PRIVATE)

        val sortedOrders = rawOrders.sortedWith { order1, order2 ->
            val plan1 = sharedPreferences.getString("${order1.username}_PLAN", "BRONZE") ?: "BRONZE"
            val plan2 = sharedPreferences.getString("${order2.username}_PLAN", "BRONZE") ?: "BRONZE"

            val isGold1 = (plan1 == "GOLD")
            val isGold2 = (plan2 == "GOLD")

            val isMine1 = (order1.username == currentUser)
            val isMine2 = (order2.username == currentUser)

            // 1. ПРІОРИТЕТ ЗОЛОТОГО ПЛАНУ
            if (isGold1 && !isGold2) -1
            else if (!isGold1 && isGold2) 1
            // 2. Свої ордери вище за інші
            else if (isMine1 && !isMine2) -1
            else if (!isMine1 && isMine2) 1
            // 3. Звичайне сортування за ціною
            else if (isBuyOrder) order2.price.compareTo(order1.price)
            else order1.price.compareTo(order2.price)
        }

        val formatAmount = java.text.DecimalFormat("#.####")
        for (order in sortedOrders) {
            val rowView = layoutInflater.inflate(R.layout.item_order_row, binding.orderBookContainer, false)

            val userTxt = rowView.findViewById<android.widget.TextView>(R.id.rowUserTxt)
            val orderPlan = sharedPreferences.getString("${order.username}_PLAN", "BRONZE") ?: "BRONZE"

            // ЗОЛОТА ПОЗНАЧКА ТА КОРОНА
            if (orderPlan == "GOLD") {
                userTxt.text = "👑 ${order.username}"
                userTxt.setTextColor(Color.parseColor("#FFD700")) // Золотий колір
            } else {
                userTxt.text = "# ${order.username}"
                userTxt.setTextColor(Color.parseColor("#A0A0A0")) // Стандартний сірий колір
            }

            val pTxt = rowView.findViewById<android.widget.TextView>(R.id.rowPriceTxt); pTxt.text = formatter.format(order.price)
            val tTxt = rowView.findViewById<android.widget.TextView>(R.id.rowTotalTxt); tTxt.text = formatter.format(order.price * order.amount)
            rowView.findViewById<android.widget.TextView>(R.id.rowAmountTxt).text = formatAmount.format(order.amount)
            val cancelBtn = rowView.findViewById<android.widget.TextView>(R.id.rowCancelBtn)

            if (isBuyOrder) { pTxt.setTextColor(Color.parseColor("#4CAF50")); tTxt.setTextColor(Color.parseColor("#4CAF50")) }
            else { pTxt.setTextColor(Color.parseColor("#F44336")); tTxt.setTextColor(Color.parseColor("#F44336")) }

            if (order.username == currentUser) { cancelBtn.visibility = View.VISIBLE; cancelBtn.setOnClickListener { cancelMyOrder(order.price, order.amount, isBuyOrder) } }
            else { cancelBtn.visibility = View.INVISIBLE; rowView.setOnClickListener { fillOrderPanel(order.username, order.price, order.amount, isBuyOrder) } }
            binding.orderBookContainer.addView(rowView)
        }
    }

    private fun setupOrderTypeSpinner() {
        val options = if (prefs.getCurrentPlan() == "BRONZE") listOf("Ринкова ціна") else listOf("Своя ціна", "Ринкова ціна")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.orderTypeSpin.adapter = adapter

        binding.orderTypeSpin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedOrderType = parent?.getItemAtPosition(position).toString()
                if (selectedOrderType == "Ринкова ціна") { binding.priceEdt.setText(String.format(Locale.US, if (currentCoinSymbol == "SHIB") "%.6f" else "%.2f", item.Price)); binding.priceEdt.isEnabled = false }
                else binding.priceEdt.isEnabled = true
                calculateTotal()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCalculation() {
        binding.priceEdt.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.priceEdt.removeTextChangedListener(this)
                    val cleanString = s.toString().replace(",", "")
                    if (cleanString.isNotEmpty()) {
                        try { val parts = cleanString.split("."); val formattedInteger = String.format(Locale.US, "%,d", parts[0].toLong()); current = if (parts.size > 1) "$formattedInteger.${parts[1]}" else if (cleanString.endsWith(".")) "$formattedInteger." else formattedInteger } catch (e: Exception) { current = cleanString }
                    } else current = ""
                    binding.priceEdt.setText(current); binding.priceEdt.setSelection(current.length); binding.priceEdt.addTextChangedListener(this)
                }
                calculateTotal()
                if (!isSystemFillingText) { targetedOrderOwner = null; targetedOrderIsBuy = null }
            }
        })
        binding.amountEdt.addTextChangedListener(object : TextWatcher { override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}; override fun afterTextChanged(s: Editable?) { calculateTotal(); if (!isSystemFillingText) { targetedOrderOwner = null; targetedOrderIsBuy = null } } })
    }

    private fun calculateTotal() {
        val price = binding.priceEdt.text.toString().replace(",", "").toDoubleOrNull() ?: 0.0
        val amount = binding.amountEdt.text.toString().toDoubleOrNull() ?: 0.0
        binding.totalEdt.setText(formatter.format(price * amount))
    }

    private fun updateAvailableBalanceUI() {
        val usdBalance = prefs.getBalance()
        val cryptoBalance = prefs.getCryptoBalance(currentCoinSymbol)
        val formatAmount = java.text.DecimalFormat("#.######")
        binding.availableBalanceTxt.text = "Доступно:\n${String.format(Locale.US, "%,.2f", usdBalance)} $\n${formatAmount.format(cryptoBalance)} ${item.ShortSymbol.trim()}"
    }

    private fun showConfirmTransactionDialog() {
        val amount = binding.amountEdt.text.toString().trim().toDoubleOrNull() ?: 0.0
        val priceUSD = binding.priceEdt.text.toString().replace(",", "").trim().toDoubleOrNull() ?: 0.0

        if (amount <= 0 || priceUSD <= 0) { Toast.makeText(this, "Введіть коректну кількість та ціну", Toast.LENGTH_SHORT).show(); return }

        val totalUSD = amount * priceUSD
        val formatTotal = String.format(Locale.US, "%,.2f", totalUSD)
        val formatAmount = java.text.DecimalFormat("#.######", DecimalFormatSymbols(Locale.US)).format(amount)

        val actionText = if (isBuyOrder) "купити" else "продати"
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(if (isBuyOrder) "Підтвердження купівлі" else "Підтвердження продажу")
            .setMessage("Ви впевнені, що хочете $actionText $formatAmount $currentCoinSymbol на загальну суму $formatTotal $?")
            .setPositiveButton("Підтвердити") { _, _ -> processTransaction() }
            .setNegativeButton("Скасувати", null).create()

        dialog.show()
        val solidBackground = android.graphics.drawable.GradientDrawable().apply { setColor(Color.parseColor("#1C1F26")); cornerRadius = 32f }
        dialog.window?.setBackgroundDrawable(solidBackground); dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(if (isBuyOrder) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")); dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.parseColor("#E0E0E0"))
    }

    private fun processTransaction() {
        val amount = binding.amountEdt.text.toString().toDoubleOrNull() ?: 0.0
        val priceUSD = binding.priceEdt.text.toString().replace(",", "").toDoubleOrNull() ?: 0.0

        if (amount <= 0 || priceUSD <= 0) { Toast.makeText(this, "Введіть коректну кількість та ціну", Toast.LENGTH_SHORT).show(); return }
        if (prefs.getCurrentPlan() == "BRONZE" && selectedOrderType == "Своя ціна") { Toast.makeText(this, "Лімітні ордери доступні лише для Срібного та Золотого планів!", Toast.LENGTH_LONG).show(); return }

        val totalUSD = amount * priceUSD
        val formatTotal = String.format(Locale.US, "%,.2f", totalUSD)
        val formatAmount = java.text.DecimalFormat("#.######", DecimalFormatSymbols(Locale.US)).format(amount)

        if (targetedOrderOwner != null && selectedOrderType == "Своя ціна") {
            if (isBuyOrder && targetedOrderIsBuy == false) {
                if (prefs.getBalance() >= totalUSD) {
                    if (prefs.removeLimitOrder(currentCoinSymbol, false, targetedOrderOwner!!, priceUSD, amount)) {
                        prefs.addBalance(-totalUSD); prefs.addCryptoBalance(currentCoinSymbol, amount); prefs.addBalanceToUser(targetedOrderOwner!!, totalUSD)
                        prefs.addTransaction("Купівля", "гаманець", currentCoinSymbol, "- $formatTotal $", false, "+ $formatAmount $currentCoinSymbol", true)
                        prefs.recordTrade(true, totalUSD, currentCoinSymbol); Toast.makeText(this, "Успішно куплено у ${targetedOrderOwner}!", Toast.LENGTH_SHORT).show()
                    } else Toast.makeText(this, "Ордер вже неактуальний!", Toast.LENGTH_SHORT).show()
                } else { Toast.makeText(this, "Недостатньо коштів!", Toast.LENGTH_SHORT).show(); return }
            } else if (!isBuyOrder && targetedOrderIsBuy == true) {
                if (prefs.getCryptoBalance(currentCoinSymbol) >= amount) {
                    if (prefs.removeLimitOrder(currentCoinSymbol, true, targetedOrderOwner!!, priceUSD, amount)) {
                        prefs.addCryptoBalance(currentCoinSymbol, -amount); prefs.addBalance(totalUSD); prefs.addCryptoBalanceToUser(targetedOrderOwner!!, currentCoinSymbol, amount)
                        prefs.addTransaction("Продаж", currentCoinSymbol, "гаманець", "- $formatAmount $currentCoinSymbol", false, "+ $formatTotal $", true)
                        prefs.recordTrade(false, totalUSD, currentCoinSymbol); Toast.makeText(this, "Успішно продано ${targetedOrderOwner}!", Toast.LENGTH_SHORT).show()
                    } else Toast.makeText(this, "Ордер вже неактуальний!", Toast.LENGTH_SHORT).show()
                } else { Toast.makeText(this, "Недостатньо крипти!", Toast.LENGTH_SHORT).show(); return }
            }
            targetedOrderOwner = null; updateOrderBookUI(); updateAvailableBalanceUI(); return
        }

        if (selectedOrderType == "Своя ціна") {
            if (isBuyOrder) {
                if (prefs.getBalance() >= totalUSD) {
                    prefs.addBalance(-totalUSD); prefs.addLimitOrder(currentCoinSymbol, true, priceUSD, amount)
                    prefs.addTransaction("Ордер (Купівля)", "гаманець", "coins", "- $formatTotal $", false); prefs.recordTrade(true, totalUSD, currentCoinSymbol)
                    Toast.makeText(this, "Ордер за вашою ціною додано!", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(this, "Недостатньо коштів!", Toast.LENGTH_SHORT).show()
            } else {
                if (prefs.getCryptoBalance(currentCoinSymbol) >= amount) {
                    prefs.addCryptoBalance(currentCoinSymbol, -amount); prefs.addLimitOrder(currentCoinSymbol, false, priceUSD, amount)
                    prefs.addTransaction("Ордер (Продаж)", currentCoinSymbol, "coins", "- $formatAmount $currentCoinSymbol", false); prefs.recordTrade(false, totalUSD, currentCoinSymbol)
                    Toast.makeText(this, "Ордер за вашою ціною додано!", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(this, "Недостатньо крипти!", Toast.LENGTH_SHORT).show()
            }
            updateOrderBookUI(); updateAvailableBalanceUI()
        } else {
            if (isBuyOrder) {
                if (prefs.getBalance() >= totalUSD) {
                    prefs.addBalance(-totalUSD); prefs.addCryptoBalance(currentCoinSymbol, amount)
                    prefs.addTransaction("Купівля (Маркет)", "гаманець", currentCoinSymbol, "- $formatTotal $", false, "+ $formatAmount $currentCoinSymbol", true)
                    prefs.recordTrade(true, totalUSD, currentCoinSymbol); Toast.makeText(this, "Куплено за ринковою ціною!", Toast.LENGTH_SHORT).show(); finish()
                } else Toast.makeText(this, "Недостатньо коштів!", Toast.LENGTH_SHORT).show()
            } else {
                if (prefs.getCryptoBalance(currentCoinSymbol) >= amount) {
                    prefs.addCryptoBalance(currentCoinSymbol, -amount); prefs.addBalance(totalUSD)
                    prefs.addTransaction("Продаж (Маркет)", currentCoinSymbol, "гаманець", "- $formatAmount $currentCoinSymbol", false, "+ $formatTotal $", true)
                    prefs.recordTrade(false, totalUSD, currentCoinSymbol); Toast.makeText(this, "Продано за ринковою ціною!", Toast.LENGTH_SHORT).show(); finish()
                } else Toast.makeText(this, "Недостатньо крипти!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPasswordDialog(title: String, message: String, onSuccess: () -> Unit) {
        val savedPassword = prefs.getCurrentPassword()

        val textInputLayout = com.google.android.material.textfield.TextInputLayout(this).apply { endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE; setEndIconTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.LTGRAY)); boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_NONE; isHintEnabled = false }
        val editText = com.google.android.material.textfield.TextInputEditText(textInputLayout.context).apply { inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD; hint = "Ваш пароль"; setTextColor(android.graphics.Color.WHITE); setHintTextColor(android.graphics.Color.WHITE); textSize = 18f; setPadding(0, 32, 0, 32); backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE) }
        textInputLayout.addView(editText)
        val layout = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(64, 32, 64, 16); addView(textInputLayout) }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(title).setMessage(message).setView(layout)
            .setPositiveButton("Підтвердити") { _, _ -> if (editText.text.toString() == savedPassword) onSuccess() else Toast.makeText(this, "Невірний пароль!", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Скасувати", null).create()

        dialog.show()
        val solidBackground = android.graphics.drawable.GradientDrawable().apply { setColor(android.graphics.Color.parseColor("#1C1F26")); cornerRadius = 32f }
        dialog.window?.setBackgroundDrawable(solidBackground)
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.parseColor("#F44336"))
    }

    private fun setupBottomNavigation() {
        binding.navWalletImg.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)); finish() }
        binding.navWalletTxt.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)); finish() }
        binding.navFuturesImg.setOnClickListener { startActivity(Intent(this, GraphActivity::class.java)); finish() }
        binding.navFuturesTxt.setOnClickListener { startActivity(Intent(this, GraphActivity::class.java)); finish() }
        binding.navTradeImg.setOnClickListener { startActivity(Intent(this, AccountsActivity::class.java)); finish() }
        binding.navTradeTxt.setOnClickListener { startActivity(Intent(this, AccountsActivity::class.java)); finish() }
        binding.navMarketImg.setOnClickListener { startActivity(Intent(this, StatisticsActivity::class.java)); finish() }
        binding.navMarketTxt.setOnClickListener { startActivity(Intent(this, StatisticsActivity::class.java)); finish() }
        binding.navProfileImg.setOnClickListener { showPasswordDialog("Безпека", "Введіть пароль для доступу до Профілю.") { startActivity(Intent(this, ProfileActivity::class.java)); finish() } }
        binding.navProfileTxt.setOnClickListener { showPasswordDialog("Безпека", "Введіть пароль для доступу до Профілю.") { startActivity(Intent(this, ProfileActivity::class.java)); finish() } }
    }
}
package com.example.coinflow.Activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coinflow.Helper.PrefsManager
import com.example.coinflow.R
import com.example.coinflow.databinding.ActivityConvertBinding
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.util.Locale

class ConvertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConvertBinding
    private lateinit var prefs: PrefsManager

    private var isUahToUsd = true
    private var feePercent = 0.015 // 1.5% за замовчуванням

    // Курси за замовчуванням
    private var rateBuy = 40.00
    private var rateSale = 40.50

    private val currencies = listOf("Гривня (UAH)", "Долар (USD)")
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConvertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        prefs = PrefsManager(this)

        // ПЕРЕВІРКА ПЛАНУ: Для Золотого плану комісія 0%
        if (prefs.getCurrentPlan() == "GOLD") {
            feePercent = 0.0
        }

        rateBuy = prefs.getLivePrice("CURRENCY_BUY", 40.00)
        rateSale = prefs.getLivePrice("CURRENCY_SALE", 40.50)

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setupBottomNavigation()
        setupSpinner()
        fetchExchangeRate()

        binding.swapIcon.setOnClickListener {
            val newPosition = if (binding.currencySpin.selectedItemPosition == 0) 1 else 0
            binding.currencySpin.setSelection(newPosition)
        }

        binding.amountEdt.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.amountEdt.removeTextChangedListener(this)
                    val cleanString = s.toString().replace(",", "")
                    if (cleanString.isNotEmpty()) {
                        try {
                            val parts = cleanString.split(".")
                            val formattedInteger = String.format(Locale.US, "%,d", parts[0].toLong())
                            current = if (parts.size > 1) "$formattedInteger.${parts[1]}" else formattedInteger
                        } catch (e: Exception) { current = cleanString }
                    } else { current = "" }
                    binding.amountEdt.setText(current)
                    binding.amountEdt.setSelection(current.length)
                    binding.amountEdt.addTextChangedListener(this)
                }
                calculateReceiveAmount()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.convertBtn.setOnClickListener { showConfirmExchangeDialog() }
    }

    private fun fetchExchangeRate() {
        val request = Request.Builder()
            .url("https://api.privatbank.ua/p24api/pubinfo?json&exchange&coursid=5")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@ConvertActivity, "Курс не оновлено (офлайн)", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonBody ->
                    try {
                        val jsonArray = JSONArray(jsonBody)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            if (obj.getString("ccy") == "USD") {
                                rateBuy = obj.getDouble("buy")
                                rateSale = obj.getDouble("sale")

                                prefs.saveLivePrice("CURRENCY_BUY", rateBuy)
                                prefs.saveLivePrice("CURRENCY_SALE", rateSale)

                                runOnUiThread { updateUI() }
                                break
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        })
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, R.layout.spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.currencySpin.adapter = adapter

        binding.currencySpin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                isUahToUsd = (position == 0)
                binding.amountEdt.text?.clear()
                updateUI()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateUI() {
        val currentRate = if (isUahToUsd) rateSale else rateBuy
        val rateDisplay = String.format(Locale.US, "%.2f", currentRate)

        if (isUahToUsd) {
            binding.giveTitleTxt.text = "Ви віддаєте (Курс продажу: $rateDisplay ₴):"
            binding.receiveTitleTxt.text = "Ви отримуєте (USD):"
            binding.availableGiveTxt.text = "Доступно: ${String.format(Locale.US, "%,.2f ₴", prefs.getUahBalance())}"
            binding.receiveAmountTxt.setTextColor(resources.getColor(R.color.green))
        } else {
            binding.giveTitleTxt.text = "Ви віддаєте (Курс купівлі: $rateDisplay ₴):"
            binding.receiveTitleTxt.text = "Ви отримуєте (UAH):"
            binding.availableGiveTxt.text = "Доступно: ${String.format(Locale.US, "%,.2f $", prefs.getBalance())}"
            binding.receiveAmountTxt.setTextColor(resources.getColor(R.color.green))
        }
        calculateReceiveAmount()
    }

    private fun calculateReceiveAmount() {
        val amount = binding.amountEdt.text.toString().replace(",", "").toDoubleOrNull() ?: 0.0
        val currentRate = if (isUahToUsd) rateSale else rateBuy
        val pureReceiveAmount = if (isUahToUsd) amount / currentRate else amount * currentRate

        val fee = pureReceiveAmount * feePercent
        val finalReceive = pureReceiveAmount - fee

        binding.receiveAmountTxt.text = if (isUahToUsd)
            String.format(Locale.US, "%,.2f $", finalReceive)
        else
            String.format(Locale.US, "%,.2f ₴", finalReceive)

        val feeFormat = String.format(Locale.US, "%,.2f", fee)
        val finalFormat = String.format(Locale.US, "%,.2f", finalReceive)

        val feeText = if (feePercent == 0.0) "Комісія (Золотий план: 0%)" else "Комісія системи (1.5%)"

        if (isUahToUsd) {
            binding.feeInfoTxt.text = "$feeText: $feeFormat $\nОстаточно до отримання: $finalFormat $"
        } else {
            binding.feeInfoTxt.text = "$feeText: $feeFormat ₴\nОстаточно до отримання: $finalFormat ₴"
        }
    }

    private fun showConfirmExchangeDialog() {
        val amountStr = binding.amountEdt.text.toString().trim().replace(",", "")
        val giveAmount = amountStr.toDoubleOrNull() ?: 0.0
        val currentRate = if (isUahToUsd) rateSale else rateBuy

        if (giveAmount <= 0) {
            Toast.makeText(this, "Введіть суму", Toast.LENGTH_SHORT).show()
            return
        }

        val hasFunds = if (isUahToUsd) prefs.getUahBalance() >= giveAmount else prefs.getBalance() >= giveAmount

        if (!hasFunds) {
            Toast.makeText(this, "Недостатньо коштів!", Toast.LENGTH_SHORT).show()
            return
        }

        val receiveStr = binding.receiveAmountTxt.text.toString()
        val currencyFrom = if (isUahToUsd) "₴" else "$"
        val message = "Обміняти ${String.format(Locale.US, "%,.2f", giveAmount)} $currencyFrom на $receiveStr (з урахуванням комісії) за курсом $currentRate?"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Підтвердження").setMessage(message)
            .setPositiveButton("Підтвердити") { _, _ ->
                showPasswordDialog("Перевірка безпеки", "Введіть пароль для підтвердження обміну.") {

                    val pureReceiveAmount = if (isUahToUsd) giveAmount / currentRate else giveAmount * currentRate
                    val fee = pureReceiveAmount * feePercent
                    val finalReceive = pureReceiveAmount - fee

                    processExchange(giveAmount, fee, finalReceive)
                }
            }
            .setNegativeButton("Скасувати", null).create()

        dialog.show()
        val solidBackground = android.graphics.drawable.GradientDrawable().apply { setColor(android.graphics.Color.parseColor("#1C1F26")); cornerRadius = 32f }
        dialog.window?.setBackgroundDrawable(solidBackground)
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.parseColor("#F44336"))
    }

    private fun processExchange(giveAmount: Double, fee: Double, finalReceive: Double) {
        if (isUahToUsd) {
            prefs.addUahBalance(-giveAmount)
            prefs.addBalance(finalReceive)
            prefs.addTransaction("Конвертація (UAH → USD)", "wallet", "wallet", "- ${String.format(Locale.US, "%,.2f", giveAmount)} ₴", false, "+ ${String.format(Locale.US, "%,.2f", finalReceive)} $", true)
        } else {
            prefs.addBalance(-giveAmount)
            prefs.addUahBalance(finalReceive)
            prefs.addTransaction("Конвертація (USD → UAH)", "wallet", "wallet", "- ${String.format(Locale.US, "%,.2f", giveAmount)} $", false, "+ ${String.format(Locale.US, "%,.2f", finalReceive)} ₴", true)
        }

        val currencyGive = if (isUahToUsd) "₴" else "$"
        val currencyReceive = if (isUahToUsd) "$" else "₴"

        showReceiptDialog(
            title = if (isUahToUsd) "Обмін UAH на USD" else "Обмін USD на UAH",
            amountStr = "${String.format(Locale.US, "%,.2f", giveAmount)} $currencyGive",
            feeStr = "${String.format(Locale.US, "%,.2f", fee)} $currencyReceive",
            finalStr = "${String.format(Locale.US, "%,.2f", finalReceive)} $currencyReceive",
            cardNumber = null
        )
    }

    // --- БЛОК ГЕНЕРАЦІЇ ЧЕКУ ---
    private fun showReceiptDialog(title: String, amountStr: String, feeStr: String, finalStr: String, cardNumber: String?) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Транзакція успішна")
            .setMessage("Бажаєте завантажити чек у форматі PDF?")
            .setPositiveButton("Завантажити") { _, _ -> generatePdfReceipt(title, amountStr, feeStr, finalStr, cardNumber); finish() }
            .setNegativeButton("Ні, дякую") { _, _ -> finish() }
            .setCancelable(false).create()

        dialog.show()
        val solidBackground = android.graphics.drawable.GradientDrawable().apply { setColor(android.graphics.Color.parseColor("#1C1F26")); cornerRadius = 32f }
        dialog.window?.setBackgroundDrawable(solidBackground)
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.parseColor("#F44336"))
    }

    private fun generatePdfReceipt(title: String, amountStr: String, feeStr: String, finalStr: String, cardNumber: String?) {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(400, 450, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()

        paint.color = android.graphics.Color.WHITE
        canvas.drawPaint(paint)
        paint.color = android.graphics.Color.BLACK; paint.textSize = 20f; paint.isFakeBoldText = true
        canvas.drawText("Квитанція CoinFlow", 20f, 40f, paint)

        paint.textSize = 14f; paint.isFakeBoldText = false
        canvas.drawText("Операція: $title", 20f, 80f, paint)

        var yPos = 110f
        canvas.drawText("Акаунт: ${prefs.getCurrentUsername()}", 20f, yPos, paint); yPos += 30f

        if (cardNumber != null) {
            val maskedCard = if (cardNumber.length >= 4) "**** **** **** " + cardNumber.takeLast(4) else cardNumber
            canvas.drawText("Картка: $maskedCard", 20f, yPos, paint); yPos += 30f
        }

        canvas.drawText("Сума віддано: $amountStr", 20f, yPos, paint); yPos += 30f
        canvas.drawText("Комісія: $feeStr", 20f, yPos, paint); yPos += 30f
        paint.isFakeBoldText = true; paint.textSize = 16f
        canvas.drawText("Підсумок: $finalStr", 20f, yPos, paint); yPos += 40f

        paint.isFakeBoldText = false; paint.textSize = 10f; paint.color = android.graphics.Color.GRAY
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        canvas.drawText("Дата: ${sdf.format(java.util.Date())}", 20f, yPos, paint)

        pdfDocument.finishPage(page)
        try {
            val file = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "CoinFlow_Receipt_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(java.io.FileOutputStream(file))
            Toast.makeText(this, "Чек збережено в Завантаження!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace(); Toast.makeText(this, "Помилка збереження чеку: ${e.message}", Toast.LENGTH_LONG).show()
        } finally { pdfDocument.close() }
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
package com.example.coinflow.Activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coinflow.Helper.PrefsManager
import com.example.coinflow.databinding.ActivityDepositBinding
import java.util.Locale

class DepositActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDepositBinding
    private var feePercent = 0.015 // 1.5% за замовчуванням
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDepositBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        prefs = PrefsManager(this)

        // ПЕРЕВІРКА ПЛАНУ: Для Золотого плану комісія 0%
        if (prefs.getCurrentPlan() == "GOLD") {
            feePercent = 0.0
        }

        setupBottomNavigation()

        binding.amountEdt.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.amountEdt.removeTextChangedListener(this)
                    val cleanString = s.toString().replace(",", "")
                    if (cleanString.isNotEmpty()) {
                        try {
                            val parts = cleanString.split(".")
                            val formattedInteger = String.format(Locale.US, "%,d", parts[0].toLong())
                            current = if (parts.size > 1) "$formattedInteger.${parts[1]}"
                            else if (cleanString.endsWith(".")) "$formattedInteger."
                            else formattedInteger
                        } catch (e: Exception) { current = cleanString }
                    } else { current = "" }
                    binding.amountEdt.setText(current)
                    binding.amountEdt.setSelection(current.length)
                    binding.amountEdt.addTextChangedListener(this)
                }

                // РАХУЄМО КОМІСІЮ
                val amount = current.replace(",", "").toDoubleOrNull() ?: 0.0
                val fee = amount * feePercent
                val totalToReceive = amount - fee

                val feeText = if (feePercent == 0.0) "Комісія (Золотий план: 0%)" else "Комісія системи (1.5%)"
                binding.feeInfoTxt.text = "$feeText: ${String.format(Locale.US, "%,.2f", fee)} ₴\nДо зарахування на баланс: ${String.format(Locale.US, "%,.2f", totalToReceive)} ₴"
            }
        })

        binding.cardNumberEdt.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                isUpdating = true
                var digitsOnly = s.toString().replace("\\D".toRegex(), "")
                if (digitsOnly.length > 16) digitsOnly = digitsOnly.substring(0, 16)
                val formatted = StringBuilder()
                for (i in digitsOnly.indices) {
                    if (i > 0 && i % 4 == 0) formatted.append(" ")
                    formatted.append(digitsOnly[i])
                }
                binding.cardNumberEdt.setText(formatted.toString())
                binding.cardNumberEdt.setSelection(formatted.length)
                isUpdating = false
            }
        })

        binding.cardExpiryEdt.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                isUpdating = true
                var digitsOnly = s.toString().replace("\\D".toRegex(), "")
                if (digitsOnly.length > 4) digitsOnly = digitsOnly.substring(0, 4)
                val formatted = StringBuilder()
                for (i in digitsOnly.indices) {
                    if (i == 2) formatted.append("/")
                    formatted.append(digitsOnly[i])
                }
                binding.cardExpiryEdt.setText(formatted.toString())
                binding.cardExpiryEdt.setSelection(formatted.length)
                isUpdating = false
            }
        })

        val savedCardNumber = prefs.getCardNumber()
        val savedCardExpiry = prefs.getCardExpiry()

        if (savedCardNumber.isNotEmpty()) binding.cardNumberEdt.setText(savedCardNumber)
        if (savedCardExpiry.isNotEmpty()) binding.cardExpiryEdt.setText(savedCardExpiry)

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.sendDepositBtn.setOnClickListener {
            val amountStr = binding.amountEdt.text.toString().replace(",", "")
            val amount = amountStr.toDoubleOrNull()
            val cardNumber = binding.cardNumberEdt.text.toString().replace(" ", "")
            val cardExpiry = binding.cardExpiryEdt.text.toString()

            if (cardNumber.length < 16) { Toast.makeText(this, "Введіть повний номер картки", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (cardExpiry.length < 5) { Toast.makeText(this, "Введіть термін дії картки", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (amount == null || amount <= 0) { Toast.makeText(this, "Будь ласка, введіть коректну суму", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            showPasswordDialog("Підтвердження", "Введіть пароль для поповнення рахунку.") {
                val fee = amount * feePercent
                val totalToReceive = amount - fee

                prefs.addUahBalance(totalToReceive)

                val formattedAmount = String.format(Locale.US, "%,.2f", totalToReceive)
                prefs.addTransaction(
                    title = "Поповнення",
                    leftIcon = "card",
                    rightIcon = "wallet",
                    amount1 = "+ $formattedAmount ₴",
                    isAmount1Green = true
                )

                // ПАРТНЕРСЬКА ПРОГРАМА (БОНУСИ CFC)
                val currentPlan = prefs.getCurrentPlan()
                if (currentPlan == "SILVER" || currentPlan == "GOLD") {
                    val bonusCFC = (amount / 1000).toInt().toDouble() // 1 CFC за кожні 1000 ₴
                    if (bonusCFC > 0) {
                        prefs.addCryptoBalance("CFC", bonusCFC)
                        Toast.makeText(this, "Поповнено! Бонус: +$bonusCFC CFC", Toast.LENGTH_LONG).show()
                    }
                }

                showReceiptDialog(
                    title = "Поповнення рахунку",
                    amountStr = "${String.format(Locale.US, "%,.2f", amount)} ₴",
                    feeStr = "${String.format(Locale.US, "%,.2f", fee)} ₴",
                    finalStr = "$formattedAmount ₴",
                    cardNumber = cardNumber
                )
            }
        }
    }

    private fun showReceiptDialog(title: String, amountStr: String, feeStr: String, finalStr: String, cardNumber: String?) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Транзакція успішна")
            .setMessage("Бажаєте завантажити чек у форматі PDF?")
            .setPositiveButton("Завантажити") { _, _ ->
                generatePdfReceipt(title, amountStr, feeStr, finalStr, cardNumber)
                finish()
            }
            .setNegativeButton("Ні, дякую") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .create()

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
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Квитанція CoinFlow", 20f, 40f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText("Операція: $title", 20f, 80f, paint)

        var yPos = 110f
        val username = prefs.getCurrentUsername()
        canvas.drawText("Акаунт: $username", 20f, yPos, paint)
        yPos += 30f

        if (cardNumber != null) {
            val maskedCard = if (cardNumber.length >= 4) "**** **** **** " + cardNumber.takeLast(4) else cardNumber
            canvas.drawText("Картка: $maskedCard", 20f, yPos, paint)
            yPos += 30f
        }

        canvas.drawText("Сума: $amountStr", 20f, yPos, paint)
        yPos += 30f
        canvas.drawText("Комісія: $feeStr", 20f, yPos, paint)
        yPos += 30f

        paint.isFakeBoldText = true
        paint.textSize = 16f
        canvas.drawText("Підсумок: $finalStr", 20f, yPos, paint)
        yPos += 40f

        paint.isFakeBoldText = false
        paint.textSize = 10f
        paint.color = android.graphics.Color.GRAY
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        canvas.drawText("Дата: ${sdf.format(java.util.Date())}", 20f, yPos, paint)

        pdfDocument.finishPage(page)

        try {
            val file = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "CoinFlow_Receipt_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(java.io.FileOutputStream(file))
            Toast.makeText(this, "Чек збережено в Завантаження!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Помилка збереження чеку: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
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
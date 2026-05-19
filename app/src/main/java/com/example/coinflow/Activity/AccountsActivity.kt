package com.example.coinflow.Activity

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.coinflow.Adapter.TransactionAdapter
import com.example.coinflow.Helper.PrefsManager
import com.example.coinflow.databinding.ActivityAccountsBinding

class AccountsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountsBinding
    private lateinit var prefs: PrefsManager

    private fun showProfilePasswordDialog() {
        val prefs = com.example.coinflow.Helper.PrefsManager(this)
        val savedPassword = prefs.getCurrentPassword()

        // 1. Створюємо контейнер
        val textInputLayout = com.google.android.material.textfield.TextInputLayout(this).apply {
            endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
            setEndIconTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.LTGRAY)) // Світло-сіре око
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_NONE
            isHintEnabled = false // ВИМИКАЄМО плаваючий фіолетовий текст
        }

        // 2. Створюємо саме поле для вводу пароля
        val editText = com.google.android.material.textfield.TextInputEditText(textInputLayout.context).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Ваш пароль"
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.WHITE) // Білий текст підказки
            textSize = 18f // Збільшений шрифт
            setPadding(0, 32, 0, 32) // Рівні відступи всередині поля
            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        }

        textInputLayout.addView(editText)

        // 3. Формуємо макет для вікна з відступами
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(64, 32, 64, 16)
            addView(textInputLayout)
        }

        // 4. Будуємо спливаюче вікно
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Безпека")
            .setMessage("Введіть пароль від акаунту для доступу до Профілю.")
            .setView(layout)
            .setPositiveButton("Увійти") { _, _ ->
                if (editText.text.toString() == savedPassword) {
                    startActivity(android.content.Intent(this, ProfileActivity::class.java))
                    finish()
                } else {
                    android.widget.Toast.makeText(this, "Невірний пароль!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Скасувати", null)
            .create()

        dialog.show()

        // 5. Дизайн вікна (темний фон та кольорові кнопки)
        val solidBackground = android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.parseColor("#1C1F26"))
            cornerRadius = 32f
        }
        dialog.window?.setBackgroundDrawable(solidBackground)

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.parseColor("#F44336"))
    }
    private fun showClearHistoryDialog() {
        // Створюємо діалог
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Очищення історії")
            .setMessage("Ви впевнені, що хочете видалити всі записи рахунків?")
            .setPositiveButton("Видалити") { _, _ ->
                // 1. Видаляємо дані з бази
                val prefs = com.example.coinflow.Helper.PrefsManager(this)
                prefs.clearTransactionsHistory()

                // 2. Очищаємо список на екрані
                // ВАЖЛИВО: Заміни 'adapter' та 'loadTransactions' на ті назви, які ти використовуєш у своєму коді!
                // Наприклад, якщо в тебе є функція для завантаження списку, виклич її знову:
                // loadData() або initRecyclerView()

                android.widget.Toast.makeText(this, "Історію успішно очищено", android.widget.Toast.LENGTH_SHORT).show()

                // Щоб екран оновився, можна просто перезапустити Activity:
                finish()
                startActivity(intent)
            }
            .setNegativeButton("Скасувати", null)
            .create()

        dialog.show()

        // Робимо красивий темний дизайн вікна
        val solidBackground = android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.parseColor("#1C1F26"))
            cornerRadius = 32f
        }
        dialog.window?.setBackgroundDrawable(solidBackground)

        // Кнопка "Видалити" буде червоною, а "Скасувати" - світлою
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#F44336"))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.parseColor("#E0E0E0"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        prefs = PrefsManager(this)

        // Кнопка назад
        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Клік по кнопці смітника
        binding.clearHistoryBtn.setOnClickListener {
            showClearHistoryDialog()
        }

        // Ініціалізація списку
        binding.transactionsRecycler.layoutManager = LinearLayoutManager(this)

        // Отримуємо історію з нашої бази даних
        val transactionsList = prefs.getTransactions()
        binding.transactionsRecycler.adapter = TransactionAdapter(transactionsList)

        // --- Налаштування нижньої навігації через BINDING ---
        binding.navWalletImg.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.navWalletTxt.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.navFuturesImg.setOnClickListener { startActivity(Intent(this, GraphActivity::class.java)); finish() }
        binding.navFuturesTxt.setOnClickListener { startActivity(Intent(this, GraphActivity::class.java)); finish() }

        // Перехід на екран "Профіль"
        binding.navProfileImg.setOnClickListener { showProfilePasswordDialog() }
        binding.navProfileTxt.setOnClickListener { showProfilePasswordDialog() }

        binding.navMarketImg.setOnClickListener { startActivity(Intent(this, StatisticsActivity::class.java)); finish() }
        binding.navMarketTxt.setOnClickListener { startActivity(Intent(this, StatisticsActivity::class.java)); finish() }

    }

}
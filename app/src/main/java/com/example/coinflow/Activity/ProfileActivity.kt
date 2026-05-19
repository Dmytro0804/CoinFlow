package com.example.coinflow.Activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coinflow.Helper.PrefsManager
import com.example.coinflow.R
import com.example.coinflow.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Блокуємо логін
        binding.profileUsernameEdt.isEnabled = false

        // 1. TextWatcher для номера картки (жорсткий ліміт 16 цифр)
        binding.profileCardNumberEdt.addTextChangedListener(object : TextWatcher {
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
                binding.profileCardNumberEdt.setText(formatted.toString())
                binding.profileCardNumberEdt.setSelection(formatted.length)
                isUpdating = false
            }
        })

        // 2. TextWatcher для ТЕРМІНУ ДІЇ (жорсткий ліміт 4 цифри)
        binding.profileCardExpiryEdt.addTextChangedListener(object : TextWatcher {
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
                binding.profileCardExpiryEdt.setText(formatted.toString())
                binding.profileCardExpiryEdt.setSelection(formatted.length)
                isUpdating = false
            }
        })

        // 3. Завантажуємо дані
        loadData()

        setupBottomNavigation()

        // 4. Кнопка Назад
        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // 5. Кнопка Зберегти
        binding.saveProfileBtn.setOnClickListener {
            val email = binding.profileEmailEdt.text.toString().trim()
            val phone = binding.profilePhoneEdt.text.toString().trim()
            val pass = binding.profilePasswordEdt.text.toString().trim()
            val cardNumber = binding.profileCardNumberEdt.text.toString().trim()
            val cardExpiry = binding.profileCardExpiryEdt.text.toString().trim()

            if (email.isEmpty() || phone.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Заповніть усі основні поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Введіть коректну ел. пошту (наприклад: name@mail.com)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.updateUserData(email, phone, pass)
            prefs.saveCardInfo(cardNumber, cardExpiry)

            Toast.makeText(this, "Дані оновлено!", Toast.LENGTH_SHORT).show()
        }

        // 6. Кнопка Вийти
        binding.logoutBtn.setOnClickListener {
            prefs.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // --- ЛОГІКА ОНОВЛЕННЯ ПЛАНУ ---
    // Викликається щоразу, коли користувач повертається на сторінку Профілю
    override fun onResume() {
        super.onResume()

        val currentPlan = prefs.getCurrentPlan()

        // Змінюємо текст і фон кнопки-плашки
        when (currentPlan) {
            "BRONZE" -> {
                binding.planNameTxt.text = "Бронзовий план"
                binding.planContainerBtn.setBackgroundResource(R.drawable.bg_plan_bronze)
            }
            "SILVER" -> {
                binding.planNameTxt.text = "Срібний план"
                binding.planContainerBtn.setBackgroundResource(R.drawable.bg_plan_silver)
            }
            "GOLD" -> {
                binding.planNameTxt.text = "Золотий план"
                binding.planContainerBtn.setBackgroundResource(R.drawable.bg_plan_gold)
            }
        }

        // Натискання на кнопку відкриває вікно з тарифами
        binding.planContainerBtn.setOnClickListener {
            startActivity(Intent(this, PlanActivity::class.java))
        }
    }

    private fun loadData() {
        binding.profileUsernameEdt.setText(prefs.getCurrentUsername())
        binding.profileEmailEdt.setText(prefs.getCurrentEmail())
        binding.profilePhoneEdt.setText(prefs.getCurrentPhone())
        binding.profilePasswordEdt.setText(prefs.getCurrentPassword())

        binding.profileCardNumberEdt.setText(prefs.getCardNumber())
        binding.profileCardExpiryEdt.setText(prefs.getCardExpiry())
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

        binding.navProfileImg.setOnClickListener { }
        binding.navProfileTxt.setOnClickListener { }
    }
}
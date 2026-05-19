package com.example.coinflow.Activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coinflow.Helper.NetworkUtils
import com.example.coinflow.Helper.PrefsManager
import com.example.coinflow.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Робимо екран на весь розмір
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // --- МАГІЯ ДЛЯ ФОРМАТУВАННЯ НОМЕРА ТЕЛЕФОНУ ---
        binding.phonenumEdt.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                isUpdating = true

                var digitsOnly = s.toString().replace("\\D".toRegex(), "")
                if (digitsOnly.startsWith("0")) digitsOnly = "38$digitsOnly"
                if (digitsOnly.length > 12) digitsOnly = digitsOnly.substring(0, 12)

                val formatted = StringBuilder()
                if (digitsOnly.isNotEmpty()) {
                    formatted.append("+")
                    for (i in digitsOnly.indices) {
                        if (i == 2) formatted.append(" (")
                        if (i == 5) formatted.append(") ")
                        if (i == 8 || i == 10) formatted.append("-")
                        formatted.append(digitsOnly[i])
                    }
                }

                binding.phonenumEdt.setText(formatted.toString())
                binding.phonenumEdt.setSelection(formatted.length)
                isUpdating = false
            }
        })
        // -----------------------------------------------

        // --- ОБРОБКА КНОПКИ РЕЄСТРАЦІЇ ---
        binding.registerBtn.setOnClickListener {
            // Зчитуємо те, що ввів користувач
            val email = binding.emailEdt.text.toString().trim()

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Введіть коректну ел. пошту (наприклад: name@mail.com)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val phone = binding.phonenumEdt.text.toString().trim()
            val username = binding.usernameEdt.text.toString().trim()
            val password = binding.passwordEdt.text.toString().trim()

            // Перевіряємо, чи всі поля заповнені
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Будь ласка, заповніть всі обов'язкові поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Зупиняємо реєстрацію
            }

            // 1. Викликаємо нашого помічника
            val prefs = PrefsManager(this)

            // 2. Зберігаємо нового користувача (створюємо його "сейф")
            prefs.registerUser(username, email, phone, password)

            Toast.makeText(this, "Реєстрація успішна!", Toast.LENGTH_SHORT).show()

            // 3. Перехід на головний екран
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Обробка натискання на текст "Увійдіть" внизу екрана
        binding.loginTxt.setOnClickListener {
            finish()
        }
    }
}
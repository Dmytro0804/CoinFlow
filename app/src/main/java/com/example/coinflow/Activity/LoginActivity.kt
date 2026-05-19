package com.example.coinflow.Activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.coinflow.Helper.NetworkUtils
import com.example.coinflow.Helper.PrefsManager
import com.example.coinflow.databinding.ActivityLoginBinding
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var countDownTimer: CountDownTimer? = null

    // Налаштування безпеки
    private val MAX_ATTEMPTS = 4 // Даємо 4 спроби (після 1-ї помилки напише "залишилося 3")
    private val LOCK_TIME_MILLIS = 5 * 60 * 1000L // 5 хвилин у мілісекундах

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Бронебійний захист: перевірка інтернету
        if (!NetworkUtils.isInternetAvailable(this)) {
            startActivity(Intent(this, NoInternetActivity::class.java))
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Перевіряємо, чи не заблокований користувач з минулого разу
        checkLockoutStatus()

        // Кнопка Входу
        binding.loginBtn.setOnClickListener {
            val username = binding.usernameEdt.text.toString().trim()
            val password = binding.passwordEdt.text.toString().trim()

            val prefs = PrefsManager(this)

            if (prefs.loginUser(username, password)) {
                resetAttempts() // Успішний вхід скидає лічильник помилок
                Toast.makeText(this, "Вітаємо, $username!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                handleFailedAttempt() // Обробляємо помилку
            }
        }

        // Перехід на реєстрацію
        binding.registerTxt.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleFailedAttempt() {
        val sharedPreferences = getSharedPreferences("LoginSecurity", Context.MODE_PRIVATE)
        var attempts = sharedPreferences.getInt("FAILED_ATTEMPTS", 0)
        attempts++
        sharedPreferences.edit().putInt("FAILED_ATTEMPTS", attempts).apply()

        val attemptsLeft = MAX_ATTEMPTS - attempts

        if (attemptsLeft > 0) {
            binding.attemptsTxt.visibility = View.VISIBLE
            binding.attemptsTxt.text = "Залишилося спроб: $attemptsLeft"
            binding.attemptsTxt.setTextColor(Color.WHITE)
            Toast.makeText(this, "Невірний логін або пароль", Toast.LENGTH_SHORT).show()
        } else {
            // Ліміт вичерпано -> Блокуємо на 5 хвилин
            val lockoutTime = System.currentTimeMillis() + LOCK_TIME_MILLIS
            sharedPreferences.edit().putLong("LOCKOUT_END_TIME", lockoutTime).apply()
            startLockoutTimer(lockoutTime)
        }
    }

    private fun checkLockoutStatus() {
        val sharedPreferences = getSharedPreferences("LoginSecurity", Context.MODE_PRIVATE)
        val lockoutEndTime = sharedPreferences.getLong("LOCKOUT_END_TIME", 0)

        if (lockoutEndTime > System.currentTimeMillis()) {
            // Час блокування ще не вийшов (навіть якщо програму перезапустили)
            startLockoutTimer(lockoutEndTime)
        } else {
            // Час вийшов (або блокування не було)
            val attempts = sharedPreferences.getInt("FAILED_ATTEMPTS", 0)
            if (attempts >= MAX_ATTEMPTS) {
                resetAttempts() // Розблоковуємо, якщо 5 хв вже минули
            } else if (attempts > 0) {
                // Показуємо залишок спроб, якщо були невдалі спроби до виходу з додатка
                binding.attemptsTxt.visibility = View.VISIBLE
                binding.attemptsTxt.text = "Залишилося спроб: ${MAX_ATTEMPTS - attempts}"
            }
        }
    }

    private fun startLockoutTimer(endTime: Long) {
        binding.loginBtn.isEnabled = false
        binding.loginBtn.alpha = 0.5f // Робимо кнопку візуально "вимкненою"

        val timeLeft = endTime - System.currentTimeMillis()

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.attemptsTxt.text = String.format(Locale.getDefault(), "Спробуйте через: %02d:%02d", minutes, seconds)
                binding.attemptsTxt.setTextColor(Color.parseColor("#F44336")) // Червоний колір (колір помилки)
                binding.attemptsTxt.visibility = View.VISIBLE
            }

            override fun onFinish() {
                resetAttempts()
            }
        }.start()
    }

    private fun resetAttempts() {
        val sharedPreferences = getSharedPreferences("LoginSecurity", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putInt("FAILED_ATTEMPTS", 0)
            .putLong("LOCKOUT_END_TIME", 0)
            .apply()

        binding.loginBtn.isEnabled = true
        binding.loginBtn.alpha = 1.0f
        binding.attemptsTxt.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        // Обов'язково зупиняємо таймер, щоб не було витоку пам'яті, якщо користувач закриє екран
        countDownTimer?.cancel()
    }
}
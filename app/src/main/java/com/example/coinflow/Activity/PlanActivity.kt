package com.example.coinflow.Activity

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.coinflow.Helper.PlanExpirationReceiver
import com.example.coinflow.Helper.PrefsManager
import com.example.coinflow.R
import com.example.coinflow.databinding.ActivityPlanBinding
import java.util.Locale

class PlanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlanBinding
    private lateinit var prefs: PrefsManager

    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        updateUI()
        setupBottomNavigation()
        startLiveTimer()

        binding.backBtn.setOnClickListener { finish() }

        binding.btnBronze.setOnClickListener { processPlanPurchase("BRONZE", 0.0, "Бронзовий план") }
        binding.btnSilver.setOnClickListener { processPlanPurchase("SILVER", 100.0, "Срібний план") }
        binding.btnGold.setOnClickListener { processPlanPurchase("GOLD", 250.0, "Золотий план") }

        binding.detailBronzeBtn.setOnClickListener {
            showPlanDetailsDialog("Бронзовий план", listOf("Базовий функціонал програми", "Базова статистика акаунта", "Доступ до купівлі та продажу лише по ринковій ціні", "Обмежений доступ до ринку"))
        }

        binding.detailSilverBtn.setOnClickListener {
            showPlanDetailsDialog("Срібний план", listOf("Розширена статистика акаунта", "Повний доступ до купівлі та продажу криптовалюти", "Партнерська програма (CoinFlow Coin)"))
        }

        binding.detailGoldBtn.setOnClickListener {
            showPlanDetailsDialog("Золотий план", listOf("Розширена статистика акаунта", "Відсутність комісії", "Повний доступ до купівлі та продажу", "Партнерська програма (CoinFlow Coin)", "Пріоритет ваших трейдів над іншими", "Доступ до інтелектуального помічника","Золота позначка акаунту"))
        }
    }

    private fun startLiveTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                updateTimerText()
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable!!)
    }

    private fun updateTimerText() {
        val currentPlan = prefs.getCurrentPlan()
        val expTime = prefs.getPlanExpiration()
        val diff = expTime - System.currentTimeMillis()

        binding.timerBronze.visibility = View.GONE
        binding.timerSilver.visibility = View.GONE
        binding.timerGold.visibility = View.GONE

        if (currentPlan == "BRONZE" || expTime == 0L) return

        if (diff <= 0) {
            prefs.setCurrentPlan("BRONZE")
            prefs.setPlanExpiration(0L)
            updateUI()
        } else {
            val days = diff / (1000 * 60 * 60 * 24)
            val hours = (diff / (1000 * 60 * 60)) % 24
            val minutes = (diff / (1000 * 60)) % 60
            val seconds = (diff / 1000) % 60

            val timeString = String.format(Locale.US, "Залишилося: %02d днів %02d год %02d хв %02d сек", days, hours, minutes, seconds)

            if (currentPlan == "SILVER") {
                binding.timerSilver.visibility = View.VISIBLE
                binding.timerSilver.text = timeString
            } else if (currentPlan == "GOLD") {
                binding.timerGold.visibility = View.VISIBLE
                binding.timerGold.text = timeString
            }
        }
    }

    private fun showPlanDetailsDialog(planName: String, features: List<String>) {
        val formattedFeatures = features.joinToString("\n\n") { "• $it" }
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(planName)
            .setMessage(formattedFeatures)
            .setPositiveButton("Зрозуміло", null)
            .create()

        dialog.show()
        val solidBackground = android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.parseColor("#1C1F26")); cornerRadius = 32f
        }
        dialog.window?.setBackgroundDrawable(solidBackground)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
    }

    private fun updateUI() {
        val currentPlan = prefs.getCurrentPlan()

        binding.cardBronze.setBackgroundResource(R.drawable.stroke_bg)
        binding.cardSilver.setBackgroundResource(R.drawable.stroke_bg)
        binding.cardGold.setBackgroundResource(R.drawable.stroke_bg)

        binding.btnBronze.text = "Перейти на цей план"
        binding.btnSilver.text = "Перейти на цей план"
        binding.btnGold.text = "Перейти на цей план"

        binding.btnBronze.setBackgroundResource(R.drawable.green_bg)
        binding.btnSilver.setBackgroundResource(R.drawable.green_bg)
        binding.btnGold.setBackgroundResource(R.drawable.green_bg)

        when (currentPlan) {
            "BRONZE" -> {
                binding.cardBronze.setBackgroundResource(R.drawable.bg_plan_bronze)
                binding.btnBronze.text = "Активний"
                binding.btnBronze.setBackgroundResource(R.drawable.semi_white_bg)
            }
            "SILVER" -> {
                binding.cardSilver.setBackgroundResource(R.drawable.bg_plan_silver)
                binding.btnSilver.text = "Активний"
                binding.btnSilver.setBackgroundResource(R.drawable.semi_white_bg)
            }
            "GOLD" -> {
                binding.cardGold.setBackgroundResource(R.drawable.bg_plan_gold)
                binding.btnGold.text = "Активний"
                binding.btnGold.setBackgroundResource(R.drawable.semi_white_bg)
            }
        }
        updateTimerText()
    }

    private fun processPlanPurchase(planCode: String, cost: Double, planName: String) {
        if (prefs.getCurrentPlan() == planCode) {
            Toast.makeText(this, "Цей план вже активний!", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUah = prefs.getUahBalance()
        if (currentUah < cost) {
            Toast.makeText(this, "Недостатньо коштів (Потрібно $cost ₴)", Toast.LENGTH_LONG).show()
            return
        }

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Оформлення підписки")
            .setMessage("Активувати $planName? З балансу буде списано $cost ₴.")
            .setPositiveButton("Підтвердити") { _, _ ->

                prefs.addUahBalance(-cost)
                prefs.setCurrentPlan(planCode)

                if (cost > 0) {
                    prefs.addTransaction(
                        title = "Підписка: $planName",
                        leftIcon = "wallet", rightIcon = "wallet",
                        amount1 = "- ${String.format(Locale.US, "%,.2f", cost)} ₴",
                        isAmount1Green = false
                    )
                }

                // НАЛАШТОВУЄМО ТАЙМЕР ТА 2 БУДИЛЬНИКИ
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

                // 1. Інтент для закінчення підписки (requestCode = 0)
                val expirationIntent = Intent(this, PlanExpirationReceiver::class.java).apply {
                    putExtra("IS_WARNING", false)
                }
                val expirationPendingIntent = PendingIntent.getBroadcast(
                    this, 0, expirationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 2. Інтент для попередження за 5 днів (requestCode = 1)
                val warningIntent = Intent(this, PlanExpirationReceiver::class.java).apply {
                    putExtra("IS_WARNING", true)
                }
                val warningPendingIntent = PendingIntent.getBroadcast(
                    this, 1, warningIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (planCode == "BRONZE") {
                    prefs.setPlanExpiration(0L)
                    alarmManager.cancel(expirationPendingIntent)
                    alarmManager.cancel(warningPendingIntent)
                } else {
                    // Режим "ПРОДАКШН" (31 день)
                    val expirationTime = System.currentTimeMillis() + (31L * 24 * 60 * 60 * 1000)
                    val warningTime = expirationTime - (5L * 24 * 60 * 60 * 1000)

                    // ДЛЯ ТЕСТУВАННЯ (розкоментуй ці 2 рядки, а верхні 2 - закоментуй):
                    //val expirationTime = System.currentTimeMillis() + (120 * 1000) // кінець через 60 сек
                    //val warningTime = System.currentTimeMillis() + (60 * 1000) // попередження через 30 сек

                    prefs.setPlanExpiration(expirationTime)

                    try {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, warningTime, warningPendingIntent)
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expirationTime, expirationPendingIntent)
                    } catch (e: SecurityException) {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, warningTime, warningPendingIntent)
                        alarmManager.set(AlarmManager.RTC_WAKEUP, expirationTime, expirationPendingIntent)
                    }
                }

                Toast.makeText(this, "План успішно змінено на $planName!", Toast.LENGTH_LONG).show()
                updateUI()
            }
            .setNegativeButton("Скасувати", null)
            .create()

        dialog.show()
        val solidBackground = android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.parseColor("#1C1F26")); cornerRadius = 32f
        }
        dialog.window?.setBackgroundDrawable(solidBackground)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.parseColor("#F44336"))
    }

    override fun onDestroy() {
        super.onDestroy()
        timerRunnable?.let { timerHandler.removeCallbacks(it) }
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
        binding.navProfileImg.setOnClickListener { finish() }
        binding.navProfileTxt.setOnClickListener { finish() }
    }
}
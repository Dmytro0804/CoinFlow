package com.example.coinflow.Activity

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.coinflow.Helper.NetworkUtils
import com.example.coinflow.databinding.ActivityNoInternetBinding

class NoInternetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoInternetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoInternetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        // Блокуємо системну кнопку "Назад", щоб не можна було обійти перевірку
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@NoInternetActivity, "Для продовження потрібен інтернет", Toast.LENGTH_SHORT).show()
            }
        })

        binding.retryBtn.setOnClickListener {
            if (NetworkUtils.isInternetAvailable(this)) {
                // Інтернет з'явився! Просто закриваємо цей екран і автоматично повертаємось туди, де були
                finish()
            } else {
                Toast.makeText(this, "З'єднання досі відсутнє...", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
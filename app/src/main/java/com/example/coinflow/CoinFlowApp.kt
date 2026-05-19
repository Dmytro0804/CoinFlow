package com.example.coinflow

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.coinflow.Activity.NoInternetActivity
import com.example.coinflow.Helper.NetworkUtils

class CoinFlowApp : Application(), Application.ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        monitorNetwork() // Запускаємо постійний моніторинг інтернету
    }

    private fun monitorNetwork() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {

            // Якщо інтернет ЗНИК у будь-який момент
            override fun onLost(network: Network) {
                super.onLost(network)
                Handler(Looper.getMainLooper()).post {
                    val activity = currentActivity
                    if (activity != null && activity !is NoInternetActivity) {
                        // Відкриваємо екран помилки ПОВЕРХ поточного
                        val intent = Intent(activity, NoInternetActivity::class.java)
                        activity.startActivity(intent)
                    }
                }
            }

            // Якщо інтернет З'ЯВИВСЯ
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Handler(Looper.getMainLooper()).post {
                    val activity = currentActivity
                    if (activity is NoInternetActivity) {
                        // Просто закриваємо екран помилки!
                        activity.finish()
                    }
                }
            }
        })
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        // Перевіряємо інтернет при кожному відкритті будь-якого екрана
        if (activity !is NoInternetActivity && !NetworkUtils.isInternetAvailable(this)) {
            val intent = Intent(activity, NoInternetActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity == activity) currentActivity = null
    }

    // Обов'язкові методи інтерфейсу (залишаємо порожніми)
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
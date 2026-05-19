package com.example.coinflow.Helper

import android.content.Context
import android.content.SharedPreferences
import com.example.coinflow.Model.TransactionModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LimitOrder(val username: String, val price: Double, val amount: Double)

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("CoinFlowPrefs", Context.MODE_PRIVATE)

    fun registerUser(username: String, email: String, phone: String, pass: String) {
        val editor = prefs.edit()
        editor.putString("${username}_EMAIL", email)
        editor.putString("${username}_PHONE", phone)
        editor.putString("${username}_PASSWORD", pass)
        editor.putString("CURRENT_USER", username)
        editor.putBoolean("IS_LOGGED_IN", true).apply()
    }

    fun loginUser(username: String, pass: String): Boolean {
        val savedPass = prefs.getString("${username}_PASSWORD", null)
        if (savedPass != null && savedPass == pass) {
            prefs.edit().putString("CURRENT_USER", username).putBoolean("IS_LOGGED_IN", true).apply()
            return true
        }
        return false
    }

    fun logout() = prefs.edit().putString("CURRENT_USER", "").putBoolean("IS_LOGGED_IN", false).apply()
    fun getCurrentUsername(): String = prefs.getString("CURRENT_USER", "Користувач") ?: "Користувач"

    fun updateUserData(email: String, phone: String, pass: String) {
        val user = getCurrentUsername()
        prefs.edit().putString("${user}_EMAIL", email).putString("${user}_PHONE", phone).putString("${user}_PASSWORD", pass).apply()
    }
    fun getCurrentEmail(): String = prefs.getString("${getCurrentUsername()}_EMAIL", "") ?: ""
    fun getCurrentPhone(): String = prefs.getString("${getCurrentUsername()}_PHONE", "") ?: ""
    fun getCurrentPassword(): String = prefs.getString("${getCurrentUsername()}_PASSWORD", "") ?: ""

    // --- БАЛАНСИ (ДОЛАРИ ТА ГРИВНІ) ---
    fun addBalance(amount: Double) {
        val user = getCurrentUsername()
        val newBalance = getBalance() + amount
        prefs.edit().putFloat("${user}_BALANCE_USD", newBalance.toFloat()).apply()
    }

    fun getBalance(): Double = prefs.getFloat("${getCurrentUsername()}_BALANCE_USD", 0.0f).toDouble()

    fun addUahBalance(amount: Double) {
        val user = getCurrentUsername()
        val newBalance = getUahBalance() + amount
        prefs.edit().putFloat("${user}_BALANCE_UAH", newBalance.toFloat()).apply()
    }

    fun getUahBalance(): Double = prefs.getFloat("${getCurrentUsername()}_BALANCE_UAH", 0.0f).toDouble()

    // --- КРИПТО БАЛАНСИ ТА CFC (БОНУСНИЙ ТОКЕН) ---
    fun addCryptoBalance(symbol: String, amount: Double) {
        val user = getCurrentUsername()
        val newBalance = getCryptoBalance(symbol) + amount
        prefs.edit().putFloat("${user}_CRYPTO_${symbol}", newBalance.toFloat()).apply()
    }

    fun getCryptoBalance(symbol: String): Double = prefs.getFloat("${getCurrentUsername()}_CRYPTO_${symbol}", 0.0f).toDouble()

    // --- ПІДПИСКИ (ТЕПЕР ПРИВ'ЯЗАНІ ДО КОРИСТУВАЧА) ---
    fun getCurrentPlan(): String {
        val user = getCurrentUsername()
        // Ключ тепер включає ім'я користувача
        return prefs.getString("${user}_PLAN", "BRONZE") ?: "BRONZE"
    }

    fun setCurrentPlan(planName: String) {
        val user = getCurrentUsername()
        prefs.edit().putString("${user}_PLAN", planName).apply()
    }

    fun setPlanExpiration(timeInMillis: Long) {
        val user = getCurrentUsername()
        prefs.edit().putLong("${user}_PLAN_EXPIRATION", timeInMillis).apply()
    }

    fun getPlanExpiration(): Long {
        val user = getCurrentUsername()
        return prefs.getLong("${user}_PLAN_EXPIRATION", 0L)
    }

    // --- P2P ПЕРЕКАЗИ ---
    fun addBalanceToUser(username: String, amount: Double) {
        val current = prefs.getFloat("${username}_BALANCE_USD", 0.0f).toDouble()
        prefs.edit().putFloat("${username}_BALANCE_USD", (current + amount).toFloat()).apply()
    }

    fun addCryptoBalanceToUser(username: String, symbol: String, amount: Double) {
        val current = prefs.getFloat("${username}_CRYPTO_${symbol}", 0.0f).toDouble()
        prefs.edit().putFloat("${username}_CRYPTO_${symbol}", (current + amount).toFloat()).apply()
    }

    // --- СТАКАН ОРДЕРІВ ---
    fun addLimitOrder(symbol: String, isBuy: Boolean, price: Double, amount: Double) {
        val key = if (isBuy) "ORDERS_BUY_$symbol" else "ORDERS_SELL_$symbol"
        val currentOrdersStr = prefs.getString(key, "") ?: ""
        val newOrderStr = "${getCurrentUsername()},$price,$amount"
        var updatedOrdersStr = if (currentOrdersStr.isEmpty()) newOrderStr else "$currentOrdersStr;$newOrderStr"

        val ordersList = updatedOrdersStr.split(";")
        if (ordersList.size > 50) {
            val ordersToDelete = ordersList.dropLast(50)
            for (order in ordersToDelete) {
                val parts = order.split(",")
                if (parts.size == 3) {
                    val dropUser = parts[0]; val dropPrice = parts[1].toDoubleOrNull() ?: 0.0; val dropAmount = parts[2].toDoubleOrNull() ?: 0.0
                    if (isBuy) addBalanceToUser(dropUser, dropPrice * dropAmount)
                    else addCryptoBalanceToUser(dropUser, symbol, dropAmount)
                }
            }
            updatedOrdersStr = ordersList.takeLast(50).joinToString(";")
        }
        prefs.edit().putString(key, updatedOrdersStr).apply()
    }

    fun removeLimitOrder(symbol: String, isBuy: Boolean, orderOwner: String, price: Double, amount: Double): Boolean {
        val key = if (isBuy) "ORDERS_BUY_$symbol" else "ORDERS_SELL_$symbol"
        val ordersStr = prefs.getString(key, "") ?: ""
        if (ordersStr.isEmpty()) return false

        val ordersList = ordersStr.split(";").toMutableList()
        var indexToRemove = -1
        for (i in ordersList.indices) {
            val parts = ordersList[i].split(",")
            if (parts.size == 3 && parts[0] == orderOwner && parts[1].toDouble() == price && parts[2].toDouble() == amount) {
                indexToRemove = i; break
            }
        }
        if (indexToRemove != -1) {
            ordersList.removeAt(indexToRemove)
            prefs.edit().putString(key, ordersList.joinToString(";")).apply()
            return true
        }
        return false
    }

    fun getLimitOrders(symbol: String, isBuy: Boolean): List<LimitOrder> {
        val key = if (isBuy) "ORDERS_BUY_$symbol" else "ORDERS_SELL_$symbol"
        val ordersStr = prefs.getString(key, "") ?: ""
        if (ordersStr.isEmpty()) return emptyList()

        val ordersList = mutableListOf<LimitOrder>()
        for (o in ordersStr.split(";")) {
            val parts = o.split(",")
            if (parts.size == 3) ordersList.add(LimitOrder(parts[0], parts[1].toDouble(), parts[2].toDouble()))
        }
        if (isBuy) ordersList.sortByDescending { it.price } else ordersList.sortBy { it.price }
        return ordersList
    }

    // --- ІСТОРІЯ ТРАНЗАКЦІЙ ---
    fun addTransaction(
        title: String, leftIcon: String, rightIcon: String,
        amount1: String, isAmount1Green: Boolean,
        amount2: String = "", isAmount2Green: Boolean = false,
        targetUser: String = getCurrentUsername()
    ) {
        val key = "${targetUser}_TRANSACTIONS"
        val currentHistory = prefs.getString(key, "") ?: ""
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val date = sdf.format(Date())
        val timestamp = System.currentTimeMillis()
        val newTx = "$title|||$leftIcon|||$rightIcon|||$amount1|||$isAmount1Green|||$amount2|||$isAmount2Green|||$date|||$timestamp"
        val updatedHistory = if (currentHistory.isEmpty()) newTx else "$currentHistory;;;$newTx"
        prefs.edit().putString(key, updatedHistory).apply()
    }

    fun getTransactions(): List<TransactionModel> {
        val user = getCurrentUsername()
        val key = "${user}_TRANSACTIONS"
        val historyStr = prefs.getString(key, "") ?: ""
        if (historyStr.isEmpty()) return emptyList()
        val list = mutableListOf<TransactionModel>()
        val txStrings = historyStr.split(";;;")
        for (tx in txStrings) {
            val parts = tx.split("|||")
            if (parts.size == 9) {
                list.add(TransactionModel(parts[0], parts[1], parts[2], parts[3], parts[4].toBoolean(), parts[5], parts[6].toBoolean(), parts[7], parts[8].toLongOrNull() ?: 0L))
            }
        }
        list.sortByDescending { it.timestamp }
        return list
    }

    fun clearTransactionsHistory() {
        prefs.edit().remove("${getCurrentUsername()}_TRANSACTIONS").apply()
    }

    // --- ДАНІ КАРТКИ ---
    fun saveCardInfo(cardNumber: String, expiryDate: String) {
        val user = getCurrentUsername()
        prefs.edit().putString("${user}_CARD_NUMBER", cardNumber).putString("${user}_CARD_EXPIRY", expiryDate).apply()
    }

    fun getCardNumber(): String = prefs.getString("${getCurrentUsername()}_CARD_NUMBER", "") ?: ""
    fun getCardExpiry(): String = prefs.getString("${getCurrentUsername()}_CARD_EXPIRY", "") ?: ""

    // --- ЖИВІ ЦІНИ (BINANCE / PRIVAT API) ---
    fun saveLivePrice(symbol: String, price: Double) {
        prefs.edit().putFloat("LIVE_PRICE_$symbol", price.toFloat()).apply()
    }

    fun getLivePrice(symbol: String, defaultPrice: Double): Double {
        val savedPrice = prefs.getFloat("LIVE_PRICE_$symbol", -1f)
        return if (savedPrice == -1f) defaultPrice else savedPrice.toDouble()
    }

    fun saveLiveChangePercent(symbol: String, percent: Double) {
        prefs.edit().putFloat("LIVE_PERCENT_$symbol", percent.toFloat()).apply()
    }

    fun getLiveChangePercent(symbol: String, defaultPercent: Double): Double {
        val savedPercent = prefs.getFloat("LIVE_PERCENT_$symbol", -999f)
        return if (savedPercent == -999f) defaultPercent else savedPercent.toDouble()
    }

    // --- СТАТИСТИКА ТРЕЙДИНГУ ---
    fun recordTrade(isBuy: Boolean, usdAmount: Double, coinSymbol: String) {
        val user = getCurrentUsername()
        if (user.isEmpty()) return
        val editor = prefs.edit()
        if (isBuy) {
            val totalBought = prefs.getFloat("TOTAL_BOUGHT_$user", 0f)
            editor.putFloat("TOTAL_BOUGHT_$user", totalBought + usdAmount.toFloat())
            val maxBuy = prefs.getFloat("MAX_BUY_$user", 0f)
            if (usdAmount > maxBuy) editor.putFloat("MAX_BUY_$user", usdAmount.toFloat())
        } else {
            val totalSold = prefs.getFloat("TOTAL_SOLD_$user", 0f)
            editor.putFloat("TOTAL_SOLD_$user", totalSold + usdAmount.toFloat())
            val maxSell = prefs.getFloat("MAX_SELL_$user", 0f)
            if (usdAmount > maxSell) editor.putFloat("MAX_SELL_$user", usdAmount.toFloat())
        }
        val coinCount = prefs.getInt("COIN_COUNT_${user}_$coinSymbol", 0)
        editor.putInt("COIN_COUNT_${user}_$coinSymbol", coinCount + 1)
        editor.apply()
    }

    fun getTradingStats(): Map<String, Double> {
        val user = getCurrentUsername()
        return mapOf(
            "totalBought" to prefs.getFloat("TOTAL_BOUGHT_$user", 0f).toDouble(),
            "totalSold" to prefs.getFloat("TOTAL_SOLD_$user", 0f).toDouble(),
            "maxBuy" to prefs.getFloat("MAX_BUY_$user", 0f).toDouble(),
            "maxSell" to prefs.getFloat("MAX_SELL_$user", 0f).toDouble()
        )
    }

    fun getFavoriteCoin(): String {
        val user = getCurrentUsername()
        val coins = listOf("BTC", "ETH", "BNB", "SOL", "XRP", "DOGE", "SHIB", "TRX", "USDT")
        var favoriteCoin = "NONE"
        var maxCount = 0
        for (coin in coins) {
            val count = prefs.getInt("COIN_COUNT_${user}_$coin", 0)
            if (count > maxCount) {
                maxCount = count
                favoriteCoin = coin
            }
        }
        return favoriteCoin
    }
}
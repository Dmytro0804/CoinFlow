package com.example.coinflow.Activity

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coinflow.Helper.LimitOrder
import com.example.coinflow.Helper.PrefsManager
import com.example.coinflow.R // Додано імпорт R
import com.example.coinflow.databinding.ActivityAssistantBinding
import java.util.Locale

class AssistantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssistantBinding
    private lateinit var prefs: PrefsManager
    private lateinit var chatAdapter: ChatAdapter
    private val messagesList = mutableListOf<ChatMessage>()

    // Змінні "пам'яті" бота
    private var pendingCoinSymbol: String? = null
    private var pendingActionIsBuy: Boolean? = null
    private var foundOrder: LimitOrder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssistantBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        prefs = PrefsManager(this)

        if (prefs.getCurrentPlan() != "GOLD") {
            Toast.makeText(this, "Помічник доступний лише для Золотого плану!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupRecyclerView()

        binding.backBtn.setOnClickListener { finish() }

        botReply("Привіт, я інтелектуальний помічник 🤖\nОбери потрібну команду або напиши в чаті.")

        binding.btnFunc.setOnClickListener { sendUserMessage("Які функції ти маєш?") }
        binding.btnAdvise.setOnClickListener { sendUserMessage("Що вигідно купити?") }
        binding.btnOffers.setOnClickListener { sendUserMessage("Вигідні пропозиції") }

        binding.sendBtn.setOnClickListener {
            val text = binding.messageEdt.text.toString().trim()
            if (text.isNotEmpty()) {
                sendUserMessage(text)
                binding.messageEdt.text.clear()
            }
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messagesList)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun sendUserMessage(text: String) {
        messagesList.add(ChatMessage(text, false))
        chatAdapter.notifyItemInserted(messagesList.size - 1)
        binding.chatRecyclerView.scrollToPosition(messagesList.size - 1)
        processBotLogic(text)
    }

    private fun botReply(text: String) {
        messagesList.add(ChatMessage(text, true))
        chatAdapter.notifyItemInserted(messagesList.size - 1)
        binding.chatRecyclerView.scrollToPosition(messagesList.size - 1)
    }

    private fun processBotLogic(userMessage: String) {
        val msg = userMessage.lowercase(Locale.getDefault())

        if (foundOrder != null && pendingCoinSymbol != null && pendingActionIsBuy != null) {
            if (msg == "так") {
                executeTrade()
            } else if (msg == "ні") {
                botReply("Транзакцію скасовано. Чим ще можу допомогти?")
                resetBotMemory()
            } else {
                botReply("Будь ласка, напиши 'так' або 'ні'.")
            }
            return
        }

        if (pendingCoinSymbol != null && pendingActionIsBuy == null) {
            if (msg.contains("купівля") || msg.contains("купити")) {
                pendingActionIsBuy = true
                searchBestOrder()
            } else if (msg.contains("продаж") || msg.contains("продати")) {
                pendingActionIsBuy = false
                searchBestOrder()
            } else {
                botReply("Обери: 'Купівля' чи 'Продаж'?")
            }
            return
        }

        when {
            msg.contains("функції") -> {
                botReply("Я можу:\n1. Аналізувати ринок і радити монети.\n2. Шукати P2P ордери інших користувачів.\n3. Проводити миттєві транзакції без комісії (Ви ж VIP 👑).")
            }
            msg.contains("що вигідно купити") || msg.contains("радиш") -> {
                val coins = listOf("BTC", "ETH", "SOL", "BNB")
                val randomCoin = coins.random()
                botReply("Наразі вигідно купити $randomCoin, індикатори показують потенційний ріст найближчими днями 📈. Бажаєте подивитися пропозиції для $randomCoin?")
            }
            msg.contains("пропозиції") -> {
                botReply("Напишіть назву криптовалюти (наприклад: Bitcoin, Ethereum, Solana, CFC), пропозиції якої вас цікавлять.")
            }
            msg.contains("bitcoin") || msg.contains("btc") -> initiateOrderSearch("BTC")
            msg.contains("ethereum") || msg.contains("eth") -> initiateOrderSearch("ETH")
            msg.contains("solana") || msg.contains("sol") -> initiateOrderSearch("SOL")
            msg.contains("cfc") || msg.contains("coinflow") -> initiateOrderSearch("CFC")

            else -> botReply("Я вас не зовсім зрозумів. Спробуйте скористатися кнопками швидких відповідей.")
        }
    }

    private fun initiateOrderSearch(symbol: String) {
        pendingCoinSymbol = symbol
        botReply("Ви обрали $symbol. Вас цікавить купівля чи продаж?")
    }

    private fun searchBestOrder() {
        val symbol = pendingCoinSymbol!!
        val isBuy = pendingActionIsBuy!!

        val orders = prefs.getLimitOrders(symbol, !isBuy)
        val currentUser = prefs.getCurrentUsername()
        val availableOrders = orders.filter { it.username != currentUser }

        if (availableOrders.isEmpty()) {
            val actionStr = if (isBuy) "продаж" else "купівлю"
            botReply("На жаль, зараз немає активних пропозицій на $actionStr $symbol від інших користувачів.")
            resetBotMemory()
            return
        }

        foundOrder = if (isBuy) availableOrders.minByOrNull { it.price } else availableOrders.maxByOrNull { it.price }

        val actionWord = if (isBuy) "продає" else "купує"

        // Виправлено: Отримуємо план іншого юзера напряму з SharedPreferences
        val sharedPreferences = getSharedPreferences("CoinFlowPrefs", Context.MODE_PRIVATE)
        val orderPlan = sharedPreferences.getString("${foundOrder!!.username}_PLAN", "BRONZE") ?: "BRONZE"
        val userWord = if (orderPlan == "GOLD") "👑 ${foundOrder!!.username}" else foundOrder!!.username

        val totalPrice = foundOrder!!.price * foundOrder!!.amount

        botReply("Користувач $userWord $actionWord ${foundOrder!!.amount} $symbol за ціною ${foundOrder!!.price} $.\nРазом сума: ${String.format(Locale.US, "%.2f", totalPrice)} $.\n\nПроводимо транзакцію? (Напишіть 'так' або 'ні')")
    }

    private fun executeTrade() {
        val order = foundOrder!!
        val symbol = pendingCoinSymbol!!
        val isUserBuying = pendingActionIsBuy!!
        val totalUSD = order.price * order.amount

        if (isUserBuying) {
            if (prefs.getBalance() >= totalUSD) {
                if (prefs.removeLimitOrder(symbol, false, order.username, order.price, order.amount)) {
                    prefs.addBalance(-totalUSD)
                    prefs.addCryptoBalance(symbol, order.amount)
                    prefs.addBalanceToUser(order.username, totalUSD)

                    prefs.addTransaction("Купівля (ШІ)", "гаманець", symbol, "- ${String.format(Locale.US, "%.2f", totalUSD)} $", false, "+ ${order.amount} $symbol", true)
                    botReply("✅ Транзакція успішна! Ви купили ${order.amount} $symbol. Комісія: 0$ (Gold Plan).")
                } else botReply("Ордер вже неактуальний (хтось купив швидше).")
            } else botReply("❌ Недостатньо коштів на балансі ($totalUSD $).")
        } else {
            if (prefs.getCryptoBalance(symbol) >= order.amount) {
                if (prefs.removeLimitOrder(symbol, true, order.username, order.price, order.amount)) {
                    prefs.addCryptoBalance(symbol, -order.amount)
                    prefs.addBalance(totalUSD)
                    prefs.addCryptoBalanceToUser(order.username, symbol, order.amount)

                    prefs.addTransaction("Продаж (ШІ)", symbol, "гаманець", "- ${order.amount} $symbol", false, "+ ${String.format(Locale.US, "%.2f", totalUSD)} $", true)
                    botReply("✅ Транзакція успішна! Ви продали $symbol і отримали $totalUSD $. Комісія: 0$ (Gold Plan).")
                } else botReply("Ордер вже неактуальний.")
            } else botReply("❌ Недостатньо $symbol для продажу.")
        }

        resetBotMemory()
    }

    private fun resetBotMemory() {
        pendingCoinSymbol = null
        pendingActionIsBuy = null
        foundOrder = null
    }

    data class ChatMessage(val text: String, val isBot: Boolean)

    inner class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

        inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView as TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val textView = TextView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(16, 16, 16, 16)
                }
                setPadding(32, 24, 32, 24)
                textSize = 16f
                maxWidth = 800
            }
            return ChatViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val message = messages[position]
            holder.textView.text = message.text

            val layoutParams = holder.textView.layoutParams as LinearLayout.LayoutParams

            if (message.isBot) {
                holder.textView.setBackgroundResource(R.drawable.semi_white_bg)
                holder.textView.setTextColor(Color.WHITE)
                layoutParams.gravity = Gravity.START
            } else {
                holder.textView.setBackgroundResource(R.drawable.green_bg)
                holder.textView.setTextColor(Color.WHITE)
                layoutParams.gravity = Gravity.END
            }
            holder.textView.layoutParams = layoutParams
        }

        override fun getItemCount() = messages.size
    }
}
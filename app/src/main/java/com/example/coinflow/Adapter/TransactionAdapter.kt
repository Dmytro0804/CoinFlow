package com.example.coinflow.Adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.coinflow.Model.TransactionModel
import com.example.coinflow.R

class TransactionAdapter(private val items: List<TransactionModel>) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTxt: TextView = view.findViewById(R.id.titleTxt)
        val amount1Txt: TextView = view.findViewById(R.id.amount1Txt)
        val amount2Txt: TextView = view.findViewById(R.id.amount2Txt)
        val dateTxt: TextView = view.findViewById(R.id.dateTxt)
        val iconLeftImg: ImageView = view.findViewById(R.id.iconLeftImg)
        val iconRightImg: ImageView = view.findViewById(R.id.iconRightImg)
        val arrowTxt: TextView = view.findViewById(R.id.arrowTxt) // Наша стрілочка
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.titleTxt.text = item.title
        holder.dateTxt.text = item.date

        // Налаштування першої суми
        holder.amount1Txt.text = item.amount1
        holder.amount1Txt.setTextColor(if (item.isAmount1Green) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))

        // Налаштування другої суми (ховаємо, якщо її немає)
        if (item.amount2.isNotEmpty()) {
            holder.amount2Txt.visibility = View.VISIBLE
            holder.amount2Txt.text = item.amount2
            holder.amount2Txt.setTextColor(if (item.isAmount2Green) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
        } else {
            holder.amount2Txt.visibility = View.GONE
        }

        // --- ЛОГІКА ВІДОБРАЖЕННЯ ІКОНОК ТА СТРІЛОЧКИ ---
        val leftLower = item.leftIcon.lowercase()
        val rightLower = item.rightIcon.lowercase()

        if (leftLower == "card" || rightLower == "card" || leftLower == "картка" || rightLower == "картка") {
            // Це поповнення або зняття фіату (ховаємо стрілку і праву іконку)
            holder.arrowTxt.visibility = View.GONE
            holder.iconRightImg.visibility = View.GONE
            // Залишаємо тільки іконку гаманця
            holder.iconLeftImg.setImageResource(R.drawable.wallet)
        } else {
            // Це крипто операція (показуємо обидві іконки і стрілку)
            holder.arrowTxt.visibility = View.VISIBLE
            holder.iconRightImg.visibility = View.VISIBLE

            holder.iconLeftImg.setImageResource(getIconResId(context, item.leftIcon))
            holder.iconRightImg.setImageResource(getIconResId(context, item.rightIcon))
        }
    }

    override fun getItemCount(): Int = items.size

    // --- ПЕРЕКЛАДАЧ СИМВОЛІВ У НАЗВИ КАРТИНОК ---
    private fun getIconResId(context: Context, iconName: String): Int {
        val mappedName = when (iconName.uppercase()) {
            // Криптовалюти
            "BTC" -> "bitcoin"
            "ETH" -> "etherium"
            "BNB" -> "binance"
            "SOL" -> "solana"    // Додано Solana
            "XRP" -> "ripple"    // Додано Ripple
            "DOGE" -> "dogecoin" // Додано Dogecoin
            "SHIB" -> "shiba"
            "TRX" -> "trox"
            "USDT" -> "tether"   // Додано Tether

            // Системні іконки (з підтримкою української мови)
            "WALLET", "ГАМАНЕЦЬ" -> "wallet"
            "COINS", "МОНЕТИ" -> "trade"
            "CARD", "КАРТКА" -> "card"

            else -> iconName.lowercase()
        }

        val resId = context.resources.getIdentifier(mappedName, "drawable", context.packageName)
        return if (resId != 0) resId else R.drawable.bitcoin // Біткоїн як запасний варіант
    }
}
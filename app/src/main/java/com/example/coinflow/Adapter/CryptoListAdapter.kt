package com.example.coinflow.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.coinflow.Activity.DetailCryptoActivity
import com.example.coinflow.Helper.PrefsManager
import com.example.coinflow.Model.CryptoModel
import com.example.coinflow.R
import com.example.coinflow.databinding.ViewholderWalletBinding
import java.text.DecimalFormat
import java.util.Locale

class CryptoListAdapter(private var items: List<CryptoModel>) : RecyclerView.Adapter<CryptoListAdapter.Viewholder>() {

    class Viewholder(val binding: ViewholderWalletBinding) : RecyclerView.ViewHolder(binding.root)

    private lateinit var context: Context
    private var formatter: DecimalFormat? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewholderWalletBinding.inflate(inflater, parent, false)
        context = parent.context
        formatter = DecimalFormat("###,###,###,###.##")
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val item = items[position]
        val prefs = PrefsManager(context)

        // 1. Отримуємо чисту назву монети
        val coinSymbol = item.ShortSymbol.trim().replace("/USDT", "").replace("/USD", "")

        // 2. Беремо ДИНАМІЧНУ кількість монет з бази даних саме для цього юзера
        val actualAmount = prefs.getCryptoBalance(coinSymbol)

        // 3. Рахуємо ДИНАМІЧНУ вартість у доларах
        val actualAmountDollar = actualAmount * item.Price

        // Заповнюємо стандартні дані
        holder.binding.cryptoNameTxt.text = item.Symbol
        holder.binding.cryptoPricetxt.text = "$" + formatter?.format(item.Price)
        holder.binding.changePercentTxt.text = String.format(Locale.US, "%.2f%%", item.ChangePercent)

        if(item.ChangePercent < 0) {
            holder.binding.changePercentTxt.setTextColor(context.resources.getColor(R.color.red, null))
        } else {
            holder.binding.changePercentTxt.setTextColor(context.resources.getColor(R.color.green, null))
        }

        // 4. Відображаємо динамічні дані (баланс монети)
        val amountFormatter = DecimalFormat("#.######")
        holder.binding.propertySizeTxt.text = amountFormatter.format(actualAmount) + " " + coinSymbol
        holder.binding.propertyAmountTxt.text = "$" + formatter?.format(actualAmountDollar)

        // Логотип
        val drawableResourceId = holder.itemView.resources.getIdentifier(item.SymbolLogo, "drawable", holder.itemView.context.packageName)
        Glide.with(context)
            .load(drawableResourceId)
            .into(holder.binding.logoImg)

        // Клік по монеті
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailCryptoActivity::class.java)
            intent.putExtra("object", item)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateData(newItems: List<CryptoModel>) {
        items = newItems
        notifyDataSetChanged()
    }
}
package com.example.coinflow.Model

import android.os.Parcel
import android.os.Parcelable

data class CryptoModel(
    val Symbol: String = "",
    val ShortSymbol: String = "",
    var Price: Double,
    var ChangePercent: Double,
    val AmountNumber: Double,
    val AmountDollar: Double,
    val SellPrice1: Double,
    val SellPrice2: Double,
    val SellPrice3: Double,
    val SellPrice4: Double,
    val SellPrice5: Double,
    val SellAmount1: Double,
    val SellAmount2: Double,
    val SellAmount3: Double,
    val SellAmount4: Double,
    val SellAmount5: Double,
    val BuyPrice1: Double,
    val BuyPrice2: Double,
    val BuyPrice3: Double,
    val BuyPrice4: Double,
    val BuyPrice5: Double,
    val BuyAmount1: Double,
    val BuyAmount2: Double,
    val BuyAmount3: Double,
    val BuyAmount4: Double,
    val BuyAmount5: Double,
    val Open: Double,
    val Close: Double,
    val High: Double,
    val Low: Double,
    val DailyChange: Double,
    val DailyVol: Double,
    val SymbolLogo: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(Symbol)
        parcel.writeString(ShortSymbol)
        parcel.writeDouble(Price)
        parcel.writeDouble(ChangePercent)
        parcel.writeDouble(AmountNumber)
        parcel.writeDouble(AmountDollar)

        // Sell Prices
        parcel.writeDouble(SellPrice1)
        parcel.writeDouble(SellPrice2)
        parcel.writeDouble(SellPrice3)
        parcel.writeDouble(SellPrice4)
        parcel.writeDouble(SellPrice5)

        // Sell Amounts
        parcel.writeDouble(SellAmount1)
        parcel.writeDouble(SellAmount2)
        parcel.writeDouble(SellAmount3)
        parcel.writeDouble(SellAmount4)
        parcel.writeDouble(SellAmount5)

        // Buy Prices
        parcel.writeDouble(BuyPrice1)
        parcel.writeDouble(BuyPrice2)
        parcel.writeDouble(BuyPrice3)
        parcel.writeDouble(BuyPrice4)
        parcel.writeDouble(BuyPrice5)

        // Buy Amounts
        parcel.writeDouble(BuyAmount1)
        parcel.writeDouble(BuyAmount2)
        parcel.writeDouble(BuyAmount3)
        parcel.writeDouble(BuyAmount4)
        parcel.writeDouble(BuyAmount5)

        // Other stats
        parcel.writeDouble(Open)
        parcel.writeDouble(Close)
        parcel.writeDouble(High)
        parcel.writeDouble(Low)
        parcel.writeDouble(DailyChange)
        parcel.writeDouble(DailyVol)

        parcel.writeString(SymbolLogo)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CryptoModel> {
        override fun createFromParcel(parcel: Parcel): CryptoModel {
            return CryptoModel(parcel)
        }

        override fun newArray(size: Int): Array<CryptoModel?> {
            return arrayOfNulls(size)
        }
    }
}
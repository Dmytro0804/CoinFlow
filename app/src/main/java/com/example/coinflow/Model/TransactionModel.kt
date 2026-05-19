package com.example.coinflow.Model

data class TransactionModel(
    val title: String,           // Назва: "Поповнення", "Зняття", "Конвертація" тощо
    val leftIcon: String,        // Ліва іконка: "card", "wallet", "coins", або "BTC", "ETH"
    val rightIcon: String,       // Права іконка: "card", "wallet", "coins", або "BTC", "ETH"
    val amount1: String,         // Перший рядок суми (напр. "+ 96,000.00 $")
    val isAmount1Green: Boolean, // True = зелений текст, False = червоний
    val amount2: String,         // Другий рядок суми (напр. "- 5 ETH" або пустий "")
    val isAmount2Green: Boolean, // Колір для другого рядка
    val date: String,            // Дата: "07.04.2026"
    val timestamp: Long          // Точний час (щоб найновіші рахунки завжди були зверху)
)
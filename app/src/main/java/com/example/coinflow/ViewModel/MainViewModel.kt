package com.example.coinflow.ViewModel

import androidx.lifecycle.ViewModel
import com.example.coinflow.Repository.MainRepository

class MainViewModel
    (val repository: MainRepository): ViewModel(){
    constructor():this(MainRepository())

    fun loadData()=repository.items
}
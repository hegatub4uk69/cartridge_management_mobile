package com.mobile.cartridgemanagement.ui.cartridge_actions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CartridgeActionsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is cartridge actions Fragment"
    }

    val text: LiveData<String> = _text
}
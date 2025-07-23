package com.mobile.cartridgemanagement.ui.operations

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OperationsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is operations Fragment"
    }
    val text: LiveData<String> = _text
}
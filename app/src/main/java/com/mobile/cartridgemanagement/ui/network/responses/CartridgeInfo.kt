package com.mobile.cartridgemanagement.ui.network.responses
import java.math.BigInteger

data class CartridgeInfo(
    val id: BigInteger,
    val model: String,
    val department: String,
    val department_date: String
)

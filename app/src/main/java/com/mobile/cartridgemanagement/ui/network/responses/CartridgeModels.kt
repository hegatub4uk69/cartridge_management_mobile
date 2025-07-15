package com.mobile.cartridgemanagement.ui.network.responses

data class CartridgeModels(
    val result: List<Item>
)

data class Item(
    val id: Int,
    val name: String
)
package com.mobile.cartridgemanagement.ui.network.responses

data class Departments(
    val result: List<DepartmentItem>
)

data class DepartmentItem(
    val id: Int,
    val name: String
)
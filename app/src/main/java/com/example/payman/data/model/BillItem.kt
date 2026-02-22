package com.example.payman.data.model

import java.util.*

data class BillItem(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var unitPrice: Double,
    var quantity: Int = 1,
    val assignedPersonIds: List<String> = emptyList()
) {
    val totalPrice: Double get() = unitPrice * quantity
}

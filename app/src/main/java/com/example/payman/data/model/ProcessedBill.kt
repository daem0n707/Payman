package com.example.payman.data.model

import android.graphics.Bitmap
import android.net.Uri
import java.util.*

data class ProcessedBill(
    val id: String = UUID.randomUUID().toString(),
    val restaurantName: String,
    var payeeName: String = "",
    var payeeId: String? = null,
    val items: MutableList<BillItem>,
    var tax: Double,
    var serviceCharge: Double,
    var miscFees: Double,
    var bookingFees: Double = 0.0,
    var discountPercentage: Double = 0.0,
    var discountAmount: Double = 0.0,
    var dinecashDeduction: Double = 0.0,
    var isDiscountApplied: Boolean = false,
    var isDiscountFixedAmount: Boolean = false,
    var isSwiggyHdfcApplied: Boolean = false,
    val imageUri: Uri? = null,
    val bitmap: Bitmap? = null,
    val participatingPersonIds: List<String> = emptyList(),
    var isProcessing: Boolean = false,
    var sectionName: String? = "General",
    val timestamp: Long = System.currentTimeMillis(),
    val isManualExpense: Boolean = false
) {
    val dineoutSavings: Double get() {
        if (!isDiscountApplied) return 0.0
        return if (isDiscountFixedAmount) {
            discountAmount
        } else {
            val foodItemsSum = items.sumOf { it.totalPrice }
            (foodItemsSum + tax) * (discountPercentage / 100.0)
        }
    }

    val hdfcDiscountValue: Double get() {
        if (!isSwiggyHdfcApplied) return 0.0
        val foodItemsSum = items.sumOf { it.totalPrice }
        val hdfcBase = (foodItemsSum + tax + serviceCharge + miscFees) - bookingFees - dineoutSavings - dinecashDeduction.coerceAtLeast(0.0)
        return hdfcBase.coerceAtLeast(0.0) * 0.10
    }

    val totalAmount: Double get() {
        if (isManualExpense) return items.sumOf { it.totalPrice }

        val foodItemsSum = items.sumOf { it.totalPrice }
        val totalBeforeHdfc = (foodItemsSum + tax + serviceCharge + miscFees + bookingFees) - dineoutSavings - dinecashDeduction.coerceAtLeast(0.0)
        return totalBeforeHdfc - hdfcDiscountValue
    }
}

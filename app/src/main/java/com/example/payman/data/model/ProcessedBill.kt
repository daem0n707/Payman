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
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalAmount: Double get() {
        val baseAmount = items.sumOf { it.totalPrice } + tax + serviceCharge
        val discountedAmount = if (isDiscountApplied) {
            if (isDiscountFixedAmount) {
                baseAmount - discountAmount
            } else {
                baseAmount * (1 - discountPercentage / 100.0)
            }
        } else baseAmount
        
        val afterMisc = discountedAmount + miscFees + bookingFees
        // Dinecash is subtracted BEFORE applying the 10% offer
        val beforeSwiggy = afterMisc - dinecashDeduction.coerceAtLeast(0.0)

        return if (isSwiggyHdfcApplied) {
            beforeSwiggy * 0.90
        } else {
            beforeSwiggy
        }
    }
}

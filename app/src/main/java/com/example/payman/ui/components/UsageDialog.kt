package com.example.payman.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UsageDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF36454F),
        title = { Text("Usage Guide", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                item {
                    UsageSection("Interactive Tour", "New to the app? Open the sidebar menu and tap 'Start Tour' to get a guided walkthrough of all the key features, from bill scanning to advanced splitting logic.")
                    UsageSection("Manual Expenses", "Need to split travel or other non-bill costs? Click the menu icon (three dots) on any Section Header and select 'Add Manual Expense'. You can specify a single amount, a payee, and multiple payers.")
                    UsageSection("Reorder Everything", "You can reorder individual bills within a section or move them between sections by long-pressing. You can also reorder entire sections by long-pressing their headers.")
                    UsageSection("Smart Split", "Perform a Smart Split to simplify debts across an entire section. Toggle the 'List' icon to see a detailed breakdown of every item and offer for every person. Clicking the 'Copy' icon while in detailed view will copy the full breakdown to your clipboard.")
                    UsageSection("Payee Selection", "Click on the Restaurant Name in the Bill Details screen to select a Payee. The Payee is the person who initially paid the bill. Other participants will owe their shares to this person.")
                    UsageSection("Lengthy Bills", "For long receipts that don't fit in one photo, capture multiple images. The app will merge the text and use AI to de-duplicate overlapping items.")
                    UsageSection("Split Methods (Misc Fees)", "Choose how to distribute non-item costs (Misc Fees, Booking Fees):\n" +
                            "• Equal: Everyone pays the same share.\n" +
                            "• Economically Fair: Fees are proportional to food consumption.\n" +
                            "• Balanced: A 50-50 mix of both.")
                    UsageSection("Precision Splitting", "The split result will warn you if any items are unassigned. These items are excluded from individual shares to ensure the math remains accurate.")
                    UsageSection("Recycle Bin", "Deleted bills are moved to the Recycle Bin and held for 30 days before permanent removal.")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))) {
                Text("Got it", color = Color.Black)
            }
        }
    )
}

@Composable
fun UsageSection(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF1DB954), fontSize = 16.sp)
        Text(description, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

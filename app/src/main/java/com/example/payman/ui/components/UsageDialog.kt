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
                    UsageSection("Payee Selection", "Click on the Restaurant Name in the Bill Details screen to select a Payee. The Payee is the person who initially paid the bill. Other participants will owe their shares to this person. The Payee name shows up in the final bill split text that is copied.")
                    UsageSection("Smart Split", "This option in each section header performs a Smart Split across all bills in that section. It simplifies debts (e.g., if A owes B ₹10 and B owes A ₹4, A simply owes B ₹6) and tracks who owes what based on the designated Payees.")
                    UsageSection("How Split Works", "By default, items are split among all participating people. You can assign specific people to an item to split only that item's cost (and quantity) among them.")
                    UsageSection("Split Methods (Misc Fees)", "When viewing the split result, you can choose how to distribute non-item costs (Misc Fees, Booking Fees):\n" +
                            "• Equal: Everyone pays the same share of fees.\n" +
                            "• Economically Fair: Fees are proportional to food consumption. This protects those who ordered less from paying high flat fees.\n" +
                            "• Balanced: A 50-50 mix of both methods.")
                    UsageSection("Discounts & Dinecash", "Discounts (Fixed or Percentage) are applied to the subtotal of items, taxes, and service charges. Dinecash is a total fixed amount deducted from the bill total and is split equally among all participating people. Note: Dinecash is subtracted BEFORE the Swiggy HDFC Card 10% cashback is applied in case this is toggled ON.")
                    UsageSection("Misc and Booking Fees", "Misc Fees and Booking Fees are added to the final total. Booking fees are intended for platform or convenience charges added manually by you.")
                    UsageSection("Recycle Bin", "Deleted bills are moved to the Recycle Bin and can be restored. They are permanently deleted after 30 days.")
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

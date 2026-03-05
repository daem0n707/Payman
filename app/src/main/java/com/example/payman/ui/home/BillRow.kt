package com.example.payman.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.payman.data.model.ProcessedBill

@Composable
fun BillRow(
    bill: ProcessedBill,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (bill.isManualExpense) Color(0xFF232D34) else Color(0xFF2B373E)
        ),
        border = if (bill.isManualExpense) BorderStroke(1.dp, Color(0xFF1DB954).copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(60.dp)) {
                if (bill.isManualExpense) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1DB954).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description, 
                            contentDescription = null, 
                            tint = Color(0xFF1DB954), 
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    if (bill.imageUri != null) {
                        AsyncImage(
                            model = bill.imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else if (bill.bitmap != null) {
                        Image(
                            bitmap = bill.bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Gray, RoundedCornerShape(8.dp)))
                    }
                }
                
                if (bill.isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)), 
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF1DB954), strokeWidth = 2.dp)
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.restaurantName, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!bill.isProcessing) {
                        val participantCount = if (bill.isManualExpense) bill.participatingPersonIds.size else bill.items.size
                        val unit = if (bill.isManualExpense) "people" else "items"
                        Text("$participantCount $unit", fontSize = 14.sp, color = Color.Gray)
                        if (bill.payeeName.isNotBlank()) {
                            Text(" • ${bill.payeeName}", fontSize = 14.sp, color = Color.Gray)
                        }
                    } else {
                        Text("AI is parsing bill...", fontSize = 14.sp, color = Color(0xFF1DB954))
                    }
                }
            }
            if (!bill.isProcessing) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₹${String.format("%.2f", bill.totalAmount)}",
                        color = Color(0xFF1DB954),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Bill", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

package com.example.payman.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.payman.data.model.Person
import com.example.payman.data.model.ProcessedBill
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingStatsUI(
    bills: List<ProcessedBill>,
    people: List<Person>,
    onDismiss: () -> Unit
) {
    var selectedPerson by remember { mutableStateOf<Person?>(null) }
    var showPersonSelector by remember { mutableStateOf(false) }

    val metrics = remember(bills, selectedPerson) {
        val filteredBills = if (selectedPerson == null) {
            bills
        } else {
            bills.filter { it.participatingPersonIds.contains(selectedPerson?.id) }
        }

        val totalSpent = if (selectedPerson == null) {
            filteredBills.sumOf { it.totalAmount }
        } else {
            filteredBills.sumOf { calculatePersonShareInBill(it, selectedPerson!!.id) }
        }

        val billCount = filteredBills.size
        val avgPerBill = if (billCount > 0) totalSpent / billCount else 0.0

        val statsByMonth = filteredBills.groupBy { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            val month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            val year = cal.get(Calendar.YEAR)
            "$month $year"
        }.mapValues { entry ->
            val monthBills = entry.value
            val total = if (selectedPerson == null) {
                monthBills.sumOf { it.totalAmount }
            } else {
                monthBills.sumOf { calculatePersonShareInBill(it, selectedPerson!!.id) }
            }

            // Group by category/section
            val byCategory = monthBills.groupBy { it.sectionName ?: "General" }
                .mapValues { sectionEntry -> 
                    if (selectedPerson == null) {
                        sectionEntry.value.sumOf { it.totalAmount }
                    } else {
                        sectionEntry.value.sumOf { calculatePersonShareInBill(it, selectedPerson!!.id) }
                    }
                }

            // Categorized breakdown for selected person
            val categorizedItems = if (selectedPerson != null) {
                monthBills.groupBy { it.sectionName ?: "General" }.mapValues { sectionEntry ->
                    sectionEntry.value.map { bill ->
                        val share = calculatePersonShareInBill(bill, selectedPerson!!.id)
                        val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(bill.timestamp))
                        Triple(bill.restaurantName, date, share)
                    }
                }
            } else emptyMap()
            
            Triple(total, byCategory, categorizedItems)
        }.toList().sortedByDescending { 
            val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            format.parse(it.first)?.time ?: 0L
        }

        Triple(totalSpent, avgPerBill, statsByMonth)
    }

    val (totalSpent, avgPerBill, statsByMonth) = metrics

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Spending Stats", fontSize = 18.sp)
                        Text(
                            text = if (selectedPerson == null) "All People" else "For ${selectedPerson!!.name}",
                            fontSize = 12.sp,
                            color = Color(0xFF1DB954)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { showPersonSelector = true }) {
                        Icon(Icons.Default.Person, contentDescription = "Select Person", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF36454F), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF36454F))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B373E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Total Spent", color = Color.Gray, fontSize = 12.sp)
                                Text("₹${String.format(Locale.US, "%.2f", totalSpent)}", color = Color(0xFF1DB954), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Avg / Bill", color = Color.Gray, fontSize = 12.sp)
                                Text("₹${String.format(Locale.US, "%.2f", avgPerBill)}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            if (statsByMonth.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data available for this selection", color = Color.LightGray)
                    }
                }
            } else {
                item {
                    Text("Monthly Breakdown", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 8.dp))
                }

                items(statsByMonth) { (monthYear, data) ->
                    val (monthTotal, byCategory, categorizedItems) = data
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B373E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(monthYear, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1DB954))
                                Text("₹${String.format(Locale.US, "%.2f", monthTotal)}", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Category progress bars
                            byCategory.forEach { (category, amount) ->
                                val percentage = if (monthTotal > 0) (amount / monthTotal).toFloat() else 0f
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(category, color = Color.White, fontSize = 13.sp)
                                        Text("₹${String.format(Locale.US, "%.2f", amount)}", color = Color.LightGray, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { percentage },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = Color(0xFF1DB954),
                                        trackColor = Color.Gray.copy(alpha = 0.2f)
                                    )
                                }
                            }

                            if (selectedPerson != null && categorizedItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Bill History", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                                
                                categorizedItems.forEach { (category, items) ->
                                    if (items.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(category, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1DB954).copy(alpha = 0.7f))
                                        items.forEach { (name, date, amount) ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                                    Text(name, color = Color.White, fontSize = 14.sp)
                                                    Text(date, color = Color.Gray, fontSize = 11.sp)
                                                }
                                                Text("₹${String.format(Locale.US, "%.2f", amount)}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPersonSelector) {
        AlertDialog(
            onDismissRequest = { showPersonSelector = false },
            containerColor = Color(0xFF36454F),
            title = { Text("Select Person", color = Color.White) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        ListItem(
                            headlineContent = { Text("All People", color = Color.White) },
                            modifier = Modifier.clickable { 
                                selectedPerson = null
                                showPersonSelector = false
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                    items(people) { person ->
                        ListItem(
                            headlineContent = { Text(person.name, color = Color.White) },
                            modifier = Modifier.clickable { 
                                selectedPerson = person
                                showPersonSelector = false
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPersonSelector = false }) {
                    Text("Close", color = Color(0xFF1DB954))
                }
            }
        )
    }
}

private fun calculatePersonShareInBill(bill: ProcessedBill, personId: String): Double {
    if (!bill.participatingPersonIds.contains(personId)) return 0.0

    val discountMultiplier = if (bill.isDiscountApplied && !bill.isDiscountFixedAmount) (1 - bill.discountPercentage / 100.0) else 1.0
    
    var personalItemTotal = 0.0
    bill.items.forEach { item ->
        val itemAssignedIds = item.assignedPersonIds.filter { bill.participatingPersonIds.contains(it) }
        val splitAmong = itemAssignedIds.ifEmpty { bill.participatingPersonIds }
        
        if (splitAmong.contains(personId)) {
            // Updated to use the correct logic: total quantity / number of assigned people
            val personalQuantity = item.quantity.toDouble() / splitAmong.size
            personalItemTotal += (item.unitPrice * personalQuantity * discountMultiplier)
        }
    }
    
    val baseExtraFees = bill.tax + bill.serviceCharge
    var extraPerPerson = (baseExtraFees * discountMultiplier) / bill.participatingPersonIds.size
    
    if (bill.isDiscountApplied && bill.isDiscountFixedAmount) {
        extraPerPerson -= bill.discountAmount / bill.participatingPersonIds.size
    }
    
    extraPerPerson += bill.miscFees / bill.participatingPersonIds.size
    
    val totalBeforeDinecash = personalItemTotal + extraPerPerson
    val shareAfterDinecash = totalBeforeDinecash - (bill.dinecashDeduction / bill.participatingPersonIds.size)
    
    return if (bill.isSwiggyHdfcApplied) {
        shareAfterDinecash * 0.90
    } else {
        shareAfterDinecash
    }
}

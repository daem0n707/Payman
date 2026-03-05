package com.example.payman.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.payman.data.local.LogEntry
import com.example.payman.data.model.Person
import com.example.payman.data.model.ProcessedBill
import com.example.payman.ui.home.BillRow
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalculatorDialog(onDismiss: () -> Unit) {
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var operator by remember { mutableStateOf<String?>(null) }
    var shouldResetDisplayNextTime by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF36454F),
        title = { Text("Calculator", color = Color.White) },
        text = {
            Column(horizontalAlignment = Alignment.End) {
                if (expression.isNotBlank()) {
                    Text(text = expression, fontSize = 14.sp, color = Color.Gray)
                }
                Text(
                    text = display,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = Color.White
                )
                val buttons = listOf(
                    listOf("7", "8", "9", "/"),
                    listOf("4", "5", "6", "*"),
                    listOf("1", "2", "3", "-"),
                    listOf("C", "0", "=", "+")
                )
                buttons.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { btn ->
                            Button(
                                onClick = {
                                    when (btn) {
                                        in "0123456789" -> {
                                            if (display == "0" || shouldResetDisplayNextTime) {
                                                display = btn
                                            } else {
                                                display += btn
                                            }
                                            shouldResetDisplayNextTime = false
                                        }
                                        "C" -> {
                                            display = "0"
                                            expression = ""
                                            operand1 = null
                                            operator = null
                                            shouldResetDisplayNextTime = false
                                        }
                                        "=" -> {
                                            if (operand1 != null && operator != null) {
                                                val val2 = display.toDoubleOrNull() ?: 0.0
                                                val res = when (operator) {
                                                    "+" -> operand1!! + val2
                                                    "-" -> operand1!! - val2
                                                    "*" -> operand1!! * val2
                                                    "/" -> if (val2 != 0.0) operand1!! / val2 else 0.0
                                                    else -> 0.0
                                                }
                                                expression = ""
                                                display = if (res % 1.0 == 0.0) res.toInt().toString() else String.format(Locale.US, "%.2f", res)
                                                operand1 = null
                                                operator = null
                                                shouldResetDisplayNextTime = true
                                            }
                                        }
                                        else -> {
                                            operand1 = display.toDoubleOrNull()
                                            operator = btn
                                            expression = "$display $btn"
                                            shouldResetDisplayNextTime = true
                                        }
                                    }
                                },
                                modifier = Modifier.size(60.dp).padding(4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (btn in "+-*/=") Color(0xFF1DB954) else Color(0xFF2B373E)
                                )
                            ) {
                                Text(btn, fontSize = 20.sp, color = if (btn in "+-*/=") Color.Black else Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = Color(0xFF1DB954)) } }
    )
}

@Composable
fun SmartSplitDialog(bills: List<ProcessedBill>, people: List<Person>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var isDetailedView by remember { mutableStateOf(false) }

    // Map: Payer -> (Payee -> Amount)
    val netDebts = mutableMapOf<String, MutableMap<String, Double>>()
    
    // Structure for detailed copy: PersonId -> (PayeeId -> (RestaurantName -> Details))
    data class BillDetails(
        val items: MutableList<String> = mutableListOf(),
        val offers: MutableList<String> = mutableListOf(),
        var total: Double = 0.0
    )
    val detailedCopyMap = mutableMapOf<String, MutableMap<String, MutableMap<String, BillDetails>>>()
    
    // Track unassigned items: RestaurantName -> List<ItemDescription>
    val unassignedItems = mutableMapOf<String, MutableList<String>>()

    bills.forEach { bill ->
        val payeeId = bill.payeeId ?: return@forEach
        val participatingIds = bill.participatingPersonIds
        if (participatingIds.isEmpty()) return@forEach

        val discountMultiplier = if (bill.isDiscountApplied && !bill.isDiscountFixedAmount) (1 - bill.discountPercentage / 100.0) else 1.0
        
        // Calculate shares for this specific bill
        val tempShares = mutableMapOf<String, Double>()
        bill.items.forEach { item ->
            val itemAssignedIds = item.assignedPersonIds.filter { participatingIds.contains(it) }
            
            if (itemAssignedIds.isEmpty()) {
                // Unassigned item
                val list = unassignedItems.getOrPut(bill.restaurantName) { mutableListOf() }
                val qtyText = if (item.quantity.toDouble() % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.1f", item.quantity.toDouble())
                list.add("${item.name} (x$qtyText) ₹${String.format(Locale.US, "%.2f", item.totalPrice)}")
                return@forEach
            }
            
            val splitAmong = itemAssignedIds
            val personalQuantity = item.quantity.toDouble() / splitAmong.size
            val share = (item.unitPrice * personalQuantity * discountMultiplier)
            
            splitAmong.forEach { id -> 
                if (id != payeeId) {
                    tempShares[id] = (tempShares[id] ?: 0.0) + share
                    
                    val pMap = detailedCopyMap.getOrPut(id) { mutableMapOf() }
                    val payeeMap = pMap.getOrPut(payeeId) { mutableMapOf() }
                    val bDetails = payeeMap.getOrPut(bill.restaurantName) { BillDetails() }
                    
                    val qtyText = if (personalQuantity % 1.0 == 0.0) personalQuantity.toInt().toString() else String.format(Locale.US, "%.1f", personalQuantity)
                    bDetails.items.add("${item.name} (x$qtyText) ₹${String.format(Locale.US, "%.2f", share)}")
                    bDetails.total += share
                }
            }
        }
        
        val baseExtraFees = bill.tax + bill.serviceCharge
        var extraPerPerson = (baseExtraFees * discountMultiplier) / participatingIds.size
        
        if (bill.isDiscountApplied && bill.isDiscountFixedAmount) {
            extraPerPerson -= bill.discountAmount / participatingIds.size
        }
        
        extraPerPerson += bill.miscFees / participatingIds.size
        extraPerPerson += bill.bookingFees / participatingIds.size
        
        val dinecashShare = bill.dinecashDeduction / participatingIds.size
        val shareBeforeSwiggy = extraPerPerson - dinecashShare
        val finalExtraPerPerson = if (bill.isSwiggyHdfcApplied) shareBeforeSwiggy * 0.90 else shareBeforeSwiggy

        participatingIds.forEach { id -> 
            if (id != payeeId) {
                val itemShareInThisBill = (tempShares[id] ?: 0.0)
                val finalItemShare = if (bill.isSwiggyHdfcApplied) itemShareInThisBill * 0.90 else itemShareInThisBill
                
                tempShares[id] = finalItemShare + finalExtraPerPerson
                
                val pMap = detailedCopyMap.getOrPut(id) { mutableMapOf() }
                val payeeMap = pMap.getOrPut(payeeId) { mutableMapOf() }
                val bDetails = payeeMap.getOrPut(bill.restaurantName) { BillDetails() }
                
                if (finalExtraPerPerson != 0.0) {
                    bDetails.offers.add("Fees/Tax/Disc/Dinecash ₹${String.format(Locale.US, "%.2f", finalExtraPerPerson)}")
                    bDetails.total += finalExtraPerPerson
                }
            }
        }

        tempShares.forEach { (payerId, amount) ->
            if (payerId != payeeId) {
                val payerMap = netDebts.getOrPut(payerId) { mutableMapOf() }
                payerMap[payeeId] = (payerMap[payeeId] ?: 0.0) + amount
            }
        }
    }

    // Simplify debts and track deductions
    data class SimplifiedDebt(
        val otherId: String,
        val netAmount: Double, // current person owes otherId if positive, or gets if negative
        val deduction: Double // amount subtracted due to reverse debt
    )
    val simplified = mutableMapOf<String, MutableList<SimplifiedDebt>>()
    val allPersonIds = (netDebts.keys + netDebts.values.flatMap { it.keys }).distinct()
    
    val personIdList = allPersonIds.sorted()
    for (i in personIdList.indices) {
        for (j in i + 1 until personIdList.size) {
            val p1 = personIdList[i]
            val p2 = personIdList[j]
            val p1OwesP2 = netDebts[p1]?.get(p2) ?: 0.0
            val p2OwesP1 = netDebts[p2]?.get(p1) ?: 0.0
            
            val net = p1OwesP2 - p2OwesP1
            val deduction = Math.min(p1OwesP2, p2OwesP1)
            
            if (net > 0) {
                simplified.getOrPut(p1) { mutableListOf() }.add(SimplifiedDebt(p2, net, deduction))
                simplified.getOrPut(p2) { mutableListOf() }.add(SimplifiedDebt(p1, -net, deduction))
            } else if (net < 0) {
                simplified.getOrPut(p2) { mutableListOf() }.add(SimplifiedDebt(p1, -net, deduction))
                simplified.getOrPut(p1) { mutableListOf() }.add(SimplifiedDebt(p2, net, deduction))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF36454F),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Smart Split", color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = { isDetailedView = !isDetailedView }) {
                    Icon(if (isDetailedView) Icons.Default.Description else Icons.AutoMirrored.Filled.List, null, tint = Color.White)
                }
                IconButton(onClick = {
                    val summaryText = buildString {
                        if (isDetailedView) {
                            append("*Smart Split Detailed View*\n\n")
                            detailedCopyMap.forEach { (payerId, payees) ->
                                val payerName = people.find { it.id == payerId }?.name ?: "Unknown"
                                append("*$payerName*\n")
                                payees.forEach { (payeeId, restaurantBills) ->
                                    val payeeName = people.find { it.id == payeeId }?.name ?: "Unknown"
                                    append("  Owes $payeeName:\n")
                                    restaurantBills.forEach { (restName, details) ->
                                        append("    _${restName}_\n")
                                        details.items.forEach { append("      • $it\n") }
                                        details.offers.forEach { append("      • $it\n") }
                                        append("      *Total:* ₹${String.format(Locale.US, "%.2f", details.total)}\n")
                                    }
                                }
                                append("\n")
                            }
                        } else {
                            append("*Smart Split Summary*\n\n")
                            simplified.forEach { (p1, others) ->
                                val name1 = people.find { it.id == p1 }?.name ?: "Unknown"
                                others.forEach { debt ->
                                    if (debt.netAmount > 0) {
                                        val name2 = people.find { it.id == debt.otherId }?.name ?: "Unknown"
                                        append("$name1 owes $name2: ₹${String.format(Locale.US, "%.2f", debt.netAmount)}")
                                        if (debt.deduction > 0) {
                                            append(" (Net after ₹${String.format(Locale.US, "%.2f", debt.deduction)} deduction)")
                                        }
                                        append("\n")
                                    }
                                }
                            }
                        }
                        
                        if (unassignedItems.isNotEmpty()) {
                            append("\n*Unassigned Items*\n")
                            unassignedItems.forEach { (bill, items) ->
                                append("$bill:\n")
                                items.forEach { append("  - $it\n") }
                            }
                        }
                    }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Smart Split", summaryText))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, null, tint = Color.White)
                }
            }
        },
        text = {
            LazyColumn {
                if (simplified.isEmpty() && unassignedItems.isEmpty()) {
                    item { Text("No outstanding debts!", color = Color.Gray) }
                } else {
                    simplified.forEach { (p1, others) ->
                        val name1 = people.find { it.id == p1 }?.name ?: "Unknown"
                        item { 
                            Text(name1, fontWeight = FontWeight.Bold, color = Color(0xFF1DB954), modifier = Modifier.padding(top = 8.dp))
                        }
                        items(others) { debt ->
                            val name2 = people.find { it.id == debt.otherId }?.name ?: "Unknown"
                            Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (debt.netAmount > 0) "owes $name2" else "gets from $name2", color = Color.White)
                                    Column(horizontalAlignment = Alignment.End) {
                                        val isNetGetsFromOwing = debt.netAmount < 0 && debt.deduction > 0
                                        Text(
                                            "₹${String.format(Locale.US, "%.2f", Math.abs(debt.netAmount))}", 
                                            fontWeight = FontWeight.Bold, 
                                            color = if (isNetGetsFromOwing) Color(0xFFCF6679) else Color.White
                                        )
                                        if (debt.deduction > 0) {
                                            Text(
                                                if (debt.netAmount > 0) "(-₹${String.format(Locale.US, "%.2f", debt.deduction)})"
                                                else "(after ₹${String.format(Locale.US, "%.2f", debt.deduction)} deduction)",
                                                fontSize = 10.sp,
                                                color = if (debt.netAmount < 0) Color(0xFFCF6679) else Color.Gray
                                            )
                                        }
                                    }
                                }
                                if (isDetailedView && debt.netAmount > 0) {
                                    detailedCopyMap[p1]?.get(debt.otherId)?.forEach { (restName, details) ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(restName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                        details.items.forEach { Text("• $it", fontSize = 11.sp, color = Color.Gray) }
                                        details.offers.forEach { Text("• $it", fontSize = 11.sp, color = Color(0xFF1DB954)) }
                                        Text("Total: ₹${String.format(Locale.US, "%.2f", details.total)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    
                    if (unassignedItems.isNotEmpty()) {
                        item {
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 12.dp))
                            Text("Unassigned Items", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        unassignedItems.forEach { (billName, items) ->
                            item {
                                Text(billName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.LightGray, modifier = Modifier.padding(top = 8.dp))
                            }
                            items(items) { detail ->
                                Text("• $detail", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))) { Text("Close", color = Color.Black) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinUI(
    deletedBills: Map<Long, ProcessedBill>,
    onRestore: (ProcessedBill) -> Unit,
    onPermanentDelete: (String) -> Unit,
    onEmptyBin: () -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (deletedBills.isNotEmpty()) {
                        IconButton(onClick = onEmptyBin) { Icon(Icons.Default.Delete, contentDescription = "Empty Bin", tint = Color(0xFFCF6679)) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF36454F), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF36454F)).padding(padding).padding(16.dp)) {
            Text("Bills here will be permanently deleted after 30 days.", color = Color.LightGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            if (deletedBills.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Recycle bin is empty", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(deletedBills.toList()) { (timestamp, bill) ->
                        val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
                        Column {
                            Text("Deleted on $date", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                            BillRow(bill = bill, onClick = {}, onDelete = { onPermanentDelete(bill.id) })
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onRestore(bill) }) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1DB954))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restore", color = Color(0xFF1DB954))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsUI(
    errorLogs: List<LogEntry>,
    groqLogs: List<LogEntry>,
    onClearErrorLogs: () -> Unit,
    onClearGroqLogs: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logs") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    val hasLogs = if (selectedTab == 0) errorLogs.isNotEmpty() else groqLogs.isNotEmpty()
                    if (hasLogs) {
                        IconButton(onClick = {
                            if (selectedTab == 0) onClearErrorLogs() else onClearGroqLogs()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = Color(0xFFCF6679))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF36454F), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF36454F)).padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF36454F),
                contentColor = Color(0xFF1DB954),
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        with(TabRowDefaults) {
                            SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color(0xFF1DB954)
                            )
                        }
                    }
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Error Logs", modifier = Modifier.padding(16.dp), color = if (selectedTab == 0) Color(0xFF1DB954) else Color.White)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Groq Responses", modifier = Modifier.padding(16.dp), color = if (selectedTab == 1) Color(0xFF1DB954) else Color.White)
                }
            }

            val currentLogs = if (selectedTab == 0) errorLogs else groqLogs
            
            if (currentLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No logs available", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currentLogs) { log ->
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2B373E))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(date, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Log", log.message)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(log.message, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.payman.ui.billdetails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.payman.data.model.Person
import com.example.payman.data.model.ProcessedBill
import com.example.payman.ui.components.TourState
import com.example.payman.ui.components.tourTarget
import java.util.Locale

@Composable
fun SplitResultDialog(
    bill: ProcessedBill, 
    people: List<Person>, 
    onDismiss: () -> Unit,
    tourState: TourState = TourState()
) {
    var isFlipped by remember { mutableStateOf(false) }
    var isDetailedView by remember { mutableStateOf(false) }
    
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        )
    )

    // Tour interaction logic for Split dialog
    LaunchedEffect(tourState.currentStepIndex) {
        val stepId = tourState.currentStep?.id ?: return@LaunchedEffect
        if (stepId == "split_methods_btn" || stepId.startsWith("method_")) {
            isFlipped = true
        } else if (stepId == "split_summary") {
            isFlipped = false
        }
    }

    val participatingPeople = people.filter { bill.participatingPersonIds.contains(it.id) }
    
    // Internal state for split method
    var currentMethod by remember { mutableStateOf(SplitMethod.EQUAL) }

    // Unified calculation data
    val calculationData = remember(bill, participatingPeople) {
        val foodTotals = mutableMapOf<String, Double>()
        val itemDetails = mutableMapOf<String, MutableList<String>>()
        val unassignedItemsList = mutableListOf<String>()

        participatingPeople.forEach { 
            foodTotals[it.id] = 0.0 
            itemDetails[it.id] = mutableListOf()
        }
        
        bill.items.forEach { item ->
            val assigned = item.assignedPersonIds.filter { bill.participatingPersonIds.contains(it) }
            if (assigned.isNotEmpty()) {
                val perPersonQty = item.quantity.toDouble() / assigned.size
                val share = (item.unitPrice * item.quantity) / assigned.size
                assigned.forEach { id -> 
                    foodTotals[id] = (foodTotals[id] ?: 0.0) + share 
                    val qtyText = if (perPersonQty % 1.0 == 0.0) perPersonQty.toInt().toString() else String.format(Locale.US, "%.1f", perPersonQty)
                    itemDetails[id]?.add("${item.name} (x$qtyText): ₹${String.format(Locale.US, "%.2f", share)}")
                }
            } else {
                val qtyText = if (item.quantity.toDouble() % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.1f", item.quantity.toDouble())
                unassignedItemsList.add("${item.name} (x$qtyText): ₹${String.format(Locale.US, "%.2f", item.totalPrice)}")
            }
        }
        
        val totalFood = foodTotals.values.sum()
        val totalMisc = bill.miscFees + bill.bookingFees
        val personCount = participatingPeople.size
        val leastSpenderId = foodTotals.minByOrNull { it.value }?.key
        val fi = foodTotals[leastSpenderId] ?: 0.0
        val oldMisc = if (personCount > 0) totalMisc / personCount else 0.0
        val newMiscProportional = if (totalFood > 0) totalMisc * (fi / totalFood) else oldMisc
        val maxSaving = oldMisc - newMiscProportional
        
        object {
            val foodShares = foodTotals
            val personItemDetails = itemDetails
            val unassignedItems = unassignedItemsList
            val totalMisc = totalMisc
            val totalFood = totalFood
            val personCount = personCount
            val fi = fi
            val oldMisc = oldMisc
            val maxSaving = maxSaving
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .then(
                    if (isDetailedView && !isFlipped) Modifier.heightIn(max = 600.dp) // Max size constraint
                    else Modifier.wrapContentHeight()
                )
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 8000 * density
                }
                .background(Color(0xFF2B373E), RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            if (rotation <= 90f) {
                // Front Side: Split Summary
                SplitSummaryContent(
                    bill = bill,
                    participatingPeople = participatingPeople,
                    currentMethod = currentMethod,
                    isDetailedView = isDetailedView,
                    foodShares = calculationData.foodShares,
                    personItemDetails = calculationData.personItemDetails,
                    unassignedItems = calculationData.unassignedItems,
                    maxSaving = calculationData.maxSaving,
                    onDetailedViewToggle = { isDetailedView = !isDetailedView },
                    onFlip = { isFlipped = true },
                    onResetMethod = { currentMethod = SplitMethod.EQUAL },
                    onDismiss = onDismiss,
                    tourState = tourState
                )
            } else {
                // Back Side: Split Methods
                Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                    SplitMethodsContent(
                        bill = bill,
                        participatingPeople = participatingPeople,
                        currentMethod = currentMethod,
                        foodShares = calculationData.foodShares,
                        totalMisc = calculationData.totalMisc,
                        totalFood = calculationData.totalFood,
                        personCount = calculationData.personCount,
                        fi = calculationData.fi,
                        oldMisc = calculationData.oldMisc,
                        maxSaving = calculationData.maxSaving,
                        onMethodSelected = { method ->
                            currentMethod = method
                            isFlipped = false
                        },
                        onFlipBack = { isFlipped = false },
                        tourState = tourState
                    )
                }
            }
        }
    }
}

@Composable
fun SplitSummaryContent(
    bill: ProcessedBill,
    participatingPeople: List<Person>,
    currentMethod: SplitMethod,
    isDetailedView: Boolean,
    foodShares: Map<String, Double>,
    personItemDetails: Map<String, List<String>>,
    unassignedItems: List<String>,
    maxSaving: Double,
    onDetailedViewToggle: () -> Unit,
    onFlip: () -> Unit,
    onResetMethod: () -> Unit,
    onDismiss: () -> Unit,
    tourState: TourState = TourState()
) {
    val context = LocalContext.current

    val totalMisc = bill.miscFees + bill.bookingFees
    val miscShares = remember(foodShares, totalMisc, currentMethod, participatingPeople) {
        SplitCalculator.calculateMiscShares(foodShares, totalMisc, currentMethod, participatingPeople.size)
    }

    // Comprehensive final share calculation
    val finalResults = remember(foodShares, miscShares, bill, participatingPeople) {
        participatingPeople.associate { person ->
            val foodShare = foodShares[person.id] ?: 0.0
            val taxService = (bill.tax + bill.serviceCharge) / participatingPeople.size
            val miscShare = miscShares[person.id] ?: 0.0
            
            val baseShare = foodShare + taxService + miscShare
            
            val discount = if (bill.isDiscountApplied) {
                if (bill.isDiscountFixedAmount) {
                    bill.discountAmount / participatingPeople.size
                } else {
                    (foodShare + taxService) * (bill.discountPercentage / 100.0)
                }
            } else 0.0
            
            val dinecash = bill.dinecashDeduction / participatingPeople.size
            
            val shareBeforeSwiggy = baseShare - discount - dinecash
            val swiggySaving = if (bill.isSwiggyHdfcApplied) shareBeforeSwiggy * 0.10 else 0.0
            
            person.id to object {
                val finalShare = shareBeforeSwiggy - swiggySaving
                val savings = discount + swiggySaving + dinecash
                val breakdown = mutableListOf<String>().apply {
                    if (taxService > 0) add("Tax & Service: ₹${String.format(Locale.US, "%.2f", taxService)}")
                    if (miscShare > 0) add("Misc & Booking: ₹${String.format(Locale.US, "%.2f", miscShare)}")
                    if (discount > 0) add("Discount Applied (On Food+Tax): -₹${String.format(Locale.US, "%.2f", discount)}")
                    if (dinecash > 0) add("Dinecash: -₹${String.format(Locale.US, "%.2f", dinecash)}")
                    if (swiggySaving > 0) add("Swiggy HDFC: -₹${String.format(Locale.US, "%.2f", swiggySaving)}")
                }
            }
        }
    }

    val finalShares = finalResults.mapValues { it.value.finalShare }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).tourTarget(tourState, "split_summary")) {
                Text("Split Summary", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (bill.payeeName.isNotBlank()) {
                    Text("Payee: ${bill.payeeName}", color = Color.Gray, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onDetailedViewToggle) {
                Icon(
                    imageVector = if (isDetailedView) Icons.Default.List else Icons.Default.Description,
                    contentDescription = "Toggle Detailed View",
                    tint = if (isDetailedView) Color(0xFF1DB954) else Color.White
                )
            }
            IconButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val text = buildString {
                    append("*${bill.restaurantName} Split*\n")
                    if (bill.payeeName.isNotBlank()) {
                        append("Payee: ${bill.payeeName}\n")
                    }
                    append("\n")
                    participatingPeople.forEach { p ->
                        val result = finalResults[p.id]
                        append("*${p.name}*: ₹${String.format(Locale.US, "%.2f", finalShares[p.id] ?: 0.0)}\n")
                        
                        // Copy details if available
                        personItemDetails[p.id]?.forEach { detail ->
                            append("  • $detail\n")
                        }
                        result?.breakdown?.forEach { detail ->
                            append("  • $detail\n")
                        }
                        append("\n")
                    }
                    
                    if (unassignedItems.isNotEmpty()) {
                        append("*Unassigned Items*:\n")
                        unassignedItems.forEach { append("  • $it\n") }
                        append("\n")
                    }
                    
                    append("*Total*: ₹${String.format(Locale.US, "%.2f", bill.totalAmount)}")
                    
                    if (currentMethod != SplitMethod.EQUAL) {
                        val methodTitle = when(currentMethod) {
                            SplitMethod.PROPORTIONAL -> "_Economically Fair_"
                            SplitMethod.HYBRID -> "_Balanced_"
                            else -> ""
                        }
                        append("\n\nNote: A special split method ($methodTitle) was used for Misc Fees to ensure fairness based on food consumption. Tax isn't adjusted this way because Dineout/Swiggy offers apply to Tax but not to Misc Fees.")
                    }
                }
                clipboard.setPrimaryClip(ClipData.newPlainText("Split Summary", text))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(participatingPeople) { person ->
                val result = finalResults[person.id]
                val share = result?.finalShare ?: 0.0
                
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(person.name, color = Color.White, fontWeight = if (isDetailedView) FontWeight.Bold else FontWeight.Normal)
                        Text("₹${String.format(Locale.US, "%.2f", share)}", color = Color(0xFF1DB954), fontWeight = FontWeight.Bold)
                    }
                    
                    if (isDetailedView) {
                        personItemDetails[person.id]?.forEach { detail ->
                            Text("• $detail", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                        }
                        result?.breakdown?.forEach { detail ->
                            Text("• $detail", color = if (detail.contains("-")) Color(0xFF1DB954) else Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))
                    }
                }
            }
            
            if (unassignedItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Unassigned Items", color = Color(0xFFCF6679), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    unassignedItems.forEach { detail ->
                        Text("• $detail", color = Color(0xFFCF6679), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                    }
                    Text(
                        "Warning: These items are not assigned to anyone and are excluded from the individual shares.",
                        color = Color(0xFFCF6679),
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            if (isDetailedView && currentMethod != SplitMethod.EQUAL) {
                item {
                    val methodTitle = when(currentMethod) {
                        SplitMethod.PROPORTIONAL -> "Economically Fair"
                        SplitMethod.HYBRID -> "Balanced"
                        else -> ""
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Note: A special split method ($methodTitle) was used for Misc Fees to ensure fairness based on food consumption. Tax isn't adjusted because Dineout/Swiggy offers apply to Tax but not to Misc Fees.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        if (maxSaving < 3.0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Note: Alternative split methods are disabled as they offer negligible savings (< ₹3) for the least spender.",
                color = Color.Gray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentMethod != SplitMethod.EQUAL) {
                TextButton(
                    onClick = onResetMethod,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCF6679))
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Split", fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onFlip, 
                modifier = Modifier.padding(end = 8.dp).tourTarget(tourState, "split_methods_btn")
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Split Methods", tint = if (maxSaving < 3.0) Color.Gray else Color.White)
            }
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SplitMethodsContent(
    bill: ProcessedBill,
    participatingPeople: List<Person>,
    currentMethod: SplitMethod,
    foodShares: Map<String, Double>,
    totalMisc: Double,
    totalFood: Double,
    personCount: Int,
    fi: Double,
    oldMisc: Double,
    maxSaving: Double,
    onMethodSelected: (SplitMethod) -> Unit,
    onFlipBack: () -> Unit,
    tourState: TourState = TourState()
) {
    fun f2(v: Double) = String.format(Locale.US, "%.2f", v)
    fun df(v: Double) = if (v % 1.0 == 0.0) String.format(Locale.US, "%.0f", v) else String.format(Locale.US, "%.2f", v)
    fun diffStr(v: Double) = (if (v >= 0) "+" else "-") + "₹" + f2(Math.abs(v))

    Column {
        Column {
            Text("Split Methods", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (bill.payeeName.isNotBlank()) {
                Text("Payee: ${bill.payeeName}", color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        MethodCard(
            title = "Economically Fair",
            subtitle = "Proportional to food consumed",
            isSelected = currentMethod == SplitMethod.PROPORTIONAL,
            enabled = maxSaving >= 3.0,
            onApply = { onMethodSelected(SplitMethod.PROPORTIONAL) },
            modifier = Modifier.tourTarget(tourState, "method_proportional")
        ) {
            val newMisc = if (totalFood > 0) totalMisc * (fi / totalFood) else oldMisc
            val diff = newMisc - oldMisc
            Column {
                Text(
                    text = "M_i = M_total × (F_i / F_total)\n" +
                           "= ${df(totalMisc)} × (${df(fi)} / ${df(totalFood)})\n" +
                           "= ₹${f2(newMisc)} (Old: ₹${f2(oldMisc)} → ${diffStr(diff)})",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        MethodCard(
            title = "Balanced",
            subtitle = "50% Equal, 50% Proportional",
            isSelected = currentMethod == SplitMethod.HYBRID,
            enabled = maxSaving >= 3.0,
            onApply = { onMethodSelected(SplitMethod.HYBRID) },
            modifier = Modifier.tourTarget(tourState, "method_balanced")
        ) {
            val equalPortion = if (personCount > 0) (totalMisc * 0.5) / personCount else 0.0
            val propPortion = if (totalFood > 0) (totalMisc * 0.5) * (fi / totalFood) else 0.0
            val newMisc = equalPortion + propPortion
            val diff = newMisc - oldMisc
            Column {
                Text(
                    text = "M_i = (50% equal) + (50% proportional)\n" +
                           "= (${df(totalMisc)} × 0.5 / $personCount) + (${df(totalMisc)} × 0.5 × ${df(fi)} / ${df(totalFood)})\n" +
                           "= ₹${f2(newMisc)} (Old: ₹${f2(oldMisc)} → ${diffStr(diff)})",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val inequality = SplitCalculator.calculateInequalityPercentage(foodShares, totalMisc, participatingPeople.size)
        if (inequality > 0) {
            Text(
                "Without using these split methods, the least spender is paying ${String.format(Locale.US, "%.1f", inequality)}% higher misc-to-food ratio.",
                color = if (maxSaving < 3.0) Color.Gray else MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onFlipBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
        ) {
            Text("Back to Summary")
        }
    }
}

@Composable
fun MethodCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1.0f else 0.5f)
            .clickable(enabled = enabled) { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF36454F)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = if (enabled) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color.Gray, fontSize = 12.sp)
                }
                Icon(
                    Icons.Default.ExpandMore,
                    null,
                    tint = if (enabled) Color.White else Color.Gray,
                    modifier = Modifier.rotate(rotation)
                )
            }
            
            AnimatedVisibility(visible = expanded || isSelected) { // Show if applied or expanded
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    content()
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onApply,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color.Gray else Color(0xFF1DB954))
                    ) {
                        Text(if (isSelected) "Applied" else "Apply", color = Color.Black)
                    }
                }
            }
        }
    }
}

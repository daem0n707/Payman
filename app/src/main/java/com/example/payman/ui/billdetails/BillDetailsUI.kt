package com.example.payman.ui.billdetails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.payman.data.model.BillItem
import com.example.payman.data.model.Group
import com.example.payman.data.model.Person
import com.example.payman.data.model.ProcessedBill
import com.example.payman.ui.components.DetailRow
import com.example.payman.ui.components.TourState
import com.example.payman.ui.components.tourTarget
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillDetailsUI(
    bill: ProcessedBill?,
    people: List<Person>,
    groups: List<Group>,
    onDismiss: () -> Unit,
    onBillUpdated: (ProcessedBill) -> Unit,
    swiggyHdfcOptionEnabled: Boolean = true,
    tourState: TourState = TourState()
) {
    if (bill == null) return

    var currentBill by remember { mutableStateOf(bill) }
    var history by remember { mutableStateOf(listOf<ProcessedBill>()) }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<BillItem?>(null) }
    var editingMiscType by remember { mutableStateOf<String?>(null) }
    var showSplitResult by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showEditRestaurantNameDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    // Tour interaction logic
    LaunchedEffect(tourState.currentStepIndex) {
        val stepId = tourState.currentStep?.id ?: return@LaunchedEffect
        when (stepId) {
            "misc_amount", "booking_amount" -> {
                editingMiscType = "Misc and Booking Fees"
            }
            "toggle_discount_type", "discount_amount", "discount_percentage", "dinecash_value", "swiggy_hdfc_checkbox" -> {
                showDiscountDialog = true
            }
        }
    }

    // Automatically unassign people who are no longer participating
    LaunchedEffect(currentBill.participatingPersonIds) {
        val participatingIds = currentBill.participatingPersonIds
        val updatedItems = currentBill.items.map { item ->
            val newAssigned = item.assignedPersonIds.filter { participatingIds.contains(it) }
            if (newAssigned.size != item.assignedPersonIds.size) {
                item.copy(assignedPersonIds = newAssigned)
            } else item
        }
        if (updatedItems != currentBill.items) {
            currentBill = currentBill.copy(items = updatedItems.toMutableList())
            onBillUpdated(currentBill)
        }
    }

    val participatingPeople = people.filter { currentBill.participatingPersonIds.contains(it.id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier
                            .tourTarget(tourState, "assign_payee")
                            .clickable { showEditRestaurantNameDialog = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currentBill.restaurantName, modifier = Modifier.weight(1f))
                            Text(
                                "₹${String.format(Locale.US, "%.2f", currentBill.totalAmount)}",
                                color = Color(0xFF1DB954),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        if (currentBill.payeeName.isNotBlank()) {
                            Text(
                                "Payee: ${currentBill.payeeName}",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.alpha(0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF36454F), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF36454F)).padding(padding).padding(16.dp)
        ) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { showFullScreenImage = true }
            ) {
                if (currentBill.imageUri != null) {
                    AsyncImage(model = currentBill.imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else if (currentBill.bitmap != null) {
                    Image(bitmap = currentBill.bitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Items", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                if (history.isNotEmpty()) {
                    IconButton(onClick = {
                        currentBill = history.last()
                        history = history.dropLast(1)
                        onBillUpdated(currentBill)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = Color.White)
                    }
                }
                IconButton(onClick = {
                    editingItem = BillItem(name = "", unitPrice = 0.0, quantity = 1)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .tourTarget(tourState, "bill_items")
            ) {
                items(currentBill.items) { item ->
                    BillItemRow(
                        item = item,
                        people = people,
                        onEdit = { editingItem = it },
                        onQuickAdd = {
                            val newHistory = history + currentBill.copy(items = currentBill.items.map { it.copy() }.toMutableList())
                            history = newHistory
                            val newItems = currentBill.items.toMutableList()
                            val idx = newItems.indexOfFirst { it.id == item.id }
                            if (idx != -1) newItems[idx] = item.copy(quantity = item.quantity + 1)
                            currentBill = currentBill.copy(items = newItems)
                            onBillUpdated(currentBill)
                        }
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { showSplitResult = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                    modifier = Modifier.tourTarget(tourState, "split_btn")
                ) {
                    Text("Split", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            showDiscountDialog = true
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .tourTarget(tourState, "discount_sec")
                            .alpha(if (currentBill.isDiscountApplied || currentBill.isSwiggyHdfcApplied || currentBill.dinecashDeduction > 0) 1f else 0.5f)
                    ) {
                        Icon(Icons.Default.LocalOffer, contentDescription = "Discount", tint = Color.White)
                    }
                    
                    IconButton(
                        onClick = { showDiscountDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Edit Discount", tint = Color(0xFF1DB954), modifier = Modifier.size(16.dp))
                    }
                }

                IconButton(
                    onClick = { showAddPersonDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .tourTarget(tourState, "add_people_btn")
                        .background(Color(0xFF1DB954), CircleShape)
                ) {
                    Icon(Icons.Default.People, contentDescription = "Add People", tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.Gray, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            DetailRow("Tax", "₹${String.format(Locale.US, "%.2f", currentBill.tax)}", onClick = { editingMiscType = "Tax" })
            DetailRow("Service Charge", "₹${String.format(Locale.US, "%.2f", currentBill.serviceCharge)}", onClick = { editingMiscType = "Service Charge" })
            DetailRow(
                "Misc and Booking Fees", 
                "₹${String.format(Locale.US, "%.2f", currentBill.miscFees + currentBill.bookingFees)}", 
                onClick = { editingMiscType = "Misc and Booking Fees" },
                modifier = Modifier.tourTarget(tourState, "misc_charges")
            )
            
            if (currentBill.isDiscountApplied) {
                val discountLabel = if (currentBill.isDiscountFixedAmount) {
                    "Discount (Fixed)"
                } else {
                    "Discount (${currentBill.discountPercentage}%)"
                }
                val discountVal = if (currentBill.isDiscountFixedAmount) {
                    currentBill.discountAmount
                } else {
                    (currentBill.items.sumOf { it.totalPrice } + currentBill.tax + currentBill.serviceCharge) * (currentBill.discountPercentage / 100.0)
                }
                DetailRow(discountLabel, "-₹${String.format(Locale.US, "%.2f", discountVal)}")
            }
            
            if (currentBill.dinecashDeduction > 0) {
                DetailRow("Dinecash (Total)", "-₹${String.format(Locale.US, "%.2f", currentBill.dinecashDeduction)}")
            }

            if (currentBill.isSwiggyHdfcApplied) {
                val base = currentBill.items.sumOf { it.totalPrice } + currentBill.tax + currentBill.serviceCharge
                val discounted = if (currentBill.isDiscountApplied) {
                    if (currentBill.isDiscountFixedAmount) base - currentBill.discountAmount else base * (1 - currentBill.discountPercentage / 100.0)
                } else base
                val swiggyAmount = (discounted + currentBill.miscFees + currentBill.bookingFees - currentBill.dinecashDeduction) * 0.10
                DetailRow("Swiggy HDFC Card (10%)", "-₹${String.format(Locale.US, "%.2f", swiggyAmount)}")
            }
        }
    }

    if (showEditRestaurantNameDialog) {
        var newName by remember { mutableStateOf(currentBill.restaurantName) }
        var selectedPayeeId by remember { mutableStateOf(currentBill.payeeId) }
        
        AlertDialog(
            onDismissRequest = { showEditRestaurantNameDialog = false },
            containerColor = Color(0xFF36454F),
            title = { Text("Edit Details", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName, 
                        onValueChange = { newName = it }, 
                        label = { Text("Restaurant Name") }, 
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF1DB954),
                            unfocusedLabelColor = Color.Gray,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (participatingPeople.isNotEmpty()) {
                        Text("Select Payee (Participating People):", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                        Box(modifier = Modifier.height(150.dp).background(Color(0xFF2B373E), RoundedCornerShape(8.dp)).padding(8.dp)) {
                            LazyColumn {
                                items(participatingPeople) { person ->
                                    val isSelected = selectedPayeeId == person.id
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) Color(0xFF1DB954).copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable {
                                                selectedPayeeId = if (isSelected) null else person.id
                                            }
                                            .padding(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF1DB954) else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(person.name, color = if (isSelected) Color.White else Color.Gray)
                                    }
                                }
                            }
                        }
                    } else {
                        Text("No participating people added to this bill.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val selectedPayeeName = participatingPeople.find { it.id == selectedPayeeId }?.name ?: ""
                    currentBill = currentBill.copy(restaurantName = newName, payeeName = selectedPayeeName, payeeId = selectedPayeeId)
                    onBillUpdated(currentBill)
                    showEditRestaurantNameDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))) { Text("Save", color = Color.Black) }
            },
            dismissButton = { TextButton(onClick = { showEditRestaurantNameDialog = false }) { Text("Cancel", color = Color.White) } }
        )
    }

    if (showFullScreenImage) {
        Dialog(
            onDismissRequest = { showFullScreenImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF36454F)).clickable { showFullScreenImage = false }) {
                if (currentBill.imageUri != null) {
                    AsyncImage(model = currentBill.imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } else if (currentBill.bitmap != null) {
                    Image(bitmap = currentBill.bitmap!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
                IconButton(onClick = { showFullScreenImage = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    if (editingItem != null) {
        ItemEditorDialog(
            item = editingItem!!,
            people = participatingPeople,
            onDismiss = { editingItem = null },
            onSave = { updatedItem ->
                val newHistory = history + currentBill.copy(items = currentBill.items.map { it.copy() }.toMutableList())
                history = newHistory
                val newItems = currentBill.items.toMutableList()
                val idx = newItems.indexOfFirst { it.id == updatedItem.id }
                if (idx != -1) {
                    newItems[idx] = updatedItem
                } else {
                    newItems.add(updatedItem)
                }
                currentBill = currentBill.copy(items = newItems)
                onBillUpdated(currentBill)
                editingItem = null
            },
            onDelete = {
                val newHistory = history + currentBill.copy(items = currentBill.items.map { it.copy() }.toMutableList())
                history = newHistory
                val newItems = currentBill.items.toMutableList()
                newItems.removeIf { it.id == editingItem!!.id }
                currentBill = currentBill.copy(items = newItems)
                onBillUpdated(currentBill)
                editingItem = null
            }
        )
    }

    if (editingMiscType != null) {
        if (editingMiscType == "Tax" || editingMiscType == "Service Charge") {
            MiscFeeEditorDialog(
                type = editingMiscType!!,
                currentValue = if (editingMiscType == "Tax") currentBill.tax else currentBill.serviceCharge,
                onDismiss = { editingMiscType = null },
                onSave = { newValue ->
                    val newHistory = history + currentBill.copy()
                    history = newHistory
                    currentBill = if (editingMiscType == "Tax") currentBill.copy(tax = newValue) else currentBill.copy(serviceCharge = newValue)
                    onBillUpdated(currentBill)
                    editingMiscType = null
                },
                onClearAll = {
                    val newHistory = history + currentBill.copy()
                    history = newHistory
                    currentBill = if (editingMiscType == "Tax") currentBill.copy(tax = 0.0) else currentBill.copy(serviceCharge = 0.0)
                    onBillUpdated(currentBill)
                }
            )
        } else {
            MiscAndBookingFeeEditorDialog(
                type = editingMiscType!!,
                currentMiscValue = currentBill.miscFees,
                currentBookingValue = currentBill.bookingFees,
                onDismiss = { editingMiscType = null },
                onSave = { newMisc, newBooking ->
                    val newHistory = history + currentBill.copy()
                    history = newHistory
                    currentBill = currentBill.copy(miscFees = newMisc, bookingFees = newBooking)
                    onBillUpdated(currentBill)
                    editingMiscType = null
                },
                onClearAll = {
                    val newHistory = history + currentBill.copy()
                    history = newHistory
                    currentBill = currentBill.copy(miscFees = 0.0, bookingFees = 0.0)
                    onBillUpdated(currentBill)
                },
                tourState = tourState
            )
        }
    }

    if (showDiscountDialog) {
        DiscountEditorDialog(
            currentPercentage = currentBill.discountPercentage,
            currentAmount = currentBill.discountAmount,
            currentDinecash = currentBill.dinecashDeduction,
            isFixed = currentBill.isDiscountFixedAmount,
            isSwiggyHdfc = currentBill.isSwiggyHdfcApplied,
            onDismiss = { showDiscountDialog = false },
            onSave = { value, dinecash, isFixed, isSwiggyHdfc ->
                if (isFixed) {
                    currentBill = currentBill.copy(discountAmount = value, dinecashDeduction = dinecash, discountPercentage = 0.0, isDiscountFixedAmount = true, isDiscountApplied = value > 0, isSwiggyHdfcApplied = isSwiggyHdfc)
                } else {
                    currentBill = currentBill.copy(discountPercentage = value, dinecashDeduction = dinecash, discountAmount = 0.0, isDiscountFixedAmount = false, isDiscountApplied = value > 0, isSwiggyHdfcApplied = isSwiggyHdfc)
                }
                onBillUpdated(currentBill)
                showDiscountDialog = false
            },
            swiggyHdfcEnabled = swiggyHdfcOptionEnabled,
            tourState = tourState
        )
    }

    if (showAddPersonDialog) {
        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            containerColor = Color(0xFF36454F),
            title = { Text("Add People or Groups to Bill", color = Color.White) },
            text = {
                LazyColumn {
                    if (groups.isNotEmpty()) {
                        item { Text("Groups", fontWeight = FontWeight.Bold, color = Color.White) }
                        items(groups) { group ->
                            val isParticipating = group.memberIds.all { currentBill.participatingPersonIds.contains(it) }
                            ListItem(
                                headlineContent = { Text(group.name, color = Color.White) },
                                trailingContent = { Checkbox(checked = isParticipating, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1DB954))) },
                                modifier = Modifier.clickable {
                                    val newIds = currentBill.participatingPersonIds.toMutableList()
                                    if (isParticipating) {
                                        newIds.removeAll(group.memberIds)
                                    } else {
                                        group.memberIds.forEach { if (!newIds.contains(it)) newIds.add(it) }
                                    }
                                    currentBill = currentBill.copy(participatingPersonIds = newIds)
                                    onBillUpdated(currentBill)
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                    if (people.isNotEmpty()) {
                        item { Text("People", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 8.dp)) }
                        item {
                            Box(modifier = Modifier.height(250.dp)) {
                                LazyColumn {
                                    items(people) { person ->
                                        val isParticipating = currentBill.participatingPersonIds.contains(person.id)
                                        ListItem(
                                            headlineContent = { Text(person.name, color = Color.White) },
                                            trailingContent = { Checkbox(checked = isParticipating, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1DB954))) },
                                            modifier = Modifier.clickable {
                                                val newIds = currentBill.participatingPersonIds.toMutableList()
                                                if (isParticipating) newIds.remove(person.id) else newIds.add(person.id)
                                                currentBill = currentBill.copy(participatingPersonIds = newIds)
                                                onBillUpdated(currentBill)
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (groups.isEmpty() && people.isEmpty()) {
                        item { Text("No groups or people available. Add them from the sidebar.", color = Color.Gray) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddPersonDialog = false }) { Text("Done", color = Color(0xFF1DB954)) } }
        )
    }

    if (showSplitResult) {
        SplitResultDialog(bill = currentBill, people = people, onDismiss = { showSplitResult = false })
    }
}

@Composable
fun BillItemRow(item: BillItem, people: List<Person>, onEdit: (BillItem) -> Unit, onQuickAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onQuickAdd, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF1DB954), modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f).clickable { onEdit(item) }) {
            Text(item.name, color = Color.White)
            Row {
                Text("x${item.quantity} ", color = Color.Gray, fontSize = 14.sp)
                Text("${String.format(Locale.US, "%.0f", item.unitPrice)} ", color = Color.Gray, fontSize = 14.sp)
                if (item.assignedPersonIds.isNotEmpty()) {
                    val counts = item.assignedPersonIds.groupingBy { it }.eachCount()
                    val assignedText = counts.entries.joinToString { (id, count) ->
                        val name = people.find { it.id == id }?.name ?: "Unknown"
                        if (count > 1) "$name (x$count)" else name
                    }
                    Text("• $assignedText", color = Color(0xFF1DB954), fontSize = 12.sp)
                }
            }
        }
        Text("₹${String.format(Locale.US, "%.2f", item.totalPrice)}", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ItemEditorDialog(item: BillItem, people: List<Person>, onDismiss: () -> Unit, onSave: (BillItem) -> Unit, onDelete: () -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var price by remember { mutableStateOf(item.unitPrice.toString()) }
    var quantity by remember { mutableStateOf(item.quantity.toString()) }
    val assignedIds = remember { mutableStateListOf<String>().apply { addAll(item.assignedPersonIds) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF36454F),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (item.name.isEmpty()) "Add Item" else "Edit Item", color = Color.White, modifier = Modifier.weight(1f))
                if (item.name.isNotEmpty()) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCF6679))
                    }
                }
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Name") }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = price, 
                        onValueChange = { price = it }, 
                        label = { Text("Price") }, 
                        modifier = Modifier.weight(1f), 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = quantity, 
                        onValueChange = { quantity = it }, 
                        label = { Text("Qty") }, 
                        modifier = Modifier.weight(0.5f), 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (people.isNotEmpty()) {
                    Text("Assign To (Participating People):", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                    Box(modifier = Modifier.height(150.dp).background(Color(0xFF2B373E), RoundedCornerShape(8.dp)).padding(8.dp)) {
                        LazyColumn {
                            items(people) { person ->
                                val count = assignedIds.count { it == person.id }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (count > 0) Color(0xFF1DB954).copy(alpha = 0.2f) else Color.Transparent)
                                        .padding(8.dp)
                                ) {
                                    Text(person.name, color = if (count > 0) Color.White else Color.Gray, modifier = Modifier.weight(1f))
                                    
                                    if (count > 0) {
                                        IconButton(onClick = { assignedIds.remove(person.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Remove, contentDescription = "Remove", tint = Color(0xFFCF6679), modifier = Modifier.size(16.dp))
                                        }
                                        Text("$count", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp))
                                    }
                                    
                                    IconButton(onClick = { assignedIds.add(person.id) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF1DB954), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("No participating people added to this bill. Add them using the people icon on bill details screen.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(item.copy(name = name, unitPrice = price.toDoubleOrNull() ?: 0.0, quantity = quantity.toIntOrNull() ?: 1, assignedPersonIds = assignedIds.toList()))
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))) { Text("Save", color = Color.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) } }
    )
}

@Composable
fun MiscFeeEditorDialog(type: String, currentValue: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit, onClearAll: () -> Unit) {
    var valueStr by remember { mutableStateOf("") }
    var runningTotal by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF36454F),
        title = { Text("Edit $type", color = Color.White) },
        text = {
            Column {
                Text("Current Total: ₹${String.format(Locale.US, "%.2f", runningTotal)}", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))
                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it },
                    label = { Text("Amount to add") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            val toAdd = valueStr.toDoubleOrNull() ?: 0.0
                            runningTotal += toAdd
                            valueStr = ""
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF1DB954))
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalVal = runningTotal + (valueStr.toDoubleOrNull() ?: 0.0)
                onSave(finalVal)
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))) { Text("Save", color = Color.Black) }
        },
        dismissButton = {
            TextButton(onClick = { 
                runningTotal = 0.0
                valueStr = ""
                onClearAll()
            }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCF6679))) { Text("Clear All") }
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        }
    )
}

@Composable
fun MiscAndBookingFeeEditorDialog(
    type: String, 
    currentMiscValue: Double, 
    currentBookingValue: Double, 
    onDismiss: () -> Unit, 
    onSave: (Double, Double) -> Unit, 
    onClearAll: () -> Unit,
    tourState: TourState = TourState()
) {
    var miscStr by remember { mutableStateOf("") }
    var bookingStr by remember { mutableStateOf("") }
    var runningMisc by remember { mutableStateOf(currentMiscValue) }
    var runningBooking by remember { mutableStateOf(currentBookingValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF36454F),
        title = { Text("Edit $type", color = Color.White) },
        text = {
            Column {
                Text("Total Misc: ₹${String.format(Locale.US, "%.2f", runningMisc)}", color = Color.White, fontSize = 14.sp)
                Text("Total Booking: ₹${String.format(Locale.US, "%.2f", runningBooking)}", color = Color.White, fontSize = 14.sp)
                Text("Combined Total: ₹${String.format(Locale.US, "%.2f", runningMisc + runningBooking)}", fontWeight = FontWeight.Bold, color = Color(0xFF1DB954), modifier = Modifier.padding(vertical = 8.dp))
                
                OutlinedTextField(
                    value = miscStr,
                    onValueChange = { miscStr = it },
                    label = { Text("Amount to add (Misc)") },
                    modifier = Modifier.fillMaxWidth().tourTarget(tourState, "misc_amount"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            runningMisc += (miscStr.toDoubleOrNull() ?: 0.0)
                            miscStr = ""
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Misc", tint = Color(0xFF1DB954))
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bookingStr,
                    onValueChange = { bookingStr = it },
                    label = { Text("Amount to add (Booking)") },
                    modifier = Modifier.fillMaxWidth().tourTarget(tourState, "booking_amount"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            runningBooking += (bookingStr.toDoubleOrNull() ?: 0.0)
                            bookingStr = ""
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Booking", tint = Color(0xFF1DB954))
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalMisc = runningMisc + (miscStr.toDoubleOrNull() ?: 0.0)
                val finalBooking = runningBooking + (bookingStr.toDoubleOrNull() ?: 0.0)
                onSave(finalMisc, finalBooking)
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))) { Text("Save", color = Color.Black) }
        },
        dismissButton = {
            TextButton(onClick = { 
                runningMisc = 0.0
                runningBooking = 0.0
                miscStr = ""
                bookingStr = ""
                onClearAll()
            }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCF6679))) { Text("Clear All") }
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        }
    )
}

@Composable
fun DiscountEditorDialog(
    currentPercentage: Double, 
    currentAmount: Double,
    currentDinecash: Double,
    isFixed: Boolean,
    isSwiggyHdfc: Boolean,
    onDismiss: () -> Unit, 
    onSave: (Double, Double, Boolean, Boolean) -> Unit,
    swiggyHdfcEnabled: Boolean = true,
    tourState: TourState = TourState()
) {
    var isFixedAmount by remember { mutableStateOf(isFixed) }
    var swiggyHdfc by remember { mutableStateOf(isSwiggyHdfc) }
    var valueStr by remember { mutableStateOf(if (isFixed) (if (currentAmount > 0) currentAmount.toString() else "") else (if (currentPercentage > 0) currentPercentage.toString() else "")) }
    var dinecashStr by remember { mutableStateOf(if (currentDinecash > 0) currentDinecash.toString() else "") }

    // Internal tour logic for Discount dialog
    LaunchedEffect(tourState.currentStepIndex) {
        val stepId = tourState.currentStep?.id ?: return@LaunchedEffect
        if (stepId == "discount_amount") isFixedAmount = true
        if (stepId == "discount_percentage") isFixedAmount = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF36454F),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Discount & Dinecash", color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = { isFixedAmount = !isFixedAmount }, modifier = Modifier.tourTarget(tourState, "toggle_discount_type")) {
                    Icon(Icons.Default.SyncAlt, contentDescription = "Toggle Type", tint = Color.White)
                }
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it },
                    label = { Text(if (isFixedAmount) "Discount Amount (₹)" else "Discount Percentage (%)") },
                    modifier = Modifier.fillMaxWidth()
                        .tourTarget(tourState, "discount_amount")
                        .tourTarget(tourState, "discount_percentage")
                        .tourTarget(tourState, "discount_value"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = if (isFixedAmount) Icons.Default.CurrencyRupee else Icons.Default.Percent, 
                            contentDescription = null, 
                            tint = Color.Gray
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = dinecashStr,
                    onValueChange = { dinecashStr = it },
                    label = { Text("Dinecash Deduction") },
                    modifier = Modifier.fillMaxWidth().tourTarget(tourState, "dinecash_value"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CurrencyRupee, 
                            contentDescription = null, 
                            tint = Color.Gray
                        )
                    }
                )
                
                if (swiggyHdfcEnabled || swiggyHdfc) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { swiggyHdfc = !swiggyHdfc }.tourTarget(tourState, "swiggy_hdfc_checkbox"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = swiggyHdfc,
                            onCheckedChange = { swiggyHdfc = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1DB954))
                        )
                        Text("Swiggy HDFC Card (Extra 10%)", color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    valueStr.toDoubleOrNull() ?: 0.0,
                    dinecashStr.toDoubleOrNull() ?: 0.0,
                    isFixedAmount,
                    swiggyHdfc
                )
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))) { Text("Save", color = Color.Black) }
        },
        dismissButton = {
            TextButton(onClick = { onSave(0.0, 0.0, false, false) }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCF6679))) { Text("Remove All") }
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        }
    )
}

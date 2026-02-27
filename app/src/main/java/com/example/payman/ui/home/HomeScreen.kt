package com.example.payman.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.payman.data.local.saveBills
import com.example.payman.data.model.Group
import com.example.payman.data.model.Person
import com.example.payman.data.model.ProcessedBill
import com.example.payman.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitterUI(
    onAddGroupClick: () -> Unit,
    onEditGroupClick: (Group) -> Unit,
    onAddBillClick: () -> Unit,
    onAddBillToSection: (String) -> Unit,
    onRecycleBinClick: () -> Unit,
    onLogsClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onSpendingStatsClick: () -> Unit,
    bills: MutableList<ProcessedBill>,
    onBillClick: (ProcessedBill) -> Unit,
    onDeleteBill: (ProcessedBill) -> Unit,
    onDeleteSection: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onAddPeopleClick: () -> Unit,
    people: List<Person>,
    groups: List<Group>,
    onUpdatePerson: (Person) -> Unit,
    onDeletePerson: (String) -> Unit,
    onUpdateGroup: (Group) -> Unit,
    onDeleteGroup: (String) -> Unit,
    swiggyDineoutEnabled: Boolean,
    onSwiggyDineoutToggle: (Boolean) -> Unit,
    emptySections: SnapshotStateList<String>,
    tourState: TourState,
    onStartTour: () -> Unit,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
) {
    val scope = rememberCoroutineScope()
    var selectedBillsForSmartSplit by remember { mutableStateOf<List<ProcessedBill>?>(null) }
    var showNewSectionDialog by remember { mutableStateOf(false) }
    var showUsageDialog by remember { mutableStateOf(false) }
    var newSectionName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val collapsedSections = remember { mutableStateListOf<String>() }

    // Drag and Drop State
    val lazyListState = rememberLazyListState()
    var draggedBill by remember { mutableStateOf<ProcessedBill?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var hoveredSection by remember { mutableStateOf<String?>(null) }

    // Auto-scroll logic
    LaunchedEffect(draggedBill, dragPosition) {
        if (draggedBill != null) {
            while (true) {
                val layoutInfo = lazyListState.layoutInfo
                val viewportHeight = layoutInfo.viewportSize.height
                val threshold = 200f
                val scrollAmount = when {
                    dragPosition.y < threshold -> -25f * (1f - (dragPosition.y / threshold))
                    dragPosition.y > viewportHeight - threshold -> 25f * (1f - ((viewportHeight - dragPosition.y) / threshold))
                    else -> 0f
                }
                
                if (scrollAmount != 0f) {
                    lazyListState.scrollBy(scrollAmount)
                }
                delay(16)
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier.fillMaxHeight(),
                        drawerContainerColor = Color(0xFF2B373E)
                    ) {
                        SidebarMenu(
                            onAddGroupClick = {
                                scope.launch { drawerState.close() }
                                onAddGroupClick()
                            },
                            onEditGroupClick = { group ->
                                scope.launch { drawerState.close() }
                                onEditGroupClick(group)
                            },
                            onPeopleClick = {
                                scope.launch { drawerState.close() }
                                onAddPeopleClick()
                            },
                            onRecycleBinClick = {
                                scope.launch { drawerState.close() }
                                onRecycleBinClick()
                            },
                            onLogsClick = {
                                scope.launch { drawerState.close() }
                                onLogsClick()
                            },
                            onCalculatorClick = {
                                scope.launch { drawerState.close() }
                                onCalculatorClick()
                            },
                            onSpendingStatsClick = {
                                scope.launch { drawerState.close() }
                                onSpendingStatsClick()
                            },
                            apiKey = apiKey,
                            onApiKeyChange = onApiKeyChange,
                            groups = groups,
                            people = people,
                            onUpdatePerson = onUpdatePerson,
                            onDeletePerson = onDeletePerson,
                            onUpdateGroup = onUpdateGroup,
                            onDeleteGroup = onDeleteGroup,
                            swiggyDineoutEnabled = swiggyDineoutEnabled,
                            onSwiggyDineoutToggle = onSwiggyDineoutToggle,
                            tourState = tourState
                        )
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Payman", fontWeight = FontWeight.Bold) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF36454F),
                                titleContentColor = Color.White,
                                actionIconContentColor = Color.White
                            ),
                            actions = {
                                if (bills.isNotEmpty()) {
                                    IconButton(onClick = onStartTour) {
                                        Icon(Icons.Default.Explore, contentDescription = "Start Tour", tint = Color(0xFF1DB954))
                                    }
                                }
                                IconButton(onClick = { showUsageDialog = true }) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = "Usage Guide")
                                }
                                IconButton(
                                    onClick = { showNewSectionDialog = true },
                                    modifier = Modifier.tourTarget(tourState, "new_section_btn")
                                ) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New Section")
                                }
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.tourTarget(tourState, "menu_btn")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF36454F))
                            .padding(padding)
                    ) {
                        if (bills.isEmpty() && emptySections.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SquareButton(
                                    icon = Icons.Default.Add,
                                    label = "Add bill",
                                    onClick = onAddBillClick
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Add your first bill to get a tour across the features of the app!",
                                    color = Color.LightGray,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                TextButton(onClick = { showNewSectionDialog = true }) {
                                    Icon(Icons.Default.CreateNewFolder, null, tint = Color(0xFF1DB954))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create a Section", color = Color(0xFF1DB954))
                                }
                            }
                        } else {
                            val persistentSectionNames = bills.mapNotNull { it.sectionName }.distinct()
                            val allSectionNames = (persistentSectionNames + emptySections).distinct().sorted()

                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .pointerInput(allSectionNames) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                dragPosition = offset
                                                val itemUnderPointer = lazyListState.layoutInfo.visibleItemsInfo
                                                    .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
                                                
                                                val key = itemUnderPointer?.key?.toString() ?: ""
                                                if (key.startsWith("bill_")) {
                                                    val billId = key.removePrefix("bill_")
                                                    draggedBill = bills.find { it.id == billId }
                                                    dragOffset = 0f
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount.y
                                                dragPosition = change.position

                                                draggedBill?.let { bill ->
                                                    // Detect hovered item/section
                                                    val hoveredItem = lazyListState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { item -> dragPosition.y.toInt() in item.offset..(item.offset + item.size) }
                                                    
                                                    val key = hoveredItem?.key?.toString() ?: ""
                                                    val newHoveredSection = when {
                                                        key.startsWith("header_") -> key.removePrefix("header_")
                                                        key.startsWith("empty_") -> key.removePrefix("empty_")
                                                        key.startsWith("bill_") -> {
                                                            val targetBillId = key.removePrefix("bill_")
                                                            val targetBill = bills.find { it.id == targetBillId }
                                                            targetBill?.sectionName
                                                        }
                                                        else -> null
                                                    }

                                                    if (newHoveredSection != hoveredSection && newHoveredSection != null) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    hoveredSection = newHoveredSection
                                                    
                                                    // Reordering within the same section logic
                                                    if (key.startsWith("bill_")) {
                                                        val targetBillId = key.removePrefix("bill_")
                                                        val targetBill = bills.find { it.id == targetBillId }
                                                        if (targetBill != null && targetBill.sectionName == bill.sectionName && targetBill.id != bill.id) {
                                                            val sectionBills = bills.filter { it.sectionName == bill.sectionName }
                                                            val currentIdxInSec = sectionBills.indexOf(bill)
                                                            val targetIdxInSec = sectionBills.indexOf(targetBill)
                                                            
                                                            val threshold = 40f
                                                            if ((dragOffset > threshold && targetIdxInSec > currentIdxInSec) || 
                                                                (dragOffset < -threshold && targetIdxInSec < currentIdxInSec)) {
                                                                val fromIndex = bills.indexOf(bill)
                                                                val toIndex = bills.indexOf(targetBill)
                                                                Collections.swap(bills, fromIndex, toIndex)
                                                                dragOffset = 0f
                                                                saveBills(context, bills)
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                if (draggedBill != null && hoveredSection != null && draggedBill?.sectionName != hoveredSection) {
                                                    updateBillSection(bills, draggedBill!!, hoveredSection!!, context, emptySections)
                                                }
                                                draggedBill = null
                                                dragOffset = 0f
                                                dragPosition = Offset.Zero
                                                hoveredSection = null
                                            },
                                            onDragCancel = {
                                                draggedBill = null
                                                dragOffset = 0f
                                                dragPosition = Offset.Zero
                                                hoveredSection = null
                                            }
                                        )
                                    },
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                allSectionNames.forEach { sectionName ->
                                    val sectionBills = bills.filter { it.sectionName == sectionName }
                                    val isCollapsed = collapsedSections.contains(sectionName)
                                    val isHovered = hoveredSection == sectionName && draggedBill != null && draggedBill?.sectionName != sectionName
                                    
                                    item(key = "header_$sectionName") {
                                        Surface(
                                            color = if (isHovered) Color(0xFF1DB954).copy(alpha = 0.2f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().animateItem()
                                        ) {
                                            SectionHeader(
                                                name = sectionName,
                                                isCollapsed = isCollapsed,
                                                onToggle = {
                                                    if (isCollapsed) collapsedSections.remove(sectionName)
                                                    else collapsedSections.add(sectionName)
                                                },
                                                onSmartSplit = { selectedBillsForSmartSplit = sectionBills },
                                                onAddBill = { onAddBillToSection(sectionName) },
                                                onDeleteSection = {
                                                    onDeleteSection(sectionName)
                                                    emptySections.remove(sectionName)
                                                },
                                                hasBills = sectionBills.isNotEmpty(),
                                                isHovered = isHovered,
                                                tourState = tourState
                                            )
                                        }
                                    }
                                    
                                    if (!isCollapsed) {
                                        if (sectionBills.isEmpty()) {
                                            item(key = "empty_$sectionName") {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 16.dp)
                                                        .animateItem(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "Add new bills here", 
                                                        color = if (isHovered) Color(0xFF1DB954) else Color.Gray, 
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        } else {
                                            itemsIndexed(sectionBills, key = { _, bill -> "bill_${bill.id}" }) { index, bill ->
                                                val isDragged = draggedBill?.id == bill.id
                                                val isFirstBillInFirstSection = index == 0 && sectionName == allSectionNames.firstOrNull()

                                                BillRow(
                                                    bill = bill,
                                                    modifier = Modifier
                                                        .animateItem()
                                                        .then(if (isFirstBillInFirstSection) Modifier.tourTarget(tourState, "first_bill") else Modifier)
                                                        .graphicsLayer {
                                                            if (isDragged) {
                                                                scaleX = 1.1f
                                                                scaleY = 1.1f
                                                                translationY = dragOffset
                                                                shadowElevation = 12.dp.toPx()
                                                            }
                                                        }
                                                        .zIndex(if (isDragged) 1f else 0f),
                                                    onClick = { onBillClick(bill) },
                                                    onDelete = { onDeleteBill(bill) }
                                                )
                                            }
                                        }
                                    }
                                }

                                val uncategorizedBills = bills.filter { it.sectionName == null }
                                if (uncategorizedBills.isNotEmpty()) {
                                    item(key = "header_uncategorized") {
                                        Text("Uncategorized", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 16.dp).animateItem())
                                    }
                                    itemsIndexed(uncategorizedBills, key = { _, bill -> "bill_${bill.id}" }) { _, bill ->
                                        val isDragged = draggedBill?.id == bill.id
                                        BillRow(
                                            bill = bill,
                                            modifier = Modifier
                                                .animateItem()
                                                .graphicsLayer {
                                                    if (isDragged) {
                                                        scaleX = 1.1f
                                                        scaleY = 1.1f
                                                        translationY = dragOffset
                                                        shadowElevation = 12.dp.toPx()
                                                    }
                                                }
                                                .zIndex(if (isDragged) 1f else 0f),
                                            onClick = { onBillClick(bill) },
                                            onDelete = { onDeleteBill(bill) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showNewSectionDialog) {
                        AlertDialog(
                            onDismissRequest = { showNewSectionDialog = false },
                            title = { Text("New Section") },
                            text = {
                                OutlinedTextField(
                                    value = newSectionName,
                                    onValueChange = { newSectionName = it },
                                    label = { Text("Section Name") },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (newSectionName.isNotBlank()) {
                                            emptySections.add(newSectionName)
                                            showNewSectionDialog = false
                                            newSectionName = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                                ) { Text("Create") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showNewSectionDialog = false }) { Text("Cancel") }
                            }
                        )
                    }

                    if (selectedBillsForSmartSplit != null) {
                        SmartSplitDialog(
                            bills = selectedBillsForSmartSplit!!,
                            people = people,
                            onDismiss = { selectedBillsForSmartSplit = null }
                        )
                    }

                    if (showUsageDialog) {
                        UsageDialog(onDismiss = { showUsageDialog = false })
                    }
                }
            }
        }
    }
}

/**
 * Handles moving a bill from one section to another.
 */
fun updateBillSection(
    bills: MutableList<ProcessedBill>,
    draggedBill: ProcessedBill,
    targetSection: String,
    context: android.content.Context,
    emptySections: SnapshotStateList<String>
) {
    val idx = bills.indexOfFirst { it.id == draggedBill.id }
    if (idx != -1) {
        val updatedBill = bills[idx].copy(sectionName = targetSection)
        bills[idx] = updatedBill
        
        // When dropped into an empty section, remove it from the "emptySections" tracking list
        // as it now persistently belongs to the bills data.
        emptySections.remove(targetSection)
        
        saveBills(context, bills)
    }
}

@Composable
fun SectionHeader(
    name: String,
    isCollapsed: Boolean,
    onToggle: () -> Unit,
    onSmartSplit: () -> Unit,
    onAddBill: () -> Unit,
    onDeleteSection: () -> Unit,
    hasBills: Boolean,
    isHovered: Boolean = false,
    tourState: TourState
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = if (isHovered) Color(0xFF1DB954) else Color.White,
            modifier = Modifier.clickable { onToggle() }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            color = if (isHovered) Color(0xFF1DB954) else Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).clickable { onToggle() }
        )
        
        if (hasBills) {
            IconButton(
                onClick = onSmartSplit,
                modifier = Modifier.tourTarget(tourState, "smart_split")
            ) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Smart Split", tint = Color(0xFF1DB954))
            }
        }
        
        IconButton(
            onClick = onAddBill,
            modifier = Modifier.tourTarget(tourState, "add_bill_to_section")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Bill to Section", tint = Color.White)
        }
        
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete Section") },
                    onClick = {
                        showMenu = false
                        onDeleteSection()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                )
            }
        }
    }
}

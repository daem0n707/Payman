package com.example.payman.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.payman.data.local.saveBills
import com.example.payman.data.local.saveDeletedBills
import com.example.payman.data.model.Group
import com.example.payman.data.model.Person
import com.example.payman.data.model.ProcessedBill
import com.example.payman.ui.components.SidebarMenu
import com.example.payman.ui.components.SquareButton
import com.example.payman.ui.components.SmartSplitDialog
import com.example.payman.ui.components.UsageDialog
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitterUI(
    onAddGroupClick: () -> Unit,
    onAddBillClick: () -> Unit,
    onAddBillToSection: (String) -> Unit,
    onRecycleBinClick: () -> Unit,
    onLogsClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onSpendingStatsClick: () -> Unit,
    bills: MutableList<ProcessedBill>, // Changed to MutableList for drag and drop
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
    emptySections: SnapshotStateList<String>
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedBillsForSmartSplit by remember { mutableStateOf<List<ProcessedBill>?>(null) }
    var showNewSectionDialog by remember { mutableStateOf(false) }
    var showUsageDialog by remember { mutableStateOf(false) }
    var newSectionName by remember { mutableStateOf("") }
    val context = LocalContext.current

    val collapsedSections = remember { mutableStateListOf<String>() }

    // Drag and Drop State
    var draggedBill by remember { mutableStateOf<ProcessedBill?>(null) }
    var dragOffset by remember { mutableStateOf(IntOffset.Zero) }
    var dropTargetSection by remember { mutableStateOf<String?>(null) }

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
                            onSwiggyDineoutToggle = onSwiggyDineoutToggle
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
                                IconButton(onClick = { showUsageDialog = true }) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = "Usage Guide")
                                }
                                IconButton(onClick = { showNewSectionDialog = true }) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New Section")
                                }
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
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
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SquareButton(
                                    icon = Icons.Default.Add,
                                    label = "Add bill",
                                    onClick = onAddBillClick
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                TextButton(onClick = { showNewSectionDialog = true }) {
                                    Icon(Icons.Default.CreateNewFolder, null, tint = Color(0xFF1DB954))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create a Section", color = Color(0xFF1DB954))
                                }
                            }
                        } else {
                            val groupedBills = bills.groupBy { it.sectionName }
                            // Include ALL existing section names from bills AND the explicitly created empty ones
                            val persistentSectionNames = bills.mapNotNull { it.sectionName }.distinct()
                            val allSectionNames = (persistentSectionNames + emptySections).distinct().sorted()
                            val uncategorizedBills = groupedBills[null] ?: emptyList()

                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Text("Recent Bills", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(vertical = 16.dp))
                                }

                                allSectionNames.forEach { sectionName ->
                                    val sectionBills = groupedBills[sectionName] ?: emptyList()
                                    val isCollapsed = collapsedSections.contains(sectionName)
                                    val isDropTarget = dropTargetSection == sectionName
                                    
                                    item(key = "header_$sectionName") {
                                        Surface(
                                            color = if (isDropTarget) Color(0xFF1DB954).copy(alpha = 0.2f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
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
                                                hasBills = sectionBills.isNotEmpty()
                                            )
                                        }
                                    }
                                    
                                    if (!isCollapsed) {
                                        if (sectionBills.isEmpty()) {
                                            item(key = "empty_$sectionName") {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("No bills in this section", color = Color.LightGray, fontSize = 14.sp)
                                                }
                                            }
                                        } else {
                                            items(sectionBills, key = { it.id }) { bill ->
                                                DraggableBillRow(
                                                    bill = bill,
                                                    onDragStart = { draggedBill = bill },
                                                    onDrag = { offset ->
                                                        dragOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                                                    },
                                                    onDragEnd = {
                                                        draggedBill = null
                                                        dragOffset = IntOffset.Zero
                                                    },
                                                    onClick = { onBillClick(bill) },
                                                    onDelete = { onDeleteBill(bill) },
                                                    allSections = allSectionNames + "General",
                                                    onMoveToSection = { newSection ->
                                                        val idx = bills.indexOfFirst { it.id == bill.id }
                                                        if (idx != -1) {
                                                            val target = if (newSection == "General") null else newSection
                                                            
                                                            // If we move a bill into an "empty" section, it's no longer empty in the UI
                                                            if (target != null) {
                                                                emptySections.remove(target)
                                                            }
                                                            
                                                            // If moving FROM a section made IT empty, we might want to track it
                                                            val oldSection = bill.sectionName
                                                            
                                                            bills[idx] = bill.copy(sectionName = target)
                                                            saveBills(context, bills)
                                                            
                                                            if (oldSection != null && bills.none { it.sectionName == oldSection }) {
                                                                if (!emptySections.contains(oldSection)) {
                                                                    emptySections.add(oldSection)
                                                                }
                                                            }
                                                            
                                                            Toast.makeText(context, "Moved to $newSection", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                            }
                                        }
                                    }
                                }

                                if (uncategorizedBills.isNotEmpty() || allSectionNames.isEmpty()) {
                                    val isGeneralCollapsed = collapsedSections.contains("General")
                                    item(key = "header_uncategorized") {
                                        SectionHeader(
                                            name = "General",
                                            isCollapsed = isGeneralCollapsed,
                                            onToggle = {
                                                if (isGeneralCollapsed) collapsedSections.remove("General")
                                                else collapsedSections.add("General")
                                            },
                                            onSmartSplit = { selectedBillsForSmartSplit = uncategorizedBills },
                                            onAddBill = onAddBillClick,
                                            onDeleteSection = null,
                                            hasBills = uncategorizedBills.isNotEmpty()
                                        )
                                    }
                                    
                                    if (!isGeneralCollapsed) {
                                        items(uncategorizedBills, key = { it.id }) { bill ->
                                            DraggableBillRow(
                                                bill = bill,
                                                onDragStart = {},
                                                onDrag = { _ -> },
                                                onDragEnd = {},
                                                onClick = { onBillClick(bill) },
                                                onDelete = { onDeleteBill(bill) },
                                                allSections = allSectionNames + "General",
                                                onMoveToSection = { newSection ->
                                                    val idx = bills.indexOfFirst { it.id == bill.id }
                                                    if (idx != -1) {
                                                        val target = if (newSection == "General") null else newSection
                                                        
                                                        if (target != null) {
                                                            emptySections.remove(target)
                                                        }
                                                        
                                                        bills[idx] = bill.copy(sectionName = target)
                                                        saveBills(context, bills)
                                                        Toast.makeText(context, "Moved to $newSection", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }
                                }
                                
                                item {
                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                        }
                    }

                    if (bills.isNotEmpty() || emptySections.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomEnd) {
                            FloatingActionButton(onClick = onAddBillClick, containerColor = Color(0xFF1DB954)) {
                                Icon(Icons.Default.Add, contentDescription = "Add Bill", tint = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewSectionDialog) {
        AlertDialog(
            onDismissRequest = { showNewSectionDialog = false },
            containerColor = Color(0xFF36454F),
            title = { Text("New Section", color = Color.White) },
            text = {
                TextField(
                    value = newSectionName,
                    onValueChange = { newSectionName = it },
                    placeholder = { Text("Section Name (e.g. Trip to Goa)", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newSectionName.isNotBlank()) {
                        if (!emptySections.contains(newSectionName)) {
                            emptySections.add(newSectionName)
                        }
                        newSectionName = ""
                        showNewSectionDialog = false
                    }
                }) {
                    Text("Create", color = Color(0xFF1DB954))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSectionDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
    
    if (showUsageDialog) {
        UsageDialog(onDismiss = { showUsageDialog = false })
    }

    selectedBillsForSmartSplit?.let { billsToSplit ->
        SmartSplitDialog(bills = billsToSplit, people = people, onDismiss = { selectedBillsForSmartSplit = null })
    }
}

@Composable
fun DraggableBillRow(
    bill: ProcessedBill,
    onDragStart: () -> Unit,
    onDrag: (androidx.compose.ui.geometry.Offset) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    allSections: List<String>,
    onMoveToSection: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        BillRow(
            bill = bill,
            onClick = onClick,
            onDelete = onDelete,
            modifier = Modifier.pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = { 
                        onDragEnd()
                        showMenu = true 
                    },
                    onDragCancel = { onDragEnd() }
                )
            }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(Color(0xFF2B373E))
        ) {
            Text(
                "Move to Section",
                modifier = Modifier.padding(12.dp),
                color = Color(0xFF1DB954),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            allSections.forEach { section ->
                DropdownMenuItem(
                    text = { Text(section, color = Color.White) },
                    onClick = {
                        onMoveToSection(section)
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    name: String,
    isCollapsed: Boolean,
    onToggle: () -> Unit,
    onSmartSplit: () -> Unit,
    onAddBill: () -> Unit,
    onDeleteSection: (() -> Unit)?,
    hasBills: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.ExpandMore,
                null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1DB954))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasBills) {
                IconButton(onClick = onSmartSplit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.AutoAwesome, "Smart Split", tint = Color(0xFF1DB954), modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = onAddBill, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, "Add Bill", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            if (onDeleteSection != null) {
                IconButton(onClick = onDeleteSection, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Delete Section", tint = Color(0xFFCF6679), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

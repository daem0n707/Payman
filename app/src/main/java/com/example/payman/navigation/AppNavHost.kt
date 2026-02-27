package com.example.payman.navigation

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.payman.data.local.*
import com.example.payman.data.model.Group
import com.example.payman.data.model.Person
import com.example.payman.data.model.ProcessedBill
import com.example.payman.domain.bill.processBillImage
import com.example.payman.ui.addbill.AddBillUI
import com.example.payman.ui.billdetails.BillDetailsUI
import com.example.payman.ui.components.*
import com.example.payman.ui.groups.CreateGroupUI
import com.example.payman.ui.home.BillSplitterUI
import com.example.payman.ui.people.PeopleUI
import kotlinx.coroutines.launch

@Composable
fun AppNavHost() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf("home") }
    var apiKey by remember { mutableStateOf(loadApiKey(context)) }
    val bills = remember { mutableStateListOf<ProcessedBill>().apply { addAll(loadBills(context)) } }
    val people = remember { mutableStateListOf<Person>().apply { addAll(loadPeople(context)) } }
    val groups = remember { mutableStateListOf<Group>().apply { addAll(loadGroups(context)) } }
    val errorLogs = remember { mutableStateListOf<LogEntry>().apply { addAll(loadLogs(context)) } }
    val geminiLogs = remember { mutableStateListOf<LogEntry>().apply { addAll(loadGeminiLogs(context)) } }
    
    val deletedBills = remember { mutableStateMapOf<Long, ProcessedBill>().apply { putAll(loadDeletedBills(context)) } }
    var swiggyDineoutOptionEnabled by remember { mutableStateOf(loadSwiggyHdfcOption(context)) }
    
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val tourState = rememberTourState()

    // Persist empty sections across screens
    val emptySections = remember { mutableStateListOf<String>().apply {
        if (bills.none { it.sectionName == "General" }) {
            add("General")
        }
    } }

    var selectedBill by remember { mutableStateOf<ProcessedBill?>(null) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var showCalculator by remember { mutableStateOf(false) }
    var targetSectionForNewBill by remember { mutableStateOf<String?>(null) }

    // 30-day auto-delete logic
    LaunchedEffect(Unit) {
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        val currentTime = System.currentTimeMillis()
        val toRemove = deletedBills.keys.filter { currentTime - it > thirtyDaysInMillis }
        if (toRemove.isNotEmpty()) {
            toRemove.forEach { deletedBills.remove(it) }
            saveDeletedBills(context, deletedBills)
        }
    }

    BackHandler(enabled = currentScreen != "home") {
        currentScreen = "home"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showCalculator) {
            CalculatorDialog(onDismiss = { showCalculator = false })
        }
        
        val deletePersonAction: (String) -> Unit = { id ->
            people.removeIf { it.id == id }
            savePeople(context, people)
            
            // Update groups to remove the person
            groups.forEachIndexed { index, group ->
                if (group.memberIds.contains(id)) {
                    groups[index] = group.copy(memberIds = group.memberIds.filter { it != id })
                }
            }
            saveGroups(context, groups)
            
            // Update bills to remove the person from participating and assigned IDs
            bills.forEachIndexed { index, bill ->
                var billModified = false
                val newParticipating = bill.participatingPersonIds.filter { it != id }
                if (newParticipating.size != bill.participatingPersonIds.size) {
                    billModified = true
                }
                
                val newItems = bill.items.map { item ->
                    val newAssigned = item.assignedPersonIds.filter { it != id }
                    if (newAssigned.size != item.assignedPersonIds.size) {
                        billModified = true
                        item.copy(assignedPersonIds = newAssigned)
                    } else item
                }
                
                if (billModified) {
                    bills[index] = bill.copy(
                        participatingPersonIds = newParticipating,
                        items = newItems.toMutableList(),
                        payeeId = if (bill.payeeId == id) null else bill.payeeId,
                        payeeName = if (bill.payeeId == id) "" else bill.payeeName
                    )
                }
            }
            saveBills(context, bills)
        }

        when (currentScreen) {
            "home" -> BillSplitterUI(
                onAddGroupClick = { 
                    selectedGroup = null
                    currentScreen = "createGroup" 
                },
                onEditGroupClick = { group ->
                    selectedGroup = group
                    currentScreen = "createGroup"
                },
                onAddBillClick = { 
                    targetSectionForNewBill = "General"
                    currentScreen = "addBill" 
                },
                onAddBillToSection = { sectionName ->
                    targetSectionForNewBill = sectionName
                    currentScreen = "addBill"
                },
                onRecycleBinClick = { currentScreen = "recycleBin" },
                onLogsClick = { currentScreen = "logs" },
                onCalculatorClick = { showCalculator = true },
                onSpendingStatsClick = { currentScreen = "stats" },
                bills = bills,
                onBillClick = { bill ->
                    if (!bill.isProcessing) {
                        selectedBill = bill
                        currentScreen = "billDetails"
                    }
                },
                onDeleteBill = { bill ->
                    bills.remove(bill)
                    deletedBills[System.currentTimeMillis()] = bill
                    saveBills(context, bills)
                    saveDeletedBills(context, deletedBills)
                    Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                },
                onDeleteSection = { sectionName ->
                    val billsToDelete = bills.filter { it.sectionName == sectionName }
                    bills.removeAll(billsToDelete)
                    val timestamp = System.currentTimeMillis()
                    billsToDelete.forEachIndexed { index, bill ->
                        deletedBills[timestamp + index] = bill
                    }
                    saveBills(context, bills)
                    saveDeletedBills(context, deletedBills)
                    Toast.makeText(context, "Section moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                },
                apiKey = apiKey,
                onApiKeyChange = {
                    apiKey = it
                    saveApiKey(context, it)
                },
                onAddPeopleClick = { currentScreen = "people" },
                people = people,
                groups = groups,
                onUpdatePerson = { updatedPerson ->
                    val index = people.indexOfFirst { it.id == updatedPerson.id }
                    if (index != -1) people[index] = updatedPerson
                    savePeople(context, people)
                },
                onDeletePerson = deletePersonAction,
                onUpdateGroup = { updatedGroup ->
                    val index = groups.indexOfFirst { it.id == updatedGroup.id }
                    if (index != -1) groups[index] = updatedGroup
                    saveGroups(context, groups)
                },
                onDeleteGroup = { id ->
                    groups.removeIf { it.id == id }
                    saveGroups(context, groups)
                },
                swiggyDineoutEnabled = swiggyDineoutOptionEnabled,
                onSwiggyDineoutToggle = {
                    swiggyDineoutOptionEnabled = it
                    saveSwiggyHdfcOption(context, it)
                },
                emptySections = emptySections,
                tourState = tourState,
                onStartTour = {
                    val steps = listOf(
                        TourStep("menu_btn", "Sidebar Menu", "Open the sidebar to access settings, people, and groups.", "home") {
                            scope.launch { drawerState.open() }
                        },
                        TourStep("swiggy_hdfc", "HDFC Card Offer", "Enable this if you have a Swiggy HDFC card to get an extra 10% discount on dineout bills.", "home"),
                        TourStep("people_sec", "Manage People", "Add and manage people who you frequently share bills with.", "home"),
                        TourStep("groups_sec", "Manage Groups", "Create groups to quickly add multiple people to a bill at once.", "home") {
                            scope.launch { drawerState.close() }
                        },
                        TourStep("new_section_btn", "Create New Section", "Group your bills into custom categories like 'Trip to Goa' or 'Weekend Brunch'.", "home"),
                        TourStep("add_bill_to_section", "Add Bill to Category", "Quickly add a new bill directly into this specific section.", "home"),
                        TourStep("first_bill", "Manage Bills", "Long press and drag a bill to reorder it within a section or move it to another section.", "home") {
                            if (bills.isNotEmpty()) {
                                selectedBill = bills.first()
                                currentScreen = "billDetails"
                            }
                        },
                        TourStep("add_people_btn", "Add People to Bill", "Add participants from your people or groups list to this specific bill.", "billDetails"),
                        TourStep("assign_payee", "Set Payee", "Select who paid the full amount. This person will be owed money by others.", "billDetails"),
                        TourStep("discount_sec", "Apply Discounts", "Add Dineout discounts or manual deductions to reduce individual shares.", "billDetails"),
                        TourStep("misc_charges", "Misc & Booking Fees", "Add extra charges like platform fees or delivery fees to be split.", "billDetails") {
                            currentScreen = "home"
                            scope.launch { drawerState.open() }
                        },
                        TourStep("recycle_bin_btn", "Recycle Bin", "Deleted bills are held here for 30 days before permanent removal. You can restore them anytime.", "home") {
                            scope.launch { drawerState.close() }
                        },
                        TourStep("smart_split", "Smart Split", "Calculate net transfers between everyone across all bills in this section.", "home")
                    )
                    tourState.startTour(steps)
                },
                drawerState = drawerState
            )
            "recycleBin" -> RecycleBinUI(
                deletedBills = deletedBills,
                onRestore = { bill ->
                    val keyToRemove = deletedBills.filterValues { it.id == bill.id }.keys.firstOrNull()
                    keyToRemove?.let { deletedBills.remove(it) }
                    bills.add(0, bill)
                    saveBills(context, bills)
                    saveDeletedBills(context, deletedBills)
                    currentScreen = "home"
                },
                onPermanentDelete = { billId ->
                    val keyToRemove = deletedBills.filterValues { it.id == billId }.keys.firstOrNull()
                    keyToRemove?.let { deletedBills.remove(it) }
                    saveDeletedBills(context, deletedBills)
                },
                onEmptyBin = {
                    deletedBills.clear()
                    saveDeletedBills(context, deletedBills)
                },
                onDismiss = { currentScreen = "home" }
            )
            "stats" -> SpendingStatsUI(
                bills = bills,
                people = people,
                onDismiss = { currentScreen = "home" }
            )
            "logs" -> LogsUI(
                errorLogs = errorLogs,
                geminiLogs = geminiLogs,
                onClearErrorLogs = {
                    errorLogs.clear()
                    saveLogs(context, errorLogs)
                },
                onClearGeminiLogs = {
                    geminiLogs.clear()
                    saveGeminiLogs(context, geminiLogs)
                },
                onDismiss = { currentScreen = "home" }
            )
            "createGroup" -> CreateGroupUI(
                group = selectedGroup,
                people = people,
                onDismiss = { currentScreen = "home" },
                onGroupCreated = { group ->
                    val index = groups.indexOfFirst { it.id == group.id }
                    if (index != -1) {
                        groups[index] = group
                    } else {
                        groups.add(group)
                    }
                    saveGroups(context, groups)
                    currentScreen = "home"
                }
            )
            "addBill" -> AddBillUI(
                apiKey = apiKey,
                onBillProcessedRequest = { bitmaps, uris ->
                    val section = targetSectionForNewBill ?: "General"
                    val tempBill = ProcessedBill(
                        restaurantName = "Processing...",
                        items = mutableListOf(),
                        tax = 0.0,
                        serviceCharge = 0.0,
                        miscFees = 0.0,
                        bitmap = bitmaps.firstOrNull(),
                        imageUri = uris.firstOrNull(),
                        isProcessing = true,
                        sectionName = section
                    )
                    bills.add(0, tempBill)
                    currentScreen = "home"
                    
                    scope.launch {
                        processBillImage(context, bitmaps, apiKey, uris,
                            onSuccess = { realBill ->
                                val index = bills.indexOfFirst { it.id == tempBill.id }
                                if (index != -1) {
                                    val finalizedBill = realBill.copy(
                                        id = tempBill.id,
                                        sectionName = section,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    bills[index] = finalizedBill
                                    saveBills(context, bills)

                                    // Refresh logs from storage since BillProcessor writes to it
                                    geminiLogs.clear()
                                    geminiLogs.addAll(loadGeminiLogs(context))
                                }
                            },
                            onError = { msg ->
                                val index = bills.indexOfFirst { it.id == tempBill.id }
                                if (index != -1) {
                                    bills[index] = tempBill.copy(restaurantName = "Error occured; check logs", isProcessing = false)
                                }

                                // Refresh logs from storage
                                errorLogs.clear()
                                errorLogs.addAll(loadLogs(context))
                            }
                        )
                    }
                },
                onDismiss = { currentScreen = "home" }
            )
            "billDetails" -> BillDetailsUI(
                bill = selectedBill,
                people = people,
                groups = groups,
                onDismiss = { currentScreen = "home" },
                onBillUpdated = { updatedBill ->
                    val index = bills.indexOfFirst { it.id == updatedBill.id }
                    if (index != -1) {
                        bills[index] = updatedBill
                        saveBills(context, bills)
                    }
                    selectedBill = updatedBill
                },
                swiggyHdfcOptionEnabled = swiggyDineoutOptionEnabled,
                tourState = tourState
            )
            "people" -> PeopleUI(
                people = people,
                bills = bills,
                onDismiss = { currentScreen = "home" },
                onAddPerson = { name ->
                    people.add(Person(name = name))
                    savePeople(context, people)
                },
                onDeletePerson = deletePersonAction
            )
        }
        TourHost(state = tourState, currentScreen = currentScreen)
    }
}

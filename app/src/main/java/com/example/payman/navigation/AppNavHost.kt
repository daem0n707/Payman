package com.example.payman.navigation

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.payman.data.local.*
import com.example.payman.data.model.Group
import com.example.payman.data.model.Person
import com.example.payman.data.model.ProcessedBill
import com.example.payman.domain.bill.processBillImage
import com.example.payman.ui.addbill.AddBillUI
import com.example.payman.ui.billdetails.BillDetailsUI
import com.example.payman.ui.components.CalculatorDialog
import com.example.payman.ui.components.LogsUI
import com.example.payman.ui.components.RecycleBinUI
import com.example.payman.ui.components.SpendingStatsUI
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
    var swiggyDineoutOptionEnabled by remember { mutableStateOf(loadSwiggyDineoutOption(context)) }
    
    // Persist empty sections across screens
    val emptySections = remember { mutableStateListOf<String>() }

    var selectedBill by remember { mutableStateOf<ProcessedBill?>(null) }
    var showCalculator by remember { mutableStateOf(false) }
    var targetSectionForNewBill by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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

    if (showCalculator) {
        CalculatorDialog(onDismiss = { showCalculator = false })
    }

    when (currentScreen) {
        "home" -> BillSplitterUI(
            onAddGroupClick = { currentScreen = "createGroup" },
            onAddBillClick = { 
                targetSectionForNewBill = null
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
            onDeletePerson = { id ->
                people.removeIf { it.id == id }
                savePeople(context, people)
            },
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
                saveSwiggyDineoutOption(context, it)
            },
            emptySections = emptySections
        )
        "recycleBin" -> RecycleBinUI(
            deletedBills = deletedBills,
            onRestore = { bill ->
                deletedBills.values.remove(bill)
                bills.add(0, bill)
                saveBills(context, bills)
                saveDeletedBills(context, deletedBills)
                currentScreen = "home"
            },
            onPermanentDelete = { id ->
                deletedBills.entries.removeIf { it.value.id == id }
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
            people = people,
            onDismiss = { currentScreen = "home" },
            onGroupCreated = { group ->
                groups.add(group)
                saveGroups(context, groups)
                currentScreen = "home"
            }
        )
        "addBill" -> AddBillUI(
            apiKey = apiKey,
            onBillProcessedRequest = { bitmaps, uris ->
                val tempBill = ProcessedBill(
                    restaurantName = "Processing...",
                    items = mutableListOf(),
                    tax = 0.0,
                    serviceCharge = 0.0,
                    miscFees = 0.0,
                    bitmap = bitmaps.firstOrNull(),
                    imageUri = uris.firstOrNull(),
                    isProcessing = true,
                    sectionName = targetSectionForNewBill
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
                                    sectionName = targetSectionForNewBill,
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
            swiggyDineoutOptionEnabled = swiggyDineoutOptionEnabled
        )
        "people" -> PeopleUI(
            people = people,
            onDismiss = { currentScreen = "home" },
            onAddPerson = { name ->
                people.add(Person(name = name))
                savePeople(context, people)
            }
        )
    }
}

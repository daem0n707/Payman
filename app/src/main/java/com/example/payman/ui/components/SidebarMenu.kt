package com.example.payman.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.payman.data.model.Group
import com.example.payman.data.model.Person

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarMenu(
    onAddGroupClick: () -> Unit,
    onPeopleClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onLogsClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onSpendingStatsClick: () -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    groups: List<Group>,
    people: List<Person>,
    onUpdatePerson: (Person) -> Unit,
    onDeletePerson: (String) -> Unit,
    onUpdateGroup: (Group) -> Unit,
    onDeleteGroup: (String) -> Unit,
    swiggyDineoutEnabled: Boolean,
    onSwiggyDineoutToggle: (Boolean) -> Unit
) {
    var editingPerson by remember { mutableStateOf<Person?>(null) }
    var editingGroup by remember { mutableStateOf<Group?>(null) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var groupsExpanded by remember { mutableStateOf(false) }
    var peopleExpanded by remember { mutableStateOf(false) }
    var showUsageDialog by remember { mutableStateOf(false) }
    
    // Store which specific group is expanded
    val expandedGroupIds = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("Groq API Key") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                    Icon(
                        imageVector = if (apiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1DB954),
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onSwiggyDineoutToggle(!swiggyDineoutEnabled) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Swiggy Dineout Card", color = Color.White, fontSize = 14.sp)
            Switch(
                checked = swiggyDineoutEnabled,
                onCheckedChange = onSwiggyDineoutToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF1DB954),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            // Usage Guide Item
            item {
                SidebarItem(Icons.Default.HelpOutline, "Usage Guide", { showUsageDialog = true })
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            }

            // Stats Item
            item {
                SidebarItem(Icons.Default.BarChart, "Spending Stats", onSpendingStatsClick)
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            }

            // Groups Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { groupsExpanded = !groupsExpanded }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (groupsExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        "Groups",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    IconButton(onClick = {
                        onAddGroupClick()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Group", tint = Color(0xFF1DB954))
                    }
                }
            }
            if (groupsExpanded) {
                items(groups) { group ->
                    val isGroupExpanded = expandedGroupIds.contains(group.id)
                    Column {
                        ListItem(
                            headlineContent = { Text(group.name, color = Color.White) },
                            leadingContent = {
                                Icon(
                                    imageVector = if (isGroupExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp).clickable {
                                        if (isGroupExpanded) expandedGroupIds.remove(group.id) else expandedGroupIds.add(group.id)
                                    }
                                )
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { editingGroup = group }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                    IconButton(onClick = { onDeleteGroup(group.id) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                                }
                            },
                            modifier = Modifier.clickable {
                                if (isGroupExpanded) expandedGroupIds.remove(group.id) else expandedGroupIds.add(group.id)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        
                        if (isGroupExpanded) {
                            Column(modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)) {
                                val members = people.filter { group.memberIds.contains(it.id) }
                                if (members.isEmpty()) {
                                    Text("No members", color = Color.Gray, fontSize = 12.sp)
                                } else {
                                    members.forEach { member ->
                                        Text(member.name, color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // People Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { peopleExpanded = !peopleExpanded }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (peopleExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        "People",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    IconButton(onClick = {
                        onPeopleClick()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Person", tint = Color(0xFF1DB954))
                    }
                }
            }
            if (peopleExpanded) {
                items(people) { person ->
                    ListItem(
                        headlineContent = { Text(person.name, color = Color.White) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { editingPerson = person }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray) }
                                IconButton(onClick = { onDeletePerson(person.id) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.Gray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

        SidebarItem(Icons.Default.Calculate, "Calculator", onCalculatorClick)
        SidebarItem(Icons.Default.DeleteSweep, "Recycle Bin", onRecycleBinClick)
        SidebarItem(Icons.Default.Terminal, "Logs", onLogsClick)
    }

    if (showUsageDialog) {
        UsageDialog(onDismiss = { showUsageDialog = false })
    }

    if (editingPerson != null) {
        var newName by remember { mutableStateOf(editingPerson!!.name) }
        AlertDialog(
            onDismissRequest = { editingPerson = null },
            title = { Text("Edit Person") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") }) },
            confirmButton = {
                Button(onClick = {
                    onUpdatePerson(editingPerson!!.copy(name = newName))
                    editingPerson = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingPerson = null }) { Text("Cancel") } }
        )
    }

    if (editingGroup != null) {
        EditGroupDialog(
            group = editingGroup!!,
            people = people,
            onDismiss = { editingGroup = null },
            onSave = { updatedGroup ->
                onUpdateGroup(updatedGroup)
                editingGroup = null
            }
        )
    }
}

@Composable
fun EditGroupDialog(
    group: Group,
    people: List<Person>,
    onDismiss: () -> Unit,
    onSave: (Group) -> Unit
) {
    var newName by remember { mutableStateOf(group.name) }
    val selectedMemberIds = remember { mutableStateListOf<String>().apply { addAll(group.memberIds) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF36454F),
        title = { Text("Edit Group", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF1DB954),
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Members", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                
                Box(modifier = Modifier.height(200.dp)) {
                    LazyColumn {
                        items(people) { person ->
                            val isSelected = selectedMemberIds.contains(person.id)
                            ListItem(
                                headlineContent = { Text(person.name, color = Color.White) },
                                trailingContent = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedMemberIds.add(person.id) else selectedMemberIds.remove(person.id)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1DB954))
                                    )
                                },
                                modifier = Modifier.clickable {
                                    if (isSelected) selectedMemberIds.remove(person.id) else selectedMemberIds.add(person.id)
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(group.copy(name = newName, memberIds = selectedMemberIds.toList())) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
            ) { Text("Save", color = Color.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        }
    )
}

@Composable
fun UsageDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF36454F),
        title = { Text("Usage Guide", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                item {
                    UsageSection("Payee Selection", "Click on the Restaurant Name in the Bill Details screen to select a Payee. The Payee is the person who initially paid the bill. Other participants will owe their shares to this person.")
                    UsageSection("Smart Split", "The 'AutoAwesome' icon in each section header performs a Smart Split across all bills in that section. It simplifies debts (e.g., if A owes B ₹10 and B owes A ₹4, A simply owes B ₹6) and tracks who owes what based on the designated Payees.")
                    UsageSection("Lengthy Bills", "For long receipts that don't fit in one photo, you can capture multiple images. The app will merge the text and use AI to de-duplicate any overlapping items.")
                    UsageSection("People & Groups", "Add people individually or create groups in Settings. You can quickly add all members of a group to a bill by selecting the group in the 'Add People' dialog.")
                    UsageSection("How Split Works", "By default, items are split among all participating people. You can assign specific people to an item to split only that item's cost (and quantity) among them.")
                    UsageSection("Discounts", "Discounts (Fixed or Percentage) are applied to the subtotal of items, taxes, and service charges. You can also toggle the 'Swiggy Dineout Card' for an additional 10% discount.")
                    UsageSection("Misc Fees", "Misc Fees are added to the final total after all discounts have been applied. They are split equally among all participating people and are NOT subject to discount logic.")
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

@Composable
fun SidebarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1DB954))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = Color.White, fontSize = 16.sp)
    }
}

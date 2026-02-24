package com.example.payman.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.payman.data.model.Person
import com.example.payman.data.model.ProcessedBill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleUI(
    people: List<Person>,
    bills: List<ProcessedBill>,
    onDismiss: () -> Unit,
    onAddPerson: (String) -> Unit,
    onDeletePerson: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var personToDelete by remember { mutableStateOf<Person?>(null) }
    var showDeleteError by remember { mutableStateOf<Pair<Person, List<String>>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("People") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF36454F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF36454F))
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName, 
                    onValueChange = { newName = it }, 
                    label = { Text("Name") }, 
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1DB954),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF1DB954),
                        unfocusedLabelColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { if (newName.isNotBlank()) { onAddPerson(newName); newName = "" } },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                ) { Text("Add", color = Color.Black) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(people) { person ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2B373E))) {
                        ListItem(
                            headlineContent = { Text(person.name, color = Color.White) },
                            trailingContent = {
                                IconButton(onClick = {
                                    val billsWithPerson = bills.filter { bill ->
                                        bill.participatingPersonIds.contains(person.id) ||
                                                bill.payeeId == person.id ||
                                                bill.items.any { it.assignedPersonIds.contains(person.id) }
                                    }.map { it.restaurantName }
                                    
                                    if (billsWithPerson.isNotEmpty()) {
                                        showDeleteError = person to billsWithPerson.distinct()
                                    } else {
                                        personToDelete = person
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }

    if (personToDelete != null) {
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            title = { Text("Delete Person") },
            text = { Text("Are you sure you want to delete ${personToDelete?.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    personToDelete?.let { onDeletePerson(it.id) }
                    personToDelete = null
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteError != null) {
        AlertDialog(
            onDismissRequest = { showDeleteError = null },
            title = { Text("Cannot Delete Person") },
            text = {
                Column {
                    Text("${showDeleteError?.first?.name} is part of the following bills:")
                    Spacer(modifier = Modifier.height(8.dp))
                    showDeleteError?.second?.forEach { billName ->
                        Text("• $billName", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Force deleting will remove them from all these bills. This action cannot be undone.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteError?.first?.let { onDeletePerson(it.id) }
                    showDeleteError = null
                }) {
                    Text("Force Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteError = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

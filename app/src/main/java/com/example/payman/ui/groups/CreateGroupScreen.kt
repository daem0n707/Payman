package com.example.payman.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.payman.data.model.Group
import com.example.payman.data.model.Person

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupUI(people: List<Person>, onDismiss: () -> Unit, onGroupCreated: (Group) -> Unit) {
    var groupName by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create group") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                actions = { 
                    TextButton(onClick = { 
                        if (groupName.isNotBlank()) onGroupCreated(Group(name = groupName, memberIds = selectedMembers.toList())) 
                    }) { 
                        Text("Done", fontWeight = FontWeight.Bold) 
                    } 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(padding).padding(24.dp)) {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF1DB954), cursorColor = Color(0xFF1DB954))
            )
            Spacer(modifier = Modifier.height(32.dp))
            if (people.isNotEmpty()) {
                Text("Add Members", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(people) { person ->
                        val isSelected = selectedMembers.contains(person.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1DB954).copy(alpha = 0.2f) else Color(0xFF1E1E1E))
                                .clickable {
                                    if (isSelected) selectedMembers.remove(person.id) else selectedMembers.add(person.id)
                                }
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF1DB954) else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(person.name, color = if (isSelected) Color.White else Color.Gray, fontSize = 16.sp)
                        }
                    }
                }
            } else {
                Text("No people found. Add them from the sidebar first.", color = Color.Gray)
            }
        }
    }
}

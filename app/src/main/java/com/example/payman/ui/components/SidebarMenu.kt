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
    onEditGroupClick: (Group) -> Unit,
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
    onSwiggyDineoutToggle: (Boolean) -> Unit,
    tourState: TourState,
    onUsageClick: () -> Unit
) {
    var apiKeyVisible by remember { mutableStateOf(false) }
    var groupsExpanded by remember { mutableStateOf(false) }
    var peopleExpanded by remember { mutableStateOf(false) }
    
    // Store which specific group is expanded
    val expandedGroupIds = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = onUsageClick) {
                Icon(Icons.Default.HelpOutline, contentDescription = "Usage Guide", tint = Color.Gray)
            }
        }

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("Groq API Key") },
            modifier = Modifier.fillMaxWidth().tourTarget(tourState, "api_key"),
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
            modifier = Modifier
                .fillMaxWidth()
                .tourTarget(tourState, "swiggy_hdfc")
                .clickable { onSwiggyDineoutToggle(!swiggyDineoutEnabled) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Swiggy HDFC 10%", color = Color.White, fontSize = 14.sp)
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
            // Stats Item
            item {
                SidebarItem(
                    icon = Icons.Default.BarChart, 
                    label = "Spending Stats", 
                    onClick = onSpendingStatsClick,
                    modifier = Modifier.tourTarget(tourState, "stats_btn")
                )
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            }

            // Groups Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tourTarget(tourState, "groups_sec")
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
                    IconButton(onClick = onAddGroupClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Groups", tint = Color(0xFF1DB954))
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
                                IconButton(onClick = { onEditGroupClick(group) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Group", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                            },
                            modifier = Modifier.clickable {
                                if (isGroupExpanded) expandedGroupIds.remove(group.id) else expandedGroupIds.add(group.id)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        
                        if (isGroupExpanded) {
                            Column(modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)) {
                                val memberIds = group.memberIds ?: emptyList()
                                val members = people.filter { memberIds.contains(it.id) }
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
                        .tourTarget(tourState, "people_sec")
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
                    IconButton(onClick = onPeopleClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit People", tint = Color(0xFF1DB954))
                    }
                }
            }
            if (peopleExpanded) {
                items(people) { person ->
                    ListItem(
                        headlineContent = { Text(person.name, color = Color.White) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.Gray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

        SidebarMenuFooter(onCalculatorClick, onRecycleBinClick, onLogsClick, tourState)
    }
}

@Composable
fun SidebarMenuFooter(onCalculatorClick: () -> Unit, onRecycleBinClick: () -> Unit, onLogsClick: () -> Unit, tourState: TourState) {
    SidebarItem(Icons.Default.Calculate, "Calculator", onCalculatorClick)
    SidebarItem(Icons.Default.DeleteSweep, "Recycle Bin", onRecycleBinClick, modifier = Modifier.tourTarget(tourState, "recycle_bin_btn"))
    SidebarItem(Icons.Default.Terminal, "Logs", onLogsClick)
}

@Composable
fun SidebarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1DB954))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = Color.White, fontSize = 16.sp)
    }
}

package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FamilyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: FamilyViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.allUsers.collectAsState()
    val pendingMembers by viewModel.pendingUsers.collectAsState()

    var broadcastText by remember { mutableStateOf("") }
    var inviteCodeSetting by remember { mutableStateOf("FAM99") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Admin", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Administrative Control Center", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Manage private registrations, cloud synchronization, and safety codes.", fontSize = 11.sp)
                    }
                }
            }
        }

        // 1. Pending Registrations List
        item {
            Text("Pending Registrations Approval", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        }

        if (pendingMembers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                        Text("No pending member registrations.", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }
        } else {
            items(pendingMembers) { member ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FamilyAvatar(name = member.name, seed = member.avatarColorSeed, size = 44.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Relation: ${member.role}", fontSize = 12.sp, color = Color.Gray)
                            Text("Email: ${member.email}", fontSize = 11.sp, color = Color.Gray)
                            Text("Invite code used: ${member.inviteCodeUsed}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }

                        // Action controllers
                        Row {
                            IconButton(onClick = { viewModel.approvePendingMember(member.id) }, modifier = Modifier.testTag("approve_btn_${member.id}")) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = "Approve", tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                            }
                            IconButton(onClick = { viewModel.rejectPendingMember(member.id) }, modifier = Modifier.testTag("reject_btn_${member.id}")) {
                                Icon(Icons.Outlined.Cancel, contentDescription = "Reject", tint = Color.Red, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }

        // 2. Global Broadcast Dispatcher
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Broadcast System Announcement", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("This dispatches a high-priority push announcement message inside the Main Family Group Chat.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = broadcastText,
                        onValueChange = { broadcastText = it },
                        placeholder = { Text("E.g. Dinner is ready! Come to the main table now everyone. 🥣🥘") },
                        modifier = Modifier.fillMaxWidth().testTag("broadcast_input_cell")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (broadcastText.isNotBlank()) {
                                viewModel.broadcastSystemMessage(broadcastText)
                                broadcastText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("broadcast_submit"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Campaign, contentDescription = "Broadcast")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dispatch Broadcast")
                    }
                }
            }
        }

        // 3. System Backups & Storage Capacity Module
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Secure Cloud Sync & Backups", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ACTIVE", color = Color(0xFF15803D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Total private resources synced locally & automatically archived on Cloud database:", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))

                    // Storage graph
                    LinearProgressIndicator(
                        progress = { 0.12f },
                        color = Color(0xFF10B981),
                        trackColor = Color.LightGray,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1.8 GB used", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("15 GB available", fontSize = 12.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Private Family Code", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Requirements for joining link requests", fontSize = 11.sp, color = Color.Gray)
                        }
                        OutlinedTextField(
                            value = inviteCodeSetting,
                            onValueChange = { inviteCodeSetting = it },
                            modifier = Modifier.width(100.dp).height(50.dp)
                        )
                    }
                }
            }
        }

        // 4. Family Members Inventory List
        item {
            Text("Registered Family Members", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        }

        items(users.filter { it.isApproved && it.id != "me" }) { member ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FamilyAvatar(name = member.name, seed = member.avatarColorSeed, size = 40.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(member.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(member.role, fontSize = 12.sp, color = Color.Gray)
                        Text(member.phoneNo, fontSize = 11.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { viewModel.blockMember(member.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Block member", tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

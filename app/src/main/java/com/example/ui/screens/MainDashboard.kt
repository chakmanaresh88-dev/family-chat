package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Announcement
import com.example.data.Event
import com.example.data.Message
import com.example.data.Photo
import com.example.data.User
import com.example.ui.CallType
import com.example.ui.FamilyViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: FamilyViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.allUsers.collectAsState()
    val rawAnnouncements by viewModel.allAnnouncements.collectAsState()
    val rawEvents by viewModel.allEvents.collectAsState()
    val rawPhotos by viewModel.allPhotos.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allMessages by viewModel.allMessages.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showMemberSwitchSheet by remember { mutableStateOf(false) }

    // Dialog Sheets
    var showAddAnnouncementDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddPhotoDialog by remember { mutableStateOf(false) }
    var showSecurityInfoDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FamilyRestroom,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                "FamilyConnect",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Logged in as: ${currentUser?.name ?: "Unknown"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSecurityInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Security Status",
                            tint = Color(0xFF25D366)
                        )
                    }
                    IconButton(onClick = { showMemberSwitchSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = "Switch User Role",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chats") },
                    label = { Text("Chats") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Campaign, contentDescription = "Announcements") },
                    label = { Text("Bulletin") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = "Events") },
                    label = { Text("Events") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.Collections, contentDescription = "Gallery") },
                    label = { Text("Gallery") }
                )
                if (currentUser?.role == "Admin" || currentUser?.id == "me") {
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Admin") },
                        label = { Text("Admin") },
                        modifier = Modifier.testTag("admin_tab_button")
                    )
                }
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                1 -> {
                    FloatingActionButton(
                        onClick = { showAddAnnouncementDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "New Announcement", tint = Color.White)
                    }
                }
                2 -> {
                    FloatingActionButton(
                        onClick = { showAddEventDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Filled.Event, contentDescription = "New Event", tint = Color.White)
                    }
                }
                3 -> {
                    FloatingActionButton(
                        onClick = { showAddPhotoDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = "New Photo", tint = Color.White)
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main views switcher
            when (selectedTab) {
                0 -> ChatsTab(viewModel, users, allMessages)
                1 -> AnnouncementsTab(viewModel, rawAnnouncements, currentUser)
                2 -> EventsTab(viewModel, rawEvents, currentUser)
                3 -> GalleryTab(viewModel, rawPhotos, currentUser)
                4 -> AdminDashboardScreen(viewModel)
            }

            // Member Switcher bottom Modal
            if (showMemberSwitchSheet) {
                AlertDialog(
                    onDismissRequest = { showMemberSwitchSheet = false },
                    title = { Text("Simulation: Roleplay Switcher") },
                    text = {
                        Column {
                            Text(
                                "Switch your active account instantly to verify 1-to-1 conversations, dynamic read receipts, visual typing statuses, and administrative dashboards.",
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            LazyColumn(modifier = Modifier.height(240.dp)) {
                                items(users.filter { it.isApproved }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.switchActiveUser(item.id)
                                                showMemberSwitchSheet = false
                                            }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FamilyAvatar(name = item.name, seed = item.avatarColorSeed, size = 36.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp,
                                                color = if (currentUser?.id == item.id) MaterialTheme.colorScheme.primary else Color.Unspecified
                                            )
                                            Text(item.role, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (currentUser?.id == item.id) {
                                            Icon(Icons.Filled.Check, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMemberSwitchSheet = false }) {
                            Text("Dismiss")
                        }
                    }
                )
            }

            // Security Encryption info Dialog
            if (showSecurityInfoDialog) {
                AlertDialog(
                    onDismissRequest = { showSecurityInfoDialog = false },
                    icon = { Icon(Icons.Filled.EnhancedEncryption, contentDescription = "Encrypted", tint = Color(0xFF128C7E), modifier = Modifier.size(40.dp)) },
                    title = { Text("End-to-End Encrypted", textAlign = TextAlign.Center) },
                    text = {
                        Text(
                            "FamilyConnect relies on simulated local-first SQLite encryption and secure cloud sync backup schemas. All messaging pipelines, video call packets, and shared media files are protected out of reach using robust AES-256 protocols.",
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    },
                    confirmButton = {
                        Button(onClick = { showSecurityInfoDialog = false }) {
                            Text("Great!")
                        }
                    }
                )
            }

            // Add Event Dialog
            if (showAddEventDialog) {
                var titleText by remember { mutableStateOf("") }
                var descText by remember { mutableStateOf("") }
                var dateText by remember { mutableStateOf("2026-06-10") }
                var timeText by remember { mutableStateOf("18:00") }
                var locText by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showAddEventDialog = false },
                    title = { Text("Schedule Family Event") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = titleText, onValueChange = { titleText = it }, label = { Text("Event Name") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = descText, onValueChange = { descText = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = dateText, onValueChange = { dateText = it }, label = { Text("Date (yyyy-mm-dd)") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = timeText, onValueChange = { timeText = it }, label = { Text("Time (hh:mm)") }, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(value = locText, onValueChange = { locText = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (titleText.isNotBlank()) {
                                viewModel.addEvent(titleText, descText, dateText, timeText, locText)
                                showAddEventDialog = false
                            }
                        }) {
                            Text("Create Event")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddEventDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Add Announcement Dialog
            if (showAddAnnouncementDialog) {
                var titleText by remember { mutableStateOf("") }
                var contextText by remember { mutableStateOf("") }
                var pinChecked by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showAddAnnouncementDialog = false },
                    title = { Text("Broaden Announcement bulletin") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = titleText, onValueChange = { titleText = it }, label = { Text("Header Title") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = contextText, onValueChange = { contextText = it }, label = { Text("Content description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = pinChecked, onCheckedChange = { pinChecked = it })
                                Text("Pin to the top of the board")
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (titleText.isNotBlank() && contextText.isNotBlank()) {
                                viewModel.addAnnouncement(titleText, contextText, pinChecked)
                                showAddAnnouncementDialog = false
                            }
                        }) {
                            Text("Broadcast")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddAnnouncementDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Add Photo Dialog (Mock Upload)
            if (showAddPhotoDialog) {
                var imgSelectedIdx by remember { mutableStateOf(0) }
                var capInput by remember { mutableStateOf("") }
                val mockupPhotos = listOf(
                    "https://images.unsplash.com/photo-1511285560929-80b456fea0bc?w=500&auto=format&fit=crop", // Wedding beach
                    "https://images.unsplash.com/photo-1473177104440-ffee2f376098?w=500&auto=format&fit=crop", // Cozy fire
                    "https://images.unsplash.com/photo-1513151233558-d860c5398176?w=500&auto=format&fit=crop"  // Party lights
                )

                AlertDialog(
                    onDismissRequest = { showAddPhotoDialog = false },
                    title = { Text("Post Shared Family Photo") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Select an image from gallery preview:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                mockupPhotos.forEachIndexed { idx, url ->
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { imgSelectedIdx = idx }
                                            .background(if (imgSelectedIdx == idx) MaterialTheme.colorScheme.primary else Color.LightGray)
                                            .padding(if (imgSelectedIdx == idx) 3.dp else 0.dp)
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = capInput,
                                onValueChange = { capInput = it },
                                label = { Text("Write Photo Caption...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.addPhotoToGallery(capInput, mockupPhotos[imgSelectedIdx])
                            showAddPhotoDialog = false
                        }) {
                            Text("Share Photo")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddPhotoDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

// ------------------------------------------------------------
// CHATS LIST COMPOSABLE
// ------------------------------------------------------------
@Composable
fun ChatsTab(
    viewModel: FamilyViewModel,
    users: List<User>,
    messages: List<Message>
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = remember(users, searchQuery) {
        users.filter { user ->
            user.isApproved &&
            user.id != "me" &&
            (user.name.contains(searchQuery, ignoreCase = true) ||
             user.role.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search family members or messages...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Static group chat at top if matches search
            if ("family".contains(searchQuery, ignoreCase = true) || searchQuery.isEmpty()) {
                item {
                    val groupUser = User(
                        id = "family_group",
                        name = "🌟 Family Main Hall 🏡",
                        role = "Shared Broad Group Chat",
                        phoneNo = "",
                        email = "",
                        avatarColorSeed = 6, // cyan group index
                        isOnline = true,
                        isApproved = true
                    )
                    ChatMemberItem(groupUser, messages.filter { it.chatId == "family_group" }) {
                        viewModel.selectChat(groupUser)
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
                }
            }

            items(filteredUsers) { user ->
                // Sort private chatId combination
                val privChatId = listOf("me", user.id).sorted().joinToString("-")
                val privMsgs = messages.filter { it.chatId == privChatId }

                ChatMemberItem(user, privMsgs) {
                    viewModel.selectChat(user)
                }
                HorizontalDivider(modifier = Modifier.padding(start = 76.dp))
            }

            if (filteredUsers.isEmpty() && searchQuery.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No matching family channels found.", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMemberItem(
    user: User,
    chatMessagesBefore: List<Message>,
    onClick: () -> Unit
) {
    val lastMsg = chatMessagesBefore.lastOrNull()
    val unreadCount = chatMessagesBefore.count { it.senderId != "me" && it.status != "seen" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FamilyAvatar(
            name = user.name,
            seed = user.avatarColorSeed,
            size = 52.dp,
            isOnline = user.isOnline
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (lastMsg != null) {
                    val formattedTime = remember(lastMsg.timestamp) {
                        val mTime = java.util.Date(lastMsg.timestamp)
                        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                        sdf.format(mTime)
                    }
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = if (unreadCount > 0) MaterialTheme.colorScheme.primary else Color.Gray,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (user.isTyping) {
                    Text(
                        text = "typing...",
                        fontSize = 13.sp,
                        color = Color(0xFF25D366),
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (lastMsg != null) {
                    if (lastMsg.senderId == "me") {
                        val checkIcon = when (lastMsg.status) {
                            "seen" -> Icons.Filled.DoneAll
                            "delivered" -> Icons.Filled.DoneAll
                            else -> Icons.Filled.Done
                        }
                        val checkColor = if (lastMsg.status == "seen") Color(0xFF53BDEB) else Color.Gray
                        Icon(
                            imageVector = checkIcon,
                            contentDescription = "status",
                            tint = checkColor,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = if (lastMsg.isDeleted) "This message was deleted." else lastMsg.text,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = user.role,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------
// BULLETIN ANNOUNCEMENTS TAB
// ------------------------------------------------------------
@Composable
fun AnnouncementsTab(
    viewModel: FamilyViewModel,
    announcements: List<Announcement>,
    currentUser: User?
) {
    if (announcements.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Campaign, contentDescription = "empty", modifier = Modifier.size(72.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No announcements posted yet.", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Announcements Bulletin 📢",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(announcements) { announce ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (announce.isPinned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (announce.isPinned) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = "Pinned",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = announce.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentUser?.role == "Admin" || currentUser?.id == "me" || announce.authorName.contains("Me")) {
                                IconButton(onClick = { viewModel.removeAnnouncement(announce.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = announce.content, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Posted by: ${announce.authorName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            val fDate = remember(announce.timestamp) {
                                val d = java.util.Date(announce.timestamp)
                                val format = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                format.format(d)
                            }
                            Text(fDate, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------
// EVENTS TAB COMPOSABLE
// ------------------------------------------------------------
@Composable
fun EventsTab(
    viewModel: FamilyViewModel,
    events: List<Event>,
    currentUser: User?
) {
    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "empty", modifier = Modifier.size(72.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No upcoming events scheduled.", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Upcoming Family Plans 🗓🌻",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(events) { event ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.EventNote, contentDescription = "Date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = event.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentUser?.role == "Admin" || currentUser?.id == "me" || event.creatorName.contains("Me")) {
                                IconButton(onClick = { viewModel.removeEvent(event.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(event.description, fontSize = 14.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📍 ${event.location}", fontSize = 12.sp, color = Color.Gray)
                            Text("🕒 ${event.date} @ ${event.time}", fontSize = 12.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Shared by: ${event.creatorName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                            var isGoing by remember { mutableStateOf(false) }
                            Button(
                                onClick = { isGoing = !isGoing },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isGoing) Color(0xFF25D366) else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(if (isGoing) "✓ Attending" else "RSVP Going", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------
// GALLERY COMPOSABLE
// ------------------------------------------------------------
@Composable
fun GalleryTab(
    viewModel: FamilyViewModel,
    photos: List<Photo>,
    currentUser: User?
) {
    if (photos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Collections, contentDescription = "empty", modifier = Modifier.size(72.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your Family Photo Gallery is empty.", color = Color.Gray)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                "Shared Family Album 📸✨",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Text(
                "Memories posted inside the private network secure cloud",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos) { photo ->
                    var showDetailDialog by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable { showDetailDialog = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            Box(modifier = Modifier.weight(1f)) {
                                AsyncImage(
                                    model = photo.uri,
                                    contentDescription = photo.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = photo.caption,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(8.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (showDetailDialog) {
                        AlertDialog(
                            onDismissRequest = { showDetailDialog = false },
                            title = { Text("Shared Memory Details") },
                            text = {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    ) {
                                        AsyncImage(
                                            model = photo.uri,
                                            contentDescription = "Details",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(photo.caption, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Uploaded by: ${photo.uploadedBy}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            confirmButton = {
                                if (currentUser?.role == "Admin" || currentUser?.id == "me" || photo.uploadedBy.contains("Me")) {
                                    TextButton(onClick = {
                                        viewModel.removePhoto(photo.id)
                                        showDetailDialog = false
                                    }) {
                                        Text("Delete memory", color = Color.Red)
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDetailDialog = false }) {
                                    Text("Dismiss")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

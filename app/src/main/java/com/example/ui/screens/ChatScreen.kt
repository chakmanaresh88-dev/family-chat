package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Message
import com.example.data.User
import com.example.ui.CallType
import com.example.ui.FamilyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: FamilyViewModel,
    modifier: Modifier = Modifier
) {
    val partner by viewModel.selectedChatPartner.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val replyToMessage by viewModel.replyToMessage.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Recording simulation states
    var isRecordingVoicelist by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }

    // Dropdown Dialog state
    var selectedMsgForOptions by remember { mutableStateOf<Message?>(null) }
    var showMsgOptionsDialog by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    // Scroll to bottom on load or new messages
    LaunchedEffect(messages.size, partner?.isTyping) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice recording timer simulation
    LaunchedEffect(isRecordingVoicelist) {
        if (isRecordingVoicelist) {
            recordingSeconds = 0
            while (isRecordingVoicelist) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    val actualPartner = partner ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { viewModel.selectChat(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            // Can click header block to view stats
                        }
                    ) {
                        FamilyAvatar(
                            name = actualPartner.name,
                            seed = actualPartner.avatarColorSeed,
                            size = 38.dp,
                            isOnline = actualPartner.isOnline
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = actualPartner.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val statusSubtitle = when {
                                actualPartner.isTyping -> "typing..."
                                actualPartner.isOnline -> "Online"
                                else -> actualPartner.role
                            }
                            Text(
                                text = statusSubtitle,
                                fontSize = 11.sp,
                                color = if (actualPartner.isTyping) Color(0xFF25D366) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = if (actualPartner.isTyping) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startCall(actualPartner, CallType.Audio) },
                        modifier = Modifier.testTag("audio_call_button")
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = "Voice Call", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { viewModel.startCall(actualPartner, CallType.Video) },
                        modifier = Modifier.testTag("video_call_button")
                    ) {
                        Icon(Icons.Filled.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFECE5DD)) // Subtle WhatsApp-style light wallpaper tint
        ) {
            // Messages List Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        // Secure Header Information Tag
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔒 Messages inside this chat are end-to-end encrypted with AES-256. No third parties can read them.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier
                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    items(messages) { msg ->
                        MessageBubble(
                            message = msg,
                            currentUserId = currentUser?.id ?: "me",
                            onClick = {
                                selectedMsgForOptions = msg
                                showMsgOptionsDialog = true
                            }
                        )
                    }

                    if (actualPartner.isTyping) {
                        item {
                            TypingIndicatorBubble(actualPartner)
                        }
                    }
                }
            }

            // Keyboard/Input section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                // Reply context preview bar
                AnimatedVisibility(
                    visible = replyToMessage != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE2E8F0))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Reply, contentDescription = "Reply", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to ${replyToMessage?.senderName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = replyToMessage?.text ?: "",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.setReplyTo(null) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }

                // Interactive Quick Emojis Selection Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 4.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val emojis = listOf("❤", "🥘", "😂", "👍", "🏡", "🙏", "🤣", "🧁")
                    emojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clickable { textInput += emoji }
                                .padding(4.dp)
                        )
                    }
                }

                // Main Message Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left accessories clip
                    IconButton(onClick = { showAttachmentSheet = !showAttachmentSheet }) {
                        Icon(
                            imageVector = if (showAttachmentSheet) Icons.Filled.Close else Icons.Filled.AddCircle,
                            contentDescription = "Attach Files",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Simulated Microphone block vs standard text editor text field
                    if (isRecordingVoicelist) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(Color(0xFFF3F4F6), RoundedCornerShape(22.dp))
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recording Audio: ${recordingSeconds}s",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Cancel",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clickable { isRecordingVoicelist = false }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Write family message...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color(0xFFF0F2F5),
                                unfocusedContainerColor = Color(0xFFF0F2F5)
                            ),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 100.dp)
                                .testTag("message_input_field")
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (textInput.isNotBlank()) {
                        // High Contrast Send Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    viewModel.sendMessage(textInput)
                                    textInput = ""
                                }
                                .testTag("send_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        // Responsive Microphone trigger recording button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isRecordingVoicelist) Color.Red else MaterialTheme.colorScheme.primary)
                                .clickable {
                                    if (isRecordingVoicelist) {
                                        // Finalize and post simulated audio prompt
                                        viewModel.recordVoiceNoteComplete(
                                            caption = "Dad, is the pizza ordered yet? We are waiting at the main hall.",
                                            duration = recordingSeconds
                                        )
                                        isRecordingVoicelist = false
                                    } else {
                                        isRecordingVoicelist = true
                                    }
                                }
                                .testTag("voice_record_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecordingVoicelist) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = "Voice Record",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Dynamic Action sheets structure expandable card
                AnimatedVisibility(
                    visible = showAttachmentSheet,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AttachmentItem(Icons.Filled.Image, "Gallery Preview", Color(0xFF9C27B0)) {
                                viewModel.shareImage(
                                    uri = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=500&auto=format&fit=crop",
                                    caption = "Look at this amazing sunset we captured today near the cottage! 🌅🏡"
                                )
                                showAttachmentSheet = false
                            }
                            AttachmentItem(Icons.Filled.Description, "Document", Color(0xFF2196F3)) {
                                viewModel.shareDocument("School_Grandpa_Homework_Guide.pdf")
                                showAttachmentSheet = false
                            }
                            AttachmentItem(Icons.Filled.Folder, "Family Video", Color(0xFFFF5722)) {
                                viewModel.sendMessage("📹 Shared video: graduation🎓sarah.mp4", "video", "https://sample.mp4")
                                showAttachmentSheet = false
                            }
                        }
                    }
                }
            }
        }

        // Long-Press/Bubble Tap Dialog Options Drawer
        if (showMsgOptionsDialog && selectedMsgForOptions != null) {
            val focusMsg = selectedMsgForOptions!!
            AlertDialog(
                onDismissRequest = { showMsgOptionsDialog = false },
                title = { Text("Message Action Options") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                viewModel.setReplyTo(focusMsg)
                                showMsgOptionsDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Reply, contentDescription = "Reply")
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Reply to this message")
                            }
                        }
                        TextButton(
                            onClick = {
                                viewModel.deleteMessageForEveryone(focusMsg.id)
                                showMsgOptionsDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Delete for everyone", color = Color.Red)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMsgOptionsDialog = false }) {
                        Text("Dismiss")
                    }
                }
            )
        }
    }
}

@Composable
fun AttachmentItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bg: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun MessageBubble(
    message: Message,
    currentUserId: String,
    onClick: () -> Unit
) {
    val isMe = message.senderId == currentUserId
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleBg = if (isMe) Color(0xFFD9FDD3) else Color.White // WhatsApp green vs white bubbles
    val cornerShape = if (isMe) {
        RoundedCornerShape(12.dp, 0.dp, 12.dp, 12.dp)
    } else {
        RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleBg),
            shape = cornerShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clickable { onClick() }
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header context author name if inside group chat
                if (!isMe && message.chatId == "family_group") {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // If reply to, display context box
                if (message.replyToText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text(
                                text = message.replyToSenderName ?: "Unknown",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = message.replyToText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.DarkGray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Main body payload switcher
                when (message.mediaType) {
                    "image" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = message.mediaUri,
                                contentDescription = "Shared Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        if (message.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = message.text, fontSize = 14.sp)
                        }
                    }
                    "voice" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play voice Note", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            // Simulated audio wave bars
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val bars = listOf(8, 16, 24, 12, 18, 6, 14, 22, 10, 16)
                                bars.forEach { height ->
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(height.dp)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("0:${message.voiceDurationSec}", fontSize = 12.sp, color = Color.Gray)
                        }
                        if (message.transcription != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = message.transcription, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = Color.DarkGray)
                        }
                    }
                    "document" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Filled.InsertDriveFile, contentDescription = "doc", tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.text.substringAfter("📄 "),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    else -> {
                        // Ordinary text messages
                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            fontWeight = if (message.senderId == "system_broadcast") FontWeight.Bold else FontWeight.Normal,
                            color = if (message.isDeleted) Color.Gray else Color.Unspecified
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Footer seen statuses and timestamp ticks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val mDate = java.util.Date(message.timestamp)
                    val sFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    Text(
                        text = sFormat.format(mDate),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(3.dp))
                        val tickIco = when (message.status) {
                            "seen" -> Icons.Filled.DoneAll
                            "delivered" -> Icons.Filled.DoneAll
                            else -> Icons.Filled.Done
                        }
                        val tickCol = if (message.status == "seen") Color(0xFF53BDEB) else Color.Gray

                        Icon(
                            imageVector = tickIco,
                            contentDescription = "seen",
                            tint = tickCol,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

// Typing Indicator visual Composable
@Composable
fun TypingIndicatorBubble(partner: User) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.width(120.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${partner.name.split(" ").firstOrNull()} writing",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                // Linear jumping dots effect
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.Gray))
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.Gray))
            }
        }
    }
}

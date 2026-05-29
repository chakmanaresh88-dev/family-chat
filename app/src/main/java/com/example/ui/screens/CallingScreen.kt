package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CallState
import com.example.ui.CallType
import com.example.ui.FamilyViewModel

@Composable
fun CallingScreen(
    viewModel: FamilyViewModel,
    modifier: Modifier = Modifier
) {
    val callState by viewModel.callState.collectAsState()

    // Control toggles
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isBackCamera by remember { mutableStateOf(false) }

    val activeState = callState
    if (activeState is CallState.Idle) return

    // Standard deep call screen carbon dark theme color background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111B21))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (activeState) {
            is CallState.Ringing -> {
                val peer = activeState.peer
                val isIncoming = activeState.isIncoming

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Profile Header Block
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 40.dp)
                    ) {
                        FamilyAvatar(name = peer.name, seed = peer.avatarColorSeed, size = 110.dp)
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = peer.name,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val label = if (isIncoming) "Incoming Private Call..." else "Private Chat Ringing..."
                        Text(
                            text = label,
                            color = Color(0xFF25D366),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Display Video Preview Box Mock
                    if (activeState.type == CallType.Video) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF202C33)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.PhotoCameraFront, contentDescription = "Face", tint = Color.LightGray, modifier = Modifier.size(52.dp))
                                Text(
                                    "✨ HD Front Camera Preview Active (Simulated)",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                                )
                            }
                        }
                    } else {
                        // Pulse audio graphic circles
                        Box(
                            modifier = Modifier.size(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFF128C7E).copy(alpha = 0.15f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF128C7E).copy(alpha = 0.25f))
                            )
                            Icon(Icons.Filled.VolumeUp, contentDescription = "Ringing Wave", tint = Color.White, modifier = Modifier.size(40.dp))
                        }
                    }

                    // Hang up or Answer button options
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 40.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isIncoming) {
                            // Decline Button
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                                    .clickable { viewModel.endCall() }
                                    .testTag("decline_call_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(28.dp))
                            }

                            // Answer Button
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                                    .clickable { viewModel.answerCall() }
                                    .testTag("answer_call_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = "Answer", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        } else {
                            // Outgoing ringing has simple cancel call
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                                    .clickable { viewModel.endCall() }
                                    .testTag("cancel_call_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CallEnd, contentDescription = "Cancel Outgoing", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }

            is CallState.Connected -> {
                val peer = activeState.peer
                val type = activeState.type
                val secCount = activeState.durationSec

                val formattedTime = remember(secCount) {
                    val m = secCount / 60
                    val s = secCount % 60
                    String.format("%02d:%02d", m, s)
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Status Info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 40.dp)
                    ) {
                        FamilyAvatar(name = peer.name, seed = peer.avatarColorSeed, size = 100.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = peer.name,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connected - $formattedTime",
                            color = Color(0xFF25D366),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (type == CallType.Video) "📹 Live HD Call (Encrypted)" else "🎵 High Quality Voice Audio (Encrypted)",
                            fontSize = 11.sp,
                            color = Color.LightGray.copy(alpha = 0.7f)
                        )
                    }

                    // Simulated Connection preview stream
                    if (type == CallType.Video) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            // Draw nice background or feed placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF202C33)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Videocam, contentDescription = "preview", tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            }
                            // Small overlay face preview representing my local feed
                            Card(
                                modifier = Modifier
                                    .size(width = 80.dp, height = 110.dp)
                                    .padding(8.dp)
                                    .align(Alignment.TopEnd),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Face, contentDescription = "Me", tint = Color.LightGray)
                                }
                            }
                        }
                    } else {
                        // Audio Calling Equalizer Blocks animation view
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(60.dp)
                        ) {
                            val activeBars = listOf(12, 36, 44, 18, 28, 48, 24, 40, 16, 32)
                            activeBars.forEach { ht ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(ht.dp)
                                        .background(Color(0xFF25D366))
                                )
                            }
                        }
                    }

                    // Calling Screen Controls Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF202C33)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute toggle
                            IconButton(onClick = { isMuted = !isMuted }) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                    contentDescription = "Mute",
                                    tint = if (isMuted) Color.Red else Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Speaker toggle
                            IconButton(onClick = { isSpeakerOn = !isSpeakerOn }) {
                                Icon(
                                    imageVector = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeDown,
                                    contentDescription = "Speaker",
                                    tint = if (isSpeakerOn) Color(0xFF25D366) else Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            if (type == CallType.Video) {
                                // Camera toggle
                                IconButton(onClick = { isBackCamera = !isBackCamera }) {
                                    Icon(
                                        imageVector = Icons.Filled.FlipCameraAndroid,
                                        contentDescription = "Flip Camera",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // Active Drop Call Button
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                                    .clickable { viewModel.endCall() }
                                    .testTag("drop_call_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CallEnd, contentDescription = "Drop Call", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
            else -> {
                // Return empty if state is idle
            }
        }
    }
}

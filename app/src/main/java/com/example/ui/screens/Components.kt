package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Elegant visual seeds colors representing different family members
val AvatarColors = listOf(
    Brush.linearGradient(listOf(Color(0xFF34BC98), Color(0xFF075E54))), // Emerald Teal (Admin)
    Brush.linearGradient(listOf(Color(0xFFFF7B94), Color(0xFFD81B60))), // Rose Sparkle (Mom)
    Brush.linearGradient(listOf(Color(0xFF4A90E2), Color(0xFF003366))), // Slate Ocean (Dad)
    Brush.linearGradient(listOf(Color(0xFFFFB300), Color(0xFFFF6F00))), // Sunset Orange (Sarah)
    Brush.linearGradient(listOf(Color(0xFF9B51E0), Color(0xFF5A2D82))), // Royal Purple (Grandpa)
    Brush.linearGradient(listOf(Color(0xFF718096), Color(0xFF2D3748))), // Stone Charcoal (Uncle Steve)
    Brush.linearGradient(listOf(Color(0xFF4FD1C5), Color(0xFF1D4ED8)))  // Vivid Cyan (Group)
)

@Composable
fun FamilyAvatar(
    name: String,
    seed: Int,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    isOnline: Boolean = false
) {
    val gradient = AvatarColors.getOrElse(seed % AvatarColors.size) { AvatarColors[0] }
    val initial = name.trim().firstOrNull()?.uppercase()?.toString() ?: "F"

    Box(
        modifier = modifier
            .size(size)
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Live green indicator dot
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(Color(0xFF25D366))
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

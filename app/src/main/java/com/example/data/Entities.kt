package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String, // e.g. "admin", "mom", "dad", "sister", "grandpa", "guest_steve"
    val name: String,
    val role: String, // "Admin", "Member"
    val phoneNo: String,
    val email: String,
    val avatarColorSeed: Int, // index of color to use since we draw avatars locally
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val isApproved: Boolean = true,
    val inviteCodeUsed: String = ""
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatId: String, // "family_group" or "userid-userid" (sorted alphabetically e.g. "admin-mom")
    val senderId: String,
    val senderName: String,
    val text: String,
    val mediaType: String = "text", // "text", "image", "video", "document", "voice"
    val mediaUri: String? = null,
    val voiceDurationSec: Int = 0,
    val transcription: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sent", // "sent", "delivered", "seen"
    val replyToId: Int? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val isDeleted: Boolean = false
)

@Entity(tableName = "announcements")
data class Announcement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val authorName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val date: String, // yyyy-MM-dd
    val time: String, // HH:mm
    val location: String,
    val creatorName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uri: String, // Local res or resource identifier
    val caption: String,
    val uploadedBy: String,
    val timestamp: Long = System.currentTimeMillis()
)

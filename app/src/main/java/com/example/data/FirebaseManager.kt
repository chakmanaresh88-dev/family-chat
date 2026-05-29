package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseManager private constructor(private val context: Context) {
    private val tag = "FirebaseManager"
    
    // Lazy initializers with safe try-catch wrapper in case Firebase is not configured
    val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "Firebase Auth not initialized: ${e.message}")
            null
        }
    }

    val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "Firebase Firestore not initialized: ${e.message}")
            null
        }
    }

    val storage: FirebaseStorage? by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "Firebase Storage not initialized: ${e.message}")
            null
        }
    }

    val messaging: FirebaseMessaging? by lazy {
        try {
            FirebaseMessaging.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "Firebase Messaging not initialized: ${e.message}")
            null
        }
    }

    // Checking if Firebase is available and configured
    val isFirebaseAvailable: Boolean
        get() = auth != null && firestore != null

    init {
        // Initial token fetch for FCM
        try {
            messaging?.token?.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(tag, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                Log.d(tag, "FCM registration token: $token")
            }
        } catch (e: Exception) {
            Log.e(tag, "FCM initialization skipped: ${e.message}")
        }
    }

    // Sync Firestore Users to Room
    fun startUsersSynchronization(userDao: UserDao, scope: CoroutineScope) {
        val db = firestore ?: return
        try {
            db.collection("users")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(tag, "Users listen failed.", error)
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshots.documentChanges) {
                                try {
                                    val id = doc.document.id
                                    val name = doc.document.getString("name") ?: ""
                                    val role = doc.document.getString("role") ?: "Member"
                                    val phoneNo = doc.document.getString("phoneNo") ?: ""
                                    val email = doc.document.getString("email") ?: ""
                                    val avatarColorSeed = doc.document.getLong("avatarColorSeed")?.toInt() ?: 1
                                    val isOnline = doc.document.getBoolean("isOnline") ?: false
                                    val isTyping = doc.document.getBoolean("isTyping") ?: false
                                    val isApproved = doc.document.getBoolean("isApproved") ?: true
                                    val inviteCodeUsed = doc.document.getString("inviteCodeUsed") ?: ""

                                    val user = User(
                                        id = id,
                                        name = name,
                                        role = role,
                                        phoneNo = phoneNo,
                                        email = email,
                                        avatarColorSeed = avatarColorSeed,
                                        isOnline = isOnline,
                                        isTyping = isTyping,
                                        isApproved = isApproved,
                                        inviteCodeUsed = inviteCodeUsed
                                    )

                                    if (doc.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                        userDao.deleteUser(id)
                                    } else {
                                        userDao.insertUser(user)
                                    }
                                } catch (ex: Exception) {
                                    Log.e(tag, "Error parsing synchronized user doc", ex)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "startUsersSynchronization failed: ${e.message}")
        }
    }

    // Sync Firestore Messages to Room (Online Realtime Messaging!)
    fun startMessagesSynchronization(messageDao: MessageDao, scope: CoroutineScope) {
        val db = firestore ?: return
        try {
            db.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(tag, "Messages listen failed.", error)
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshots.documentChanges) {
                                try {
                                    val idStr = doc.document.id
                                    // Parse firebase generated message
                                    val internalIdStr = doc.document.getString("internalId")
                                    val id = internalIdStr?.toIntOrNull() ?: doc.document.getLong("id")?.toInt() ?: Math.abs(idStr.hashCode())
                                    val chatId = doc.document.getString("chatId") ?: ""
                                    val senderId = doc.document.getString("senderId") ?: ""
                                    val senderName = doc.document.getString("senderName") ?: ""
                                    val text = doc.document.getString("text") ?: ""
                                    val mediaType = doc.document.getString("mediaType") ?: "text"
                                    val mediaUri = doc.document.getString("mediaUri")
                                    val voiceDurationSec = doc.document.getLong("voiceDurationSec")?.toInt() ?: 0
                                    val transcription = doc.document.getString("transcription")
                                    val timestamp = doc.document.getLong("timestamp") ?: System.currentTimeMillis()
                                    val status = doc.document.getString("status") ?: "sent"
                                    val replyToId = doc.document.getLong("replyToId")?.toInt()
                                    val replyToText = doc.document.getString("replyToText")
                                    val replyToSenderName = doc.document.getString("replyToSenderName")
                                    val isDeleted = doc.document.getBoolean("isDeleted") ?: false

                                    val message = Message(
                                        id = id,
                                        chatId = chatId,
                                        senderId = senderId,
                                        senderName = senderName,
                                        text = text,
                                        mediaType = mediaType,
                                        mediaUri = mediaUri,
                                        voiceDurationSec = voiceDurationSec,
                                        transcription = transcription,
                                        timestamp = timestamp,
                                        status = status,
                                        replyToId = replyToId,
                                        replyToText = replyToText,
                                        replyToSenderName = replyToSenderName,
                                        isDeleted = isDeleted
                                    )

                                    if (doc.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                        // Ignore or delete locally if requested
                                    } else {
                                        messageDao.insertMessage(message)
                                    }
                                } catch (ex: Exception) {
                                    Log.e(tag, "Error parsing synchronized message doc", ex)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "startMessagesSynchronization failed: ${e.message}")
        }
    }

    // Sync Announcements
    fun startAnnouncementsSynchronization(announcementDao: AnnouncementDao, scope: CoroutineScope) {
        val db = firestore ?: return
        try {
            db.collection("announcements")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(tag, "Announcements listen failed.", error)
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshots.documentChanges) {
                                try {
                                    val idStr = doc.document.id
                                    val id = doc.document.getLong("id")?.toInt() ?: Math.abs(idStr.hashCode())
                                    val title = doc.document.getString("title") ?: ""
                                    val content = doc.document.getString("content") ?: ""
                                    val authorName = doc.document.getString("authorName") ?: ""
                                    val timestamp = doc.document.getLong("timestamp") ?: System.currentTimeMillis()
                                    val isPinned = doc.document.getBoolean("isPinned") ?: false

                                    val announcement = Announcement(
                                        id = id,
                                        title = title,
                                        content = content,
                                        authorName = authorName,
                                        timestamp = timestamp,
                                        isPinned = isPinned
                                    )

                                    if (doc.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                        announcementDao.deleteAnnouncement(id)
                                    } else {
                                        announcementDao.insertAnnouncement(announcement)
                                    }
                                } catch (ex: Exception) {
                                    Log.e(tag, "Error parsing announcement doc", ex)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "startAnnouncementsSynchronization failed: ${e.message}")
        }
    }

    // Sync Events
    fun startEventsSynchronization(eventDao: EventDao, scope: CoroutineScope) {
        val db = firestore ?: return
        try {
            db.collection("events")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(tag, "Events listen failed.", error)
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshots.documentChanges) {
                                try {
                                    val idStr = doc.document.id
                                    val id = doc.document.getLong("id")?.toInt() ?: Math.abs(idStr.hashCode())
                                    val title = doc.document.getString("title") ?: ""
                                    val description = doc.document.getString("description") ?: ""
                                    val date = doc.document.getString("date") ?: ""
                                    val time = doc.document.getString("time") ?: ""
                                    val location = doc.document.getString("location") ?: ""
                                    val creatorName = doc.document.getString("creatorName") ?: ""
                                    val timestamp = doc.document.getLong("timestamp") ?: System.currentTimeMillis()

                                    val event = Event(
                                        id = id,
                                        title = title,
                                        description = description,
                                        date = date,
                                        time = time,
                                        location = location,
                                        creatorName = creatorName,
                                        timestamp = timestamp
                                    )

                                    if (doc.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                        eventDao.deleteEvent(id)
                                    } else {
                                        eventDao.insertEvent(event)
                                    }
                                } catch (ex: Exception) {
                                    Log.e(tag, "Error parsing event doc", ex)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "startEventsSynchronization failed: ${e.message}")
        }
    }

    // Sync Photos
    fun startPhotosSynchronization(photoDao: PhotoDao, scope: CoroutineScope) {
        val db = firestore ?: return
        try {
            db.collection("photos")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(tag, "Photos listen failed.", error)
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshots.documentChanges) {
                                try {
                                    val idStr = doc.document.id
                                    val id = doc.document.getLong("id")?.toInt() ?: Math.abs(idStr.hashCode())
                                    val uri = doc.document.getString("uri") ?: ""
                                    val caption = doc.document.getString("caption") ?: ""
                                    val uploadedBy = doc.document.getString("uploadedBy") ?: ""
                                    val timestamp = doc.document.getLong("timestamp") ?: System.currentTimeMillis()

                                    val photo = Photo(
                                        id = id,
                                        uri = uri,
                                        caption = caption,
                                        uploadedBy = uploadedBy,
                                        timestamp = timestamp
                                    )

                                    if (doc.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                        photoDao.deletePhoto(id)
                                    } else {
                                        photoDao.insertPhoto(photo)
                                    }
                                } catch (ex: Exception) {
                                    Log.e(tag, "Error parsing photo doc", ex)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "startPhotosSynchronization failed: ${e.message}")
        }
    }

    // Push local writes to Firestore Remote
    fun uploadUserToFirestore(user: User) {
        val db = firestore ?: return
        try {
            val userMap = hashMapOf(
                "name" to user.name,
                "role" to user.role,
                "phoneNo" to user.phoneNo,
                "email" to user.email,
                "avatarColorSeed" to user.avatarColorSeed,
                "isOnline" to user.isOnline,
                "isTyping" to user.isTyping,
                "isApproved" to user.isApproved,
                "inviteCodeUsed" to user.inviteCodeUsed
            )
            db.collection("users").document(user.id).set(userMap)
                .addOnSuccessListener { Log.d(tag, "User uploaded successfully: ${user.id}") }
                .addOnFailureListener { e -> Log.w(tag, "Error uploading user to Firestore", e) }
        } catch (e: Exception) {
            Log.e(tag, "uploadUserToFirestore failed: ${e.message}")
        }
    }

    fun uploadMessageToFirestore(message: Message) {
        val db = firestore ?: return
        try {
            val msgMap = hashMapOf(
                "id" to message.id,
                "internalId" to message.id.toString(),
                "chatId" to message.chatId,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "text" to message.text,
                "mediaType" to message.mediaType,
                "mediaUri" to message.mediaUri,
                "voiceDurationSec" to message.voiceDurationSec,
                "transcription" to message.transcription,
                "timestamp" to message.timestamp,
                "status" to message.status,
                "replyToId" to message.replyToId,
                "replyToText" to message.replyToText,
                "replyToSenderName" to message.replyToSenderName,
                "isDeleted" to message.isDeleted
            )
            db.collection("messages").document(message.id.toString()).set(msgMap)
                .addOnSuccessListener { Log.d(tag, "Message uploaded: ${message.id}") }
                .addOnFailureListener { e -> Log.w(tag, "Error uploadMsg", e) }
        } catch (e: Exception) {
            Log.e(tag, "uploadMessageToFirestore failed: ${e.message}")
        }
    }

    fun uploadAnnouncementToFirestore(announcement: Announcement) {
        val db = firestore ?: return
        try {
            val map = hashMapOf(
                "id" to announcement.id,
                "title" to announcement.title,
                "content" to announcement.content,
                "authorName" to announcement.authorName,
                "timestamp" to announcement.timestamp,
                "isPinned" to announcement.isPinned
            )
            db.collection("announcements").document(announcement.id.toString()).set(map)
        } catch (e: Exception) {
            Log.e(tag, "uploadAnnouncementToFirestore failed: ${e.message}")
        }
    }

    fun removeAnnouncementFromFirestore(id: Int) {
        val db = firestore ?: return
        try {
            db.collection("announcements").document(id.toString()).delete()
        } catch (e: Exception) {
            Log.e(tag, "removeAnnouncementFromFirestore failed: ${e.message}")
        }
    }

    fun uploadEventToFirestore(event: Event) {
        val db = firestore ?: return
        try {
            val map = hashMapOf(
                "id" to event.id,
                "title" to event.title,
                "description" to event.description,
                "date" to event.date,
                "time" to event.time,
                "location" to event.location,
                "creatorName" to event.creatorName,
                "timestamp" to event.timestamp
            )
            db.collection("events").document(event.id.toString()).set(map)
        } catch (e: Exception) {
            Log.e(tag, "uploadEventToFirestore failed: ${e.message}")
        }
    }

    fun removeEventFromFirestore(id: Int) {
        val db = firestore ?: return
        try {
            db.collection("events").document(id.toString()).delete()
        } catch (e: Exception) {
            Log.e(tag, "removeEventFromFirestore failed: ${e.message}")
        }
    }

    fun uploadPhotoToFirestore(photo: Photo) {
        val db = firestore ?: return
        try {
            val map = hashMapOf(
                "id" to photo.id,
                "uri" to photo.uri,
                "caption" to photo.caption,
                "uploadedBy" to photo.uploadedBy,
                "timestamp" to photo.timestamp
            )
            db.collection("photos").document(photo.id.toString()).set(map)
        } catch (e: Exception) {
            Log.e(tag, "uploadPhotoToFirestore failed: ${e.message}")
        }
    }

    fun removePhotoFromFirestore(id: Int) {
        val db = firestore ?: return
        try {
            db.collection("photos").document(id.toString()).delete()
        } catch (e: Exception) {
            Log.e(tag, "removePhotoFromFirestore failed: ${e.message}")
        }
    }

    suspend fun uploadStorageFile(localUri: String): String? {
        val st = storage ?: return "https://images.unsplash.com/photo-1543002588-bfa74002ed7e"
        return try {
            val ref = st.reference.child("uploads/${System.currentTimeMillis()}_file")
            // In case of simulated local assets or actual URIs, we simulate or execute actual upload
            if (localUri.startsWith("http") || localUri.contains("simulated")) {
                localUri
            } else {
                val androidUri = android.net.Uri.parse(localUri)
                val uploadTask = ref.putFile(androidUri).await()
                val downloadUrl = ref.downloadUrl.await()
                downloadUrl.toString()
            }
        } catch (e: Exception) {
            Log.e(tag, "File upload failed, using fallback mock: ${e.message}")
            "https://images.unsplash.com/photo-1543002588-bfa74002ed7e"
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseManager? = null

        fun getInstance(context: Context): FirebaseManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

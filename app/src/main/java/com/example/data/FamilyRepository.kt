package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class FamilyRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val messageDao = database.messageDao()
    private val announcementDao = database.announcementDao()
    private val eventDao = database.eventDao()
    private val photoDao = database.photoDao()

    val firebaseManager = FirebaseManager.getInstance(context)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            if (firebaseManager.isFirebaseAvailable) {
                Log.d("FamilyRepository", "Firebase is configured. Starting real-time sync listeners...")
                firebaseManager.startUsersSynchronization(userDao, this)
                firebaseManager.startMessagesSynchronization(messageDao, this)
                firebaseManager.startAnnouncementsSynchronization(announcementDao, this)
                firebaseManager.startEventsSynchronization(eventDao, this)
                firebaseManager.startPhotosSynchronization(photoDao, this)
            } else {
                Log.d("FamilyRepository", "Firebase not available. App running on offline-first local SQLite Room mode perfectly!")
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // Expose flows
    val allUsers: Flow<List<User>> = userDao.getAllUsersAsFlow()
    val pendingUsers: Flow<List<User>> = userDao.getPendingUsersAsFlow()
    val allAnnouncements: Flow<List<Announcement>> = announcementDao.getAnnouncementsAsFlow()
    val allEvents: Flow<List<Event>> = eventDao.getEventsAsFlow()
    val allPhotos: Flow<List<Photo>> = photoDao.getPhotosAsFlow()
    val allMessages: Flow<List<Message>> = messageDao.getAllMessagesAsFlow()

    fun getMessagesForChat(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesForChatAsFlow(chatId)
    }

    suspend fun getUser(id: String): User? = userDao.getUserById(id)
    
    fun getUserAsFlow(id: String): Flow<User?> = userDao.getUserByIdAsFlow(id)

    suspend fun insertMessage(message: Message): Int {
        val insertedId = messageDao.insertMessage(message).toInt()
        val msgToUpload = if (message.id == 0) message.copy(id = insertedId) else message

        if (firebaseManager.isFirebaseAvailable) {
            firebaseManager.uploadMessageToFirestore(msgToUpload)
        }
        
        // Trigger simulation if sender is the active user
        if (msgToUpload.senderId != "me") {
            return insertedId
        }

        // Trigger dynamic reaction
        val chatId = msgToUpload.chatId
        if (chatId == "family_group") {
            // Trigger group dialogue response from alternative character (Mom, Dad, Sis)
            simulateGroupChatReaction(msgToUpload)
        } else {
            // Trigger 1-to-1 reply from context user
            val counterpartId = chatId.split("-").firstOrNull { it != "me" }
            if (counterpartId != null) {
                simulatePrivateChatReaction(counterpartId, msgToUpload)
            }
        }

        return insertedId
    }

    suspend fun markAsSeen(chatId: String, activeUserId: String) {
        messageDao.markMessagesAsSeen(chatId, activeUserId)
        if (firebaseManager.isFirebaseAvailable) {
            try {
                // Normally we'd fetch all unseen messages of this chat and mark them as seen
                // Since this is a demo, we can update status of chat messages or let firebaseManager handle it
            } catch (e: Exception) {
                Log.e("FamilyRepository", "Error syncing seen message", e)
            }
        }
    }

    suspend fun deleteMessage(id: Int) {
        messageDao.deleteForEveryone(id)
        if (firebaseManager.isFirebaseAvailable) {
            val dbMessage = messageDao.getMessageById(id)
            if (dbMessage != null) {
                firebaseManager.uploadMessageToFirestore(dbMessage)
            }
        }
    }

    suspend fun approveUser(userId: String) {
        val user = userDao.getUserById(userId) ?: return
        val approvedUser = user.copy(isApproved = true)
        userDao.updateUser(approvedUser)
        
        if (firebaseManager.isFirebaseAvailable) {
            firebaseManager.uploadUserToFirestore(approvedUser)
        }

        // Feed welcoming message in group chat
        insertMessage(Message(
            chatId = "family_group",
            senderId = userId,
            senderName = user.name,
            text = "Hello everyone! Thank you for approving me, so glad to join the private FamilyConnect group chat!🏡❤",
            status = "seen"
        ))
    }

    suspend fun rejectUser(userId: String) {
        userDao.deleteUser(userId)
        if (firebaseManager.isFirebaseAvailable) {
            try {
                firebaseManager.firestore?.collection("users")?.document(userId)?.delete()
            } catch (e: Exception) {
                Log.e("FamilyRepository", "Error remote delete: ${e.message}")
            }
        }
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
        if (firebaseManager.isFirebaseAvailable) {
            firebaseManager.uploadUserToFirestore(user)
        }
    }

    suspend fun removeUser(userId: String) {
        userDao.deleteUser(userId)
        if (firebaseManager.isFirebaseAvailable) {
            try {
                firebaseManager.firestore?.collection("users")?.document(userId)?.delete()
            } catch (e: Exception) {
                Log.e("FamilyRepository", "Error remote delete user: ${e.message}")
            }
        }
    }

    suspend fun insertAnnouncement(title: String, content: String, author: String, isPinned: Boolean = false) {
        val annId = (1000..999999).random()
        val announcement = Announcement(
            id = annId,
            title = title,
            content = content,
            authorName = author,
            isPinned = isPinned
        )
        announcementDao.insertAnnouncement(announcement)
        if (firebaseManager.isFirebaseAvailable) {
            firebaseManager.uploadAnnouncementToFirestore(announcement)
        }
    }

    suspend fun insertEvent(title: String, description: String, date: String, time: String, location: String, creator: String) {
        val evId = (1000..999999).random()
        val event = Event(
            id = evId,
            title = title,
            description = description,
            date = date,
            time = time,
            location = location,
            creatorName = creator
        )
        eventDao.insertEvent(event)
        if (firebaseManager.isFirebaseAvailable) {
            firebaseManager.uploadEventToFirestore(event)
        }
    }

    suspend fun deleteAnnouncement(id: Int) {
        announcementDao.deleteAnnouncement(id)
        if (firebaseManager.isFirebaseAvailable) {
            firebaseManager.removeAnnouncementFromFirestore(id)
        }
    }

    suspend fun deleteEvent(id: Int) {
        eventDao.deleteEvent(id)
        if (firebaseManager.isFirebaseAvailable) {
            firebaseManager.removeEventFromFirestore(id)
        }
    }

    suspend fun uploadPhoto(uri: String, caption: String, author: String) {
        val finalUri = if (firebaseManager.isFirebaseAvailable) {
            firebaseManager.uploadStorageFile(uri) ?: uri
        } else {
            uri
        }
        val photoId = (1000..999999).random()
        val photo = Photo(
            id = photoId,
            uri = finalUri,
            caption = caption,
            uploadedBy = author
        )
        photoDao.insertPhoto(photo)
        if (firebaseManager.isFirebaseAvailable) {
            firebaseManager.uploadPhotoToFirestore(photo)
        }
    }

    suspend fun deletePhoto(id: Int) {
        photoDao.deletePhoto(id)
        if (firebaseManager.isFirebaseAvailable) {
            firebaseManager.removePhotoFromFirestore(id)
        }
    }

    // --- Dynamic Chat Simulator ---
    private fun simulateGroupChatReaction(userMessage: Message) {
        CoroutineScope(Dispatchers.IO).launch {
            // Select random active user to reply: mom, dad, or sister
            val responders = listOf("mom", "dad", "sister")
            val chosenId = responders.random()
            val responder = userDao.getUserById(chosenId) ?: return@launch

            delay(1500)
            userDao.updateOnlineStatus(chosenId, true)
            delay(1000)
            userDao.updateTypingStatus(chosenId, true)
            
            val promptContext = "We are in a private family group chat. " +
                    "My name is ${responder.name} with the role in the family as ${responder.role}. " +
                    "The family member '${userMessage.senderName}' just said in the group chat: \"${userMessage.text}\". " +
                    "I want to respond to them warmly, concisely (1-2 sentences max), keeping it real, loving, and incorporating typical family chat emojis (like heart, house, smileys, etc.)."
            
            val responseText = generateAISpokenText(promptContext, getLocalFamilyFallback(chosenId, userMessage.text))
            
            userDao.updateTypingStatus(chosenId, false)
            insertMessage(Message(
                chatId = "family_group",
                senderId = chosenId,
                senderName = responder.name,
                text = responseText,
                status = "delivered"
            ))
            delay(1500)
            // Mark all seen
            messageDao.markMessagesAsSeen("family_group", "me", "seen")
        }
    }

    private fun simulatePrivateChatReaction(partnerId: String, userMessage: Message) {
        CoroutineScope(Dispatchers.IO).launch {
            val responder = userDao.getUserById(partnerId) ?: return@launch

            delay(1500)
            userDao.updateOnlineStatus(partnerId, true)
            delay(1000)
            userDao.updateTypingStatus(partnerId, true)

            val promptContext = "This is a private 1-to-1 conversation on FamilyConnect between me, ${responder.name} (with role ${responder.role} in the family), and my child/parent/sibling ${userMessage.senderName}. " +
                    "They just said: \"${userMessage.text}\". " +
                    "I need to reply directly to their statement, keeping my tone natural, authentic, very warm and relatable. Limit to 1 or 2 small sentences, using cozy emojis."
            
            val responseText = generateAISpokenText(promptContext, getLocalFamilyFallback(partnerId, userMessage.text))

            userDao.updateTypingStatus(partnerId, false)
            insertMessage(Message(
                chatId = userMessage.chatId,
                senderId = partnerId,
                senderName = responder.name,
                text = responseText,
                status = "delivered"
            ))
            delay(1500)
            // Mark private seen
            messageDao.markMessagesAsSeen(userMessage.chatId, "me", "seen")
        }
    }

    // --- Gemini Web Service Call ---
    suspend fun generateAISpokenText(prompt: String, fallback: String): String = withContext(Dispatchers.IO) {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            Log.d("FamilyConnect", "Gemini API key is local placeholder. Using fallback.")
            return@withContext fallback
        }

        try {
            // REST call structure
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key"
            
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("FamilyConnect", "Gemini request failed: ${response.code}")
                    return@withContext fallback
                }
                
                val bodyStr = response.body?.string() ?: return@withContext fallback
                val jsonResponse = JSONObject(bodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")
                
                if (!text.isNullOrBlank()) {
                    text.trim()
                } else {
                    fallback
                }
            }
        } catch (e: Exception) {
            Log.e("FamilyConnect", "Gemini API call error", e)
            fallback
        }
    }

    // --- Voice transcription simulator ---
    suspend fun transcribeVoiceMessage(caption: String): String = withContext(Dispatchers.IO) {
        val defaultText = "Hey! Let's arrange a time for video calling everyone later tonight."
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            delay(1200) // Simulated processing latency
            return@withContext "📝 [Audio Transcribed]: \"$caption\""
        }

        val prompt = "Translate or transcribe this voice audio segment description to a beautiful, realistic voice note message: \"$caption\". Give me ONLY the transcribed text in double-quotes."
        val rawAi = generateAISpokenText(prompt, caption)
        "📝 [Audio Transcribed]: $rawAi"
    }

    private fun getLocalFamilyFallback(userId: String, incoming: String): String {
        val lower = incoming.lowercase()
        return when (userId) {
            "mom" -> {
                when {
                    "hello" in lower || "hi" in lower -> "Hi sweetheart! How is your day going? ❤ Don't forget to drink water!"
                    "eat" in lower || "dinner" in lower || "food" in lower -> "I am cooking family dinner tonight! I'm planning to make lasagna. Let me know if you are coming home early 🥘🏡"
                    "love" in lower -> "Aww, love you more! You are the absolute best child a mother could ask for! 🥰🌟"
                    "help" in lower || "need" in lower -> "Of course, dear! Let me know what you need or give me a call. I am always here."
                    else -> "That is wonderful, dear! Thanks for sharing this in our private family space. We must talk more tonight! 💕"
                }
            }
            "dad" -> {
                when {
                    "hello" in lower || "hi" in lower -> "Hey kiddo! Hope things are good. Did you check the oil in the car recently? 🚗"
                    "dinner" in lower || "food" in lower -> "Sounds delicious. Count me in. I'll pick up some dessert on my way back! 🍰"
                    "sport" in lower || "game" in lower || "football" in lower -> "Nice! Are we watching the match together this weekend? Go team! ⚽💪"
                    else -> "Got it! Keep up the good work. Talk to you soon, big hug! 👍"
                }
            }
            "sister" -> {
                when {
                    "hello" in lower || "hi" in lower -> "Heyy! Omg what's up?? Guess what happened today..."
                    "dinner" in lower || "food" in lower -> "Yesss I am starving! Please tell me we are getting pizza!! 🍕✨"
                    "money" in lower || "borrow" in lower -> "Haha, always asking for stuff! Only if you buy me ice cream later! 😜🍦"
                    else -> "Haha no way! That is so cool! Let's talk more in our group call tonight! 😂💖"
                }
            }
            "grandpa" -> "Hello grandchild. My eyesight is a bit blurry today, forgive my late answers. Love you very much. God bless. 👴👵"
            else -> "Hello there! Glad to be connected with you."
        }
    }

    // --- Seeding Data Method ---
    suspend fun seedDatabaseIfNeeded() {
        val existingUsers = userDao.getAllUsers()
        if (existingUsers.isNotEmpty()) return

        Log.d("FamilyConnect", "Database is empty. Seeding gorgeous preloaded chat, users, announcements, and photos...")

        // Seed Users
        val admin = User(
            id = "me",
            name = "Me (Admin)",
            role = "Admin",
            phoneNo = "Admin - +1 555 1010",
            email = "admin@family.com",
            avatarColorSeed = 1,
            isOnline = true,
            isApproved = true
        )
        val mom = User(
            id = "mom",
            name = "Mom 👩❤",
            role = "Mother",
            phoneNo = "+1 555 2020",
            email = "mom@family.com",
            avatarColorSeed = 2,
            isOnline = true,
            isApproved = true
        )
        val dad = User(
            id = "dad",
            name = "Dad 👨🛠",
            role = "Father",
            phoneNo = "+1 555 3030",
            email = "dad@family.com",
            avatarColorSeed = 3,
            isOnline = false,
            isApproved = true
        )
        val sister = User(
            id = "sister",
            name = "Sarah (Sister) 👧✨",
            role = "Sister",
            phoneNo = "+1 555 4040",
            email = "sarah@family.com",
            avatarColorSeed = 4,
            isOnline = true,
            isApproved = true
        )
        val grandpa = User(
            id = "grandpa",
            name = "Grandpa 👴",
            role = "Grandfather",
            phoneNo = "+1 555 5050",
            email = "grandpa@family.com",
            avatarColorSeed = 5,
            isOnline = false,
            isApproved = true
        )

        // Seed dynamic pending user for Admin Dashboard Demo!
        val uncleSteve = User(
            id = "uncle_steve",
            name = "Uncle Steve 🧔🍺",
            role = "Uncle",
            phoneNo = "+1 555 8899",
            email = "steve@family.com",
            avatarColorSeed = 6,
            isOnline = false,
            isApproved = false,
            inviteCodeUsed = "FAM99"
        )

        userDao.insertUser(admin)
        userDao.insertUser(mom)
        userDao.insertUser(dad)
        userDao.insertUser(sister)
        userDao.insertUser(grandpa)
        userDao.insertUser(uncleSteve)

        // Seed Announcements
        announcementDao.insertAnnouncement(Announcement(
            title = "🏡 Family Sunday Barbecue Picnic!",
            content = "This Sunday at 1:00 PM in Central Park! Dad is bringing his famous BBQ grill, Mom is prepping the salads, and Sarah is bringing cookies. Clean up is on the kids! Please make sure to RSVP in the Events calendar.",
            authorName = "Me (Admin)",
            isPinned = true
        ))
        announcementDao.insertAnnouncement(Announcement(
            title = "🔑 Safety Notice: Front Door Key Code Updated",
            content = "We updated the garage keypad entrance secondary code for safety. The new code is now 4821. Keep it safe and secure!",
            authorName = "Mom 👩❤",
            isPinned = false
        ))

        // Seed Events
        eventDao.insertEvent(Event(
            title = "Sunday BBQ Picnic 🥩🍔",
            description = "Main family bonding outing this month in Central Park Meadow.",
            date = "2026-06-07",
            time = "13:00",
            location = "Central Park Sector 4-B",
            creatorName = "Dad 👨🛠"
        ))
        eventDao.insertEvent(Event(
            title = "Sarah's Graduation Ceremony 🎓",
            description = "Let's gather at the University main auditorium to celebrate Sarah getting her degree!",
            date = "2026-06-15",
            time = "10:30",
            location = "State University Hall C",
            creatorName = "Mom 👩❤"
        ))

        // Seed Photos for family gallery
        photoDao.insertPhoto(Photo(
            uri = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=500&auto=format&fit=crop",
            caption = "Found Grandma's old secret recipe diary! Full of vintage baking gems 🥧👵🥖",
            uploadedBy = "Sarah (Sister) 👧✨"
        ))
        photoDao.insertPhoto(Photo(
            uri = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500&auto=format&fit=crop",
            caption = "Last summer's family road-trip vacation dinner! Best mood ever 💫🌅🏡",
            uploadedBy = "Me (Admin)"
        ))

        // Seed preloaded messages in Family Group Chat
        val groupChat = "family_group"
        messageDao.insertMessage(Message(
            chatId = groupChat,
            senderId = "mom",
            senderName = "Mom 👩❤",
            text = "Welcome to our private end-to-end encrypted family chat room! It is much safer than other apps. ✨🏡",
            status = "seen"
        ))
        messageDao.insertMessage(Message(
            chatId = groupChat,
            senderId = "dad",
            senderName = "Dad 👨🛠",
            text = "Testing. Does this thing work? I completed building the BBQ list today.",
            status = "seen"
        ))
        messageDao.insertMessage(Message(
            chatId = groupChat,
            senderId = "sister",
            senderName = "Sarah (Sister) 👧✨",
            text = "Yesss dad! I want double cheeseburgers! 🍔😋",
            status = "seen",
            replyToId = 2,
            replyToSenderName = "Dad 👨🛠",
            replyToText = "Testing. Does this thing work? I completed building the BBQ list today."
        ))

        // Seed Mom private chat
        val momChat = "me-mom"
        messageDao.insertMessage(Message(
            chatId = momChat,
            senderId = "mom",
            senderName = "Mom 👩❤",
            text = "Hi honey! Did you complete the code task yet? Let me know, Grandpa wants to see it.",
            status = "seen"
        ))
        messageDao.insertMessage(Message(
            chatId = momChat,
            senderId = "me",
            senderName = "Me (Admin)",
            text = "Working on it right now Mom! It is completely polished with custom green colors and has everything we need.",
            status = "seen"
        ))
        messageDao.insertMessage(Message(
            chatId = momChat,
            senderId = "mom",
            senderName = "Mom 👩❤",
            text = "Fantastic! You are so talented! Tell me when it is ready, lunch is almost done. 🥰🥘",
            status = "seen"
        ))
    }
}

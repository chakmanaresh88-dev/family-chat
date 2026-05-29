package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface CallType {
    object Audio : CallType
    object Video : CallType
}

sealed interface CallState {
    object Idle : CallState
    data class Ringing(val peer: User, val type: CallType, val isIncoming: Boolean) : CallState
    data class Connected(val peer: User, val type: CallType, val durationSec: Int) : CallState
}

class FamilyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FamilyRepository(application)

    // UI State Holders
    val allUsers: StateFlow<List<User>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allMessages: StateFlow<List<Message>> = repository.allMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pendingUsers: StateFlow<List<User>> = repository.pendingUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allAnnouncements: StateFlow<List<Announcement>> = repository.allAnnouncements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allEvents: StateFlow<List<Event>> = repository.allEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPhotos: StateFlow<List<Photo>> = repository.allPhotos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active Login User Session
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Login screen inputs
    var phoneNumberInput = MutableStateFlow("")
    var otpInput = MutableStateFlow("")
    var emailInput = MutableStateFlow("")
    var inviteCodeInput = MutableStateFlow("")
    var registrationNameInput = MutableStateFlow("")
    var registrationRoleInput = MutableStateFlow("Member")

    private val _loginState = MutableStateFlow<LoginState>(LoginState.LoggedOut)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // Navigation and Chat Session
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _selectedChatPartner = MutableStateFlow<User?>(null)
    val selectedChatPartner: StateFlow<User?> = _selectedChatPartner.asStateFlow()

    val chatMessages: StateFlow<List<Message>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId == null) flowOf(emptyList())
            else repository.getMessagesForChat(chatId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reply context
    private val _replyToMessage = MutableStateFlow<Message?>(null)
    val replyToMessage: StateFlow<Message?> = _replyToMessage.asStateFlow()

    // Call state controls
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private var callTimerJob: Job? = null

    init {
        // Seed and load database
        viewModelScope.launch {
            repository.seedDatabaseIfNeeded()
            // Auto-login as the default Admin account for seamless play
            val defaultAdmin = repository.getUser("me")
            if (defaultAdmin != null) {
                _currentUser.value = defaultAdmin
                _loginState.value = LoginState.Success(defaultAdmin)
            }
        }
    }

    // --- Authentication Actions ---
    fun loginWithPhone() {
        viewModelScope.launch {
            if (phoneNumberInput.value.isBlank()) {
                _loginState.value = LoginState.Error("Please enter a valid phone number.")
                return@launch
            }
            if (otpInput.value != "123456" && otpInput.value.isNotBlank()) {
                // Let code be simple demo "123456"
                _loginState.value = LoginState.Error("Invalid OTP code. Enter '123456' to pass.")
                return@launch
            }

            _loginState.value = LoginState.OtpSent
        }
    }

    fun verifyOtp() {
        viewModelScope.launch {
            if (otpInput.value != "123456") {
                _loginState.value = LoginState.Error("Invalid OTP. Please enter '123456' for testing.")
                return@launch
            }

            // Normal verification login
            val defaultAdmin = repository.getUser("me")
            if (defaultAdmin != null) {
                _currentUser.value = defaultAdmin
                _loginState.value = LoginState.Success(defaultAdmin)
            }
        }
    }

    fun submitRegistration() {
        viewModelScope.launch {
            val name = registrationNameInput.value
            val role = registrationRoleInput.value
            val invite = inviteCodeInput.value
            val cell = phoneNumberInput.value

            if (name.isBlank() || role.isBlank()) {
                _loginState.value = LoginState.Error("Name and Family role cannot be empty.")
                return@launch
            }
            if (invite.uppercase() != "FAM99") {
                _loginState.value = LoginState.Error("Invalid family invite code. Use code 'FAM99'.")
                return@launch
            }

            // Create pending user request
            val tempId = "user_" + System.currentTimeMillis()
            val newUser = User(
                id = tempId,
                name = "$name ($role)",
                role = role,
                phoneNo = cell.ifBlank { "+1 555-5555" },
                email = emailInput.value.ifBlank { "member@family.com" },
                avatarColorSeed = (3..8).random(),
                isApproved = false,
                inviteCodeUsed = invite
            )

            repository.insertUser(newUser)
            _loginState.value = LoginState.PendingApproval(newUser)
        }
    }

    fun switchActiveUser(userId: String) {
        viewModelScope.launch {
            val user = repository.getUser(userId)
            if (user != null && user.isApproved) {
                _currentUser.value = user
                _loginState.value = LoginState.Success(user)
                // Clear chat details
                _activeChatId.value = null
                _selectedChatPartner.value = null
            }
        }
    }

    fun selectChat(partner: User?) {
        viewModelScope.launch {
            _selectedChatPartner.value = partner
            if (partner == null) {
                _activeChatId.value = null
            } else {
                val activeId = _currentUser.value?.id ?: "me"
                // Generate chatId: alphabetical sorted combination if private, or "family_group"
                val destChatId = if (partner.id == "family_group") {
                    "family_group"
                } else {
                    listOf(activeId, partner.id).sorted().joinToString("-")
                }
                _activeChatId.value = destChatId
                // Clean seen notifications
                repository.markAsSeen(destChatId, activeId)
            }
        }
    }

    // --- Message Sending Operations ---
    fun sendMessage(text: String, mediaType: String = "text", mediaUri: String? = null, voiceDuration: Int = 0, trans: String? = null) {
        val user = _currentUser.value ?: return
        val chat = _activeChatId.value ?: return

        viewModelScope.launch {
            val msg = Message(
                chatId = chat,
                senderId = user.id,
                senderName = user.name,
                text = text,
                mediaType = mediaType,
                mediaUri = mediaUri,
                voiceDurationSec = voiceDuration,
                transcription = trans,
                replyToId = _replyToMessage.value?.id,
                replyToText = _replyToMessage.value?.text,
                replyToSenderName = _replyToMessage.value?.senderName,
                status = "sent"
            )
            
            // Clear reply drawer
            _replyToMessage.value = null

            repository.insertMessage(msg)
        }
    }

    fun setReplyTo(message: Message?) {
        _replyToMessage.value = message
    }

    fun deleteMessageForEveryone(id: Int) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    // --- Calling Flow Simulator ---
    fun startCall(peer: User, type: CallType) {
        _callState.value = CallState.Ringing(peer, type, isIncoming = false)
        // Simulate Ringing for 3 seconds then connect
        viewModelScope.launch {
            delay(3000)
            if (_callState.value is CallState.Ringing) {
                connectCall(peer, type)
            }
        }
    }

    fun incomingCallMock(peer: User, type: CallType) {
        _callState.value = CallState.Ringing(peer, type, isIncoming = true)
    }

    private fun connectCall(peer: User, type: CallType) {
        _callState.value = CallState.Connected(peer, type, 0)
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            var seconds = 0
            while (true) {
                delay(1000)
                seconds++
                val curr = _callState.value
                if (curr is CallState.Connected) {
                    _callState.value = curr.copy(durationSec = seconds)
                } else {
                    break
                }
            }
        }
    }

    fun answerCall() {
        val state = _callState.value
        if (state is CallState.Ringing && state.isIncoming) {
            connectCall(state.peer, state.type)
        }
    }

    fun endCall() {
        callTimerJob?.cancel()
        _callState.value = CallState.Idle
    }

    // --- Media Simulator Files ---
    fun recordVoiceNoteComplete(caption: String, duration: Int) {
        viewModelScope.launch {
            val transcriptionText = repository.transcribeVoiceMessage(caption)
            sendMessage(
                text = "🎤 Voice Message ($duration s)",
                mediaType = "voice",
                mediaUri = "simulated_audio_ref_" + System.currentTimeMillis(),
                voiceDuration = duration,
                trans = transcriptionText
            )
        }
    }

    fun shareImage(uri: String, caption: String) {
        sendMessage(
            text = if (caption.isNotBlank()) caption else "📸 Shared an image",
            mediaType = "image",
            mediaUri = uri
        )
    }

    fun shareDocument(docName: String) {
        sendMessage(
            text = "📄 Document: $docName",
            mediaType = "document",
            mediaUri = "doc_simulated_path"
        )
    }

    // --- Bulletin/Events Announcements ---
    fun addAnnouncement(title: String, content: String, isPinned: Boolean) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.insertAnnouncement(title, content, user.name, isPinned)
        }
    }

    fun removeAnnouncement(id: Int) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
        }
    }

    fun addEvent(title: String, description: String, date: String, time: String, location: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.insertEvent(title, description, date, time, location, user.name)
        }
    }

    fun removeEvent(id: Int) {
        viewModelScope.launch {
            repository.deleteEvent(id)
        }
    }

    fun addPhotoToGallery(caption: String, uri: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.uploadPhoto(uri, caption, user.name)
        }
    }

    fun removePhoto(id: Int) {
        viewModelScope.launch {
            repository.deletePhoto(id)
        }
    }

    // --- Admin Operations ---
    fun approvePendingMember(userId: String) {
        viewModelScope.launch {
            repository.approveUser(userId)
        }
    }

    fun rejectPendingMember(userId: String) {
        viewModelScope.launch {
            repository.rejectUser(userId)
        }
    }

    fun blockMember(userId: String) {
        viewModelScope.launch {
            repository.removeUser(userId)
        }
    }

    fun broadcastSystemMessage(text: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            // Broadcast as Admin to group chat
            repository.insertMessage(Message(
                chatId = "family_group",
                senderId = "system_broadcast",
                senderName = "🚨 Announcement Broadcast",
                text = text,
                status = "delivered"
            ))
        }
    }

    fun triggerLogOut() {
        _loginState.value = LoginState.LoggedOut
        _currentUser.value = null
    }
}

sealed interface LoginState {
    object LoggedOut : LoginState
    object OtpSent : LoginState
    data class PendingApproval(val tempUser: User) : LoginState
    data class Success(val activeUser: User) : LoginState
    data class Error(val errorMsg: String) : LoginState
}

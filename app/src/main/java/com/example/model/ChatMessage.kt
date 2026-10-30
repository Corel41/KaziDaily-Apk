package com.example.model

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestampFormatted: String,
    val isFromCurrentUser: Boolean
)

data class ChatThread(
    val threadId: String,
    val jobId: String,
    val jobTitle: String,
    val otherPartyId: String,
    val otherPartyName: String,
    val otherPartyRole: String, // "Employer" or "Applicant/Worker"
    val lastMessage: String,
    val lastTimestamp: String,
    val unreadCount: Int = 0,
    val messages: List<ChatMessage> = emptyList()
)

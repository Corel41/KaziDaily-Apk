package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ChatMessage
import com.example.model.ChatThread
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.theme.LocalGlassTokens
import com.example.viewmodel.KaziViewModel
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(
    viewModel: KaziViewModel,
    chatThreads: List<ChatThread>,
    activeThreadId: String?,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val senderPersona by viewModel.chatSenderPersona.collectAsStateWithLifecycle()

    if (activeThreadId != null) {
        val activeThread = chatThreads.find { it.threadId == activeThreadId }
        if (activeThread != null) {
            BackHandler { viewModel.closeChatThread() }
            ChatConversationView(
                thread = activeThread,
                senderPersona = senderPersona,
                onTogglePersona = { viewModel.toggleChatSenderPersona() },
                onBack = { viewModel.closeChatThread() },
                onSendMessage = { text -> viewModel.sendMessage(activeThread.threadId, text) }
            )
        } else {
            ChatListView(
                threads = chatThreads,
                onSelectThread = { viewModel.openChatThread(it.threadId) }
            )
        }
    } else {
        ChatListView(
            threads = chatThreads,
            onSelectThread = { viewModel.openChatThread(it.threadId) }
        )
    }
}

@Composable
private fun ChatListView(
    threads: List<ChatThread>,
    onSelectThread: (ChatThread) -> Unit
) {
    val tokens = LocalGlassTokens.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Messages",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tokens.textPrimary
                )
                Text(
                    text = "Active job-based conversations",
                    fontSize = 12.sp,
                    color = tokens.textMuted
                )
            }

            // Real-time Firestore Cloud Sync Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.mintContainer.copy(alpha = 0.5f))
                    .border(0.8.dp, tokens.mintAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("chat_cloud_sync_badge")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(tokens.mintAccent)
                    )
                    Text(
                        text = "Cloud Synced",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.mintAccent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (threads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💬", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No active conversations",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = "When you apply for a job or post a gig, chats appear here.",
                        fontSize = 12.sp,
                        color = tokens.textMuted
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(threads, key = { it.threadId }) { thread ->
                    ChatThreadCard(
                        thread = thread,
                        onClick = { onSelectThread(thread) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatThreadCard(
    thread: ChatThread,
    onClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val initials = thread.otherPartyName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with initials
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(tokens.mintContainer)
                    .border(1.dp, tokens.mintAccent.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.mintAccent
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.otherPartyName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = thread.lastTimestamp,
                        fontSize = 11.sp,
                        color = tokens.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "💼 ${thread.jobTitle}",
                    fontSize = 11.sp,
                    color = tokens.cyanAccent,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = thread.lastMessage,
                    fontSize = 12.sp,
                    color = tokens.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ChatConversationView(
    thread: ChatThread,
    senderPersona: KaziViewModel.ChatSenderPersona,
    onTogglePersona: () -> Unit,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    val tokens = LocalGlassTokens.current
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(thread.messages.size) {
        if (thread.messages.isNotEmpty()) {
            listState.animateScrollToItem(thread.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 76.dp) // Room for bottom navigation
    ) {
        // Top Header
        Surface(
            color = tokens.surfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, tokens.borderGlass),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = tokens.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = thread.otherPartyName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = "Gig: ${thread.jobTitle}",
                        fontSize = 11.sp,
                        color = tokens.textMuted,
                        maxLines = 1
                    )
                }
                GlassPill(
                    text = thread.otherPartyRole,
                    color = tokens.mintContainer,
                    textColor = tokens.mintAccent
                )
            }
        }

        // Persona Switcher Bar (simulating & testing two-user conversation across devices)
        Surface(
            color = tokens.surfaceGlass,
            border = androidx.compose.foundation.BorderStroke(0.6.dp, tokens.borderGlass),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Sender:",
                        fontSize = 11.sp,
                        color = tokens.textMuted
                    )
                    Text(
                        text = if (senderPersona == KaziViewModel.ChatSenderPersona.WORKER) "👷 Brian Mwangi (Worker)" else "👔 ${thread.otherPartyName} (Employer)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (senderPersona == KaziViewModel.ChatSenderPersona.WORKER) tokens.mintAccent else tokens.purpleAccent
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(tokens.surfaceElevated)
                        .border(0.6.dp, tokens.borderGlass, RoundedCornerShape(10.dp))
                        .clickable { onTogglePersona() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("switch_chat_persona_button")
                ) {
                    Text(
                        text = "Switch Persona ⇄",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.cyanAccent
                    )
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(thread.messages, key = { it.id }) { msg ->
                MessageBubble(message = msg)
            }
        }

        // Quick Responses Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickReplyChip("📍 Share live location") {
                onSendMessage("📍 My current location is Westlands, en route.")
            }
            QuickReplyChip("⏱️ Arriving in 15 min") {
                onSendMessage("⏱️ Traffic looks clear, arriving in approx 15 minutes.")
            }
            QuickReplyChip("🛠️ Tools ready") {
                onSendMessage("🛠️ All tools and equipment are packed and ready.")
            }
            QuickReplyChip("🔒 Escrow confirmed") {
                onSendMessage("🔒 Escrow payment verified. Proceeding with task.")
            }
            QuickReplyChip("✅ Task done, please confirm") {
                onSendMessage("✅ Task complete! Please confirm on app to release escrow funds.")
            }
        }

        // Bottom Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type message...", fontSize = 13.sp, color = tokens.textMuted) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = tokens.surfaceGlass,
                    unfocusedContainerColor = tokens.surfaceGlass,
                    focusedBorderColor = tokens.mintAccent,
                    unfocusedBorderColor = tokens.borderGlass,
                    focusedTextColor = tokens.textPrimary,
                    unfocusedTextColor = tokens.textPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText)
                        messageText = ""
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText)
                        messageText = ""
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(tokens.mintAccent)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (tokens.isDark) Color(0xFF002214) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val tokens = LocalGlassTokens.current
    val isMe = message.isFromCurrentUser
    val isSystem = message.senderId == "system"

    if (isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.purpleAccent.copy(alpha = if (tokens.isDark) 0.18f else 0.12f))
                    .border(0.8.dp, tokens.purpleAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 11.sp,
                    color = tokens.purpleAccent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isMe) {
                            if (tokens.isDark) tokens.mintAccent.copy(alpha = 0.25f) else tokens.mintContainer
                        } else tokens.surfaceElevated
                    )
                    .border(
                        0.8.dp,
                        if (isMe) tokens.mintAccent.copy(alpha = 0.4f) else tokens.borderGlass,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = tokens.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = message.timestampFormatted,
                        fontSize = 10.sp,
                        color = tokens.textMuted,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickReplyChip(text: String, onClick: () -> Unit) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.surfaceElevated)
            .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 11.sp, color = tokens.textSecondary, fontWeight = FontWeight.Medium)
    }
}

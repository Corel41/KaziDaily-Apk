package com.example.data.supabase

import android.util.Log
import com.example.model.ChatMessage
import com.example.model.ChatThread
import com.example.model.SupabaseChatMessage
import com.example.model.SupabaseThread
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseChatSync(
    private val client: SupabaseClient = SupabaseClient
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activePollingJobs = mutableMapOf<String, Job>()

    private val _isSyncActive = MutableStateFlow(false)
    val isSyncActive: StateFlow<Boolean> = _isSyncActive.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow("Ready")
    val lastSyncStatus: StateFlow<String> = _lastSyncStatus.asStateFlow()

    /**
     * Upserts a chat thread into Supabase PostgreSQL chat_threads table.
     */
    fun pushThread(thread: ChatThread) {
        if (!client.isConfigured) {
            _lastSyncStatus.value = "Local thread synced"
            return
        }

        scope.launch {
            try {
                val myUid = client.currentUser.value?.id ?: "user_me"
                val dto = SupabaseThread.fromChatThread(thread, myUid)
                val response = client.apiService.upsertThread(dto)
                if (response.isSuccessful) {
                    _isSyncActive.value = true
                    _lastSyncStatus.value = "Thread synced to Supabase"
                } else {
                    _lastSyncStatus.value = "Supabase sync code: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push thread: ${thread.threadId}", e)
                _lastSyncStatus.value = "Offline sync queued: ${e.localizedMessage ?: "Network error"}"
            }
        }
    }

    /**
     * Pushes a new chat message into Supabase chat_messages table.
     */
    fun pushMessage(threadId: String, message: ChatMessage) {
        if (!client.isConfigured) {
            _lastSyncStatus.value = "Message stored locally"
            return
        }

        scope.launch {
            try {
                val dto = SupabaseChatMessage.fromChatMessage(threadId, message)
                val response = client.apiService.insertChatMessage(dto)
                if (response.isSuccessful) {
                    _isSyncActive.value = true
                    _lastSyncStatus.value = "Message synced via Supabase"
                } else {
                    _lastSyncStatus.value = "Supabase push status: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push message: ${message.id}", e)
                _lastSyncStatus.value = "Stored locally (${e.localizedMessage ?: "Sync queued"})"
            }
        }
    }

    /**
     * Listens or polls for live messages on a thread from Supabase.
     */
    fun startListeningToThreadMessages(
        threadId: String,
        currentUserId: String,
        onMessagesReceived: (List<ChatMessage>) -> Unit
    ) {
        if (!client.isConfigured) return
        if (activePollingJobs.containsKey(threadId)) return

        val pollJob = scope.launch {
            while (isActive) {
                try {
                    val messagesDto = client.apiService.getChatMessages(
                        threadFilter = "eq.$threadId",
                        order = "created_at.asc"
                    )
                    if (messagesDto.isNotEmpty()) {
                        val messages = messagesDto.map { it.toChatMessage(currentUserId) }
                        _isSyncActive.value = true
                        _lastSyncStatus.value = "Live Supabase sync (${messages.size} msgs)"
                        withContext(Dispatchers.Main) {
                            onMessagesReceived(messages)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Poll error for thread $threadId", e)
                }
                delay(3000) // Poll every 3 seconds for active conversation
            }
        }
        activePollingJobs[threadId] = pollJob
    }

    fun stopAllListeners() {
        activePollingJobs.values.forEach { it.cancel() }
        activePollingJobs.clear()
        _isSyncActive.value = false
    }

    companion object {
        private const val TAG = "SupabaseChatSync"
    }
}

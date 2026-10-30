package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.ChatMessage
import com.example.model.ChatThread
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirestoreChatSync(
    private val firestore: FirebaseFirestore?
) {
    private val auth: FirebaseAuth = Firebase.auth
    private var threadsListener: ListenerRegistration? = null
    private val activeMessageListeners = mutableMapOf<String, ListenerRegistration>()

    private val _isSyncActive = MutableStateFlow(false)
    val isSyncActive: StateFlow<Boolean> = _isSyncActive.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow("Ready")
    val lastSyncStatus: StateFlow<String> = _lastSyncStatus.asStateFlow()

    /**
     * Writes or merges a thread into Firestore so multiple devices discover it.
     */
    fun pushThread(thread: ChatThread) {
        if (firestore == null) return
        try {
            val myUid = auth.currentUser?.uid ?: "user_me"
            val participants = listOfNotNull(myUid, thread.otherPartyId).distinct()
            val data = hashMapOf(
                "id" to thread.threadId,
                "jobId" to thread.jobId,
                "jobTitle" to thread.jobTitle,
                "otherPartyId" to thread.otherPartyId,
                "otherPartyName" to thread.otherPartyName,
                "otherPartyRole" to thread.otherPartyRole,
                "lastMessage" to thread.lastMessage,
                "lastTimestamp" to thread.lastTimestamp,
                "participantUids" to participants,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_THREADS)
                .document(thread.threadId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    _isSyncActive.value = true
                    _lastSyncStatus.value = "Thread synced to cloud"
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to push thread: ${thread.threadId}", e)
                    _lastSyncStatus.value = "Offline sync pending: ${e.localizedMessage ?: "Network error"}"
                }
        } catch (e: Exception) {
            Log.w(TAG, "Exception pushing thread", e)
        }
    }

    /**
     * Pushes a new chat message to the subcollection threads/{threadId}/messages/{messageId}
     * and updates the thread's last message.
     */
    fun pushMessage(threadId: String, message: ChatMessage) {
        if (firestore == null) return
        try {
            val effectiveSenderId = auth.currentUser?.uid ?: message.senderId
            val msgData = hashMapOf(
                "id" to message.id,
                "senderId" to effectiveSenderId,
                "senderName" to message.senderName,
                "text" to message.text,
                "timestampFormatted" to message.timestampFormatted,
                "timestamp" to System.currentTimeMillis()
            )

            val threadRef = firestore.collection(COLLECTION_THREADS).document(threadId)
            threadRef.collection(COLLECTION_MESSAGES)
                .document(message.id)
                .set(msgData)
                .addOnSuccessListener {
                    _isSyncActive.value = true
                    _lastSyncStatus.value = "Message synced across devices"
                    threadRef.set(
                        mapOf(
                            "lastMessage" to message.text,
                            "lastTimestamp" to message.timestampFormatted,
                            "updatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to push message ${message.id} to thread $threadId", e)
                    _lastSyncStatus.value = "Message stored locally (${e.localizedMessage ?: "Sync queued"})"
                }
        } catch (e: Exception) {
            Log.w(TAG, "Exception pushing message", e)
        }
    }

    /**
     * Listens for real-time messages within a specific thread to support multi-device conversation.
     */
    fun startListeningToThreadMessages(
        threadId: String,
        currentUserId: String,
        onMessagesReceived: (List<ChatMessage>) -> Unit
    ) {
        if (firestore == null) return
        if (activeMessageListeners.containsKey(threadId)) return

        try {
            val listener = firestore.collection(COLLECTION_THREADS)
                .document(threadId)
                .collection(COLLECTION_MESSAGES)
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "SnapshotListener error on thread $threadId", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val messages = snapshot.documents.mapNotNull { doc ->
                            val id = doc.getString("id") ?: doc.id
                            val senderId = doc.getString("senderId") ?: "unknown"
                            val senderName = doc.getString("senderName") ?: "User"
                            val text = doc.getString("text") ?: return@mapNotNull null
                            val timeFormatted = doc.getString("timestampFormatted") ?: "Just now"

                            ChatMessage(
                                id = id,
                                senderId = senderId,
                                senderName = senderName,
                                text = text,
                                timestampFormatted = timeFormatted,
                                isFromCurrentUser = senderId == currentUserId
                            )
                        }
                        _isSyncActive.value = true
                        _lastSyncStatus.value = "Live sync connected (${messages.size} msgs)"
                        onMessagesReceived(messages)
                    }
                }
            activeMessageListeners[threadId] = listener
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register message listener", e)
        }
    }

    fun stopAllListeners() {
        threadsListener?.remove()
        threadsListener = null
        activeMessageListeners.values.forEach { it.remove() }
        activeMessageListeners.clear()
        _isSyncActive.value = false
    }

    companion object {
        private const val TAG = "FirestoreChatSync"
        private const val COLLECTION_THREADS = "threads"
        private const val COLLECTION_MESSAGES = "messages"

        fun create(context: Context): FirestoreChatSync {
            return try {
                val dbId = context.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                FirestoreChatSync(db)
            } catch (e: Exception) {
                Log.w(TAG, "Could not initialize FirebaseFirestore with named databaseId", e)
                FirestoreChatSync(null)
            }
        }
    }
}

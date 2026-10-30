package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreJob(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "OTHER",
    val payKsh: Long = 0L,
    val durationHours: Long = 1L,
    val locationName: String = "",
    val latitude: Double = -1.2864,
    val longitude: Double = 36.8172,
    val employerId: String = "",
    val employerName: String = "",
    val employerRating: Double = 5.0,
    val employerReviewCount: Long = 0L,
    val status: String = "OPEN",
    val assignedWorkerId: String? = null,
    val assignedWorkerName: String? = null,
    val escrowAmountKsh: Long = 0L,
    val platformFeePercent: Long = 10L,
    val workerReviewed: Boolean = false,
    val employerReviewed: Boolean = false,
    val photoUrls: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toJob(): Job {
        val cat = try {
            JobCategory.valueOf(category.uppercase())
        } catch (e: Exception) {
            JobCategory.OTHER
        }
        val st = try {
            JobStatus.valueOf(status.uppercase())
        } catch (e: Exception) {
            JobStatus.OPEN
        }
        return Job(
            id = id,
            title = title,
            description = description,
            category = cat,
            payKsh = payKsh.toInt(),
            durationHours = durationHours.toInt(),
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            employerId = employerId,
            employerName = employerName,
            employerRating = employerRating.toFloat(),
            employerReviewCount = employerReviewCount.toInt(),
            status = st,
            datePosted = "Active",
            assignedWorkerId = assignedWorkerId,
            assignedWorkerName = assignedWorkerName,
            escrowAmountKsh = escrowAmountKsh.toInt(),
            platformFeePercent = platformFeePercent.toInt(),
            workerReviewed = workerReviewed,
            employerReviewed = employerReviewed,
            photoUrls = photoUrls
        )
    }
}

@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val phone: String = "+254 700 000 000",
    val rating: Double = 5.0,
    val reviewCount: Long = 0L,
    val completedJobs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreWalletSummary(
    val userId: String = "",
    val availableBalanceKsh: Long = 0L,
    val lockedInEscrowKsh: Long = 0L,
    val totalEarnedKsh: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreWalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "ESCROW_PAYOUT",
    val amountKsh: Long = 0L,
    val description: String = "",
    val referenceJobId: String = "",
    val mpesaReceipt: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreThread(
    @DocumentId val id: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val participantUids: List<String> = emptyList(),
    val otherUserName: String = "",
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val unreadCount: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

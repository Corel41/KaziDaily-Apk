package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseJob(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "category") val category: String,
    @Json(name = "pay_ksh") val payKsh: Long,
    @Json(name = "duration_hours") val durationHours: Long,
    @Json(name = "location_name") val locationName: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "employer_id") val employerId: String,
    @Json(name = "employer_name") val employerName: String,
    @Json(name = "employer_rating") val employerRating: Double = 5.0,
    @Json(name = "employer_review_count") val employerReviewCount: Long = 0,
    @Json(name = "status") val status: String = "OPEN",
    @Json(name = "escrow_amount_ksh") val escrowAmountKsh: Long = 0,
    @Json(name = "platform_fee_percent") val platformFeePercent: Long = 10,
    @Json(name = "assigned_worker_id") val assignedWorkerId: String? = null,
    @Json(name = "assigned_worker_name") val assignedWorkerName: String? = null,
    @Json(name = "worker_reviewed") val workerReviewed: Boolean = false,
    @Json(name = "employer_reviewed") val employerReviewed: Boolean = false,
    @Json(name = "photo_urls") val photoUrls: List<String>? = emptyList(),
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    fun toJob(): Job {
        val cat = try {
            JobCategory.valueOf(category.uppercase())
        } catch (e: Exception) {
            JobCategory.REPAIRS
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
            escrowAmountKsh = escrowAmountKsh.toInt(),
            platformFeePercent = platformFeePercent.toInt(),
            assignedWorkerId = assignedWorkerId,
            assignedWorkerName = assignedWorkerName,
            workerReviewed = workerReviewed,
            employerReviewed = employerReviewed,
            photoUrls = photoUrls ?: emptyList(),
            datePosted = createdAt?.take(10) ?: "Today"
        )
    }

    companion object {
        fun fromJob(job: Job): SupabaseJob {
            return SupabaseJob(
                id = job.id,
                title = job.title,
                description = job.description,
                category = job.category.name,
                payKsh = job.payKsh.toLong(),
                durationHours = job.durationHours.toLong(),
                locationName = job.locationName,
                latitude = job.latitude,
                longitude = job.longitude,
                employerId = job.employerId,
                employerName = job.employerName,
                employerRating = job.employerRating.toDouble(),
                employerReviewCount = job.employerReviewCount.toLong(),
                status = job.status.name,
                escrowAmountKsh = job.escrowAmountKsh.toLong(),
                platformFeePercent = job.platformFeePercent.toLong(),
                assignedWorkerId = job.assignedWorkerId,
                assignedWorkerName = job.assignedWorkerName,
                workerReviewed = job.workerReviewed,
                employerReviewed = job.employerReviewed,
                photoUrls = job.photoUrls
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class SupabaseProfile(
    @Json(name = "id") val id: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "role_title") val roleTitle: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "skills") val skills: List<String>? = emptyList(),
    @Json(name = "experience_years") val experienceYears: Int = 1,
    @Json(name = "average_rating") val averageRating: Double = 5.0,
    @Json(name = "total_reviews") val totalReviews: Int = 0,
    @Json(name = "jobs_completed_worker") val jobsCompletedWorker: Int = 0,
    @Json(name = "jobs_posted_employer") val jobsPostedEmployer: Int = 0,
    @Json(name = "is_phone_verified") val isPhoneVerified: Boolean = false,
    @Json(name = "is_id_verified") val isIdVerified: Boolean = false,
    @Json(name = "location") val location: String? = "Nairobi, Kenya",
    @Json(name = "avatar_url") val avatarUrl: String? = null
) {
    fun toUserProfile(): UserProfile {
        return UserProfile(
            id = id,
            fullName = fullName,
            phone = phone ?: "+254 700 000 000",
            roleTitle = roleTitle ?: "Verified Artisan",
            bio = bio ?: "Experienced daily gig worker and professional contractor across Nairobi.",
            skills = skills ?: listOf("Handyman", "Repairs"),
            experienceYears = experienceYears,
            averageRating = averageRating.toFloat(),
            totalReviews = totalReviews,
            jobsCompletedAsWorker = jobsCompletedWorker,
            jobsPostedAsEmployer = jobsPostedEmployer,
            isPhoneVerified = isPhoneVerified,
            isIdVerified = isIdVerified,
            location = location ?: "Nairobi, Kenya"
        )
    }

    companion object {
        fun fromUserProfile(p: UserProfile): SupabaseProfile {
            return SupabaseProfile(
                id = p.id,
                fullName = p.fullName,
                phone = p.phone,
                roleTitle = p.roleTitle,
                bio = p.bio,
                skills = p.skills,
                experienceYears = p.experienceYears,
                averageRating = p.averageRating.toDouble(),
                totalReviews = p.totalReviews,
                jobsCompletedWorker = p.jobsCompletedAsWorker,
                jobsPostedEmployer = p.jobsPostedAsEmployer,
                isPhoneVerified = p.isPhoneVerified,
                isIdVerified = p.isIdVerified,
                location = p.location
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class SupabaseWalletTransaction(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "title") val title: String,
    @Json(name = "subtitle") val subtitle: String,
    @Json(name = "amount_ksh") val amountKsh: Long,
    @Json(name = "date_formatted") val dateFormatted: String,
    @Json(name = "type") val type: String,
    @Json(name = "is_positive") val isPositive: Boolean,
    @Json(name = "created_at") val createdAt: String? = null
) {
    fun toWalletTransaction(): WalletTransaction {
        val txType = try {
            TransactionType.valueOf(type)
        } catch (e: Exception) {
            TransactionType.ESCROW_RELEASE
        }
        return WalletTransaction(
            id = id,
            title = title,
            subtitle = subtitle,
            amountKsh = amountKsh.toInt(),
            dateFormatted = dateFormatted,
            type = txType,
            isPositive = isPositive
        )
    }
}

@JsonClass(generateAdapter = true)
data class SupabaseReview(
    @Json(name = "id") val id: String,
    @Json(name = "job_id") val jobId: String,
    @Json(name = "job_title") val jobTitle: String,
    @Json(name = "reviewer_id") val reviewerId: String,
    @Json(name = "reviewer_name") val reviewerName: String,
    @Json(name = "reviewer_role") val reviewerRole: String = "Worker",
    @Json(name = "target_user_id") val targetUserId: String,
    @Json(name = "target_user_name") val targetUserName: String = "Employer",
    @Json(name = "rating") val rating: Long,
    @Json(name = "tags") val tags: List<String>? = emptyList(),
    @Json(name = "comment") val comment: String,
    @Json(name = "date_formatted") val dateFormatted: String = "Today",
    @Json(name = "created_at") val createdAt: String? = null
) {
    fun toReview(): Review {
        return Review(
            id = id,
            jobId = jobId,
            jobTitle = jobTitle,
            reviewerId = reviewerId,
            reviewerName = reviewerName,
            reviewerRole = reviewerRole,
            revieweeId = targetUserId,
            revieweeName = targetUserName,
            rating = rating.toInt(),
            tags = tags ?: emptyList(),
            comment = comment,
            timestampFormatted = dateFormatted
        )
    }
}

@JsonClass(generateAdapter = true)
data class SupabaseThread(
    @Json(name = "id") val id: String,
    @Json(name = "job_id") val jobId: String,
    @Json(name = "job_title") val jobTitle: String,
    @Json(name = "other_party_id") val otherPartyId: String,
    @Json(name = "other_party_name") val otherPartyName: String,
    @Json(name = "other_party_role") val otherPartyRole: String,
    @Json(name = "last_message") val lastMessage: String,
    @Json(name = "last_timestamp") val lastTimestamp: String,
    @Json(name = "unread_count") val unreadCount: Int = 0,
    @Json(name = "participant_ids") val participantIds: List<String>? = emptyList(),
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    fun toChatThread(messages: List<ChatMessage> = emptyList()): ChatThread {
        return ChatThread(
            threadId = id,
            jobId = jobId,
            jobTitle = jobTitle,
            otherPartyId = otherPartyId,
            otherPartyName = otherPartyName,
            otherPartyRole = otherPartyRole,
            lastMessage = lastMessage,
            lastTimestamp = lastTimestamp,
            unreadCount = unreadCount,
            messages = messages
        )
    }

    companion object {
        fun fromChatThread(t: ChatThread, myId: String): SupabaseThread {
            val participants = listOfNotNull(myId, t.otherPartyId).distinct()
            return SupabaseThread(
                id = t.threadId,
                jobId = t.jobId,
                jobTitle = t.jobTitle,
                otherPartyId = t.otherPartyId,
                otherPartyName = t.otherPartyName,
                otherPartyRole = t.otherPartyRole,
                lastMessage = t.lastMessage,
                lastTimestamp = t.lastTimestamp,
                unreadCount = t.unreadCount,
                participantIds = participants
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class SupabaseChatMessage(
    @Json(name = "id") val id: String,
    @Json(name = "thread_id") val threadId: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "sender_name") val senderName: String,
    @Json(name = "text") val text: String,
    @Json(name = "timestamp_formatted") val timestampFormatted: String,
    @Json(name = "created_at") val createdAt: String? = null
) {
    fun toChatMessage(currentUserId: String): ChatMessage {
        return ChatMessage(
            id = id,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestampFormatted = timestampFormatted,
            isFromCurrentUser = senderId == currentUserId
        )
    }

    companion object {
        fun fromChatMessage(threadId: String, msg: ChatMessage): SupabaseChatMessage {
            return SupabaseChatMessage(
                id = msg.id,
                threadId = threadId,
                senderId = msg.senderId,
                senderName = msg.senderName,
                text = msg.text,
                timestampFormatted = msg.timestampFormatted
            )
        }
    }
}

// MARK: - Supabase Auth Models

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    val id: String,
    val email: String?,
    val fullName: String?,
    val avatarUrl: String? = null,
    val accessToken: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "user") val user: SupabaseAuthUser? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthUser(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseIdTokenRequest(
    @Json(name = "provider") val provider: String = "google",
    @Json(name = "id_token") val idToken: String
)

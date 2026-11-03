package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class KaziRepository(
    private val supabaseSync: com.example.data.supabase.SupabaseChatSync? = com.example.data.supabase.SupabaseChatSync(),
    private val firestoreSync: FirestoreChatSync? = null
) {
    // Convenience constructor for Android Context
    constructor(context: android.content.Context) : this(
        supabaseSync = com.example.data.supabase.SupabaseChatSync(),
        firestoreSync = null
    )

    private val _userProfile = MutableStateFlow(
        UserProfile(
            id = "user_me",
            fullName = "Brian Mwangi",
            phone = "+254 712 345 678",
            roleTitle = "Verified Artisan & Homeowner",
            bio = "Certified electrician & handyman with 5+ years experience in domestic repairs, deep cleaning, and appliance setups across Nairobi.",
            skills = listOf("Electrical Repairs", "Deep Cleaning", "Plumbing", "Furniture Assembly", "Painting"),
            experienceYears = 5,
            averageRating = 4.88f,
            totalReviews = 28,
            jobsCompletedAsWorker = 26,
            jobsPostedAsEmployer = 4,
            isPhoneVerified = true,
            isIdVerified = true,
            location = "Westlands, Nairobi"
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _jobs = MutableStateFlow(initialJobs())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    private val _walletState = MutableStateFlow(
        WalletState(
            availableBalanceKsh = 8450,
            lockedInEscrowKsh = 2500,
            totalEarningsKsh = 36800,
            transactions = initialTransactions()
        )
    )
    val walletState: StateFlow<WalletState> = _walletState.asStateFlow()

    private val _reviews = MutableStateFlow(initialReviews())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _chatThreads = MutableStateFlow(initialChatThreads())
    val chatThreads: StateFlow<List<ChatThread>> = _chatThreads.asStateFlow()

    // MARK: - Job Application & Escrow Operations

    fun applyForJob(jobId: String): Boolean {
        val currentJobs = _jobs.value.toMutableList()
        val index = currentJobs.indexOfFirst { it.id == jobId }
        if (index == -1) return false

        val job = currentJobs[index]
        if (job.status != JobStatus.OPEN) return false

        val updatedJob = job.copy(
            status = JobStatus.APPLIED,
            assignedWorkerId = _userProfile.value.id,
            assignedWorkerName = _userProfile.value.fullName
        )
        currentJobs[index] = updatedJob
        _jobs.value = currentJobs

        // Create or ensure chat thread exists
        val currentThreads = _chatThreads.value.toMutableList()
        val existingThread = currentThreads.find { it.jobId == jobId }
        if (existingThread == null) {
            val newThread = ChatThread(
                threadId = UUID.randomUUID().toString(),
                jobId = jobId,
                jobTitle = job.title,
                otherPartyId = job.employerId,
                otherPartyName = job.employerName,
                otherPartyRole = "Employer",
                lastMessage = "I have submitted my application for this gig.",
                lastTimestamp = "Just now",
                unreadCount = 0,
                messages = listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        senderId = _userProfile.value.id,
                        senderName = _userProfile.value.fullName,
                        text = "Hello ${job.employerName}, I have applied for '${job.title}'. I am available immediately with all necessary tools.",
                        timestampFormatted = "Just now",
                        isFromCurrentUser = true
                    )
                )
            )
            currentThreads.add(0, newThread)
            _chatThreads.value = currentThreads
        }

        return true
    }

    /**
     * Holds payment in simulated escrow when a worker accepts or is assigned a job.
     * The funds are safely locked before any physical work starts.
     */
    fun holdPaymentInEscrow(jobId: String, customAmountKsh: Int? = null): Boolean {
        val currentJobs = _jobs.value.toMutableList()
        val index = currentJobs.indexOfFirst { it.id == jobId }
        if (index == -1) return false

        val job = currentJobs[index]
        val lockAmount = customAmountKsh ?: job.payKsh

        val updatedJob = job.copy(
            status = JobStatus.IN_PROGRESS,
            escrowAmountKsh = lockAmount
        )
        currentJobs[index] = updatedJob
        _jobs.value = currentJobs

        // Update wallet escrow balance and record transaction
        val currentWallet = _walletState.value
        val updatedTx = currentWallet.transactions.toMutableList()
        updatedTx.add(0, WalletTransaction(
            id = UUID.randomUUID().toString(),
            title = "Escrow Deposit: ${job.title}",
            subtitle = "Funds locked in simulated escrow until employer confirms completion",
            amountKsh = lockAmount,
            dateFormatted = "Just now",
            type = TransactionType.ESCROW_DEPOSIT,
            isPositive = false
        ))

        _walletState.value = currentWallet.copy(
            lockedInEscrowKsh = currentWallet.lockedInEscrowKsh + lockAmount,
            transactions = updatedTx
        )

        addMessageToThread(
            jobId = jobId,
            text = "🔒 Payment of KSh $lockAmount has been placed in Escrow. Work authorized to proceed!",
            isSystem = true
        )

        return true
    }

    // Alias for holdPaymentInEscrow
    fun fundEscrowAndStartJob(jobId: String): Boolean = holdPaymentInEscrow(jobId)

    data class CompletionPayoutResult(
        val grossKsh: Int,
        val platformFeeKsh: Int,
        val netPayoutKsh: Int,
        val jobTitle: String
    )

    /**
     * Releases escrow funds to the worker's wallet balance ONLY after the employer
     * confirms completion, applying a fixed percentage service fee during the release.
     */
    fun releaseEscrowFundsToWorker(
        jobId: String,
        fixedFeePercent: Int = 10
    ): CompletionPayoutResult? {
        val currentJobs = _jobs.value.toMutableList()
        val index = currentJobs.indexOfFirst { it.id == jobId }
        if (index == -1) return null

        val job = currentJobs[index]
        val grossKsh = if (job.escrowAmountKsh > 0) job.escrowAmountKsh else job.payKsh
        val feeKsh = (grossKsh * fixedFeePercent) / 100
        val netPayoutKsh = grossKsh - feeKsh

        val updatedJob = job.copy(
            status = JobStatus.COMPLETED,
            escrowAmountKsh = 0
        )
        currentJobs[index] = updatedJob
        _jobs.value = currentJobs

        // Credit net funds to worker's wallet balance
        val currentWallet = _walletState.value
        val updatedTx = currentWallet.transactions.toMutableList()

        updatedTx.add(0, WalletTransaction(
            id = UUID.randomUUID().toString(),
            title = "Escrow Release: ${job.title}",
            subtitle = "Gross KSh $grossKsh minus $fixedFeePercent% service fee (KSh $feeKsh)",
            amountKsh = netPayoutKsh,
            dateFormatted = "Just now",
            type = TransactionType.ESCROW_RELEASE,
            isPositive = true
        ))

        _walletState.value = currentWallet.copy(
            availableBalanceKsh = currentWallet.availableBalanceKsh + netPayoutKsh,
            lockedInEscrowKsh = Math.max(0, currentWallet.lockedInEscrowKsh - grossKsh),
            totalEarningsKsh = currentWallet.totalEarningsKsh + netPayoutKsh,
            transactions = updatedTx
        )

        // Increment user completed jobs
        val user = _userProfile.value
        _userProfile.value = user.copy(
            jobsCompletedAsWorker = user.jobsCompletedAsWorker + 1
        )

        addMessageToThread(
            jobId = jobId,
            text = "✅ Employer confirmed completion! Net KSh $netPayoutKsh released to wallet after $fixedFeePercent% platform service fee (KSh $feeKsh).",
            isSystem = true
        )

        return CompletionPayoutResult(
            grossKsh = grossKsh,
            platformFeeKsh = feeKsh,
            netPayoutKsh = netPayoutKsh,
            jobTitle = job.title
        )
    }

    // Alias for confirmJobCompletion
    fun confirmJobCompletion(jobId: String): CompletionPayoutResult? = releaseEscrowFundsToWorker(jobId)

    // MARK: - Review System

    fun submitReview(
        jobId: String,
        rating: Int,
        tags: List<String>,
        comment: String
    ): Boolean {
        val currentJobs = _jobs.value.toMutableList()
        val index = currentJobs.indexOfFirst { it.id == jobId }
        if (index == -1) return false

        val job = currentJobs[index]
        val updatedJob = job.copy(workerReviewed = true)
        currentJobs[index] = updatedJob
        _jobs.value = currentJobs

        val newReview = Review(
            id = UUID.randomUUID().toString(),
            jobId = jobId,
            jobTitle = job.title,
            reviewerId = _userProfile.value.id,
            reviewerName = _userProfile.value.fullName,
            reviewerRole = "Worker",
            revieweeId = job.employerId,
            revieweeName = job.employerName,
            rating = rating,
            tags = tags,
            comment = comment.ifBlank { "Prompt payment and clear site instructions." },
            timestampFormatted = "Just now"
        )

        val updatedReviews = _reviews.value.toMutableList()
        updatedReviews.add(0, newReview)
        _reviews.value = updatedReviews

        // Recompute user rating
        val user = _userProfile.value
        val newTotal = user.totalReviews + 1
        val newAvg = ((user.averageRating * user.totalReviews) + rating) / newTotal
        _userProfile.value = user.copy(
            totalReviews = newTotal,
            averageRating = Math.round(newAvg * 100f) / 100f
        )

        return true
    }

    // MARK: - In-App Chat Messaging

    fun sendMessage(
        threadId: String,
        text: String,
        customSenderId: String? = null,
        customSenderName: String? = null,
        isFromMe: Boolean = true
    ): Boolean {
        if (text.isBlank()) return false
        val currentThreads = _chatThreads.value.toMutableList()
        val index = currentThreads.indexOfFirst { it.threadId == threadId }
        if (index == -1) return false

        val thread = currentThreads[index]
        val senderId = customSenderId ?: _userProfile.value.id
        val senderName = customSenderName ?: _userProfile.value.fullName

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            senderName = senderName,
            text = text.trim(),
            timestampFormatted = "Just now",
            isFromCurrentUser = isFromMe
        )

        val updatedMessages = thread.messages.toMutableList()
        updatedMessages.add(userMsg)

        val updatedThread = thread.copy(
            lastMessage = userMsg.text,
            lastTimestamp = "Just now",
            messages = updatedMessages
        )
        currentThreads[index] = updatedThread
        _chatThreads.value = currentThreads

        // Push to Supabase backend for multi-device cross-device sync
        supabaseSync?.pushThread(updatedThread)
        supabaseSync?.pushMessage(threadId, userMsg)
        firestoreSync?.pushThread(updatedThread)
        firestoreSync?.pushMessage(threadId, userMsg)

        return true
    }

    fun startListeningToThread(threadId: String) {
        val currentUserId = _userProfile.value.id
        supabaseSync?.startListeningToThreadMessages(threadId, currentUserId) { remoteMessages ->
            if (remoteMessages.isEmpty()) return@startListeningToThreadMessages
            val currentThreads = _chatThreads.value.toMutableList()
            val index = currentThreads.indexOfFirst { it.threadId == threadId }
            if (index != -1) {
                val thread = currentThreads[index]
                val existingIds = thread.messages.map { it.id }.toSet()
                val newIncoming = remoteMessages.filterNot { existingIds.contains(it.id) }
                if (newIncoming.isNotEmpty()) {
                    val merged = thread.messages + newIncoming
                    val last = merged.lastOrNull()
                    currentThreads[index] = thread.copy(
                        messages = merged,
                        lastMessage = last?.text ?: thread.lastMessage,
                        lastTimestamp = last?.timestampFormatted ?: thread.lastTimestamp
                    )
                    _chatThreads.value = currentThreads
                }
            }
        }
        firestoreSync?.startListeningToThreadMessages(threadId, currentUserId) { remoteMessages ->
            if (remoteMessages.isEmpty()) return@startListeningToThreadMessages
            val currentThreads = _chatThreads.value.toMutableList()
            val index = currentThreads.indexOfFirst { it.threadId == threadId }
            if (index != -1) {
                val thread = currentThreads[index]
                val existingIds = thread.messages.map { it.id }.toSet()
                val newIncoming = remoteMessages.filterNot { existingIds.contains(it.id) }
                if (newIncoming.isNotEmpty()) {
                    val merged = thread.messages + newIncoming
                    val last = merged.lastOrNull()
                    currentThreads[index] = thread.copy(
                        messages = merged,
                        lastMessage = last?.text ?: thread.lastMessage,
                        lastTimestamp = last?.timestampFormatted ?: thread.lastTimestamp
                    )
                    _chatThreads.value = currentThreads
                }
            }
        }
    }

    private fun addMessageToThread(jobId: String, text: String, isSystem: Boolean = false) {
        val currentThreads = _chatThreads.value.toMutableList()
        val index = currentThreads.indexOfFirst { it.jobId == jobId }
        if (index == -1) return

        val thread = currentThreads[index]
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = if (isSystem) "system" else thread.otherPartyId,
            senderName = if (isSystem) "KaziDaily Escrow" else thread.otherPartyName,
            text = text,
            timestampFormatted = "Just now",
            isFromCurrentUser = false
        )
        val msgs = thread.messages.toMutableList()
        msgs.add(msg)
        currentThreads[index] = thread.copy(
            lastMessage = text,
            lastTimestamp = "Just now",
            messages = msgs
        )
        _chatThreads.value = currentThreads
    }

    // MARK: - Wallet Withdrawals

    sealed class WithdrawalResult {
        data class Success(val amountKsh: Int, val destination: String, val referenceId: String) : WithdrawalResult()
        data class Failure(val reason: String) : WithdrawalResult()
    }

    fun processWithdrawal(method: String, destinationDetails: String, amountKsh: Int): WithdrawalResult {
        val currentWallet = _walletState.value
        if (amountKsh < 100) {
            return WithdrawalResult.Failure("Minimum withdrawal is KSh 100.")
        }
        if (amountKsh > currentWallet.availableBalanceKsh) {
            return WithdrawalResult.Failure("Insufficient wallet balance.")
        }

        val ref = "KZ" + (100000..999999).random()
        val txType = if (method == "M-Pesa") TransactionType.WITHDRAWAL_MPESA else TransactionType.WITHDRAWAL_BANK
        val title = "$method Withdrawal ($destinationDetails)"

        val newTx = WalletTransaction(
            id = UUID.randomUUID().toString(),
            title = title,
            subtitle = "Reference: $ref · Processed instantly",
            amountKsh = amountKsh,
            dateFormatted = "Just now",
            type = txType,
            isPositive = false
        )

        val updatedList = currentWallet.transactions.toMutableList()
        updatedList.add(0, newTx)

        _walletState.value = currentWallet.copy(
            availableBalanceKsh = currentWallet.availableBalanceKsh - amountKsh,
            transactions = updatedList
        )

        return WithdrawalResult.Success(amountKsh, destinationDetails, ref)
    }

    // MARK: - Job Posting

    fun createJob(
        title: String,
        category: JobCategory,
        locationName: String,
        latitude: Double,
        longitude: Double,
        payKsh: Int,
        durationHours: Int,
        description: String,
        photoUrls: List<String> = emptyList()
    ): Job {
        val user = _userProfile.value
        val newJob = Job(
            id = "job_" + UUID.randomUUID().toString().take(8),
            title = title,
            description = description.ifBlank { "Immediate daily task in $locationName. All safety instructions provided on site." },
            category = category,
            payKsh = payKsh,
            durationHours = durationHours,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            employerId = user.id,
            employerName = user.fullName,
            employerRating = user.averageRating,
            employerReviewCount = user.totalReviews,
            status = JobStatus.OPEN,
            datePosted = "Just now",
            photoUrls = photoUrls
        )

        val list = _jobs.value.toMutableList()
        list.add(0, newJob)
        _jobs.value = list

        _userProfile.value = user.copy(
            jobsPostedAsEmployer = user.jobsPostedAsEmployer + 1
        )

        return newJob
    }

    fun updateUserAvatar(photoUrl: String) {
        val user = _userProfile.value
        _userProfile.value = user.copy(photoUrl = photoUrl)
    }

    fun addSkill(newSkill: String) {
        val currentSkills = _userProfile.value.skills.toMutableList()
        if (newSkill.isNotBlank() && !currentSkills.contains(newSkill.trim())) {
            currentSkills.add(newSkill.trim())
            _userProfile.value = _userProfile.value.copy(skills = currentSkills)
        }
    }

    fun resetData() {
        _userProfile.value = UserProfile(
            id = "user_me",
            fullName = "Brian Mwangi",
            phone = "+254 712 345 678",
            roleTitle = "Verified Artisan & Homeowner",
            bio = "Certified electrician & handyman with 5+ years experience in domestic repairs, deep cleaning, and appliance setups across Nairobi.",
            skills = listOf("Electrical Repairs", "Deep Cleaning", "Plumbing", "Furniture Assembly", "Painting"),
            experienceYears = 5,
            averageRating = 4.88f,
            totalReviews = 28,
            jobsCompletedAsWorker = 26,
            jobsPostedAsEmployer = 4,
            isPhoneVerified = true,
            isIdVerified = true,
            location = "Westlands, Nairobi"
        )
        _jobs.value = initialJobs()
        _walletState.value = WalletState(
            availableBalanceKsh = 8450,
            lockedInEscrowKsh = 2500,
            totalEarningsKsh = 36800,
            transactions = initialTransactions()
        )
        _reviews.value = initialReviews()
        _chatThreads.value = initialChatThreads()
    }

    // MARK: - Initial Mock Seeds

    private fun initialJobs(): List<Job> = listOf(
        Job(
            id = "job_1",
            title = "House Deep Cleaning & Sanitizing",
            description = "3-bedroom apartment full deep clean including kitchen cabinets, windows, and balcony tile scrubbing. Cleaning liquids provided.",
            category = JobCategory.CLEANING,
            payKsh = 2500,
            durationHours = 3,
            locationName = "Kilimani",
            latitude = -1.2891,
            longitude = 36.7865,
            employerId = "emp_1",
            employerName = "Amina Wanjiku",
            employerRating = 4.95f,
            employerReviewCount = 18,
            status = JobStatus.OPEN,
            datePosted = "Today",
            photoUrls = listOf(
                "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1527515637462-cff94eecc1ac?w=800&auto=format&fit=crop&q=80"
            )
        ),
        Job(
            id = "job_2",
            title = "E-Commerce Courier & Delivery Runner",
            description = "Pick up 6 packed boxes from CBD Kencom hub and dispatch along Waiyaki Way & Westlands. Fuel / fare reimbursed separately.",
            category = JobCategory.DELIVERY,
            payKsh = 1800,
            durationHours = 2,
            locationName = "Nairobi CBD",
            latitude = -1.2864,
            longitude = 36.8249,
            employerId = "emp_2",
            employerName = "Kevin Otieno",
            employerRating = 4.70f,
            employerReviewCount = 32,
            status = JobStatus.OPEN,
            datePosted = "Today",
            photoUrls = listOf(
                "https://images.unsplash.com/photo-1549465220-1a8b9238cd48?w=800&auto=format&fit=crop&q=80"
            )
        ),
        Job(
            id = "job_3",
            title = "Home Relocation Helper & Furniture Lift",
            description = "Loading heavy household items (sofa set, dining table, fridge) into a truck. Requires 2 strong movers.",
            category = JobCategory.MOVING,
            payKsh = 3200,
            durationHours = 4,
            locationName = "Upper Hill",
            latitude = -1.2985,
            longitude = 36.8188,
            employerId = "emp_3",
            employerName = "Patrick Kamau",
            employerRating = 4.85f,
            employerReviewCount = 12,
            status = JobStatus.IN_PROGRESS,
            assignedWorkerId = "user_me",
            assignedWorkerName = "Brian Mwangi",
            escrowAmountKsh = 3200,
            datePosted = "Yesterday",
            photoUrls = listOf(
                "https://images.unsplash.com/photo-1600585154526-990dced4db0d?w=800&auto=format&fit=crop&q=80"
            )
        ),
        Job(
            id = "job_4",
            title = "Garden Hedge Trimming & Lawn Mowing",
            description = "Quarter-acre compound hedge shaping, lawn trimming, and garden waste bagging. Lawnmower available on site.",
            category = JobCategory.GARDENING,
            payKsh = 2200,
            durationHours = 3,
            locationName = "Karen",
            latitude = -1.3204,
            longitude = 36.7058,
            employerId = "emp_4",
            employerName = "Winnie Nduta",
            employerRating = 5.0f,
            employerReviewCount = 8,
            status = JobStatus.OPEN,
            datePosted = "2 hours ago",
            photoUrls = listOf(
                "https://images.unsplash.com/photo-1592417817098-8f3d6910985b?w=800&auto=format&fit=crop&q=80"
            )
        ),
        Job(
            id = "job_5",
            title = "Commercial Office Circuit Breaker Fix",
            description = "Diagnosing tripping main breaker and replacing faulty 32A fuse switches in retail boutique.",
            category = JobCategory.ELECTRICAL,
            payKsh = 4000,
            durationHours = 2,
            locationName = "Parklands",
            latitude = -1.2612,
            longitude = 36.8198,
            employerId = "emp_5",
            employerName = "Suresh Patel",
            employerRating = 4.80f,
            employerReviewCount = 41,
            status = JobStatus.OPEN,
            datePosted = "3 hours ago"
        ),
        Job(
            id = "job_6",
            title = "Event Catering Prep & Serving Assistant",
            description = "Assist chef with vegetable prep, buffet setup, and plate service for a private birthday luncheon.",
            category = JobCategory.CATERING,
            payKsh = 2000,
            durationHours = 4,
            locationName = "Lavington",
            latitude = -1.2785,
            longitude = 36.7725,
            employerId = "emp_6",
            employerName = "Sarah Mutua",
            employerRating = 4.90f,
            employerReviewCount = 15,
            status = JobStatus.OPEN,
            datePosted = "Today"
        )
    )

    private fun initialTransactions(): List<WalletTransaction> = listOf(
        WalletTransaction(
            id = "tx_1",
            title = "Electrical repair payout",
            subtitle = "Gross KSh 3,000 - 10% platform fee (KSh 300)",
            amountKsh = 2700,
            dateFormatted = "Today, 10:45 AM",
            type = TransactionType.ESCROW_RELEASE,
            isPositive = true
        ),
        WalletTransaction(
            id = "tx_2",
            title = "Deep cleaning release",
            subtitle = "Gross KSh 2,000 - 10% platform fee (KSh 200)",
            amountKsh = 1800,
            dateFormatted = "Yesterday, 4:20 PM",
            type = TransactionType.ESCROW_RELEASE,
            isPositive = true
        ),
        WalletTransaction(
            id = "tx_3",
            title = "M-Pesa Withdrawal (0712345678)",
            subtitle = "Reference: KZ849201 · Completed",
            amountKsh = 3500,
            dateFormatted = "Oct 27, 2:15 PM",
            type = TransactionType.WITHDRAWAL_MPESA,
            isPositive = false
        )
    )

    private fun initialReviews(): List<Review> = listOf(
        Review(
            id = "rev_1",
            jobId = "job_prev_1",
            jobTitle = "Socket Rewiring & Lighting",
            reviewerId = "emp_1",
            reviewerName = "Amina Wanjiku",
            reviewerRole = "Employer",
            revieweeId = "user_me",
            revieweeName = "Brian Mwangi",
            rating = 5,
            tags = listOf("Punctual", "Quality Work", "Courteous"),
            comment = "Brian arrived right on time and fixed our faulty kitchen breaker in less than an hour. Very professional and tidy!",
            timestampFormatted = "Yesterday"
        ),
        Review(
            id = "rev_2",
            jobId = "job_prev_2",
            jobTitle = "Water Pump Installation",
            reviewerId = "emp_3",
            reviewerName = "Patrick Kamau",
            reviewerRole = "Employer",
            revieweeId = "user_me",
            revieweeName = "Brian Mwangi",
            rating = 5,
            tags = listOf("High Skill", "Accurate Quote", "Fast"),
            comment = "Excellent plumbing work. Explained everything beforehand and tested the pump thoroughly. Highly recommended.",
            timestampFormatted = "3 days ago"
        ),
        Review(
            id = "rev_3",
            jobId = "job_prev_3",
            jobTitle = "Office Painting Touchup",
            reviewerId = "emp_5",
            reviewerName = "Suresh Patel",
            reviewerRole = "Employer",
            revieweeId = "user_me",
            revieweeName = "Brian Mwangi",
            rating = 4,
            tags = listOf("Clean Work", "Good Communication"),
            comment = "Did a clean job on the walls and kept the floor spotless. Will hire again.",
            timestampFormatted = "1 week ago"
        )
    )

    private fun initialChatThreads(): List<ChatThread> = listOf(
        ChatThread(
            threadId = "th_1",
            jobId = "job_3",
            jobTitle = "Home Relocation Helper & Furniture Lift",
            otherPartyId = "emp_3",
            otherPartyName = "Patrick Kamau",
            otherPartyRole = "Employer",
            lastMessage = "Escrow is locked. Let me know when you reach Hospital Rd gate.",
            lastTimestamp = "11:20 AM",
            unreadCount = 1,
            messages = listOf(
                ChatMessage(
                    id = "msg_1",
                    senderId = "emp_3",
                    senderName = "Patrick Kamau",
                    text = "Hello Brian, thank you for applying. I have accepted you for the relocation task.",
                    timestampFormatted = "10:15 AM",
                    isFromCurrentUser = false
                ),
                ChatMessage(
                    id = "msg_2",
                    senderId = "system",
                    senderName = "KaziDaily Escrow",
                    text = "🔒 Payment of KSh 3,200 has been securely locked in Escrow by Patrick Kamau.",
                    timestampFormatted = "10:18 AM",
                    isFromCurrentUser = false
                ),
                ChatMessage(
                    id = "msg_3",
                    senderId = "user_me",
                    senderName = "Brian Mwangi",
                    text = "Thanks Patrick! I see the escrow confirmation. I am leaving Westlands now and will arrive in approx 18 minutes.",
                    timestampFormatted = "10:25 AM",
                    isFromCurrentUser = true
                ),
                ChatMessage(
                    id = "msg_4",
                    senderId = "emp_3",
                    senderName = "Patrick Kamau",
                    text = "Escrow is locked. Let me know when you reach Hospital Rd gate.",
                    timestampFormatted = "11:20 AM",
                    isFromCurrentUser = false
                )
            )
        ),
        ChatThread(
            threadId = "th_2",
            jobId = "job_1",
            jobTitle = "House Deep Cleaning & Sanitizing",
            otherPartyId = "emp_1",
            otherPartyName = "Amina Wanjiku",
            otherPartyRole = "Employer",
            lastMessage = "Do you come with your own steam cleaner?",
            lastTimestamp = "Yesterday",
            unreadCount = 0,
            messages = listOf(
                ChatMessage(
                    id = "msg_201",
                    senderId = "user_me",
                    senderName = "Brian Mwangi",
                    text = "Hi Amina, I have done multiple deep cleaning jobs in Kilimani.",
                    timestampFormatted = "Yesterday, 3:00 PM",
                    isFromCurrentUser = true
                ),
                ChatMessage(
                    id = "msg_202",
                    senderId = "emp_1",
                    senderName = "Amina Wanjiku",
                    text = "Do you come with your own steam cleaner?",
                    timestampFormatted = "Yesterday, 3:15 PM",
                    isFromCurrentUser = false
                )
            )
        )
    )
}

package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KaziFirestoreRepository
import com.example.data.KaziRepository
import com.example.data.UpdateManager
import com.example.model.*
import com.example.util.CloudinaryService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppNavTab(val title: String, val iconLabel: String) {
    HOME("Home", "🏠"),
    JOBS("Jobs & Map", "🗺️"),
    POST("Post Job", "➕"),
    MESSAGES("Messages", "💬"),
    PROFILE("Profile", "👤")
}

data class ReviewDialogState(
    val isOpen: Boolean = false,
    val job: Job? = null,
    val rating: Int = 5,
    val selectedTags: Set<String> = emptySet(),
    val comment: String = ""
)

data class EscrowDepositPromptState(
    val isOpen: Boolean = false,
    val job: Job? = null,
    val isFunding: Boolean = false,
    val mpesaPhone: String = "0712345678"
)

data class EscrowCompletionResultState(
    val isOpen: Boolean = false,
    val result: KaziRepository.CompletionPayoutResult? = null
)

class KaziViewModel(
    private val repository: KaziRepository = KaziRepository(),
    private val firestoreRepository: KaziFirestoreRepository? = null
) : ViewModel() {

    // Themes: true for Pitch Black AMOLED, false for Frosted White
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Navigation
    private val _currentTab = MutableStateFlow(AppNavTab.HOME)
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    // Selected active chat thread (null means chat list is shown)
    private val _activeThreadId = MutableStateFlow<String?>(null)
    val activeThreadId: StateFlow<String?> = _activeThreadId.asStateFlow()

    // Chat sender persona (Worker vs Employer for two-user testing and simulation)
    enum class ChatSenderPersona {
        WORKER,
        EMPLOYER
    }

    private val _chatSenderPersona = MutableStateFlow(ChatSenderPersona.WORKER)
    val chatSenderPersona: StateFlow<ChatSenderPersona> = _chatSenderPersona.asStateFlow()

    fun toggleChatSenderPersona() {
        _chatSenderPersona.value = if (_chatSenderPersona.value == ChatSenderPersona.WORKER) {
            ChatSenderPersona.EMPLOYER
        } else {
            ChatSenderPersona.WORKER
        }
    }

    // Selected job for detailed inspection / map routing
    private val _selectedJobForRoute = MutableStateFlow<Job?>(null)
    val selectedJobForRoute: StateFlow<Job?> = _selectedJobForRoute.asStateFlow()

    // Map vs List toggle in Jobs screen
    private val _isMapViewActive = MutableStateFlow(false)
    val isMapViewActive: StateFlow<Boolean> = _isMapViewActive.asStateFlow()

    // Filter & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<JobCategory>>(emptySet())
    val selectedCategories: StateFlow<Set<JobCategory>> = _selectedCategories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<JobCategory?>(null)
    val selectedCategory: StateFlow<JobCategory?> = _selectedCategory.asStateFlow()

    // Firestore streams
    private val _firestoreJobs = MutableStateFlow<List<Job>>(emptyList())
    private val _firestoreWallet = MutableStateFlow<WalletState?>(null)
    private val _customUserProfile = MutableStateFlow<UserProfile?>(null)

    // Data streams from repository & cloud Firestore
    val userProfile: StateFlow<UserProfile> = combine(repository.userProfile, _customUserProfile) { local, custom ->
        custom ?: local
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.userProfile.value)

    val allJobs: StateFlow<List<Job>> = combine(repository.jobs, _firestoreJobs) { local, remote ->
        if (remote.isNotEmpty()) remote else local
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.jobs.value)

    val walletState: StateFlow<WalletState> = combine(repository.walletState, _firestoreWallet) { local, remote ->
        if (remote != null) {
            local.copy(
                availableBalanceKsh = remote.availableBalanceKsh,
                lockedInEscrowKsh = remote.lockedInEscrowKsh,
                totalEarningsKsh = remote.totalEarningsKsh
            )
        } else local
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.walletState.value)

    val reviews: StateFlow<List<Review>> = repository.reviews
    val chatThreads: StateFlow<List<ChatThread>> = repository.chatThreads

    init {
        try {
            syncWithFirestore()
        } catch (e: Throwable) {
            android.util.Log.w("KaziViewModel", "Initial Firestore sync fallback", e)
        }
    }

    fun syncWithFirestore() {
        val user = try {
            Firebase.auth.currentUser
        } catch (e: Throwable) {
            null
        }
        if (user != null) {
            val cur = repository.userProfile.value
            _customUserProfile.value = cur.copy(
                id = user.uid,
                fullName = user.displayName?.takeIf { it.isNotBlank() } ?: cur.fullName,
                phone = user.phoneNumber?.takeIf { it.isNotBlank() } ?: cur.phone
            )
        }

        if (firestoreRepository != null) {
            viewModelScope.launch {
                try {
                    firestoreRepository.seedInitialJobsIfEmpty()
                } catch (e: Exception) {
                    android.util.Log.w("KaziViewModel", "Seed jobs note: ${e.message}")
                }
            }

            viewModelScope.launch {
                try {
                    firestoreRepository.observeJobs()
                        .catch { e -> android.util.Log.w("KaziViewModel", "observeJobs stream note", e) }
                        .collect { jobs ->
                            if (jobs.isNotEmpty()) {
                                _firestoreJobs.value = jobs
                            }
                        }
                } catch (e: Exception) {
                    android.util.Log.w("KaziViewModel", "observeJobs launch failed", e)
                }
            }

            val uid = user?.uid
            if (uid != null) {
                viewModelScope.launch {
                    try {
                        firestoreRepository.observeWallet(uid)
                            .catch { e -> android.util.Log.w("KaziViewModel", "observeWallet stream note", e) }
                            .collect { wallet ->
                                _firestoreWallet.value = wallet
                            }
                    } catch (e: Exception) {
                        android.util.Log.w("KaziViewModel", "observeWallet launch failed", e)
                    }
                }
            }
        }
    }

    // Job counts grouped by category for dynamic filter badges
    val categoryJobCounts: StateFlow<Map<JobCategory, Int>> = allJobs.map { list ->
        list.groupBy { it.category }.mapValues { it.value.size }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        repository.jobs.value.groupBy { it.category }.mapValues { it.value.size }
    )

    // Filtered jobs matching query and active category toggles
    val filteredJobs: StateFlow<List<Job>> = combine(
        allJobs,
        _searchQuery,
        _selectedCategories
    ) { jobsList, query, catSet ->
        jobsList.filter { job ->
            val matchesQuery = query.isBlank() ||
                    job.title.contains(query, ignoreCase = true) ||
                    job.locationName.contains(query, ignoreCase = true) ||
                    job.description.contains(query, ignoreCase = true)
            val matchesCat = catSet.isEmpty() || catSet.contains(job.category)
            matchesQuery && matchesCat
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.jobs.value)

    // Modals / Dialogs
    private val _reviewDialog = MutableStateFlow(ReviewDialogState())
    val reviewDialog: StateFlow<ReviewDialogState> = _reviewDialog.asStateFlow()

    private val _escrowDepositPrompt = MutableStateFlow(EscrowDepositPromptState())
    val escrowDepositPrompt: StateFlow<EscrowDepositPromptState> = _escrowDepositPrompt.asStateFlow()

    private val _escrowCompletionResult = MutableStateFlow(EscrowCompletionResultState())
    val escrowCompletionResult: StateFlow<EscrowCompletionResultState> = _escrowCompletionResult.asStateFlow()

    // Transient Toast notifications
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showToast(msg: String) {
        _toastMessage.value = msg
        viewModelScope.launch {
            delay(2800)
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
        showToast(if (_isDarkTheme.value) "Switched to Pitch Black theme" else "Switched to Frosted White theme")
    }

    fun setTab(tab: AppNavTab) {
        _currentTab.value = tab
        if (tab != AppNavTab.MESSAGES) {
            _activeThreadId.value = null
        }
    }

    fun openChatThread(threadId: String) {
        _activeThreadId.value = threadId
        _currentTab.value = AppNavTab.MESSAGES
        repository.startListeningToThread(threadId)
    }

    fun closeChatThread() {
        _activeThreadId.value = null
    }

    fun selectJobForRoute(job: Job?) {
        _selectedJobForRoute.value = job
        if (job != null) {
            _isMapViewActive.value = true
        }
    }

    fun toggleMapView(active: Boolean) {
        _isMapViewActive.value = active
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    /**
     * Toggles visibility of jobs for a specific category.
     * When toggled ON, jobs of this category are included in results.
     * When toggled OFF, jobs of this category are excluded.
     */
    fun toggleCategory(category: JobCategory) {
        val current = _selectedCategories.value
        val updated = if (current.contains(category)) {
            current - category
        } else {
            current + category
        }
        _selectedCategories.value = updated
        _selectedCategory.value = if (updated.size == 1) updated.first() else null
    }

    /**
     * Isolates a single category or clears it if it was already the only one active.
     */
    fun selectOnlyCategory(category: JobCategory) {
        if (_selectedCategories.value == setOf(category)) {
            clearCategoryFilter()
        } else {
            _selectedCategories.value = setOf(category)
            _selectedCategory.value = category
        }
    }

    /**
     * Clears all category filters, displaying jobs across all categories.
     */
    fun clearCategoryFilter() {
        _selectedCategories.value = emptySet()
        _selectedCategory.value = null
    }

    fun setCategoryFilter(category: JobCategory?) {
        if (category == null) {
            clearCategoryFilter()
        } else {
            toggleCategory(category)
        }
    }

    // Escrow & Application Actions

    fun applyForJob(job: Job) {
        val success = repository.applyForJob(job.id)
        if (success) {
            viewModelScope.launch {
                try {
                    firestoreRepository?.applyForJob(job.id)
                } catch (e: Exception) {
                    android.util.Log.w("KaziViewModel", "Firestore apply note: ${e.message}")
                }
            }
            showToast("Applied for ${job.title}!")
            // Also notify and find thread
            val thread = repository.chatThreads.value.find { it.jobId == job.id }
            if (thread != null) {
                _activeThreadId.value = thread.threadId
                _currentTab.value = AppNavTab.MESSAGES
            }
        } else {
            showToast("Unable to apply (already applied or closed)")
        }
    }

    fun promptEscrowDeposit(job: Job) {
        _escrowDepositPrompt.value = EscrowDepositPromptState(
            isOpen = true,
            job = job,
            isFunding = false,
            mpesaPhone = userProfile.value.phone
        )
    }

    fun dismissEscrowDepositPrompt() {
        _escrowDepositPrompt.value = EscrowDepositPromptState()
    }

    fun confirmEscrowDeposit(jobId: String) {
        holdPaymentInEscrow(jobId)
    }

    /**
     * Holds payment in simulated escrow when a worker accepts or is assigned a job.
     */
    fun holdPaymentInEscrow(jobId: String) {
        viewModelScope.launch {
            _escrowDepositPrompt.value = _escrowDepositPrompt.value.copy(isFunding = true)
            delay(1000) // Simulate M-Pesa STK Push / payment authorization
            val success = repository.holdPaymentInEscrow(jobId)
            try {
                firestoreRepository?.holdPaymentInEscrow(jobId)
            } catch (e: Exception) {
                android.util.Log.w("KaziViewModel", "Firestore hold escrow note: ${e.message}")
            }
            _escrowDepositPrompt.value = EscrowDepositPromptState()
            if (success) {
                showToast("🔒 Payment held in simulated escrow! Job started.")
            }
        }
    }

    fun confirmJobCompletion(job: Job) {
        releaseEscrowFundsToWorker(job, fixedFeePercent = 10)
    }

    /**
     * Logic flow that releases escrow funds to the worker's wallet balance ONLY
     * after the employer confirms completion, deducting the fixed percentage service fee.
     */
    fun releaseEscrowFundsToWorker(job: Job, fixedFeePercent: Int = 10) {
        val result = repository.releaseEscrowFundsToWorker(job.id, fixedFeePercent)
        viewModelScope.launch {
            try {
                firestoreRepository?.releaseEscrowFundsToWorker(job.id, fixedFeePercent)
            } catch (e: Exception) {
                android.util.Log.w("KaziViewModel", "Firestore release escrow note: ${e.message}")
            }
        }
        if (result != null) {
            _escrowCompletionResult.value = EscrowCompletionResultState(
                isOpen = true,
                result = result
            )
            showToast("✅ Completion confirmed! Net KSh ${result.netPayoutKsh} released to wallet.")
            // Open mutual review dialog automatically after closing payout or directly
            openReviewDialog(job)
        }
    }

    fun dismissEscrowCompletionResult() {
        _escrowCompletionResult.value = EscrowCompletionResultState()
    }

    // Two-Way Review Actions

    fun openReviewDialog(job: Job) {
        _reviewDialog.value = ReviewDialogState(
            isOpen = true,
            job = job,
            rating = 5,
            selectedTags = setOf("Punctual", "Quality Work"),
            comment = ""
        )
    }

    fun updateReviewRating(stars: Int) {
        _reviewDialog.value = _reviewDialog.value.copy(rating = stars)
    }

    fun toggleReviewTag(tag: String) {
        val current = _reviewDialog.value.selectedTags.toMutableSet()
        if (current.contains(tag)) {
            current.remove(tag)
        } else {
            current.add(tag)
        }
        _reviewDialog.value = _reviewDialog.value.copy(selectedTags = current)
    }

    fun updateReviewComment(comment: String) {
        _reviewDialog.value = _reviewDialog.value.copy(comment = comment)
    }

    fun submitReview() {
        val state = _reviewDialog.value
        val job = state.job ?: return
        repository.submitReview(
            jobId = job.id,
            rating = state.rating,
            tags = state.selectedTags.toList(),
            comment = state.comment
        )
        viewModelScope.launch {
            try {
                firestoreRepository?.submitReview(
                    jobId = job.id,
                    rating = state.rating,
                    tags = state.selectedTags.toList(),
                    comment = state.comment
                )
            } catch (e: Exception) {
                android.util.Log.w("KaziViewModel", "Firestore review note: ${e.message}")
            }
        }
        _reviewDialog.value = ReviewDialogState()
        showToast("Rating submitted! Thank you.")
    }

    fun dismissReviewDialog() {
        _reviewDialog.value = ReviewDialogState()
    }

    // Chat Messaging
    fun sendMessage(threadId: String, text: String, senderPersona: ChatSenderPersona? = null) {
        val persona = senderPersona ?: _chatSenderPersona.value
        val thread = repository.chatThreads.value.find { it.threadId == threadId }
        val success = if (persona == ChatSenderPersona.EMPLOYER && thread != null) {
            repository.sendMessage(
                threadId = threadId,
                text = text,
                customSenderId = thread.otherPartyId,
                customSenderName = thread.otherPartyName,
                isFromMe = false
            )
        } else {
            repository.sendMessage(
                threadId = threadId,
                text = text,
                isFromMe = true
            )
        }
        if (success) {
            // Trigger conversational reply if sent as worker and no active multi-device employer
            if (persona == ChatSenderPersona.WORKER) {
                viewModelScope.launch {
                    delay(2000)
                    val currentThread = repository.chatThreads.value.find { it.threadId == threadId }
                    if (currentThread != null && currentThread.messages.lastOrNull()?.isFromCurrentUser == true) {
                        val replies = listOf(
                            "Noted! I'm on my way now.",
                            "Thanks for clarifying the site details. See you shortly!",
                            "Escrow payment is securely funded so we are all set.",
                            "Understood, thanks for confirming!"
                        )
                        repository.sendMessage(
                            threadId = threadId,
                            text = replies.random(),
                            customSenderId = currentThread.otherPartyId,
                            customSenderName = currentThread.otherPartyName,
                            isFromMe = false
                        )
                    }
                }
            }
        }
    }

    // Wallet Withdrawals

    fun processWithdrawal(method: String, destination: String, amountKsh: Int): Boolean {
        return when (val res = repository.processWithdrawal(method, destination, amountKsh)) {
            is KaziRepository.WithdrawalResult.Success -> {
                showToast("Sent KSh ${res.amountKsh} to ${res.destination}! Ref: ${res.referenceId}")
                true
            }
            is KaziRepository.WithdrawalResult.Failure -> {
                showToast("Withdrawal failed: ${res.reason}")
                false
            }
        }
    }

    // Job Posting

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
        val job = repository.createJob(
            title, category, locationName, latitude, longitude, payKsh, durationHours, description, photoUrls
        )
        viewModelScope.launch {
            try {
                firestoreRepository?.createJob(
                    title, category, locationName, latitude, longitude, payKsh, durationHours, description, photoUrls
                )
            } catch (e: Exception) {
                android.util.Log.w("KaziViewModel", "Firestore job create note: ${e.message}")
            }
        }
        showToast("Job '${job.title}' posted live!")
        _currentTab.value = AppNavTab.JOBS
        return job
    }

    private val _isAvatarUploading = MutableStateFlow(false)
    val isAvatarUploading: StateFlow<Boolean> = _isAvatarUploading.asStateFlow()

    fun uploadAvatarImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isAvatarUploading.value = true
            showToast("Optimizing & uploading avatar to Cloudinary...")
            val result = CloudinaryService.uploadImage(context, uri, folder = "kazidaily_avatars")
            _isAvatarUploading.value = false
            result.onSuccess { url ->
                repository.updateUserAvatar(url)
                val cur = _customUserProfile.value ?: repository.userProfile.value
                _customUserProfile.value = cur.copy(photoUrl = url)
                firestoreRepository?.updateUserAvatar(url)
                showToast("Avatar updated successfully!")
            }.onFailure { err ->
                showToast("Upload failed: ${err.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun addSkill(skill: String) {
        repository.addSkill(skill)
        showToast("Added skill: $skill")
    }

    // App Version & In-App GitHub Patch Updater
    private val updateManager = UpdateManager()
    val currentVersionCode: Int = 1
    val currentVersionName: String = "1.0.0"

    private val _updateState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val updateState: StateFlow<UpdateDownloadState> = _updateState.asStateFlow()

    fun checkPatchUpdates() {
        viewModelScope.launch {
            _updateState.value = UpdateDownloadState.Checking
            val available = updateManager.checkForPatchUpdates(currentVersionCode)
            if (available != null) {
                _updateState.value = UpdateDownloadState.Available(available)
            } else {
                _updateState.value = UpdateDownloadState.UpToDate(currentVersionName)
            }
        }
    }

    fun checkForUpdates() = checkPatchUpdates()

    fun startDownloadUpdate(context: Context, updateInfo: AppUpdateInfo) {
        viewModelScope.launch {
            val totalBytes = updateInfo.deltaSizeBytes
            _updateState.value = UpdateDownloadState.Downloading(0, 0L, totalBytes)
            try {
                val patchFile = updateManager.downloadPatch(context, updateInfo) { progress ->
                    _updateState.value = UpdateDownloadState.Downloading(
                        progressPercent = progress,
                        downloadedBytes = (totalBytes * progress) / 100,
                        totalBytes = totalBytes
                    )
                }
                _updateState.value = UpdateDownloadState.ReadyToInstall(patchFile.absolutePath, updateInfo)
            } catch (e: Exception) {
                _updateState.value = UpdateDownloadState.Error("Patch download failed: ${e.message}")
            }
        }
    }

    fun installDownloadedApk(context: Context, apkFilePath: String) {
        applyPatchAndRestart(context, apkFilePath)
    }

    fun applyPatchAndRestart(context: Context, patchFilePath: String) {
        val success = updateManager.installPatchAndRestart(context, java.io.File(patchFilePath))
        if (!success) {
            showToast("Failed to launch package installer for patch update")
        } else {
            showToast("Applying patch in place... relaunching KaziDaily")
        }
    }

    fun dismissUpdateDialog() {
        _updateState.value = UpdateDownloadState.Idle
    }

    fun resetData() {
        repository.resetData()
        showToast("Local data reset to defaults.")
        _currentTab.value = AppNavTab.HOME
    }

    // Followed Employers for instant recommendations & upload notifications
    private val _followedEmployerIds = MutableStateFlow<Set<String>>(emptySet())
    val followedEmployerIds: StateFlow<Set<String>> = _followedEmployerIds.asStateFlow()

    fun toggleFollowEmployer(employerId: String, employerName: String) {
        val current = _followedEmployerIds.value.toMutableSet()
        if (current.contains(employerId)) {
            current.remove(employerId)
            showToast("Unfollowed $employerName")
        } else {
            current.add(employerId)
            showToast("🔔 Now following $employerName! You'll receive instant notifications & recommendations when they upload work.")
        }
        _followedEmployerIds.value = current
    }
}

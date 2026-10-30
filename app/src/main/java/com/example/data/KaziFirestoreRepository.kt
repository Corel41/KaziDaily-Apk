package com.example.data

import android.content.Context
import com.example.R
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Cloud Firestore repository managing real-time synchronized jobs,
 * wallet transactions, escrow holds, and job messaging.
 */
class KaziFirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val currentUserName: String
        get() = auth.currentUser?.displayName ?: "Kazi Worker"

    val currentUserEmail: String
        get() = auth.currentUser?.email ?: "worker@kazidaily.co.ke"

    // Real-time observation of all active jobs in Nairobi
    fun observeJobs(): Flow<List<Job>> = flow {
        val path = "jobs"
        emitAll(
            db.collection(path)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FirestoreJob::class.java).map { it.toJob() }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // Initialize seed jobs if database collection is empty
    suspend fun seedInitialJobsIfEmpty() {
        try {
            val snapshot = db.collection("jobs").limit(1).get().await()
            if (snapshot.isEmpty) {
                val seedList = getInitialSeedJobs()
                for (job in seedList) {
                    val docRef = db.collection("jobs").document(job.id)
                    val payload = mapOf(
                        "title" to job.title,
                        "description" to job.description,
                        "category" to job.category.name,
                        "payKsh" to job.payKsh.toLong(),
                        "durationHours" to job.durationHours.toLong(),
                        "locationName" to job.locationName,
                        "latitude" to job.latitude,
                        "longitude" to job.longitude,
                        "employerId" to job.employerId,
                        "employerName" to job.employerName,
                        "employerRating" to job.employerRating.toDouble(),
                        "employerReviewCount" to job.employerReviewCount.toLong(),
                        "status" to job.status.name,
                        "escrowAmountKsh" to job.escrowAmountKsh.toLong(),
                        "platformFeePercent" to job.platformFeePercent.toLong(),
                        "workerReviewed" to false,
                        "employerReviewed" to false,
                        "photoUrls" to job.photoUrls,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "jobs")
        }
    }

    // Create a new Job posting
    suspend fun createJob(
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
        val uid = requireUserId()
        val docRef = db.collection("jobs").document()
        val payload = mapOf(
            "title" to title,
            "description" to description,
            "category" to category.name,
            "payKsh" to payKsh.toLong(),
            "durationHours" to durationHours.toLong(),
            "locationName" to locationName,
            "latitude" to latitude,
            "longitude" to longitude,
            "employerId" to uid,
            "employerName" to currentUserName,
            "employerRating" to 4.9,
            "employerReviewCount" to 14L,
            "status" to JobStatus.OPEN.name,
            "escrowAmountKsh" to 0L,
            "platformFeePercent" to 10L,
            "workerReviewed" to false,
            "employerReviewed" to false,
            "photoUrls" to photoUrls,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return Job(
                id = docRef.id,
                title = title,
                description = description,
                category = category,
                payKsh = payKsh,
                durationHours = durationHours,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude,
                employerId = uid,
                employerName = currentUserName,
                employerRating = 4.9f,
                employerReviewCount = 14,
                status = JobStatus.OPEN,
                photoUrls = photoUrls
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "jobs/${docRef.id}")
            throw e
        }
    }

    suspend fun updateUserAvatar(photoUrl: String) {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        try {
            userRef.set(
                mapOf(
                    "userId" to uid,
                    "displayName" to currentUserName,
                    "email" to currentUserEmail,
                    "photoUrl" to photoUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
        }
    }

    // Apply for a job
    suspend fun applyForJob(jobId: String): Boolean {
        val uid = requireUserId()
        val docRef = db.collection("jobs").document(jobId)
        return try {
            docRef.update(
                mapOf(
                    "status" to JobStatus.APPLIED.name,
                    "assignedWorkerId" to uid,
                    "assignedWorkerName" to currentUserName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "jobs/$jobId")
            false
        }
    }

    // Hold payment in escrow
    suspend fun holdPaymentInEscrow(jobId: String): Boolean {
        requireUserId()
        val docRef = db.collection("jobs").document(jobId)
        return try {
            val snapshot = docRef.get().await()
            val payKsh = snapshot.getLong("payKsh") ?: 2500L
            docRef.update(
                mapOf(
                    "status" to JobStatus.IN_PROGRESS.name,
                    "escrowAmountKsh" to payKsh,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "jobs/$jobId")
            false
        }
    }

data class EscrowPayoutSummary(
    val grossKsh: Int,
    val platformFeeKsh: Int,
    val netPayoutKsh: Int,
    val platformFeePercent: Int
)

    // Employer confirms completion and releases escrow funds
    suspend fun releaseEscrowFundsToWorker(jobId: String, fixedFeePercent: Int = 10): EscrowPayoutSummary? {
        requireUserId()
        val docRef = db.collection("jobs").document(jobId)
        return try {
            val snapshot = docRef.get().await()
            val payKsh = (snapshot.getLong("payKsh") ?: 2500L).toInt()
            val title = snapshot.getString("title") ?: "Gig Work"
            val workerId = snapshot.getString("assignedWorkerId") ?: requireUserId()

            val feeKsh = (payKsh * fixedFeePercent) / 100
            val netPayoutKsh = payKsh - feeKsh

            docRef.update(
                mapOf(
                    "status" to JobStatus.COMPLETED.name,
                    "escrowAmountKsh" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Credit worker's wallet
            creditWorkerWallet(workerId, netPayoutKsh, title, jobId)

            EscrowPayoutSummary(
                grossKsh = payKsh,
                platformFeeKsh = feeKsh,
                netPayoutKsh = netPayoutKsh,
                platformFeePercent = fixedFeePercent
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "jobs/$jobId")
            null
        }
    }

    // Real-time observation of user's private wallet
    fun observeWallet(userId: String): Flow<WalletState> = flow {
        val path = "users/$userId/wallet/summary"
        emitAll(
            db.document(path).snapshots().map { snapshot ->
                val available = snapshot.getLong("availableBalanceKsh") ?: 8450L
                val locked = snapshot.getLong("lockedInEscrowKsh") ?: 0L
                val total = snapshot.getLong("totalEarnedKsh") ?: 36800L
                WalletState(
                    availableBalanceKsh = available.toInt(),
                    lockedInEscrowKsh = locked.toInt(),
                    totalEarningsKsh = total.toInt(),
                    transactions = emptyList()
                )
            }.catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, path)
                }
                emit(
                    WalletState(
                        availableBalanceKsh = 8450,
                        lockedInEscrowKsh = 0,
                        totalEarningsKsh = 36800,
                        transactions = emptyList()
                    )
                )
            }
        )
    }

    private suspend fun creditWorkerWallet(workerId: String, amountKsh: Int, jobTitle: String, jobId: String) {
        val summaryRef = db.document("users/$workerId/wallet/summary")
        val txRef = db.document("users/$workerId/wallet/tx_${System.currentTimeMillis()}")
        try {
            db.runTransaction { tx ->
                val current = tx.get(summaryRef)
                val curBal = current.getLong("availableBalanceKsh") ?: 8450L
                val curTotal = current.getLong("totalEarnedKsh") ?: 36800L
                tx.set(
                    summaryRef,
                    mapOf(
                        "userId" to workerId,
                        "availableBalanceKsh" to (curBal + amountKsh),
                        "lockedInEscrowKsh" to 0L,
                        "totalEarnedKsh" to (curTotal + amountKsh),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to workerId,
                        "type" to "ESCROW_PAYOUT",
                        "amountKsh" to amountKsh.toLong(),
                        "description" to "Payout for '$jobTitle'",
                        "referenceJobId" to jobId,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, summaryRef.path)
        }
    }

    private fun getInitialSeedJobs(): List<Job> {
        return listOf(
            Job(
                id = "job_nairobi_1",
                title = "Deep Home Cleaning & Kitchen Sanitization",
                description = "4-bedroom townhouse in Kilimani. Sweeping, mopping tiles, bathroom scrubbing, window wiping.",
                category = JobCategory.CLEANING,
                payKsh = 2500,
                durationHours = 4,
                locationName = "Kilimani, Argwings Kodhek Rd",
                latitude = -1.2884,
                longitude = 36.7830,
                employerId = "emp_wambui",
                employerName = "Wambui Kamau",
                employerRating = 4.9f,
                employerReviewCount = 14,
                status = JobStatus.OPEN,
                photoUrls = listOf(
                    "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=800&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1527515637462-cff94eecc1ac?w=800&auto=format&fit=crop&q=80"
                )
            ),
            Job(
                id = "job_nairobi_2",
                title = "Plumbing Repair: Leaking Sink & Pipe Valve",
                description = "Kitchen sink valve leaking underneath the basin in Westlands apartment. Basic replacement parts provided.",
                category = JobCategory.REPAIRS,
                payKsh = 3200,
                durationHours = 3,
                locationName = "Westlands, Muthithi Rd",
                latitude = -1.2645,
                longitude = 36.8021,
                employerId = "emp_brian",
                employerName = "Brian Otieno",
                employerRating = 4.8f,
                employerReviewCount = 8,
                status = JobStatus.OPEN,
                photoUrls = listOf(
                    "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=800&auto=format&fit=crop&q=80",
                    "https://images.unsplash.com/photo-1607472586893-edb57bdc0e39?w=800&auto=format&fit=crop&q=80"
                )
            ),
            Job(
                id = "job_nairobi_3",
                title = "Electrical Socket & Ceiling Fan Installation",
                description = "Install 4 double power sockets and 1 ceiling fan in Upper Hill commercial office space.",
                category = JobCategory.ELECTRICAL,
                payKsh = 4000,
                durationHours = 4,
                locationName = "Upper Hill, Hospital Rd",
                latitude = -1.2985,
                longitude = 36.8188,
                employerId = "emp_hassan",
                employerName = "Hassan Noor",
                employerRating = 5.0f,
                employerReviewCount = 22,
                status = JobStatus.OPEN,
                photoUrls = listOf(
                    "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800&auto=format&fit=crop&q=80"
                )
            ),
            Job(
                id = "job_nairobi_4",
                title = "Express Parcel Delivery across CBD",
                description = "Pick up branded stationery boxes from Kencom House and deliver to Riverside office park.",
                category = JobCategory.DELIVERY,
                payKsh = 1800,
                durationHours = 2,
                locationName = "Nairobi CBD, Moi Avenue",
                latitude = -1.2864,
                longitude = 36.8249,
                employerId = "emp_faith",
                employerName = "Faith Nduta",
                employerRating = 4.7f,
                employerReviewCount = 19,
                status = JobStatus.OPEN,
                photoUrls = listOf(
                    "https://images.unsplash.com/photo-1549465220-1a8b9238cd48?w=800&auto=format&fit=crop&q=80"
                )
            ),
            Job(
                id = "job_nairobi_5",
                title = "Garden Compound Clearing & Hedge Trimming",
                description = "Lawn mowing, flower bed edging, pruning bougainvillea hedge on a half-acre Karen compound.",
                category = JobCategory.GARDENING,
                payKsh = 3500,
                durationHours = 5,
                locationName = "Karen, Ngong Rd",
                latitude = -1.3204,
                longitude = 36.7058,
                employerId = "emp_peter",
                employerName = "Peter Mwangi",
                employerRating = 4.9f,
                employerReviewCount = 31,
                status = JobStatus.OPEN,
                photoUrls = listOf(
                    "https://images.unsplash.com/photo-1592417817098-8f3d6910985b?w=800&auto=format&fit=crop&q=80"
                )
            )
        )
    }

    // Submit mutual review
    suspend fun submitReview(jobId: String, rating: Int, tags: List<String>, comment: String) {
        try {
            val docRef = db.collection("jobs").document(jobId)
            docRef.update(
                mapOf(
                    "workerReviewed" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "jobs/$jobId")
        }
    }
}

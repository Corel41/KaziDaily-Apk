package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class KaziSupabaseRepository(
    private val client: SupabaseClient = SupabaseClient
) {
    private val api = client.apiService

    val currentUserId: String?
        get() = client.currentUser.value?.id

    val currentUserName: String
        get() = client.currentUser.value?.fullName ?: "Kazi Worker"

    val currentUserEmail: String
        get() = client.currentUser.value?.email ?: "worker@kazidaily.co.ke"

    fun observeJobs(): Flow<List<Job>> = flow {
        if (!client.isConfigured) {
            emit(emptyList())
            return@flow
        }
        try {
            val jobs = api.getJobs()
            emit(jobs.map { it.toJob() })
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch jobs from Supabase", e)
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun seedInitialJobsIfEmpty() = withContext(Dispatchers.IO) {
        if (!client.isConfigured) return@withContext
        try {
            val current = api.getJobs()
            if (current.isEmpty()) {
                val seedJobs = getInitialSeedJobs()
                for (job in seedJobs) {
                    try {
                        val dto = SupabaseJob.fromJob(job)
                        api.insertJob(dto)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error seeding job ${job.id}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "seedInitialJobsIfEmpty Supabase check notice: ${e.message}")
        }
    }

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
    ): Job? = withContext(Dispatchers.IO) {
        val user = client.currentUser.value
        val employerId = user?.id ?: "user_me"
        val employerName = user?.fullName ?: "Brian Mwangi"
        val jobId = "job_" + UUID.randomUUID().toString().take(8)

        val job = Job(
            id = jobId,
            title = title,
            description = description.ifBlank { "Immediate daily task in $locationName. Safety gear and requirements provided." },
            category = category,
            payKsh = payKsh,
            durationHours = durationHours,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            employerId = employerId,
            employerName = employerName,
            employerRating = 4.9f,
            employerReviewCount = 12,
            status = JobStatus.OPEN,
            photoUrls = photoUrls,
            datePosted = "Just now"
        )

        if (client.isConfigured) {
            try {
                val dto = SupabaseJob.fromJob(job)
                api.insertJob(dto)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create job in Supabase", e)
            }
        }
        job
    }

    suspend fun applyForJob(jobId: String, workerId: String, workerName: String): Boolean = withContext(Dispatchers.IO) {
        if (!client.isConfigured) return@withContext true
        try {
            val json = JSONObject().apply {
                put("status", "APPLIED")
                put("assigned_worker_id", workerId)
                put("assigned_worker_name", workerName)
            }.toString()
            val body = json.toRequestBody("application/json".toMediaType())
            val res = api.updateJob("eq.$jobId", body)
            res.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "Supabase applyForJob error", e)
            false
        }
    }

    suspend fun holdPaymentInEscrow(jobId: String, amountKsh: Int): Boolean = withContext(Dispatchers.IO) {
        if (!client.isConfigured) return@withContext true
        try {
            val json = JSONObject().apply {
                put("status", "IN_PROGRESS")
                put("escrow_amount_ksh", amountKsh)
            }.toString()
            val body = json.toRequestBody("application/json".toMediaType())
            api.updateJob("eq.$jobId", body)

            // Record transaction
            val userId = currentUserId ?: "user_me"
            val tx = SupabaseWalletTransaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "Escrow Deposit",
                subtitle = "Funds locked in Supabase escrow until completion",
                amountKsh = amountKsh.toLong(),
                dateFormatted = "Just now",
                type = TransactionType.ESCROW_DEPOSIT.name,
                isPositive = false
            )
            api.insertWalletTransaction(tx)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Supabase holdPaymentInEscrow error", e)
            false
        }
    }

    suspend fun releaseEscrowFundsToWorker(
        jobId: String,
        grossKsh: Int,
        feePercent: Int = 10,
        jobTitle: String = "Gig Task"
    ): Boolean = withContext(Dispatchers.IO) {
        if (!client.isConfigured) return@withContext true
        try {
            val feeKsh = (grossKsh * feePercent) / 100
            val netPayout = grossKsh - feeKsh

            val json = JSONObject().apply {
                put("status", "COMPLETED")
                put("escrow_amount_ksh", 0)
            }.toString()
            val body = json.toRequestBody("application/json".toMediaType())
            api.updateJob("eq.$jobId", body)

            val userId = currentUserId ?: "user_me"
            val tx = SupabaseWalletTransaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "Escrow Release: $jobTitle",
                subtitle = "Gross KSh $grossKsh minus $feePercent% fee (KSh $feeKsh)",
                amountKsh = netPayout.toLong(),
                dateFormatted = "Just now",
                type = TransactionType.ESCROW_RELEASE.name,
                isPositive = true
            )
            api.insertWalletTransaction(tx)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Supabase releaseEscrowFundsToWorker error", e)
            false
        }
    }

    suspend fun submitReview(
        jobId: String,
        rating: Int,
        tags: List<String>,
        comment: String,
        targetUserId: String = "employer_1"
    ): Boolean = withContext(Dispatchers.IO) {
        if (!client.isConfigured) return@withContext true
        try {
            val userId = currentUserId ?: "user_me"
            val userName = currentUserName
            val reviewDto = SupabaseReview(
                id = UUID.randomUUID().toString(),
                jobId = jobId,
                jobTitle = "Kazi Gig",
                reviewerId = userId,
                reviewerName = userName,
                targetUserId = targetUserId,
                rating = rating.toLong(),
                tags = tags,
                comment = comment,
                dateFormatted = "Just now"
            )
            api.insertReview(reviewDto)

            val json = JSONObject().apply {
                put("worker_reviewed", true)
            }.toString()
            api.updateJob("eq.$jobId", json.toRequestBody("application/json".toMediaType()))
            true
        } catch (e: Exception) {
            Log.w(TAG, "Supabase submitReview error", e)
            false
        }
    }

    fun observeWallet(userId: String): Flow<WalletState?> = flow {
        if (!client.isConfigured) {
            emit(null)
            return@flow
        }
        try {
            val transactionsDto = api.getWalletTransactions("eq.$userId")
            val transactions = transactionsDto.map { it.toWalletTransaction() }
            val totalInflow = transactions.filter { it.isPositive }.sumOf { it.amountKsh }
            val totalOutflow = transactions.filter { !it.isPositive }.sumOf { it.amountKsh }
            val balance = Math.max(0, totalInflow - totalOutflow)
            emit(
                WalletState(
                    availableBalanceKsh = balance,
                    lockedInEscrowKsh = 0,
                    totalEarningsKsh = totalInflow,
                    transactions = transactions
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "observeWallet Supabase error", e)
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    private fun getInitialSeedJobs(): List<Job> {
        return listOf(
            Job(
                id = "job_1",
                title = "Emergency Electrical Breaker Repair",
                description = "Frequent tripping of main circuit breaker in residential apartment. Diagnose wiring, replace faulty 32A MCB, and ensure grounding is intact.",
                category = JobCategory.ELECTRICAL,
                payKsh = 2500,
                durationHours = 3,
                locationName = "Kilimani, Nairobi",
                latitude = -1.2921,
                longitude = 36.7842,
                employerId = "emp_kilimani_1",
                employerName = "Wanjiku Kamau",
                employerRating = 4.9f,
                employerReviewCount = 18,
                status = JobStatus.OPEN,
                escrowAmountKsh = 2500,
                platformFeePercent = 10,
                datePosted = "2 hours ago"
            ),
            Job(
                id = "job_2",
                title = "3-Bedroom Apartment Deep Cleaning",
                description = "Move-in deep scrubbing of tile floors, kitchen degreasing, bathroom descaling, balcony wash, and window polishing.",
                category = JobCategory.CLEANING,
                payKsh = 3800,
                durationHours = 5,
                locationName = "Westlands, Nairobi",
                latitude = -1.2676,
                longitude = 36.8121,
                employerId = "emp_westlands_2",
                employerName = "David Ochieng",
                employerRating = 4.8f,
                employerReviewCount = 31,
                status = JobStatus.OPEN,
                escrowAmountKsh = 3800,
                platformFeePercent = 10,
                datePosted = "4 hours ago"
            ),
            Job(
                id = "job_3",
                title = "Compound Hedge Trimming & Lawn Mowing",
                description = "Overgrown perimeter hedge requires neat shaping along 45-meter fence. Lawn clearing, edging, and bagging of garden waste.",
                category = JobCategory.GARDENING,
                payKsh = 1800,
                durationHours = 4,
                locationName = "Lavington, Nairobi",
                latitude = -1.2789,
                longitude = 36.7698,
                employerId = "emp_lavington_3",
                employerName = "Grace Mutua",
                employerRating = 5.0f,
                employerReviewCount = 9,
                status = JobStatus.OPEN,
                escrowAmountKsh = 1800,
                platformFeePercent = 10,
                datePosted = "Just now"
            ),
            Job(
                id = "job_4",
                title = "Express Document Delivery to Upper Hill",
                description = "Urgent legal contract pick-up from CBD (City Hall Way) and swift delivery to Britam Tower reception in Upper Hill. Time sensitive.",
                category = JobCategory.DELIVERY,
                payKsh = 950,
                durationHours = 1,
                locationName = "Upper Hill, Nairobi",
                latitude = -1.2988,
                longitude = 36.8155,
                employerId = "emp_cbd_4",
                employerName = "Kevin Kiprono",
                employerRating = 4.7f,
                employerReviewCount = 42,
                status = JobStatus.OPEN,
                escrowAmountKsh = 950,
                platformFeePercent = 10,
                datePosted = "30 mins ago"
            )
        )
    }

    companion object {
        private const val TAG = "KaziSupabaseRepository"
    }
}

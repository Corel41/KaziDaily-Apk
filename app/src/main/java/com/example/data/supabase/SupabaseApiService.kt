package com.example.data.supabase

import com.example.model.*
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SupabaseApiService {

    // MARK: - Jobs
    @GET("rest/v1/jobs")
    suspend fun getJobs(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): List<SupabaseJob>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/jobs")
    suspend fun insertJob(
        @Body job: SupabaseJob
    ): List<SupabaseJob>

    @PATCH("rest/v1/jobs")
    suspend fun updateJob(
        @Query("id") idFilter: String,
        @Body updates: RequestBody
    ): Response<ResponseBody>

    // MARK: - Profiles
    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Query("id") idFilter: String
    ): List<SupabaseProfile>

    @Headers("Prefer: resolution=merge-duplicates")
    @POST("rest/v1/profiles")
    suspend fun upsertProfile(
        @Body profile: SupabaseProfile
    ): Response<ResponseBody>

    // MARK: - Wallet Transactions
    @GET("rest/v1/wallet_transactions")
    suspend fun getWalletTransactions(
        @Query("user_id") userFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): List<SupabaseWalletTransaction>

    @POST("rest/v1/wallet_transactions")
    suspend fun insertWalletTransaction(
        @Body tx: SupabaseWalletTransaction
    ): Response<ResponseBody>

    // MARK: - Reviews
    @GET("rest/v1/reviews")
    suspend fun getReviews(
        @Query("order") order: String = "created_at.desc"
    ): List<SupabaseReview>

    @POST("rest/v1/reviews")
    suspend fun insertReview(
        @Body review: SupabaseReview
    ): Response<ResponseBody>

    // MARK: - Chat Threads
    @GET("rest/v1/chat_threads")
    suspend fun getChatThreads(
        @Query("select") select: String = "*",
        @Query("order") order: String = "updated_at.desc"
    ): List<SupabaseThread>

    @Headers("Prefer: resolution=merge-duplicates")
    @POST("rest/v1/chat_threads")
    suspend fun upsertThread(
        @Body thread: SupabaseThread
    ): Response<ResponseBody>

    // MARK: - Chat Messages
    @GET("rest/v1/chat_messages")
    suspend fun getChatMessages(
        @Query("thread_id") threadFilter: String,
        @Query("order") order: String = "created_at.asc"
    ): List<SupabaseChatMessage>

    @POST("rest/v1/chat_messages")
    suspend fun insertChatMessage(
        @Body message: SupabaseChatMessage
    ): Response<ResponseBody>

    // MARK: - Supabase Auth
    @POST("auth/v1/token?grant_type=id_token")
    suspend fun signInWithGoogleIdToken(
        @Body request: SupabaseIdTokenRequest
    ): SupabaseAuthResponse
}

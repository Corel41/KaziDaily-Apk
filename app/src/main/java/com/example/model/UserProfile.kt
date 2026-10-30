package com.example.model

data class UserProfile(
    val id: String,
    val fullName: String,
    val phone: String,
    val roleTitle: String,
    val bio: String,
    val skills: List<String>,
    val experienceYears: Int,
    val averageRating: Float,
    val totalReviews: Int,
    val jobsCompletedAsWorker: Int,
    val jobsPostedAsEmployer: Int,
    val isPhoneVerified: Boolean = true,
    val isIdVerified: Boolean = true,
    val location: String = "Nairobi, Kenya",
    val photoUrl: String? = null
)

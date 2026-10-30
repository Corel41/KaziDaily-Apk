package com.example.model

data class Review(
    val id: String,
    val jobId: String,
    val jobTitle: String,
    val reviewerId: String,
    val reviewerName: String,
    val reviewerRole: String, // "Employer" or "Worker"
    val revieweeId: String,
    val revieweeName: String,
    val rating: Int, // 1 to 5
    val tags: List<String>,
    val comment: String,
    val timestampFormatted: String = "Today"
)

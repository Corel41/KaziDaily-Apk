package com.example.model

enum class JobCategory(val displayName: String, val iconName: String) {
    CLEANING("Cleaning", "🧹"),
    DELIVERY("Delivery", "📦"),
    MOVING("Moving", "🚚"),
    GARDENING("Gardening", "🌿"),
    REPAIRS("Repairs", "🔧"),
    ELECTRICAL("Electrical", "⚡"),
    PAINTING("Painting", "🎨"),
    CATERING("Catering", "🍳"),
    OTHER("Other", "💼")
}

enum class JobStatus {
    OPEN,                   // Available for workers to apply
    APPLIED,                // Current user has submitted an application
    ESCROW_PENDING,         // Employer accepted worker, waiting for escrow deposit
    IN_PROGRESS,            // Escrow funded and locked, job being executed
    COMPLETED,              // Employer confirmed completion, funds released
    CANCELLED
}

data class Job(
    val id: String,
    val title: String,
    val description: String,
    val category: JobCategory,
    val payKsh: Int,
    val durationHours: Int,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val employerId: String,
    val employerName: String,
    val employerRating: Float,
    val employerReviewCount: Int,
    val status: JobStatus = JobStatus.OPEN,
    val datePosted: String = "Today",
    val assignedWorkerId: String? = null,
    val assignedWorkerName: String? = null,
    val escrowAmountKsh: Int = 0,
    val platformFeePercent: Int = 10,
    val workerReviewed: Boolean = false,
    val employerReviewed: Boolean = false,
    val photoUrls: List<String> = emptyList()
) {
    val platformFeeKsh: Int
        get() = (payKsh * platformFeePercent) / 100

    val netWorkerPayoutKsh: Int
        get() = payKsh - platformFeeKsh
}

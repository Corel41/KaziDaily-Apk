package com.example

import com.example.data.supabase.SupabaseClient
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class SupabaseIntegrationTest {

    @Test
    fun testSupabaseJobDtoConversion() {
        val originalJob = Job(
            id = "job_test_101",
            title = "Plumbing Leak Fix in Karen",
            description = "Fix burst copper pipe under sink",
            category = JobCategory.REPAIRS,
            payKsh = 3500,
            durationHours = 2,
            locationName = "Karen, Nairobi",
            latitude = -1.3197,
            longitude = 36.7065,
            employerId = "emp_karen_1",
            employerName = "Sarah Wambui",
            employerRating = 4.95f,
            employerReviewCount = 24,
            status = JobStatus.IN_PROGRESS,
            escrowAmountKsh = 3500,
            platformFeePercent = 10,
            assignedWorkerId = "user_me",
            assignedWorkerName = "Brian Mwangi",
            workerReviewed = true,
            employerReviewed = false,
            photoUrls = listOf("https://res.cloudinary.com/demo/image/upload/sample.jpg")
        )

        val dto = SupabaseJob.fromJob(originalJob)
        assertEquals("job_test_101", dto.id)
        assertEquals("Plumbing Leak Fix in Karen", dto.title)
        assertEquals("REPAIRS", dto.category)
        assertEquals(3500L, dto.payKsh)
        assertEquals(3500L, dto.escrowAmountKsh)
        assertEquals(10L, dto.platformFeePercent)
        assertEquals("IN_PROGRESS", dto.status)
        assertEquals("user_me", dto.assignedWorkerId)
        assertTrue(dto.workerReviewed)

        val convertedBack = dto.toJob()
        assertEquals(originalJob.id, convertedBack.id)
        assertEquals(originalJob.title, convertedBack.title)
        assertEquals(originalJob.category, convertedBack.category)
        assertEquals(originalJob.payKsh, convertedBack.payKsh)
        assertEquals(originalJob.escrowAmountKsh, convertedBack.escrowAmountKsh)
        assertEquals(originalJob.status, convertedBack.status)
        assertEquals(originalJob.assignedWorkerId, convertedBack.assignedWorkerId)
    }

    @Test
    fun testSupabaseProfileDtoConversion() {
        val profile = UserProfile(
            id = "worker_999",
            fullName = "John Kimani",
            phone = "+254 711 222 333",
            roleTitle = "Senior Electrician",
            bio = "Certified domestic and industrial electrical specialist.",
            skills = listOf("Wiring", "Solar Installation", "Generators"),
            experienceYears = 7,
            averageRating = 4.92f,
            totalReviews = 45,
            jobsCompletedAsWorker = 43,
            jobsPostedAsEmployer = 2,
            isPhoneVerified = true,
            isIdVerified = true,
            location = "Kilimani, Nairobi"
        )

        val dto = SupabaseProfile.fromUserProfile(profile)
        assertEquals("worker_999", dto.id)
        assertEquals("John Kimani", dto.fullName)
        assertEquals(7, dto.experienceYears)
        assertEquals(45, dto.totalReviews)

        val converted = dto.toUserProfile()
        assertEquals(profile.id, converted.id)
        assertEquals(profile.fullName, converted.fullName)
        assertEquals(profile.roleTitle, converted.roleTitle)
        assertEquals(profile.skills, converted.skills)
    }

    @Test
    fun testSupabaseWalletTransactionConversion() {
        val dto = SupabaseWalletTransaction(
            id = "tx_888",
            userId = "user_me",
            title = "Escrow Payout: Breaker Repair",
            subtitle = "Gross KSh 2500 minus 10% platform fee",
            amountKsh = 2250L,
            dateFormatted = "Today at 2:30 PM",
            type = "ESCROW_RELEASE",
            isPositive = true
        )

        val tx = dto.toWalletTransaction()
        assertEquals("tx_888", tx.id)
        assertEquals(2250, tx.amountKsh)
        assertEquals(TransactionType.ESCROW_RELEASE, tx.type)
        assertTrue(tx.isPositive)
    }

    @Test
    fun testSupabaseReviewDtoConversion() {
        val dto = SupabaseReview(
            id = "rev_555",
            jobId = "job_1",
            jobTitle = "Emergency Electrical Breaker Repair",
            reviewerId = "emp_1",
            reviewerName = "Wanjiku Kamau",
            targetUserId = "user_me",
            rating = 5L,
            tags = listOf("Punctual", "Quality Work"),
            comment = "Arrived on time and resolved the circuit issue safely.",
            dateFormatted = "Yesterday"
        )

        val review = dto.toReview()
        assertEquals("rev_555", review.id)
        assertEquals(5, review.rating)
        assertEquals(2, review.tags.size)
        assertTrue(review.tags.contains("Punctual"))
        assertEquals("Arrived on time and resolved the circuit issue safely.", review.comment)
    }

    @Test
    fun testSupabaseChatThreadAndMessageConversion() {
        val originalThread = ChatThread(
            threadId = "th_777",
            jobId = "job_1",
            jobTitle = "Emergency Electrical Breaker Repair",
            otherPartyId = "emp_kilimani_1",
            otherPartyName = "Wanjiku Kamau",
            otherPartyRole = "Employer",
            lastMessage = "I am on site now.",
            lastTimestamp = "5m ago",
            unreadCount = 1,
            messages = listOf(
                ChatMessage(
                    id = "msg_1",
                    senderId = "emp_kilimani_1",
                    senderName = "Wanjiku Kamau",
                    text = "Hello, are you available?",
                    timestampFormatted = "10m ago",
                    isFromCurrentUser = false
                )
            )
        )

        val threadDto = SupabaseThread.fromChatThread(originalThread, myId = "user_me")
        assertEquals("th_777", threadDto.id)
        assertEquals("job_1", threadDto.jobId)
        assertTrue(threadDto.participantIds!!.contains("user_me"))
        assertTrue(threadDto.participantIds!!.contains("emp_kilimani_1"))

        val msgDto = SupabaseChatMessage.fromChatMessage("th_777", originalThread.messages.first())
        val convertedMsg = msgDto.toChatMessage(currentUserId = "user_me")
        assertEquals("msg_1", convertedMsg.id)
        assertEquals("Hello, are you available?", convertedMsg.text)
        assertFalse(convertedMsg.isFromCurrentUser)
    }

    @Test
    fun testSupabaseClientConfigurationHandling() {
        assertNotNull(SupabaseClient.baseUrl)
        assertNotNull(SupabaseClient.anonKey)
        // Verify apiService initializes without crashing
        assertNotNull(SupabaseClient.apiService)
    }
}

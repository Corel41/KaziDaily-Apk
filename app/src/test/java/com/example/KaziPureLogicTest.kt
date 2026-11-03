package com.example

import com.example.data.KaziRepository
import com.example.model.JobCategory
import com.example.model.JobStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class KaziPureLogicTest {

    @Test
    fun `escrow hold on job acceptance and release on employer completion with fixed service fee`() {
        val repo = KaziRepository()
        val job = repo.jobs.value.first { it.id == "job_1" }
        val initialWalletBalance = repo.walletState.value.availableBalanceKsh
        val initialEscrowLocked = repo.walletState.value.lockedInEscrowKsh

        // 1. Worker accepts/applies for job
        val applied = repo.applyForJob(job.id)
        assertTrue(applied)

        // 2. Payment is held in simulated escrow when job is accepted/undertaken
        val held = repo.holdPaymentInEscrow(job.id)
        assertTrue(held)
        assertEquals(JobStatus.IN_PROGRESS, repo.jobs.value.first { it.id == job.id }.status)
        assertEquals(initialEscrowLocked + job.payKsh, repo.walletState.value.lockedInEscrowKsh)

        // 3. Employer confirms completion -> funds released to worker's wallet with fixed 10% fee deducted
        val payout = repo.releaseEscrowFundsToWorker(job.id, fixedFeePercent = 10)
        assertNotNull(payout)
        assertEquals(2500, payout!!.grossKsh)
        assertEquals(250, payout.platformFeeKsh) // 10% fixed fee
        assertEquals(2250, payout.netPayoutKsh) // 90% net to worker

        // 4. Verify wallet balance increased by net payout
        assertEquals(initialWalletBalance + 2250, repo.walletState.value.availableBalanceKsh)
        assertEquals(JobStatus.COMPLETED, repo.jobs.value.first { it.id == job.id }.status)
    }

    @Test
    fun `withdrawal validation logic`() {
        val repo = KaziRepository()
        // Minimum withdrawal check
        val failMin = repo.processWithdrawal("M-Pesa", "0712345678", 50)
        assertTrue(failMin is KaziRepository.WithdrawalResult.Failure)

        // Exceed balance check
        val failBalance = repo.processWithdrawal("M-Pesa", "0712345678", 999999)
        assertTrue(failBalance is KaziRepository.WithdrawalResult.Failure)

        // Valid withdrawal
        val initialBalance = repo.walletState.value.availableBalanceKsh
        val success = repo.processWithdrawal("M-Pesa", "0712345678", 1000)
        assertTrue(success is KaziRepository.WithdrawalResult.Success)
        assertEquals(initialBalance - 1000, repo.walletState.value.availableBalanceKsh)
    }

    @Test
    fun `review submission recomputes rating`() {
        val repo = KaziRepository()
        val initialReviews = repo.userProfile.value.totalReviews
        val ok = repo.submitReview("job_1", 5, listOf("Punctual", "Quality Work"), "Great job!")
        assertTrue(ok)
        assertEquals(initialReviews + 1, repo.userProfile.value.totalReviews)
    }

    @Test
    fun `in app update checker identifies new release`() = kotlinx.coroutines.test.runTest(kotlinx.coroutines.test.UnconfinedTestDispatcher()) {
        val updateManager = com.example.data.UpdateManager()
        val update = updateManager.checkForUpdates(currentVersionCode = 1)
        assertNotNull(update)
        assertEquals(2, update!!.latestVersionCode)
        assertTrue(update.latestVersionName.startsWith("1.2.0"))
        assertTrue(update.isPatch)
        assertTrue(update.isUpdateAvailable(currentVersionCode = 1))
        assertFalse(update.isUpdateAvailable(currentVersionCode = 2))
    }

    @Test
    fun `category filter system toggles visibility and narrows down jobs`() {
        val repo = KaziRepository()
        val viewModel = com.example.viewmodel.KaziViewModel(repo)

        assertTrue(viewModel.selectedCategories.value.isEmpty())

        // 1. Toggle CLEANING category filter
        viewModel.toggleCategory(JobCategory.CLEANING)
        assertTrue(viewModel.selectedCategories.value.contains(JobCategory.CLEANING))
        assertEquals(JobCategory.CLEANING, viewModel.selectedCategory.value)

        // 2. Toggle DELIVERY in addition to CLEANING (multi-category toggle)
        viewModel.toggleCategory(JobCategory.DELIVERY)
        assertTrue(viewModel.selectedCategories.value.contains(JobCategory.CLEANING))
        assertTrue(viewModel.selectedCategories.value.contains(JobCategory.DELIVERY))

        // 3. Toggle CLEANING OFF -> Only DELIVERY remains
        viewModel.toggleCategory(JobCategory.CLEANING)
        assertFalse(viewModel.selectedCategories.value.contains(JobCategory.CLEANING))
        assertTrue(viewModel.selectedCategories.value.contains(JobCategory.DELIVERY))

        // 4. Clear all filters -> all cleared
        viewModel.clearCategoryFilter()
        assertTrue(viewModel.selectedCategories.value.isEmpty())
    }

    @Test
    fun `category job counts accurately tally available inventory`() = kotlinx.coroutines.test.runTest(kotlinx.coroutines.test.UnconfinedTestDispatcher()) {
        val repo = KaziRepository()
        val viewModel = com.example.viewmodel.KaziViewModel(repo)

        val counts = viewModel.categoryJobCounts.value
        val allJobs = repo.jobs.value

        val cleaningCount = allJobs.count { it.category == JobCategory.CLEANING }
        val deliveryCount = allJobs.count { it.category == JobCategory.DELIVERY }

        assertEquals(cleaningCount, counts[JobCategory.CLEANING] ?: 0)
        assertEquals(deliveryCount, counts[JobCategory.DELIVERY] ?: 0)
    }

    @Test
    fun `messages section lists active job conversations and records messages between worker and employer`() {
        val repo = KaziRepository()
        val viewModel = com.example.viewmodel.KaziViewModel(repo)

        val threads = viewModel.chatThreads.value
        assertTrue("Active job-based conversations should not be empty", threads.isNotEmpty())

        val targetThread = threads.first()
        viewModel.openChatThread(targetThread.threadId)
        assertEquals(com.example.viewmodel.AppNavTab.MESSAGES, viewModel.currentTab.value)
        assertEquals(targetThread.threadId, viewModel.activeThreadId.value)

        // 1. Send message as Worker
        val initialMsgCount = repo.chatThreads.value.first { it.threadId == targetThread.threadId }.messages.size
        viewModel.sendMessage(targetThread.threadId, "Hello, confirming I am arriving with the gear.")

        val afterWorkerMsg = repo.chatThreads.value.first { it.threadId == targetThread.threadId }
        assertEquals(initialMsgCount + 1, afterWorkerMsg.messages.size)
        val lastWorkerMsg = afterWorkerMsg.messages.last()
        assertTrue(lastWorkerMsg.isFromCurrentUser)
        assertEquals("Hello, confirming I am arriving with the gear.", lastWorkerMsg.text)

        // 2. Toggle to Employer persona and send message
        viewModel.toggleChatSenderPersona()
        assertEquals(com.example.viewmodel.KaziViewModel.ChatSenderPersona.EMPLOYER, viewModel.chatSenderPersona.value)

        viewModel.sendMessage(targetThread.threadId, "Gate security has your name on the visitor list.")
        val afterEmployerMsg = repo.chatThreads.value.first { it.threadId == targetThread.threadId }
        val lastEmployerMsg = afterEmployerMsg.messages.last()
        assertFalse(lastEmployerMsg.isFromCurrentUser)
        assertEquals("Gate security has your name on the visitor list.", lastEmployerMsg.text)
    }

    @Test
    fun `google map route calculator computes distance and transit times for nairobi locations`() {
        val kilimani = com.example.model.NairobiLocations.PRESET_LOCATIONS.first { it.name == "Kilimani" }
        val route = com.example.model.NairobiLocations.calculateRoute(
            com.example.model.NairobiLocations.CURRENT_USER_LOC.latitude,
            com.example.model.NairobiLocations.CURRENT_USER_LOC.longitude,
            kilimani.latitude,
            kilimani.longitude,
            kilimani.name
        )

        assertTrue(route.distanceKm > 0.0)
        assertTrue(route.bodaBodaMinutes > 0)
        assertTrue(route.drivingMinutes > 0)
        assertTrue(route.walkingMinutes > 0)
        assertTrue(route.turnDirections.isNotEmpty())
    }

    @Test
    fun `guest user attempting to create job or apply triggers login bottom sheet`() {
        val repo = KaziRepository()
        val viewModel = com.example.viewmodel.KaziViewModel(repo)
        assertFalse(viewModel.isLoggedIn.value)

        val job = repo.jobs.value.first()
        viewModel.applyForJob(job)
        assertTrue(viewModel.showLoginBottomSheet.value)

        viewModel.dismissLoginBottomSheet()
        assertFalse(viewModel.showLoginBottomSheet.value)

        viewModel.createJob(
            title = "Office IT Setup",
            category = JobCategory.ELECTRICAL,
            locationName = "Riverside",
            latitude = -1.2750,
            longitude = 36.8000,
            payKsh = 3500,
            durationHours = 4,
            description = "Setup monitor arms"
        )
        assertTrue(viewModel.showLoginBottomSheet.value)
    }

    @Test
    fun `maps config helper gracefully detects missing or placeholder api key for zero-key mode`() {
        val isConfigured = com.example.util.MapsConfigHelper.isMapsApiKeyConfigured
        assertFalse("Placeholder key should not be treated as a live production key", isConfigured)
    }

    @Test
    fun `job details route calculation computes commute and step by step directions for job site`() {
        val repo = KaziRepository()
        val job = repo.jobs.value.first { it.id == "job_1" }
        val route = com.example.model.NairobiLocations.calculateRoute(
            com.example.model.NairobiLocations.CURRENT_USER_LOC.latitude,
            com.example.model.NairobiLocations.CURRENT_USER_LOC.longitude,
            job.latitude,
            job.longitude,
            job.locationName
        )
        assertTrue("Distance should be greater than 0", route.distanceKm > 0.0)
        assertTrue("Boda boda time should be positive", route.bodaBodaMinutes > 0)
        assertTrue("Driving time should be positive", route.drivingMinutes > 0)
        assertTrue("Walking time should be positive", route.walkingMinutes > 0)
        assertTrue("Driving time should be less than walking time", route.drivingMinutes < route.walkingMinutes)
        assertEquals(4, route.turnDirections.size)
        assertTrue(route.turnDirections.last().contains(job.locationName))
    }
}

package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Job
import com.example.model.JobStatus
import com.example.model.WalletState
import com.example.ui.components.EscrowBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.theme.LocalGlassTokens
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.KaziViewModel

@Composable
fun HomeScreen(
    viewModel: KaziViewModel,
    walletState: WalletState,
    recentJobs: List<Job>,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    var inspectingJob by remember { mutableStateOf<Job?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Welcome Header
        item {
            Column {
                Text(
                    text = "Welcome back 👋",
                    fontSize = 14.sp,
                    color = tokens.textMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Find work. Earn today.",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tokens.textPrimary
                )
            }
        }

        // Wallet & Escrow Balance Glass Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_wallet_card"),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Available Balance",
                            fontSize = 12.sp,
                            color = tokens.textMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "KSh ${walletState.availableBalanceKsh}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tokens.mintAccent
                        )
                    }

                    if (walletState.lockedInEscrowKsh > 0) {
                        EscrowBadge(amountKsh = walletState.lockedInEscrowKsh)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = "Withdraw Funds",
                        onClick = { viewModel.setTab(AppNavTab.PROFILE) },
                        isPrimary = true,
                        modifier = Modifier.weight(1.2f),
                        testTag = "withdraw_button"
                    )
                    GlassButton(
                        text = "View Ledger",
                        onClick = { viewModel.setTab(AppNavTab.PROFILE) },
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions Grid
        item {
            Column {
                Text(
                    text = "Quick Actions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        icon = "➕",
                        title = "Post a Job",
                        subtitle = "Hire someone",
                        onClick = { viewModel.setTab(AppNavTab.POST) },
                        modifier = Modifier.weight(1f),
                        testTag = "post_job_action"
                    )
                    QuickActionCard(
                        icon = "🗺️",
                        title = "Map & Gigs",
                        subtitle = "Find near me",
                        onClick = {
                            viewModel.toggleMapView(true)
                            viewModel.setTab(AppNavTab.JOBS)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "map_action"
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        icon = "💬",
                        title = "Messages",
                        subtitle = "Active chats",
                        onClick = { viewModel.setTab(AppNavTab.MESSAGES) },
                        modifier = Modifier.weight(1f),
                        testTag = "messages_action"
                    )
                    QuickActionCard(
                        icon = "💸",
                        title = "Withdraw",
                        subtitle = "M-Pesa / Bank",
                        onClick = { viewModel.setTab(AppNavTab.PROFILE) },
                        modifier = Modifier.weight(1f),
                        testTag = "withdraw_action"
                    )
                }
            }
        }

        // Recent Gigs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby Jobs in Nairobi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
                TextButton(onClick = { viewModel.setTab(AppNavTab.JOBS) }) {
                    Text("View all (${recentJobs.size})", color = tokens.mintAccent, fontSize = 13.sp)
                }
            }
        }

        // Recent Gigs List
        items(recentJobs.take(3), key = { it.id }) { job ->
            HomeJobCard(
                job = job,
                onClick = { inspectingJob = job },
                onApply = { viewModel.applyForJob(job) },
                onEmployerConfirmCompletion = { viewModel.confirmJobCompletion(job) },
                onViewRoute = {
                    viewModel.selectJobForRoute(job)
                    viewModel.toggleMapView(true)
                    viewModel.setTab(AppNavTab.JOBS)
                }
            )
        }
    }

    // Dedicated Job Details Sheet with Job Escrow state indicator
    inspectingJob?.let { job ->
        JobDetailsSheet(
            job = job,
            viewModel = viewModel,
            onDismiss = { inspectingJob = null },
            onApply = { viewModel.applyForJob(job) },
            onHoldEscrow = { viewModel.promptEscrowDeposit(job) },
            onEmployerConfirmCompletion = { viewModel.confirmJobCompletion(job) },
            onViewRoute = {
                viewModel.selectJobForRoute(job)
                viewModel.toggleMapView(true)
                viewModel.setTab(AppNavTab.JOBS)
            },
            onOpenReview = { viewModel.openReviewDialog(job) }
        )
    }
}
}

@Composable
private fun QuickActionCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val tokens = LocalGlassTokens.current
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        testTag = testTag,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = tokens.textPrimary
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = tokens.textMuted
        )
    }
}

@Composable
private fun HomeJobCard(
    job: Job,
    onClick: () -> Unit,
    onApply: () -> Unit,
    onViewRoute: () -> Unit,
    onEmployerConfirmCompletion: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(job.category.iconName, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = job.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 ${job.locationName} · ${job.durationHours} hrs · ${job.datePosted}",
                    fontSize = 12.sp,
                    color = tokens.textMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "KSh ${job.payKsh}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tokens.mintAccent
                )
                Text(
                    text = "Net: KSh ${job.netWorkerPayoutKsh}",
                    fontSize = 10.sp,
                    color = tokens.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Employer info & Rating / Escrow Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Posted by ${job.employerName} · ★ ${job.employerRating}",
                fontSize = 11.sp,
                color = tokens.textSecondary
            )

            when (job.status) {
                JobStatus.IN_PROGRESS -> {
                    GlassPill(
                        text = "🔒 Funds held in escrow",
                        color = tokens.purpleAccent.copy(alpha = 0.25f),
                        textColor = tokens.purpleAccent
                    )
                }
                JobStatus.APPLIED -> {
                    GlassPill(text = "Applied ✓", color = tokens.mintAccent.copy(alpha = 0.2f), textColor = tokens.mintAccent)
                }
                JobStatus.COMPLETED -> {
                    GlassPill(text = "✅ Released", color = tokens.mintAccent.copy(alpha = 0.2f), textColor = tokens.mintAccent)
                }
                else -> {}
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassButton(
                text = "🗺️ Route & Site",
                onClick = onViewRoute,
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )

            when (job.status) {
                JobStatus.IN_PROGRESS -> {
                    GlassButton(
                        text = "Employer Confirm Completion",
                        onClick = onEmployerConfirmCompletion,
                        isPrimary = true,
                        modifier = Modifier.weight(1.8f),
                        testTag = "home_employer_confirm_button"
                    )
                }
                JobStatus.APPLIED -> {
                    GlassButton(
                        text = "Applied ✓",
                        onClick = onClick,
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    GlassButton(
                        text = "Apply Now",
                        onClick = onApply,
                        enabled = job.status == JobStatus.OPEN,
                        isPrimary = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

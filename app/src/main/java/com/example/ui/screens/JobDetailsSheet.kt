package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Job
import com.example.model.JobStatus
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.components.JobLocationRouteMapView
import com.example.ui.theme.LocalGlassTokens
import com.example.util.CloudinaryService
import com.example.viewmodel.KaziViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailsSheet(
    job: Job,
    viewModel: KaziViewModel? = null,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onHoldEscrow: () -> Unit,
    onEmployerConfirmCompletion: () -> Unit,
    onViewRoute: () -> Unit,
    onOpenReview: () -> Unit
) {
    val tokens = LocalGlassTokens.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.surfaceElevated,
        dragHandle = { BottomSheetDefaults.DragHandle(color = tokens.borderGlass) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .testTag("job_details_sheet")
        ) {
            // Header: Category, Title, Gross Pay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(job.category.iconName, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = job.category.displayName,
                            fontSize = 13.sp,
                            color = tokens.mintAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tokens.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📍 ${job.locationName} · ${job.durationHours} hours duration",
                        fontSize = 12.sp,
                        color = tokens.textMuted
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "KSh ${job.payKsh}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tokens.mintAccent
                    )
                    Text(
                        text = "Gross Budget",
                        fontSize = 11.sp,
                        color = tokens.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dedicated 'Job Escrow' State Indicator
            JobEscrowStateIndicator(
                job = job,
                onEmployerConfirmCompletion = onEmployerConfirmCompletion
            )

            Spacer(modifier = Modifier.height(16.dp))

            val followedIds = viewModel?.followedEmployerIds?.collectAsStateWithLifecycle()?.value ?: emptySet()
            val isFollowing = followedIds.contains(job.employerId)

            // Employer Information Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(tokens.mintContainer)
                            .border(1.dp, tokens.mintAccent.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = job.employerName.take(2).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = tokens.mintAccent,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = job.employerName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = tokens.amberAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${job.employerRating} (${job.employerReviewCount} reviews) · Verified Employer",
                                fontSize = 11.sp,
                                color = tokens.textMuted
                            )
                        }
                    }

                    if (viewModel != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        GlassButton(
                            text = if (isFollowing) "Following ✓" else "+ Follow",
                            onClick = { viewModel.toggleFollowEmployer(job.employerId, job.employerName) },
                            isPrimary = !isFollowing,
                            modifier = Modifier.testTag("follow_employer_btn")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Site & Task Photos (Cloudinary CDN)
            if (job.photoUrls.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Site & Task Photos (${job.photoUrls.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = "Cloudinary CDN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.mintAccent
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(job.photoUrls) { photoUrl ->
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(130.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, tokens.borderGlass, RoundedCornerShape(14.dp))
                        ) {
                            AsyncImage(
                                model = CloudinaryService.getJobThumbnailUrl(photoUrl, 440, 260) ?: photoUrl,
                                contentDescription = "Job site photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Description
            Text(
                text = "Task Description",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = job.description,
                fontSize = 13.sp,
                color = tokens.textSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Google Maps SDK Map & Route Tracker
            JobLocationRouteMapView(
                job = job,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassButton(
                    text = "🗺️ View Route",
                    onClick = {
                        onDismiss()
                        onViewRoute()
                    },
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                )

                when (job.status) {
                    JobStatus.OPEN -> {
                        GlassButton(
                            text = "Apply for Job",
                            onClick = {
                                onApply()
                                onDismiss()
                            },
                            isPrimary = true,
                            modifier = Modifier.weight(1.5f),
                            testTag = "detail_apply_button"
                        )
                    }
                    JobStatus.APPLIED -> {
                        GlassButton(
                            text = "Accept & Hold Escrow",
                            onClick = {
                                onDismiss()
                                onHoldEscrow()
                            },
                            isPrimary = true,
                            modifier = Modifier.weight(1.8f),
                            testTag = "detail_hold_escrow_button"
                        )
                    }
                    JobStatus.IN_PROGRESS -> {
                        GlassButton(
                            text = "Employer Confirm Completion",
                            onClick = {
                                onDismiss()
                                onEmployerConfirmCompletion()
                            },
                            isPrimary = true,
                            modifier = Modifier.weight(2f),
                            testTag = "detail_employer_confirm_button"
                        )
                    }
                    JobStatus.COMPLETED -> {
                        GlassButton(
                            text = if (job.workerReviewed) "Reviewed ★" else "Leave Review",
                            onClick = {
                                onDismiss()
                                onOpenReview()
                            },
                            enabled = !job.workerReviewed,
                            isPrimary = true,
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * Prominent 'Job Escrow' state indicator component displaying
 * escrow protection status, fee deduction breakdown, and action triggers.
 */
@Composable
fun JobEscrowStateIndicator(
    job: Job,
    onEmployerConfirmCompletion: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val feeKsh = job.platformFeeKsh
    val netPayoutKsh = job.netWorkerPayoutKsh

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                when (job.status) {
                    JobStatus.IN_PROGRESS -> tokens.purpleAccent.copy(alpha = if (tokens.isDark) 0.18f else 0.10f)
                    JobStatus.COMPLETED -> tokens.mintAccent.copy(alpha = if (tokens.isDark) 0.18f else 0.10f)
                    else -> tokens.surfaceGlass
                }
            )
            .border(
                1.dp,
                when (job.status) {
                    JobStatus.IN_PROGRESS -> tokens.purpleAccent.copy(alpha = 0.45f)
                    JobStatus.COMPLETED -> tokens.mintAccent.copy(alpha = 0.45f)
                    else -> tokens.borderGlass
                },
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
            .testTag("job_escrow_state_indicator")
    ) {
        Column {
            // Header Row: Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (job.status == JobStatus.COMPLETED) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = "Escrow Status",
                        tint = when (job.status) {
                            JobStatus.IN_PROGRESS -> tokens.purpleAccent
                            JobStatus.COMPLETED -> tokens.mintAccent
                            else -> tokens.cyanAccent
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Job Escrow",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = tokens.textPrimary
                    )
                }

                // Status Badge
                when (job.status) {
                    JobStatus.IN_PROGRESS -> {
                        GlassPill(
                            text = "🔒 Funds held in escrow",
                            color = tokens.purpleAccent.copy(alpha = 0.25f),
                            textColor = tokens.purpleAccent
                        )
                    }
                    JobStatus.COMPLETED -> {
                        GlassPill(
                            text = "✅ Released to Wallet",
                            color = tokens.mintAccent.copy(alpha = 0.25f),
                            textColor = tokens.mintAccent
                        )
                    }
                    JobStatus.APPLIED -> {
                        GlassPill(
                            text = "Escrow Pending",
                            color = tokens.cyanAccent.copy(alpha = 0.25f),
                            textColor = tokens.cyanAccent
                        )
                    }
                    else -> {
                        GlassPill(
                            text = "Escrow Guaranteed",
                            color = tokens.surfaceElevated,
                            textColor = tokens.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Escrow Financial Breakdown
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(tokens.surfaceElevated.copy(alpha = 0.6f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Agreed Escrow Lock:", fontSize = 11.sp, color = tokens.textMuted)
                    Text("KSh ${job.payKsh}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tokens.textPrimary)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Platform Service Fee (10%):", fontSize = 11.sp, color = tokens.coralAccent)
                    Text("− KSh $feeKsh", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tokens.coralAccent)
                }
                Divider(color = tokens.borderGlass, thickness = 0.8.dp, modifier = Modifier.padding(vertical = 2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Worker Net Payout on Completion:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tokens.textPrimary)
                    Text("KSh $netPayoutKsh", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = tokens.mintAccent)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Explanatory note
            Text(
                text = when (job.status) {
                    JobStatus.IN_PROGRESS ->
                        "Payment is securely held in simulated escrow. Funds will be released to the worker's wallet balance only after the employer confirms completion."
                    JobStatus.COMPLETED ->
                        "Job has been completed and verified. Net payout was credited to the worker's wallet with the 10% platform fee deducted."
                    else ->
                        "When a worker accepts this job, payment is automatically locked in escrow before work begins."
                },
                fontSize = 11.sp,
                color = tokens.textMuted,
                lineHeight = 15.sp
            )

            // Direct 'Employer Confirm Completion' button if in progress and action provided
            if (job.status == JobStatus.IN_PROGRESS && onEmployerConfirmCompletion != null) {
                Spacer(modifier = Modifier.height(12.dp))
                GlassButton(
                    text = "Employer Confirm Completion",
                    onClick = onEmployerConfirmCompletion,
                    isPrimary = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employer_confirm_completion_button")
                )
            }
        }
    }
}

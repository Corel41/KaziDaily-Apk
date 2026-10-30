package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Job
import com.example.model.JobCategory
import com.example.model.JobStatus
import com.example.ui.components.*
import com.example.ui.theme.LocalGlassTokens
import com.example.viewmodel.KaziViewModel

@Composable
fun JobsScreen(
    viewModel: KaziViewModel,
    jobs: List<Job>,
    selectedJobForRoute: Job?,
    isMapViewActive: Boolean,
    searchQuery: String,
    selectedCategory: JobCategory?,
    selectedCategories: Set<JobCategory> = emptySet(),
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val focusManager = LocalFocusManager.current
    var inspectingJob by remember { mutableStateOf<Job?>(null) }
    var routingJobForDialog by remember { mutableStateOf<Job?>(null) }

    val categoryJobCounts by viewModel.categoryJobCounts.collectAsStateWithLifecycle()
    val allJobs by viewModel.allJobs.collectAsStateWithLifecycle()
    val vmSelectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    val activeCategories = if (selectedCategories.isNotEmpty()) selectedCategories else vmSelectedCategories

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Screen Title & View Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Find Daily Work",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tokens.textPrimary
                )
                Text(
                    text = "${jobs.size} active gigs in Nairobi",
                    fontSize = 12.sp,
                    color = tokens.textMuted
                )
            }

            // Map vs List Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.surfaceElevated)
                    .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                ViewModePill(
                    icon = Icons.Default.List,
                    label = "List",
                    isSelected = !isMapViewActive,
                    onClick = { viewModel.toggleMapView(false) },
                    testTag = "list_view_toggle"
                )
                Spacer(modifier = Modifier.width(4.dp))
                ViewModePill(
                    icon = Icons.Default.Map,
                    label = "Map",
                    isSelected = isMapViewActive,
                    onClick = { viewModel.toggleMapView(true) },
                    testTag = "map_view_toggle"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search by title, category, or location...", fontSize = 13.sp, color = tokens.textMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = tokens.textMuted)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = tokens.textMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = tokens.surfaceGlass,
                unfocusedContainerColor = tokens.surfaceGlass,
                focusedBorderColor = tokens.mintAccent,
                unfocusedBorderColor = tokens.borderGlass,
                focusedTextColor = tokens.textPrimary,
                unfocusedTextColor = tokens.textPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("job_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Category Filter",
                    tint = tokens.mintAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Filter Categories",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
                if (activeCategories.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tokens.mintContainer)
                            .border(0.6.dp, tokens.mintAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("filter_count_badge")
                    ) {
                        Text(
                            text = "${activeCategories.size} active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.mintAccent
                        )
                    }
                }
            }

            if (activeCategories.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearCategoryFilter() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("clear_category_filters_button")
                ) {
                    Text(
                        text = "Reset All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.mintAccent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal Category Filter Carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryFilterChip(
                label = "All",
                emoji = "🌟",
                count = allJobs.size,
                isSelected = activeCategories.isEmpty(),
                onClick = { viewModel.clearCategoryFilter() },
                testTag = "category_filter_all"
            )
            JobCategory.values().forEach { cat ->
                CategoryFilterChip(
                    label = cat.displayName,
                    emoji = cat.iconName,
                    count = categoryJobCounts[cat] ?: 0,
                    isSelected = activeCategories.contains(cat),
                    onClick = { viewModel.toggleCategory(cat) },
                    testTag = "category_filter_${cat.name.lowercase()}"
                )
            }
        }

        // Active Filter Summary (when one or more categories are selected)
        if (activeCategories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(tokens.surfaceGlass)
                    .border(0.6.dp, tokens.borderGlass, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("active_filters_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active: " + activeCategories.joinToString { "${it.iconName} ${it.displayName}" },
                    fontSize = 11.sp,
                    color = tokens.textSecondary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { viewModel.clearCategoryFilter() },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Category Filters",
                        tint = tokens.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Content Area: Google Maps SDK Interactive Map or List
        if (isMapViewActive) {
            InteractiveGoogleMapView(
                jobs = jobs,
                selectedJob = selectedJobForRoute,
                onSelectJob = { viewModel.selectJobForRoute(it) },
                onApplyForJob = { viewModel.applyForJob(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 80.dp)
            )
        } else {
            if (jobs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text("🔍", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (activeCategories.isNotEmpty()) "No jobs in selected categories" else "No matching jobs found",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (activeCategories.isNotEmpty()) {
                                "No active gigs currently match ${activeCategories.joinToString { it.displayName }}. Tap below to reset and see all open jobs across Nairobi."
                            } else {
                                "Try clearing your search query or adjusting your filters."
                            },
                            fontSize = 12.sp,
                            color = tokens.textMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.clearCategoryFilter()
                                viewModel.setSearchQuery("")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tokens.mintAccent,
                                contentColor = if (tokens.isDark) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("reset_category_filters_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Show All Jobs", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(jobs, key = { it.id }) { job ->
                        JobItemCard(
                            job = job,
                            onClick = { inspectingJob = job },
                            onApply = { viewModel.applyForJob(job) },
                            onViewRoute = {
                                viewModel.selectJobForRoute(job)
                                routingJobForDialog = job
                            },
                            onPromptEscrow = { viewModel.promptEscrowDeposit(job) },
                            onConfirmCompletion = { viewModel.confirmJobCompletion(job) },
                            onOpenReview = { viewModel.openReviewDialog(job) }
                        )
                    }
                }
            }
        }
    }

    // Dedicated Job Details Sheet with Job Escrow state indicator
    inspectingJob?.let { job ->
        JobDetailsSheet(
            job = job,
            onDismiss = { inspectingJob = null },
            onApply = { viewModel.applyForJob(job) },
            onHoldEscrow = { viewModel.promptEscrowDeposit(job) },
            onEmployerConfirmCompletion = { viewModel.confirmJobCompletion(job) },
            onViewRoute = {
                viewModel.selectJobForRoute(job)
                routingJobForDialog = job
            },
            onOpenReview = { viewModel.openReviewDialog(job) }
        )
    }

    // Interactive Google Map Job Routing Dialog
    routingJobForDialog?.let { job ->
        JobRoutingDialog(
            job = job,
            onDismiss = { routingJobForDialog = null }
        )
    }
}

@Composable
private fun ViewModePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val tokens = LocalGlassTokens.current
    val bg = if (isSelected) tokens.mintAccent.copy(alpha = if (tokens.isDark) 0.35f else 0.90f) else Color.Transparent
    val fg = if (isSelected) (if (tokens.isDark) tokens.mintAccent else Color.White) else tokens.textMuted

    Row(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    emoji: String = "",
    count: Int = 0,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val tokens = LocalGlassTokens.current
    val bg = if (isSelected) tokens.mintContainer else tokens.surfaceGlass
    val fg = if (isSelected) tokens.mintAccent else tokens.textSecondary
    val border = if (isSelected) tokens.mintAccent.copy(alpha = 0.8f) else tokens.borderGlass

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(if (isSelected) 1.5.dp else 1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .heightIn(min = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = tokens.mintAccent,
                    modifier = Modifier.size(13.dp)
                )
            }
            if (emoji.isNotEmpty()) {
                Text(text = emoji, fontSize = 14.sp)
            }
            Text(
                text = label,
                color = fg,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) tokens.mintAccent.copy(alpha = 0.22f) else tokens.surfaceElevated)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) tokens.mintAccent else tokens.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun JobItemCard(
    job: Job,
    onClick: () -> Unit,
    onApply: () -> Unit,
    onViewRoute: () -> Unit,
    onPromptEscrow: () -> Unit,
    onConfirmCompletion: () -> Unit,
    onOpenReview: () -> Unit
) {
    val tokens = LocalGlassTokens.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp)
    ) {
        // Header: Category emoji, Title, Pay
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = tokens.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 ${job.locationName} · ${job.durationHours} hrs duration",
                    fontSize = 12.sp,
                    color = tokens.textMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "KSh ${job.payKsh}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
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

        // Description
        Text(
            text = job.description,
            fontSize = 12.sp,
            color = tokens.textSecondary,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Employer info & Escrow Status Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Posted by ${job.employerName} · ★ ${job.employerRating} (${job.employerReviewCount})",
                fontSize = 11.sp,
                color = tokens.textMuted
            )

            when (job.status) {
                JobStatus.OPEN -> GlassPill(text = "Open Gigs", color = tokens.surfaceElevated, textColor = tokens.textSecondary)
                JobStatus.APPLIED -> GlassPill(text = "Applied ✓", color = tokens.mintAccent.copy(alpha = 0.2f), textColor = tokens.mintAccent)
                JobStatus.IN_PROGRESS -> GlassPill(text = "🔒 Funds held in escrow", color = tokens.purpleAccent.copy(alpha = 0.25f), textColor = tokens.purpleAccent)
                JobStatus.COMPLETED -> GlassPill(text = "✅ Released to Wallet", color = tokens.mintAccent.copy(alpha = 0.25f), textColor = tokens.mintAccent)
                else -> {}
            }
        }

        // When in progress, show prominent Job Escrow status box
        if (job.status == JobStatus.IN_PROGRESS) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.purpleAccent.copy(alpha = if (tokens.isDark) 0.16f else 0.08f))
                    .border(0.8.dp, tokens.purpleAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🔒 Job Escrow: Funds held in escrow",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.purpleAccent
                        )
                        Text(
                            text = "Release on completion: Gross KSh ${job.payKsh} − 10% fee = Net KSh ${job.netWorkerPayoutKsh}",
                            fontSize = 10.sp,
                            color = tokens.textMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row depending on status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassButton(
                text = "🗺️ Route",
                onClick = onViewRoute,
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )

            when (job.status) {
                JobStatus.OPEN -> {
                    GlassButton(
                        text = "Apply Now",
                        onClick = onApply,
                        isPrimary = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }
                JobStatus.APPLIED -> {
                    // Simulates employer accepting and holding payment in escrow
                    GlassButton(
                        text = "Accept & Hold Escrow",
                        onClick = onPromptEscrow,
                        isPrimary = true,
                        modifier = Modifier.weight(1.8f)
                    )
                }
                JobStatus.IN_PROGRESS -> {
                    // Employer confirms completion to release escrow minus fixed service fee
                    GlassButton(
                        text = "Employer Confirm Completion",
                        onClick = onConfirmCompletion,
                        isPrimary = true,
                        modifier = Modifier.weight(2f),
                        testTag = "employer_confirm_completion_button"
                    )
                }
                JobStatus.COMPLETED -> {
                    GlassButton(
                        text = if (job.workerReviewed) "Reviewed ★" else "Leave Review",
                        onClick = onOpenReview,
                        enabled = !job.workerReviewed,
                        isPrimary = false,
                        modifier = Modifier.weight(1.3f)
                    )
                }
                else -> {}
            }
        }
    }
}

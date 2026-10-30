package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.Review
import com.example.model.UserProfile
import com.example.model.WalletState
import com.example.ui.components.*
import com.example.ui.theme.LocalGlassTokens
import com.example.util.CloudinaryService
import com.example.viewmodel.KaziViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileWalletScreen(
    viewModel: KaziViewModel,
    userProfile: UserProfile,
    walletState: WalletState,
    reviews: List<Review>,
    onSignOut: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    val isAvatarUploading by viewModel.isAvatarUploading.collectAsStateWithLifecycle()

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadAvatarImage(context, uri)
        }
    }

    var showWithdrawModal by remember { mutableStateOf(false) }
    var showAddSkillDialog by remember { mutableStateOf(false) }
    var newSkillText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar: Title & Theme Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Profile & Wallet",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tokens.textPrimary
                    )
                    Text(
                        text = "Verified artisan & daily contractor",
                        fontSize = 12.sp,
                        color = tokens.textMuted
                    )
                }

                // Grok-like Mobile Theme Switcher
                GrokThemeSwitcher(
                    isDarkTheme = tokens.isDark,
                    onToggle = { viewModel.toggleTheme() }
                )
            }
        }

        // Profile Identity Glass Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar with Photo Picker and Cloudinary CDN
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(tokens.mintContainer)
                            .border(1.5.dp, tokens.mintAccent, CircleShape)
                            .clickable {
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("artisan_avatar_picker"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!userProfile.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = CloudinaryService.getAvatarUrl(userProfile.photoUrl, size = 180) ?: userProfile.photoUrl,
                                contentDescription = "Artisan Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val initials = userProfile.fullName.split(" ")
                                .mapNotNull { it.firstOrNull()?.toString() }
                                .take(2)
                                .joinToString("")
                                .ifBlank { "BM" }
                            Text(
                                text = initials,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = tokens.mintAccent
                            )
                        }

                        if (isAvatarUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = tokens.mintAccent,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Camera edit indicator chip
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(tokens.surfaceElevated)
                                .border(1.dp, tokens.mintAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📷", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userProfile.fullName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokens.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = tokens.mintAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = userProfile.roleTitle,
                            fontSize = 12.sp,
                            color = tokens.textMuted
                        )
                        Text(
                            text = "📍 ${userProfile.location} · ${userProfile.experienceYears} yrs exp",
                            fontSize = 11.sp,
                            color = tokens.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Row: Rating, Completed, Posted
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProfileStatItem(
                        title = "★ Rating",
                        value = "${userProfile.averageRating} / 5",
                        sub = "${userProfile.totalReviews} reviews"
                    )
                    ProfileStatItem(
                        title = "Jobs Completed",
                        value = "${userProfile.jobsCompletedAsWorker}",
                        sub = "As worker"
                    )
                    ProfileStatItem(
                        title = "Gigs Posted",
                        value = "${userProfile.jobsPostedAsEmployer}",
                        sub = "As employer"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bio
                Text(
                    text = userProfile.bio,
                    fontSize = 12.sp,
                    color = tokens.textSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Skills & Endorsements
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Skills & Expertise",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary
                    )
                    TextButton(onClick = { showAddSkillDialog = true }) {
                        Text("+ Add Skill", fontSize = 11.sp, color = tokens.mintAccent)
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    userProfile.skills.forEach { skill ->
                        GlassPill(
                            text = skill,
                            color = tokens.surfaceElevated,
                            textColor = tokens.textPrimary
                        )
                    }
                }
            }
        }

        // Wallet Balance & Escrow Overview
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "KaziDaily Wallet",
                            fontSize = 12.sp,
                            color = tokens.textMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "KSh ${walletState.availableBalanceKsh}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tokens.mintAccent
                        )
                        Text(
                            text = "Available for instant payout",
                            fontSize = 11.sp,
                            color = tokens.textMuted
                        )
                    }

                    if (walletState.lockedInEscrowKsh > 0) {
                        EscrowBadge(amountKsh = walletState.lockedInEscrowKsh)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                GlassButton(
                    text = "Withdraw Funds (M-Pesa / Bank)",
                    onClick = { showWithdrawModal = true },
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "open_withdraw_modal_button"
                )
            }
        }

        // Transaction History Header
        item {
            Text(
                text = "Financial Activity & Statements",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.textPrimary
            )
        }

        items(walletState.transactions, key = { it.id }) { tx ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tx.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "${tx.subtitle} · ${tx.dateFormatted}",
                            fontSize = 11.sp,
                            color = tokens.textMuted
                        )
                    }
                    Text(
                        text = "${if (tx.isPositive) "+" else "−"}KSh ${tx.amountKsh}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (tx.isPositive) tokens.mintAccent else tokens.coralAccent
                    )
                }
            }
        }

        // Community Reviews Section Header
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Client & Employer Reviews (${reviews.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
                Text(
                    text = "★ ${userProfile.averageRating}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.amberAccent
                )
            }
        }

        items(reviews, key = { it.id }) { rev ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = rev.reviewerName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "For: ${rev.jobTitle} · ${rev.timestampFormatted}",
                            fontSize = 11.sp,
                            color = tokens.textMuted
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (i in 1..rev.rating) {
                            Text("★", color = tokens.amberAccent, fontSize = 14.sp)
                        }
                    }
                }

                if (rev.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rev.tags.forEach { tag ->
                            GlassPill(
                                text = tag,
                                color = tokens.surfaceElevated,
                                textColor = tokens.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\"${rev.comment}\"",
                    fontSize = 12.sp,
                    color = tokens.textSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }

        // App Version & In-App APK Updates
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KaziDaily for Android",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "Current: v${viewModel.currentVersionName} (Build ${viewModel.currentVersionCode})",
                            fontSize = 11.sp,
                            color = tokens.textMuted
                        )
                    }
                    GlassButton(
                        text = "Check Updates",
                        onClick = { viewModel.checkForUpdates() },
                        isPrimary = true,
                        testTag = "check_updates_button"
                    )
                }
            }
        }

        // Account Reset Button
        item {
            Spacer(modifier = Modifier.height(6.dp))
            GlassButton(
                text = "Reset Local App Data",
                onClick = { viewModel.resetData() },
                isPrimary = false,
                isDanger = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Account Sign Out
        if (onSignOut != null) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                GlassButton(
                    text = "Sign Out of Account",
                    onClick = onSignOut,
                    isPrimary = false,
                    isDanger = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button")
                )
            }
        }
    }

    // Withdrawal Modal Sheet
    if (showWithdrawModal) {
        WithdrawalSheet(
            walletState = walletState,
            onDismiss = { showWithdrawModal = false },
            onWithdraw = { method, dest, amount ->
                val ok = viewModel.processWithdrawal(method, dest, amount)
                if (ok) showWithdrawModal = false
            }
        )
    }

    // Add Skill Dialog
    if (showAddSkillDialog) {
        AlertDialog(
            onDismissRequest = { showAddSkillDialog = false },
            containerColor = tokens.surfaceElevated,
            title = { Text("Add New Skill", color = tokens.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSkillText,
                    onValueChange = { newSkillText = it },
                    placeholder = { Text("e.g. Tile Laying, Solar Installation", color = tokens.textMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary,
                        focusedBorderColor = tokens.mintAccent,
                        unfocusedBorderColor = tokens.borderGlass
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newSkillText.isNotBlank()) {
                            viewModel.addSkill(newSkillText)
                            newSkillText = ""
                            showAddSkillDialog = false
                        }
                    }
                ) {
                    Text("Add Skill", color = tokens.mintAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSkillDialog = false }) {
                    Text("Cancel", color = tokens.textMuted)
                }
            }
        )
    }
}

@Composable
private fun ProfileStatItem(title: String, value: String, sub: String) {
    val tokens = LocalGlassTokens.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 11.sp, color = tokens.textMuted)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = tokens.textPrimary)
        Text(text = sub, fontSize = 10.sp, color = tokens.textMuted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithdrawalSheet(
    walletState: WalletState,
    onDismiss: () -> Unit,
    onWithdraw: (String, String, Int) -> Unit
) {
    val tokens = LocalGlassTokens.current
    var selectedMethod by remember { mutableStateOf("M-Pesa") }
    var phoneInput by remember { mutableStateOf("0712345678") }
    var bankName by remember { mutableStateOf("Equity Bank") }
    var bankAccountInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("2000") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.surfaceElevated,
        dragHandle = { BottomSheetDefaults.DragHandle(color = tokens.borderGlass) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Withdraw Funds",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tokens.textPrimary
            )
            Text(
                text = "Available Balance: KSh ${walletState.availableBalanceKsh}",
                fontSize = 12.sp,
                color = tokens.mintAccent,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Method Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.surfaceGlass)
                    .border(1.dp, tokens.borderGlass, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                WithdrawMethodTab(
                    title = "📱 M-Pesa",
                    isSelected = selectedMethod == "M-Pesa",
                    onClick = { selectedMethod = "M-Pesa" },
                    modifier = Modifier.weight(1f)
                )
                WithdrawMethodTab(
                    title = "🏦 Bank Account",
                    isSelected = selectedMethod == "Bank",
                    onClick = { selectedMethod = "Bank" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedMethod == "M-Pesa") {
                Text("M-Pesa Phone Number", fontSize = 12.sp, color = tokens.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mpesa_phone_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary,
                        focusedBorderColor = tokens.mintAccent,
                        unfocusedBorderColor = tokens.borderGlass
                    )
                )
            } else {
                Text("Select Bank", fontSize = 12.sp, color = tokens.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                val banks = listOf("Equity Bank", "KCB Bank", "Co-operative Bank", "NCBA Bank", "Absa Bank Kenya")
                var bankExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = bankExpanded,
                    onExpandedChange = { bankExpanded = !bankExpanded }
                ) {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = tokens.textPrimary,
                            unfocusedTextColor = tokens.textPrimary,
                            focusedBorderColor = tokens.mintAccent,
                            unfocusedBorderColor = tokens.borderGlass
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = bankExpanded,
                        onDismissRequest = { bankExpanded = false },
                        containerColor = tokens.surfaceElevated
                    ) {
                        banks.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b, color = tokens.textPrimary) },
                                onClick = {
                                    bankName = b
                                    bankExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Bank Account Number", fontSize = 12.sp, color = tokens.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = bankAccountInput,
                    onValueChange = { bankAccountInput = it },
                    placeholder = { Text("e.g. 0123456789012", color = tokens.textMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary,
                        focusedBorderColor = tokens.mintAccent,
                        unfocusedBorderColor = tokens.borderGlass
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Amount to Withdraw (KSh)", fontSize = 12.sp, color = tokens.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_amount_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = tokens.textPrimary,
                    unfocusedTextColor = tokens.textPrimary,
                    focusedBorderColor = tokens.mintAccent,
                    unfocusedBorderColor = tokens.borderGlass
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            GlassButton(
                text = "Confirm Withdrawal",
                onClick = {
                    val amount = amountInput.toIntOrNull() ?: 0
                    val dest = if (selectedMethod == "M-Pesa") phoneInput.trim() else "$bankName ($bankAccountInput)"
                    onWithdraw(selectedMethod, dest, amount)
                },
                isPrimary = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_withdrawal_button")
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WithdrawMethodTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val bg = if (isSelected) tokens.mintAccent.copy(alpha = if (tokens.isDark) 0.35f else 0.90f) else Color.Transparent
    val fg = if (isSelected) (if (tokens.isDark) tokens.mintAccent else Color.White) else tokens.textMuted

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

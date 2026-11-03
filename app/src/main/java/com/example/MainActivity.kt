package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.credentials.CredentialManager
import com.example.auth.GoogleAuthManager
import com.example.data.KaziFirestoreRepository
import com.example.data.KaziRepository
import com.example.ui.components.GrokThemeSwitcher
import com.example.ui.components.UpdateDialog
import com.example.ui.screens.*
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.KaziViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val supabaseUser by com.example.auth.SupabaseAuthManager.currentUser.collectAsStateWithLifecycle()

            val viewModel: KaziViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return KaziViewModel(
                            repository = KaziRepository(context.applicationContext),
                            supabaseRepository = com.example.data.supabase.KaziSupabaseRepository(),
                            firestoreRepository = KaziFirestoreRepository(context.applicationContext)
                        ) as T
                    }
                }
            )

            LaunchedEffect(supabaseUser) {
                viewModel.updateCurrentUser(supabaseUser)
            }

            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            val showLoginSheet by viewModel.showLoginBottomSheet.collectAsStateWithLifecycle()
            val loginContextMessage by viewModel.loginContextMessage.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val credentialManager = remember { CredentialManager.create(context) }
                KaziDailyApp(
                    viewModel = viewModel,
                    onSignOut = {
                        com.example.auth.SupabaseAuthManager.signOut(
                            context = context,
                            credentialManager = credentialManager,
                            onSignOutComplete = {
                                viewModel.updateCurrentUser(null as com.example.model.SupabaseUser?)
                                try {
                                    GoogleAuthManager.signOut(context, credentialManager, {}, coroutineScope)
                                } catch (e: Exception) {
                                    // optional fallback
                                }
                            },
                            scope = coroutineScope
                        )
                    }
                )

                com.example.ui.components.GlassLoginBottomSheet(
                    isOpen = showLoginSheet,
                    actionContextMessage = loginContextMessage,
                    onAuthSuccess = {
                        viewModel.updateCurrentUser(com.example.auth.SupabaseAuthManager.currentUser.value)
                        viewModel.dismissLoginBottomSheet()
                        viewModel.showToast("Successfully signed in!")
                    },
                    onDismiss = {
                        viewModel.dismissLoginBottomSheet()
                    }
                )
            }
        }
    }
}

@Composable
fun KaziDailyApp(
    viewModel: KaziViewModel,
    onSignOut: (() -> Unit)? = null
) {
    val tokens = LocalGlassTokens.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val walletState by viewModel.walletState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val filteredJobs by viewModel.filteredJobs.collectAsStateWithLifecycle()
    val allJobs by viewModel.allJobs.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val chatThreads by viewModel.chatThreads.collectAsStateWithLifecycle()
    val activeThreadId by viewModel.activeThreadId.collectAsStateWithLifecycle()
    val selectedJobForRoute by viewModel.selectedJobForRoute.collectAsStateWithLifecycle()
    val isMapViewActive by viewModel.isMapViewActive.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()

    val reviewDialogState by viewModel.reviewDialog.collectAsStateWithLifecycle()
    val escrowDepositState by viewModel.escrowDepositPrompt.collectAsStateWithLifecycle()
    val escrowCompletionState by viewModel.escrowCompletionResult.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    val totalUnread = chatThreads.sumOf { it.unreadCount }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background),
        containerColor = tokens.background,
        topBar = {
            // Glass Top Bar
            Surface(
                color = tokens.background.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, tokens.borderGlass),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo
                    Row(
                        modifier = Modifier.clickable { viewModel.setTab(AppNavTab.HOME) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kazi",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tokens.textPrimary
                        )
                        Text(
                            text = "Daily",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tokens.mintAccent
                        )
                    }

                    // Actions: Grok Theme Switcher & Notifications
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sleek Grok-inspired mobile theme switcher
                        GrokThemeSwitcher(
                            isDarkTheme = isDarkTheme,
                            onToggle = { viewModel.toggleTheme() }
                        )

                        // Notification bell
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(tokens.surfaceElevated)
                                .border(0.8.dp, tokens.borderGlass, CircleShape)
                                .clickable {
                                    viewModel.showToast(
                                        if (walletState.lockedInEscrowKsh > 0)
                                            "🔒 Escrow Status: KSh ${walletState.lockedInEscrowKsh} protected"
                                        else "No new notifications"
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = tokens.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            if (walletState.lockedInEscrowKsh > 0 || totalUnread > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-2).dp, y = (2).dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(tokens.mintAccent)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Floating Glass Bottom Navigation
            GlassBottomNavigationBar(
                currentTab = currentTab,
                unreadChatCount = totalUnread,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main View
            when (currentTab) {
                AppNavTab.HOME -> {
                    HomeScreen(
                        viewModel = viewModel,
                        walletState = walletState,
                        recentJobs = allJobs
                    )
                }
                AppNavTab.JOBS -> {
                    JobsScreen(
                        viewModel = viewModel,
                        jobs = filteredJobs,
                        selectedJobForRoute = selectedJobForRoute,
                        isMapViewActive = isMapViewActive,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        selectedCategories = selectedCategories
                    )
                }
                AppNavTab.POST -> {
                    PostJobScreen(viewModel = viewModel)
                }
                AppNavTab.MESSAGES -> {
                    MessagesScreen(
                        viewModel = viewModel,
                        chatThreads = chatThreads,
                        activeThreadId = activeThreadId
                    )
                }
                AppNavTab.PROFILE -> {
                    ProfileWalletScreen(
                        viewModel = viewModel,
                        userProfile = userProfile,
                        walletState = walletState,
                        reviews = reviews,
                        onSignOut = onSignOut
                    )
                }
            }

            // Top Floating Toast Notification Banner
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp, start = 20.dp, end = 20.dp)
            ) {
                if (toastMessage != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(tokens.surfaceElevated.copy(alpha = 0.95f))
                            .border(1.dp, tokens.mintAccent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = toastMessage ?: "",
                            color = tokens.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Modal Dialogs
    ReviewDialog(
        state = reviewDialogState,
        onRatingChange = { viewModel.updateReviewRating(it) },
        onToggleTag = { viewModel.toggleReviewTag(it) },
        onCommentChange = { viewModel.updateReviewComment(it) },
        onSubmit = { viewModel.submitReview() },
        onDismiss = { viewModel.dismissReviewDialog() }
    )

    EscrowDepositDialog(
        state = escrowDepositState,
        onConfirm = { viewModel.confirmEscrowDeposit(it) },
        onDismiss = { viewModel.dismissEscrowDepositPrompt() }
    )

    EscrowCompletionResultDialog(
        state = escrowCompletionState,
        onDismiss = { viewModel.dismissEscrowCompletionResult() }
    )

    UpdateDialog(
        viewModel = viewModel,
        updateState = updateState
    )
}

@Composable
fun GlassBottomNavigationBar(
    currentTab: AppNavTab,
    unreadChatCount: Int,
    onTabSelected: (AppNavTab) -> Unit
) {
    val tokens = LocalGlassTokens.current

    NavigationBar(
        containerColor = tokens.surfaceElevated.copy(alpha = 0.95f),
        contentColor = tokens.textPrimary,
        tonalElevation = 4.dp,
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier
            .fillMaxWidth()
            .border(androidx.compose.foundation.BorderStroke(0.8.dp, tokens.borderGlass))
    ) {
        AppNavTab.values().forEach { tab ->
            val isSelected = currentTab == tab

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (tab == AppNavTab.MESSAGES && unreadChatCount > 0) {
                                Badge(
                                    containerColor = tokens.coralAccent,
                                    contentColor = Color.White
                                ) {
                                    Text("$unreadChatCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Text(
                            text = tab.iconLabel,
                            fontSize = if (isSelected) 22.sp else 19.sp
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = tokens.mintAccent,
                    selectedTextColor = tokens.mintAccent,
                    indicatorColor = tokens.mintContainer.copy(alpha = if (tokens.isDark) 0.45f else 0.85f),
                    unselectedIconColor = tokens.textMuted,
                    unselectedTextColor = tokens.textMuted
                ),
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
        }
    }
}

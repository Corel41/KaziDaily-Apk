package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalGlassTokens

/**
 * Sleek, Grok-inspired mobile segmented theme switcher.
 * Replaces old "amoled / white" text with an animated sliding pill,
 * minimalist glyphs, spring physics, and mobile-native haptic feedback.
 */
@Composable
fun GrokThemeSwitcher(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp
) {
    val tokens = LocalGlassTokens.current
    val haptic = LocalHapticFeedback.current

    // Spring animation for the sliding indicator
    val indicatorOffset by animateDpAsState(
        targetValue = if (isDarkTheme) 0.dp else 36.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "GrokIndicatorOffset"
    )

    val trackBgColor by animateColorAsState(
        targetValue = if (tokens.isDark) Color(0xFF0F141E) else Color(0xFFE2E8F0),
        label = "TrackBg"
    )

    val pillColor by animateColorAsState(
        targetValue = if (tokens.isDark) Color(0xFF1E2638) else Color.White,
        label = "PillBg"
    )

    val moonTint by animateColorAsState(
        targetValue = if (isDarkTheme) tokens.mintAccent else tokens.textMuted.copy(alpha = 0.5f),
        label = "MoonTint"
    )

    val sunTint by animateColorAsState(
        targetValue = if (!isDarkTheme) Color(0xFFD97706) else tokens.textMuted.copy(alpha = 0.5f),
        label = "SunTint"
    )

    Box(
        modifier = modifier
            .testTag("grok_theme_switcher")
            .height(height)
            .width(76.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(trackBgColor)
            .border(
                width = 1.dp,
                color = if (tokens.isDark) Color(0x33FFFFFF) else Color(0x22000000),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onToggle()
            }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Sliding indicator pill
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .size(width = 34.dp, height = height - 6.dp)
                .shadow(elevation = if (isDarkTheme) 2.dp else 4.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(pillColor)
                .border(
                    width = 0.8.dp,
                    color = if (tokens.isDark) tokens.mintAccent.copy(alpha = 0.4f) else Color(0x15000000),
                    shape = RoundedCornerShape(16.dp)
                )
        )

        // Segment icons
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Dark Mode Glyph (Moon)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 16.dp)
                    ) {
                        if (!isDarkTheme) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onToggle()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Outlined.DarkMode,
                    contentDescription = "Switch to Dark Theme",
                    tint = moonTint,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Light Mode Glyph (Sun)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 16.dp)
                    ) {
                        if (isDarkTheme) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onToggle()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (!isDarkTheme) Icons.Filled.LightMode else Icons.Outlined.LightMode,
                    contentDescription = "Switch to Light Theme",
                    tint = sunTint,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalGlassTokens

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    val clickableMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = onClick
        )
    } else Modifier

    val tagMod = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Surface(
        modifier = modifier
            .then(tagMod)
            .clip(shape)
            .then(clickableMod),
        shape = shape,
        color = tokens.surfaceGlass,
        tonalElevation = elevation,
        border = BorderStroke(borderWidth, tokens.borderGlass)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    isDanger: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    testTag: String? = null
) {
    val tokens = LocalGlassTokens.current
    val tagMod = if (testTag != null) Modifier.testTag(testTag) else Modifier

    val containerColor = when {
        !enabled -> tokens.surfaceElevated.copy(alpha = 0.5f)
        isDanger -> tokens.coralAccent.copy(alpha = 0.18f)
        isPrimary -> tokens.mintAccent.copy(alpha = if (tokens.isDark) 0.22f else 0.90f)
        else -> tokens.surfaceElevated
    }

    val contentColor = when {
        !enabled -> tokens.textMuted
        isDanger -> tokens.coralAccent
        isPrimary -> if (tokens.isDark) tokens.mintAccent else Color.White
        else -> tokens.textPrimary
    }

    val borderStroke = when {
        isDanger -> BorderStroke(1.dp, tokens.coralAccent.copy(alpha = 0.35f))
        isPrimary && tokens.isDark -> BorderStroke(1.dp, tokens.mintAccent.copy(alpha = 0.40f))
        else -> BorderStroke(1.dp, tokens.borderGlass)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(tagMod),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = tokens.surfaceElevated.copy(alpha = 0.4f),
            disabledContentColor = tokens.textMuted
        ),
        border = borderStroke,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    textColor: Color? = null
) {
    val tokens = LocalGlassTokens.current
    val bg = color ?: tokens.mintContainer
    val fg = textColor ?: tokens.mintAccent

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(bg.copy(alpha = if (tokens.isDark) 0.35f else 0.85f))
            .border(0.8.dp, fg.copy(alpha = 0.3f), RoundedCornerShape(30.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChanged: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 26.dp
) {
    val tokens = LocalGlassTokens.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxStars) {
            val isSelected = i <= rating
            val icon = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star
            val tint = if (isSelected) tokens.amberAccent else tokens.textMuted.copy(alpha = 0.4f)

            val clickMod = if (onRatingChanged != null) {
                Modifier.clickable { onRatingChanged(i) }
            } else Modifier

            Icon(
                imageVector = icon,
                contentDescription = "$i stars",
                tint = tint,
                modifier = Modifier
                    .size(starSize)
                    .then(clickMod)
            )
        }
    }
}

@Composable
fun EscrowBadge(
    amountKsh: Int,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tokens.purpleAccent.copy(alpha = if (tokens.isDark) 0.18f else 0.12f))
            .border(0.8.dp, tokens.purpleAccent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🔒 Escrow Locked: KSh $amountKsh",
            color = tokens.purpleAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

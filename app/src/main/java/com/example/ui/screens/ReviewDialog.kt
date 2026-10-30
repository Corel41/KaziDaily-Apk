package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StarRatingBar
import com.example.ui.theme.LocalGlassTokens
import com.example.viewmodel.ReviewDialogState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewDialog(
    state: ReviewDialogState,
    onRatingChange: (Int) -> Unit,
    onToggleTag: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isOpen || state.job == null) return
    val tokens = LocalGlassTokens.current
    val job = state.job

    val availableTags = listOf(
        "Punctual", "Quality Work", "Great Communication",
        "Fair Employer", "Prompt Payment", "Accurate Instructions", "Clean Finish"
    )

    val ratingLabels = mapOf(
        1 to "Poor ⚠️",
        2 to "Fair 🤔",
        3 to "Good 👍",
        4 to "Great 🌟",
        5 to "Exceptional! 🏆"
    )

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("review_dialog_card"),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "Mutual Review & Rating",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tokens.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "For ${job.title} · ${job.employerName}",
                fontSize = 12.sp,
                color = tokens.textMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Star Rating Selector
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StarRatingBar(
                    rating = state.rating,
                    onRatingChanged = onRatingChange,
                    starSize = 34.dp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = ratingLabels[state.rating] ?: "Good",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.amberAccent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Skill / Experience Tags
            Text(
                text = "Select Experience Highlights",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = tokens.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableTags.forEach { tag ->
                    val isSelected = state.selectedTags.contains(tag)
                    val bg = if (isSelected) tokens.mintContainer else tokens.surfaceElevated
                    val fg = if (isSelected) tokens.mintAccent else tokens.textMuted
                    val border = if (isSelected) tokens.mintAccent.copy(alpha = 0.5f) else tokens.borderGlass

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .border(0.8.dp, border, RoundedCornerShape(16.dp))
                            .clickable { onToggleTag(tag) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = fg
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Detailed Feedback Comment
            Text(
                text = "Review Comments (Optional)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = tokens.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = state.comment,
                onValueChange = onCommentChange,
                placeholder = { Text("Share honest feedback to help the KaziDaily community...", fontSize = 12.sp, color = tokens.textMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = tokens.textPrimary,
                    unfocusedTextColor = tokens.textPrimary,
                    focusedBorderColor = tokens.mintAccent,
                    unfocusedBorderColor = tokens.borderGlass
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassButton(
                    text = "Skip",
                    onClick = onDismiss,
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Submit Review",
                    onClick = onSubmit,
                    isPrimary = true,
                    modifier = Modifier.weight(1.5f),
                    testTag = "submit_review_button"
                )
            }
        }
    }
}

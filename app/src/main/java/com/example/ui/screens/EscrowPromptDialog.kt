package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import com.example.ui.theme.LocalGlassTokens
import com.example.viewmodel.EscrowCompletionResultState
import com.example.viewmodel.EscrowDepositPromptState

@Composable
fun EscrowDepositDialog(
    state: EscrowDepositPromptState,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isOpen || state.job == null) return
    val tokens = LocalGlassTokens.current
    val job = state.job

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("escrow_deposit_dialog"),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "🔒 Secure Escrow Deposit",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tokens.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Accept applicant & authorize work for '${job.title}'",
                fontSize = 12.sp,
                color = tokens.textMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Fee breakdown card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.surfaceElevated)
                    .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Agreed Worker Wage:", fontSize = 12.sp, color = tokens.textMuted)
                        Text("KSh ${job.payKsh}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = tokens.textPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Escrow Protection Fee:", fontSize = 12.sp, color = tokens.textMuted)
                        Text("KSh 0 (Free)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = tokens.mintAccent)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Escrow Lock:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tokens.textPrimary)
                        Text("KSh ${job.payKsh}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = tokens.mintAccent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "⚡ Payment will be held safely by KaziDaily. It is only released to the worker once you inspect and confirm job completion.",
                fontSize = 11.sp,
                color = tokens.textMuted,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (state.isFunding) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = tokens.mintAccent,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Simulating M-Pesa STK Push...",
                        fontSize = 12.sp,
                        color = tokens.mintAccent
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Deposit KSh ${job.payKsh}",
                        onClick = { onConfirm(job.id) },
                        isPrimary = true,
                        modifier = Modifier
                            .weight(1.6f)
                            .testTag("confirm_escrow_deposit_button")
                    )
                }
            }
        }
    }
}

@Composable
fun EscrowCompletionResultDialog(
    state: EscrowCompletionResultState,
    onDismiss: () -> Unit
) {
    if (!state.isOpen || state.result == null) return
    val tokens = LocalGlassTokens.current
    val result = state.result

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "🎉 Job Completed & Paid!",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tokens.mintAccent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.jobTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = tokens.textPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.surfaceElevated)
                    .border(0.8.dp, tokens.borderGlass, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gross Escrow Released:", fontSize = 12.sp, color = tokens.textMuted)
                        Text("KSh ${result.grossKsh}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tokens.textPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Platform Service Fee (10%):", fontSize = 12.sp, color = tokens.coralAccent)
                        Text("− KSh ${result.platformFeeKsh}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tokens.coralAccent)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Net Credited to Wallet:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tokens.mintAccent)
                        Text("+ KSh ${result.netPayoutKsh}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = tokens.mintAccent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "The net balance is now in your KaziDaily wallet. You can accumulate earnings or withdraw anytime to M-Pesa.",
                fontSize = 11.sp,
                color = tokens.textMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassButton(
                text = "Continue to Review",
                onClick = onDismiss,
                isPrimary = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

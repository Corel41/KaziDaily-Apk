package com.example.model

enum class TransactionType {
    ESCROW_RELEASE,     // Job payment earned (positive)
    WITHDRAWAL_MPESA,   // Sent to M-Pesa (negative)
    WITHDRAWAL_BANK,    // Sent to Bank (negative)
    ESCROW_DEPOSIT,     // Funded escrow for hired worker (held)
    ESCROW_REFUND       // Cancelled escrow refund
}

data class WalletTransaction(
    val id: String,
    val title: String,
    val subtitle: String,
    val amountKsh: Int,
    val dateFormatted: String,
    val type: TransactionType,
    val isPositive: Boolean
)

data class WalletState(
    val availableBalanceKsh: Int,
    val lockedInEscrowKsh: Int,
    val totalEarningsKsh: Int,
    val transactions: List<WalletTransaction>
)

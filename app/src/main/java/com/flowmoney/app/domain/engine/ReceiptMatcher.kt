package com.flowmoney.app.domain.engine

import com.flowmoney.app.data.local.dao.TransactionDao
import com.flowmoney.app.data.local.dao.TransactionReceiptLinkDao
import com.flowmoney.app.data.local.entity.ReceiptEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.local.entity.TransactionReceiptLinkEntity
import com.flowmoney.app.data.model.MatchingStatus
import com.flowmoney.app.data.model.TransactionDirection
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class MatchCandidate(
    val transaction: TransactionEntity,
    val receipt: ReceiptEntity,
    val score: Float,
    val amountDiff: Long,
    val timeDiffHours: Long
)

@Singleton
class ReceiptMatcher @Inject constructor(
    private val transactionDao: TransactionDao,
    private val linkDao: TransactionReceiptLinkDao
) {
    companion object {
        const val MAX_TIME_WINDOW_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Find candidate transactions for a given receipt
     */
    suspend fun findMatchingCandidates(receipt: ReceiptEntity): List<MatchCandidate> {
        val receiptTime = receipt.receiptDate ?: receipt.createdAt
        val minTime = receiptTime - MAX_TIME_WINDOW_MS
        val maxTime = receiptTime + MAX_TIME_WINDOW_MS

        val transactions = transactionDao.getTransactionsBetween(minTime, maxTime)
            .filter { it.direction == TransactionDirection.EXPENSE && !it.isDeleted }

        val candidates = mutableListOf<MatchCandidate>()

        for (tx in transactions) {
            val score = calculateMatchScore(receipt, tx)
            if (score >= 0.5f) {
                val timeDiffHours = abs(tx.transactionTime - receiptTime) / (1000 * 60 * 60)
                val amountDiff = abs(tx.amountMinor - receipt.totalMinor)
                candidates.add(
                    MatchCandidate(
                        transaction = tx,
                        receipt = receipt,
                        score = score,
                        amountDiff = amountDiff,
                        timeDiffHours = timeDiffHours
                    )
                )
            }
        }

        return candidates.sortedByDescending { it.score }
    }

    /**
     * Calculate similarity score between 0.0 and 1.0
     */
    fun calculateMatchScore(receipt: ReceiptEntity, transaction: TransactionEntity): Float {
        var score = 0.0f

        // 1. Amount match (weight 0.6)
        val amountDiff = abs(receipt.totalMinor - transaction.amountMinor)
        if (amountDiff == 0L) {
            score += 0.6f
        } else if (amountDiff <= 5000L) { // within 5,000 VND
            score += 0.4f
        } else if (receipt.totalMinor > 0 && amountDiff.toFloat() / receipt.totalMinor <= 0.05f) {
            score += 0.3f
        }

        // 2. Time proximity (weight 0.25)
        val receiptTime = receipt.receiptDate ?: receipt.createdAt
        val timeDiffHours = abs(transaction.transactionTime - receiptTime) / (1000 * 60 * 60)
        if (timeDiffHours <= 2) {
            score += 0.25f
        } else if (timeDiffHours <= 12) {
            score += 0.15f
        } else if (timeDiffHours <= 24) {
            score += 0.05f
        }

        // 3. Merchant Name similarity (weight 0.15)
        val rMerchant = receipt.merchantName?.lowercase() ?: ""
        val tDesc = transaction.description.lowercase()
        val tMerchant = transaction.merchantName?.lowercase() ?: ""

        if (rMerchant.isNotEmpty()) {
            if (tDesc.contains(rMerchant) || (tMerchant.isNotEmpty() && tMerchant.contains(rMerchant))) {
                score += 0.15f
            } else if (rMerchant.split(" ").any { it.length > 3 && tDesc.contains(it) }) {
                score += 0.10f
            }
        }

        return score.coerceIn(0.0f, 1.0f)
    }

    /**
     * Confirm a match link between a transaction and a receipt without double counting
     */
    suspend fun confirmMatch(transactionId: Long, receiptId: Long, confidence: Float = 1.0f) {
        val link = TransactionReceiptLinkEntity(
            transactionId = transactionId,
            receiptId = receiptId,
            matchConfidence = confidence,
            status = MatchingStatus.CONFIRMED
        )
        linkDao.insertLink(link)
        transactionDao.linkReceipt(transactionId, receiptId)
    }

    suspend fun rejectMatch(linkId: Long) {
        linkDao.updateStatus(linkId, MatchingStatus.REJECTED)
    }
}

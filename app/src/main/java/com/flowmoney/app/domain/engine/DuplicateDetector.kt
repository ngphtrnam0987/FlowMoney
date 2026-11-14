package com.flowmoney.app.domain.engine

import com.flowmoney.app.data.local.dao.NotificationFingerprintDao
import com.flowmoney.app.data.local.dao.TransactionDao
import com.flowmoney.app.data.local.entity.NotificationFingerprintEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.domain.parser.ParsedBankNotification
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicateDetector @Inject constructor(
    private val fingerprintDao: NotificationFingerprintDao,
    private val transactionDao: TransactionDao
) {
    companion object {
        // 5 minutes time window for duplicate detection
        const val TIME_WINDOW_MS = 5 * 60 * 1000L
    }

    suspend fun isDuplicate(parsed: ParsedBankNotification): Boolean {
        val hash = generateFingerprint(parsed)

        // 1. Check exact notification fingerprint
        if (fingerprintDao.exists(hash)) {
            return true
        }

        // 2. Check external reference if present
        if (!parsed.externalReference.isNullOrBlank()) {
            val existing = transactionDao.findByExternalReference(parsed.externalReference)
            if (existing != null) {
                return true
            }
        }

        // 3. Multi-factor check: amount, direction, time window (+/- 5 min)
        val minTime = parsed.timestamp - TIME_WINDOW_MS
        val maxTime = parsed.timestamp + TIME_WINDOW_MS
        val similarTransactions = transactionDao.findSimilarTransactions(
            amountMinor = parsed.amountMinor,
            direction = parsed.direction,
            minTime = minTime,
            maxTime = maxTime
        )

        if (similarTransactions.isNotEmpty()) {
            val normalizedCurrent = normalizeText(parsed.transactionDescription)
            for (existing in similarTransactions) {
                val normalizedExisting = normalizeText(existing.description)
                // If description matches or either contains the other, consider it duplicate
                if (normalizedExisting.isNotEmpty() && normalizedCurrent.isNotEmpty()) {
                    if (normalizedCurrent == normalizedExisting ||
                        normalizedCurrent.contains(normalizedExisting) ||
                        normalizedExisting.contains(normalizedCurrent)) {
                        return true
                    }
                } else {
                    // Both have empty or similar descriptions within time window & exact amount
                    return true
                }
            }
        }

        return false
    }

    suspend fun recordFingerprint(parsed: ParsedBankNotification, transactionId: Long?) {
        val hash = generateFingerprint(parsed)
        val entity = NotificationFingerprintEntity(
            fingerprintHash = hash,
            bankIdentifier = parsed.bankIdentifier,
            amountMinor = parsed.amountMinor,
            direction = parsed.direction,
            receivedAt = parsed.timestamp,
            transactionId = transactionId
        )
        fingerprintDao.insert(entity)
    }

    fun generateFingerprint(parsed: ParsedBankNotification): String {
        // Fingerprint based on bank, amount, direction, externalRef or timestamp normalized to 1-minute bucket
        val timeBucket = parsed.timestamp / 60000L
        val raw = "${parsed.bankIdentifier}|${parsed.amountMinor}|${parsed.direction}|${parsed.externalReference ?: ""}|$timeBucket"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun normalizeText(text: String): String {
        return text.lowercase().replace(Regex("[^a-z0-9]"), "")
    }
}

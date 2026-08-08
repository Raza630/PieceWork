package com.example.workman.dataClass

import com.example.workman.dataClass.PaymentStatus.BOSS_MARKED_PAID
import com.example.workman.dataClass.PaymentStatus.PAID_OUT
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Payment status values for the manual (no-gateway) Phase 1 flow.
 *
 * The record is a two-sided confirmation state machine:
 *  - Boss taps "Mark as Paid"  → [BOSS_MARKED_PAID]
 *  - Worker taps "Confirm Received" → when both sides agree → [PAID_OUT]
 */
object PaymentStatus {
    const val UNPAID = "UNPAID"                       // No money recorded yet
    const val BOSS_MARKED_PAID = "BOSS_MARKED_PAID"   // Boss says paid, awaiting worker
    const val WORKER_CONFIRMED = "WORKER_CONFIRMED"   // Worker says received, awaiting boss
    const val PAID_OUT = "PAID_OUT"                   // Both sides confirmed ✅

    /** Method of payment chosen by the boss when posting the job. */
    const val METHOD_CASH = "CASH"
    const val METHOD_ONLINE = "ONLINE"

    /** Derive the combined status from the two confirmation flags. */
    fun resolve(bossMarkedPaid: Boolean, workerConfirmed: Boolean): String = when {
        bossMarkedPaid && workerConfirmed -> PAID_OUT
        bossMarkedPaid -> BOSS_MARKED_PAID
        workerConfirmed -> WORKER_CONFIRMED
        else -> UNPAID
    }
}

/**
 * An auditable record of a boss→worker payment for a single job.
 *
 * One document per job in the `payments` collection, with docId == jobId for a
 * clean 1:1 link to the work offer and booking. No payment gateway is involved:
 * this simply records that money changed hands (cash / UPI / etc.) and that both
 * parties agreed it did.
 */
@IgnoreExtraProperties
data class PaymentRecord(
    val jobId: String = "",
    val bossId: String = "",
    val bossName: String = "",
    val workerId: String = "",
    val workerName: String = "",
    val serviceName: String = "",
    val amount: Double = 0.0,
    val currency: String = "Rs",
    val method: String = PaymentStatus.METHOD_CASH,
    val bossMarkedPaid: Boolean = false,
    val workerConfirmed: Boolean = false,
    val status: String = PaymentStatus.UNPAID,
    // Transient — filled from the Firestore Timestamp at read time
    val paidAtMillis: Long = 0L
) {
    val isPaidOut: Boolean get() = status == PaymentStatus.PAID_OUT

    /** Human-readable amount, e.g. "Rs 1500" or "Negotiable" when unset. */
    fun amountLabel(): String {
        if (amount <= 0.0) return "Negotiable"
        val amt = if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
        return "$currency $amt"
    }
}


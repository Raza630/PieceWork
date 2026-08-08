package com.example.workman.utils

import android.util.Log
import com.example.workman.dataClass.PaymentRecord
import com.example.workman.dataClass.PaymentStatus
import com.example.workman.utils.PaymentRepository.confirmReceivedByWorker
import com.example.workman.utils.PaymentRepository.markPaidByBoss
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

/**
 * Manual (no-gateway) payment recording for Phase 1.
 *
 * The `payments` collection stores one document per job (docId == jobId). Money
 * is exchanged off-app (cash / UPI / bank); this repository only records the
 * two-sided confirmation so earnings become honest and auditable:
 *
 *   Boss  → [markPaidByBoss]      sets bossMarkedPaid = true
 *   Worker→ [confirmReceivedByWorker] sets workerConfirmed = true
 *   When both are true → status = PAID_OUT.
 *
 * All writes source the amount / participants from the linked work offer inside
 * a transaction, so a stray client can't invent an amount.
 */
object PaymentRepository {

    private const val TAG = "PaymentRepository"
    const val COLLECTION = "payments"

    private val db get() = FirebaseFirestore.getInstance()

    /** Boss records that they've paid the worker (cash/UPI/etc.). */
    fun markPaidByBoss(jobId: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        upsert(jobId, byBoss = true, onResult)
    }

    /** Worker confirms they actually received the money. */
    fun confirmReceivedByWorker(jobId: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        upsert(jobId, byBoss = false, onResult)
    }

    /**
     * Idempotently create/update the payment doc for [jobId], flipping either the
     * boss or worker confirmation flag and recomputing the combined status.
     */
    private fun upsert(
        jobId: String,
        byBoss: Boolean,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (jobId.isBlank()) {
            onResult(false, "Missing job id")
            return
        }
        val payRef = db.collection(COLLECTION).document(jobId)
        val offerRef = db.collection("workOffers").document(jobId)

        db.runTransaction { tr ->
            val offer = tr.get(offerRef)
            if (!offer.exists()) throw IllegalStateException("Job no longer exists")
            val pay = tr.get(payRef)

            val bossMarked = (pay.getBoolean("bossMarkedPaid") ?: false) || byBoss
            val workerConfirmed = (pay.getBoolean("workerConfirmed") ?: false) || !byBoss
            val status = PaymentStatus.resolve(bossMarked, workerConfirmed)

            val data = hashMapOf<String, Any?>(
                "jobId" to jobId,
                "bossId" to (offer.getString("bossId") ?: ""),
                "bossName" to (offer.getString("bossName") ?: ""),
                "workerId" to (offer.getString("acceptedBy") ?: ""),
                "workerName" to (offer.getString("acceptedByName") ?: "Worker"),
                "serviceName" to (offer.getString("title") ?: ""),
                "amount" to (offer.getDouble("budgetAmount") ?: 0.0),
                "currency" to (offer.getString("currency") ?: "Rs"),
                "method" to (offer.getString("paymentMethod") ?: PaymentStatus.METHOD_CASH),
                "bossMarkedPaid" to bossMarked,
                "workerConfirmed" to workerConfirmed,
                "status" to status
            )
            if (!pay.exists()) data["createdAt"] = FieldValue.serverTimestamp()
            if (byBoss) data["bossMarkedAt"] = FieldValue.serverTimestamp()
            else data["workerConfirmedAt"] = FieldValue.serverTimestamp()
            // Stamp the settlement time exactly once, when it first reaches PAID_OUT.
            if (status == PaymentStatus.PAID_OUT && pay.getString("status") != PaymentStatus.PAID_OUT) {
                data["paidAt"] = FieldValue.serverTimestamp()
            }

            tr.set(payRef, data, SetOptions.merge())
            status
        }.addOnSuccessListener { status ->
            // Best-effort mirror onto the linked booking so the boss's Bookings
            // tab can show payment state without a second listener.
            db.collection("bookings").document(jobId)
                .update("paymentStatus", status)
                .addOnFailureListener {
                    Log.w(
                        TAG,
                        "Booking paymentStatus sync failed: ${it.message}"
                    )
                }
            onResult(true, status)
        }.addOnFailureListener { e ->
            Log.e(TAG, "Payment upsert failed for $jobId", e)
            onResult(false, e.localizedMessage ?: "Something went wrong")
        }
    }

    /** Map a Firestore doc to a [PaymentRecord]. */
    fun fromSnapshot(doc: DocumentSnapshot): PaymentRecord? {
        if (!doc.exists()) return null
        return PaymentRecord(
            jobId = doc.getString("jobId") ?: doc.id,
            bossId = doc.getString("bossId") ?: "",
            bossName = doc.getString("bossName") ?: "",
            workerId = doc.getString("workerId") ?: "",
            workerName = doc.getString("workerName") ?: "Worker",
            serviceName = doc.getString("serviceName") ?: "",
            amount = doc.getDouble("amount") ?: 0.0,
            currency = doc.getString("currency") ?: "Rs",
            method = doc.getString("method") ?: PaymentStatus.METHOD_CASH,
            bossMarkedPaid = doc.getBoolean("bossMarkedPaid") ?: false,
            workerConfirmed = doc.getBoolean("workerConfirmed") ?: false,
            status = doc.getString("status") ?: PaymentStatus.UNPAID,
            paidAtMillis = doc.getTimestamp("paidAt")?.toDate()?.time ?: 0L
        )
    }

    /** Live listener on a single job's payment doc. */
    fun listenForJob(jobId: String, onChange: (PaymentRecord?) -> Unit): ListenerRegistration {
        return db.collection(COLLECTION).document(jobId)
            .addSnapshotListener { snap, _ ->
                onChange(snap?.let { fromSnapshot(it) })
            }
    }

    /** Live listener on every payment that belongs to a given user (boss or worker). */
    fun listenForUser(
        field: String, // "bossId" or "workerId"
        userId: String,
        onChange: (List<PaymentRecord>) -> Unit
    ): ListenerRegistration {
        return db.collection(COLLECTION)
            .whereEqualTo(field, userId)
            .addSnapshotListener { snap, _ ->
                onChange(snap?.documents?.mapNotNull { fromSnapshot(it) } ?: emptyList())
            }
    }
}


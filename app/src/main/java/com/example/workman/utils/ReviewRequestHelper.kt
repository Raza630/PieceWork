package com.example.workman.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Handles the "worker seeks feedback from the boss" flow.
 *
 * When a job is completed, the worker can politely ask the client to leave a
 * review. This:
 *   1. Flags the work offer (`reviewRequested = true`) so the UI can show status.
 *   2. Creates an in-app notification for the boss prompting them to review.
 */
object ReviewRequestHelper {

    /**
     * Sends a feedback/review request from the current worker to the job's boss.
     *
     * @param jobId     The completed work offer id.
     * @param bossId    The client's user id (recipient of the notification).
     * @param jobTitle  Job title, used in the notification body.
     * @param onResult  Callback with (success, userMessage).
     */
    fun requestBossReview(
        jobId: String,
        bossId: String,
        jobTitle: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (bossId.isBlank()) {
            onResult(false, "We couldn't find the client for this job.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val workerName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Your worker"

        // 1. Flag the offer so the worker sees the request status.
        db.collection("workOffers").document(jobId)
            .update(
                mapOf(
                    "reviewRequested" to true,
                    "reviewRequestedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                // 2. Notify the boss in-app.
                val notification = hashMapOf(
                    "recipientId" to bossId,
                    "title" to "⭐ Feedback requested",
                    "body" to "$workerName would appreciate your feedback on \"$jobTitle\". " +
                            "Tap to leave a quick review.",
                    "type" to "review_request",
                    "jobId" to jobId,
                    "isRead" to false,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                db.collection("notifications").add(notification)
                    .addOnSuccessListener {
                        onResult(true, "Your feedback request was sent to the client.")
                    }
                    .addOnFailureListener {
                        // Offer flag already saved, so treat as partial success.
                        onResult(
                            true,
                            "Request saved. The client will be reminded to review your work."
                        )
                    }
            }
            .addOnFailureListener { e ->
                onResult(
                    false,
                    e.localizedMessage ?: "Couldn't send the request. Please try again."
                )
            }
    }
}


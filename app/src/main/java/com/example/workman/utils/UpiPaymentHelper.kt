package com.example.workman.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Builds UPI deep links so a boss can pay a worker directly from any installed
 * UPI app (GPay, PhonePe, Paytm, BHIM…). No payment gateway, no SDK, no fees —
 * this simply hands a pre-filled payment request to the user's own UPI app.
 *
 * NOTE: UPI intents do NOT give us a trustworthy success callback (the result
 * can be spoofed and many apps return nothing at all). That's why Phase 1 still
 * relies on the two-sided confirmation in [PaymentRepository]: the deep link is
 * a convenience for *making* the payment, not proof that it happened.
 */
object UpiPaymentHelper {

    /**
     * A worker's payout destination. Either a real VPA ("name@bank") or a phone
     * number that we convert into the common `@upi` handle.
     */
    data class PayeeInfo(
        val upiId: String = "",
        val phone: String = "",
        val name: String = "Worker"
    ) {
        /** The VPA we should pay to, or empty when the worker hasn't set one up. */
        val resolvedVpa: String
            get() = when {
                upiId.isNotBlank() -> upiId.trim()
                // Many Indian banks map a 10-digit mobile to <number>@upi
                phone.filter { it.isDigit() }.length >= 10 ->
                    phone.filter { it.isDigit() }.takeLast(10) + "@upi"

                else -> ""
            }

        val canPayOnline: Boolean get() = resolvedVpa.isNotBlank()
    }

    /** Basic sanity check for a VPA like `someone@okhdfcbank`. */
    fun isValidUpiId(value: String): Boolean {
        val v = value.trim()
        if (!v.contains('@')) return false
        val parts = v.split('@')
        if (parts.size != 2) return false
        return parts[0].length >= 2 && parts[1].length >= 2 &&
                v.all { it.isLetterOrDigit() || it == '@' || it == '.' || it == '-' || it == '_' }
    }

    /**
     * Builds the standard UPI payment URI.
     *
     * @param amount pass <= 0 to let the payer type the amount themselves.
     * @param note   short transaction note shown in the UPI app.
     */
    fun buildUpiUri(
        payeeVpa: String,
        payeeName: String,
        amount: Double,
        note: String,
        transactionRef: String
    ): Uri {
        val builder = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", payeeVpa)
            .appendQueryParameter("pn", payeeName.ifBlank { "Worker" })
            .appendQueryParameter("cu", "INR")
        if (amount > 0.0) {
            builder.appendQueryParameter("am", String.format("%.2f", amount))
        }
        if (note.isNotBlank()) {
            builder.appendQueryParameter("tn", note.take(50))
        }
        if (transactionRef.isNotBlank()) {
            builder.appendQueryParameter("tr", transactionRef.take(35))
        }
        return builder.build()
    }

    /**
     * Opens the Android UPI app chooser pre-filled with this payment.
     *
     * Package visibility (Android 11+) requires the `upi` scheme to be declared
     * in AndroidManifest `<queries>`, otherwise installed UPI apps are invisible
     * and resolution silently fails.
     *
     * @return true if a UPI app was launched, false when none is installed.
     */
    fun launchUpiPayment(
        context: Context,
        payee: PayeeInfo,
        amount: Double,
        note: String,
        transactionRef: String
    ): Boolean {
        val vpa = payee.resolvedVpa
        if (vpa.isBlank()) return false

        val uri = buildUpiUri(vpa, payee.name, amount, note, transactionRef)
        val intent = Intent(Intent.ACTION_VIEW, uri)

        // Prefer an explicit chooser so the user can pick GPay / PhonePe / Paytm…
        val targets = context.packageManager.queryIntentActivities(intent, 0)
        val chooser = Intent.createChooser(intent, "Pay with").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            when {
                targets.isNotEmpty() -> {
                    context.startActivity(chooser)
                    true
                }

                else -> {
                    // queryIntentActivities can still come back empty on some OEM
                    // builds even when a UPI app exists — try launching anyway and
                    // let ActivityNotFoundException be the real signal.
                    context.startActivity(chooser)
                    true
                }
            }
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}


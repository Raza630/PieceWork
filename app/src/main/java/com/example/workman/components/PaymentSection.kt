package com.example.workman.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workman.dataClass.PaymentRecord
import com.example.workman.dataClass.PaymentStatus
import com.example.workman.utils.PaymentRepository
import com.example.workman.utils.UpiPaymentHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private val PaidGreen = Color(0xFF4CAF50)
private val PendingAmber = Color(0xFFFF9800)
private val MutedGrey = Color(0xFF8E92B2)

/**
 * Two-sided manual payment panel (Phase 1 — no gateway).
 *
 * Boss view : "Pay via UPI" deep link + "Mark as Paid".
 * Worker view: "Confirm Received".
 * When both sides confirm, the record becomes PAID_OUT and the worker's
 * earnings update automatically.
 */
@Composable
fun PaymentSection(
    jobId: String,
    isBoss: Boolean,
    modifier: Modifier = Modifier,
    onChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    var record by remember(jobId) { mutableStateOf<PaymentRecord?>(null) }
    var payee by remember(jobId) { mutableStateOf(UpiPaymentHelper.PayeeInfo()) }
    var isWorking by remember(jobId) { mutableStateOf(false) }
    var error by remember(jobId) { mutableStateOf<String?>(null) }

    // Live payment state for this job.
    DisposableEffect(jobId) {
        val reg = PaymentRepository.listenForJob(jobId) { record = it }
        onDispose { reg.remove() }
    }

    // Boss needs the worker's payout details to build the UPI link.
    LaunchedEffect(jobId, isBoss) {
        if (!isBoss) return@LaunchedEffect
        try {
            val db = FirebaseFirestore.getInstance()
            val offer = db.collection("workOffers").document(jobId).get().await()
            val workerId = offer.getString("acceptedBy").orEmpty()
            if (workerId.isNotBlank()) {
                val w = db.collection("users").document(workerId).get().await()
                payee = UpiPaymentHelper.PayeeInfo(
                    upiId = w.getString("upiId").orEmpty(),
                    phone = w.getString("phone").orEmpty(),
                    name = w.getString("name") ?: "Worker"
                )
            }
        } catch (_: Exception) { /* non-fatal: UPI button simply won't show */
        }
    }

    val status = record?.status ?: PaymentStatus.UNPAID
    val amount = record?.amount ?: 0.0
    val bossDone = record?.bossMarkedPaid == true
    val workerDone = record?.workerConfirmed == true

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Status banner
        val (bannerColor, bannerText) = when {
            status == PaymentStatus.PAID_OUT ->
                PaidGreen to "✓ Payment settled — both sides confirmed"

            bossDone && !workerDone ->
                PendingAmber to "Boss marked as paid — waiting for worker to confirm"

            workerDone && !bossDone ->
                PendingAmber to "Worker confirmed receipt — waiting for boss to confirm"

            else ->
                MutedGrey to "Payment not recorded yet"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerColor.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status == PaymentStatus.PAID_OUT) {
                Icon(
                    Icons.Default.CheckCircle, null,
                    tint = PaidGreen, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    bannerText,
                    fontSize = 12.sp,
                    color = bannerColor,
                    fontWeight = FontWeight.Medium
                )
                if (amount > 0.0) {
                    Text(
                        record?.amountLabel().orEmpty(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = bannerColor
                    )
                }
            }
        }

        error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, fontSize = 11.sp, color = Color.Red)
        }

        if (status == PaymentStatus.PAID_OUT) return@Column

        Spacer(Modifier.height(10.dp))

        if (isBoss) {
            // ── Pay via UPI (opens the boss's own UPI app, pre-filled)
            if (payee.canPayOnline) {
                OutlinedButton(
                    onClick = {
                        val launched = UpiPaymentHelper.launchUpiPayment(
                            context = context,
                            payee = payee,
                            amount = amount,
                            note = "WorkMan job payment",
                            transactionRef = jobId
                        )
                        if (!launched) error = "No UPI app found on this device."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📲  Pay ${payee.resolvedVpa} via UPI", fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    isWorking = true; error = null
                    PaymentRepository.markPaidByBoss(jobId) { ok, msg ->
                        isWorking = false
                        if (ok) onChanged() else error = msg
                    }
                },
                enabled = !isWorking && !bossDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PaidGreen)
            ) {
                if (isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (bossDone) "✓ You marked this paid" else "Mark as Paid",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
            }
            if (!bossDone) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Only mark as paid after you've actually sent the money.",
                    fontSize = 10.sp, color = MutedGrey
                )
            }
        } else {
            Button(
                onClick = {
                    isWorking = true; error = null
                    PaymentRepository.confirmReceivedByWorker(jobId) { ok, msg ->
                        isWorking = false
                        if (ok) onChanged() else error = msg
                    }
                },
                enabled = !isWorking && !workerDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PaidGreen)
            ) {
                if (isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (workerDone) "✓ You confirmed receipt" else "Confirm Payment Received",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
            }
            if (!workerDone) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Confirm only after the money has reached you. " +
                            "Your earnings update once both sides confirm.",
                    fontSize = 10.sp, color = MutedGrey
                )
            }
        }
    }
}


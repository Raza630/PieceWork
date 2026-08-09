package com.example.workman.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workman.dataClass.PaymentRecord
import com.example.workman.dataClass.PaymentStatus
import com.example.workman.utils.PaymentRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
private val PaidGreen = Color(0xFF4CAF50)
private val PendingAmber = Color(0xFFFF9800)
private val MutedGrey = Color(0xFF8E92B2)
private val TextDark = Color(0xFF1A1C3D)
private val TrackGrey = Color(0xFFE4E6F0)
/**
 * Manual payment record panel (Phase 1 - no gateway, no UPI deep links).
 *
 * Money changes hands outside the app (cash, bank transfer, UPI - whatever the
 * two parties agreed). This panel exists purely to produce an auditable record
 * that BOTH sides agree the payment happened:
 *
 *   Client -> "Mark as Paid"
 *   Worker -> "Confirm Received"
 *   Both confirmed -> PAID_OUT -> counts toward the worker's earnings.
 *
 * Neither side can complete the record alone, which keeps earnings honest.
 */
@Composable
fun PaymentSection(
    jobId: String,
    isBoss: Boolean,
    modifier: Modifier = Modifier,
    onChanged: () -> Unit = {}
) {
    var record by remember(jobId) { mutableStateOf<PaymentRecord?>(null) }
    var offerAmount by remember(jobId) { mutableStateOf(0.0) }
    var offerCurrency by remember(jobId) { mutableStateOf("Rs") }
    var offerMethod by remember(jobId) { mutableStateOf(PaymentStatus.METHOD_CASH) }
    var isWorking by remember(jobId) { mutableStateOf(false) }
    var error by remember(jobId) { mutableStateOf<String?>(null) }
    // Live payment state for this job.
    DisposableEffect(jobId) {
        val reg = PaymentRepository.listenForJob(jobId) { record = it }
        onDispose { reg.remove() }
    }
    // The agreed amount/method live on the work offer. The payments document
    // isn't created until someone confirms, so we can't rely on it for display.
    LaunchedEffect(jobId) {
        try {
            val offer = FirebaseFirestore.getInstance()
                .collection("workOffers").document(jobId).get().await()
            offerAmount = offer.getDouble("budgetAmount") ?: 0.0
            offerCurrency = offer.getString("currency") ?: "Rs"
            offerMethod = offer.getString("paymentMethod") ?: PaymentStatus.METHOD_CASH
        } catch (_: Exception) {
            // Non-fatal: the panel still works, it just shows no amount.
        }
    }
    val status = record?.status ?: PaymentStatus.UNPAID
    val amount = record?.amount?.takeIf { it > 0.0 } ?: offerAmount
    val method = record?.method?.takeIf { it.isNotBlank() } ?: offerMethod
    val bossDone = record?.bossMarkedPaid == true
    val workerDone = record?.workerConfirmed == true
    val isSettled = status == PaymentStatus.PAID_OUT
    val accent = when {
        isSettled -> PaidGreen
        bossDone || workerDone -> PendingAmber
        else -> MutedGrey
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.06f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: title, state, amount + method
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Payment",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = when {
                            isSettled -> "Settled - both sides confirmed"
                            bossDone -> "Awaiting worker confirmation"
                            workerDone -> "Awaiting client confirmation"
                            else -> "Not recorded yet"
                        },
                        fontSize = 11.sp,
                        color = accent,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (amount > 0.0) {
                        val amt = if (amount % 1.0 == 0.0) amount.toLong().toString()
                        else amount.toString()
                        Text(
                            "$offerCurrency $amt",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    } else {
                        Text("Negotiable", fontSize = 13.sp, color = MutedGrey)
                    }
                    Text(
                        if (method == PaymentStatus.METHOD_ONLINE) "Online" else "Cash",
                        fontSize = 10.sp,
                        color = MutedGrey
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // Two-step confirmation tracker
            ConfirmationStep(label = "Client marked as paid", done = bossDone)
            Spacer(Modifier.height(6.dp))
            ConfirmationStep(label = "Worker confirmed receipt", done = workerDone)
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 11.sp, color = Color(0xFFD32F2F))
            }
            // Once settled the record is final - no further action needed.
            if (isSettled) return@Column
            Spacer(Modifier.height(12.dp))
            val myTurnDone = if (isBoss) bossDone else workerDone
            Button(
                onClick = {
                    isWorking = true
                    error = null
                    val onResult: (Boolean, String?) -> Unit = { ok, msg ->
                        isWorking = false
                        if (ok) onChanged() else error = msg
                    }
                    if (isBoss) PaymentRepository.markPaidByBoss(jobId, onResult)
                    else PaymentRepository.confirmReceivedByWorker(jobId, onResult)
                },
                enabled = !isWorking && !myTurnDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaidGreen,
                    disabledContainerColor = PaidGreen.copy(alpha = 0.35f)
                )
            ) {
                if (isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = when {
                            myTurnDone && isBoss -> "You marked this paid"
                            myTurnDone -> "You confirmed receipt"
                            isBoss -> "Mark as Paid"
                            else -> "Confirm Payment Received"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            if (!myTurnDone) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isBoss) {
                        "Only mark as paid after you've actually paid the worker."
                    } else {
                        "Confirm only once the money has reached you. " +
                                "Your earnings update when both sides confirm."
                    },
                    fontSize = 10.sp,
                    color = MutedGrey
                )
            }
        }
    }
}

/** A single tick-row in the two-sided confirmation tracker. */
@Composable
private fun ConfirmationStep(label: String, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (done) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PaidGreen,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(TrackGrey)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = if (done) TextDark else MutedGrey,
            fontWeight = if (done) FontWeight.Medium else FontWeight.Normal
        )
    }
}

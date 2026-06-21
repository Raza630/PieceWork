package com.example.workman.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.TextDark
import com.example.workman.ui.theme.TextMuted

/**
 * Reusable Report Dialog — required for Google Play Store compliance.
 *
 * Usage:
 * ```
 * ReportDialog(
 *     entityId = workerId,
 *     entityType = "USER",  // or "JOB"
 *     onSubmit = { entityId, type, reason -> viewModel.submitReport(entityId, type, reason) },
 *     onDismiss = { showDialog = false }
 * )
 * ```
 */
@Composable
fun ReportDialog(
    entityId: String,
    entityType: String = "USER", // USER or JOB
    onSubmit: (entityId: String, type: String, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    val reportReasons = if (entityType == "USER") {
        listOf(
            "Inappropriate behavior",
            "Fake profile / Scam",
            "Harassment or threats",
            "No-show / Unreliable",
            "Inappropriate content",
            "Other"
        )
    } else {
        listOf(
            "Misleading job description",
            "Suspicious / Scam posting",
            "Inappropriate content",
            "Discriminatory requirements",
            "Unsafe working conditions",
            "Other"
        )
    }

    var selectedReason by remember { mutableStateOf("") }
    var additionalDetails by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = if (entityType == "USER") "Report User" else "Report Job",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Help us keep WorkMan safe. Select a reason:",
                    fontSize = 13.sp,
                    color = TextMuted
                )

                Spacer(Modifier.height(16.dp))

                // Reason selection
                reportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                        )
                        Text(
                            text = reason,
                            fontSize = 14.sp,
                            color = TextDark,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Additional details
                OutlinedTextField(
                    value = additionalDetails,
                    onValueChange = { additionalDetails = it },
                    label = { Text("Additional details (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val fullReason = if (additionalDetails.isNotBlank()) {
                                "$selectedReason: $additionalDetails"
                            } else {
                                selectedReason
                            }
                            onSubmit(entityId, entityType, fullReason)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedReason.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("Report", color = Color.White)
                    }
                }
            }
        }
    }
}


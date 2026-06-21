package com.example.workman.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.TextDark
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Quick Contact Section shown on Job Details after acceptance.
 * Allows boss to call or WhatsApp the worker, and vice versa.
 */
@Composable
fun QuickContactSection(
    userId: String,
    userName: String,
    label: String = "Contact",
    onChatClick: () -> Unit = {}
) {
    var phone by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(userId) {
        try {
            val doc = db.collection("users").document(userId).get().await()
            phone = doc.getString("phone")
        } catch (_: Exception) {
        }
        isLoading = false
    }

    if (isLoading) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$label: $userName",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TextDark
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call button
                Button(
                    onClick = {
                        if (phone.isNullOrEmpty()) {
                            Toast.makeText(
                                context,
                                "Phone number not available",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            makePhoneCall(context, phone!!)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Call", fontSize = 13.sp)
                }

                // WhatsApp button
                Button(
                    onClick = {
                        if (phone.isNullOrEmpty()) {
                            Toast.makeText(
                                context,
                                "Phone number not available",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            openWhatsApp(context, phone!!)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Text("💬", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("WhatsApp", fontSize = 13.sp)
                }

                // In-app Chat button
                OutlinedButton(
                    onClick = onChatClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PrimaryBlue
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Chat", fontSize = 13.sp, color = PrimaryBlue)
                }
            }
        }
    }
}

private fun makePhoneCall(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phone")
    }
    context.startActivity(intent)
}

private fun openWhatsApp(context: Context, phone: String) {
    val formattedPhone = phone.replace(" ", "").replace("-", "")
    val whatsappNumber =
        if (formattedPhone.startsWith("+")) formattedPhone else "+91$formattedPhone"
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/${whatsappNumber.replace("+", "")}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
    }
}


package com.example.workman.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.TextDark
import com.example.workman.ui.theme.TextMuted
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Placeholder names that mean "we don't really know the name yet". */
private val PLACEHOLDER_NAMES = setOf("user", "worker", "boss", "workman client", "")

/**
 * Quick Contact Section shown on Job Details after acceptance.
 * Allows the boss to call/WhatsApp/chat the worker, and vice versa.
 *
 * The display name and phone are resolved from the user's Firestore profile so
 * they are always correct — even for older jobs that stored a placeholder like
 * "User". The passed [userName] is only used as an initial hint.
 */
@Composable
fun QuickContactSection(
    userId: String,
    userName: String,
    label: String = "Contact",
    onChatClick: () -> Unit = {}
) {
    var phone by remember { mutableStateOf<String?>(null) }
    var resolvedName by remember { mutableStateOf(userName) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(userId) {
        try {
            val doc = db.collection("users").document(userId).get().await()
            phone = doc.getString("phone")

            // Prefer the profile's real name; fall back to firstName + lastName.
            val storedName = doc.getString("name")?.trim().orEmpty()
            val composed = listOfNotNull(
                doc.getString("firstName")?.trim(),
                doc.getString("lastName")?.trim()
            ).filter { it.isNotBlank() }.joinToString(" ")
            val best = when {
                storedName.isNotBlank() -> storedName
                composed.isNotBlank() -> composed
                else -> ""
            }
            // Only override when we actually found something and the current hint
            // is empty/placeholder — otherwise keep the passed name.
            if (best.isNotBlank() &&
                (userName.trim().lowercase() in PLACEHOLDER_NAMES)
            ) {
                resolvedName = best
            } else if (best.isNotBlank() && resolvedName.isBlank()) {
                resolvedName = best
            }
        } catch (_: Exception) {
        }
        isLoading = false
    }

    val displayName = resolvedName.ifBlank { userName.ifBlank { "—" } }
    val hasPhone = !phone.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header: avatar + label + resolved name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(PrimaryBlue.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, fontSize = 11.sp, color = TextMuted)
                    if (isLoading) {
                        Text("Loading…", fontSize = 15.sp, color = TextMuted)
                    } else {
                        Text(
                            displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Adaptive action tiles: Call · WhatsApp · Chat (equal width)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ContactActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Call,
                    label = "Call",
                    containerColor = Color(0xFF4CAF50),
                    enabled = hasPhone,
                    onClick = {
                        if (hasPhone) makePhoneCall(context, phone!!)
                        else toastNoPhone(context)
                    }
                )
                ContactActionTile(
                    modifier = Modifier.weight(1f),
                    emoji = "💬",
                    label = "WhatsApp",
                    containerColor = Color(0xFF25D366),
                    enabled = hasPhone,
                    onClick = {
                        if (hasPhone) openWhatsApp(context, phone!!)
                        else toastNoPhone(context)
                    }
                )
                ContactActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Chat,
                    label = "Chat",
                    containerColor = PrimaryBlue,
                    outlined = true,
                    enabled = true,
                    onClick = onChatClick
                )
            }

            if (!isLoading && !hasPhone) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Phone not shared — use in-app Chat instead.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

/**
 * A compact, equal-width contact action: icon/emoji on top with a label below.
 * This layout adapts to narrow screens far better than side-by-side text
 * buttons and keeps all three actions visually balanced.
 */
@Composable
private fun ContactActionTile(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emoji: String? = null,
    label: String,
    containerColor: Color,
    outlined: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    val bg = if (outlined) Color.Transparent else containerColor.copy(alpha = alpha)
    val contentColor = if (outlined) containerColor.copy(alpha = alpha) else Color.White

    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (outlined)
                    Modifier.border(
                        1.5.dp,
                        containerColor.copy(alpha = alpha),
                        RoundedCornerShape(14.dp)
                    )
                else Modifier
            )
            .background(bg, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            emoji != null -> Text(emoji, fontSize = 18.sp)
            icon != null -> Icon(
                icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun toastNoPhone(context: Context) {
    Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
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

